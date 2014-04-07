(function($) {
$.namespace('biomart.martform', function(self) {
    var appName = 'MartForm',
        queryClient = 'webbrowser',

        queryMart = null,
        martsHash = {},
        dsHash = {},
        configs = {},
        multiselect = false,
        independent = false,
        datasets = [],
        filtersCache = {},
        filters = [],
        QUERY_LIMIT = 1000,
        ANIMATION_TIME = 150,
        blockOptions = {
            message: '<span class="loading" style="margin: 3px auto"></span>',
            css: { padding: '5px', borderColor: '#2e6e9e' },
            overlayCSS:  { backgroundColor: '#fff' }
        },
        queue,
        _elements = {};

    biomart._state = {mart: null, datasets: []};

    biomart.renderer.renderInvalid = true;

    /*
     * PUBLIC 
     */
    self.setMart = function(mart, fn) {
        var martParam,
            resource;

        biomart._state.mart = martsHash[mart];

        if (!biomart.utils.hasGroupedMarts(biomart._state.mart)) {
            martParam = biomart._state.mart.name;
            resource = 'datasets';
        } else {
            martParam = $.map(biomart._state.mart, function(m) { return m.name });
            martParam = martParam.join(',');
            resource = 'datasets/mapped';
        }

        queryMart = biomart.query.createQueryMart(biomart._state.mart);

        if (!biomart.utils.hasGroupedMarts() && biomart._state.mart.operation == biomart.OPERATION.MULTI_SELECT) {
            multiselect = true;
        }

        independent = biomart._state.mart.independent === true;

        queue.queue(function() {
            biomart.resource.load(resource, function(json) {
                var mart;

                self.setDatasets(biomart._state.mart, json);

                queue.dequeue();
            }, {mart: martParam});
        });

        queue.queue(function() {
            if (biomart._state.datasets.length === 0) {
                return; 
            }

            // Don't allow partial attribute lists to be returned
            var params = { datasets: biomart._state.datasets, independent: independent, allowPartialList: false },
                mart;

            if (biomart._state.config) {
                params.config = biomart._state.config;
            }

            biomart.resource.load('attributes', function(json) {
                self.setAttributes(json);
                queue.dequeue();
            }, params);
        });

        queue.queue(function() {
            if (biomart._state.datasets.length === 0) {
                return; 
            }

            var params = {datasets: biomart._state.datasets};

            if (biomart._state.config)
                params.config = biomart._state.config;

            biomart.resource.load('filters', function(json) {
                self.setFilters(json);
                queue.dequeue();
            }, params);
        });

        _elements.content.one('queue.done', function() {
            resizeFilterLabels();
            if ($.isFunction(fn)) fn();
        });

        queue.dequeue();
    };

    self.setDatasets = function(mart, d) {
        var $li,
            arr = [],
            dsRegex = self.params.getDatasets(),
            isMapped = biomart.utils.hasGroupedMarts();

        if (dsRegex) dsRegex = new RegExp(dsRegex.replace(/,/g, '|'))

        _elements.datasetList.empty();

        if (!isMapped) {
            for (var i=0, ds; ds=d[i]; i++) {
                if (ds.isHidden) continue;

                ds.mart = mart;

                $li = $(['<li class="', ds.name, '">',
                            ds.displayName,
                        '</li>'].join(''))
                    .data('dataset', ds)
                    .addClass('ui-widget-content ui-corner-all ui-selectee')
                    .appendTo(_elements.datasetList);

                if (dsRegex) {
                    if (dsRegex.test(ds.name)) {
                        $li.addClass('ui-selected');
                        arr.push(ds.name);
                    }
                } else {
                    if (i===0) {
                        $li.addClass('ui-selected');
                        arr.push(ds.name);
                    }
                }
                dsHash[ds.name] = ds;
            }
        } else {
            // For keep tracking of first dataset among all marts
            var num = 0;
            
            for (var k in d) {
                var datasets = d[k],
                    mart;

                for (var i=0, ds; ds=datasets[i]; i++) {
                    if (ds.isHidden) continue;

                    ds.mart = martsHash[k];

                    $li = $(['<li class="', ds.name, '">',
                                ds.displayName,
                            '</li>'].join(''))
                        .data('dataset', ds)
                        .addClass('ui-widget-content ui-corner-all ui-selectee')
                        .appendTo(_elements.datasetList);

                    if (dsRegex) {
                        if (dsRegex.test(ds.name)) {
                            $li.addClass('ui-selected');
                            arr.push(ds.name);
                        }
                    } else {
                        if (num++ === 0) {
                            $li.addClass('ui-selected');
                            arr.push(ds.name);
                        }
                    }
                    dsHash[ds.name] = ds;
                }
            }
        }

        biomart._state.datasets = arr.join(',');

        if (_elements.datasetList.hasClass('ui-selectable')) {
            _elements.datasetList.selectable('destroy');
        } else {
            _elements.datasetList.singleselect('destroy');
        }
        
        if (multiselect) {
            _elements.datasetList.data({multiple: true});
            _elements.datasets.find('p.info').show();
            _elements.datasetList
                .selectable({
                    start: function(ev, ui) {
                        $("input.ui-autocomplete-input").blur(); // Hack to blur autocomplete inputs
                        $(this).data('prev', $(this).children('.ui-selected'));
                    },
                    stop: function() {
                        if (!$(this).children('.ui-selected').length) {
                            $(this).data('prev').addClass('ui-selected');
                        } else {
                            updateDatasets();
                        }
                        $(this).data('prev', null);
                    }
                })
                .disableSelection();
        } else {
            _elements.datasets.find('p.info').hide();
            _elements.datasetList.singleselect({ selected: updateDatasets });
        }

        queryMart.datasets = biomart._state.datasets;

        (function() {
            var mart;
            if (biomart.utils.hasGroupedMarts()) {
                mart = dsHash[biomart._state.datasets].mart;
            } else {
                mart = biomart._state.mart;
            }

            if (mart.config) 
                biomart._state.config = mart.config;
        })();
    };

    self.setAttributes = function(json) {
        var $node,
            hash = {},
            a,
            $list = _elements.attributes.find('ul.items').empty(),
            nodes = {},
            userSet = self.params.getAttr(),
            // First attribute that allows user to select
            // Selectable when attrribute list exists and not empty
            firstSelectable = !!userSet;

        if (independent) {
            for (var i=0, ds; ds=json.datasets[i]; i++) {
                iterate(ds.attributes, ds.name);
            }
        } else {
            iterate(json);
        }

        for (var k in nodes) {
            var curr = nodes[k];
            $node = curr.element;
            a = curr.item;

            if (!independent && a.attributes.length && !firstSelectable) {
                $node.addClass('ui-active').find('.checkbox').attr('checked', true);
                firstSelectable = a;
            } else if (!a.attributes.length) {
                $node
                    .append(biomart.getBlockElement('This option is unavailable'))
                    .find(':input').attr('tabIndex', -1);
            } else if (userSet && $.inArray(a.name, userSet) != -1) {
                $node.addClass('ui-active').find('.checkbox').attr('checked', true);
                firstSelectable = a;
            }

            $list.append($node);
            hash[a.name] = a;
        }

        if (firstSelectable) {
            _elements.attributes.show();
            hash = {};
            hash[firstSelectable.name] = firstSelectable;
        } else {
            _elements.attributes.hide();
        }

        queryMart.attributes = hash;
        
        function iterate(attributes, dataset) {
            for (var i=0, a; a=attributes[i]; i++) {
                if (a.isHidden) continue;

                if (nodes[a.name]) {
                    if (a.attributes.length) {
                        if (dataset) {
                            if(!nodes[a.name].item.datasets) nodes[a.name].item.datasets = [];
                            nodes[a.name].item.datasets.push(dataset);
                        }
                         nodes[a.name].item.attributes = a.attributes;
                    }
                } else {
                    if (dataset && a.attributes.length) {
                        a.datasets = [dataset]; 
                    }
                    $node = biomart.renderer.attribute('li', a, false);
                    if (a.independent !== true) $node.simplerattribute({radio:true});

                    nodes[a.name] = {
                        element: $node,
                        item: a
                    };
                }
            } // for
        } // iterate
    };

    // @param filters: union of all filters
    // @params display: intersection of filters to display (a bit of a hack)
    self.setFilters = function(filters) {
        var n = filters.length,
            $node,
            $list = _elements.filters.find('ul.items').empty(),
            tmp = {},
            userSet = self.params.getFilters();

        for (var i=0, f; f=filters[i]; i++) {
            if (!f.isHidden) {
                $node = biomart.renderer.filter('li', f);
                if (!$node) continue;

                if (userSet[f.name] && !biomart.utils.filterValueExists(f, userSet[f.name])) {
                    delete userSet[f.name];
                }

                $node.simplerfilter({ defaultValue: userSet[f.name] || '' });

                if ($node.children('div.invalid').length) {
                    biomart.disableFilter($node);
                }

                $list.append($node);
                filtersCache[f.name] = f;

                if (userSet[f.name]) {
                    setQueryFilter(f.name, userSet[f.name]);
                    $node.addClass('ui-active');
                }
            }
        }
    };

    self.init = function() {
        var datasets,
            processor,
            attributes,
            filters;

        // Elements
        _elements.wrapper = $('#biomart-top-wrapper');
        _elements.header = $('#biomart-header');
        _elements.topnav = $('#biomart-topnav');
        _elements.footer = $('#biomart-footer');
        _elements.heading = _elements.wrapper.find('h2');
        _elements.content = $('#biomart-content');
        _elements.nav = $('#biomart-nav');
        _elements.help = $('#biomart-help');
        _elements.explain = $('#biomart-explain');
        _elements.submit = $('#biomart-submit');
        _elements.results = $('#biomart-results').resultsPanel();
        _elements.datasets = _elements.content.find('div.datasets');
        _elements.datasetList = $('#biomart-datasets');
        _elements.attributes = _elements.content.find('div.attributes');
        _elements.filters = _elements.content.find('div.filters');
        _elements.configure = $('#biomart-configure');
        _elements.exportForm = $('#biomart-export-form');
        _elements.views;
        _elements.meta = _elements.heading.find('div.meta');

        biomart.getHomeLinks().click(function() { self.params.setPreview(false); return false });

        queue = new biomart.Queue(_elements.content, 'martform');

        _elements.heading
            .delegate('a.help', 'click.martform', function() {
                _elements.help.dialog({
                    width: 500,
                    autoShow: false,
                    close: function() { $(this).dialog('destroy') },
                    buttons: {
                        "OK": function() { $(this).dialog('close') }
                    }
                });
            return false;
        });

        biomart.resource.load('gui', function(json) {
            var active,
                displayName;

            _elements.nav.hide();

            _elements.views = biomart.makeMartSelect(json.marts, {
                selected: self.params.getMart(),
                each: function(mart) { 
                    if (biomart.utils.hasGroupedMarts(mart)) {
                        martsHash[mart[0].group] = mart;
                        for (var i=0, m; m=mart[i]; i++) {
                            martsHash[m.name] = m;
                        }
                    } else {
                        martsHash[mart.name] = mart;
                    }
                }
            }).appendTo(_elements.meta);

            active = _elements.views.children('option:selected');
            if (!active.length) active = _elements.views.children('option').first();

            mart = active.data('mart');

            if (biomart.utils.hasGroupedMarts(mart)) {
                displayName = mart[0].group;
            } else {
                displayName = mart.displayName;
            }

            _elements.views
                .prettybox()
                .siblings('.ui-prettybox')
                    .children('input')
                        .attr('size', displayName.length + 10)
                    .end()
                .end()
                .change(function(ev) {
                    var $select = $(ev.target),
                        $this = $select.children('option:selected');

                    if (!$this.is('option')) return;

                    var m = $this.data('mart'),
                        displayName;

                    if (biomart.utils.hasGroupedMarts(m)) {
                        displayName = m[0].group;
                    } else {
                        displayName = m.displayName;
                    }

                    $select.siblings('.ui-prettybox').children('input').attr('size', displayName.length + 10);
                    
                    self.params.setMart(biomart.utils.hasGroupedMarts(m) ? m[0].group : m.name);
                });

            self.params.setMart(biomart.utils.hasGroupedMarts(mart) ? mart[0].group : mart.name);
            self.setMart(biomart.utils.hasGroupedMarts(mart) ? mart[0].group : mart.name, function() {
                if (self.params.isPreview()) self.getResults();
                else $.publish('biomart.ready');
            });
        }, {name: self.params.getGuiContainer()});

        // Attach filters listeners
        attachFilterListeners();

        _elements.explain.delegate('p.actions a', 'click.martform', function() {
            var xml = prepareXML('TSV', QUERY_LIMIT, true, ''),
                $this = $(this);

            if ($this.hasClass('plain')) {
                xml = xml.replace(/"/g, '\\\"');
            }

            $this.addClass('ui-active').siblings().removeClass('ui-active');

            _elements.explain.find('textarea').val(xml);

            return false;
        });

        _elements.heading.delegate('a.explain', 'click.martform', function() {
            _elements.explain
                .find('textarea')
                    .height(255)
                    .val(prepareXML('TSV', QUERY_LIMIT, true, ''))
                .end()
                .dialog({
                width: 700,
                height: 400,
                autoShow: false,
                close: function() { $(this).dialog('destroy') },
                buttons: {
                    "OK": function() { $(this).dialog('close')}
                },
                resize: function(ev, ui) {
                    var $this = $(this);
                    $this.find('textarea').height($this.height() - 26);
                }
            });
            return false;
        });

        _elements.results
            .bind('hide', function() {
                _elements.heading.slideDown({
                    duration: ANIMATION_TIME/2
                });
                _elements.wrapper.removeClass('wide', ANIMATION_TIME/2, function() {
                    _elements.configure.slideDown({
                        duration: ANIMATION_TIME
                    });
                });
            })
            .bind('show', function() {
                $.publish('biomart.ready');
            })
            .bind('edit', function() {
                self.params.setPreview(false);
                self.params.setParam('page', null);
                self.params.setParam('order', null);
                self.params.setParam('col', null);
            });

        $('#biomart-submit').delegate('button', 'click', function() {
            var invalid = biomart.checkRequiredFilters(_elements.filters.find('.filter-container'));

            if (!invalid) {
                self.params.setPreview(true);
            } else {
                var element = invalid;
                setTimeout(function() { element.fadeAndRemove() }, 3000);
            }
        });

        // Handles selecting/unselecting attributes
        _elements.attributes.click(function(ev) {
            var $origin = $(ev.target),
                $li = $origin.closest('li'),
                attribute = $li.data('item');

            // Make sure origin is from checkbox
            if ($origin.is('input.checkbox')) {
                if ($origin.is('[type=checkbox]')) { // Independent means more than one may be selected
                    if ($origin[0].checked) {
                        $li.addClass('ui-active');
                        setQueryAttribute(attribute);
                    } else {
                        $li.removeClass('ui-active');
                        removeQueryAttribute(attribute);
                    }
                } else { // Non-independent means only one may be selected
                    self.params.setAttr(attribute.name);
                    $li.addClass('ui-active')
                        .siblings().removeClass('ui-active');
                    queryMart.attributes = [attribute];
                }
            }
        }); // attributes.click
    };

    self.ready = function() {
        $('#biomart-loading').animate({opacity: 'hide'}, ANIMATION_TIME);
    };

    // Update functions
    self.updateMart = function (m) {
        var displayName;

        m = martsHash[m];

        if (biomart.utils.hasGroupedMarts(m)) {
            displayName = m[0].group;
        } else {
            displayName = m.displayName;
        }

        _elements.content.block(blockOptions);
        _elements.views.siblings('.ui-prettybox').children('input')
            .val(displayName)
            .attr('size', displayName.length + 5);

        mart = m;

        self.setMart(biomart.utils.hasGroupedMarts(m) ? m[0].group : m.name, function() {
            _elements.content.unblock();
            if (_elements.configure.is(':hidden')) self.setPreview(false);
        });
    };

    self.updateDatasets = function(ds) {
        var arr = ds.split(','),
            classes = '.' + ds.replace(/,/g, ',.');
        _elements.datasetList.children('.ui-selectee').removeClass('ui-selected').filter(classes).addClass('ui-selected');
        setQueryDatasets(ds);
    };

    self.updatePage = function(i) {
        _elements.data.queryResults('page', i);
    };

    self.updateSort = function(sort) {
        if (sort) _elements.data.queryResults('sort', sort[0]=='asc', sort[1]);
        else _elements.data.queryResults('removeSort');
    };

    self.restart = function () {
        self.params.setPreview(false);

        _elements.content.find('span.loading').remove();

        _elements.meta.children('select:not([multiple])').each(function() {
            var $this = $(this),
                $box = $this.siblings('.ui-prettybox')
                value = $this.children('option:selected').text() || '-- Choose --';
            $box.children('.ui-autocomplete-input').val(value);
        });
        _elements.filters.find('ul.items li').simplerfilter('refresh');

        _elements.submit.removeClass('hidden');
        _elements.results.resultsPanel('edit');
    };

    self.getResults = function() {
        _elements.configure.slideUp({
            duration: ANIMATION_TIME,
            complete: function() {
            	// Load settings from metainfo if not loaded already
                if (!config) {
                    var meta = new biomart.utils.MetaInfo(mart);
                    config = configs[mart.name] = meta._globalConfig;
                }
                if (config.limit == "none") {
                	QUERY_LIMIT = -1;
                }
                
                var options = {
                        queries: prepareXML('TSVX', QUERY_LIMIT, true, queryClient, independent),
                        downloadXml: prepareXML('TSV', -1, true, queryClient),
                        martObj: queryMart,
                        independentQuery: independent,
                        onPageChange: function(i) {
                            if (i != 1) self.params.setPage(i);
                        },
                        onSort: function(asc, index /*column*/) {
                            self.params.setSort(asc, index);
                        },
                        page: self.params.getPage()
                    },
                    sort = self.params.getSort(),
                    config = configs[mart.name],
                    datasets = [];
                    
                
                options.displayType = config.rendering;
                options.dataAggregation = config.aggregation;
                options.displayOptions = config.options;
                
                var colTypes = [];
                for(var name in queryMart.attributes){
                	colTypes.push(queryMart.attributes[name].dataType);
                }
                options.colDataTypes = colTypes;

                if (sort) options.sort = {order: sort[0], col: sort[1]};

                _elements.heading.slideUp({
                    duration: ANIMATION_TIME/2
                });
                _elements.datasetList.children('.ui-selected').each(function() { 
                    datasets.push($(this).data('dataset').displayName);
                });
                var title = biomart.utils.hasGroupedMarts(mart) ? mart[0].group : mart.displayName;
                title = [title, ' &raquo; ', datasets.join(', ')].join('');

                _elements.submit.addClass('hidden');
                _elements.results.resultsPanel('run', title, options);
            }
        });
    };

    /*
     * PRIVATE
     */
    function setQueryDatasets(datasets) {
        biomart._state.datasets = queryMart.datasets = datasets;
        queryMart.filters = {};
    }
    function setQueryFilter(filterName, value) {
        self.params.setParam(filterName, value);
        var filterObj = filtersCache[filterName];
        filterObj.value = value;
        queryMart.filters[filterName] = filterObj;
    }
    function removeQueryFilter(filterName) {
        self.params.setParam(filterName, null);
        delete queryMart.filters[filterName];
    }
    function setQueryAttribute(attribute) {
        self.params.addAttr(attribute.name);
        queryMart.attributes[attribute.name] = attribute;
    }
    function removeQueryAttribute(attribute) {
        self.params.removeAttr(attribute.name);
        delete queryMart.attributes[attribute.name];
    }

    function attachFilterListeners() {
        _elements.filters
            .delegate('li.filter-container', 'removefilter', function(ev) {
                 biomart.clearFilter($(this), function(item) {
                    removeQueryFilter(item.name);
                 });
            })
            .delegate('li.filter-container', 'addfilter', function(ev) {
                var $li = $(this),
                    $box = $li.children('input.checkbox'),
                    name = $li.attr('filter-name'),
                    value = biomart.validator.filter($li),
                    valid = false;

                if ($.isArray(value)) valid = !!value[0] && !!value[1];
                else valid = !!value;
                
                if (valid) {
                    $li.addClass('ui-active');
                    setQueryFilter(name, value);
                } else {
                    $li.removeClass('ui-active');
                    removeQueryFilter(name);
                }
            });
    } // attachFilterListeners

    function blockElements() {
        _elements.filters.children('.content').block(blockOptions);
        _elements.attributes.children('.content').block(blockOptions);
    }

    function unblockElements() {
        _elements.filters.children('.content').unblock();
        _elements.attributes.children('.content').unblock();
    }

    function resizeFilterLabels() {
        var max = -Infinity,
            arr = [],
            n = 0;

        _elements.filters.find('li.filter-container').each(function() {
            var label = $(this).children('.item-name'),
                w = label.width();
            max = Math.max(max, w);
            arr.push(label);
            n++;
        });

        while (n--) {
             arr[n].width(max+10);
        }
    }

    function getElements(datasets) {
        if ($.isArray(datasets)) datasets = datasets.join(',');
        blockElements();

        _elements.content.one('queue.done', function() {
            resizeFilterLabels();
            unblockElements();
        });

        // Independent querying means we need to get attributes for each dataset 
        queue.queue(function() {
            // Don't allow partial attribute lists to be returned
            var params = { datasets: biomart._state.datasets, independent: independent, allowPartialList: false };

            if (biomart._state.config)
                params.config = biomart._state.config;

            biomart.resource.load('attributes', function(json) {
                self.setAttributes(json);
                queue.dequeue();
            }, params);
        });

        queue.queue(function() {
            var params = {datasets: biomart._state.datasets};

            if (biomart._state.config)
                params.config = biomart._state.config;

            biomart.resource.load('filters', function(json) {
                self.setFilters(json);
                queue.dequeue();
            }, params);
        });

        queue.dequeue();
    }

    function updateDatasets() {
        var arr = [],
            ds;
        _elements.datasetList.find('.ui-selected').each(function(){
            var ds = $(this).data('dataset');
            arr.push(ds.name);
        });
        ds = arr.join(',');
        self.params.setDatasets(ds);
        getElements(ds);

        (function() {
            var mart;
            if (biomart.utils.hasGroupedMarts()) {
                mart = dsHash[ds].mart;
            } else {
                mart = biomart._state.mart;
            }

            if (mart.config) 
                biomart._state.config = mart.config;
        })();
    }
    
    
    function prepareXML(processor, QUERY_LIMIT, header, client, perDataset) {
        queryMart.config = biomart._state.config;

        if (!perDataset) {
             return biomart.query.compile('XML', queryMart, processor, QUERY_LIMIT, header, client);
        }

        // for independent queries (i.e. one per dataset) we need to make an array that has the query and the heading
        var arr = [],
            datasets = queryMart.datasets,
            temp,
            xml,
            attr,
            queries = [],
            header;

        for (var i=0, ds; ds=datasets[i]; i++) {
            for (var k in queryMart.attributes) {
                if ($.inArray(ds, queryMart.attributes[k].datasets) != -1) {
                    temp = $.extend({}, queryMart);
                    temp.datasets = [ds];
                    attr = {};
                    attr[k] = temp.attributes[k];
                    temp.attributes = attr;
                    queries.push({
                        query: biomart.query.compile('XML', temp, processor, QUERY_LIMIT, header, queryClient),
                        attr: queryMart.attributes[k]
                    });
                }
            }
        }

        return queries;
    }
});

$.subscribe('biomart.init', biomart.martform, 'init');
$.subscribe('biomart.ready', biomart.martform, 'ready');
$.subscribe('biomart.changed.mart', biomart.martform, 'updateMart');
$.subscribe('biomart.changed.datasets', biomart.martform, 'updateDatasets');
$.subscribe('biomart.changed.page', biomart.martform, 'updatePage');
$.subscribe('biomart.changed.sort', biomart.martform, 'updateSort');
$.subscribe('biomart.preview', biomart.martform, 'getResults');
$.subscribe('biomart.edit', biomart.martform, 'restart');
})(jQuery);
