;(function (angular) {
"use strict";

var app = angular.module("martVisualEnrichment.services");

app.factory("progressState", function progress() {

    var s = {};
    var stateNames = [
        "GETTING_DATA",
        "PROCESSING",
        "TABLE",
        "NETWORK",
        "DONE",
        "ERROR"
    ];

    s.states = {};
    stateNames.forEach(function (state, i) {
        Object.defineProperty(s.states, state, {
            value: i,
            enumerable: true
        });
    });

    s.currentState = s.states.GETTING_DATA;
    s.setState = function set(state) {
        if (stateNames[state] in s.states) {
            this.currentState = state;
        }
    };

    return s;
});


}).call(this, angular);