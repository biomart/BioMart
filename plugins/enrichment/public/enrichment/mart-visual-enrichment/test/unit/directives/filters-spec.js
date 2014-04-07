describe("biomart filters", function () {
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
    }));


    function testTitle() {
        expect(elm.find("h3").text()).to.eql(filter.displayName);
    }

    function testDescription() {
        expect(elm.find("p").text()).to.eql(filter.description);
    }

    describe ("booleanFilter", function () {

        beforeEach(function() {
            $templateCache.put(
                'mart-visual-enrichment/app/partials/boolean-filter.html',
                window.__html__['app/partials/boolean-filter.html']);
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
            elm = $compile('<boolean-filter filter="filter" />')($scope);
            $rootScope.$apply();
        });


        it ("should have the right title", testTitle);


        it ("should have the right description", testDescription);


        it ("pushes its state to the queryBuilder when input content changes", function () {
            elm.find("input").eq(0).triggerHandler("click");
            expect(qb.q).to.have.property(filter.name, filter);
            expect(filter).to.have.property("value", "only");
            elm.find("input").eq(1).triggerHandler("click");
            expect(qb.q).to.have.property(filter.name, filter);
            expect(filter).to.have.property("value", "excluded");
        });
    });


    describe ("textFilter", function () {

        beforeEach(function() {
            $templateCache.put(
                'mart-visual-enrichment/app/partials/text-filter.html',
                window.__html__['app/partials/text-filter.html']);
            filter = {
                "name": "text filter test",
                "displayName": "P-Value",
                "description": "",
                "type": "text",
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
            elm = $compile('<text-filter filter="filter" />')($scope);
            $rootScope.$apply();
        });


        it ("should have the right title", testTitle);


        it ("should have the right description", testDescription);


        it ("pushes its state to the queryBuilder when input content changes", function () {
            var i = elm.find("input");
            i.val("booom")
            i.triggerHandler("change");
            expect(qb.q).to.have.property(filter.name, "booom");
        });


        it ("when the input text is empty it sets its value as null", function () {
            var i = elm.find("input");
            i.val("")
            i.triggerHandler("change");
            expect(qb.q).to.have.property(filter.name).and.be.null;
        });
    });


describe("uploadFilter", function () {

    beforeEach(function() {
        $templateCache.put(
            'mart-visual-enrichment/app/partials/upload-filter.html',
            window.__html__['app/partials/upload-filter.html']);
        filter = {
            "name": "upload filter test",
            "displayName": "P-Value",
            "description": "",
            "type": "upload",
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
        elm = $compile('<upload-filter filter="filter" />')($scope);
        $rootScope.$apply();
    });

    it ("should have the right title", testTitle);


    it ("should have the right description", testDescription);

    it ("pushes its value to queryBuilder when the textarea value changes", function() {
        var t = elm.find("textarea");
        t.val("booom");
        t.triggerHandler("change");
        expect(qb.q).to.have.property(filter.name, "booom");
        t.val("");
        t.triggerHandler("change");
        expect(qb.q).to.have.property(filter.name).and.be.null;
        t.val("false");
        t.triggerHandler("change");
        expect(qb.q).to.have.property(filter.name, "false");
    });

});

describe("singleSelectUploadFilter", function () {

    beforeEach(function() {
        $templateCache.put(
            'mart-visual-enrichment/app/partials/single-select-upload-filter.html',
            window.__html__['app/partials/single-select-upload-filter.html']);
        filter = fixtures.reqs().sets.filters[0]
        $scope.filter = filter;
        elm = $compile('<single-select-upload-filter filter="filter" />')($scope);
        $rootScope.$apply();
    });

    it ("should have the right title", testTitle);


    it ("should have the right description", testDescription);


    it ("should initially put the first filter of the list as selected", function () {
        var scope = elm.scope();
        expect(scope.options).to.have.length(2).and.eql(filter.filters);
        expect(scope.selected).to.eql(filter.filters[0]);
    });


    it ("when the textarea value changes it push the value of the currentrly selected filter to queryBuilder", function() {
        var scope = elm.scope(), t = elm.find("textarea");
        t.val("booom");
        t.triggerHandler("change");
        expect(qb.q).to.have.property(scope.selected.name, "booom");
        t.val("")
        t.triggerHandler("change");
        expect(qb.q).to.have.property(scope.selected.name).and.be.null;
    });

    // it ("when another filter is selected, it set the value of the previous filter to null and push the new one", function () {
    //     var scope = elm.scope(), t = elm.find("textarea");
    //     t.val("booom");
    //     t.triggerHandler("change");
    //     expect(qb.q).to.have.property(scope.selected.name, "booom");
    //     elm.find("select").prop("selectedIndex", 2);
    //     expect(scope.selected).to.eql(filter.filters[1]);
    // })

});

});