
describe("mvSpecies directive", function () {
    chai.should();
    var mkCtrl, $rootScope, $scope, $loc, species, mkDir;

    beforeEach(module("martVisualEnrichment.directives"));

    beforeEach(inject(function ($injector) {
        var $templateCache = $injector.get("$templateCache");
        $templateCache.put('mart-visual-enrichment/app/partials/species.html', window.__html__['app/partials/species.html']);
        var $compile = $injector.get("$compile");
        $rootScope = $injector.get("$rootScope");
        $scope = $rootScope.$new();
        $loc = $injector.get("$location");

        species = [{name: "foo"}, {name: "bar"}];
        config = "configName";
        mkDir = function () {
            $scope.species = species;
            var d = $compile('<mv-species species="{{species}}"/>')($scope);
            $rootScope.$apply();
            return d;
        }
    }));

    afterEach(function () { $scope = null; });

    it ("should hold the species", function () {
        var d = mkDir();
        $scope.species.should.eql(species);
    });


    it ("has the name of the current species equal to the species url query", function () {
        $loc.search("species", species[0].name);
        var d = mkDir();
        var scope = d.scope();
        scope.currentSpeciesName.should.eql(species[0].name);
        $loc.search("species", species[1].name);
        scope.$apply();
        scope.currentSpeciesName.should.eql(species[1].name);
    });

    it ("should have selected the proper species", function () {
        $loc.search("species", species[0].name);
        var d = mkDir();
        var scope = d.scope();
        scope.selectedSpecies.should.eql(species[0]);
        $loc.search("species", species[1].name);
        scope.$apply();
        scope.selectedSpecies.should.eql(species[1]);
    });


    it ("should select the first species if the query url paramters is not a species", function () {
        $loc.search("species", species[0].name);
        var d = mkDir();
        var scope = d.scope();
        scope.selectedSpecies.should.eql(species[0]);
        $loc.search("species", "boooom");
        scope.$apply();
        scope.selectedSpecies.should.eql(species[0]);
    });


    describe("#updateSpecies(species)", function() {
        it ("updates the current species and the url query", function () {
            var d = mkDir();
            var scope = d.scope();
            scope.updateSpecies(species[1]);
            scope.currentSpeciesName.should.eql(species[1].name);
            $loc.search().should.have.property("species", species[1].name)
        });
    })
})