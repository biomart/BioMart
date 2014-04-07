describe("terms", function () {
    "use strict";

    var expect = chai.expect

    var data, obj, terms, $rootScope;

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(module(function ($provide) {
        data = {
            nodes: [
                {_id: "a0", type: "term", description: "luca", "p-value": 0.1},
                {_id: "a1", type: "term", description: "carmy", "p-value": 0.2},
                {_id: "g0", type: "term", description: "elios", "p-value": .3},
                {_id: "g1", type: "term", description: "alba", "p-value": .4},
                {_id: "g1", type: "gene"}
            ]
        };
    }));

    beforeEach(inject(function ($q, _$rootScope_, termsAsync) {
        $rootScope = _$rootScope_;
        terms = new termsAsync($q.when(data.nodes));
    }));

    it ("#all() returns only objects with `type` equal to 'term'", function (done) {
        terms.all().then(function (all) {
            all.forEach(function (el) {
                expect(el).to.have.property("type", "term");
            })
            done();
        });
        $rootScope.$digest();
    })

    describe ("#filterByDescription(str)", function () {
        describe("given terms with descriptions luca, carmy, elios, alba", function () {
            var nodes;
            beforeEach(function () { nodes  = data.nodes; });

            it ("with pattern 'ca' returns only terms with description 'luca', 'carmy'", function (done){
                terms.filterByDescription("ca").then(function (results) {
                    expect(results).to.have.length(2);
                    // expect(results).to.include(nodes[0]);
                    // expect(results).to.include(nodes[1]);
                    expect(results[0]).to.eql(nodes[0]);
                    expect(results[1]).to.eql(nodes[1]);
                    done();
                });
                $rootScope.$digest();
            });

            it ("with pattern 'eli' returns only terms with description 'elios'", function (done){
                terms.filterByDescription("eli").then(function (results) {
                    expect(results).to.have.length(1);
                    expect(results[0]).to.eql(nodes[2]);
                    done();
                });
                $rootScope.$digest();
            });

            it ("with pattern 'a' returns only terms with description 'luca', 'carmy', 'alba'", function (done){
                terms.filterByDescription("a").then(function (results) {
                    expect(results).to.have.length(3);
                    // expect(results).to.include(nodes[0]);
                    // expect(results).to.include(nodes[1]);
                    // expect(results).to.include(nodes[3]);
                    expect(results[0]).to.eql(nodes[0]);
                    expect(results[1]).to.eql(nodes[1]);
                    expect(results[2]).to.eql(nodes[3]);
                    done();
                });
                $rootScope.$digest();
            });


            it ("returns all results when str is empty", function (done) {
                terms.filterByDescription("").then(function (results) {
                    expect(results).to.have.length(4);
                    done();
                });
                $rootScope.$digest();
            });

            it ("returns all results when str is undefined", function (done) {
                terms.filterByDescription().then(function (results) {
                    expect(results).to.have.length(4);
                    done();
                });
                $rootScope.$digest();
            });
        });
    });

    describe ("#filterByScore", function () {
        var nodes;
        beforeEach(function () {nodes = data.nodes;});

        it ("given a min and max score, returns terms with score in [min, max)", function (done) {
            terms.filterByScore(0.1, 0.4).then(function (t) {
                expect(t).to.have.length(3);
                expect(t[0]).to.eql(nodes[0]);
                expect(t[1]).to.eql(nodes[1]);
                expect(t[2]).to.eql(nodes[2]);

                terms.filterByScore(0.2, 0.4).then(function (t) {
                    expect(t).to.have.length(2);
                    expect(t[0]).to.deep.equal(nodes[1]);
                    expect(t[1]).to.deep.equal(nodes[2]);
                    done();
                });
            });
            $rootScope.$digest();
        })
    });

    describe ("filter composition", function () {
        var nodes;
        beforeEach(function () { nodes = data.nodes; });

        it ("#all(), after results has been filtered, resolves to unfiltered results",
            function (done) {
            terms.filterByDescription("eli").
                all().
                then(function (terms) {
                    expect(terms).to.eql(nodes.slice(0, -1));
                    done();
                });
            $rootScope.$digest();
        });

        it ("#then() restore results to all of them", function (done) {
            terms.filterByDescription("lu").then(function () {});
            terms.then(function (terms) {
                expect(terms).to.eql(nodes.slice(0, -1));
                done();
            });
            $rootScope.$digest();
        });

        it ("#filterByDescription() with empty pattern doesn't affect the results",
            function (done) {
            terms.filterByScore(0, 0.3).
                filterByDescription().then(function (terms) {
                    expect(terms).to.have.length(2);
                    expect(terms[0]).to.eql(nodes[0]);
                    expect(terms[1]).to.eql(nodes[1]);
                    done();
            });
            $rootScope.$digest();
        });

        describe ("filter by description and then by score", function () {
            it ("returns results filtered by both description and score", function (done) {
                terms.filterByDescription("ca").
                    filterByScore(0.1, 0.2).
                    then(function (terms) {
                        expect(terms).to.have.length(1);
                        expect(terms[0]).to.eql(nodes[0]);
                        done();
                    });
                $rootScope.$digest();
            });

            it ("is commutative", function (done) {
                var t1 = 0, t2 = 1;
                terms.filterByDescription("ca").
                    filterByScore(0.1, 0.2).
                    then(function (terms) {
                        t1 = terms;
                    }).
                    then(function () {
                        terms.filterByScore(0.1, 0.2).
                        filterByDescription("ca").
                        then(function (terms) {
                            t2 = terms;
                            expect(t1).to.eql(t2);
                            done();
                        });
                    });
                $rootScope.$digest();
            });
        });
    });
});