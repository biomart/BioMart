<!doctype html>
<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<html lang="en-ca">
<c:set var="currentPage" scope="request">
  Converter
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
    <header class="gradient-grey-reverse ui-corner-top clearfix"> 
      <h2></h2>
    </header>
    <div id="biomart-content" class="ui-widget-content clearfix">
      <div id="biomart-datasets" class="clearfix">
        <label for="dataset-list"><bm:message code="dataset" capitalize="true"/>:</label> <select id="dataset-list"></select></div>
      <div id="biomart-from">
        <h3>Convert</h3>
      </div>
      <div id="biomart-to">
        <h3>To</h3>
      </div>
      <div id="biomart-submit">
        <button class="large green awesome"><bm:message code="go" capitalize="true"/> &raquo;</button>
      </div>
      <div id="biomart-results-wrapper" style="display: none" title="Conversion Results">
        <div id="biomart-results"></div>
        <div class="actions clearfix" style="display:none">
          <a title="Download all results" class="export" href="#">
            <span class="wrapper">
              <span class="ui-icon ui-icon-arrowthickstop-1-s"></span>
              Download data
            </span>
            <form id="biomart-form" style="visibility: hidden" target="_blank">
              <input type="hidden" name="query"/>
              <input type="hidden" name="download" value="true"/>
            </form>
          </a>
        </div>
      </div>
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
<c:import url="/_js_includes.jsp?path=../" context="/"/>
<script type="text/javascript" src="js/main.js"></script>
<script type="text/javascript">
  // Do not touch this
	$(document).ready(function() {
		$.publish('biomart.login');	
    $.subscribe('biomart.restart', {refresh:function(){location=location.href}}, 'refresh');
	});
</script>
</body>
</html>
