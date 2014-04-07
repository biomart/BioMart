<!doctype html>
<%@ page language="java"%>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<html lang="<bm:locale/>">
<head>
	<jsp:include page="./conf/config.jsp"/>
  <title><bm:message code="document_title"/></title>

	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

	<jsp:include page="_head.jsp">
		<jsp:param name="path" value="./"/>
	</jsp:include>
  <%
    Boolean sidebarEnabled = Boolean.getBoolean("biomart.sidebar");
    pageContext.setAttribute("sidebarEnabled", sidebarEnabled);
  %>
</head>
<!--[if lt IE 7 ]> <body class="biomart layout1 main ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body class="biomart layout1 main ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body class="biomart layout1 main ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body class="biomart layout1 main"> <!--<![endif]--> 

<div id="biomart-top-wrapper">
  <div id="biomart-header">
      <jsp:include page="/_header.jsp"/>
  </div>

  <jsp:include page="_context.jsp">
    <jsp:param name="path" value="./"/>
  </jsp:include>

  <div id="biomart-wrapper">
    <div id="biomart-content" class="clearfix">
      <c:if test="${sidebarEnabled}">
        <div class="col-b">
          <c:import url="/sidebar.html" context="/includes" charEncoding="UTF-8"/>
        </div>
        <div class="col-a">
      </c:if>
        <biomart:getRootGuiContainer var="root"/>
        <biomart:displayContainer item="root"/>
      <c:if test="${sidebarEnabled}">
        </div>
      </c:if>

    </div>
    <div id="biomart-content-footer" class="clearfix">
      <jsp:include page="/_content_footer.jsp"/>
    </div>
  </div>

  <div id="biomart-footer">
    <jsp:include page="/_footer.jsp"/>
  </div>
</div>

<jsp:include page="_js_includes.jsp">
	<jsp:param name="path" value="./"/>
</jsp:include>

<script type="text/javascript">
	$(document).ready(function() {
    $('.accordion').accordion({header: 'h4', autoHeight: false, collapsible: true, active: false});
		$.publish('biomart.login');	
    $.subscribe('biomart.restart', {refresh:function(){location=location.href}}, 'refresh');
	});
</script>
</body>
</html>
