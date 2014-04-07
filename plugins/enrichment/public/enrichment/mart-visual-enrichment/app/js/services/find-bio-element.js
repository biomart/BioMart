;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.services");

app.factory("findBioElement", function() {

    function Finder (containers) {
        if (!(this instanceof Finder)) {
            return new Finder(containers);
        }

        this.coll = containers;
        this.funcs = {};
    }


    Finder.prototype.addFunctions = function (funcs) {
        if (angular.isArray(funcs)) {
            funcs.forEach(function (k) {
                if (!(k in this.funcs)) {this.funcs[k] = {attributes: [], filters: []};}
            }, this);
        } else {
            if (!(funcs in this.funcs)) {this.funcs[funcs] = {attributes: [], filters: []};}
        }

        return this;
    };


    // The containars form a tree
    Finder.prototype.find = function walk() {
        // The root
        var c, q = [this.coll];
        while (q.length) {
            c = q.shift();
            if (c.attributes) {this.inspectAttrs(c.attributes, this.funcs);}
            if (c.filters) {this.inspectFilters(c.filters, this.funcs);}
            for (var i = 0; i < c.containers.length; ++i) {
                q.push(c.containers[i]);
            }
        }
        return this.funcs;
    };


    Finder.prototype.inspectAttrs = function (els, acc) {
        this.inspect(els, acc, "attributes");
    };


    Finder.prototype.inspectFilters = function (els, acc) {
        this.inspect(els, acc, "filters");
    };


    Finder.prototype.inspect = function (els, acc, set) {
        els.reduce(function fold (acc, e) {
            var f;
            if ((f = e.function) in acc) { acc[f][set].push(e); }
            return acc;
        }, acc);
    };

    Finder.prototype.getFunctionMap = function () {
        return this.funcs;
    };

    return Finder;

});

})(angular);