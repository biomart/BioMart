<!doctype html>
<%@ page language="java"%>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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
    <p>Successfully authorized OAuth request token. <strong>Verifier PIN code = ${param.oauth_verifier}.</strong></p>
    <p>Please <a href="javascript:window.close()">close</a> this window to continue.</p>
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
  biomart.pinCode = '${param.oauth_verifier}';
	$(document).ready(function() {
		$.publish('biomart.login');	
    window.opener.biomart.admin.handleVerifier(biomart.pinCode);
	});
</script>
</body>
</html>
