;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.services");

QueryStore.$inject = ["$q", "$location", "$localForage"];
app.service("queryStore", QueryStore);
// It handles the storage of filter values, attributes names, configuration and
// dataset to persist the state of parameters selected by the deployer and the user.
// It allows to get hold of the query parameters across different sessions.
// It should have as little state as possible, because services are instanciated
// only on URL load or re-load, not through page history.

// All the names for filters, attributes, configurations, datasets must be unique
// or they'll clash.
function QueryStore($q, $loc, $localForage) {
    this.$q = $q;
    this.$loc = $loc;
    this.$localForage = $localForage;
    this.init();
}


QueryStore.prototype = {

    init: function () {
        var self = this, db = this.getDb();
        db.getItem(this._attrKeys).then(function (val) {
            if (!self._hasValue(val)) {
                db.setItem(self._attrKeys, []);
            }
        });
        db.getItem(this._filterKeys).then(function (val) {
            if (!self._hasValue(val)) {
                db.setItem(self._filterKeys, []);
            }
        });
    },

    _filterKeys: "qs.filters",

    _attrKeys: "qs.attrs",

    _configKey: "qs.config",

    _datasetKey: "qs.dataset",

    _hasValue: function hasValue(value) {
        return !(angular.isUndefined(value) || value === null);
    },

    _coll: function _coll(collKey) {
        var db = this.getDb(), self = this;
        return db.getItem(collKey).then(function keysFn (keys) {
            keys || (keys = []);
            var values = keys.reduce(function pValue(m, k) {
                m[k] = db.getItem(k);
                return m;
            }, {});
            return self.$q.all(values);
        });
    },

    _checkName: function _ckName(name) {
        if (!angular.isString(name)) {
            throw new Error("`name` is not either a string, null or undefined: "+ name);
        }
    },

    _param: function _slParam (key, value) {
        var db = this.getDb();
        return this._hasValue(name) ?
                // create/replace it, otheiwise remove it
                db.setItem(key, value) : db.removeItem(key);
    },

    _elem: function (collKey, eKey, eVal) {
        var self = this, db = this.getDb(), idx;
        return db.getItem(collKey).then(function aColl (aKeys) {
            var inColl;
            aKeys || (aKeys = []);
            idx = aKeys.indexOf(eKey);
            inColl = idx !== -1;

            if (eVal === null && inColl) {
                // Remove
                aKeys.splice(idx, 1);
                return db.removeItem(eKey);
            } else if (angular.isDefined(eVal)) {
                // add, replace
                if (!inColl) {
                    aKeys.push(eKey);
                    db.setItem(collKey, aKeys);
                }
                return db.setItem(eKey, eVal);
            } else {
                // get
                return db.getItem(eKey);
            }
        });
    },

    getDb: function _getIt () {
        return this.$localForage;
    },

    // Getter/Setter.
    // If `value` is null, it will remove the filter with
    // name `name`; if missing returns the value for that key,
    // otherwise it will replace the value of the filter or create
    // a new entry key `name` with value `value` amongst the filters.
    // If name isn't a string, it throws an Error.
    // name String
    // value Any
    filter: function (name, value) {
        this._checkName(name);
        return this._elem(this._filterKeys, name, value);
    },

    allFilters: function() {
        return this._coll(this._filterKeys);
    },

    // Getter/Setter.
    // See `#filter(name, value)`.
    // name String
    // value Any
    attr: function (name, value) {
        this._checkName(name);
        return this._elem(this._attrKeys, name, value);
    },

    allAttrs: function () {
        return this._coll(this._attrKeys);
    },

    // Getter/Setter
    // Returns a promise.
    // If `name` is a string it'll replace or create an entry for the config
    // `name` as value, and fulfill the promise; if is null
    // or undefined it'll remove the config entry, otherwise it throws an Error.
    // config([name])
    // name String
    config: function (name) {
        var db = this.getDb();
        if (angular.isString(name)) {
            return db.setItem(this._configKey, name);
        } else if (name === null || angular.isUndefined(name)) {
            return db.getItem(this._configKey);
        } else {
            throw new Error("`name` must be a string, null or undefined");
        }
    },

    // Getter/Setter.
    // See `#config(name)`.
    // name String
    dataset: function (name) {
        var db = this.getDb();
        if (angular.isString(name)) {
            return db.setItem(this._datasetKey, name);
        } else if (name === null || angular.isUndefined(name)) {
            return db.getItem(this._datasetKey);
        } else {
            throw new Error("`name` must be a string, null or undefined");
        }
    }

};


}).call(this, angular);
