describe("terms", function () {
    "use strict";

    var expect = chai.expect

    var data, obj, terms;

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

    beforeEach(inject(function ($q, termsSync) {
        terms = new termsSync(data.nodes);
    }));

    it ("#all() returns only objects with `type` equal to 'term'", function () {
        terms.all().forEach(function (el) {
            expect(el).to.have.property("type", "term");
        });
    });

    describe ("#filterByDescription(str)", function () {
        describe("given terms with descriptions luca, carmy, elios, alba", function () {
            var nodes;
            beforeEach(function () { nodes  = data.nodes; });

            it ("with pattern 'ca' returns only terms with description 'luca', 'carmy'", function (){
                var results = terms.filterByDescription("ca")
                expect(results).to.have.length(2);
                expect(results[0]).to.eql(nodes[0]);
                expect(results[1]).to.eql(nodes[1]);
            });

            it ("with pattern 'eli' returns only terms with description 'elios'", function (){
                var results = terms.filterByDescription("eli")
                expect(results).to.have.length(1);
                expect(results[0]).to.eql(nodes[2]);
            });

            it ("with pattern 'a' returns only terms with description 'luca', 'carmy', 'alba'", function (){
                var results = terms.filterByDescription("a")
                expect(results).to.have.length(3);
                expect(results[0]).to.eql(nodes[0]);
                expect(results[1]).to.eql(nodes[1]);
                expect(results[2]).to.eql(nodes[3]);
            });


            it ("returns all results when str is empty", function () {
                var results = terms.filterByDescription("")
                expect(results).to.have.length(4);
            });

            it ("returns all results when str is undefined", function () {
                var results = terms.filterByDescription()
                expect(results).to.have.length(4);
            });
        });
    });

    describe ("#filterByScore", function () {
        var nodes;
        beforeEach(function () {nodes = data.nodes;});

        it ("given a min and max score, returns terms with score in [min, max)", function () {
            var t = terms.filterByScore(0.1, 0.4)
            expect(t).to.have.length(3);
            expect(t[0]).to.eql(nodes[0]);
            expect(t[1]).to.eql(nodes[1]);
            expect(t[2]).to.eql(nodes[2]);

            t = terms.filterByScore(0.2, 0.4)
            expect(t).to.have.length(2);
            expect(t[0]).to.eql(nodes[1]);
            expect(t[1]).to.eql(nodes[2]);
        })
    });

    describe ("filter composition", function () {
        var nodes;
        beforeEach(function () { nodes = data.nodes; });

        it ("#all(), after results has been filtered, resolves to unfiltered results",
            function () {
            terms.filterByDescription("eli")
            var t = terms.all()
            expect(t).to.eql(nodes.slice(0, -1));
        });

        it ("#filterByDescription() with empty pattern doesn't affect the results",
            function () {
            terms.filterByScore(0, 0.3)
            var t = terms.filterByDescription()
            expect(t).to.have.length(2);
            expect(t[0]).to.eql(nodes[0]);
            expect(t[1]).to.eql(nodes[1]);
        });

        describe ("filter by description and then by score", function () {
            it ("returns results filtered by both description and score", function () {
                terms.filterByDescription("ca")
                var t = terms.filterByScore(0.1, 0.2)
                expect(t).to.have.length(1);
                expect(t[0]).to.eql(nodes[0]);
            });

            it ("is commutative", function () {
                var t1 = 0, t2 = 1;
                terms.filterByDescription("ca")
                t1 = terms.filterByScore(0.1, 0.2)
                terms.all()
                terms.filterByScore(0.1, 0.2)
                t2 = terms.filterByDescription("ca")
                expect(t1).to.eql(t2);
            });
        });
    });
});