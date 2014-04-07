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
<!--[if lt IE 7 ]> <body id="martform2" class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body id="martform2" class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body id="martform2" class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body id="martform2" class="biomart layout1"> <!--<![endif]--> 
<div id="biomart-top-wrapper" class="ui-corner-all clearfix">
  <div id="biomart-header">
    <div class="content">
      <jsp:include page="/_header.jsp"/>
    </div>
  </div>
  <jsp:include page="/_context.jsp">
    <jsp:param name="path" value="../"/>
  </jsp:include>
  <div id="biomart-wrapper" class="ui-corner-all clearfix">
    <header class="gradient-grey-reverse ui-corner-top clearfix"> 
      <h2>
        MartForm
      </h2>
    </header>
    <div id="biomart-content" class="ui-widget-content clearfix">
      <section id="biomart-datasets" class="datasets">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="dataset" plural="true" capitalize="true"/></h3>
        <div class="content items clearfix">
          <div class="item clearfix">
            <label class="item-name" for="field-marts"><bm:message code="database" capitalize="true"/>:</label>
            <select id="field-marts" class="marts">
            </select>
          </div>
          <div class="item clearfix">
            <label class="item-name" for="field-datasets"><bm:message code="dataset" plural="true" capitalize="true"/>:</label>
            <select id="field-datasets" class="datasets">
            </select>
          </div>
        </div>
      </section>
      <section id="biomart-filters" class="filters">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="filter" plural="true"/></h3>
        <div class="content clearfix">
          <div class="message">
            <bm:message code="please select datasets" capitalize="true"/>
          </div>
          <div class="containers"></div>
        </div>
      </section>
      <section id="biomart-attributes" class="attributes">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="attribute" plural="true"/></h3>
        <div class="content clearfix">
          <div class="message">
            <bm:message code="please select datasets" capitalize="true"/>
          </div>
          <div class="containers"></div>
        </div>
      </section>
    </div>
    <div id="biomart-results-wrapper" class="ui-corner-top">
      <div class="message select-attributes" style="display:none">
        <bm:message code="please select attributes" capitalize="true"/>
      </div>
      <div id="error-message" class="message" style="display:none"></div>
      <div id="biomart-submit" class="clearfix">
        <button class="large green awesome"><bm:message code="go" capitalize="true"/> &raquo;</button>
      </div>
      <jsp:include page="/_results.jsp"/>
    </div>
    <footer id="biomart-content-footer" class="ui-widget-content ui-state-default ui-corner-bottom gradient-grey clearfix">
      <jsp:include page="/_content_footer.jsp"/>
    </footer>
    <jsp:include page="/conf/error.jsp"/>
  </div>
  <div id="biomart-footer" class="clearfix">
    <jsp:include page="/_footer.jsp"/>
  </div>
</div>
<div id="biomart-loading">
	<p class="message">
    <bm:message code="loading" capitalize="true"/> MartForm
	</p>
	<span class="loading"></span>
</div>
<jsp:include page="/_js_includes.jsp">
	<jsp:param name="path" value="../"/>
</jsp:include>
<script src="/martform/js/main.js"></script>
<script type="text/javascript">
	$(document).ready(function() {
		$.publish('biomart.login');	
	});
</script>
</body>
</html>
