
describe("bmservice service", function () {
"use strict";

    var expect = chai.expect, $httpBackend, $rootScope, bm,
        conf = { url: "/martservice" };

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(module(function ($provide) {
        $provide.value("mvConfig", conf);
    }));
    beforeEach(inject(function (_$httpBackend_, _bmservice_) {
        $httpBackend = _$httpBackend_;
        bm = _bmservice_;

        $httpBackend.when("GET", "/martservice/gui.json?name=Enrichment").
            respond([]);
        $httpBackend.when("GET", "/martservice/datasets.json?config=my_cfg").
            respond([]);
        $httpBackend.when("GET", "/martservice/containers.json?datasets=my_dataset&config=my_conf").
            respond([]);
        $httpBackend.when("GET", "/martservice/containers.json?datasets=my_dataset&config=my_conf&withfilters=false&withattributes=true").
            respond([]);
        $httpBackend.when("GET", "/martservice/containers.json?datasets=my_dataset&config=my_conf&withfilters=true").
            respond([]);
        $httpBackend.when("GET", "/martservice/containers.json?datasets=my_dataset&config=my_conf&withfilters=true&withattributes=true").
            respond([]);
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });


    it ("#marts(guiContainer) requests marts to the server", function () {
        bm.marts("Enrichment");
        $httpBackend.expectGET("/martservice/gui.json?name=Enrichment");
        $httpBackend.flush();
    });

    it ("#datasets(mart) requests all the datasets of a mart", function () {
        bm.datasets("my_cfg");
        $httpBackend.expectGET("/martservice/datasets.json?config=my_cfg");
        $httpBackend.flush();
    });

    describe("#containers(datasets, config, withFilters, withAttributes)", function () {
        it ("without optional parameters doesn't ask for filters and attributes", function () {
            bm.containers("my_dataset", "my_conf");
            $httpBackend.expectGET("/martservice/containers.json?datasets=my_dataset&config=my_conf");
            $httpBackend.flush();
        });

        it ("with filters truthy, it asks for filters too", function () {
            bm.containers("my_dataset", "my_conf", "filters");
            $httpBackend.expectGET("/martservice/containers.json?datasets=my_dataset&config=my_conf&withfilters=true");
            $httpBackend.flush();
        });

        it ("with attributes truthy and filters falsy, it asks for attributes too", function () {
            bm.containers("my_dataset", "my_conf", null, 3);
            $httpBackend.expectGET("/martservice/containers.json?datasets=my_dataset&config=my_conf&withfilters=false&withattributes=true");
            $httpBackend.flush();
        });

        it ("with both optionals truthy, it asks for filters and attributes", function () {
            bm.containers("my_dataset", "my_conf", "filters", 3);
            $httpBackend.expectGET("/martservice/containers.json?datasets=my_dataset&config=my_conf&withfilters=true&withattributes=true");
            $httpBackend.flush();
        });
    });
});