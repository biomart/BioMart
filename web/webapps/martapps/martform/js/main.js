(function($) {
$.namespace('biomart.martform', function(self) {
    var APP_NAME = 'MartForm',
        ANIMATION_TIME = 100,
        QUERY_CLIENT = 'webbrowser',
        QUERY_LIMIT = 1000,
        QUERY_RESULTS_OPTIONS = {
            paginateBy: 20,
            showProgress: true,
            animationTime: ANIMATION_TIME,
            showLoading: false,
            sorting: true,
            footer: true,
            iframe: true
        },

        MESSAGES = {
            select_dataset: _('Please select a dataset'),
            select_datasets: _('Please select datasets')
        },

        BLOCK_OPTIONS = {
            message: '<span class="loading" style="margin: 3px auto"></span>',
            css: { padding: '5px', borderColor: '#2e6e9e' },
            overlayCSS:  { backgroundColor: '#fff' }
        },

        _elements = {},
        _urlHash = {},
        _data = {},

        // Helper functions (populated later in this file)
        _init = {},
        _updater = {},
        _loader = {},
        _util = {};

    biomart._state = { initialized: false };

    self.init = function() {
        biomart.renderer.renderInvalid = true;
        _init.elements();
        _init.params();
        _init.state();
        _init.data();
        _init.events();
        _loader.marts();
    };

    self.ready = function() {
        // Trigger load datasets
        _elements.field.datasets.trigger('change.martform');
        $('#biomart-loading').fadeAndRemove();
        biomart._state.initialized = true;
    };

    // Initializers
    _init.elements = function() {
        _elements.wrapper = $('#biomart-wrapper');
        _elements.header = _elements.wrapper.children('header');
        _elements.h2 = _elements.header.children('h2');
        _elements.content = $('#biomart-content');
        _elements.section = {
            datasets: $('#biomart-datasets'),
            filters: $('#biomart-filters'),
            attributes: $('#biomart-attributes')
        };
        _elements.resultsWrapper = $('#biomart-results-wrapper');
        _elements.results = $('#biomart-results')
            .resultsPanel()
            .bind('edit', function() {
                _elements.results.resultsPanel('edit');
                _elements.submit.removeClass('hidden');
                _elements.header.slideDown('fast');
                _elements.content.slideDown({
                    duration: 'fast'
                });
            });
        _elements.submit = $('#biomart-submit');
        _elements.field = {
            marts: $('#field-marts'),
            datasets: $('#field-datasets')
        };
        _elements.filterContainers = _elements.section.filters.find('.containers');
        _elements.filterMessage = _elements.section.filters.find('.message');
        _elements.attributeContainers = _elements.section.attributes.find('.containers');
        _elements.attributeMessage = _elements.section.attributes.find('.message');
        _elements.resultsMessage = _elements.resultsWrapper.find('.select-attributes');
        _elements.errorMessage = $('#error-message')
    };
    _init.params = function() {
        // Grab relevant info from URL fragment
        var fragment = biomart.url.jsonify(location.href).fragment;
        if (fragment) {
            _urlHash = biomart.url.jsonify(fragment);
        }
    };
    _init.state = function() {
        var arr = _urlHash.path.split('/');
        biomart._state.root = arr[0] || null;
        biomart._state.martName = arr[1] || null;
        biomart._state.attributes = {};
        biomart._state.filters = {};
    };
    _init.data = function() {
        _data = {
            martHash: {}
        };
    };
    _init.events = function() {
        // Mart select
        _elements.field.marts.bind('change.martform', function(ev) {
            var selected = $(this).children(':selected'),
                item = selected.data('item');

            // check for grouping
            if (item) {
                biomart._state.mart = item;
            } else {
                biomart._state.mart = selected.data('item');
            }

            _updater.mart();
            _loader.datasets();
        });
        // Dataset select
        _elements.field.datasets.bind('change.martform', function(ev) {
            var selected = $(this).children(':selected'),
                arr = [];
            for (var i=0, ds; i<selected.length; i++) {
                ds = selected.eq(i).data('item');
                arr.push(ds.name);
                biomart._state.martObj = ds.mart;
            }
            biomart._state.datasets = arr.join(',');
            if (biomart._state.initialized) {
                _util.clearFiltersAndAttributes();
                _util.clearResults();
            }
            biomart._state.numAttributes = 0;
            if (biomart._state.datasets) {
                _updater.datasets();
                _loader.containers();
            }
        });
        // Filter select
        _elements.section.filters.delegate('.filter-container', 'addfilter', function(ev, item, value) {
            var $this = $(this),
                value = biomart.validator.filter($this);
            if (value && (!$.isArray(value) || value[0])) {
                $this.addClass('ui-active').data('value', value);
            } else {
                $this.removeClass('ui-active').data('value', null);
            }
            item.value = value;
            biomart._state.filters[item.name] = item;
            _updater.filters();
        });
        _elements.section.filters.delegate('.filter-container', 'removefilter', function(ev, item) {
            biomart.clearFilter($(this).removeClass('ui-active').data('value', null));
            item.value = null;
            delete biomart._state.filters[item.name];
            _updater.filters();
        });
        // Attribute select
        _elements.section.attributes.delegate('.attribute-container', 'addattribute', function() {
            var item = $(this).addClass('ui-active').data('item');
            biomart._state.attributes[item.name] = item;
            if (!biomart._state.numAttributes++ || biomart._state.resultsLoaded) {
                _util.toggleResults();
            }
            _updater.attributes();
        });
        _elements.section.attributes.delegate('.attribute-container', 'removeattribute', function() {
            var item = $(this).removeClass('ui-active').data('item');
            delete biomart._state.attributes[item.name];
            if (!--biomart._state.numAttributes && !biomart._state.resultsLoaded) {
                _util.clearResults();
            } else if (biomart._state.resultsLoaded) {
                _util.toggleResults();
            }
            _updater.attributes();
        });
        // Results
        _elements.submit
            .delegate('button', 'click.martform', function() {
                if (!biomart._state.numAttributes) {
                    _elements.resultsMessage.fadeIn(100);
                } else {
                    _loader.results();
                }
                return false;
            })
    };

    // Updaters (for URL query params)
    _updater.mart = function() {
        var params = $.deparam(_urlHash.query),
            martPart;

        if (!biomart.utils.hasGroupedMarts()) {
            martPart = biomart._state.mart.name;
        } else {
            martPart = biomart._state.mart[0].group;
        }

        if (biomart._state.initialized) {
            delete params.datasets;
        }

        _urlHash.path = [biomart._state.root, martPart].join('/');
        _urlHash.query = $.param(params);

        updateUrlHash();
    };
    _updater.datasets = function() {
        var params = $.deparam(_urlHash.query);
        params.datasets = biomart._state.datasets;
        _urlHash.query = $.param(params);
        updateUrlHash();
    };
    _updater.filters = function() {
        var params = $.deparam(_urlHash.query),
            filters = biomart._state.filters;
        // Clear out filters
        for (var k in params) {
            if ($.inArray(k, biomart.NON_FILTER_PARAMS) == -1) {
                delete params[k];
            }
        }
        for (var k in filters) {
            params[k] = filters[k].value;
        }
        _urlHash.query = $.param(params);
        updateUrlHash();
    };
    _updater.attributes = function() {
        var params = $.deparam(_urlHash.query),
            attributes = biomart._state.attributes,
            arr = [];
        for (var k in attributes) {
            arr.push(attributes[k].name);
        }
        if (arr.length) {
            params.attributes = arr.join(',');
        } else {
            delete params.attributes;
        }
        _urlHash.query = $.param(params);
        updateUrlHash();
    };

    // Loaders
    _loader.marts = function () {
        biomart.resource.load('gui', function(json) {
            biomart._state.martGroupMap = {}; // for grouping

            _elements.h2.html(json.displayName);
            for (var i=0, mart; mart=json.marts[i]; i++) {
                if (!mart.isHidden) {
                    _data.martHash[mart.name] = mart;

                    if (!mart.group) {
                        $(['<option value="', mart.name, '"', mart.name==biomart._state.martName ? ' selected' : '', '>', mart.displayName, '</option>'].join(''))
                            .data('item', mart)
                            .appendTo(_elements.field.marts);
                    } else {
                        var el = biomart._state.martGroupMap[mart.group];
                        if (!el) {
                            biomart._state.martGroupMap[mart.group] = $(['<option value="', mart.name, '"', 
                                        biomart._state.martName == mart.group ? ' selected' : '', '>', mart.group, '</option>'].join(''))
                                    .data('item', [mart])
                                    .data('group', mart.group)
                                    .appendTo(_elements.field.marts);
                        } else {
                            var value = el.val(),
                                items = el.data('item');
                            el.val(value + ',' + mart.name);
                            items.push(mart);
                            el.data('item', items);
                        }
                    }
                }
            }
            _elements.field.marts.prettybox();
            _elements.field.marts.trigger('change.martform');
        }, {name: biomart._state.root});
    };
    _loader.datasets = function(callback) {
        var martParam,
            resource;

        if (!biomart.utils.hasGroupedMarts()) {
            martParam = biomart._state.mart.name;
            resource = 'datasets';
        } else {
            martParam = $.map(biomart._state.mart, function(m) { return m.name });
            martParam = martParam.join(',');
            resource = 'datasets/mapped';
        }

        biomart.resource.load(/*datasets*/resource, function(json) {
            _elements.field.datasets.empty();
            var multiple = false,
                selectedDatasets = null,
                params,
                isMapped = biomart.utils.hasGroupedMarts();

            if (!isMapped) {
                multiple = _data.martHash[biomart._state.mart.name].operation == biomart.OPERATION.MULTI_SELECT;
            }

            // Grab the selected datasets from URL parameter
            if (_urlHash.query) {
                var params = $.deparam(_urlHash.query);
                if (params.datasets) {
                    selectedDatasets = params.datasets.split(',');
                }
            } else {
                if (!isMapped) {
                    selectedDatasets = [json[0].name];
                } else {
                    for (var k in json) {
                        selectedDatasets = json[k][0].name;
                        break;
                    }
                }
            }

            if (!isMapped) {
                for (var i=0, ds; ds=json[i]; i++) {
                    ds.mart = biomart._state.mart;
                    $(['<option value="', ds.name, '">', ds.displayName, '</option>'].join(''))
                        .data('item', ds)
                        .appendTo(_elements.field.datasets);
                }
            } else {
                for (var k in json) {
                    var datasets = json[k],
                        mart;
                    for (var i=0, curr; curr=biomart._state.mart[i]; i++) {
                        if (curr.name == k) {
                            mart = curr;
                            break;
                        }
                    }
                    for (i=0, ds; ds=datasets[i]; i++) {
                        ds.mart = mart;
                        $(['<option value="', ds.name, '">', ds.displayName, '</option>'].join(''))
                            .data('item', ds)
                            .appendTo(_elements.field.datasets);
                    }
                }
            }

            // Clean up prettybox if initialized
            if (_elements.field.datasets.hasClass('ui-prettybox')) {
                _elements.field.datasets.prettybox('destroy');
            }
            if (multiple) {
                _elements.field.datasets
                    .attr('size', Math.min(10, json.length))
                    .attr('multiple', true);
                if (selectedDatasets) {
                    _elements.field.datasets.val(selectedDatasets);
                }
            } else {
                if (selectedDatasets) {
                    _elements.field.datasets.val(selectedDatasets[0]);
                }
                _elements.field.datasets
                    .attr('multiple', false)
                    .prettybox();
            }
            if (callback) callback();
            $.publish('biomart.ready');
        }, {mart: martParam});
    };
    _loader.containers = function() {
        var reqParams = {datasets: biomart._state.datasets};

        if (!biomart.utils.hasGroupedMarts()) {
            if (biomart._state.mart.config) {
                reqParams.config = biomart._state.mart.config;
            }
        } else {
            if (biomart._state.martObj.config) {
                reqParams.config = biomart._state.martObj.config;
            }
        }

        _elements.filterMessage.hide();
        _elements.attributeMessage.hide();
        _elements.errorMessage.hide();
        _elements.section.filters.block(BLOCK_OPTIONS);
        _elements.section.attributes.block(BLOCK_OPTIONS);

        biomart.resource.load('containers', function(json) {
            var containers = json.containers
                filterContainers = _elements.filterContainers.empty(),
                params = $.deparam(_urlHash.query),
                selectedFilters = {};

            for (var k in params) {
                if ($.inArray(k, biomart.NON_FILTER_PARAMS) == -1) {
                    selectedFilters[k] = params[k];
                }
            }

            for (var i=0, container; container=containers[i]; i++) {
                biomart.renderer.container({
                    tagName:'div', 
                    selectedFilters: selectedFilters,
                    headerTagName: 'h4',
                    item: container, 
                    mode: biomart.renderer.FILTERS,
                    appendTo: filterContainers
                });
            }

            _elements.section.filters.unblock();
        }, $.extend({withfilters: true, withattributes: false}, reqParams));

        biomart.resource.load('containers', function(json) {
            var containers = json.containers
                attributeContainers = _elements.attributeContainers.empty(),
                selectedAttributes = $.deparam(_urlHash.query).attributes;

            if (selectedAttributes) {
                selectedAttributes = selectedAttributes.split(',');
            } else {
                selectedAttributes = [];
            }

            for (var i=0, container; container=containers[i]; i++) {
                biomart.renderer.container({
                    tagName:'div', 
                    headerTagName: 'h4',
                    item: container, 
                    mode: biomart.renderer.ATTRIBUTES,
                    onAttributeSelect: function(attribute) {
                        biomart._state.attributes[attribute.name] = attribute;
                        biomart._state.numAttributes++;
                    },
                    selectedAttributes: selectedAttributes,
                    appendTo: attributeContainers,
                    extras: function(item, element) {
                        if (item.maxContainers) {
                            element
                                .bind('containershow', function(ev, panel) {
                                    var n = 0;
                                    panel.find('.attribute-container.ui-active').each(function() {
                                        var item = $(this).data('item');
                                        biomart._state.attributes[item.name] = item;
                                        n++;
                                    });
                                    var list = panel.find('.attribute-container.ui-active'),
                                        n = list.length;
                                    biomart._state.numAttributes += n;
                                    if (!biomart._state.numAttributes) {
                                        _util.toggleResults();
                                    }
                                })
                                .bind('containerhide', function(ev, panel) {
                                    var n = 0;
                                    panel.find('.attribute-container.ui-active').each(function() {
                                        var item = $(this).data('item');
                                        delete biomart._state.attributes[item.name];
                                        n++;
                                    });
                                    biomart._state.numAttributes -= n;
                                    if (!biomart._state.numAttributes && !biomart._state.resultsLoaded) {
                                        _util.clearResults();
                                    } else {
                                        _util.toggleResults();
                                    }
                                });
                        }
                    }
                });
            }

            _elements.section.attributes.unblock();
        }, $.extend({withfilters: false, withattributes: true}, reqParams));
    };
    _loader.results = function() {
        var invalid = biomart.checkRequiredFilters(_elements.filterContainers.find('.filter-container'));

        if (invalid) {
            var element = invalid;
            setTimeout(function() { element.fadeAndRemove() }, 3000);
            return;
        }

        var mart = getQueryMart(),
            xml = biomart.query.compile('XML', mart, 'TSVX', QUERY_LIMIT, QUERY_CLIENT),
            downloadXml = biomart.query.compile('XML', mart, 'TSV', -1, QUERY_CLIENT),
            item = _elements.field.marts.children(':selected').eq(0).data('item'),
            title;

        if (item) {
            title = item.displayName
        } else {
            title = _elements.field.marts.children(':selected').eq(0).data('group');
        }
        
        var colTypes = [];
        for(var name in mart.attributes){
        	colTypes.push(mart.attributes[name].type);
        }
        
        console.log(colTypes);
        
        _elements.submit.addClass('hidden');
        _elements.header.slideUp('fast');
        _elements.content.slideUp({
            duration: 'fast',
            complete: function() {
                _elements.results.resultsPanel('run', 
                    title,
                    $.extend({
                        queries: xml,
                        downloadXml: downloadXml,
                        martObj: mart,
                        dataAggregation: 'none',
                        displayType: 'table',
                        colDataTypes: colTypes
                    }, QUERY_RESULTS_OPTIONS));
            }
        });

        biomart._state.resultsLoaded = true;
    };

    // Utility functions
    _util.clearFiltersAndAttributes = function() {
        var multiple = false;
        if (!biomart.utils.hasGroupedMarts()) {
            multiple = _data.martHash[biomart._state.mart.name].operation == biomart.OPERATION.MULTI_SELECT;
        }
        _elements.filterContainers.add(_elements.attributeContainers).empty();
        _elements.filterMessage.add(_elements.attributeMessage)
            .html(multiple ? MESSAGES.select_datasets : MESSAGES.select_dataset)
            .fadeIn();
        biomart._state.filters = {};
        biomart._state.attributes = {};
        _updater.filters();
        _updater.attributes();
    };
    _util.clearResults = function() {
        biomart._state.resultsLoaded = false;
        _elements.submit.removeClass('hidden');
    };
    _util.toggleResults = function() {
        _elements.resultsMessage.fadeOut({duration: 100});
    };

    function getQueryMart() {
        var mart = {datasets: [], attributes: {}, filters: {}},
            isMapped = biomart.utils.hasGroupedMarts(),
            martObj;

        if (!isMapped) {
            martObj = _elements.field.marts.children(':selected').eq(0).data('item');
        } else {
            martObj = biomart._state.martObj;
        }

        if (martObj.config) mart.config = martObj.config;

        // Grab selections from summary panel and generate XML
        _elements.field.datasets.children(':selected').each(function() {
            var item = $(this).data('item');
            if (item)
                mart.datasets.push($(this).data('item').name);
        });

        // Get a list of selected filters
        // Also make sure that required filters are selected
        var filterElements = _elements.filterContainers.find('.filter-container');
        for (var i=0, filterElement; i<filterElements.length; i++) {
                filterElement = filterElements.eq(i);
                isActive = filterElement.hasClass('ui-active'),
                item = filterElement.data('item'),
                value = filterElement.data('value'),
                name;

            if (item) {
                if (isActive) {
                    if ($.isArray(value)) {
                        name = value[0];
                        value = value[1];
                    } else {
                        name = item.name;
                    }
                    mart.filters[name] = {name: name, value: value};
                }
            }
        }
        
        for (var k in biomart._state.attributes) {
            var item = biomart._state.attributes[k];
            mart.attributes[item.name] = {name: item.name, type: item.dataType};
        }

        return mart;
    }

    function createContainer(item) {
        return $([
            '<div class="container ', item.name, ' clearfix gradient-grey-reverse">',
                '<h4>', item.displayName, '</h4>',
                '<ul class="items clearfix"/>',
            '</div>'
        ].join('')).data('item', item);
    }

    function updateUrlHash() {
        location.hash = biomart.url.SEPARATOR + biomart.url.stringify(_urlHash);
    }
});

$.subscribe('biomart.init', biomart.martform, 'init');
$.subscribe('biomart.ready', biomart.martform, 'ready');

})(jQuery);
