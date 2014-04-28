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
}


QueryStore.prototype = {

    _ready: function () {
        var self = this, db = this.getDb();
        return this.$q.all([
            db.getItem(this._attrKeys).then(function (val) {
                if (!val) {
                    db.setItem(self._attrKeys, []);
                }
            }),
            db.getItem(this._filterKeys).then(function (val) {
                if (!val) {
                    db.setItem(self._filterKeys, []);
                }
            }),
            this.lastAction = this.$q.when(42)
        ]);
    },

    _filterKeys: "qs.filters",

    _attrKeys: "qs.attrs",

    _configKey: "qs.config",

    _datasetKey: "qs.dataset",

    _coll: function _coll(collKey) {
        var db = this.getDb(), self = this;
        return this._ready().then(function () {
            return db.getItem(collKey).then(function keysFn (keys) {
                var values = keys.reduce(function pValue(m, k) {
                    m[k] = db.getItem(k);
                    return m;
                }, {});
                return self.$q.all(values);
            });
        });
    },

    _checkName: function _ckName(name) {
        if (!angular.isString(name)) {
            throw new Error("`name` is not either a string, null or undefined: "+ name);
        }
    },


    _addItem: function (collKey, eKey, eVal, idx) {
        var db = this.getDb();
        return db.getItem(collKey).then(function aColl (keys) {
            if (idx !== -1) {
                return db.setItem(eKey, eVal);                        
            } else {
                keys.push(eKey);        
                return db.setItem(collKey, keys).then(function () {
                    return db.setItem(eKey, eVal);
                });
            }
        });
    },

    _removeItem: function (collKey, eKey, idx) {
        var db = this.getDb();
        return db.getItem(collKey).then(function (keys) {
            keys.splice(idx, 1);
            return db.setItem(collKey, keys).then(function () {
                return db.removeItem(eKey);
            });
        });
    },

    _elem: function (collKey, eKey, eVal) {
        var db = this.getDb(), self = this;
        return this.lastAction = this.lastAction.then(function () {
            return db.getItem(collKey).then(function aColl (keys) {
                var p, idx = keys.indexOf(eKey);

                if (eVal !== null && angular.isDefined(eVal)) {
                    // add, replace
                    p = self._addItem(collKey, eKey, eVal, idx);
                } else if (eVal === null && idx !== -1) {
                    p = self._removeItem(collKey, eKey, idx);
                } else {
                    // get
                    p = db.getItem(eKey);
                }
                return p;
            });
        });
    },

    getDb: function () {
        return this.$localForage;
    },

    clear: function () {
        this.getDb().clear();
        var self = this;
        return this._ready().then(function () { return self; });
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
        var self = this;
        this._checkName(name);
        return this._ready().then(function () {
            return self._elem(self._filterKeys, name, value);
        });
    },

    allFilters: function() {
        var self = this;
        return this._ready().then(function () {
            return self._coll(self._filterKeys);
        });
    },

    // Getter/Setter.
    // See `#filter(name, value)`.
    // name String
    // value Any
    attr: function (name, value) {
        var self = this;
        this._checkName(name);
        return this._ready().then(function () {
            return self._elem(self._attrKeys, name, value);
        });
    },

    allAttrs: function () {
        var self = this;
        return this._ready().then(function() {
            return self._coll(self._attrKeys);
        });
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
        var self = this;
        return this._ready().then(function () {
            if (angular.isString(name)) {
                return db.setItem(self._configKey, name);
            } else if (name === null || angular.isUndefined(name)) {
                return db.getItem(self._configKey);
            } else {
                throw new Error("`name` must be a string, null or undefined");
            }
        });
    },

    // Getter/Setter.
    // See `#config(name)`.
    // name String
    dataset: function (name) {
        var db = this.getDb();
        var self = this;
        return this._ready().then(function () {
            if (angular.isString(name)) {
                return db.setItem(self._datasetKey, name);
            } else if (name === null || angular.isUndefined(name)) {
                return db.getItem(self._datasetKey);
            } else {
                throw new Error("`name` must be a string, null or undefined");
            }
        });
    }

};


}).call(this, angular);
