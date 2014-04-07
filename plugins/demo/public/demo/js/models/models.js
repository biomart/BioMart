_('BM.models').namespace(function(self) {
    self.operations = {
        MULTI_SELECT: 'MULTISELECT',
        SINGLE_SELECT: 'SINGLESELECT'
    }

    self.GuiContainer = Backbone.Model.extend({
        url: function() {
            return BM.conf.url + (this.name ? '/gui?name=' + this.name : '/portal')
        },
        initialize: function(options) {
            options.name && (this.name = options.name)
            this.marts = new BM.models.MartList
        }
    })

    self.Mart = Backbone.Model.extend({
        defaults: {selected: false}
    })

    self.Dataset = Backbone.Model.extend({
        defaults: {selected: false}
    })

    self.Filter = Backbone.Model.extend({
        initialize: function() {
            this.set({
                selected: false,
                value: null,
                filterList: new BM.models.FilterList
            }, {silent: true})
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
                that.get('filterList').add(newFilter)
            })

            return this
        }
    })


    self.Attribute = Backbone.Model.extend({
        defaults: {selected: false},
        initialize: function() {
            this.set({
                attributeList: new BM.models.AttributeList,
            }, {silent: true})
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
                    datasets: this.get('datasets').toString(),
                    withattributes: this.get('withattributes'),
                    withfilters: this.get('withfilters')
                },
                config

            if (config = this.get('mart').get('config')) {
                params.config = config
            }

            return BM.conf.url + '/containers?' + $.param(params)
        },
        initialize: function() {
            _.bindAll(this, 'parse')

            this.set({
                containerList: new BM.models.ContainerList,
                attributeList: new BM.models.AttributeList,
                filterList: new BM.models.FilterList
            }, {silent: true})
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

            _.each(resp.containers, function(container) {
                var newContainer = new BM.models.Container().parse(container)
                that.get('containerList').add(newContainer)
            })

            _.each(resp.attributes, function(attribute) {
                var newAttribute = new BM.models.Attribute().parse(attribute)
                that.get('attributeList').add(newAttribute)
            })

            _.each(resp.filters, function(filter) {
                var newFilter = new BM.models.Filter().parse(filter)
                that.get('filterList').add(newFilter)
            })

            return this
       }
    })

    self.ProcessorGroup = Backbone.Model.extend({})

    self.Processor = Backbone.Model.extend({})

})
