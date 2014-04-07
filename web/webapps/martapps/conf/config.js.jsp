<%@ page language="java" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%
	response.setHeader("Content-Type", "application/x-javascript");
	response.setHeader("Cache-Control", "max-age=3600; must-revalidate");

  if (request.isSecure()) {
    pageContext.setAttribute("siteUrl", System.getProperty("https.url"));
  } else {
    pageContext.setAttribute("siteUrl", System.getProperty("http.url"));
  }
%>
<jsp:include page="config.jsp"/>
var BIOMART_CONFIG = {
  securePort: <%= System.getProperty("https.port") %>,
	labels: {
	<c:forEach var="curr" items="${labels}" varStatus="status">
		'${curr.key}': '${curr.value}'<c:if test="${not status.last}">,</c:if>
	</c:forEach>
	},
	service: {
	<c:forEach var="curr" items="${service}" varStatus="status">
		'${curr.key}': '${curr.value}'<c:if test="${not status.last}">,</c:if>
	</c:forEach>
  },
  siteURL: '${siteUrl}'
};
