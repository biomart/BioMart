_('BM.models').namespace(function(self) {
    self.MartList = Backbone.Collection.extend({
        model: BM.models.Mart,
        url: BM.conf.url + '/marts',
        selected: function() {
            return this.detect(function(mart) {
                return mart.get('selected')
            })
        }
    })

    self.DatasetList = Backbone.Collection.extend({
        model: BM.models.Dataset,
        url: BM.conf.url + '/datasets',
        initialize: function(options) {
            if (options && options.mart) {
                this.mart = options.mart
            }
        },

        toString: function() {
            var arr = []
            this.forEach(function(ds) {
                if (ds.get('selected')) {
                    arr.push(ds.get('name'))
                }
            })
            return arr.join(',')
        }
    })

    self.AttributeList = Backbone.Collection.extend({
        model: BM.models.Attribute,
        url: BM.conf.url + '/attributes'
    })

    self.FilterList = Backbone.Collection.extend({
        model: BM.models.Filter,
        url: BM.conf.url + '/filters'
    })

    self.ContainerList = Backbone.Collection.extend({
        model: BM.models.Container
    })
})
