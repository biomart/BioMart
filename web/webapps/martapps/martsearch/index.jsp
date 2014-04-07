<!doctype html>
<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<%
	String query = request.getParameter("q");
	if (query != null) response.sendRedirect("#!/?q=" + query);
%>
<c:if test="${not empty param.gui}">
  <biomart:getGuiContainer name="${param.gui}" var="guiContainer"/>
</c:if>
<html lang="<bm:locale/>">
<head>
	<jsp:include page="/conf/config.jsp"/>
  <title><bm:message code="document_title"/></title>

	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

	<jsp:include page="/_head.jsp">
		<jsp:param name="path" value="../"/>
	</jsp:include>
</head>
<!--[if lt IE 7 ]> <body id="martsearch" class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body id="martsearch" class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body id="martsearch" class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body id="martsearch" class="biomart layout1"> <!--<![endif]--> 

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
    <h2 class="gradient-grey-reverse ui-corner-top">
      Mart Search
    </h2>

    <div id="biomart-content" class="ui-widget-content clearfix">
      <form action="/martservice/search/query" id="biomart-form">
        <input size="50" type="text" name="q" value="${param.q}" id="biomart-query" class="query gradient-grey-reverse"/>
        <button type="submit" id="biomart-submit"><bm:message code="search" capitalize="true"/></button>
      </form>
      <p class="hint clearfix">
      <bm:message code="enter search term" capitalize="true"/>
      </p>
      <div id="biomart-results" class="in-progress">
        <div id="biomart-progressbar"></div>
        <p class="info">
          <bm:message code="display results" capitalize="true"/> <span class="start"></span> - <span class="end"></span> 
          <bm:message code="out of"/> <span class="total"></span>
          <span class="fetched"></span>
        </p>
        <p class="no-results"></p>
        <ol class="results"></ol>
        <p class="pagination">
        <span class="prev">&laquo; <bm:message code="previous" capitalize="true"/></span>
        <span class="next"><bm:message code="next" capitalize="true"/> &raquo;</span>
        </p>
      </div>
    </div>

    <div id="biomart-content-footer" class="ui-widget-content ui-corner-bottom gradient-grey clearfix">
      <jsp:include page="/_content_footer.jsp"/>
    </div>
     
    <jsp:include page="/conf/error.jsp"/>
  </div>
  <div id="biomart-footer">
    <jsp:include page="/_footer.jsp"/>
  </div>
</div>

<div id="biomart-loading">
	<p class="message">
    <bm:message code="loading" capitalize="true"/> MartSearch
	</p>
	<span class="loading"></span>
</div>

<jsp:include page="/_js_includes.jsp">
	<jsp:param name="path" value="../"/>
</jsp:include>

<%-- MartForm specific JS --%>
<script src="/martsearch/js/main.js"></script>

<script type="text/javascript">
	biomart.martsearch.term = '${param.q}';
	$(document).ready(function() {
		$.publish('biomart.login');	
	});
</script>

</body>
</html>
