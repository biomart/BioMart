(function($) {
$.namespace('biomart.martsearch', function(self) {
    var LIMIT = 1000,
        CLIENT = 'webbrowser',
        DELAY = $.browser.msie ? 50 : 25,

        $body = $(document.body),
        $query = $('#biomart-query'),
        $content = $('#biomart-content'),
        $results = $('#biomart-results'),
        $info = $results.children('.info'),
        $start = $info.children('.start'),
        $fetched = $info.children('.fetched'),
        $end = $info.children('.end'),
        $total = $info.children('.total'),
        $list = $results.find('.results'),
        $pagination = $results.find('.pagination'),
        $progressbar = $('#biomart-progressbar'),
        $noResults = $results.find('p.no-results'),
        $hint = $content.find('p.hint'),
        pageSize = 20,
        pageNum = 1,
        searches = [],
        liveRequests = [],

        noResults = true,
        inProgress = false,
        cancelled = false;

    /*
     * =init
     */
    self.init = function() {
        refresh(); // refresh param from fragment

        // Listeners and UI widgets
        $(window).bind('hashchange', function() {
            refresh();
            queueSearch();
        });
        $('#biomart-submit').button({
             icons: {
                primary: 'ui-icon-search'
            }
        });
        $query.bind('focus.search', function() {
            $hint.animate({opacity: 1}, 200);
        });
        $query.bind('blur.search', function() {
            $hint.animate({opacity: 0}, 200);
        });
        $('#biomart-form').submit(function() {
            pageNum = 1;
            self.term = $query.val();
            self.term = $.trim(self.term);
            $query.val(self.term);
            location.hash = [biomart.url.SEPARATOR, '?q=', self.term].join('');
            return false;
        });
        $pagination.click(function(ev) {
            var $origin = $(ev.target);
            if ($origin.hasClass('next')) {
                pageNum++;
                queueSearch();
            } else if ($origin.hasClass('prev')) {
                pageNum--;
                queueSearch();
            }
            return false;
        });
        $query.focus();

        // Reading in config from MartService
        biomart.resource.load('portal', function(root) {
            var marts = biomart.utils.getMartsFromGuiContainer(root, 'martsearch'),
                $temp = $('<div/>').appendTo($body),
                queue = new biomart.Queue($temp, 'marts', true);

            for (var i=0, mart; mart=marts[i]; i++) {
                queue.queue(function(mart) {
                    loadDatasets(mart, function(datasets) {
                        var datasetString = $.map(datasets, function(a) { return a.name }).join(',')
                        loadContainers(mart, datasetString, function() { queue.dequeue() });
                    });
                }.partial(mart));
            }

            $temp.one('queue.done.marts', function() {
                $('#biomart-loading').hide();
                if (self.term) doSearch();
            });
            queue.dequeue();
        });
    };

    function refresh() {
        var urlJson = biomart.url.jsonify(location.href),
            params = biomart.url.simpleQueryParams(
                biomart.url.jsonify(urlJson.fragment).query
            );

        if (params && params.q) {
            self.term = params.q;
            $query.val(self.term);
        }
    }

    function loadDatasets(mart, callback) {
        biomart.resource.load('datasets', function(json) {
            callback(json);
        }, {config: mart.name});
    }

    function loadContainers(mart, datasets, callback) {
        var params = {datasets: datasets};
        if (mart.config)
            params.config = mart.config;
        biomart.resource.load('containers', function(root) {
            var containers = root.containers,
                queue = new biomart.Queue($content, 'containers', true);

            if (containers) {
                for (var i=0, container; container=containers[i]; i++) {
                    queue.queue(function(container) {
                        var search = {
                            datasets: datasets,
                            container: container
                        };

                        if (mart.config) search.config = mart.config;

                        loadFilters(mart, container, search.datasets, function(filters) {
                            search.filters = filters;
                            loadAttributes(mart, container, search.datasets, function(attributes) {
                                search.attributes = attributes;
                                searches.push(search);
                                queue.dequeue();
                            });
                        });
                    }.partial(container));
                }
            }

            $content.one('queue.done.containers', callback);
            queue.dequeue();
        }, params);
    }

    function loadFilters(mart, container, datasets, callback) {
        var params = {datasets: datasets, container: container.name};
        if (mart.config)
            params.config = mart.config;
        biomart.resource.load('filters', function(json) {
            var filterNames = flattenItems('filter', json);
            callback(filterNames);
        }, params);
    }

    function loadAttributes(mart, container, datasets, callback) {
        var params = {datasets: datasets, container: container.name};
        if (mart.config)
            params.config = mart.config;
        biomart.resource.load('attributes', function(json) {
            var attributeNames = flattenItems('attribute', json);
            callback(attributeNames);
        }, params);
    }

    // Returns an array of filter/attribute names
    // Ignores any filter/attribute lists
    function flattenItems(type, list) {
        var arr = [],
            plural = type + 's';

        for (var i=0, item; item=list[i]; i++) {
            if (item.isHidden) continue;
            if (!item[plural].length) {
                arr.push({
                    name: item.name,
                    displayName: item.displayName
                });
            } else {
                for (var j=0, curr; curr=item[plural][j]; j++) {
                    arr.push({
                        name: curr.name,
                        displayName: curr.displayName
                    });
                }
            }
        }

        return arr;
    }

    function queueSearch() {
        if (inProgress) {
            cancelled = true;

            $results.one('queue.done.queries', function() {
                cancelled = false;
                queueSearch();
            });

            for (var i=0, n=liveRequests.length; i<n; i++) {
                liveRequests[i].abort();
            }

            liveRequests = [];
        } else {
            doSearch();
        }
    }

    function doSearch() {
        var start = new Date().getTime(),
            completed = 0,
            total = 0,
            resultsQ = new biomart.Queue($results, 'queries', true),
            regex = new RegExp(self.term, 'i');

        if (inProgress) return;

        inProgress = true;
        noResults = true;

        $query.blur();
        $list.empty();
        $noResults.hide();
        $progressbar.progressbar();

        // For each search, fire one query per filter
        for (var i=0, search; search=searches[i]; i++) {
            resultsQ.queue(function(search, index) {
                var localQ = new biomart.Queue($results, 'search_' + index, true),
                    hasResult = false;

                for (var j=0, filter; filter=search.filters[j]; j++, total++) {
                    var xml = biomart.query.compile('XML', {
                            config: search.config,
                            datasets: search.datasets,
                            attributes: search.attributes,
                            filters: [{ name: filter.name, value: self.term }]
                        }, 'TSVX', LIMIT, 0, CLIENT);

                    localQ.queue(function(xml) {
                        if (cancelled || hasResult) {
                            finalize(function() { localQ.dequeue() });
                            return;
                        }
                        liveRequests.push($.ajax({
                            url: BIOMART_CONFIG.service.url + 'results',
                            data: {
                                query: xml
                            },
                            type: 'POST',
                            success: function(data) {
                                if (biomart.errorRegex.test(data)) return;
                                if (data) {
                                    var rows = data.split('\n'),
                                        html = [];

                                    hasResult = true;

                                    for (var i=0, row; row=rows[i]; i++) {
                                        var cols = row.split('\t'),
                                            curr = [];

                                        html.push('<li>');

                                        html.push([
                                            '<span class="container">',
                                                search.container.displayName,
                                            '</span> '
                                        ].join(''));

                                        for (var j=0, col; j<cols.length; j++) {
                                            var col = cols[j],
                                                match = regex.test(col);
                                            if (col) {
                                                curr.push([
                                                    '<span class="value', (match?' match':''), '" title="', search.attributes[j].displayName, '">',
                                                        col,
                                                    '</span>'
                                                ].join(''));
                                            }
                                        }

                                        html.push(curr.join(', '));
                                        html.push('</li>');
                                    }
                                    $list.append(html.join(''));
                                }
                            },
                            complete: function() {
                                finalize(function() { localQ.dequeue() });
                            }
                        }));
                    }.partial(xml));
                }

                $results.one(['queue.done.', 'search_', index].join(''), function() {
                    if (hasResult) noResults = false;
                    resultsQ.dequeue();
                });
                localQ.dequeue();
            }.partial(search, i));
        }

        $results.one('queue.done.queries', function() {
            inProgress = false;
            if (!cancelled) {
                if (noResults)
                    $noResults.html(_('no_results')).show();

                $progressbar.animate({opacity: 0}, {
                    duration: 100,
                    complete: function() { $progressbar.progressbar('destroy').animate({opacity: 1}) }
                });
            } else {
                 $progressbar.progressbar('destroy').animate({opacity: 1});
            }
        });
        resultsQ.dequeue();

        function finalize(callback) {
            var percent = ++completed/total * 100;
            $progressbar.progressbar('value', percent);
            if (callback) {
                // Stagger every 20 requests or Firefox will falsely throw recursion error :(
                if (completed%20 == 0) {
                    setTimeout(callback, 15);
                } else {
                    callback();
                }
            }
        }
    }

    function getStart() { return (pageNum-1) * pageSize + 1 }
    function getEnd(max) { return Math.min(pageNum * pageSize, max) }
});

$.subscribe('biomart.init', biomart.martsearch, 'init');
})(jQuery);

// Old success function for MongoDB.
//                    success: function(json) {
//                        if (!json.total) {
//                            $list.html(['<li><span>No results found for <strong>', term, '</strong></span></li>'].join(''));
//                            $results.unblock();
//                            return;
//                        }
//
//                        var end = new Date().getTime(),
//                            rStart = getStart(),
//                            rEnd = getEnd(json.total)
//                            ;
//
//                        $fetched.text(['(fetched in ', biomart.number.format((end-start)/1000, {decimals: 2}), ' seconds)'].join(''));
//                        $start.text(rStart);
//                        $end.text(rEnd);
//                        $total.text(json.total);
//
//                        for (var i=0, row, html=[]; row=json.results[i]; i++) {
//                            var buf = [];
//                            html.push([
//                                '<li>',
//                                '    <span class="number">', rStart+i, '.</span>'
//                            ].join(''));
//                            for (var k in row) {
//                                if (k == '_id') continue;
//                                buf.push([
//                                    '<span class="key">', k, '</span> ',
//                                    '<span class="value">', row[k], '</span>',
//                                ].join(''));
//                            }
//                            html.push(buf.join('; '));
//                            html.push('</li>');
//                        }
//
//                        $list.html(html.join(''));
//
//                        if (rStart > 1)
//                            $pagination.children('.prev').removeClass('hide');
//                         else
//                            $pagination.children('.prev').addClass('hide');
//
//                        if (rEnd < json.total)
//                            $pagination.children('.next').removeClass('hide');
//                         else
//                            $pagination.children('.next').addClass('hide');
//
//                        $results
//                            .unblock()
//                            .show()
//                            .removeClass('in-progress')
//                            ;
//                    }
//
