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
<!--[if lt IE 7 ]> <body id="martwizard" class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body id="martwizard" class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body id="martwizard" class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body id="martwizard" class="biomart layout1"> <!--<![endif]--> 
<div id="biomart-top-wrapper">
  <div id="biomart-header">
    <div class="content">
      <jsp:include page="/_header.jsp"/>
    </div>
  </div>
  <jsp:include page="/_context.jsp">
    <jsp:param name="path" value="../"/>
  </jsp:include>
  <div id="biomart-wrapper" class="clearfix">
    <div id="biomart-content" class="ui-widget-content clearfix">
      <div id="biomart-toolbar" class="gradient-grey-reverse ui-corner-top clearfix">
        <ul id="biomart-step-info" class="clearfix">
          <li class="step-1 active" data-step-number="1">
            <span class="name"><bm:message code="dataset" plural="true" capitalize="true"/></span>
          </li>
          <li class="step-2 ui-state-disabled" data-step-number="2">
            <span class="ui-icon ui-icon-arrowthick-1-e"></span>
            <span class="name"><bm:message code="filter" plural="true" capitalize="true"/></span>
          </li>
          <li class="step-3 ui-state-disabled" data-step-number="3">
            <span class="ui-icon ui-icon-arrowthick-1-e"></span>
            <span class="name"><bm:message code="output" capitalize="true"/></span>
          </li>
        </ul>
        <ul id="biomart-navigation" class="clearfix">
          <li>
            <a id="biomart-new" href="#" title="Start a new query">
              <span class="ui-icon ui-icon-arrowrefresh-1-n"></span>
              <span class="text"><bm:message code="restart" capitalize="true"/></span>
            </a>
          </li>
          <li>
            <a class="prev" href="javascript:;">
              <span class="ui-icon ui-icon-triangle-1-w"></span>
              <span class="text"><bm:message code="previous" capitalize="true"/></span>
            </a>
          </li>
          <li>
            <a class="next awesome medium orange" href="javascript:;">
              <span class="ui-icon ui-icon-triangle-1-e"></span>
              <span class="text"><bm:message code="next" capitalize="true"/></span>
            </a>
          </li>
          <li>
            <a class="next results awesome medium green" href="javascript:;" style="display:none">
              <bm:message code="results" capitalize="true"/> &raquo;
            </a>
          </li>
        </ul>
      </div>
      <div id="biomart-content-wrapper" class="clearfix">
        <div id="biomart-main" class="panel clearfix">
          <div class="steps">
            <div class="step clearfix datasets" data-step-number="1">
              <div id="biomart-select-mart" class="item marts clearfix">
                <label><bm:message code="database" capitalize="true"/>:</label> 
              </div>
              <div id="biomart-select-dataset" class="item datasets clearfix">
                <label><bm:message code="dataset" plural="true" capitalize="true"/>:</label> 
              </div>
            </div>
            <div class="step clearfix filters items" style="display:none" data-step-number="2">
            </div>
            <div class="step clearfix attributes items" style="display:none" data-step-number="3">
            </div>
          </div>
        </div>
        <div id="biomart-summary" class="panel clearfix">
          <h4 class="ui-widget-header clearfix">
            <span><bm:message code="summary" capitalize="true"/></span>
            <div class="actions" title="View query XML">
              <span class="text"><bm:message code="view"/> XML</span>
              <span class="ui-icon ui-icon-carat-2-e-w"></span>
            </div>
          </h4>
          <ul>
            <li class="mart">
              <h5><bm:message code="database" capitalize="true"/></h5>
              <ul>
                <li class="empty">${labels.empty_value}</li>
              </ul>
            </li>
            <li class="datasets">
            <h5><bm:message code="dataset" plural="true" capitalize="true"/></h5>
              <ul>
                <li class="empty">${labels.empty_value}</li>
              </ul>
            </li>
            <li class="filters">
              <h5>Filters</h5>
              <ul>
                <li class="empty">${labels.empty_value}</li>
              </ul>
            </li>
            <li class="attributes">
              <h5>Attributes</h5>
              <ul>
                <li class="empty">${labels.empty_value}</li>
              </ul>
            </li>
          </ul>
        </div>
      </div>
      <jsp:include page="/_results.jsp"/>
      <div id="biomart-content-footer" class="gradient-grey ui-widget-content ui-corner-bottom clearfix">
        <jsp:include page="/_content_footer.jsp"/>
      </div>
    </div>
    <jsp:include page="/conf/error.jsp"/>
  </div>
  <div id="biomart-footer" class="clearfix">
    <jsp:include page="/_footer.jsp"/>
  </div>
</div>
<div id="biomart-loading">
	<p class="message">
    <bm:message code="loading" capitalize="true"/> MartWizard
	</p>
	<span class="loading"></span>
</div>
<iframe class="streaming" name="export-frame"></iframe>
<div id="biomart-view-xml" title="Query XML">
<textarea></textarea>
</div>
<jsp:include page="/_restart_warning.jsp"/>
<jsp:include page="/_js_includes.jsp">
	<jsp:param name="path" value="../"/>
</jsp:include>
<script src="/martwizard/js/main.js"></script>
<script type="text/javascript">
	$(document).ready(function() {
		$.publish('biomart.login');	
	});
</script>
</body>
</html>
