describe("tabs service", function () {
    "use strict";

    var expect = chai.expect, graphs, tabs, obj, data;

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(module(function ($provide) {
        data = {
            graphs: {
                st: {
                    nodes: [
                        {_id: "a0", type: "term", description: "luca", "p-value": 0.1},
                        {_id: "a1", type: "term", description: "carmy", "p-value": 0.2},
                        {_id: "g1", type: "gene"}
                    ],

                    edges: [
                        {s: 0, t: 12},
                        {s: 21, t: 23}
                    ]
                },

                nd: {
                    nodes: [
                        {_id: "g0", type: "term", description: "elios", "p-value": .3},
                        {_id: "g1", type: "term", description: "alba", "p-value": .4},
                        {_id: "g1", type: "gene"}
                    ],

                    edges: [
                        {s: 0, t: 1},
                        {s: 1, t: 2}
                    ]
                }
            }
        };
        graphs = data.graphs;

        obj = [{
            getElementById: function () {
                return { textContent: JSON.stringify(data) };
            }
        }];

        $provide.value("dataContainerId", "idd");
        $provide.value("$document", obj);
    }));

    beforeEach(inject(function (_tabs_) {
        tabs = _tabs_;
    }));

    it ("returns the proper data", function (done) {
        tabs.then(function (tabs) {
            expect(tabs).to.eql([
                {
                    title: "st",
                    nodes: graphs.st.nodes,
                    edges: graphs.st.edges
                },
                {
                    title: "nd",
                    nodes: graphs.nd.nodes,
                    edges: graphs.nd.edges
                }
            ], function (m) { done(m); });
            done();
        })
    })

});