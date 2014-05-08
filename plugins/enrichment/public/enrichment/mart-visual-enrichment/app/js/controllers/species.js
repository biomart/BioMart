;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.controllers");


app.controller("SpeciesCtrl", ["$scope", "$location", "$route", "queryStore",
    function SpeciesCtrl($scope, $loc, $route, qs) {

    var sName = $loc.search().species;

    this.getSpeciesByName = function (name) {
        for (var i = 0, len = $scope.species.length, species = ""; i < len; ++i) {
            species = $scope.species[i];
            if (species.name === name) {
                return species;
            }
        }

        throw new Error("Cannot find species with name "+ name);
    };

    this.updateSpecies = function (species) {
        var s = $loc.search(), newS = {};
        newS.config = s.config;
        newS.species = species.name;
        // $loc.url("/gui/Enrichment");
        $loc.search(newS);
        qs.clear().then(function () {
            $route.reload();
        });
    };

    $scope.updateSpecies = this.updateSpecies;
    $scope.selectedSpecies = this.getSpeciesByName(sName);

    $scope.$on("$routeUpdate", function () {
        var species = $loc.search().species;

        if (species !== $scope.selectedSpecies.name) {
            $scope.selectedSpecies = this.getSpeciesByName(species);
        }
    }.bind(this));

}]);



})(angular);