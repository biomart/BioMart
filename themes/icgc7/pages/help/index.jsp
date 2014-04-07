<!doctype html>
<%@ page language="java"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="currentPage" scope="request">
  ICGC Data Portal Help
</c:set>
<html>
<head>
  <c:import url="/conf/config.jsp" context="/"/>
  <title>${requestScope.currentPage}</title>

    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

  <c:import url="/_head.jsp?path=/" context="/"/>
</head>
<!--[if lt IE 7 ]> <body class="biomart layout1 main ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body class="biomart layout1 main ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body class="biomart layout1 main ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body class="biomart layout1 main"> <!--<![endif]--> 

<div id="biomart-top-wrapper">
  <div id="biomart-header">
    <c:import url="/_header.jsp?path=/" context="/"/>
  </div>

  <c:import url="/_context.jsp?path=/" context="/"/>

  <div id="biomart-wrapper">
    <h2 class="gradient-grey-reverse ui-corner-top">Help</h2>
    <div id="biomart-content" class="ui-widget-content clearfix">
    <h3>Contacting Us For Help</h3>
    <p>
    If you have questions related to the use of the ICGC Data Portal or its data, please send an email to <a href='mailto:dcc-support@lists.oicr.on.ca?subject=Help%20page%20question'>dcc-support@lists.oicr.on.ca</a>.
    </p>
    <br />
    <p>
    For general questions related to the ICGC project, please refer to the ICGC contacts page at <a href='https://www.icgc.org/icgc/contacts'>https://www.icgc.org/icgc/contacts</a>.
    </p>

    </div>
    <div id="biomart-content-footer" class="ui-widget-content ui-corner-bottom gradient-grey clearfix">
      <p class="version">Powered by <a href="http://www.biomart.org/" title="Visit biomart.org">BioMart</a></p>
    </div>
  </div>

  <div id="biomart-footer">
    <c:import url="/_footer.jsp?path=/" context="/"/>
  </div>
</div>

<c:import url="/_js_includes.jsp?path=/" context="/"/>

<script type="text/javascript">
  // Do not touch this
    $(document).ready(function() {
        $.publish('biomart.login'); 
    $.subscribe('biomart.restart', {refresh:function(){location=location.href}}, 'refresh');
    });
</script>

</body>
</html>
