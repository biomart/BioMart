;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.services");

queryValidator.$inject = ["$location"];
app.service("queryValidator", queryValidator);

function queryValidator($loc) {
    this.err = "";

    // this.mkFnMap = function mkFnMap(elmMap, m) {
    //     return Object.keys(elmMap).reduce(function r(map, prop) {
    //         var el = elmMap[prop], f;
    //         if (el) {
    //             f = el.function;
    //             if (!map[f]) { map[f] = []; }
    //             map[f].push(el);
    //         }
    //         return map;
    //     }, m || {});
    // };

    // function def (t) {
    //     return angular.isDefined(t) && angular.isArray(t) && t.length !== 0;
    // }

    // function undef (t) {
    //     return ! def(t);
    // }

    // TODO: add validation rules to the config
    // this.validate = function validate (elmMap) {
    //     var elm  = this.mkFnMap(elmMap.attributes);
    //     elm  = this.mkFnMap(elmMap.filters, elm);
    //     // 1. sets and bed_regions cannot be set at the same time or both missing
    //     if (! (def(elm.sets) ^ def(elm.bed_regions))) {
    //         if (def(elm.sets)) {
    //             this.err = "There can only be a list of genes or a BED file, not both.";
    //         } else {
    //             this.err = "One between list of genes and BED file must be chosen, please.";
    //         }
    //         return false;
    //     }
    //     if (undef(elm.cutoff)) {
    //         this.err = "The cutoff must be provided.";
    //         return false;
    //     }
    //     if (undef(elm.annotation)) {
    //         this.err = "Atleast one annotation must be selected, please.";
    //         return false;
    //     }

    //     this.err = "";
    //     return true;
    // };
    function def (el) {
        return angular.isDefined(el) && el !== "";
    }

    function undef (el) {
        return !def(el);
    }

    this.validate = function validate () {
        var elm = $loc.search();
        // 1. sets and bed_regions cannot be set at the same time or both missing
        if (! (def(elm.sets) ^ def(elm.bed_regions))) {
            if (def(elm.sets)) {
                this.err = "There can only be a list of genes or a BED file, not both.";
            } else {
                this.err = "One between list of genes and BED file must be chosen, please.";
            }
            return false;
        }
        if (undef(elm.cutoff)) {
            this.err = "The cutoff must be provided.";
            return false;
        }
        if (undef(elm.annotation)) {
            this.err = "Atleast one annotation must be selected, please.";
            return false;
        }

        this.err = "";
        return true;
    };

    this.errMessage = function message() {
        return this.err;
    };

}

})(angular);