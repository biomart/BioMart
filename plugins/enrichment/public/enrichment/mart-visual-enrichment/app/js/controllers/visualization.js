;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.controllers");

app.controller("VisualizationCtrl",
           ["$scope", "queryBuilder", "bmservice", "progressState",
           function VisualizationCtrl ($scope, qb, bm, state) {
    var ctrl = this;

    var tabs = qb.build().then(function (xml) {
        return bm.query(xml, {
            cache: false,
            headers: {
                "Accept": "application/json,text/plain",
            }
        }).then(function then (res) {
            var graphs = res.data.graphs;
            return Object.keys(graphs).map(function (tabTitle) {
                var g = graphs[tabTitle];
                return {
                    title: tabTitle,
                    nodes: g.nodes,
                    edges: g.edges
                };
            });
        });
    });

    $scope.mvTabs = [];
    $scope.progressbarValue = 33;

    ctrl.state = state;

    ctrl.state.setState(ctrl.state.states.GETTING_DATA);

    tabs.then(function (tabs) {
        $scope.mvTabs = tabs;
        ctrl.state.setState(ctrl.state.states.PROCESSING);
    }, function (reason) {
        $scope.errorMessage = reason;
        $scope.progressbarValue = 100;
        ctrl.state.setState(ctrl.state.states.ERROR);
    });

}]);

}) (angular);