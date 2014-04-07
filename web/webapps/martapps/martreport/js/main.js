(function($) {
$.namespace('biomart.martreport', function(self) {
    // jQuery DOM Elements we need to manipulate
    var $header = $('#biomart-header'),
        $wrapper = $('#biomart-wrapper'),
        $content = $('#biomart-content'),
        $footer = $('#biomart-footer'),
        $sidenav = $('#biomart-sidenav'),
        $containers = $('#biomart-containers'),
        $filters = $('#biomart-common-filters'),
        $heading = $('h2'),
        $loading = $('#biomart-interim-loading'),

        // Flags and app settings
        appName = 'MartReport',
        queryClient = 'webbrowser',
        initialized = false,
        validFilters = false,
        animationTime = 200,
        delay = 500, // delay in processing subsequent requests

        QUERY_RESULTS_OPTIONS = {
            timeout: 60000,
            showProgress: false,
            animationTime: animationTime,
            iframe: false,
            showLoading: false,
            footer: false
        },

        // User defined values (via URL query params)
        urlHash,
        defaultQuery,
        params,
        reportName,
        userDatasets,
        userFilters,
        selectedMart,

        allFilters = {},

        // Helper variables
        marts = [],
        datasetMap = {},
        totalContainers = 0; // used to track progress of page load

    /*
     * =init
     */
    self.init = function() {
        urlHash = biomart.url.jsonify(location.href);
        params = biomart.url.simpleQueryParams(urlHash.query);

        $('#biomart-submit')
            .button({
                icons: {
                    primary: 'ui-icon-refresh'
                }
            })
            .bind('click.martreport', function() {
                submitForm();
            });

        if (params) {
            if (params.report) {
                reportName  = params.report;
                delete params.report;
            }
            if (params.datasets) {
                userDatasets = params.datasets.split(',');
                delete params.datasets;
            }
            if (params._q) {
                defaultQuery = params._q;
                delete params._q;
            }
        }

        // Retrieve user-defined filters
        userFilters = params;

        if (!reportName) {
            initialized = true;
            $.publish('biomart.initialized');
            return;
        }

        // Get root container
        biomart.resource.load('gui', function(json) {
            marts = biomart.utils.getMartsFromGuiContainer(json, appName.toLowerCase());

            // Loop through backwards and find the mart specified in URL
            for (var i=0; selectedMart=marts[i]; i++) {
                if (selectedMart.name == params.mart) {
                    break;
                }
            }

            // Mart was not found
            if (i == marts.length) {
                selectedMart = marts[0];
            }

            $wrapper.find('h2').text(selectedMart.displayName);

            // Get all datasets for this mart, then get containers
            biomart.resource.load('datasets', function(json) {
                var arr = [];
                for (var i=0, item; item=json[i]; i++) {
                    datasetMap[item.name] = item; // for use later
                    arr.push(item.name);
                }
                selectedMart.datasets = arr;
                getContainersFromMart(selectedMart, drawContainers);
            }, {config: selectedMart.name});
        }, {name: reportName});

        $sidenav
            .hoverIntent({
                over: function() {
                    $(this).stop(true, true).addClass('active hover', 500);
                },
                out: function() {
                    $(this).stop(true, true).removeClass('active hover', 500);
                },
                timeout: 500
            })
            .delegate('a', 'click.sidenav', function(ev) {
                var selector = $(this).attr('href'),
                    container = $(selector);
                if (!container.data('loaded')) {
                    $loading.slideDown();
                    $(selector).scrollTo(3);
                    $content.one('loaded.container', function() {
                        $(selector).scrollTo(3, function() { $loading.slideUp() });
                    });
                } else {
                    $(selector)
                        .children('h3').minimizer('show')
                        .end()
                        .scrollTo(3, function() { $loading.slideUp() });
                }
                return false;
            });
    };

    /*
     * =ready
     */
    self.ready = function() {
        validFilters = processUserFilters();

        // Draw any common filters
        for (var k in allFilters) {
            var rendered = biomart.renderer.filter('div', allFilters[k].filter)
            if (rendered) {
                rendered
                    .limitedfilter({value: allFilters[k].filter.value})
                    .appendTo($filters);
            } else {
                biomart.error(_('There was a problem rendering filter: ' + allFilters[k].filter.displayName));
            }
        }

        var sections = $sidenav.find('ol.sections');

        for (var i=0, m; m=marts[i]; i++) {
            if (!m.containers) continue;
            for (var j=0, c; c=m.containers[j]; j++) {
                var $div = newContainer($containers, m, c),
                    id = $div.attr('id');
                c.topLevel = true;
                $div.append(biomart.templates.loader.clone());
                sections.append([
                    '<li>',
                        '<a href="#', id, '">', c.displayName, '</a>',
                    '</li>'
                ].join(''));
            }
        }

        // Can remove loading screen now
        $('#biomart-loading').animate({opacity: 'hide'}, function() { $(this).remove() });

        self.generateReport();

        setTimeout(function() {
            if (!$sidenav.hasClass('hover')) $sidenav.stop(true, true).removeClass('active', 500);
        }, 3000);
    };


    // Prevent concurrent loading of containers
    self.lock = function() {
        self._loadInProgress = true;
    };
    self.unlock = function() {
        self._loadInProgress = false;
        // show all the donwload links after all container is loaded
        var $children = $containers.children('.container');
        for( var i=0, container; i< $children.length; i++){
        	container = $children.eq(i);
	        for( var j=0, dl; j<container.find('.report-download').length; j++){
	        	dl = container.find('.report-download').eq(j);
	        	dl.parent().show('slow');
	        }
        }
    };

    /*
     * =generateReport
     */
    self.generateReport = function() {
        if (!initialized) return;

        var k,
            filter,
            userFilters = [],
            name, displayName, value;

        if (!validFilters) {
            $('input.field.text:first').focus();
            return;
        }

        $('div.container').show();

        self.load();
    };

    self.load = function() {
        if (self._loadInProgress) return;

        var queue = new biomart.Queue($content, 'lazy', true),
            $children = $containers.children('.container'),
            total = 0,
            numLoaded = 0;

        self.lock();

        for (var i=0, c; i<$children.length; i++) {
            c = $children.eq(i);

            if (c.data('loaded')) {
                continue;
                numLoaded++;
            }

            queue.queue(function(c, i) {
                if (!c.data('loaded')) {
                    if (i==0) $loading.slideDown();
                    c.children('h3,h4').minimizer('show');
                    loadContainer(c, function() {
                        if (--total == 0) {
                            self.unlock();
                            $loading.slideUp();
                        }
                        queue.dequeue();
                    });
                } else {
                    if (--total == 0) {
                        $content.trigger('loaded.container');
                        self.unlock();
                        $loading.slideUp();
                    }
                    queue.dequeue();
                }
            }.partial(c, total++));
        }

        if (total == 0) {
            self.unlock();
            $content.trigger('loaded.container');
        }
        if (numLoaded == $children.length) {
            $(window).unbind('scroll.lazy');
        }
        queue.dequeue();
    };

    function processUserFilters() {
        var validFilters = true,
            k,
            f,
            validFilters_sub; // for any sub-filters

        // We should generate report only when all common filters are set
        for (k in allFilters) {
            f = allFilters[k].filter;
            // Case 1: filter type is a list of sub-filters
            if (f.filters.length) {
                validFilters_sub = false;
                // the value of _q parameter becomes the default value for all filters
                if (defaultQuery) {
                    f.value = [f.filters[0].name, defaultQuery, f.filters[0].displayName];
                } else {
                    // Check value against all sub-filters
                    for (var i=0, f_sub; f_sub=f.filters[i]; i++) {
                        if (userFilters[f_sub.name]) {
                            validFilters_sub = true;
                            f.value = [f_sub.name, userFilters[f_sub.name], f_sub.displayName];
                            break;
                        }
                    }
                    // If one required filter isn't set
                    if (!validFilters_sub) {
                        validFilters = false;
                        break;
                    }
                }
            // Case 2: we can directly use the filter value
            } else {
                // the value of _q parameter becomes the default value for all filters
                if (defaultQuery) {
                    f.value = defaultQuery;
                } else {
                    if (!userFilters[f.name]) {
                        validFilters = false;
                        break;
                    } else {
                        f.value = userFilters[f.name];
                    }
                }
            }
        }

        return validFilters;
    }

    /*
     * Loads containers for the givien mart
     * @param {object} m: mart object
     * @param {function} callback
     */
    function getContainersFromMart(m, callback) {
        var datasets = userDatasets || m.datasets,
            params = {datasets: datasets.join(',')};
        if (m.config)
            params.config = m.config;
        biomart.resource.load('containers', function(json) {
            if (!json.containers) { // ignore marts with no containers
                if (callback) callback();
                return;
            }
            // first container should contain global filters, and the descendents are for display
            var topContainer = json.containers[0];
            processFilters(topContainer);
            for (var i=0, c; c=topContainer.containers[i]; i++)
                storeDisplayContainers(m, c);
            if (callback) callback();
        }, params);
    }

    function loadContainer(element, callback) {
        var item = element.data('item'),
            mart = element.data('mart');
        loadData(mart, item, element, function() {
            if (callback) callback();
        });
    }


    /*
     * Extract all leaf containers recursively and store the info on parent object.
     * @param p: parent object
     * @param c: container object
     * @param d: current node depth
     */
    function storeDisplayContainers(p, c, d) {
        d = d || 1; // defaults to 1
        var temp = { displayName: c.displayName, name: c.name, independent: c.independent, depth: d, description: c.description };

        if (c.filters) {
            temp.filters = c.filters;
        }

        if (c.attributes) {
            temp.attributes  = c.attributes;
            if (!p.containers) p.containers = [];
            p.containers.push(temp);
        } else return;

        if (!c.containers) return;

        for (var i=0, child; child=c.containers[i]; i++)
            storeDisplayContainers(temp, child, d+1);
    }

    function processFilters(c) {
        for (var i=0, f; f=c.filters[i]; i++) {
            if (!allFilters[f.displayName])
                allFilters[f.displayName] = { filter: f, containers: [c] };
            else
                allFilters[f.displayName].containers.push(c);
        }
    }

    /*
     * Using containers loaded earlier, draw each container in the DOM
     * Only draw the first level of containers
     */
    function drawContainers() {
        var queue = new biomart.Queue($containers, 'draw', true);

        $filters.keydown(function(ev) {
            if (ev.keyCode == 13 && $(ev.target).hasClass('text')) {
                $(ev.target).trigger('change');
                submitForm();
            }
        });

        initialized = true;
        $.publish('biomart.initialized');
    }

    /*
     * Update all filter values and submit form
     */
    function submitForm() {
        var baseUrl = location.href.split('?')[0],
            newParams = {mart: selectedMart.name},
            userFilters = [];

        $content.find('div.filter-container').each(function() {
            var $this = $(this),
                value = biomart.validator.filter($this),
                obj = $this.data('item'),
                name = obj.name,
                displayName = obj.displayName,
                curr,
                queryString;

            if (value) {
                if ($.isArray(value)) {
                    name = value[0];
                    displayName = value[2];
                    value = value[1];
                }
                newParams[name] = value;
                userFilters.push([name, '^', displayName, '=', value].join(''));
            }
        });

        newParams.report = reportName;
        if (userDatasets) newParams.datasets = userDatasets.join(',');
        urlHash.query = queryString = $.param(newParams);

        location = biomart.url.stringify(urlHash);
    }

    /*
     * Loads data for the report given set filters
     * @param {object} mart: mart object
     * @param {object} container: container object
     * @param {DOM element} element: DOM element
     * @param {function} callback
     */
    function loadData(mart, container, element, callback) {
        var $header = element.children('h3,h4'),
            $content = element.find('div.content'),
            $data = $content.children('div.data'),
            datasets,
            meta;

        var queryMart = $.extend({}, mart);

        if (mart.meta) {
            meta = new biomart.utils.MetaInfo(mart);
        }

        if (!element.length || element.data('loaded')) {
            doNext();
            return;
        }

        if (container.independent) {
            datasets = mart.datasets;
        } else {
            datasets = userDatasets || mart.datasets.slice(0,1);
        }

        // For independent querying, need to know which attributes are for which container
        if (container.independent) {
            var params = {datasets: datasets.join(','), container: container.name};
            if (mart.config) {
                params.config = mart.config;
            }
            biomart.resource.load('attributes/mapped', function(json) {
                runQuery(json);
            }, params);
        } else {
             runQuery(container.attributes);
        }

        function runQuery(json) {
            /*
             * dict will be populated by iterate() function
             * if container has independent query, dict will be a hash of attr_name -> attr_obj
             * else it'll be a list objects containing (dataset_name, attributes)
             */
            var dict = {},
                displayType,
                aggregation,
                renderAsTable,
                config,
                extraOptions;

            if (meta) {
                if (config = meta._containerConfigs[container.name]) {
                    if (config.rendering) displayType = config.rendering;
                    if (config.options) extraOptions = config.options;
                    if (config.aggregation) aggregation = config.aggregation;
                }
            }

            if (!displayType) {
                // default to table or list
                displayType = container.independent || (!container.containers && container.depth==1) ? 'table' : 'list';
            }

            if (!aggregation) {
                 aggregation = container.independent ? 'count' : 'none';
            }

            renderAsTable = displayType == 'table';

            // Sub-containers will use their own filter and ignore the global ones
            queryMart.datasets = container.independent ? datasets : datasets[datasets.length > 10 ? 10 : 0];
            queryMart.filters = {};
            queryMart.attributes = json;

            beforeQuery();

            // Render results
            $data.queryResults($.extend({
                queries: prepareXML('TSVX', -1, true, queryClient, queryMart, container.independent),
                loading: element.find('.loading'),
                dataAggregation: aggregation,
                displayType: displayType,
                displayOptions: $.extend({
                    squish: !container.containers,
                    paginateBy: container.independent || !renderAsTable ? false : 10
                }, extraOptions),
                showEmpty: !renderAsTable,
                done: function() {
                    element.data('loaded', true);
                    doNext();
                }
            }, QUERY_RESULTS_OPTIONS));
        }

        function beforeQuery() {
            var f;
            if (!container.topLevel) {
                if (!container.filters) {
                    alert('Sub-containers must container one filter: ' + container.name);
                    return;
                }
                for (var i=0, filter; filter = container.filters[i]; i++) {
                    queryMart.filters[filter.name] = filter;
                }
            } else {
                for (var k in allFilters) {
                    f = allFilters[k].filter;
                    queryMart.filters[f.name] = f;
                }
            }
        }

        function doNext() {
            if (container.description) {
                $(['<p class="description">', container.description, '</p>'].join('')).appendTo($data);
            }

            element.find('li:not(.group)').hover(function() {
                $(this).addClass('hover').children('span').addClass('hover');
            }, function() {
                $(this).removeClass('hover').children('span').removeClass('hover');
            });

            if (!container.independent && container.containers) {
                createSubContainers(mart, container, element, callback);
            } else {
                if (callback) callback();
            }
        }
    }

    /* Builds a hash object such that each attribute knows the dataset in which it is contained in.
     * For use in aggregate-type results; Only valid for attribute lists.
     */
    function iterate(hash, attributes, dataset) {
        for (var i=0, a; a=attributes[i]; i++) {
            if (a.isHidden) continue;
            // Need at least two attributes in a list
            if (a.attributes.length < 2) {
                return;
            }

            if (hash[a.name]) { // attribute processed before?
                if (a.attributes.length) {
                    if (dataset) {
                        if(!hash[a.name].datasets) hash[a.name].datasets = [];
                        hash[a.name].datasets.push(dataset);
                    }
                    hash[a.name].attributes= a.attributes;
                }
            } else {
                hash[a.name] = a;
                if (dataset && a.attributes.length) {
                    a.datasets = [dataset];
                }
            }
        } // for
    } // iterate

    /*
     * Create DOM elements and load data for a container + additional subcontainers
     * @param {object} mart
     * @param {object} c - the container
     * @param {DOM element} element - DOM element corresponding to container
     * @param {function} callback
     */
    function createSubContainers(mart, c, element, callback) {
        var qn = ['sub', c.name, c.depth].join('_'),
            queue = new biomart.Queue($containers, qn, true);

        $containers.one('queue.done.' + qn, function() {
            if (callback) callback();
        });

        element.find('div.content > div.data > ul.writee > li.group').each(function() {
            var $this = $(this),
                pk = $this.attr('pk');

            for (var i=0, c_i; c_i=c.containers[i]; i++) {
                // If container does not specify own filters, use parent filters
                if (!c_i.hasFilters) {
                    c_i.filters = [c.filters[0]];
                }

                c_i.filters[0].value = pk;

                var el = $this.children('.container.' + c_i.name);
                if (!el.length) el = newContainer($this, mart, c_i, true);
                else el.show();

                loadData(mart, c_i, el, function() { queue.dequeue() });
            }
        });

        queue.dequeue();
    }

    /*
     * Create new DOM element for given primary attribute
     * @param {DOM Element} parentElement
     * @param {Object} container
     * @param {boolean} isSub
     */
    function newContainer(parentElement, mart, container, isSub) {
        var element = $('<div id="' + biomart.uuid() + '"/>')
                    .addClass(['container', container.name, 'level-'+container.depth, 'gradient-grey-reverse'].join(' '))
                    .appendTo(parentElement),
            level = isSub ? 4 : 3;

        if (isSub) element.addClass('sub');
        else element.hide()

        if (!container.containers) element.addClass('leaf');

        // New content div
        $('<div/>')
            .addClass('data')
            .appendTo(element)
            .wrap('<div class="content"/>');


        var dlLink = $('<div align="right" style="display: none"><a href="javascript:;" class="report-download">Download</a></div>');
        dlLink.delegate('a.report-download','click',function(ev){
        	ev.stopPropagation();

       	 	var $form = $('<form style="height: 1; visibility: hidden" action="'+ BIOMART_CONFIG.service.url+'results">').appendTo(document.body);

            $form
            	.append( $('<input type="hidden" name="download" value="true"/>'))
            	.append( $('<input type="hidden" name="query"/>'));

            var datasets;
            if (container.independent) {
                datasets = mart.datasets;
            } else {
                datasets = userDatasets || mart.datasets.slice(0,1);
            }

            var queryMart = $.extend({}, mart);
            queryMart.datasets = container.independent ? datasets : datasets[datasets.length > 10 ? 10 : 0];
            queryMart.filters = {};
            if (!container.topLevel) {
                if (!container.filters) {
                    alert('Sub-containers must container one filter: ' + container.name);
                    return;
                }
                for (var i=0, filter; filter = container.filters[i]; i++) {
                    queryMart.filters[filter.name] = filter;
                }
            } else {
                for (var k in allFilters) {
                    f = allFilters[k].filter;
                    queryMart.filters[f.name] = f;
                }
            }

            if (container.independent) {
                var params = {datasets: datasets.join(','), container: container.name};
                if (mart.config) {
                    params.config = mart.config;
                }
                biomart.resource.load('attributes/mapped', function(json) {
                	queryMart.attributes = json;
                }, params);
            }else{
            	queryMart.attributes = container.attributes;
            }

            var dlQueries = prepareXML('TSV', -1, true, queryClient, queryMart, container.independent)

            $form.children('input[name=query]').val(dlQueries);

            $form.submit();
        });
        // New header
        if (!isSub) {
        	// remove download link for mutation
        	if(container.name == 'mutation'){
        	$('<h' + level + '/>')
                .text(container.displayName)
                .css('cursor', 'pointer')
                .append('<span class="ui-icon ui-icon-triangle-1-s"/>')
                .disableSelection()
                .prependTo(element)
                .minimizer({duration: 100, state: 'hide'});
        	}else{
            $('<h' + level + '/>')
                .text(container.displayName)
                .css('cursor', 'pointer')
                .append('<span class="ui-icon ui-icon-triangle-1-s"/>')
                .append(dlLink)
                .disableSelection()
                .prependTo(element)
                .minimizer({duration: 100, state: 'hide'});
        	}
        }
        element.data('item', container);
        element.data('mart', mart);

        return element;
    }

    /*
     * Generates query XML text (could be an array of XML strings)
     */
    function prepareXML(processor, limit, header, client, mart, perDataset) {
        if (perDataset !== true) {
             return biomart.query.compile('XML', mart, processor, limit, header, client);
        }

        // for independent queries (i.e. one per dataset) we need to make an array that has the query and the heading
        var arr = [],
            datasets = mart.attributes.datasets,
            temp,
            xml,
            attr,
            queries = [],
            header;

        datasets.sort(function(a, b) {
            var x = datasetMap[a.name].displayName.toLowerCase(),
                y = datasetMap[b.name].displayName.toLowerCase();
            if (x > y) return 1;
            if (y > x) return -1;
            return 0;
        });

        for (var i=0, ds; ds=datasets[i]; i++) {
            var added = false;

            for (var j=0, currMart, attr; attr=ds.attributes[j]; j++) {
                currMart = {
                    datasets: [ds.name],
                    filters: mart.filters,
                    attributes: [attr]
                };

                if (mart.config) currMart.config = mart.config;

                added = true;

                queries.push({
                    query: biomart.query.compile('XML', currMart, processor, limit, header, queryClient),
                    attr: attr,
                    dataset: datasetMap[ds.name]
                });
            }

            if (!added) {
                queries.push({
                    dataset: datasetMap[ds.name]
                });
            }
        }

        return queries;
    }
});

$.subscribe('biomart.initialized', biomart.martreport, 'ready');
$.subscribe('biomart.init', biomart.martreport, 'init');
})(jQuery);

