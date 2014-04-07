<!doctype html>
<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<html lang="<bm:locale/>">
<head>
	<jsp:include page="/conf/config.jsp"/>
  <title><bm:message code="document_title"/></title>

	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

	<jsp:include page="/_head.jsp">
		<jsp:param name="path" value="../"/>
	</jsp:include>
</head>
<!--[if lt IE 7 ]> <body class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body class="biomart layout1"> <!--<![endif]--> 

<div id="biomart-top-wrapper">
  <div id="biomart-header">
    <div class="content">
      <jsp:include page="/_header.jsp"/>
    </div>
  </div>

  <jsp:include page="/_context.jsp">
    <jsp:param name="path" value="../"/>
  </jsp:include>

  <div id="biomart-wrapper" class="ui-state-default">
    <h2 class="gradient-grey-reverse clearfix">
      <span class="title"><bm:message code="view" capitalize="true"/>:</span>
      <div class="meta clearfix"></div>
      <%--div class="icons">
        <a class="icon help" href="#">
          <span class="ui-icon ui-icon-help"></span>
          help
        </a>
      </div--%>
    </h2>
    <div id="biomart-content" class="clearfix">
      <div id="biomart-nav" class="clearfix"></div>

      <div id="biomart-configure" class="clearfix">
        <div class="meta panel ui-corner-all clearfix">
        </div>
        <div class="datasets panel clearfix">
          <h3 class="ui-widget-header ui-corner-all">1. <bm:message code="select" capitalize="true"/> <bm:message code="dataset" plural="true"/></h3>
          <div class="content">
            <p class="info"><bm:message code="selectable_msg"/></p>
            <div id="biomart-datasets-container">
              <ul id="biomart-datasets" class="ui-selectable"></ul>
            </div>
          </div>
        </div>
        <div class="filters panel clearfix">
          <h3 class="ui-widget-header ui-corner-all">2. <bm:message code="restrict search" capitalize="true"/></h3>
          <div class="content">
            <ul class="items"></ul> 
          </div>
        </div>
        <div class="attributes panel clearfix" style="display: none">
          <h3 class="ui-widget-header ui-corner-all">3. <bm:message code="select" capitalize="true"/> <bm:message code="attribute" plural="true"/></h3>
          <div class="content">
            <ul class="items"></ul> 
          </div>
        </div>
      </div>

      <div class="panel results">
        <div id="biomart-submit" class="clearfix">
          <button class="large green awesome"><bm:message code="go" capitalize="true"/> &raquo;</button>
          <form id="biomart-query-form" action="${service.url}query" method="POST">
            <input type="hidden" name="query"/>
          </form>
        </div>
        <jsp:include page="/_results.jsp"/>
      </div>
    </div>

    <div id="biomart-content-footer" class="ui-widget-content ui-state-default ui-corner-bottom gradient-grey clearfix">
      <jsp:include page="/_content_footer.jsp"/>
    </div>
     
    <jsp:include page="/conf/error.jsp"/>
  </div>
  <div id="biomart-footer">
    <jsp:include page="/_footer.jsp"/>
  </div>
</div>

<%--div id="biomart-help" title="Help" style="display: none">
	<p>Change the view from the top dropdown box, under the <span style="color: #555; font-family: Optima,Segoe,'Segoe UI',Candara,Calibri,Arial,sans-serif">Quick Search</span> text.</p>
	<p>Select <span style="color: #2E6E9E">cancer types</span> from the list on the left.</p>
	<p><span style="color: #9E2E2E">Restrict your query</span> by selecting and configuring filters on the right.</p>
	<p>When you're ready, click on the <em style="color: #247F07">Go</em> button to download the results.</p>
</div--%>

<jsp:include page="/_explain.jsp"/>

<div id="biomart-loading">
	<p class="message">
    <bm:message code="loading" capitalize="true"/> MartAnalysis
	</p>
	<span class="loading"></span>
</div>

<jsp:include page="/_js_includes.jsp">
	<jsp:param name="path" value="../"/>
</jsp:include>

<script src="/martanalysis/js/main.js"></script>
<script src="/martanalysis/js/params.js"></script>

<script type="text/javascript">
	$(document).ready(function() {
		$.publish('biomart.login');	
	});
</script>

</body>
</html>
