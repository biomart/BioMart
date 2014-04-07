;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.controllers");


app.controller("SpeciesCtrl", ["$scope", "$location", "$route", "queryStore",
    function SpeciesCtrl($scope, $loc, $route, qs) {

    var sName = $loc.search().species, species;
    for (var i = 0, len = $scope.species.length; i < len; ++i) {
        species = $scope.species[i];
        if (species.name === sName) {
            $scope.selectedSpecies = species;
            break;
        }
    }
    this.updateSpecies = function (species) {
        var s = $loc.search(), newS = {};
        newS.config = s.config;
        newS.species = species.name;
        // $loc.url("/gui/Enrichment");
        $loc.search(newS);
        qs.getDb().clear().then(function () {
            $route.reload();
        });
    };

    $scope.updateSpecies = this.updateSpecies;

}]);



})(angular);