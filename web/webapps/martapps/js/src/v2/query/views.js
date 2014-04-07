_('BM.query.views').namespace(function(self) {
    self.QueryToolbarView = Backbone.View.extend({
        tagName: 'menu',
        className: 'biomart-query ui-widget ui-widget-content ui-corner-all clearfix',

        events: {
            'click .remove-dataset': 'removeDataset',
            'click .remove-filter': 'removeFilter',
            'click .remove-attribute': 'removeAttribute',
            'click .biomart-compile-target': 'showQuery'
        },

        initialize: function(options) {
            // automatically render toolbar
            this.render()
        },

        render: function() {
            log('QueryView.render')

            var targets = [
                    { target: 'xml', label: 'REST/SOAP' },
                    { target: 'sparql', label: 'SPARQL' },
                    { target: 'java', label: 'Java' }
                ]

            this._toolbar = render.query({
                compilationTargets: targets
            }).prependTo( this.el )

            this._dialogs = {}

            for (var i = 0, target; target = targets[i]; i++) {
                this._dialogs[target.target] = new self.ExplainQueryView(target)
            }

            return this
        },

        removeDataset: function() {},

        removeFilter: function() {},

        removeAttribute: function() {},

        showQuery: function(evt) {
            log('QueryInfoView.showQuery')

            var $target = $(evt.target).closest('.biomart-compile-target'),
                compilationTarget = $target.data('target'),
                source = this.model.compile(compilationTarget)

            this._dialogs[compilationTarget].source(source).open()
        },

        remove: function() {
            this.__super__('remove')
        }
    })

    self.ExplainQueryView = Backbone.View.extend({
        tagName: 'div',

        events: {
            'click .close': 'close',
            'click .escapeQuotes': 'escapeQuotes'
        },

        initialize: function(options) {
            this._target = options.target
            this._label = options.label

        },

        source: function(source) {
            log('ExplainQueryView.source', source)
            this._source = source
            return this
        },

        open: function() {
            // Lazy creation of dialog when needed
            if (!this.$dialog) {
                this.$dialog = render.queryDialog({
                        title: this._label
                    })
                    .appendTo(document.body)
                    .dialog({
                        dialogClass: 'biomart-query',
                        autoOpen: false,
                        modal: true,
                        width: 700,
                        height: 450,
                        buttons: {
                            'Close': function() {
                                $(this).dialog('close')
                            }
                        }
                    })
            }

            this.$dialog
                .find('textarea').text(this._source).end()
                .dialog('open')

            return this
        },

        close: function() {
            this.$dialog.dialog('close')
            return this
        },

        escapeQuotes: function() {
        },

        remove: function() {
            this.__super__('remove')
            if (this.$dialog) {
                this.$dialog.dialog('destroy').remove()
            }
        }
    })
})
