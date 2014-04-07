<!doctype html>
<%@ page language="java" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<html lang="<bm:locale/>">
<c:set var="currentPage" scope="request">
  Demo
</c:set>
<head>
  <c:import url="/conf/config.jsp" context="/"/>
	<title>${labels.document_title}</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <c:import url="/_head.jsp?path=../" context="/"/>
</head>
<!--[if lt IE 7 ]> <body id="martform2" class="biomart layout1 ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body id="martform2" class="biomart layout1 ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body id="martform2" class="biomart layout1 ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body id="martform2" class="biomart layout1"> <!--<![endif]--> 
<div id="biomart-top-wrapper" class="ui-corner-all clearfix">
  <div id="biomart-header">
    <div class="content">
      <c:import url="/_header.jsp?path=../" context="/"/>
    </div>
  </div>
  <c:import url="/_context.jsp?path=../" context="/"/>
  <div id="biomart-wrapper" class="ui-corner-all clearfix">
    <h2 class="gradient-grey-reverse ui-corner-top guiContainerName clearfix"></h2>
    <div id="biomart-content" class="ui-widget-content clearfix">
      <section id="biomart-datasets" class="datasets">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="dataset" plural="true" capitalize="true"/></h3>
        <div class="content items clearfix">
          <div id="biomart-mart-list" class="item clearfix">
          </div>
          <div id="biomart-dataset-list" class="item clearfix">
          </div>
        </div>
      </section>
      <section id="biomart-filters" class="filters">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="filter" plural="true" capitalize="true"/></h3>
      </section>
      <section id="biomart-attributes" class="attributes">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="attribute" plural="true" capitalize="true"/></h3>
      </section>
      <button class="large green awesome">Go &raquo;</button>
    </div>
    <footer id="biomart-content-footer" class="ui-widget-content ui-state-default ui-corner-bottom gradient-grey clearfix">
      <p class="version">Powered by <a href="http://www.biomart.org/" title="Visit biomart.org">BioMart</a></p>
    </footer>
    <c:import url="/conf/error.jsp" context="/"/>
  </div>
  <div id="biomart-footer" class="clearfix">
    <c:import url="/_footer.jsp?path=../" context="/"/>
  </div>
</div>

<%-- 
    TEMPLATES 
--%>

<script type="text/html" id="mart">
  <option value="{{name}}" class="model-mart {{name}}">{{displayName}}</option>
</script>

<script type="text/html" id="selectBox">
  <label class="item-name" for="{{id}}">{{label}}:</label>
  <select class="{{className}}" id="{{id}}"></select>
</script>

<script type="text/html" id="dataset">
  <option value="{{name}}" class="model-dataset {{name}}">{{displayName}}</option>
</script>

<script type="text/html" id="attribute">
  <input type="checkbox" id="{{id}}"/>
  <label for="{{id}}">{{displayName}}</label>
</script>

<script type="text/html" id="filter">
  <label class="filter-item-name">{{displayName}}</label>
  {{#hasValues}}
    <select class="filter-field filter-field-values filter-value" {{#isMultiple}}multiple{{/isMultiple}}>
      {{#values}}
        <option value="{{name}}">{{displayName}}</option>
      {{/values}}
    </select>
  {{/hasValues}}
  {{#hasFilters}}
  <select class="filter-field filter-field-filters" {{#isMultiple}}multiple{{/isMultiple}}>
      {{#filters}}
        <option value="{{name}}">{{displayName}}</option>
      {{/filters}}
    </select>
  {{/hasFilters}}
  {{#hasText}}
    <input type="text" class="filter-field filter-field-text filter-value"/>
  {{/hasText}}
  {{#hasUpload}}
    <div class="filter-field-upload">
      <textarea class="filter-field filter-field-upload-text filter-value"></textarea>
      <input type="file" class="filter-field filter-field-upload-file"/>
    </div>
  {{/hasUpload}}
</script>

<script type="text/html" id="container">
  <div class="model-container model-container-{{name}}" id="{{id}}">
    <h4>{{displayName}}</h4>
  </div>
</script>

<!--c:import url="/_js_includes.jsp?path=../" context="/"/-->
<script type="text/javascript" src="js/lib/json2.js"></script>
<script type="text/javascript" src="js/lib/jquery-1.5.1.min.js"></script>
<script type="text/javascript" src="js/lib/underscore.js"></script>
<script type="text/javascript" src="js/lib/ICanHaz.js"></script>
<script type="text/javascript" src="js/lib/backbone.js"></script>
<script type="text/javascript" src="js/lib/jquery.blockUI.js"></script>
<script type="text/javascript" src="js/lib/ajaxupload.js"></script>
<script type="text/javascript" src="js/lib/jquery-ui-1.8.custom.min.js"></script>
<script type="text/javascript" src="js/utils.js"></script>
<script type="text/javascript" src="js/ui.common.js"></script>
<script type="text/javascript" src="js/core.js"></script>
<script type="text/javascript" src="js/models/models.js"></script>
<script type="text/javascript" src="js/models/collections.js"></script>
<script type="text/javascript" src="js/views/views.js"></script>
<script type="text/javascript" src="/conf/config.js.jsp"></script>
<script type="text/javascript" src="js/app.js"></script>
<script type="text/javascript">
  $(document).ready(function () {
      var path = BM.jsonify(BM.jsonify().fragment).path
      if (path) {
        path = path.split('/')
        window.app = BM.MartForm.init({
          guiName: path[0],
          martName: path[1],
          el: $('#biomart-wrapper')
        })
      }
  });
</script>
</body>
</html>
