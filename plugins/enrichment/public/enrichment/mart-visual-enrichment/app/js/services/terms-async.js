;(function (angular) {

"use strict";

angular.module("martVisualEnrichment.services").

factory("termsAsync", function () {
    return function termsAsync (nodes) {
        this._all = nodes.then(function then (nodes) {
            return nodes.filter(function filterTerms(node) {
                return node.type === "term";
            });
        });

        this._filtered = this._all;

        /**
         * Remove all the filters and returns a promise that resolves to all results.
         * This is the apparent behaviour, not what really will happen.
         **/
        this.all = function () {
            this._clearFilters();
            return this;
        };

        this._clearFilters = function () {
            this._filtered = null;
            this._filtered = this._all;
            return this;
        };

        this.filterByDescription = function (desc) {
            if (desc && desc !== "") {
                var pattern = new RegExp(desc, "i");
            // var strip = /\W/g, pattern = new RegExp(desc.replace(strip, ""), "i");
                this._filtered = this._filtered.then(function (terms) {
                    return terms.filter(function (term) {
                        return term.description.match(pattern);
                    });
                });
            }
            return this;
        };

        this.filterByScore = function (min, max) {
            this._filtered = this._filtered.then(function (terms) {
                return terms.filter(function (term) {
                    return term["p-value"] >= min && term["p-value"] < max;
                });
            });
            return this;
        };

        /**
         * Returns a promise that fulfils the filters previously applied.
         * The filters previously applied will be removed, after calling this method.
         */
        this.then = function (fn, errfn, progressfn) {
            var p = this._filtered.then(fn, errfn, progressfn);
            this._clearFilters();
            return p;
        };
    };
});

})(angular);