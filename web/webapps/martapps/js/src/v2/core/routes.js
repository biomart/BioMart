_('BM.routes').namespace(function(self) {
    self.Router = Backbone.Router.extend({
        routes: {
            '*params': 'defaultRoute'
        },
        defaultRoute: function(params) {
            log('defaultRoute', params)
        }
    })
})
