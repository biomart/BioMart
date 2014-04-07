(function($) {
/*
 * Query Results
 *
 * Can either display a simple table (one query -> one table) or
 * can also display aggregate results table (multiple queries -> one table)
 *
 * Other display types are defined in js/renderer.js, but aggregation only works for 'table'
 *
 * NB: In order for aggregate tables to work, some hacks are needed for
 *        each attribute list to know which dataset(s) they belong to. See
 *     martreport/js/main.js: loadData() and prepareXML() for details.
 */
var QueryResults = {
    _htmlRegex: /<.+?>/g,
    options: {
        data: null, // either data or queries need to be used
        queries: null, // can be an array or string
        animationTime: 250,
        iframe: false, // if set to DOM Element <iframe>, will be used to stream data
        headers: true, // treat first row as headers?
        footer: true, // print data info on bottom?
        displayType: 'table',
        dataAggregation: 'none',
        paginateBy: false,
        page: 1,
        sorting: true,
        showEmpty: false,
        timeout: 45000,
        displayOptions: {},
        colDataTypes: {} //add all column types for sort to make sense
    },

    _isPaginated: false,

    _create: function() {
        var self = this,
            options = self.options;

        self._originalClassNames = self.element.attr('className');

        if (options.footer) {
            self._info = $('<p class="info"/>').insertBefore(self.element);
            self._addedDomElements.push(self._info);
        }

        self.element.addClass('ui-queryResults');
    }, // create

    _init: function () {
        var self = this,
            options = self.options;

        if (options.displayOptions.paginateBy) {
            options.paginateBy = options.displayOptions.paginateBy;
        }

        // Remove query limit for charts
        if (!$.isArray(options.queries) && (options.displayType == 'chart' || options.displayType == 'histogram')) {
            options.queries = options.queries.replace(/limit=".+?"/, 'limit="-1"');
        }

        // No sorting or pagination for nontable types
        if (options.displayType != 'table') {
            options.sorting = false;
            options.paginateBy = false;
        }

        // Make sure we have at least one query to run
        if (!options.queries && !options.data) {
            self.element.html('Query or data is not valid').addClass('error');
             return;
        }

        // Figure out rendering function, and default it if not valid
        engine = self._aggregators[options.dataAggregation] || self._aggregators.none;
        options.displayOptions.type = options.displayType;
        engine.apply(self, [options.displayOptions]);

        if (options.sorting) {
            self.element
                .delegate('td.sort', 'click.queryResults', function() {
                    var $this = $(this),
                        index = $this.index(),
                        asc;
                    if ($this.hasClass('sorted'))
                        asc = !$this.hasClass('asc');
                    else
                        asc = true;
                    self.sort(asc, index);
                });
        }
    },

    _addedDomElements: [],

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);

        for (var i=0, $curr; $curr = this._addedDomElements[i]; i++) {
            $curr.remove();
        }

        this._addedDomElements = null;

        this.element
            .datacontroller('destroy')
            .datasource('destroy')
            .paginate('destroy')
            .unbind('datasource.success.queryResult')
            .unbind('datasource.error.queryResult')
            .unbind('datasource.complete')
            .attr('className', this._originalClassNames);

        delete this.element.datacontroller('writee')._originalCache;

        if (this.options.sorting) this.element.undelegate('td.sort', 'click.queryResults');

        this.element.removeClass('ui-queryResults');
    },

    sort: function(asc, index) {
        var self = this,
            $element = self.element.datacontroller('writee'),
            $td = $element.find('td.sort').removeClass('sorted desc asc').eq(index),
            cache = $element._resultsCache,
            originalCache = cache.slice();

        $td.addClass(['sorted ', asc ? 'asc' : 'desc'].join(''));

        self.element.datacontroller('highlight', index);

        $element._originalCache = originalCache;
        $element._resultsCache.sort(function(left, right) {

            var a = left[index].replace(self._htmlRegex, '').toUpperCase(),
                b = right[index].replace(self._htmlRegex, '').toUpperCase();
            var datatype = self.options.colDataTypes[index];
            // when sort always move no data to the end
            if(a === '' || a === null) return 1;
            if(b === '' || b === null) return -1;
            // parse the data to the correct data types for sorting
            switch(datatype){
	            case 'STRING':
	    			break;
	    		case 'INTEGER':
	    			a = parseInt(a);
	    			b = parseInt(b);
	    			break;
	    		case 'FLOAT':
	    			a = parseFloat(a);
	    			b = parseFloat(b);
	    			break;
	    		case 'BOOLEAN':
	    			a = (a === 'TRUE');
	    			b = (b === 'TRUE');
	    			break;
            }
            if (a > b) return asc ? 1 : -1;
            else if (a < b) return asc ? -1 : 1;
            return 0;
        });

        if (self._isPaginated) {
            self.element.paginate('page', 1, true);
        } else {
            self.element.datacontroller('paginate', 0, Infinity)
        }

        if (self.options.onSort) {
            self.options.onSort(asc, index);
        }
    },

    removeSort: function() {
        var self = this,
            $element = self.element.datacontroller('writee'),
            originalCache = $element._originalCache;
        $td = $element.find('td.sorted').removeClass('sorted desc asc');
        $element._resultsCache = originalCache;
        delete $element._originalCache;
        this.element.datacontroller('highlight', null);
        if (self._isPaginated) self.element.paginate('page', 1, true);
        else self.element.datacontroller('paginate', 0, Infinity)
    },

    // These functions need to be invoked via the apply() method so that 'this' refers to the widget
    _aggregators: {
        /* NONE */
        none: function(displayOptions) {
            var self = this,
                element = self.element,
                options = self.options,
                queries = options.queries,
                iframe = options.iframe,
                loading = options.loading || $('<span class="loading with-msg" style="display: none"/>').insertBefore(element),
                sourceOptions = {
                    url: BIOMART_CONFIG.service.url + 'results',
                    timeout: options.timeout,
                    data: {
                        query: queries
                    }
                },
                controllerOptions = $.extend(displayOptions, {
                    done: function(total, start, end) {
                        var html = [],
                            paginateOptions = {
                                pageSize: options.paginateBy,
                                total: total,
                                start: start,
                                end: end,
                                change: options.onPageChange
                            };

                        if (options.showEmpty || total) {
                            if (options.footer) {
                                 paginateOptions.infoElement = self._info;
                            }

                            if (options.sorting) {
                                element.find('thead > tr > td')
                                    .addClass('sort')
                                    .children('p')
                                        .prepend('<span class="ui-icon"/>');
                            }

                            // Paginate if the renderer supports it
                            // Also check that paginateBy is a proper number
                            // Lastly, there has to be enough rows to paginate
                            if (this._renderer.canPaginate && options.paginateBy && end < total) {
                                self._isPaginated = true;
                                element.paginate(paginateOptions);
                                if (total >= 1000 && options.footer) {
                                    self._info.append([
                                        '<br/><br/>',
                                        '<span class="fyi">',
                                            _('results beyond 1000 not displayed'),
                                        '</span>'
                                    ].join(''));
                                }
                            }

                            if (options.sort) self.sort(options.sort.order=='asc', options.sort.col);
                            if (self._isPaginated) element.paginate('page', options.page);
                        } else if (!element.datacontroller('hasError')) {
                            element.html(['<p class="empty">', _('no_results'), '</p>'].join(''));
                        }

                        loading.animate({opacity:'hide'}, 1000, function() {
                            $(this).remove();
                            if (options.done) options.done(total);
                        });
                    },
                    headers: true
                });

            loading.animate({opacity: 'show'}, 200);
            self._addedDomElements.push(loading);

            if (options.paginateBy) {
                controllerOptions.limit = options.paginateBy, // Need to cache data for pagination to work
                controllerOptions.skip = 0;
            }

            // TODO: Should refactor these blocks to use `sourceOptions`
            // First need to write tests so the code can be checked after refactoring
            if (iframe) {
                sourceOptions.iframe = typeof iframe == 'object' ? iframe : null;
                sourceOptions.type = 'streaming';
            }
            if (options.data) {
                sourceOptions.type = 'local';
                sourceOptions.data = options.data;
            }

            // Setting source options
            if (options.sourceOptions) {
                sourceOptions = $.extend(sourceOptions, options.sourceOptions);
            }

            // Setup data streams and controller
            element
                .datacontroller(controllerOptions)
                .datasource(sourceOptions)
                // Direct data from datasoure to datacontroller
                .bind('datasource.success.queryResult', function(ev, s) {
                    element.datacontroller('write', s);
                })
                .bind('datasource.error.queryResult', function(ev, reason) {
                    element.datacontroller('error', reason);
                })
                .one('datasource.complete', function() {
                    element
                        .unbind('datasource.success.queryResult')
                        .unbind('datasource.error.queryResult')
                        .datacontroller('done');
                })
                .datasource('exec');
        },

        /* COUNT */
        // TODO: Clean this aggregation code up... it should pass the display to the renderer
        count: function(displayOptions) {
            var self = this,
                element = self.element.addClass('table'),
                table = $('<table class="aggregate writee"/>').appendTo(element),
                thead = $('<thead  unselectable="on" style="-moz-user-select: none;"/>').appendTo(table),
                tbody = $('<tbody/>').appendTo(table),
                options = self.options,
                queries = options.queries,
                queue = new biomart.Queue(element, 'aggregate'),
                openConnections = 0, // keep track of open connections
                rowTemplate = [],
                tableHeader = [],
                rowMap = {},
                colMap = {},
                cache = [],
                colIndex = 1, // start at 1 because 0 is the merge/primary column
                next = function() {
                    openConnections--;
                    queue.dequeue(biomart.MAX_CONCURRENT_REQUESTS - openConnections);
                };

                self.element.datacontroller('writee', table);
                self.element._resultsCache = [];

            element.one('queue.done.aggregate', function() {
                if (self._error) {
                    self.element.before([
                        '<p class="info error">',
                            _('error_msg_verbose'), ' ', _('error_affected_rows_msg'),
                        '</p>',
                    ].join(''));
                }
                if (options.loading) options.loading.animate({opacity:'hide'}, 1000, function() { $(this).remove() });
                if (options.done) options.done();
            });

            // Delegate function for showing more details
            element.delegate('a[rel=more]', 'click.queryresults.count', function(ev) {
                var $target = $(ev.target),
                    $cell = $target.parent(),
                    results = $cell.data('results'),
                    $div = $(['<div title="', $cell.data('uniqueName'),'"/>'].join(''));
                $div.hide().appendTo(document.body).dialog({
                    autoOpen: true,
                    dialogClass: 'details',
                    width: Math.min(800, results[0].length * 100),
                    height: Math.min(400, results.length * 60 + 40),
                    open: function() {
                        $(this)
                            .queryResults({
                                data: results,
                                sorting: true
                            });
                    },
                    close: function() {
                        $(this)
                            .queryResults('destroy')
                            .dialog('destroy');
                    }
                });
            });

            // build templates
            rowTemplate[0] = '<td class="merge"></td>';
            tableHeader[0] = '<td class="merge sort"><p><span class="ui-icon"/>Dataset</p></td>';
            for (var i=0, query; query=queries[i]; i++) {
                if (query.attr) {
                    if (!colMap[query.attr.name]) {
                        colMap[query.attr.name] = colIndex;
                        rowTemplate[colIndex] = ['<td class="', query.attr.name, '"><span class="icon throbber">&nbsp;</span></td>'].join('');
                        tableHeader[colIndex] = ['<td class="', query.attr.name, ' sort"><p><span class="ui-icon"/>', query.attr.displayName, '</p></td>'].join('');
                        colIndex++;
                    }
                }
            }

            rowTemplate = $(['<tr>', rowTemplate.join(''), '</tr>'].join(''));
            thead.append(['<tr>', tableHeader.join(''), '</tr>'].join(''));

            // Loop through again, this time making AJAX requests
            for (i=0, query; query=queries[i]; i++) {
                if (!rowMap[query.dataset.name]) {
                    rowMap[query.dataset.name] = {
                        element: rowTemplate.clone().appendTo(tbody).children().first().text(query.dataset.displayName).end().end(),
                        index: i
                    };
                }

                queue.queue(function(curr) {
                    openConnections++;
                    // If no attributes to query (or less than 2), just print an empty message
                    // Need at least two to display properly
                    if (!curr.attr || curr.attr.attributes.length < 2) {
                        finalizeColumn(rowMap[curr.dataset.name].element, curr.attr.name);
                        next();
                        return;
                    }

                    $.ajax({
                        url: BIOMART_CONFIG.service.url + 'results',
                        timeout: options.timeout,
                        data: {
                            iframe: false,
                            query: curr.query
                        },
                        type: 'POST',
                        complete: next,
                        error: function() {
                            self._error = true;
                            aggr = rowMap[curr.dataset.name].element;
                            aggr.addClass('error');
                            finalizeColumn(aggr, curr.attr.name);
                        },
                        success: function(data) {
                            var rows = data.split('\n');
                            var cache = [];
                            cache.push(curr.dataset.displayName);
                            for (var i=0, row; row=rows[i]; i++) {
                                var aggr = rowMap[curr.dataset.name].element,
                                    cell;

                                row = row.split('\t');

                                if (!row.length) continue;

                                aggr.children().first().text(curr.dataset.displayName);

                                cell = aggr.children('td.'+curr.attr.name).eq(0);

                                // count on the first column then calculate ratio using second column as total
                                if (biomart.errorRegex.test(row[0])) {
                                    self._error = true;
                                    element.datacontroller('error');
                                    aggr.addClass('error');
                                } else if (row[0]) {
                                    var seen = cell.data('seen'),
                                        results = cell.data('results'),
                                        slice;

                                    if (!results) {
                                        results = [];
                                        cell.data('results', results);
                                    }

                                    // Ignore the 'total' column
                                    slice = row.slice(0);
                                    slice.remove(1);
                                    results.push(slice);

                                    if (i==0) continue; // header

                                    if (!seen) {
                                        seen = [];
                                        cell
                                            .data('seen', seen)
                                            .data('total', row[1])
                                            .data('totalName', curr.attr.attributes[1].displayName)
                                            .data('uniqueName', curr.dataset.displayName);
                                        cache.push((seen / row[1] * 100).toString());
                                    }

                                    if ($.inArray(row[0], seen) == -1) {
                                        seen.push(row[0]);
                                    }
                                }
                            } // for
                            self.element._resultsCache.push(cache);

                            finalizeColumn(aggr, curr.attr.name);
                        }
                    });
                }.partial(queries[i]));
            }
            queue.dequeue(biomart.MAX_CONCURRENT_REQUESTS);

            // Searches for any unloaded rows and tries to display an aggregate percentage of (col_0 / col_1)
            // Or if no results are found, displays a "X"
            function finalizeColumn (element, columnName) {
                if (!element) return;
                if (columnName) element = element.find('td.'+columnName);
                element.find('span.throbber').each(function() {
                    var $this = $(this),
                        cell = $this.parent(),
                        seen = cell.data('seen'),
                        total = parseInt(cell.data('total')),
                        totalName = cell.data('totalName'),
                        count,
                        html;

                    if (seen && seen.length && total) {
                        count = seen.length;
                        html = [
                            '<a href="javascript:;" rel="more">',
                                biomart.number.format(count/total * 100, {decimals: 2, suffix: '%'}),
                                ' (', count, '/', total, ')',
                            '</a>'
                        ].join('');

                        cell.html(html);
                    }
                    else if (seen && seen.length) cell.html(['<span class="zero">0.00%</span>'].join(''));
                    else cell.html(['<span class="empty">', _('empty_value'), '</span>'].join(''));
                });
            }
        },

        /* FREQUENCY */
        frequency: function(displayOptions) {
            var self = this,
                isHeader = true,
                seen = [],
                element = self.element,
                options = self.options,
                queries = options.queries,
                iframe = options.iframe,
                loading = options.loading || $('<span class="loading with-msg" style="display: none"/>').insertBefore(element),
                cache = [],
                truncateLongText = options.displayType == 'table',
                sourceOptions = {
                    url: BIOMART_CONFIG.service.url + 'results',
                    timeout: options.timeout,
                    data: {
                        query: queries
                    }
                },
                controllerOptions = $.extend(displayOptions, {
                    // data aggregation as they come in
                    preprocess: function(s) {
                        var row, id, ds;
                        if (biomart.errorRegex.test(s)) {
                            element.datacontroller('error');
                            return false;
                        }
                        if (s) {
                            row = s.split('\t');
                            id = row[row.length-1];
                            ds = row.length>3 ? row[row.length-2] : null;

                            if (isHeader) { // headers
                                isHeader = false;
                                return [row[0], 'Frequency', id].join('\t');

                            } else {
                                for (var i=0, curr; curr=cache[i]; i++) {
                                    if (curr.key == row[0]) break;
                                }

                                if (!curr) {
                                    curr = {
                                        key: row[0],
                                        dataset: ds,
                                        values: []
                                    };
                                    cache.push(curr);
                                }

                                if ($.inArray(id, seen) == -1) {
                                    curr.values.push(id);
                                    seen.push(id);
                                }

                                return false; // don't show streaming data
                            }
                        }
                    },
                    done: function(total, start, end) {
                        var results = [];

                        this._writee.addClass('frequency');

                        cache.sort(function(left, right) {
                            return right.values.length - left.values.length;
                        });

                        for (var i=0, values, curr, num; curr=cache[i]; i++) {
                            num = curr.values.length;
                            if (truncateLongText && num > 5) {
                                values = [
                                    '<span style="cursor: pointer" title="Show ', curr.values.length-5 ,' more" class="truncated" full="', curr.values.join(', '), '">',
                                        curr.values.splice(0,5).join(', '), '&hellip;',
                                    '</span>'
                                ].join('');
                            } else {
                                values = curr.values.join(', ');
                            }

                            results.push([
                                curr.key,
                                values,
                                num,
                                seen.length
                            ]);
                        }

                        if (results.length) {
                            this._renderer.parse(results, this._writee)
                            this._renderer._rawData = cache;
                            this._renderer.draw();
                        } else {
                            this._writee.html(['<p class="empty">', _('no_results'), '</p>'].join(''));
                        }

                        loading.fadeAndRemove();
                        if (options.done) options.done();
                    },
                    headers: true
                });

            loading.animate({opacity: 'show'}, 200);

            if (iframe) {
                sourceOptions.iframe = iframe;
                sourceOptions.type = 'streaming';
            }

            element
                .delegate('span.truncated', 'click.freq', function() {
                    var $this = $(this);
                    $this
                        .removeClass('truncated')
                        .css({'cursor': 'text'})
                        .attr('title', '')
                        .text($this.attr('full'));
                })
                .datasource(sourceOptions)
                .datacontroller(controllerOptions)
                .bind('datasource.success.queryResult', function(ev, s) {
                    self.element.datacontroller('write', s);
                })
                .one('datasource.complete', function() {
                    self.element
                        .unbind('datasource.success.queryResult')
                        .datacontroller('done');
                })
                .datasource('exec');
        },

        /* SUMMARY */
        summary: function(displayOptions) {
            var self = this,
                element = self.element,
                options = self.options,
                queries = options.queries,
                iframe = options.iframe,
                loading = options.loading || $('<span class="loading with-msg" style="display: none"/>').insertBefore(element),
                cache = [],
                headerMap = {},
                rowMap = {},
                headerIndex = 0, headers = true,
                truncateLongText = options.displayType == 'table',
                sourceOptions = {
                    url: BIOMART_CONFIG.service.url + 'results',
                    timeout: options.timeout,
                    data: {
                        query: queries
                    }
                },
                controllerOptions = $.extend(displayOptions, {
                    limit: biomart.DEFAULT_PAGE_SIZE,
                    skip: 0,
                    // data aggregation as they come in
                    preprocess: function(s) {
                        if (biomart.errorRegex.test(s)) {
                            element.datacontroller('error');
                            return false;
                        }
                        if (s) {
                            var row = s.split('\t'),
                                label = row[0],
                                count = parseInt(row[1]),
                                total = parseInt(row[2]),
                                key = row[row.length-1],
                                insertIndex;

                            if (options.displayOptions.extraLabel) {
                                key = [row[row.length-2], ' (', key, ')'].join('');
                            }

                            if (headers) { // headers
                                headers = false;
                                headerMap[key] = headerIndex++;
                                return false; // don't show streaming data
                            } else {
                                if (count) { // Ignore zero or non-numeric values
                                    var i = rowMap[key],
                                        curr;

                                    if (isNaN(i)) {
                                        curr = [key];
                                        cache.push(curr);
                                        i = rowMap[key] = cache.length-1;
                                    } else {
                                        curr = cache[i];
                                    }
                                    if (!headerMap[label]) {
                                        headerMap[label] = headerIndex++;
                                    }

                                    insertIndex = headerMap[label];

                                    curr[insertIndex] = options.displayOptions.useRaw ?
                                        {count:count, total:total} :
                                        ['<span class="icon check" data-count="',
                                                count, '" data-total="', total, '"/>'].join('');
                                }
                                return false; // don't show streaming data
                            }
                        }
                    },
                    done: function(total, start, end) {
                        var pageSize = this.options.displayType=='table' ? biomart.DEFAULT_PAGE_SIZE : Infinity;
                        if (cache.length) {
                            var headers = [];

                            this._writee.addClass('summary');

                            for (var k in headerMap) {
                                headers.push(k);
                            }

                            this._writeln(headers);

                            for (var i=0, row; row=cache[i]; i++) {
                                this._writeln(row);
                            }

                            // hack to load data by "paginating"
                            this.paginate(0, pageSize);

                            if (this._renderer.canPaginate) {
                                if (i > pageSize) {
                                    self._isPaginated = true;
                                    element.paginate({
                                        pageSize: pageSize,
                                        total: i,
                                        start: 1,
                                        end: pageSize,
                                        infoElement: $('<p class="info"/>').insertBefore(element)
                                    });
                                }
                            }
                        } else if (!element.datacontroller('hasError')) {
                            this._writee.html(['<p class="empty">', _('no_results'), '</p>'].join(''));
                        }

                        loading.fadeAndRemove();
                        if (options.done) options.done();
                    },
                    headers: true
                });

            loading.animate({opacity: 'show'}, 200);

            if (iframe) {
                sourceOptions.iframe = iframe;
                sourceOptions.type = 'streaming';
            }

            element
                .delegate('span.truncated', 'click.freq', function() {
                    var $this = $(this);
                    $this
                        .removeClass('truncated')
                        .css({'cursor': 'text'})
                        .attr('title', '')
                        .text($this.attr('full'));
                })
                .datasource(sourceOptions)
                .datacontroller(controllerOptions)
                .bind('datasource.success.queryResult', function(ev, s) {
                    self.element.datacontroller('write', s);
                })
                .one('datasource.complete', function() {
                    self.element
                        .unbind('datasource.success.queryResult')
                        .datacontroller('done');
                })
                .datasource('exec');
        }
    }
};

$.widget('ui.queryResults', QueryResults);

})(jQuery);

