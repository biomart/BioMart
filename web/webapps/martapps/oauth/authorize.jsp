<!doctype html>
<%@ page language="java"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%
    String callback = (String)request.getAttribute("CALLBACK");
    String openIdIdentifier = (String)session.getAttribute("openid_identifier");
    if(callback == null) callback = "";
    pageContext.setAttribute("token", (String)request.getAttribute("TOKEN"));
    pageContext.setAttribute("callback", callback);
    pageContext.setAttribute("app_name", (String)request.getAttribute("CONS_NAME"));
    pageContext.setAttribute("openid_identifier", openIdIdentifier);
%>
<html>
<head>
	<jsp:include page="../conf/config.jsp"/>
	<title>${labels.document_title}</title>

	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />


	<jsp:include page="../_head.jsp">
		<jsp:param name="path" value="../"/>
	</jsp:include>
</head>
<!--[if lt IE 7 ]> <body class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body class="biomart layout1"> <!--<![endif]--> 

<div id="biomart-top-wrapper">
<div id="biomart-header">
	<jsp:include page="/_header.jsp"/>
</div>

<jsp:include page="../_context.jsp">
	<jsp:param name="path" value="../"/>
</jsp:include>

<div id="biomart-wrapper">
	<div id="biomart-content" class="clearfix">
    <c:choose>
      <c:when test="${token == null}">
        <h3>Error</h3>
        <p class="error">OAuth token is missing from request.</p>
      </c:when>
      <c:when test="${openid_identifier != null}">
        <h3>"${app_name}" is trying to access your information.</h3>
        <form name="authZForm" action="authorize" method="POST">
          <input type="hidden" name="oauth_token" value="${token}"/>
          <input type="hidden" name="oauth_callback" value="${callback}"/>
          <input type="hidden" name="userId" value="${openid_identifier}"/>
          <button>Authorize</button>
        </form>
      </c:when>
      <c:otherwise>
        Please login at the top to continue.
      </c:otherwise>
    </c:choose>

	</div>
</div>

<div id="biomart-footer">
	<jsp:include page="/_footer.jsp"/>
</div>
</div>

<jsp:include page="../_js_includes.jsp">
	<jsp:param name="path" value="../"/>
	<jsp:param name="jQueryVersion" value="1.8"/>
</jsp:include>

<script type="text/javascript">
	$(document).ready(function() {
		$.publish('biomart.login');	
    $.subscribe('biomart.restart', biomart.utils, 'reloadPage');
	});
</script>
</body>
</html>
