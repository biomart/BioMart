_('BM.views').namespace(function(self) {
    self.GuiContainerView = Backbone.View.extend({
        initialize: function(options) {
            _.bindAll(this, 'render', 'update')
            this.model.bind('change', this.update)
            this.model.bind('change', this.render)
            this.model.view = this
        },

        update: function() {
            this.model.marts.reset(this.model.get('marts'))
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
            this.collection.bind('reset', this.render)
            this._optionViews = []

            // Render initial select box
            this.el.append(render.selectBox({
                label: options.label,
                id: options.id,
                className: options.className
            }))
            this.$select = this.$('select')
        },

        events: {
            'change select': 'select'
        },

        render: function() {
            var i = 0

            this.$select.prettybox('destroy').empty()

            this.collection.each(_(function(that, model) {
                var view = new BM.views.SelectBoxOptionView({model: model}),
                    el = view.render().el.appendTo(that.$select)
                if (i == 0) {
                    el.attr('selected', true)
                }
                i++
            }).partial(this))

            if (!this.multiple) {
                this.$select.attr('multiple', false)
                this.$select.prettybox()
            } else {
                this.$select.attr('multiple', true)
            }

            this.$select.trigger('change')

            return this // for chaining
        },

        select: function(ev) {
            var value = this.$select.val()
            this.collection.each(function(model) {
                var name = model.get('name')
                if (( _.isArray(value) && _.include(value, name) )  || name == value) {
                    model.set({ selected: true })
                } else {
                    model.set({ selected: false })
                }
            })
            this.trigger('select')
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
            render.attribute(this.model.toJSON()).appendTo(this.el)
            $(this.el)
                .addClass('model-attribute-' + this.model.get('name'))
                .data('view', this)
            return this
        },

        updateAttribute: function(evt) {
            this.model.set({'selected': evt.target.checked})
            if (this.model.get('selected')) {
                $(this.el).addClass(this._activeClassName)
                this.trigger('add', {model: this.model})
            } else {
                $(this.el).removeClass(this._activeClassName)
                this.trigger('remove', {model: this.model})
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
                filterList = this.model.filterList.toJSON()

            render.filter(_.extend(this.model.toJSON(), {
                filters: filterList,
                // Helper variables for template 
                isValid: this.isValid(),
                hasText: this.hasTextField(),
                isMultiple: this.isMultiple(),
                hasUpload: this.hasUploadField()
            })).appendTo(this.el)

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

            this.$closeButton = $(['<span class="ui-icon ui-icon-circle-close filter-remove" title="', 
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
                this.$closeButton.show()
                this.trigger('add', {model: this.model})
            } else {
                $(this.el).removeClass(this._activeClassName)
                this.$closeButton.hide()
                this.trigger('remove', {model: this.model})
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
                return !!model.filterList.length
            },
            singleSelectUpload: function(model) {
                return !!model.filterList.length
            },
            multiSelect: function(model) {
                return !!model.get('values').length
            },
            multiSelectBoolean: function(model) {
                return !!model.filterList.length
            },
            multiSelectUpload: function(model) {
                return !!model.filterList.length
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
            this.collection.bind('reset', this.render)
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
            this.collection.bind('reset', this.render)
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
                filterList = this.model.filterList,
                attributeList = this.model.attributeList,
                containerList = this.model.containerList

            if (this.$rendered) {
                this.$rendered.remove()
            }

            // Render THIS container
            this.$rendered = render.container(this.model.toJSON()).appendTo(this.el)

            // Render attributes
            if (attributeList.length) {
                this._attributesView = new BM.views.AttributeListView({
                    collection: attributeList
                })

                this._attributesView.bind('all', function(evtName, data) {
                    that.trigger(evtName, data)
                })

                this.$rendered.append(this._attributesView.render().el)
            }

            // Render filters
            if (filterList.length) {
                this._filtersView = new BM.views.FilterListView({
                    collection: filterList
                })

                this._filtersView.bind('all', function(evtName, data) {
                    that.trigger(evtName, data)
                })

                this.$rendered.append(this._filtersView.render().el)
            }

            // Render sub containers
            this.model.containerList.each(function(container) {
                var newView = new BM.views.ContainerView({
                    el: that.$rendered,
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

    /*
     * Paginator view provides page links for a Paginator object.
     *
     * Events triggers:
     *     "page" - passes the page number (one-indexed) as the argument
     */
    self.PaginatorView = Backbone.View.extend({
        tagName: 'p',
        className: 'model-paginator',
        _defaultPagesToDisplay: 10,
        events: {
            'click .ui-page': 'page'
        },
        initialize: function() {
            _.bindAll(this, 'render')
            this.model.bind('change', this.render)
            this.$el = $(this.el)
        },
        render: function(model) {
            log('PaginatorView.render()')
            var currPage = model.get('currPage'),
                pages = model.get('pages'),
                options = { pages: [] },
                last = _.last(pages),
                start, end

            // no pages to render
            if (!pages.length) {
                return
            }

            // Put currPage in the middle of page links
            start = Math.max(1, currPage - Math.floor(this._defaultPagesToDisplay / 2) + 1)

            // If start + (# pages to display) puts us out of range
            if (start + this._defaultPagesToDisplay > last) {
                start = Math.max(1, last - this._defaultPagesToDisplay + 1)
            }

            end = Math.min(_.last(pages), start + this._defaultPagesToDisplay)

            // Figure out prev and next pages if applicable
            if (currPage > 1) options.prevPage = currPage - 1
            if (currPage < last) options.nextPage = currPage + 1

            // Populate pages array
            for (var i = start; i <= end; i++) {
                options.pages.push({ num: i, isActive: i == currPage })
                // Only display a set number of pages for pagination
                if (options.pages.length >= this._defaultPagesToDisplay) {
                    break
                }
            }

            // Attach rendered pages to view element
            this.$el.html( render.pages(options) )

            return this // for chaining
        },
        page: function(evt) {
            var clicked = $(evt.target),
                num = clicked.data('page')
            log('Page clicked', num)
            this.model.set({
                currPage: num
            })
            this.trigger('page', num)
        }
    })
})
