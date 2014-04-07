describe("mvFilters", function () {
    var expect = chai.expect, $compile, $rootScope, $scope, filter, qb,
        $templateCache, elm;

    beforeEach(module("martVisualEnrichment.directives"));
    beforeEach(module(function ($provide) {
        qb = {
            q: {},
            setFilter: function (name, value) {
                this.q[name] = value;
            }
        };

        var sanitize = {
            stripTags: function (value) { return value }
        }

        $provide.value("queryBuilder", qb);
        $provide.value("sanitize", sanitize);
    }));
    beforeEach(inject(function (_$compile_, _$rootScope_, _$templateCache_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $templateCache = _$templateCache_;
        $scope = $rootScope.$new();

        $templateCache.put(
            'mart-visual-enrichment/app/partials/boolean-filter.html',
            window.__html__['app/partials/boolean-filter.html']);
        $templateCache.put(
            'mart-visual-enrichment/app/partials/text-filter.html',
            window.__html__['app/partials/text-filter.html']);
        $templateCache.put(
            'mart-visual-enrichment/app/partials/upload-filter.html',
            window.__html__['app/partials/upload-filter.html']);
        $templateCache.put(
            'mart-visual-enrichment/app/partials/single-select-upload-filter.html',
            window.__html__['app/partials/single-select-upload-filter.html']);

        $templateCache.put(
            'mart-visual-enrichment/app/partials/filter.html',
            window.__html__['app/partials/filter.html']);
        filter = {
            "name": "boolean filter test",
            "displayName": "P-Value",
            "description": "",
            "type": "boolean",
            "isHidden": false,
            "qualifier": "",
            "required": false,
            "function": "cutoff",
            "attribute": "transcript_count",
            "filters": [],
            "values": [],
            "parent": "Cut Off",
            "dependsOn": ""
        }
        $scope.filter = filter;
    }));

    it ("should instanciate a boolean filter", function () {
        elm = $compile('<div mv-filter="filter"></div>')($scope);
        $rootScope.$apply();
        expect(elm.children().find("boolean-filter")).to.have.length(1)
        expect(elm.children().find("text-filter")).to.have.length(0)
        expect(elm.children().find("uplaod-filter")).to.have.length(0)
        expect(elm.children().find("single-select-upload-filter")).to.have.length(0)
    });


});