<!doctype html>
<%@ page language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<html>
<head>
	<title>MartApps Test Suite</title>
	<link rel="stylesheet" href="lib/qunit.css" type="text/css" media="screen">
  <style>
  </style>
</head>
<body>
  <c:set var="debug" value="${true}" scope="request"/>
	<h1 id="qunit-header">MartApps Test Suite</h1>
	<h2 id="qunit-banner"></h2>
	<div id="qunit-testrunner-toolbar"></div>
	<h2 id="qunit-userAgent"></h2>
	<ol id="qunit-tests"></ol>

  <div id="biomart-top-wrapper" style="position: absolute; top: -10000px; left: -10000px;">
     <div id="test"></div>
  </div>

	<script type="text/javascript" src="../js/lib/jquery-1.4.4.min.js"></script>

	<jsp:include page="../_js_includes.jsp">
		<jsp:param name="path" value="../"/>
	</jsp:include>

	<script type="text/javascript" src="lib/qunit.js"></script>
	<script type="text/javascript" src="lib/jquery.simulate.js"></script>

	<script type="text/javascript" src="unit/core.js"></script>
	<script type="text/javascript" src="unit/auth.js"></script>
	<script type="text/javascript" src="unit/resource.js"></script>
	<script type="text/javascript" src="unit/url.js"></script>
	<script type="text/javascript" src="unit/query.js"></script>
	<script type="text/javascript" src="unit/validator.js"></script>
	<script type="text/javascript" src="unit/data.widgets.js"></script>
	<script type="text/javascript" src="unit/ui.common.js"></script>
	<script type="text/javascript" src="unit/utils.js"></script>
	<script type="text/javascript" src="unit/renderer.js"></script>
	<script type="text/javascript" src="unit/renderer_results.js"></script>
</body>
</html>
