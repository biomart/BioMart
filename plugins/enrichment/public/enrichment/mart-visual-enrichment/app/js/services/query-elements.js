;(function (angular) {
    "use strict";

    var app = angular.module("martVisualEnrichment.services");

    function QueryElements ($loc, $q) {
        this.get = function get() {
            var setsTxt = angular.element(".mve-sets-form textarea");
            var bedTxt = angular.element(".mve-bed-form textarea");

            return $q.when(angular.extend({
                dataset: $loc.search().species,
                sets: setsTxt ? setsTxt.val() : undefined,
                bed: bedTxt ? bedTxt.val() : undefined
            }, $loc.search()));
        };
    }

    QueryElements.$inject = ["$location", "$q"];
    app.service("queryElements", QueryElements);

}).call(this, angular);