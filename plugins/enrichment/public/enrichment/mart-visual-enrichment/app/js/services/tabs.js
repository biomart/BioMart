;(function (angular) {

"use strict";

angular.module("martVisualEnrichment.services").

factory("tabs",
        ["dataRetrieve", "$log", "$rootScope",
        function tabsService(dataRetrieve, $log, $rootScope) {

    return dataRetrieve.then(function (data) {
        var graphs = data.graphs, tabs;
        tabs = Object.keys(graphs).map(function (tabTitle) {
            var g = graphs[tabTitle];
            return {
                title: tabTitle,
                nodes: g.nodes,
                edges: g.edges
            };
        });
        // $rootScope.$apply();
        return tabs;
    },
    function (reason) {
        $log.error("ERROR: tabs service: "+reason);
        return {};
    });

}]);

})(angular);