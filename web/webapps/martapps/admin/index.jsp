<!doctype html>
<%@ page language="java" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<html lang="en-ca">
<head>
	<jsp:include page="../conf/config.jsp"/>
	<title>BioMart Administration</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<jsp:include page="../_head.jsp">
		<jsp:param name="path" value="../"/>
	</jsp:include>
</head>
<body id="admin" class="biomart">
<div id="biomart-wrapper" class="ui-corner-all clearfix">
  <header class="gradient-grey-reverse ui-corner-top clearfix"> 
    <h1 class="ui-corner-top">BioMart Administration </h1>
  </header>
	<div id="biomart-content" class="ui-widget-content clearfix">
    <section id="oauth-consumers">
      <h2>OAuth Consumers</h2>
      <ul class="clearfix">
      </ul>
      <button class="add ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon" role="button" aria-disabled="false"><span class="ui-button-icon-primary ui-icon ui-icon-plus"></span><span class="ui-button-text">Add New Consumer</span></button>
      <form action="" method="GET" class="oauth-new-form" title="Add OAuth Consumer" style="display:none">
        <label for="oauth-new-name">Name (required):</label>
        <input name="name" id="oauth-new-name" type="text" size="50" placeholder="Enter name"/>
        <label for="oauth-new-description">Description:</label>
        <textarea name="description" id="oauth-new-description" cols="50" placeholder="Enter description"></textarea>
        <label for="oauth-new-key">Key (required):</label>
        <input name="key" id="oauth-new-key" type="text" size="50"/>
        <label for="oauth-new-secret">Secret (required):</label>
        <input name="secret" id="oauth-new-secret" type="text" size="50"/>
        <label for="oauth-new-callback">Callback:</label>
        <input name="callback" id="oauth-new-callback" type="text" size="50"/>
        <div class="actions">
          <button class="box add">Add</button>
          <span class="cancel">cancel</span>
        </div>
      </form>
      <form class="oauth-authorization" method="POST" target="biomart_oauth" style="display:none"></form>
      <form class="oauth-consumer-delete" method="POST" target="biomart_oauth" style="display:none">
        <p>Are you sure you want to delete <em class="name"></em></p>
        <button class="box delete">Yes, delete</button>
        <span class="cancel">No, cancel</span>
      </form>
    </section>
	</div>
	<footer id="biomart-content-footer" class="ui-corner-bottom gradient-grey clearfix">
    <p class="version">Powered by <a href="http://www.biomart.org/" title="Visit biomart.org">BioMart</a></p>
	</footer>
	<jsp:include page="../conf/error.jsp"/>
</div>
<form id="dummy" method="POST" target="biomart-admin" style="visibility:hidden"></form>
<jsp:include page="../_js_includes.jsp">
	<jsp:param name="path" value="../"/>
	<jsp:param name="jQueryVersion" value="1.8"/>
</jsp:include>
<script type="text/javascript" src="../js/lib/oauth.js"></script>
<script type="text/javascript" src="../js/lib/md5.js"></script>
<script type="text/javascript" src="../js/lib/sha1.js"></script>
<script type="text/javascript" src="js/main.js"></script>
<script type="text/javascript">
	$(document).ready(biomart.admin.init);
</script>
</body>
</html>
