;(function (angular) {

"use strict";

var app = angular.module("martVisualEnrichment.controllers");


app.controller("GraphCtrl",
           ["$scope", "$timeout",
           function ($scope, $timeout) {
    var timeout = null;
    $scope.nodes = $scope.mvTab.nodes;
    $scope.edges = $scope.mvTab.edges;

    $scope.$watch(function (scope) {
        return scope.mvTab.pattern;
    }, function () {
        if (timeout) {
            $timeout.cancel(timeout);
        }
        timeout = $timeout(function () {
            $scope.search = $scope.mvTab.pattern;
        }, 500);
    });
}]);


})(angular);
