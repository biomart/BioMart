<!doctype html>
<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<html lang="<bm:locale/>">
<head>
  <c:import url="/conf/config.jsp" context="/"/>
	<title>${labels.document_title}</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <c:import url="/_head.jsp?path=../&version=2" context="/"/>
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
      <div id="biomart-explain"></div>
      <section id="biomart-datasets" class="content-section datasets">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="dataset" plural="true" capitalize="true"/></h3>
        <div class="content items clearfix">
          <div id="biomart-mart-list" class="item clearfix">
          </div>
          <div id="biomart-dataset-list" class="item clearfix">
          </div>
        </div>
      </section>
      <section id="biomart-filters" class="content-section filters">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="filter" plural="true" capitalize="true"/></h3>
      </section>
      <section id="biomart-attributes" class="content-section attributes">
        <h3 class="ui-widget-header ui-corner-all"><bm:message code="attribute" plural="true" capitalize="true"/></h3>
      </section>
      <section id="biomart-results">
        <button class="large green awesome biomart-results-show">
          Go &raquo;
        </button>
        <div class="biomart-results-header">
          <button class="large blue awesome biomart-results-hide" style="display:none">
            <span class="wrapper">
              <span class="ui-icon ui-icon-arrowreturnthick-1-w"></span>
              Back 
            </span>
          </button>
        </div>
        <div class="biomart-results-content"></div>
        <div class="biomart-results-footer"></div>
      </section>
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

<c:import url="/_js_includes.jsp?path=../&version=2" context="/"/>

<c:import url="/_templates.jsp" context="/"/>

<script src="/martform2/js/app.js"></script>

<script type="text/javascript">
  $(function () {
      var path = BM.jsonify(BM.jsonify().fragment).path
      if (path) {
        path = path.split('/')
        window.BM_App = BM.MartForm.init({
          guiName: path[0],
          martName: path[1],
          el: $('#biomart-wrapper')
        })
      }
  });
</script>

