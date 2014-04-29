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
            self.pushStateToStore(val);
            self.setBindings(val);
        } else {
            self.getStoredVal().then(function (val) {
                self.pushStateToUrl(val);
                self.setBindings(val);
            });
        }
    }
}

var UploadFilterCreator = {
    call: function (self, scope, elm) {
        elm.on("change", scope.onFileUpload);
        scope.$on("$destroy", function () {
            elm.off("change");
        });
    }
}

var SelectFilterCreator = {
    call: function (self, scope) {
        var search = self.$loc.search();
        var idx = +search[self.getQueryParam()];
        var oi = angular.isNumber(idx) && !isNaN(idx) ? 
                    search[self.getQueryParam()] : 0;
        scope.prevSelected = scope.selected = scope.options[oi];
        self.pushStateToUrl(oi);
        self.getStoredVal().then(function (val) {
            self.setBindings(val);
        });
    }
}

// var UpdateOnRouteChangeFilterCreator = {
//     call: function (self, scope) {
//         scope.$on("$destory", scope.$on("$routeChange", function () {
//             self.getUrlVal
//         }));
//     }
// }

/**
 *  requires these methods:
 *  + getFilterKey() 
 *  + getQueryParam()
 *  + setBindings() */
function Filter() {
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
}


function SelectFilter(scope) {

    var self = this;

    scope.onSelect = function (selected) {
        if (scope.prevSelected !== selected) {
            self.pushStateToStore(null);
            scope.prevSelected = scope.selected = selected;
            self.pushStateToStore(scope.filter.value);
            self.pushStateToUrl(scope.options.indexOf(selected));
        }
    };
}



function UploadFilter(scope) {
    var self = this;

    scope.onTextChange = function (value) {
        self.setBindings(value);
        self.pushStateToStore(value);
    }

    scope.onFileUpload = function (evt) {
        putTextPromise(self.$q, evt).then(function then(text) {     
            return text ? self.sanitize.stripTags(text) : null;
        }, function (reason) {
            return null;
        }, function (val) {
            self.pushStateToUrl(val);
            self.pushStateToStore(val);
            self.setBindings(val);
        });
    }
}


app.directive("uploadFilter",
          ["$q", "queryStore", "sanitize", function ($q, qs, sanitize) {

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

            self.getFilterKey = function () {
                return scope.filter.name;
            };

            self.getQueryParam = function () {};

            self.setBindings = function (value) {
                value = value ? sanitize.stripTags(value) : null;
                scope.filter.value = value; //value && value !== "" ? value : null;
            };

            Filter.call(self);
            UploadFilter.call(self, scope);
            TextFilterCreator.call(self, scope);
            UploadFilterCreator.call(self, scope, iElement.find("input"));
        }
    };
}]);


app.directive("singleSelectUploadFilter",
          ["$q", "queryStore", "sanitize", "$location", function ($q, qs, sanitize, $loc) {
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

            Filter.call(self);
            SelectFilter.call(self, scope);
            UploadFilter.call(self, scope, iElement);
            // At the moment, unfortunately, the order of the following calls
            // is important.
            SelectFilterCreator.call(self, scope);
            UploadFilterCreator.call(self, scope, iElement.find("input"));
        }

    };
}]);


app.directive("textFilter",
          ["queryStore", "$location", "sanitize", function (qs, $loc, sanitize) {
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

            self.getFilterKey = function () {
                return scope.filter.name;
            }

            self.getQueryParam = function () {
                return scope.filter.function;
            }

            self.setBindings = function (value) {
                value = angular.isString(value) && value !== "" ?
                    sanitize.stripTags(value) : null;
                scope.filter.value = value;
            }

            Filter.call(self);
            TextFilterCreator.call(self, scope);
        }
    };
}]);


app.directive("booleanFilter",
          ["queryStore", "$location", function booleanFilter (qs, $loc) {
    return {
        restrict: "E",
        templateUrl: partialsDir + "/boolean-filter.html",
        scope: {},
        link: function (scope, iElement, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            var self = {};

            self.getFilterKey = function () {
                return scope.filter.name;
            };

            self.getQueryParam = function () {
                return scope.filter.function;
            };

            self.setBindings = function (value) {
                scope.filter.value = value;
            };

            var self = self;
            scope.set = function (value) {
                self.pushStateToUrl(value);
                self.pushStateToStore(value);
            }



            // self.setFilterValue(value || "excluded");
        }
    };
}]);

app.directive("singleSelectBooleanFilter", [
              "queryStore", "$location",
              function multiSelectFilter (qs, $loc) {

    return {
        restrict: "E",
        templateUrl: partialsDir + "/single-select-boolean-filter.html",
        scope: {},
        link: function link(scope, elem, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            var self = {};

            self.getFilterKey = function () {
                return scope.prevSelected.name;
            };

            self.getQueryParam = function () {
                return scope.filter.function;
            };

            self.setBindings = function (value) {
                scope.filter.value = "only";
            };

            Filter.call(self);
            SelectFilter.call(self, scope);
        }
    };
}]);


// function UploadFilter($q, qs, sanitize, scope, elm, attrs) {
//     var self = this;
//     this.qs = qs;
//     this.sanitize = sanitize;
//     scope.filter = scope.$parent.$eval(attrs.filter);


//     scope.onTextChange = function (value) {
//         self.storeFilterValue(scope.filter.name, value);
//     }

//     self.getFilterValue = function (name) {
//         self.qs.filter(name).then(function (text) {
//             self.setFilterValue(text);
//         });
//     }

//     self.bindInputHandler = function () {
//         var rmInputHandler = elm.find("input").on("change", function onChange(evt) {
//             putTextPromise(self.$q, evt).then(function then(text) {
//                 var v = text ? self.sanitize.stripTags(text) : null;
//                 self.setFilterValue(v);
//                 self.storeFilterValue(v);
//             }).catch(function rejected(reason) {
//                 self.setFilterValue(reason);
//                 self.storeFilterValue(null);
//             });
//         });

//         scope.$on("$destroy", rmInputHandler);
//     }

//     self.setFilterValue = function (value) {
//         scope.filter.value = value;
//     };

//     self.storeFilterValue = function (value) {
//         self.qs.filter(scope.filter.name, value);
//     };
// } 


// app.directive("uploadFilter",
//           ["$q", "queryStore", "sanitize", function ($q, qs, sanitize) {
//     return {
//         restrict: "E",
//         templateUrl: partialsDir + "/upload-filter.html",
//         scope: {},
//         link: function (scope, iElement, attrs) {
//             UploadFilter.call(this, $q, qs, sanitize, scope, iElement, attrs);
//             this.getFilterValue(scope.filter.name);
//             this.bindInputHandler();
//         }
//     };
// }]);






// app.directive("singleSelectUploadFilter",
//           ["$q", "queryStore", "sanitize", "$location", function ($q, qs, sanitize, $loc) {
//     return {
//         restrict: "E",
//         templateUrl: partialsDir + "/single-select-upload-filter.html",
//         scope: {},
//         link: function (scope, iElement, attrs) {
//             var self = this;
//             UploadFilter.call(self, $q, qs, sanitize, scope, iElement, attrs);
//             self.$loc = $loc;
            

//             self.setUrlQuery =  function (fn, value) {
//                 self.$loc.search(fn, value);
//             };

//             self.storeFilterValue = function (value) {
//                 self.qs.filter(prevSelected.name, value);
//             };

//             scope.onTextChange = function (value) {
//                 self.storeFilterValue(scope.selected, value);
//             };

//             scope.onSelect = function (selected) {
//                 if (prevSelected !== selected) {
//                     self.storeFilterValue(null);
//                     prevSelected = selected;
//                     self.storeFilterValue(scope.filter.value);
//                     self.setUrlQuery(scope.filter.function, 
//                                      scope.options.indexOf(selected));
//                 }
//             };

//             scope.options = scope.filter.filters;
//             var search = $loc.search(), prevSelected, oi;
            
//             if (angular.isNumber(oi = search[scope.filter.function])) {
//                 scope.selected = scope.options[oi];
//             } else {
//                 scope.selected = scope.options[0];
//                 self.setUrlQuery(scope.filter.function, 0);
//             }

//             prevSelected = scope.selected;

//             self.getFilterValue(prevSelected.name);
//             self.bindInputHandler();
//         }

//     };
// }]);




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