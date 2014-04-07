;(function (angular) {
"use strict";

function style(cy) {
    return cy.stylesheet().
        selector("node").css({
            "font-family": "helvetica",
            "font-size": 7,
            "text-valign": "center",
            "text-outline-width": 1,
            "color": "#fff",
            "text-outline-opacity": 1,
            "border-width": 2
        }).
        selector(":selected").css({
            "background-color": "#2316BA"
        }).
        selector("node[type='term']").css({
            "content": "data(shortDesc)",
            "text-outline-color": "#000078",
            "background-color": "#FF1FA1",
            "border-color": "#F668C5",
            "width": "mapData(pvalue, 0, 1, 3, 60)",
            "height": "mapData(pvalue, 0, 1, 3, 60)"
        }).
        selector("node[type='gene']").css({
            "content": "data(id)",
            "background-color": "#00D57F",
            "border-color": "#64FC3F",
            "text-outline-color": "#000078",
            "width": 15,
            "height": 15
        }).
        selector(":active").css({
            "background-color": "#f00",
            "line-color": "#FFFD1E"
        }).
        selector(".faded").css({
            "opacity": 0.25,
            "text-opacity": 0
        }).
        selector("edge").css({
            "line-color": "#2316BA"
        }).
        selector(".link-hightlight").css({
            "line-color": "#FF185A"
        }).
        selector("core").css({
            "selection-box-color": "#FAB8D8",
            "selection-box-opacity": 0.5,
            "selection-box-border-color": "#E273B1",
            "selection-box-border-width": 3
        });
}


function container($cont) {
    return $cont[0];
}

function formatEles(eles) {
    var strip = /\W/g;
    return eles.map(function (e) {
        /* jshint sub:true */
        var id = e["id"];
        if (id) {
            e._id = id;
            e["id"] = id.replace(strip, '');
        }
        if (e.target && e.source) {
            e.target = e.target.replace(strip, '');
            e.source = e.source.replace(strip, '');
        }
        return { data: e };
    });
}

function truncateDescr(eles) {
    eles.forEach(function (e) {
        var l = 16;
        e.shortDesc = e.description.length > l ?
            e.description.substr(0, l - 1) + "..." : e.description;
    });
}


function ready(scope) {
    return function () {
        var cy = scope.cy = this, eles = cy.elements();
        // eles.unselectify();

        cy.on("tap", "node", function (e) {
            var node = e.cyTarget,
                neighborhood = node.neighborhood().add(node),
                edges = neighborhood.filter("edge");

            cy.elements().addClass("faded");
            neighborhood.removeClass("faded");
            edges.addClass("link-hightlight");
        });

        cy.on("tap", function (e) {
            if (e.cyTarget === cy) {
                cy.elements().removeClass("faded link-hightlight");
            }
        });

        scope.state.setState(scope.state.states.NETWORK);
    };
}


function initOpts(o) {
    o.layout = { name: "cose", padding: 20, hideEdgesOnViewport: true };
    o.showOverlay = false;
    o.elements = {};
    return o;
}


function updateGraph(scope, pattern) {
    var nodes = null;
    if (pattern === "" || !pattern) {
        scope.cy.elements().show();
    } else {
        scope.cy.elements().hide();
        nodes = scope.cy.nodes("[type='term']").nodes("[description@*='"+pattern+"']");
        nodes.neighborhood().add(nodes).show();
    }
}


angular.module("martVisualEnrichment.directives").

directive("mvGraph",
          ["$rootScope", "progressState", "$timeout",
          function ($rootScope, state, $timeout) {
    /* global cytoscape: false */
    function link (scope, iElement, iAttrs) {
        scope.state = state;
        $rootScope.$on("term.mouseover", function (evt, term) {
            /* jshint sub:true */
            cy.$("node#"+term["id"]).select();
        });
        $rootScope.$on("term.mouseout", function (evt, term) {
            /* jshint sub:true */
            cy.$("node#"+term["id"]).unselect();
        });
        scope.$on("$destroy", function () {
            scope.cy.elements().remove();
        });
        angular.element(window).on("resize", function () {
            iElement.css("height", angular.element(window).prop("innerHeight") + "px");
            iElement.css("width", angular.element(window).prop("innerWidth") + "px");
            cy.forceRender();
        });

        var angularInitialization = true;
        scope.$watch(function (scope) {
            return scope.filterPattern;
        }, function (newPattern, oldPattern) {
            if (angularInitialization) {
                angularInitialization = false;
            } else {
                if (newPattern !== oldPattern) {
                    updateGraph(scope, newPattern);
                }
            }
        });

        var o = initOpts({});
        o.style = style(cytoscape);
        o.container = container(iElement);
        o.ready = ready(scope);
        truncateDescr(scope.nodes);
        o.elements.edges = formatEles(scope.edges);
        o.elements.nodes = formatEles(scope.nodes);
        o.pan = {
            x: iElement.prop("clientWidth") / 2,
            y: iElement.prop("clientHeight") / 2
        };
        var cy = cytoscape(o);

    }
    return {
        restrict: "E",
        replace: true,
        templateUrl: "mart-visual-enrichment/app/partials/vis.html",
        scope: {
            nodes: "=nodes",
            edges: "=edges",
            filterPattern: "="
        },
        compile: function (tElement) {
            var h = angular.element(window).prop("innerHeight");
            tElement.css("height", h + "px");
            return link;
        }
    };
}]);

})(angular, cytoscape);