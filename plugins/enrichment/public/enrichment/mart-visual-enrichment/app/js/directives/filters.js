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


app.directive("uploadFilter",
          ["$q", "queryStore", "sanitize", function ($q, qs, sanitize) {
    return {
        restrict: "E",
        templateUrl: partialsDir + "/upload-filter.html",
        scope: {},
        link: function (scope, iElement, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            // Get data from storage.
            qs.filter(scope.filter.name).then(function (text) {
                scope.textareaValue = text;
            });
            iElement.find("input").on("change", function onChange (evt) {
                var p = putTextPromise($q, evt);
                p.then(function then(text) {
                    var v = text ? sanitize.stripTags(text) : null;
                    scope.setFilter(v);
                }).catch(function rejected(reason) {
                    scope.setFilter();
                    scope.textareaValue = reason;
                });
            });

            scope.setFilter = function (value) {
                scope.filter.value = scope.textareaValue = value ? value : "";
                qs.filter(scope.filter.name, value && value !== "" ? value : null);
            };
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
            var prevSelected, ownProp = Object.prototype.hasOwnProperty,
                search = $loc.search(), dsPromise, fName;
            scope.filter = scope.$parent.$eval(attrs.filter);
            scope.options = scope.filter.filters;
            if (fName = search[scope.filter.function]) {
                dsPromise = qs.filter(fName).
                    then(function (val) {
                        for (var i = 0, len = scope.options.length; i < len; ++i) {
                            if (scope.options[i].name === fName) {
                                prevSelected = scope.options[i];
                                scope.textareaValue = val;
                                return prevSelected;
                            }
                        }
                    });
            } else {
                dsPromise = qs.allFilters().then(function (all) {
                    for (var f in all) {
                        if (ownProp.call(all, f)) {
                            if (prevSelected) { break; }
                            for (var i = 0, len = scope.options.length; i < len; ++i) {
                                if (scope.options[i].name === f) {
                                    prevSelected = scope.options[i];
                                    scope.textareaValue = all[f];
                                    break;
                                }
                            }
                        }
                    }
                    return prevSelected;
                });
            }
            dsPromise.then(function (selected) {
                scope.selected = selected ? selected : scope.options[0];
                prevSelected = scope.selected;
                $loc.search(scope.filter.function, scope.textareaValue && scope.textareaValue !== "" ? scope.selected.name : null);
            });
            iElement.find("input").on("change", function onChange (evt) {
                var p = putTextPromise($q, evt);
                p.then(function then(text) {
                    var v = text ? sanitize.stripTags(text) : null;
                    scope.setFilter(v);
                }).catch(function rejected(reason) {
                    scope.setFilter();
                    scope.textareaValue = reason;
                });
            });

            scope.csv = function (text) {
                return text.split("\n").join(",");
            };

            scope.setFilter = function (value) {
                scope.selected.value = scope.textareaValue = value ? value : "";
                $loc.search(scope.filter.function, value ? scope.selected.name : null);
                if (!value) {
                    var input = iElement.find("input");
                    input.val("");
                }
                qs.filter(scope.selected.name, value);
            };

            scope.onSelect = function (selected) {
                if (prevSelected !== selected) {
                    // $loc.search(scope.filter.function, null);
                    qs.filter(prevSelected.name, null);
                    prevSelected = selected;
                    this.setFilter(this.textareaValue);
                }
            };
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
            var s = $loc.search()[scope.filter.function];
            scope.set = function set (value, update) {
                var v = angular.isString(value) && value !== "" ?
                    sanitize.stripTags(value) : null;
                scope.filter.value = v;
                if (!update) {
                    $loc.search(scope.filter.function, v ? v : null);
                }
                scope.filterText = v;
                qs.filter(scope.filter.name, v && v !== "" ? v : null);
            };

            if (s) {
                scope.set(s, true);
            }
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
            var fnValue = $loc.search()[scope.filter.function];
            scope.set = function set (value) {
                scope.filter.value = value;
                $loc.search(scope.filter.function, value);
                qs.filter(scope.filter.name, value);
            };

            scope.set(scope.choice = fnValue || "excluded");
        }
    };
}]);


app.directive("multiSelectFilter", [
              "queryStore", "$location",
              function multiSelectFilter (qs, $loc) {

    return {
        restrict: "E",
        templateUrl: partialsDir + "/multi-select-filter.html",
        scope: {},
        link: function link(scope, elem, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            var fnValue = $loc.search()[scope.filter.function];
            scope.options = scope.filter.values;
            scope.setFilter = function setFilter (values) {
                if (values && values.length) {
                    var vs = values.map(function (f) { return f.name; });
                    scope.filter.value = vs.join(",");
                    qs.filter(scope.filter.name, scope.filter.value);
                } else {
                    qs.filter(scope.filter.name);
                }
            };
            scope.onSelect = function select (value) {
                this.setFilter(value);
            };
            if (fnValue) {
                scope.selected = fnValue;
            }
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
            var prevSelected = scope.selected, fName,
                search = $loc.search();
            scope.filter = scope.$parent.$eval(attrs.filter);
            scope.options = scope.filter.filters;
            if (fName = search[scope.filter.function]) {
                qs.filter(fName).
                    then(function (val) {
                        for (var i = 0, len = scope.options.length; i < len; ++i) {
                            if (scope.options[i].name === fName) {
                                scope.setFilter(scope.options[i]);
                                return prevSelected;
                            }
                        }
                    });
            }
            scope.setFilter = function setFilter (filter) {
                if (filter) {
                    $loc.search(scope.filter.function, filter.name);
                    qs.filter(filter.name, "only");
                }
                if (prevSelected) {
                    qs.filter(prevSelected.name);
                }
                scope.selected = prevSelected = filter;
            };

            scope.onSelect = function select (value) {
                this.setFilter(value);
            };

            // if (fnValue) {
            //     scope.setFilter(fnValue);
            // }
        }
    };
}]);


app.directive("singleSelectFilter", [
              "queryBuilder",
              function multiSelectFilter (qb) {

    return {
        restrict: "E",
        templateUrl: partialsDir + "/single-select-filter.html",
        scope: {},
        link: function link(scope, elem, attrs) {
            scope.filter = scope.$parent.$eval(attrs.filter);
            scope.options = scope.filter.filters || scope.filter.values;
            scope.setFilter = function setFilter (value) {
                if (value && value !== "") {
                    scope.filter.value = value;
                    qb.setFilter(scope.filter.name, scope.filter);
                } else {
                    qb.setFilter(scope.filter.name);
                }
            };

            scope.onSelect = function select (value) {
                this.setFilter(value);
            };
        }
    };
}]);


// app.controller("FilterCtrl", ["$scope", "queryBuilder", function FilterCtrl ($scope, qb) {
//     var ctrl = this;

//     ctrl.setFilter = function filterValue(name, value) {
//         qb.setFilter(name, value);
//     }

// }]);


// app.controller("AttributeCtrl", ["$scope", "queryBuilder", function FilterCtrl ($scope, qb) {
//     var ctrl = this;

//     ctrl.setAttribute = function attrValue(name, value) {
//         qb.setAttribute(name, value);
//     }

// }]);



})(angular);