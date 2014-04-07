
describe("Species Controller", function () {
    var mkCtrl, $rootScope, $scope, $loc, species, config, expect = chai.expect;

    beforeEach(module("martVisualEnrichment.controllers"));
    beforeEach(inject(function ($injector) {
        $rootScope = $injector.get("$rootScope");
        $loc = $injector.get("$location");
        var $ctrl = $injector.get("$controller");

        species = [{name: "foo"}, {name: "bar"}];
        config = "configName";

        mkCtrl = function () {
            $scope = $rootScope.$new();
            $scope.species = species;
            var c = $ctrl("SpeciesCtrl", { $scope: $scope });
            $rootScope.$apply();
            return c;
        };
    }));

    afterEach(function () { $scope = null; });

    it ("should select the species with name as the species url query paramter", function () {
        $loc.search("species", "bar");
        var c = mkCtrl();
        expect($scope.selectedSpecies).to.eql(species[1]);
    });

    describe("#updateSpecies(species)", function () {
        it ("should update the url query", function () {
            $loc.search("species", "bar");
            var c = mkCtrl();
            expect($scope.selectedSpecies).to.eql(species[1]);
            c.updateSpecies(species[0])
            expect($loc.search().species).to.eql(species[0].name);
        })
    })

});