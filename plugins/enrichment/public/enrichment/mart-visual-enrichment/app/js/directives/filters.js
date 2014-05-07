;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.directives");
var partialsDir = "mart-visual-enrichment/app/partials";


function putTextPromise($q, evt) {
    var d = $q.defer();

    var file = evt.target.files[0];
    if (file) {
        var reader = new FileReader();
        reader.readAsText(file, "UTF-8");
        reader.onload = function (evt) {
            d.resolve(evt.target.result);
        };
        reader.onerror = function (evt) {
            d.reject("Error reading file");
        };

        return d.promise;
    }

    return d.reject("No file provided");
}

var TextFilterCreator = {
    call: function (self, scope) {
        var val = self.getUrlVal();
        if (val) {
            self.setBindings(val);
        }
    }
}

var UploadFilterCreator = {
    call: function (self, scope, elm) {
        elm.on("change", scope.onFileUpload);
        scope.$on("$destroy", function () {
            elm.off("change");
        });

        self.getStoredVal().then(function (val) {
            self.setBindings(val);
        });
    }
}

var SelectFilterCreator = {
    call: function (self, scope) {
        var search = self.$loc.search();
        var idx = +search[self.getQueryParam()];
        var oi = angular.isNumber(idx) && !isNaN(idx) ? idx : 0;
        // if (!isNaN(idx) && idx !== oi) {
        self.pushStateToUrl(oi);
        // }
        scope.prevSelected = scope.selected = scope.options[oi];
    }
}


var StoreStateFilterCreator = {
    call: function (self, scope) {
        scope.$on("$destroy", self.storePusher.onStoreState(function () {
            return self.pushStateToStore(self.getVal());
        }));
    }
}

/**
 *  requires these methods:
 *  + getFilterKey()
 *  + getQueryParam()
 *  + setBindings() */
function Filter(scope) {
    var val = null, self = this;

    this.pushStateToStore = function (val) {
        return this.qs.filter(self.getFilterKey(), val);
    };

    this.pushStateToUrl = function (val) {
        var key = self.getQueryParam();
        if (key) {
            this.$loc.search(key, val);
            // return self.$q.when(val);
        }
    };

    this.getUrlVal = function () {
        var key = self.getQueryParam();
        if (key) {
            var query = this.$loc.search();
            return query[key];
        }
    }

    this.getStoredVal = function () {
        return self.qs.filter(self.getFilterKey())
    }

    this.getVal = function () {
        return scope.filter.value;
    }
}


function TextFilter(scope) {
    var self = this;

    scope.onTextChange = function (text) {
        self.pushStateToUrl(text);
    }
}


function SelectFilter(scope) {

    var self = this;

    scope.onSelect = function (selected) {
        if (scope.prevSelected !== selected) {
            // self.pushStateToStore(null);
            scope.prevSelected = scope.selected = selected;
            self.pushStateToUrl(scope.options.indexOf(selected));
        }
    };

    scope.$on("$destroy", scope.$on("$routeUpdate", function () {
        var i = +self.$loc.search()[self.getQueryParam()];

        if (angular.isNumber(i) && !isNaN(i)) {
            scope.onSelect(scope.options[i]);
        }
    }));
}



function UploadFilter(scope) {
    var self = this;

    scope.onTextChange = function (value) {
        self.setBindings(value);
    }

    scope.onFileUpload = function (evt) {
        function save (val) {
            self.setBindings(val);
        }
        putTextPromise(self.$q, evt).then(function then(text) {
            return text ? self.sanitize.stripTags(text) : null;
        }, function (reason) {
            return null;
        }).then(save, save);
    }
}


app.directive("uploadFilter",
          ["storePusher", "$q", "queryStore", "sanitize", function (storePusher, $q, qs, sanitize) {

    return {
        restrict: "E",
        templateUrl: partialsDir + "/upload-filter.html",
        scope: {},
        link: function (scope, iElement, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            var self = {};

            self.$q = $q;
            self.qs = qs;
            self.sanitize = sanitize;
            self.storePusher = storePusher;

            self.getFilterKey = function () {
                return scope.filter.name;
            };

            self.getQueryParam = function () {};

            self.setBindings = function (value) {
                value = value ? sanitize.stripTags(value) : null;
                scope.filter.value = value; //value && value !== "" ? value : null;
            };



            Filter.call(self, scope);
            UploadFilter.call(self, scope);
            StoreStateFilterCreator.call(self, scope);
            TextFilterCreator.call(self, scope);
            UploadFilterCreator.call(self, scope, iElement.find("input"));
        }
    };
}]);


app.directive("singleSelectUploadFilter",
          ["storePusher", "$q", "queryStore", "sanitize", "$location",
          function (storePusher, $q, qs, sanitize, $loc) {
    return {
        restrict: "E",
        templateUrl: partialsDir + "/single-select-upload-filter.html",
        scope: {},
        link: function (scope, iElement, attrs) {

            scope.filter = scope.$parent.$eval(attrs.filter);
            var self = {};

            self.$q = $q;
            self.qs = qs;
            self.sanitize = sanitize;
            self.$loc = $loc;
            self.storePusher = storePusher;

            scope.options = scope.filter.filters;

            self.getFilterKey = function () {
                return scope.prevSelected.name;
            };

            self.getQueryParam = function () {
                return scope.filter.function;
            };

            self.setBindings = function (value) {
                value = value ? sanitize.stripTags(value) : null;
                scope.filter.value = value; // value && value !== "" ? value : null;
            };



            Filter.call(self, scope);
            SelectFilter.call(self, scope);
            UploadFilter.call(self, scope, iElement);
            // At the moment, unfortunately, the order of the following calls
            // is important.
            StoreStateFilterCreator.call(self, scope);
            SelectFilterCreator.call(self, scope);
            UploadFilterCreator.call(self, scope, iElement.find("input"));
        }

    };
}]);


app.directive("textFilter",
          ["storePusher", "queryStore", "$location", "sanitize",
          function (storePusher, qs, $loc, sanitize) {
    return {
        restrict: "E",
        templateUrl: partialsDir + "/text-filter.html",
        scope: {},
        link: function (scope, iElement, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            var self = {};

            self.$loc = $loc;
            self.qs = qs;
            self.sanitize = sanitize;
            self.storePusher = storePusher;

            self.getFilterKey = function () {
                return scope.filter.name;
            };

            self.getQueryParam = function () {
                return scope.filter.function;
            };

            self.setBindings = function (value) {
                if (angular.isDefined(value) && !isNaN(+value)) {
                    value = angular.isString(value) && value.trim() !== "" ?
                        sanitize.stripTags(value) : value;
                } else {
                    value = null;
                }

                scope.filter.value = value;
            };

            scope.$on("$routeUpdate", function () {
                self.setBindings(self.getUrlVal());
            });

            Filter.call(self, scope);
            TextFilter.call(self, scope);
            StoreStateFilterCreator.call(self, scope);
            TextFilterCreator.call(self, scope);
        }
    };
}]);


app.directive("booleanFilter",
          ["storePusher", "queryStore", "$location",
          function booleanFilter (storePusher, qs, $loc) {
    return {
        restrict: "E",
        templateUrl: partialsDir + "/boolean-filter.html",
        scope: {},
        link: function (scope, iElement, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            var self = {};

            self.$loc = $loc;
            self.qs = qs;
            self.storePusher = storePusher;

            self.getFilterKey = function () {
                return scope.filter.name;
            };

            self.getQueryParam = function () {
                return scope.filter.function;
            };

            self.setBindings = function (value) {
                scope.filter.value = value;
            };

            scope.$on("$routeUpdate", function () {
                self.setBindings(self.getUrlVal());
            });



            var self = self;
            scope.set = function (value) {
                self.pushStateToUrl(value);
                // self.pushStateToStore(value);
            }

            Filter.call(self, scope);
            StoreStateFilterCreator.call(self, scope);



            // self.setFilterValue(value || "excluded");
        }
    };
}]);

app.directive("singleSelectBooleanFilter", [
              "storePusher", "queryStore", "$location",
              function multiSelectFilter (storePusher, qs, $loc) {

    return {
        restrict: "E",
        templateUrl: partialsDir + "/single-select-boolean-filter.html",
        scope: {},
        link: function link(scope, elem, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            var self = {};

            self.$loc = $loc;
            self.qs = qs;

            self.getFilterKey = function () {
                return scope.prevSelected.name;
            };

            self.getQueryParam = function () {
                return scope.filter.function;
            };

            self.setBindings = function (value) {
                scope.filter.value = "only";
            };

            scope.$on("$routeUpdate", function () {
                self.setBindings(self.getUrlVal());
            });

            Filter.call(self, scope);
            SelectFilter.call(self, scope);
            StoreStateFilterCreator.call(self, scope);
        }
    };
}]);



// app.directive("multiSelectFilter", [
//               "queryStore", "$location",
//               function multiSelectFilter (qs, $loc) {

//     return {
//         restrict: "E",
//         templateUrl: partialsDir + "/multi-select-filter.html",
//         scope: {},
//         link: function link(scope, elem, attrs) {
//             scope.filter = scope.$parent.$eval(attrs.filter);
//             var fnValue = $loc.search()[scope.filter.function];
//             scope.options = scope.filter.values;
//             scope.setFilter = function setFilter (values) {
//                 if (values && values.length) {
//                     var vs = values.map(function (f) { return f.name; });
//                     scope.filter.value = vs.join(",");
//                     qs.filter(scope.filter.name, scope.filter.value);
//                 } else {
//                     qs.filter(scope.filter.name, null);
//                 }
//             };
//             scope.onSelect = function select (value) {
//                 this.setFilter(value);
//             };
//             if (fnValue) {
//                 scope.selected = fnValue;
//             }
//         }
//     };
// }]);





// app.directive("singleSelectFilter", [
//               "queryBuilder",
//               function multiSelectFilter (qb) {

//     return {
//         restrict: "E",
//         templateUrl: partialsDir + "/single-select-filter.html",
//         scope: {},
//         link: function link(scope, elem, attrs) {
//             scope.filter = scope.$parent.$eval(attrs.filter);

//             this.getFilterKey = function () {
//                 return scope.prevSelected.name;
//             };

//             this.getQueryParam = function () {
//                 return scope.filter.function;
//             };

//             this.setBindings = function (value) {
//                 scope.filter.value = value;
//             };

//             Filter.call(this);
//             // ...


//             // scope.options = scope.filter.filters || scope.filter.values;
//             // scope.setFilter = function setFilter (value) {
//             //     if (value && value !== "") {
//             //         scope.filter.value = value;
//             //         qb.setFilter(scope.filter.name, scope.filter);
//             //     } else {
//             //         qb.setFilter(scope.filter.name);
//             //     }
//             // };

//             // scope.onSelect = function select (value) {
//             //     this.setFilter(value);
//             // };
//         }
//     };
// }]);

})(angular);