<!doctype html>
<%@ page language="java"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="currentPage" scope="request">
  Test Page
</c:set>
<html>
<head>
  <c:import url="/conf/config.jsp" context="/"/>
  <title>${requestScope.currentPage}</title>

	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

  <c:import url="/_head.jsp?path=../" context="/"/>
</head>
<!--[if lt IE 7 ]> <body class="biomart layout1 main ie6 "> <![endif]--> 
<!--[if IE 7 ]>    <body class="biomart layout1 main ie7 "> <![endif]--> 
<!--[if IE 8 ]>    <body class="biomart layout1 main ie8 "> <![endif]--> 
<!--[if !IE]><!--> <body class="biomart layout1 main"> <!--<![endif]--> 

<div id="biomart-top-wrapper">
  <div id="biomart-header">
    <c:import url="/_header.jsp?path=../" context="/"/>
  </div>

  <c:import url="/_context.jsp?path=../" context="/"/>

  <div id="biomart-wrapper">
    <div id="biomart-content" class="clearfix">
      <div id="chart"></div>
    </div>

    <div id="biomart-content-footer" class="clearfix">
      <p class="version">Powered by <a href="http://www.biomart.org/" title="Visit biomart.org">BioMart</a></p>
    </div>
  </div>

  <div id="biomart-footer">
    <c:import url="/_footer.jsp?path=../" context="/"/>
  </div>
</div>

<c:import url="/_js_includes.jsp?path=../" context="/"/>

<script type="text/javascript">


// You still have to calculate the quantiles yourself.  Whatever.
var chartData = [[  2,   4,   6,   7,   8],
			  [  2,   5, 6.7,   9,  10],
	          [2.2,   6, 7.8,  10,  11],
	          [2.3, 4.5,   6,   7,   8],
	          [  2,   3,   4,   5,   6],
	          [  2, 2.9,   4,   5,   7],
	          [  2,   4, 4.4,   5,   8]];
// The Div id is the first param, the data object the second.
             
var header = ['group a', 'group b', 'group c', 'group d', 'group e', 'group f', 'group g'];

var renderer = this.renderer = biomart.renderer.get('boxplot');

var writee = renderer.getElement()
  .appendTo( $('#chart'))
  .width(1000);

renderer.printHeader(header, writee);
renderer.parse(chartData, writee);

renderer.draw(writee);

</script>


</body>
</html>
