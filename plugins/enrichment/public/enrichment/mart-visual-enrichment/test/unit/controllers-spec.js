"use strict";

/**
 * These tests are mainly for educational purpose, to understand how to test
 * controllers.
 **/
describe("ResultsTableCtrl function", function () {
    describe ("ResultsTableCtrl", function () {
        var $rootScope, $scope, nodes, terms, all, descFiltered;

        beforeEach(module("martVisualEnrichment.controllers"));
        beforeEach(inject(function (_$rootScope_, $controller, $q) {
            nodes = [
                {_id: "a0", type: "term", description: "luca", score: 0.1},
                {_id: "a1", type: "term", description: "carmy", score: 0.2},
                {_id: "g0", type: "term", description: "elios", score: .3},
                {_id: "g1", type: "term", description: "alba", score: .4}
            ];
            $rootScope = _$rootScope_;
            all = $q.defer(); descFiltered = $q.defer();
            terms = {
                p: null,
                all: function () {this.p = all.promise; return this; },
                filterByDescription: function () {
                    this.p = descFiltered.promise;
                    return this;
                },
                then: function (fn,erfn, pfn) {
                    return this.p.then(fn,erfn,pfn);
                }
            };
            $scope = $rootScope.$new();
            $controller("ResultsTableCtrl", {
                $scope: $scope,
                terms: terms
            });
        }));

        it ("creates 'results' model with all the results", function () {
            all.resolve(nodes);
            expect($scope.results).to.eql([]);
            $rootScope.$digest();
            expect($scope.results).to.eql(nodes);
        });

        it ("#filterByDescription filters 'results' properly", function () {
            descFiltered.resolve([nodes[0], nodes[1]]);
            $scope.filterByDescription("lu");
            $rootScope.$digest();
            expect($scope.results).to.have.length(2).and.to.eql([nodes[0], nodes[1]]);
        });
    });

});