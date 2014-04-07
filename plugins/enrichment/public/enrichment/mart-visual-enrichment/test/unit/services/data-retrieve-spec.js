"use strict";


describe("dataRetrieve", function () {
    var expect = chai.expect;

    var data, obj, dataRetrieve;

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(module(function ($provide) {
        data = {
            nodes: [
                {_id: "a0", type: "term", description: "luca", "p-value": 0.1},
                {_id: "a1", type: "term", description: "carmy", "p-value": 0.2},
                {_id: "g0", type: "term", description: "elios", "p-value": .3},
                {_id: "g1", type: "term", description: "alba", "p-value": .4},
                {_id: "g1", type: "gene"}
            ],
            edges: [
                { source: 2, target: 0 },
                { source: 2, target: 1 },
                { source: 3, target: 0 }
            ]
        };

        obj = [{
            getElementById: function () {
                return { textContent: JSON.stringify(data) };
            }
        }];

        $provide.value("dataContainerId", "idd");
        $provide.value("$document", obj);
    }));


    describe("success", function () {

        beforeEach(inject(function (_dataRetrieve_) {
            dataRetrieve = _dataRetrieve_;
        }));

        it ("returns json when the proper script (application/json) is present",
            function (done) {
            dataRetrieve.then(function (value) {
                expect(value).to.eql(data);
                done();
            }, done);
        });
    });

    describe("fail", function () {
        beforeEach(module(function ($provide) {
            obj = [{
                getElementById: function () {}
            }];

            $provide.value("$document", obj);
        }));

        beforeEach(inject(function (_dataRetrieve_) {
            dataRetrieve = _dataRetrieve_;
        }));

        it ("returns an error message when the script (application/json) is missing",
            function (done) {
            obj.getElementById = function () {};
            dataRetrieve.catch(function (mex) {
                expect(mex).to.match(/^cannot find/i);
                done();
            });
        });
    });
});
