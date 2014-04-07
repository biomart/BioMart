;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.controllers");

app.controller("ProgressCtrl", [
    "$scope",
    "progressState",
    "$interval",
    function ProgressCtrl($scope, progress, $interval) {
        var ctrl = this, intPromise;
        ctrl.states = progress.states;
        ctrl.currentState = progress.states.currentState;
        $scope.progressbarValue = 0;
        $scope.max = 100;

        function inc() {
            $scope.progressbarValue += 1;
        }

        $scope.$watch(function () {
                return progress.currentState;
            }, function (newState) {
            ctrl.currentState = newState;
            switch(newState) {
                case ctrl.states.GETTING_DATA:
                    $scope.progressbarValue = 1;
                    intPromise = $interval(inc, 1350, 50);
                    break;
                case ctrl.states.PROCESSING:
                    $scope.progressbarValue = 60;
                    $interval.cancel(intPromise);
                    break;
                case ctrl.states.TABLE:
                    $scope.progressbarValue = 80;
                    break;
                case ctrl.states.NETWORK:
                case ctrl.states.ERROR:
                case ctrl.states.DONE:
                    $scope.progressbarValue = 100;
                    $interval.cancel(intPromise);
                    break;
            }
        });
    }]
);


}).call(this, angular);