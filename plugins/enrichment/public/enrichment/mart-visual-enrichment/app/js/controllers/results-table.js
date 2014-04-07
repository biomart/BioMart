;(function (angular) {

"use strict";

var app = angular.module("martVisualEnrichment.controllers");

app.controller("ResultsTableCtrl",
           ["$scope", "$q", "$rootScope", "terms",
           function ResultsTableCtrl ($scope, $q, $rootScope, terms) {

    var init = true;
    $scope.terms = terms.get($q.when($scope.mvTab.nodes));
    $scope.terms.all().then(function (ts) {
        $scope.results = ts;
    });

    $scope.filterByDescription = function (desc) {
        $scope.mvTab.pattern = desc.toLowerCase();
        // Clean filters
        $scope.terms.all().filterByDescription(desc).then(function (ts) {
            $scope.results = ts;
        });
    };
}]);

})(angular);