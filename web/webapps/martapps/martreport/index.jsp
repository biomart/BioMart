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
<!--[if lt IE 7 ]> <body id="martreport" class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body id="martreport" class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body id="martreport" class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body id="martreport" class="biomart layout1"> <!--<![endif]--> 

<div id="biomart-top-wrapper">
  <div id="biomart-header">
    <div class="content">
      <jsp:include page="/_header.jsp"/>
    </div>
  </div>

  <jsp:include page="/_context.jsp">
    <jsp:param name="path" value="../"/>
  </jsp:include>

  <div id="biomart-wrapper">
    <h2 class="gradient-grey-reverse ui-corner-top"></h2>

    <div id="biomart-content" class="ui-widget-content clearfix">
      <div class="configure clearfix">
        <div id="biomart-common-filters"></div>
        <button id="biomart-submit"><bm:message code="go" capitalize="true"/> &raquo;</button>
      </div>
      <div id="biomart-containers"></div>
    </div>

    <div id="biomart-content-footer" class="ui-widget-content ui-corner-bottom gradient-grey clearfix">
      <jsp:include page="/_content_footer.jsp"/>
    </div>
     
    <jsp:include page="/conf/error.jsp"/>
  </div>

  <div id="biomart-sidenav" class="ui-fixed-tl gradient-grey-reverse active">
    <div class="wrapper">
      <span class="ui-icon ui-icon-triangle-1-e"></span>
      <h3><bm:message code="sections" capitalize="true"/></h3>
      <ol class="sections"></ol>
    </div>
  </div>

  <div id="biomart-footer">
    <jsp:include page="/_footer.jsp"/>
  </div>
</div>

<div id="biomart-loading">
	<p class="message">
    <bm:message code="loading" capitalize="true"/> MartReport
	</p>
	<span class="loading"></span>
</div>

<div id="biomart-interim-loading" class="ui-border-bottom ui-fixed-tl clearfix" style="display:none">
  <span><bm:message code="loading" capitalize="true"/></span>
  <span class="loading"></span>
</div>

<jsp:include page="/_js_includes.jsp">
	<jsp:param name="path" value="../"/>
</jsp:include>

<%-- MartForm specific JS --%>
<script src="/martreport/js/ui.widgets.js"></script>
<script src="/martreport/js/main.js"></script>

<script type="text/javascript">
	$(document).ready(function() {
		$.publish('biomart.login');	
	});
</script>

</body>
</html>
