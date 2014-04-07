;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.directives");

app.directive("mvFilter", [function mvFilter () {
    return {
        restrict: "A",
        templateUrl: "mart-visual-enrichment/app/partials/filter.html",
        scope: true,
        link: function (scope, elm, attrs) {
            if (attrs.mvFilter) {
                scope.filter = scope.$parent.$eval(attrs.mvFilter);
            }
        }
    };

 }]);



})(angular);