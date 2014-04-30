;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.services");

queryValidator.$inject = ["$location"];
app.service("queryValidator", queryValidator);

function queryValidator($loc) {
    this.err = "";

    function def (el) {
        return angular.isDefined(el) && el !== "";
    }

    function undef (el) {
        return !def(el);
    }

    function getElms () {
        var setsTxt = angular.element(".mve-sets-form textarea");
        var bedTxt = angular.element(".mve-bed-form textarea");

        return angular.extend($loc.search(), {
            dataset: $loc.search().species,
            sets: setsTxt ? setsTxt.val() : undefined,
            bed: bedTxt ? bedTxt.val() : undefined
        });
    }

    this.validate = function validate () {
        var elm = getElms();
        // 1. sets and bed_regions cannot be set at the same time or both missing
        if (! (def(elm.sets) ^ def(elm.bed))) {
            if (def(elm.sets)) {
                this.err = "There can only be a list of genes or a BED file, not both.";
            } else {
                this.err = "One between list of genes and BED file must be chosen, please.";
            }
            return false;
        }
        if (undef(elm.cutoff) || +elm.cutoff != parseFloat(elm.cutoff)) {
            this.err = "The cutoff must be provided and a number.";
            return false;
        }
        if (undef(elm.annotation) || angular.isArray(elm.annotation) && !elm.annotation.length) {
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