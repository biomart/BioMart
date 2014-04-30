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

    $scope.$on("$routeUpdate", function () {
        ctrl.expandCollapsable();
    });

    ctrl.init();

}

EnrichmentCtrl.prototype = {
    init: function init() {
        var ctrl = this,
            search = ctrl.$loc.search();
        ctrl.reqs = ["cutoff", "bonferroni", "bed_regions", "sets", "background", 
                     "upstream", "downstream", "gene_type",
                     "gene_limit", "homolog", "annotation"];
        ctrl.enElementValues = ctrl.findElements(ctrl.containers);
        ctrl.expandCollapsable();
        ctrl.selectTab(+search[ctrl.ACTIVE_TAB]);
    },

    BK_COLLAPSED: "background.show",

    ANN_COLLAPSED: "annotation.show",

    CUTOFF_COLLAPSED: "cutoff.show",

    ACTIVE_TAB: "tab",

    expandCollapsable: function () {
        var ctrl = this;
        ctrl.backgroundIsCollapsed = ctrl._isCollapsed(ctrl.BK_COLLAPSED);
        ctrl.cutoffIsCollapsed = ctrl._isCollapsed(ctrl.CUTOFF_COLLAPSED);
        ctrl.annotationIsCollapsed = ctrl._isCollapsed(ctrl.ANN_COLLAPSED);
    },

    selectTab: function (idx) {
        var ctrl = this;
        ctrl.bedTabActive = ctrl.geneTabActive = false;
        if (idx === 2) {
            ctrl.bedTabActive = true;
        } else {
            ctrl.geneTabActive = true;
        }
        ctrl._updateUrl(this.ACTIVE_TAB, idx);
    },

    _updateUrl: function (key, val) {
        this.$loc.search(key, val.toString());
    },

    _isCollapsed: function (tab) {
        var ctrl = this, search = ctrl.$loc.search();
        return !(search[tab] === "true");
    },

    onClickBackground: function () {
        var ctrl = this;
        // ctrl.backgroundIsCollapsed = !ctrl.backgroundIsCollapsed;
        ctrl._updateUrl(ctrl.BK_COLLAPSED, ctrl._isCollapsed(ctrl.BK_COLLAPSED));
    },

    onClickCutoff: function () {
        var ctrl = this;
        // ctrl.cutoffIsCollapsed = !ctrl.cutoffIsCollapsed;
        ctrl._updateUrl(ctrl.CUTOFF_COLLAPSED, ctrl.cutoffIsCollapsed);
    },

    onClickAnnotation: function () {
        var ctrl = this;
        // ctrl.annotationIsCollapsed = !ctrl.annotationIsCollapsed;
        ctrl._updateUrl(ctrl.ANN_COLLAPSED, ctrl.annotationIsCollapsed);

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