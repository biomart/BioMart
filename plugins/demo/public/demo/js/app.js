_('BM.MartForm').namespace(function(app) {
    // Allow events
    _(app).extend(Backbone.Events)

    app.views = {}

    app.objects = {}

    app.init = function(options) {
        /*
         * Setup objects
         */
        app.objects.guiContainer = new BM.models.GuiContainer({
            name: options.guiName
        })

        app.objects.marts = app.objects.guiContainer.marts
        app.objects.datasets = new BM.models.DatasetList
        app.objects.filterRootContainer = new BM.models.Container({
            withfilters: true,
            withattributes: false
        })
        app.objects.attributeRootContainer = new BM.models.Container({
            withfilters: false,
            withattributes: true
        })

        /*
         * Setup views
         */
        this.views.guiContainer = new BM.views.GuiContainerView({
            model: app.objects.guiContainer,
            el: $('#biomart-wrapper')
        })

        this.views.marts = new BM.views.SelectBoxView({
            collection: app.objects.guiContainer.marts,
            el: $('#biomart-mart-list'),
            id: 'field-mart',
            className: 'martList',
            label: _('database').i18n(BM.i18n.CAPITALIZE)
        })

        this.views.datasets = new BM.views.SelectBoxView({
            collection: app.objects.datasets,
            el: $('#biomart-dataset-list'),
            id: 'field-dataset',
            className: 'datasetList',
            label: _('dataset').i18n(BM.i18n.CAPITALIZE | BM.i18n.PLURAL)
        })

        this.views.attributes = new BM.views.ContainerView({
            el: $('#biomart-attributes'),
            model: app.objects.attributeRootContainer
        })

        this.views.attributes.bind('all', function(evtName, data) {
            log('App: attribute event', evtName, data.model.get('displayName'))
        })

        this.views.filters = new BM.views.ContainerView({
            el: $('#biomart-filters'),
            model: app.objects.filterRootContainer
        })

        this.views.filters.bind('all', function(evtName, data) {
            log('App: filter event', evtName, data.model.get('displayName'))
        })


        /*
         * Events
         */
        app.objects.marts.bind('change:selected', function(mart) {
            if (!mart.get('selected')) return

            if (mart.get('operation') == BM.models.operations.MULTI_SELECT) {
                app.views.datasets.multiple = true
            } else {
                app.views.datasets.multiple = false
            }

            app.objects.datasets.fetch({
                data: 'mart='+mart.get('name')
            })
        })

        app.objects.datasets.bind('change:selected', function(datasets) {
            var mart = app.objects.marts.selected(),
                datasets = datasets.collection
            app.objects.attributeRootContainer.set({
                mart: mart,
                datasets: datasets
            }, {silent: true}).fetch()
            app.objects.filterRootContainer.set({
                mart: mart,
                datasets: datasets
            }, {silent: true}).fetch()
        })

        /*
         * Get GUI Container
         */
        app.objects.guiContainer.fetch()

        return this
    }
})
