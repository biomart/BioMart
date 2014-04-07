describe("MartVisualCtrl", function () {
"use strict";

    var $scope, $rootScope, $location, mkController, $httpBackend, $routeParams,
        expect = chai.expect, ds, datasetsRes, martsRes, config;

    beforeEach("Load controllers module", module("martVisualEnrichment.controllers"));
    beforeEach("Set up", inject(function ($injector) {
        $rootScope = $injector.get("$rootScope");
        $httpBackend = $injector.get("$httpBackend");
        $location = $injector.get("$location");
        $scope = $rootScope.$new();
        $routeParams = {
            gui: "mygui", mart: "mymart"
        }
        ds = [{"name":"hsapiens_gene_ensembl","displayName":"Homo sapiens genes (GRCh37.p12)","description":"hsapiens_gene_ensembl","isHidden":false},{"name":"amelanoleuca_gene_ensembl","displayName":"Ailuropoda melanoleuca genes (ailMel1)","description":"amelanoleuca_gene_ensembl","isHidden":false}];
        datasetsRes = {"gene_ensembl_config_3_1_2": ds};
        martsRes = {"name":"Enrichement","displayName":"Enrichment","description":"Enrichement","guiType":"martexplorer","isHidden":false,"guiContainers":[],"marts":[{"name":"gene_ensembl_config_3_1_2","displayName":"gene_ensembl_config","description":"gene_ensembl_config_3_1_2","config":"gene_ensembl_config_3_1_2","isHidden":false,"operation":"SINGLESELECT","meta":"","group":"Enrichment Analysis"}]};
        config = "gene_ensembl_config_3_1_2";

        var $q = $injector.get("$q"), conf = { url: "/martservice" },
            bmservice = {
                marts: function () {
                    return $q.when(martsRes);
                },
                datasets: function () {
                    return $q.when(datasetsRes);
                }
            },
            $ctrl = $injector.get("$controller");

        mkController = function () {
            var c = $ctrl("MartVisualCtrl", {
                $scope: $scope, $routeParams: $routeParams,
                bmservice: bmservice
            });
            $rootScope.$apply();
            return c;
        };
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it ("gets all the datasets available", function () {
        mkController();
        expect($scope.datasets).to.have.length(2).and.be.eql(ds);
        expect($scope.selectedDataset).to.be.eql(ds[0]);
    });

    it ("sets the first dataset as selectedDataset", function () {
        mkController();
        expect($scope.selectedDataset).to.be.eql(ds[0]);
    });

    it ("sets config as the name of the configuration of the mart", function () {
        mkController();
        expect($scope.config).to.be.eql(config);
    })


});