/*
 * This modules containers the core BioMart models and collections
 */
_('BM.models').namespace(function(self) {
    /*
     * Models
     */
    self.operations = {
        MULTI_SELECT: 'MULTISELECT',
        SINGLE_SELECT: 'SINGLESELECT'
    }

    self.GuiContainer = Backbone.Model.extend({
        url: function() {
            return BM.conf.service.url + (this.name ? 'gui?name=' + this.name : 'portal')
        },
        initialize: function(options) {
            options.name && (this.name = options.name)
            this.marts = new BM.models.MartList
        }
    })

    self.Mart = Backbone.Model.extend({
        defaults: {selected: false, config: 'default'}
    })

    self.Dataset = Backbone.Model.extend({
        defaults: {selected: false}
    })

    self.Filter = Backbone.Model.extend({
        initialize: function() {
            this.set({
                selected: false,
                value: null
            }, {silent: true})

            this.filterList = new BM.models.FilterList
        },
        parse: function(resp) {
            var that = this

            this.set({
                id: that.cid,
                name: resp.name,
                displayName: resp.displayName,
                description: resp.description,
                type: resp.type,
                isHidden: resp.isHidden,
                values: resp.values
            }, {silent: true})

            _.each(resp.filters, function(filter) {
                var newFilter = new BM.models.Filter
                newFilter.parse(filter)
                that.filterList.add(newFilter)
            })

            return this
        }
    })


    self.Attribute = Backbone.Model.extend({
        defaults: {selected: false},
        initialize: function() {
            this.attributeList = new BM.models.AttributeList
        },
        parse: function(resp) {
            this.set({
                id: this.cid,
                name: resp.name,
                displayName: resp.displayName,
                description: resp.description,
                isHidden: resp.isHidden,
                value: resp.value,
                linkURL: resp.linkURL
            }, {silent: true})
            return this
        }
    })

    self.Container = Backbone.Model.extend({
        url: function() {
            var params = {
                    datasets: this.get('datasets'),
                    withattributes: this.get('withattributes'),
                    withfilters: this.get('withfilters')
                },
                config

            if (config = this.get('mart').get('config')) {
                params.config = config
            }

            return BM.conf.service.url + 'containers?' + $.param(params)
        },
        initialize: function() {
            _.bindAll(this, 'parse')

            this.containerList = new BM.models.ContainerList
            this.attributeList = new BM.models.AttributeList
            this.filterList = new BM.models.FilterList
        },
        parse: function(resp) {
            var that = this

            this.set({
                id: that.cid,
                name: resp.name,
                displayName: resp.displayName,
                independent: resp.independent,
                description: resp.description,
                maxContainers: resp.maxContainers,
                maxAttributes: resp.maxAttributes
            }, {silent: true})

            this.containerList.reset(
                _.map(resp.containers, function(container) {
                    return new BM.models.Container().parse(container)
                })
            )

            this.attributeList.reset(
                _.map(resp.attributes, function(attribute) {
                    return new BM.models.Attribute().parse(attribute)
                })
            )

            this.filterList.reset(
                _.map(resp.filters, function(filter) {
                    return new BM.models.Filter().parse(filter)
                })
            )

            return this
        }
    })

    /* 
     * One-based index of pages
     */
    self.Paginator = Backbone.Model.extend({
        defaults: {
            pages: [],
            currPage: 1
        },
        reset: function(options) {
            var silent = options ? !!options.silent : false
            this.set({
                pages: [],
                currPage: 1
            }, { silent: silent })
        }
    })

    /* 
     * Collections
     */
    self.MartList = Backbone.Collection.extend({
        model: BM.models.Mart,
        url: BM.conf.service.url + 'marts',
        selected: function() {
            return this.detect(function(mart) {
                return mart.get('selected')
            })
        }
    })

    self.DatasetList = Backbone.Collection.extend({
        model: BM.models.Dataset,
        url: BM.conf.service.url + 'datasets',
        initialize: function(options) {
            if (options && options.mart) {
                this.mart = options.mart
            }
        },

        selected: function() {
            return this.models.filter(function(ds) {
                return !!ds.get('selected')
            })
        },

        hasSelected: function() {
            return this.models.some(function(ds) {
                return !!ds.get('selected')
            })
        },

        toString: function() {
            var arr = []
            this.models.forEach(function(ds) {
                if (ds.get('selected')) {
                    arr.push(ds.escape('name'))
                }
            })
            return arr.join(',')
        }
    })

    self.AttributeList = Backbone.Collection.extend({
        model: BM.models.Attribute,
        url: BM.conf.service.url + 'attributes'
    })

    self.FilterList = Backbone.Collection.extend({
        model: BM.models.Filter,
        url: BM.conf.service.url + 'filters'
    })

    self.ContainerList = Backbone.Collection.extend({
        model: BM.models.Container
    })
})
