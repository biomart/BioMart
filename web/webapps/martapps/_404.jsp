<!doctype html>
<%@ page language="java"%>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<html lang="<bm:locale/>">
<head>
  <title>Page Not Found &mdash; <bm:message code="document_title"/></title>

	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

	<jsp:include page="/_head.jsp">
		<jsp:param name="path" value="/"/>
		<jsp:param name="includemaincss" value="false"/>
	</jsp:include>
</head>
<body class="biomart layout1 main">

<div id="biomart-top-wrapper">
  <div id="biomart-header">
      <jsp:include page="/_header.jsp"/>
  </div>

  <div id="biomart-wrapper">
    <div style="margin: 20px 0; line-height: 1.5em; font-size: 16px" id="biomart-content" class="clearfix">
      <p>The page you are requesting could not be found.</p>
      <p>Click <a href="${siteUrl}">here</a> to return to home.</p>
    </div>
    <div id="biomart-content-footer" class="clearfix">
      <jsp:include page="/_content_footer.jsp"/>
    </div>
  </div>

  <div id="biomart-footer">
    <jsp:include page="/_footer.jsp"/>
  </div>
</div>

</body>
</html>

