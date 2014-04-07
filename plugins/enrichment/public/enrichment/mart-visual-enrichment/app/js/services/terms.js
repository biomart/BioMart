;(function (angular) {

"use strict";

angular.module("martVisualEnrichment.services").
service("terms",
        ["termsAsync",
        function TermsService(TermsAsync) {
    this.get = function model (nodes) {
        return new TermsAsync(nodes);
    };
}]);

})(angular);