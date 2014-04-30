;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.directives");

MvAttribute.$inject = ["queryStore", "$location", "storePusher"];
app.directive("mvAttribute", MvAttribute);

function MvAttribute(qs, $loc, storePusher) {

    function norm (ann) {
        return angular.isArray(ann) ? ann : angular.isDefined(ann) ? [ann] : [];
    }

    return {
        restrict: "A",
        scope: {},
        templateUrl: "mart-visual-enrichment/app/partials/attribute.html",
        link: function (scope, elem, attrs) {
            scope.attr = scope.$parent.$eval(attrs.mvAttribute);

            scope.$on("$destroy", storePusher.onStoreState(function () {
                if (scope.checked) {
                    return qs.attr(scope.attr.name, scope.attr.name);
                }
            }));

            scope.$on("$routeUpdate", function () {
                scope.checked = getUrlVal();
            });

            scope.setAttribute = function (checked) {
                var ann = norm($loc.search()[scope.attr.function]);

                if (checked) {
                    ann.push(scope.attr.name);
                } else {
                    ann.splice(ann.indexOf(scope.attr.name), 1);
                }
                $loc.search(scope.attr.function, ann);
            };

            function getUrlVal() {
                var ann = $loc.search()[scope.attr.function];
                return ann && 
                       ann.indexOf(scope.attr.name) !== -1 || 
                       scope.attr.selected;                
            }

            scope.checked = getUrlVal();
        }
    };
}


})(angular);