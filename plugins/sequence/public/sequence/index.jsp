<!doctype html>
<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<html lang="en-ca">
<c:set var="currentPage" scope="request">
  Sequence
</c:set>
<head>
  <c:import url="/conf/config.jsp" context="/"/>
	<title>${labels.document_title}</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <c:import url="/_head.jsp?path=../" context="/"/>
</head>
<!--[if lt IE 7 ]> <body id="converter" class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body id="converter" class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body id="converter" class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body id="converter" class="biomart layout1"> <!--<![endif]--> 
<div id="biomart-top-wrapper" class="ui-corner-all clearfix">
  <div id="biomart-header">
    <div class="content">
      <c:import url="/_header.jsp?path=../" context="/"/>
    </div>
  </div>
  <c:import url="/_context.jsp?path=../" context="/"/>
  <div id="biomart-wrapper" class="ui-corner-all clearfix">
    <biomart:getGuiContainer name="${param.gui}" var="gui"/>
    <header class="gradient-grey-reverse ui-corner-top clearfix"> 
    <h2>${gui.displayName}</h2>
    </header>
    <div id="biomart-content" class="ui-widget-content clearfix">
      <biomart:getMarts gui="${param.gui}" var="marts" json="true"/>
      <biomart:getDatasets mart="${param.mart}" var="datasets" json="true"/>
      <section class="datasets" id="biomart-datasets">
        <h3 class="ui-widget-header ui-corner-all">Datasets</h3>
        <div class="content items clearfix">
          <div class="item clearfix">
            <label for="field-marts" class="item-name">Database:</label>
            <select id="mart-list">
              <c:forEach var="mart" items="${marts}">
                <option value="${mart.name}" data-name="${mart.name}">${mart.displayName}</option>
              </c:forEach>
            </select>
          </div>
          <div class="item clearfix">
            <label for="field-datasets" class="item-name">Datasets:</label>
            <select id="dataset-list">
              <c:forEach var="ds" items="${datasets}">
                <option value="${ds.name}" data-name="${ds.name}">${ds.displayName}</option>
              </c:forEach>
            </select>
          </div>
        </div>
      </section>
      <section id="biomart-types" class="clearfix">
        <h3 class="ui-widget-header ui-corner-all">Sequences</h3>
        <div class="content">
          <div class="type-option gradient-grey-reverse clearfix">
            <div id="type-image"></div>
            <div id="type-list"></div>
          </div>
          <div class="type-option gradient-grey-reverse clearfix" id="flank">
            <div class="flank-container clearfix">
              <label for="upstream">Upstream Flank:</label>
              <input class="field text ui-state-default ui-corner-all" id="upstream" type="number" min="0"/>
            </div>
            <div class="flank-container clearfix">
              <label for="downstream">Downstream Flank:</label>
              <input class="field text ui-state-default ui-corner-all" id="downstream" type="number" min="0"/>
            </div>
          </div>
        </div>
      </section>
      <section id="biomart-filters" class="clearfix">
        <h3 class="ui-widget-header ui-corner-all">
          <span class="ui-text">Filters</span>
          <span class="ui-icon ui-icon-triangle-1-e"></span>
        </h3>
        <div class="content" id="filter-list" style="display:none"></div>
      </section>
      <section id="biomart-attributes" class="clearfix">
        <h3 class="ui-widget-header ui-corner-all">
          <span class="ui-text">Header Information</span>
          <span class="ui-icon ui-icon-triangle-1-e"></span>
        </h3>
        <div class="content" id="attribute-list" style="display:none"></div>
      </section>
      <section id="biomart-submit">
        <form id="biomart-form" action="${siteUrl}martservice/results" method="POST" target="streaming">
          <input type="hidden" name="query"/>
        </form>
        <button class="large green awesome"><bm:message code="go" capitalize="true"/> &raquo;</button>
      </section>
      <section id="biomart-results-wrapper" style="display: none" title="Sequence preview">
        <div id="biomart-results">
          <div class="loading"></div>
          <div class="message">
            <p>Showing the first <strong>20</strong> sequences. Use the <strong>Download</strong> button to retrieve the full result set.</p>
          </div>
          <iframe src="about:blank" id="biomart-streaming" name="streaming"></iframe>
        </div>
        <div class="actions clearfix" style="display:none">
          <a title="Download all results" class="export" href="#">
            <span class="wrapper">
              <span class="ui-icon ui-icon-arrowthickstop-1-s"></span>
              Download data
            </span>
          </a>
        </div>
      </section>
    </div>
    <footer id="biomart-content-footer" class="ui-widget-content ui-state-default ui-corner-bottom gradient-grey clearfix">
      <p class="version">Powered by <a href="http://www.biomart.org/" title="Visit biomart.org">BioMart</a></p>
    </footer>
    <c:import url="/conf/error.jsp" context="/"/>
  </div>
  <div id="biomart-footer" class="clearfix">
    <c:import url="/_footer.jsp?path=../" context="/"/>
  </div>
</div>
<div id="biomart-loading">
	<p class="message">
    Loading ${currentPage}
	</p>
	<span class="loading"></span>
</div>
<form style="visibility: hidden; height: 1px" id="biomart-download-form" action="${siteUrl}martservice/results" target="_blank" method="POST">
  <input type="hidden" name="query"/>
  <input type="hidden" name="download" value="true"/>
</form>
<c:import url="/_js_includes.jsp?path=../" context="/"/>
<script type="text/javascript" src="js/main.js"></script>
<script type="text/javascript">
  window.INITIAL_MART_NAME = '${param.mart}';
  window.MARTS = ${martsJson};
  window.DATASETS = ${datasetsJson};

	$(document).ready(function() {
		$.publish('biomart.login');	
    $.subscribe('biomart.restart', {refresh:function(){location=location.href}}, 'refresh');
	});
</script>
</body>
</html>
