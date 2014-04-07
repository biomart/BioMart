describe("terms", function () {
    "use strict";

    var expect = chai.expect, terms, termsSync, nodes;

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(module(function ($provide) {
        nodes = [
            {_id: "a0", type: "term", description: "luca", "p-value": 0.1},
            {_id: "a1", type: "term", description: "carmy", "p-value": 0.2},
            {_id: "g0", type: "term", description: "elios", "p-value": .3},
            {_id: "g1", type: "term", description: "alba", "p-value": .4},
            {_id: "g1", type: "gene"}
        ];
    }));

    beforeEach(inject(function (_terms_, _termsSync_) {
        terms = _terms_;
        termsSync = _termsSync_;
    }));

    it ("given nodes as argument returns an instance of termsSync", function () {
        var s = terms.get(nodes);
        expect(s).to.be.an.instanceof(termsSync);
        expect(s.all()).to.eql(nodes.slice(0, -1));
    })

});