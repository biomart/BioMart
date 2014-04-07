<!doctype html>
<%@ page language="java"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="currentPage" scope="request">
  Dataset Summary
</c:set>
<html>
<head>
  <c:import url="/conf/config.jsp" context="/"/>
  <title>${requestScope.currentPage}</title>

    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

  <c:import url="/_head.jsp?path=../../" context="/"/>

  <link type="text/css" href="css/summary-table.css" rel="stylesheet" />
  <link type="text/css" href="css/svg-style.css" rel="stylesheet" />
  <script type="text/javascript" src="/pages/js/d3.v2.min.js"></script>
</head>
<!--[if lt IE 7 ]> <body class="biomart layout1 main ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body class="biomart layout1 main ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body class="biomart layout1 main ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body class="biomart layout1 main"> <!--<![endif]--> 

<div id="biomart-top-wrapper">
  <div id="biomart-header">
    <c:import url="/_header.jsp?path=../../" context="/"/>
  </div>

  <c:import url="/_context.jsp?path=../../" context="/"/>

  <div id="biomart-wrapper">
    <div id="biomart-content" class="clearfix">

        <h3 class="ui-widget-header ui-corner-top">
        Dataset Summary
        </h3>



<div class="ui-widget-content ui-tabs martform level-1 clearfix ui-widget ui-corner-all" id="default">
    <ul class="container-tabs ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all">
        <li class="item-1 ui-state-default ui-corner-top ui-corner-top ui-tabs-selected ui-state-active">
            <a href="#dccdata-container" rel="noindex nofollow">Data Portal Contents</a>
        </li>
        <li class="item-2 ui-state-default ui-corner-top ui-corner-top">
            <a href="#rawdata-container" rel="noindex nofollow">Raw Data Availability</a>
        </li>
        <li class="item-3 ui-state-default ui-corner-top ui-corner-top">
            <a href="#donors-container" rel="noindex nofollow">Cumulative Donors</a>
        </li>
    </ul>
    <div id="dccdata-container" class="subcontainer ui-widget-content ui-corner-bottom item-1 ui-tabs-panel clearfix">
        <jsp:include page="_summary.html" />
    </div>
    <div id="rawdata-container" class="subcontainer ui-widget-content ui-corner-bottom item-2 ui-tabs-panel clearfix ui-tabs-hide">
        <jsp:include page="_summary-raw_data.html" />
    </div>
    <div id="donors-container" class="subcontainer ui-widget-content ui-corner-bottom item-3 ui-tabs-panel clearfix ui-tabs-hide">
        <jsp:include page="_summary-cumulative_donors.html" />
    </div>

    <div id='dialogbox' style='display: none;'></div>    
    <script>

    $(document).ready(function() {
        $(function() {$('#default').tabs()});
        $(function() {
            $("a.rawdatalink").click(function(){
                var queryurl = $(this).attr('href');
                $.ajax({
                    url: queryurl,
                    success: function(data){
                        $('#dialogbox').html("<pre>"+data+"</pre>");
                        $('#dialogbox').dialog({ width: 600});
                  }
                });
                return false;
            });
        });
    });
    </script>
</div>



      <div id="biomart-content-footer" class="clearfix">
        <p class="version">Powered by <a href="http://www.biomart.org/" title="Visit biomart.org">BioMart</a></p>
      </div>
    </div>

    <div id="biomart-footer">
      <c:import url="/_footer.jsp?path=../../" context="/"/>
    </div>
  </div>
</div>

<c:import url="/_js_includes.jsp?path=../../" context="/"/>

<script type="text/javascript">
  // Do not touch this
    $(document).ready(function() {
        $.publish('biomart.login'); 
    $.subscribe('biomart.restart', {refresh:function(){location=location.href}}, 'refresh');
    });
</script>

</body>
</html>
