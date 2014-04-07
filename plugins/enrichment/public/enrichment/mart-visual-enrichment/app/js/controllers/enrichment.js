;(function (angular) {

"use strict";

var app = angular.module("martVisualEnrichment.controllers");

app.controller("EnrichmentCtrl",
               ["$scope",
                "$location",
                "$log",
                "bmservice",
                "findBioElement",
                "$localForage",
                EnrichmentCtrl]);


function EnrichmentCtrl($scope, $loc, $log, bm, find, store) {

    var ctrl = this;
    ctrl.$loc = $loc;
    ctrl.$log = $log;
    ctrl.bm = bm;
    ctrl.find = find;
    ctrl.store = store;
    ctrl.containers = $scope.containers;
    ctrl.init();

}

EnrichmentCtrl.prototype = {
    init: function init() {
        var ctrl = this;
        ctrl.reqs = ["cutoff", "bonferroni", "bed_regions", "sets", "background", "upstream", "downstream", "gene_type",
                     "gene_limit", "homolog", "annotation"];
        ctrl.enElementValues = ctrl.findElements(ctrl.containers);
        ctrl.backgroundIsCollapsed = ctrl.cutoffIsCollapsed = ctrl.annotationIsCollapsed = true;
        // ctrl.store.getItem("background.collapsed").then(function (c) {
        //     var v = true;
        //     if (c === false) { v = false; }
        //     ctrl.backgroundIsCollapsed = v;
        // });
        // ctrl.store.getItem("cutoff.collapsed").then(function (c) {
        //     var v = true;
        //     if (c === false) { v = false; }
        //     ctrl.cutoffIsCollapsed = v;
        // });
        // ctrl.store.getItem("annotation.collapsed").then(function (c) {
        //     var v = true;
        //     if (c === false) { v = false; }
        //     ctrl.annotationIsCollapsed = v;
        // });
    },

    onClickBackground: function () {
        var ctrl = this;
        ctrl.backgroundIsCollapsed = !ctrl.backgroundIsCollapsed;
        ctrl.store.setItem("background.collapsed", ctrl.backgroundIsCollapsed);
    },

    onClickCutoff: function () {
        var ctrl = this;
        ctrl.cutoffIsCollapsed = !ctrl.cutoffIsCollapsed;
        ctrl.store.setItem("cutoff.collapsed", ctrl.cutoffIsCollapsed);
    },

    onClickAnnotation: function () {
        var ctrl = this;
        ctrl.annotationIsCollapsed = !ctrl.annotationIsCollapsed;
        ctrl.store.setItem("annotation.collapsed", ctrl.annotationIsCollapsed);
    },


    findElements: function findElements(coll) {
        var ctrl = this;
        var finder = ctrl.find(coll).addFunctions(ctrl.reqs);
        return finder.find();
    },


    getElements: function getElements(elmFunc, set) {
        var ctrl = this;
        var elms = null;
        var elmMap = ctrl.enElementValues[elmFunc];
        return elmMap && elmMap[set]? elmMap[set] : [];
    },


    // It returns the first filter with that elmFunc.
    // The first filter is the first one met with breadth visit of the
    // containers tree.
    getFilter: function getFilter(elmFunc) {
        var fls = this.getElements(elmFunc, "filters");
        return fls.length ? fls[0] : null;
    },


    // This returns all the attributes with elmFunc function
    getAttributes: function getAttributes(elmFunc) {
        return this.getElements(elmFunc, "attributes");
    }
};



})(angular);