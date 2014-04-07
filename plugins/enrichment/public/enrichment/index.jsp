<!doctype html>
<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<html lang="en">
<c:set var="currentPage" scope="request" value="Enrichment"/>
<c:set var="currentPage" scope="request">
  Enrichment
</c:set>
<head>
  <c:import url="/conf/config.jsp" context="/"/>
    <title>${labels.document_title}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link rel="stylesheet" type="text/css" href="mart-visual-enrichment/app/css/bootstrap/bootstrap.mod.css">
    <link rel="stylesheet" href="mart-visual-enrichment/app/css/app.css"/>
    <c:import url="/_head.jsp?path=../" context="/"/>
</head>
<body ng-app="martVisualEnrichment">
    <div class="container">
        <div class="row">
            <div id="biomart-header col-md-6">
                <div class="content">
                  <c:import url="/_header.jsp?path=../" context="/"/>
                </div>
                <c:import url="/_context.jsp?path=../" context="/"/>
            </div>
        </div>
    </div>
    <ng-view></ng-view>

    <script src="mart-visual-enrichment/app/lib/jquery.js"></script>
    <script src="mart-visual-enrichment/app/lib/angular/angular.js"></script>
    <script src="mart-visual-enrichment/app/lib/angular/angular-route.js"></script>
    <script src="mart-visual-enrichment/app/lib/ui-bootstrap-tpls-0.10.0.js"></script>
    <script src="mart-visual-enrichment/app/lib/localforage.js"></script>
    <script src="mart-visual-enrichment/app/lib/angular-localForage.js"></script>

    <script src="mart-visual-enrichment/app/js/app.js"></script>

    <script src="mart-visual-enrichment/app/js/services.js"></script>
    <script src="mart-visual-enrichment/app/js/services/bmservice.js"></script>
    <script src="mart-visual-enrichment/app/js/services/mv-config.js"></script>
    <script src="mart-visual-enrichment/app/js/services/find-bio-element.js"></script>
    <script src="mart-visual-enrichment/app/js/services/query-store.js"></script>
    <script src="mart-visual-enrichment/app/js/services/query-builder.js"></script>
    <script src="mart-visual-enrichment/app/js/services/query-validator.js"></script>
    <script src="mart-visual-enrichment/app/js/services/sanitize.js"></script>

    <script src="mart-visual-enrichment/app/js/controllers.js"></script>
    <script src="mart-visual-enrichment/app/js/controllers/species.js"></script>
    <script src="mart-visual-enrichment/app/js/controllers/enrichment.js"></script>
    <script src="mart-visual-enrichment/app/js/controllers/query.js"></script>

    <script src="mart-visual-enrichment/app/js/directives.js"></script>
    <script src="mart-visual-enrichment/app/js/directives/mv-species.js"></script>
    <script src="mart-visual-enrichment/app/js/directives/filters.js"></script>
    <script src="mart-visual-enrichment/app/js/directives/mv-filter.js"></script>
    <script src="mart-visual-enrichment/app/js/directives/mv-attribute.js"></script>

    <script src="mart-visual-enrichment/app/lib/cytoscape.js"></script>

    <!-- visualizations -->
    <script src="mart-visual-enrichment/app/js/services/terms-async.js"></script>
    <script src="mart-visual-enrichment/app/js/services/terms.js"></script>
    <script src="mart-visual-enrichment/app/js/services/progress-state.js"></script>

    <script src="mart-visual-enrichment/app/js/controllers/progress.js"></script>
    <script src="mart-visual-enrichment/app/js/controllers/visualization.js"></script>
    <script src="mart-visual-enrichment/app/js/controllers/results-table.js"></script>
    <script src="mart-visual-enrichment/app/js/controllers/graph.js"></script>

    <script src="mart-visual-enrichment/app/js/directives/mv-graph.js"></script>
    <script src="mart-visual-enrichment/app/js/directives/mv-results-table.js"></script>


</body>
</html>