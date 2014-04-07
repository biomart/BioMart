_('BM.results.models').namespace(function(self) {
    self.ResultData = Backbone.Model.extend({
        defaults: {
            header: null,
            rows: []
        },

        reset: function(options) {
            var silent = options ? !!options.silent : false
            this.set({
                header: null,
                rows: []
            }, { silent: silent })
            if (!silent) {
                this.trigger('reset')
            }
            return this
        },

        /*
         * Argument row should be an array
         */
        addRow: function(row) {
            var newLength = this.get('rows').push(row)

            // Triggers with model arguments[0]
            this.trigger('change:rows', this)

            // Triggers with model arguments[0], the new row arguments[1], and new index arguments[2]
            this.trigger('add:row', this, row, newLength-1)

            return this
        },
        getRows: function(offset, numRows) {
            offset = offset || 0
            if (!numRows) {
                return this.get('rows').slice(offset)
            }
            return this.get('rows').slice(offset, offset + numRows)
        },
        _htmlRegex: /<.+?>/g,
        /*
         * Sorts the ResultData by the column specified by index (zero-based).
         *
         * If ascending is true then sort by ascending order, descending otherwise. Default when
         * not specified is descending.
         */
        sort: function(index, ascending){
            var rows = this.get('rows'),
                sorted = rows.sort(function(left, right) {
                    var a = left[index].replace(this._htmlRegex, '').toUpperCase(),
                        b = right[index].replace(this._htmlRegex, '').toUpperCase()
                    if (a > b) return ascending ? 1 : -1
                    if (a < b) return ascending ? -1 : 1
                    return 0
                })

            this.set({ rows: sorted }, { silent: true }) // silent because we'll trigger sortedBy change
            this.trigger('sort:rows', this, index, ascending)

            return this
        },
        getTotal: function() {
            return this.get('rows').length
        },
        done: function() {
            this.trigger('loaded', this)
            return this
        }
    })
})
