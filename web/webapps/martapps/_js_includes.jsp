<%@ page language="java" %>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<c:set var="basepath" value="${param.path}"/>
<script type="text/javascript" src="${basepath}conf/config.js.jsp"></script>
<!--[if IE]><jwr:script src="/js/lib/ie/excanvas.min.js"/><![endif]-->

<%-- Common libraries --%>
<jwr:script src="/lib.js"/>

<% if (Boolean.getBoolean("canvasExpress.include")) { %>
<script src="${siteUrl}js/lib/canvasExpress/canvasXpress.min.js"></script>
<script src="${siteUrl}js/lib/canvasExpress/ext-base.js"></script>
<script src="${siteUrl}js/lib/canvasExpress/ext-all-debug.js"></script>
<script src="${siteUrl}js/lib/canvasExpress/color-field.js"></script>
<script src="${siteUrl}js/lib/canvasExpress/ext-canvasXpress.js"></script>
<script src="${siteUrl}js/lib/canvasExpress/datadumper.min.js"></script>
<script src="${siteUrl}js/lib/canvasExpress/sprintf.min.js"></script>
<% } %>

<c:choose>
	<c:when test="${param.version eq '2'}">
    <jwr:script src="/lib_v2.js"/>
    <jwr:script src="/biomart_v2.js"/>
	</c:when>
	<c:otherwise>
    <jwr:script src="/biomart_v1.js"/>
    <script type="text/javascript">
    biomart.METHOD = '${METHOD}';
    <c:if test="${METHOD=='POST'}">
    biomart.POST = {
      <c:forEach var='parameter' items='${param}' varStatus="status"> 
        <c:if test="${parameter.key!='path'}">
          '${parameter.key}': '${parameter.value}'<c:if test="${!status.last}">,</c:if>
        </c:if>
      </c:forEach>
    }
    </c:if>
    </script>
	</c:otherwise>
</c:choose>
