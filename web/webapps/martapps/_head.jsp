<%@ page language="java"%>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jawr.net/tags" prefix="jwr" %>

<meta name="robots" content="noindex, nofollow">
<link rel="shortcut icon" type="image/x-icon" href="/favicon.ico" />
<c:set var="basepath" value="${param.path}"/>
<%-- Timestamp for appending to resoures to force refresh from server --%>
<jsp:useBean id="dateValue" class="java.util.Date" />
<c:set scope="request" var="timestamp"><fmt:formatDate value="${dateValue}" pattern="yyyyMMddHH"/></c:set>
<%
  if (request.isSecure()) {
    request.setAttribute("siteUrl", System.getProperty("https.url"));
  } else {
    request.setAttribute("siteUrl", System.getProperty("http.url"));
  }

	String method = request.getMethod();
	request.setAttribute("METHOD", method);
 	String isDebug = System.getProperty("biomart.debug", "true");
	application.setAttribute("debug", Boolean.parseBoolean(isDebug));
%>

<jwr:style src="/lib.css"/>

<c:choose>
	<c:when test="${param.version eq '2'}">
    <jwr:style src="/biomart_v2.css"/>
	</c:when>
	<c:otherwise>
    <jwr:style src="/biomart_v1.css"/>
	</c:otherwise>
</c:choose>

<!--[if IE]>
<link href="${basepath}css/ie.css?v=${timestamp}" rel="stylesheet" />
<![endif]-->

<c:if test="${param.includemaincss != 'false'}">
  <link href="css/main.css?v=${timestamp}" rel="stylesheet" />
</c:if>

<script src="/js/lib/modernizr-1.5.min.js"></script>

<c:choose>
	<c:when test="${param.version eq '2'}">
    <script src="/js/lib/jquery-1.6.1.min.js"></script>
	</c:when>
	<c:otherwise>
    <script src="/js/lib/jquery-1.4.4.min.js"></script>
	</c:otherwise>
</c:choose>

<script type="text/javascript">
// Dummy console.log
if (typeof window.console == 'undefined') { window.console = { log: function() {} } }
// iPhone and iPad detection
var iOS = /iPhone|iPad/.test(navigator.platform)
if (iOS) document.documentElement.className += ' ios'
</script>

<c:import url="/head_extra.jsp" context="/includes" charEncoding="UTF-8"/>

