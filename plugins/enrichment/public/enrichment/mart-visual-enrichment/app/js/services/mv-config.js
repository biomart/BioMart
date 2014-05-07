;(function (angular) {
"use strict";

angular.module("martVisualEnrichment.services").

factory("mvConfig",
         [function mvConfig() {

    var configElemId = "enrichment-config", elem = document.getElementById(configElemId),
        config = {
            url: "/martservice",
            queryUrl: "/martservice/results",
            visualizationUrl: "/visualization/",
            defaults: {
                cutoff: 0.05,
                annotation: ["Gene Ontology (GO)"]
            }
        };

    if (elem) {
        config = angular.merge(JSON.parse(elem.textContent), config);
    }

    return config;

}]);


})(angular);