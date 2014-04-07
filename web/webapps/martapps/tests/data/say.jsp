<%@ page language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<c:choose>
  <c:when test="${param.what eq 'error'}">
    <% response.setStatus(400); %>
  </c:when>
  <c:otherwise>
    <% response.setHeader("Content-Type", "application/json"); %>
    { "what": "${param.what}" }
  </c:otherwise>
</c:choose>
