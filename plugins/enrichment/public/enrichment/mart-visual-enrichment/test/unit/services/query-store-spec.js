describe("queryStore service", function () {
    "use strict";

    var $q, qs, $rootScope, db, expect = chai.expect, store;

    beforeEach(module("martVisualEnrichment.services"));
    beforeEach(module(function ($provide) {
        store = {
            db: {
                // "qs.filters": null,
                // "qs.attrs": null,
                // "qs.config": null,
                // "qs.dataset": null
            }
        };
        $provide.value("$localForage", store);
    }));

    beforeEach(inject(function ($injector) {
        $rootScope = $injector.get("$rootScope");
        $q = $injector.get("$q");

        store.getItem = function (key) {
            var v = this.db[key];
            return $q.when(v ? v : null);
        }

        store.setItem = function (key, value) {
            this.db[key] = value;
            return this.getItem(key);
        }

        store.removeItem = function (key) {
            return $q.when();
        }

        qs = $injector.get("queryStore");
        db = qs._getDb();
        $rootScope.$apply();
    }));

    function testAllFilters(val) {
        return function (fn) {
            qs.allFilters().then(function (fls) {
                expect(fls).to.eql(val);
                fn && fn();
            });
        }
    }

    function testAllAttrs(val) {
        return function (fn) {
            qs.allAttrs().then(function (fls) {
                expect(fls).to.eql(val);
                fn && fn();
            });
        }
    }

    it ("initially has an empty list of filters", function (done) {
        testAllFilters({})(done);
        $rootScope.$apply();
    });

    it ("initially has an empty list of attributes", function (done) {
        qs.allAttrs().then(function (ats) {
            expect(ats).to.eql({});
            done();
        }, done);
        $rootScope.$apply();
    });

    function dcTest(method) {
        return function () {
            it ("has not got config", function (done) {
                qs[method]().then(function (cfg) {
                    expect(cfg).to.be.null;
                    done();
                }, done);
                $rootScope.$apply();
            })

            it ("set the config", function (done) {
                qs[method]("booom").then(function (cfg) {
                    expect(cfg).to.eql("booom");
                    done();
                })
                $rootScope.$apply();
            })

            it ("replace the config", function (done) {
                qs[method]("booom");
                qs[method]("mooob").then(function (cfg) {
                    expect(cfg).to.eql("mooob");
                    done();
                })
                $rootScope.$apply();
            })
        }
    }

    describe("#config(name)", dcTest("config"));
    describe("#dataset(name)", dcTest("dataset"));

    function elemTest(storeMethod, collTest) {
        return function () {
            it ("returns null if there not an element with that name", function (done) {
                qs[storeMethod]("foo").then(function (val) {
                    expect(val).to.be.null;
                    collTest({})(done);
                })
                $rootScope.$apply();
            })

            it ("store a new element", function (done) {
                var v = [1, "2"];
                qs[storeMethod]("foo", v).then(function (val) {
                    expect(val).to.eql(v);
                    collTest({foo: v})(done);
                })
                $rootScope.$apply();
            })

            it ("store multiple elements", function (done) {
                var v1 = [1, "2"], v2 = { frak: "u" };
                $q.all([
                    qs[storeMethod]("foo", v1),
                    qs[storeMethod]("bar", v2)
                ]).then(function (values) {
                    expect(values[0]).to.eql(v1);
                    expect(values[1]).to.eql(v2);
                    collTest({foo: v1, bar: v2})(done);
                })
                $rootScope.$apply();
            })

            it ("replace a element value", function (done) {
                var v = [1, "2"];
                $q.all([
                    qs[storeMethod]("foo", "booom"),
                    qs[storeMethod]("foo", v)
                ]).then(function (values) {
                    expect(values[1]).to.eql(v);
                    collTest({foo: v})(done);
                })
                $rootScope.$apply();
            })
        }
    }

    describe("#filter(name, value)", elemTest("filter", testAllFilters));
    describe("#attr(name, value)", elemTest("attr", testAllAttrs));
});