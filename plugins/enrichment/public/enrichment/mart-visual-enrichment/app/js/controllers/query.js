;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.controllers");

app.controller("QueryCtrl", QueryCtrl);

QueryCtrl.$inject = [ "$scope", "$location", "$window", "queryValidator", "queryBuilder", "bmservice", "mvConfig", "$modal", "queryStore", "$route"];
function QueryCtrl($scope, $loc, win, qv, qb, bm, config, $modal, qs, $route) {
    var ctrl = this;

    ctrl.win = win;
    ctrl.$modal = $modal;
    ctrl.qv = qv;
    ctrl.qb = qb;
    ctrl.$loc = $loc;
    ctrl.bm = bm;
    ctrl.config = config;
    ctrl.qs = qs;
    ctrl.$route = $route;
}

QueryCtrl.prototype = {
    submit: function submit() {
        var ctrl = this;
        if (ctrl.validate()) {
            ctrl.buildQuery();
            ctrl.$loc.url(ctrl.config.visualizationUrl);
        } else {
            ctrl.showError(ctrl.qv.errMessage());
        }
    },


    showError: function err(message) {
        this.win.alert(message);
    },


    openModal: function modal(xml) {
        this.$modal.open({
            template: '<div class="modal-header"><h2>XML</h2></div><div class="modal-body"><pre><code>"'+window.escapeHtmlEntities(xml)+'"</code></pre></div>'
        });
    },


    clear: function () {
        var ctrl = this;
        ctrl.qs.getDb().clear().then(function () {
            var s = ctrl.$loc.search(), newS = {};
            newS.config = s.config;
            newS.species = s.species;
            // $loc.url("/gui/Enrichment");
            ctrl.$loc.search(newS);
            ctrl.$route.reload();
        });
    },


    validate: function validate() {
        return this.qv.validate();
    },


    buildQuery: function build () {
        var ctrl = this, params = ctrl.getBuildParamters();
        ctrl.qb.build.apply(ctrl.qb, params);
    },


    getBuildParamters: function bparams () {
        var ctrl = this, s = ctrl.$loc.search(), spec = s.species,
            cfg = s.config;
        ctrl.qs.config(cfg);
        ctrl.qs.dataset(spec);
        return [spec, cfg];
    },


    showQuery: function showQuery() {
        var ctrl = this;
        if (ctrl.validate()) {
            ctrl.qb.show(ctrl.getBuildParamters()).then(function (xml) {
                ctrl.openModal(xml);
            });
        } else {
            ctrl.showError(ctrl.qv.errMessage());
        }
    }
};


})(angular);