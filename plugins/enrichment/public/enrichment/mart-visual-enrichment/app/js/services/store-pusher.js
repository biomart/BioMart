;(function (angular) {
    "use strict";


    var app = angular.module("martVisualEnrichment.services");

    storePusher.$inject = ["$q", "$rootScope"];
    app.factory("storePusher", storePusher);

    function storePusher ($q, $rootScope) {

        var lns = [];

        return {
            onStoreState: function (ln) {
                lns.push(ln)
                return function () {
                    lns.splice(lns.indexOf(ln), 1);
                }
            },

            broadcast: function () {
                var promises = [];
                for (var i = 0, ii = lns.length; i < ii; ++i) {
                    promises.push(lns[i].call(null));
                }
                return $q.all(promises);
            }
        };

    }

}).call(null, angular);