;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.controllers");

app.controller("QueryCtrl", QueryCtrl);

QueryCtrl.$inject = [ "$scope", "$location", "$window", "queryValidator", "queryBuilder", "$rootScope", "mvConfig", "$modal", "queryStore", "$route", "storePusher"];
function QueryCtrl($scope, $loc, win, qv, qb, $rootScope, config, $modal, qs, $route, storePusher) {
    var ctrl = this;

    ctrl.win = win;
    ctrl.$modal = $modal;
    ctrl.qv = qv;
    ctrl.qb = qb;
    ctrl.$loc = $loc;
    ctrl.$rootScope = $rootScope;
    ctrl.config = config;
    ctrl.qs = qs;
    ctrl.$route = $route;
    ctrl.storePusher = storePusher;

    $scope.$on("$destroy", ctrl.storePusher.onStoreState(function () {
        var s = ctrl.$loc.search(), cfg = s.config;
        return ctrl.qs.config(cfg);
    }));
    $scope.$on("$destroy", ctrl.storePusher.onStoreState(function () {
        var s = ctrl.$loc.search(), spec = s.species;
        return ctrl.qs.dataset(spec);
    }));
}

QueryCtrl.prototype = {
    submit: function submit() {
        var ctrl = this;
        if (ctrl.validate()) {
            ctrl.buildQuery().then(function () {
                ctrl.$loc.url(ctrl.config.visualizationUrl);
            });
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
        ctrl.qs.clear().then(function () {
            var s = ctrl.$loc.search(), newS = {};
            newS.config = s.config;
            newS.species = s.species;
            ctrl.$loc.search(newS);
            ctrl.$route.reload();
        });
    },


    validate: function validate() {
        return this.qv.validate();
    },


    buildQuery: function build () {
        var ctrl = this;
        return ctrl.storePusher.broadcast().then(function () {
            return ctrl.qb.build();
        });
    },


    showQuery: function showQuery() {
        var ctrl = this;
        if (ctrl.validate()) {
            return ctrl.qs.clear().then(function () {
                return ctrl.storePusher.broadcast().then(function () {
                    return ctrl.qb.show().then(function (xml) {
                        ctrl.openModal(xml);
                    });
                });
            });
        } else {
            return ctrl.showError(ctrl.qv.errMessage());
        }
    }
};


})(angular);