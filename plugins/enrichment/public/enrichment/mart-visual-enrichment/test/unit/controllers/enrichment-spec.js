
describe("Enrichment Controller", function () {
    var expect = chai.expect;
    var mkCtrl, $rootScope, $scope, $loc, species, config, $q, bm, containers;

    beforeEach(module("martVisualEnrichment.controllers"));
    beforeEach(inject(function ($injector) {
        $q = $injector.get("$q");
        $loc = $injector.get("$location");
        $rootScope = $injector.get("$rootScope");
        var $ctrl = $injector.get("$controller");

        species = [{name: "foo"}, {name: "bar"}];
        config = "configName";
        containers = fixtures.containers();
        bm = {
            containers: function() {
                return $q.when({
                    data: containers
                });
            }
        };

        var finder = function () {
            return {
                addFunctions: function (){ return this },
                find: function () {
                    return fixtures.reqs();
                }
            }
        }

        mkCtrl = function () {
            $scope = $rootScope.$new();
            var c = $ctrl("EnrichmentCtrl", { $scope: $scope, $location: $loc, bmservice: bm, findBioElement: finder });
            $rootScope.$apply();
            return c;
        };
    }));

    afterEach(function () { $scope = null; });

    it ("should initially not have containers", function () {
        var c = mkCtrl();
        expect(c).to.not.have.property("containers");
    });

    // it ("should get containers on url query change", function () {
    //     var c = mkCtrl();
    //     $loc.search({species: species[0].name, config: config});
    //     $scope.$apply();
    //     expect(c.containers).to.eql(containers);
    // });

    describe("#getFilter", function () {
        var c, reqs;
        beforeEach(function () {
            reqs = fixtures.reqs();
            c = mkCtrl();
            $loc.search({species: species[0].name, config: config});
            $scope.$apply();
        });


        it ("returns the first filter with function as specified", function () {
            expect(c.getFilter("background")).to.eql(reqs.background.filters[0]);
            expect(c.getFilter("sets")).to.eql(reqs.sets.filters[0]);
            expect(c.getFilter("cutoff")).to.eql(reqs.cutoff.filters[0]);
        });


        it ("returns null if there is not a filter with that function", function () {
            expect(c.getFilter("annotation")).to.be.null;
        });
    });


    describe("#getAttributes", function () {
        var c, reqs;
        beforeEach(function () {
            reqs = fixtures.reqs();
            c = mkCtrl();
            $loc.search({species: species[0].name, config: config});
            $scope.$apply();
        });


        it ("returns all the attributes with function as specified", function () {
            expect(c.getAttributes("annotation")).to.eql(reqs.annotation.attributes);
        });


        it ("returns [] if there is not an attribute with that function", function () {
            expect(c.getAttributes("sets")).to.be.empty;
        });
    });

    // describe("#set(funcName, funcValue)", function () {
    //     var c, reqs;
    //     beforeEach(function () {
    //         reqs = fixtures.reqs();
    //         c = mkCtrl();
    //         $loc.search({species: species[0].name, config: config});
    //         $scope.$apply();
    //     });

    //     it ("stores key, value pairs", function () {
    //         var o = {value: "booom"};
    //         c.set("myFunc", 42); c.set("foo", o);
    //         expect(c.enElementValues).to.have.property("myFunc", 42);
    //         expect(c.enElementValues).to.have.deep.property("foo.value", "booom");
    //     });
    // })
})