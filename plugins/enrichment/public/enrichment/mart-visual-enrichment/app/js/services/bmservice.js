;(function (angular) {
"use strict";

angular.module("martVisualEnrichment.services").

service("bmservice",
        ["$http", "mvConfig",
        function bmservice ($http, config) {

    var url = config.url, queryUrl = config.queryUrl,
        baseOpts = {
            cache: true,
            method: "GET",
            timeout: 1e6
        };

    //
    // It gets the marts coming from the guicontainer guiContainer.
    // guiContainer is expected to be a string representing the name of the
    // guicontainer.
    //
    // e.g. gui.json?name=Enrichement
    this.marts = function marts(guiContainer, opt) {
        opt = opt || {};
        return $http.get(url + "/gui.json?name="+guiContainer, angular.extend({}, baseOpts, opt));
    };


    this.datasets = function datasets(config, opt) {
        opt = opt || {};
        var iUrl = url + "/datasets.json?config=" + config;
        return $http.get(iUrl, angular.extend({}, baseOpts, opt));
    };


    // containers.json?datasets=hsapiens_gene_ensembl&withfilters=true&withattributes=false&config=gene_ensembl_config_3_1_2
    this.containers = function containers (datasets, config, withFilters, withAttributes, opt) {
        opt = opt || {};
        var fs = withFilters, as = withAttributes,
            iUrl = url + "/containers.json?datasets="+datasets+"&config="+ config;

        if (angular.isDefined(fs)) { iUrl += "&withfilters=" + !!fs; }
        if (angular.isDefined(as)) { iUrl += "&withattributes=" + !!as; }
        return $http.get(iUrl, angular.extend({}, baseOpts, opt));
    };

    this.query = function (xml, opt) {
        var opts = angular.extend({
            params: { query: xml },
            // data: "query="+xml,
            headers: {
                "Content-Type": "application/xml"
            }
        }, baseOpts,  opt);
        return $http.get(queryUrl, opts);
    };

}]);

})(angular);