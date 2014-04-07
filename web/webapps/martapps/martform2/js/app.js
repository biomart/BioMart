_('BM.MartForm').namespace(function(app) {
    _(app).extend(Backbone.Events) // Allow events on app

    // View and model instances
    var views = app.views = {},
        objects = app.objects = {},
        router = app.router = new BM.routes.Router()

    app.init = _.once(function(options) {
        /*
         * Setup objects
         */
        objects.guiContainer = new BM.models.GuiContainer({
            name: options.guiName
        })
        objects.marts = objects.guiContainer.marts
        objects.datasets = new BM.models.DatasetList
        objects.filterRootContainer = new BM.models.Container({
            withfilters: true,
            withattributes: false
        })
        objects.attributeRootContainer = new BM.models.Container({
            withfilters: false,
            withattributes: true
        })
        objects.query = new BM.query.models.Query

        /*
         * Setup views
         */
        views.guiContainer = new BM.views.GuiContainerView({
            model: objects.guiContainer,
            el: $('#biomart-wrapper')
        })

        views.marts = new BM.views.SelectBoxView({
            collection: objects.guiContainer.marts,
            el: $('#biomart-mart-list'),
            id: 'field-mart',
            className: 'martList',
            label: _('database').i18n(BM.i18n.CAPITALIZE)
        })

        views.datasets = new BM.views.SelectBoxView({
            collection: objects.datasets,
            el: $('#biomart-dataset-list'),
            id: 'field-dataset',
            className: 'datasetList',
            label: _('dataset').i18n(BM.i18n.CAPITALIZE | BM.i18n.PLURAL)
        })

        views.filters = new BM.views.ContainerView({
            el: $('#biomart-filters'),
            model: objects.filterRootContainer
        })

        views.attributes = new BM.views.ContainerView({
            el: $('#biomart-attributes'),
            model: objects.attributeRootContainer
        })

        views.results = new BM.results.views.ResultsPanelView({
            el: $('#biomart-results'),
            model: objects.query,
            dataViewClass: BM.results.views.TableDataView
        })

        views.query = new BM.query.views.QueryToolbarView({
            el: $('#biomart-content'),
            model: objects.query
        })

        /*
         * Events
         */
        views.filters.bind('add', function(data) {
            var mart = objects.marts.selected()
            log('App: filter add event', data.model.get('displayName'))
            objects.query.getElement(mart.get('config')).filterList.add(data.model)
        })
        views.filters.bind('remove', function(data) {
            var mart = objects.marts.selected()
            log('App: filter remove event', data.model.get('displayName'))
            objects.query.getElement(mart.get('config')).filterList.remove(data.model)
        })

        views.attributes.bind('add', function(data) {
            var mart = objects.marts.selected()
            log('App: attribute add event', data.model.get('displayName'))
            objects.query.getElement(mart.get('config')).attributeList.add(data.model)
        })
        views.attributes.bind('remove', function(data) {
            var mart = objects.marts.selected()
            log('App: attribute remove event', data.model.get('displayName'))
            objects.query.getElement(mart.get('config')).attributeList.remove(data.model)
        })

        views.marts.bind('select', function() {
            log('App: mart changed')

            var mart = this.collection.selected()

            if (!mart.get('selected')) return

            if (mart.get('operation') == BM.models.operations.MULTI_SELECT) {
                views.datasets.multiple = true
            } else {
                views.datasets.multiple = false
            }

            objects.datasets.fetch({ data: 'mart=' + mart.get('name') })
        })

        views.datasets.bind('select', function() {
            log('App: datasets changed')

            var mart = objects.marts.selected(),
                datasets = this.collection,
                datasetString = datasets.toString()

            if (datasetString) { // Only fetch if # selected datasets > 0
                // Remove prevous query elements
                objects.query.queryElements.each(function(queryElement) {
                    objects.query.removeElement(queryElement, { silent: true })
                })

                // Create new query element
                var queryElement = new BM.query.models.QueryElement({
                    config:  mart.get('config')
                })
                queryElement.datasetList.add(datasets.selected())
                objects.query.addElement(queryElement)

                // Load attributes and filters
                objects.attributeRootContainer
                    .set({ mart: mart, datasets: datasetString }, { silent: true })
                    .fetch()
                objects.filterRootContainer
                    .set({ mart: mart, datasets: datasetString }, { silent: true})
                    .fetch()
            }
        })

        views.results
            .bind('show', app.showResults)
            .bind('hide', app.hideResults)

        /*
         * Get GUI Container
         */
        objects.guiContainer.fetch()

        return app
    })

    // Functions for results view show/hide
    app.showResults = function() { this.el.siblings().slideUp({ duration: 200 }) }
    app.hideResults = function() { this.el.siblings().slideDown({ duration: 200 }) }

    Backbone.history.start()
})
