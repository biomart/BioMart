_('BM.views').namespace(function(self) {
    self.GuiContainerView = Backbone.View.extend({
        initialize: function(options) {
            _.bindAll(this, 'render', 'update')
            this.model.bind('change', this.update)
            this.model.bind('change', this.render)
            this.model.view = this
        },

        update: function() {
            this.model.marts.refresh(this.model.get('marts'))
        },

        render: function() {
            this.$('.guiContainerName').text(this.model.get('displayName'))
            return this
        }
    })

    self.SelectBoxView = Backbone.View.extend({
        multiple: false,

        initialize: function(options) {
            _.bindAll(this, 'render', 'select')
            this.collection = options.collection
            this.collection.bind('refresh', this.render)
            this._optionViews = []

            // Render initial select box
            this.el.append(ich.selectBox({
                label: options.label,
                id: options.id,
                className: options.className
            }))
            this._select = this.$('select')
        },

        events: {
            'change select': 'select'
        },

        render: function() {
            this._select.prettybox('destroy').empty()

            this.collection.each(_(function(that, model) {
                var view = new BM.views.SelectBoxOptionView({model: model})
                that._select.append(view.render().el)
            }).partial(this))

            if (!this.multiple) {
                this._select.attr('multiple', false)
                this._select.prettybox()
            } else {
                this._select.attr('multiple', true)
            }

            this._select.trigger('change')
        },

        select: function(ev) {
            var value = this._select.val()
            this.collection.each(function(model) {
                if (model.get('name') == value) {
                    model.set({selected: true})
                } else {
                    model.set({selected: false})
                }
            })
        }
    })

    self.SelectBoxOptionView = Backbone.View.extend({
        initialize: function(options) {
            _.bindAll(this, 'render')
            this.model.bind('change', this.render)
        },

        render: function() {
            this.el = $('<option>' + this.model.get('name') + '</option>')
            return this
        }
    })

    self.AttributeView = Backbone.View.extend({
        tagName: 'li',
        className: 'model-attribute clearfix',

        _activeClassName: 'model-attribute-active',

        events: {
            'change input[type=checkbox]': 'updateAttribute'
        },

        initialize: function(options) {
            _.bindAll(this, 'updateCheckbox')
            this.model.bind('change', this.updateCheckbox)
        },

        render: function() {
            ich.attribute(this.model.toJSON()).appendTo(this.el)
            $(this.el)
                .addClass('model-attribute-' + this.model.get('name'))
                .data('view', this)
            return this
        },

        updateAttribute: function(evt) {
            this.model.set({'selected': evt.target.checked})
            if (this.model.get('selected')) {
                $(this.el).addClass(this._activeClassName)
                this.trigger('attribute_add', {model: this.model})
            } else {
                $(this.el).removeClass(this._activeClassName)
                this.trigger('attribute_remove', {model: this.model})
            }
            log('AttributeView: Updating attribute model', this.model.get('displayName'))
        },

        updateCheckbox: function() {
            var input = this.$('input[type=checkbox]')[0]
            input.checked = this.model.get('selected')
            log('AttributeView: Updating attribute checkbox', this.model.get('displayName'))
        }
    })

    self.FilterView = Backbone.View.extend({
        tagName: 'li',
        className: 'model-filter clearfix',

        _activeClassName: 'model-filter-active',

        events: {
            'click .filter-remove': 'removeFilter',
            'change .filter-field': 'validateFilter'
        },

        initialize: function(options) {
            _.bindAll(this, 'render', 'updateFilter')
            this.model.bind('change:selected', this.updateFilter)
        },

        render: function() {
            var valueList = this.model.get('values'),
                filterList = this.model.get('filterList').toJSON()

            ich.filter(_.extend(this.model.toJSON(), {
                filters: filterList,
                // Helper variables for template 
                isValid: this.isValid(),
                hasText: this.hasTextField(),
                hasFilters: filterList.length > 0,
                hasValues: valueList.length > 0,
                isMultiple: this.isMultiple(),
                hasUpload: this.hasUploadField()
            })).appendTo(this.el)

            this._checkbox = this.$('.filter-item-checkbox')

            $(this.el)
                .addClass(['filter-', this.model.get('type'), ' model-filter-', this.model.get('name')].join(''))

            // Add an invalid "Choose" option to select boxes
            this.$('select:not([multiple])')
                .prepend([ '<option value="">-- ', 
                        _('select').i18n(BM.i18n.CAPITALIZE), ' --</option>'
                    ].join(''))
                .val('')
                .prettybox()

            this.$('.filter-item-name')
                .append(':').bind('click.simplerfilter', function() {
                    // prevent checkbox from being checked/unchecked
                    return false
                })

            this.$('.filter-field-text')
                .addClass('ui-state-default ui-corner-all')
                .bind('focus.simplerfilter', function() { $(this).select() })

            this.$('.filter-field-upload-file').uploadify()

            this._closeButton = $(['<span class="ui-icon ui-icon-circle-close filter-remove" title="', 
                    _('remove').i18n(BM.i18n.CAPITALIZE), '"></span>'].join(''))
                .hide()
                .appendTo(this.el)

            return this
        },

        removeFilter: function() {
            log('FilterView: Removing filter', this.model.get('displayName'))
            this.model.set({selected: false, value: null})
            this.$('.filter-field').val('')
            this.$('.ui-autocomplete-input').val([
                '-- ', _('select').i18n(BM.i18n.CAPITALIZE), ' --'
            ].join(''))
        },

        // TODO: Need some validation here
        validateFilter: function() {
            var value = this.getValue()

            if (value) {
                log('FilterView: Setting filter', this.model.get('displayName'), value)
                this.model.set({selected: true, value: value})
            } else {
                log('FilterView: Removing filter', this.model.get('displayName'))
                this.model.set({selected: false, value: null})
            }
        },

        updateFilter: function() {
            log('FilterView: Updating filter view selection', this.model.get('displayName'))
            if (this.model.get('selected')) {
                $(this.el).addClass(this._activeClassName)
                this._closeButton.show()
                this.trigger('filter_add', {model: this.model})
            } else {
                $(this.el).removeClass(this._activeClassName)
                this._closeButton.hide()
                this.trigger('filter_remove', {model: this.model})
            }
        },

        _validationHandlers: {
            text: function() { 
                return true
            },
            upload: function() { 
                return true
            },
            singleSelect: function(model) {
                return !!model.get('values').length
            },
            singleSelectBoolean: function(model) {
                return !!model.get('filterList').length
            },
            singleSelectUpload: function(model) {
                return !!model.get('filterList').length
            },
            multiSelect: function(model) {
                return !!model.get('values').length
            },
            multiSelectBoolean: function(model) {
                return !!model.get('filterList').length
            },
            multiSelectUpload: function(model) {
                return !!model.get('filterList').length
            }
        },

        isValid: function() {
            var type = this.model.get('type'),
                handler = this._validationHandlers[type]
            if (handler) {
                return handler(this.model)
            }
            return false
        },
        hasUploadField: function() {
            var type = this.model.get('type')
            return /upload/i.test(type)
        },

        hasTextField: function() {
            var type = this.model.get('type')
            return type == 'text'
        },

        isMultiple: function() {
            var type = this.model.get('type')
            return /multi/i.test(type)
        },

        getValue: function() {
            return this.$('.filter-value').val()
        }
    })

    self.AttributeListView = Backbone.View.extend({
        tagName: 'ul',
        className: 'collection-attribute clearfix',

        initialize: function(options) {
            _.bindAll(this, 'render')
            this.collection = options.collection
            this.collection.bind('refresh', this.render)
        },

        render: function() {
            this.collection.each(_(function(that, model) {
                var view = new BM.views.AttributeView({ model: model })

                view.bind('all', function(evtName, data) {
                    that.trigger(evtName, data)
                })

                $(view.render().el).appendTo(that.el)

            }).partial(this))

            return this
        }
    })

    self.FilterListView = Backbone.View.extend({
        tagName: 'ul',
        className: 'collection-filter clearfix',

        initialize: function(options) {
            _.bindAll(this, 'render')
            this.collection = options.collection
            this.collection.bind('refresh', this.render)
        },

        render: function() {
            this.collection.each(_(function(that, model) {
                var view = new BM.views.FilterView({ model: model })

                $(view.render().el).appendTo(that.el)

                view.bind('all', function(evtName, data) {
                    that.trigger(evtName, data)
                })

            }).partial(this))

            return this
        }
    })

    self.ContainerView = Backbone.View.extend({
        initialize: function(options) {
            _.bindAll(this, 'render')
            this.model.bind('change', this.render)
            this._subContainerViews = []
        },

        render: function() {
            var that = this,
                filterList = this.model.get('filterList'),
                attributeList = this.model.get('attributeList'),
                containerList = this.model.get('containerList')

            if (this._rendered) {
                this._rendered.remove()
            }

            // Render THIS container
            this._rendered = ich.container(this.model.toJSON()).appendTo(this.el)

            // Render attributes
            if (attributeList.length) {
                this._attributesView = new BM.views.AttributeListView({
                    collection: attributeList
                })

                this._attributesView.bind('all', function(evtName, data) {
                    that.trigger(evtName, data)
                })

                this._rendered.append(this._attributesView.render().el)
            }

            // Render filters
            if (filterList.length) {
                this._filtersView = new BM.views.FilterListView({
                    collection: filterList
                })

                this._filtersView.bind('all', function(evtName, data) {
                    that.trigger(evtName, data)
                })

                this._rendered.append(this._filtersView.render().el)
            }

            // Render sub containers
            this.model.get('containerList').each(function(container) {
                var newView = new BM.views.ContainerView({
                    el: that._rendered,
                    model: container
                }).render()

                newView.bind('all', function(evtName, data) {
                    that.trigger(evtName, data)
                })

                that._subContainerViews.push(newView)
            })

            return this
        }
    })
})
