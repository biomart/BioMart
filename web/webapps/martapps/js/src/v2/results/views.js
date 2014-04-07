_('BM.results.views').namespace(function(self) {
    window.BM_callbacks = {}

    self.ResultsPanelView = Backbone.View.extend({
        _hiddenClassName: 'hidden',

        events: {
            'click .biomart-results-show': 'showResults',
            'click .biomart-results-hide': 'hideResults'
        },

        _isOnHeaders: false,

        initialize: function(options) {
            _.bindAll(this, 'callback', 'done')

            this._iframeId = _.uniqueId('BioMart')

            // If data view is not specified, use plaintext
            this._dataViewClass = options.dataViewClass || self.PlainDataView
            this._dataViewOptions = options.dataViewOptions || {}   // set options if exist
            this._dataModel = new BM.results.models.ResultData()    // instantiate data model
            this._dataView = new this._dataViewClass(_.extend({     // instantiate data view
                model: this._dataModel
            }, this._dataViewOptions))

            this.$content = $(this.el).find('.biomart-results-content')
                .addClass(this._hiddenClassName)

            this.$loading = $('<span class="loading"/>').hide().appendTo(this.el)

            // iframe for streaming results from server
            this.$iframe = $('<iframe class="streaming" name="' + this._iframeId + '"/>').appendTo(this.$content)
            
            this.$submitButton = this.$('.biomart-results-show')
            this.$backButton = this.$('.biomart-results-hide')

            // form for submitting query
            this.$form = $('<form class="streaming" method="POST" target="' + this._iframeId + '" ' + 
                'action="' + BM.conf.service.url + 'results">' +
                '<input type="hidden" name="query"/>' +
                '<input type="hidden" name="iframe" value="true"/>' +
                '<input type="hidden" name="scope" value="' + 'BM_callbacks.' + this._iframeId + '"/>' +
                '<input type="hidden" name="uuid" value="' + this._iframeId + '"/>' +
                '</form>').appendTo(this.$content)

            window.BM_callbacks[this._iframeId] = {
                write: this.callback,
                done: this.done
            }
        },

        callback: function(uuid, row) {
            // Check that the row is not empty
            if (row) {
                row = row.split('\t')
                if (this._isOnHeaders) {
                    this._dataModel.set({ header: row })
                    this._isOnHeaders = false
                } else {
                    this._dataModel.addRow(row)
                }
            }
        },

        done: function(uuid) {
            this._dataModel.done()
            this.$loading.hide()
        },

        showResults: function() {
            this.trigger('show')

            // Whether we need to handle headers first
            if (this.model.get('header')) {
                this._isOnHeaders = true
            }

            this.$content
                .append(this._dataView.render().el)
                .removeClass(this._hiddenClassName)

            this.$form
                .find('input[name=query]')
                    .val(this.model.compileForPreview())
                .end()
                .submit()

            // Switch user controls
            this.$submitButton.hide()
            this.$backButton.show()

            this.$loading.show()

            return this
        },

        hideResults: function() {
            this.trigger('hide')

            this.$content.addClass(this._hiddenClassName)

            // Reset controls
            this.$submitButton.show()
            this.$backButton.hide()

            // Clear out results
            this._dataModel.reset()

            return this
        }
    })

    /*
     * PlainDataView renders a ResultData object in a <pre/> element in plaintext.
     *
     * Other data views extend this prototype.
     */
    self.PlainDataView = Backbone.View.extend({
        tagName: 'pre',
        className: 'biomart-results-plaintext',
        _noDataClassName: 'biomart-results-nodata',
        initialize: function(options) {
            _.bindAll(this, 'updateHeader', 'displayRow', 'done', 'render', 'clear')
            this.$el = $(this.el)
            
            this.model.bind('change:header', this.updateHeader)
            this.model.bind('add:row', this.displayRow)
            this.model.bind('reset', this.clear)
            this.model.bind('loaded', this.done)
        },
        render: function() {
            log('PlainDataView.render()')
            return this
        },
        updateHeader: function(model) {
            this.$el.append(model.headers.join('\t') + '\n')
        },
        displayRow: function(model, row, index) {
            this.$el.append(row.join('\t') + '\n')
        },
        done: function(model) {
            log('PlainDataView.done()')
            // If no data returned, show "No data" message
            if (model.getTotal() == 0) {
                this.$el
                    .html(_('no data').i18n(BM.i18n.CAPITALIZE))
                    .addClass(this._noDataClassName)
            }
        },
        clear: function() {
            log('PlainDataView.clear()')
            this.$el.removeClass(this._noDataClassName)
            this.$thead.empty()
            this.$tbody.empty()
        }
    })

    /*
     * Displays ResultData as a <table/> element.
     *
     * Supports pagination if the total rows returned > pageSize.
     */
    self.TableDataView = self.PlainDataView.extend({
        tagName: 'table',
        className: 'biomart-results-table',

        events: {
            'click .ui-table-sortable': 'sortResultData'
        },

        _currPage: 1, // Cache of paginator's current page (for faster row display)
        _pageSize: 20, // Default to 20 rows per page

        initialize: function(options) {
            _.bindAll(this, '_scrollToPage', '_updateCurrPage', '_updateResultsInfo', '_displaySortedData')

            this.__super__('initialize')

            if ('pageSize' in options) {
                log('Paginating by ' + options.pageSize)
                this._pageSize = options.pageSize
            }

            this._paginatorModel = new BM.models.Paginator()
            this._paginatorView = new BM.views.PaginatorView({ model: this._paginatorModel })

            this.$thead = $('<thead/>').appendTo(this.$el)
            this.$tbody = $('<tbody/>').appendTo(this.$el)

            // This ordering is important because _currPage needs to be updated first before all other callbacks
            this._paginatorModel.bind('change:currPage', this._updateCurrPage)
            this._paginatorModel.bind('change:currPage', this._scrollToPage)
            this._paginatorModel.bind('change:currPage', this._updateResultsInfo)

            this.model.bind('sort:rows', this._displaySortedData)
        },
        render: function() {
            this.__super__('render')
            return this
        },
        clear: function() {
            this.__super__('clear')
            log('TableDataView.clear()')
            if (this.$info) {
                this.$info.remove()
                delete this.$info
            }
            this._paginatorModel.reset()
            this._paginatorView.$el.empty()
            this.model.reset({ silent: true })
            delete this._sortedColumnIndex
        },
        updateHeader: function(model) {
            var arr = [],
                header = model.get('header')
            if (header) {
                for (var i = 0; i < header.length; i++) {
                    arr.push({ text: header[i], sortable: true })
                }
                this.$thead.append( render.tableHeader({ header: arr }) )
            }
        },
        displayRow: function(model, row, index) {
            // Figure out start and end ranges (if pageSize is set)
            // Range is inclusive i.e. [start, end] not (start, end)
            if (this._pageSize) {
                var range = this._getRowsRange()
                // Don't dislay if row index falls outside of range
                if (index < range.start || index > range.end) {
                    return
                }
            }
            this._appendRow(row)
        },
        // Appends the row array to the table
        _appendRow: function(row) {
            // If we know how many columns to expect, then initialize array to that size.
            // This will allow use to print empty columns even if the data doesn't come back.
            var arr = []
            for (var i = 0; i < row.length; i++) {
                arr.push(row[i])
            }
            this.$tbody.append( render.tableRow({ row: arr }) )
        },
        done: function(resultDataModel) {
            this.__super__('done', resultDataModel)
            log('TableDataView.done()')

            var total = resultDataModel.getTotal(),
                pages = []

            // Make sure we have results to display
            if (total > 0) {
                var range = this._getRowsRange()

                // Display meta info
                this.$info = render.resultsMeta({
                    start: range.start,
                    end: range.end,
                    total: total,
                    limit: BM.PREVIEW_LIMIT, // defined in core.js
                    hasMoreData: total >= BM.PREVIEW_LIMIT // We have to assume that there are
                                                           // more data available if we've reached
                                                           // the limit. It is possible that 
                                                           // the total results == limit though.
                })
                this.$el.before(this.$info)

                // If we have more rows than the page size then display pagination links
                if (this._pageSize && total >= this._pageSize) {
                    log('Total rows: ', total)
                    var i = 0 // initial page index

                    do {
                        pages.push(++i) // create one-based indices
                    } while ((total -= this._pageSize) > 0)

                    this._paginatorModel.set({ pages: pages })

                    this.$el.after(this._paginatorView.el)
                }

            } else {
                // TODO: handle empty case
            }
        },
        _sortClassNames: { asc: 'ui-table-ascending', desc: 'ui-table-descending' },
        sortResultData: function(evt) {
            log('TableDataView.sortResultData()')
            var $target = $(evt.target),
                $column = $target.closest('.ui-table-sortable'),
                isCurrentlyAscending = $column.hasClass(this._sortClassNames.asc),
                index = $column.index()

            // Remove sort class names from sibling elements
            var that = this
            $column.siblings().each(function(index, element) {
                $(element)
                    .removeClass(that._sortClassNames.asc)
                    .removeClass(that._sortClassNames.desc)
            })

            // Toggle between ascending and descending class names
            if (isCurrentlyAscending) {
                $column
                    .removeClass(this._sortClassNames.asc)
                    .addClass(this._sortClassNames.desc)
            } else {
                $column
                    .removeClass(this._sortClassNames.desc)
                    .addClass(this._sortClassNames.asc)
            }

            // Sort ResultData
            this.model.sort(index, !isCurrentlyAscending)

            this._sortedColumnIndex = index
            this._highlightColumn(index)
        },
        /*
         * Highlights the <td/> element specified by the index (zero-based)
         */
        _highlightColumn: function(columnIndex) {
            this.$('tr').each(function(index, element) {
                var $columns = $(element).children('td')
                $columns.removeClass('ui-table-highlight')
                $columns.eq(columnIndex).addClass('ui-table-highlight')
            })
        },
        /*
         * Returns the start and end row indicies based on current page number
         */
        _getRowsRange: function() {
            var start = (this._currPage - 1) * this._pageSize + 1,
                end = Math.min(start + this._pageSize - 1, this.model.getTotal())
            return { start: start, end: end }
        },
        /*
         * Callback when Paginator's currPage attribute changes. Keeps cached _currPage in sync.
         */
        _updateCurrPage: function(paginatorModel, newPage) {
            this._prevPage = this._currPage
            this._currPage = newPage
        },
        /* 
         * Scrolls to the new page
         */
        _scrollToPage: function(paginatorModel, newPage) {
            var direction = newPage < this._prevPage ? 1 : -1 // used for setting left margin

            log('Displaying table page', this._currPage)

            var w = this.$el.outerWidth() + 100,
                $parent = this.$el.parent()

            // Temporarily fix the height for smoother animation
            $parent.height($parent.height())

            var that = this
            this.$el.animate({ marginLeft: w * direction }, {
                duration: 100,
                complete: function() {
                    that._refreshCurrentPage()
                    // Put table on the other side
                    that.$el.css({ marginLeft: w * direction * -1 })
                    // Bring table back
                    that.$el.animate({ marginLeft: 0 }, {
                        duration: 100,
                        complete: function() {
                            $parent.height('auto')
                        }
                    })
                }
            })
        },
        /*
         * Refresh results from the ResultData for the current page. Used when pagination or sorting occurs.
         */
        _refreshCurrentPage: function() {
            var start = (this._currPage - 1) * this._pageSize,
                rows = this.model.getRows(start, this._pageSize) // model is ResultData
            this.$tbody.empty() // clear out old results
            // Scroll the page out of sight
            for (var i = 0, row; row = rows[i]; i++) {
                this._appendRow(row)
            }
            if ('_sortedColumnIndex' in this) {
                this._highlightColumn(this._sortedColumnIndex)
            }
        },
        /*
         * Updates the info text
         */
        _updateResultsInfo: function(paginatorModel) {
            if (this.$info) {
                var range = this._getRowsRange() 
                this.$info.find('.start').text(range.start)
                this.$info.find('.end').text(range.end)
            }
        },
        /*
         * Callback for when user clicks on a column header
         */
        _displaySortedData: function(resultDataModel, index, ascending) {
            // Go back to page 1, which will also display the sorted results
            this._paginatorModel.set({ currPage: 1 })
            this._refreshCurrentPage()
        }
    })
})
