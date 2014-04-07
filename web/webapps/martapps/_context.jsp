<%@ page language="java" %>
<%@ page session="false" %>
<%@ page import="javax.servlet.http.HttpSession" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div id="biomart-topnav" class="clearfix">
  <ul id="biomart-breadcrumbs" class="clearfix">
    <li class="first">
      <a href="${requestScope.siteUrl}">
        <bm:message code="home" capitalize="true"/>
      </a>
    </li>
    <c:if test="${requestScope.currentPage != null}">
      <li>
        ${requestScope.currentPage}
      </li>
    </c:if>
    <c:if test="${not empty guiContainer}">
      <li>
        ${guiContainer.displayName}
      </li>
    </c:if>
  </ul>
  <%
    HttpSession s = request.getSession(false);
    if (s == null) {
    } else { 
      Object userData = s.getAttribute("userData");
      if (userData != null) {
        pageContext.setAttribute("userData", userData);
        pageContext.setAttribute("loggedIn", true);
      } else {
        pageContext.setAttribute("loggedIn", false);
      }
    }
  %>
  <ul class="others clearfix">
    <li id="biomart-login"> 
      <span id="biomart-user">
        <c:choose>
          <c:when test="${loggedIn}">
            <bm:message code="Logged in as"/>: <a href="#" class="profile" title="View profile"><c:out value="${userData.email != null ? userData.email : userData.identifier}"/></a>
          </c:when>
          <c:otherwise>
            <bm:message code="Not logged in"/>
          </c:otherwise>
        </c:choose>
      </span> (
      <a class="action sign-in" href="#" <c:if test="${loggedIn}">style="display: none"</c:if>><bm:message code="login" capitalize="true"/></a>
      <a class="action sign-out" href="${service.url}user/logout" <c:if test="${not loggedIn}">style="display: none"</c:if>><bm:message code="logout" capitalize="true"/></a>
      )
      <div id="biomart-auth-info" class="gradient-grey-reverse" style="display: none">
        <span class="ui-icon ui-icon-close" title="Close"></span>
        <p><bm:message code="unique identifier" capitalize="true"/>: <span class="identifier"><c:if test="${loggedIn}">${userData.identifier}</c:if></span></p>
        <p><bm:message code="email" capitalize="true"/>: <span class="email"><c:if test="${loggedIn}">${userData.email}</c:if></span></p>
      </div>
    </li>
    <c:if test="${fn:length(requestScope.locations) > 0}">
      <li>
      <label for="biomart-locations"><bm:message code="you are on the" capitalize="true"/>: </label>
        <select id="biomart-locations">
          <c:forEach var="entry" items="${requestScope.locations}">
            <c:choose>
              <c:when test="${fn:length(entry.value) > 1}">
                <c:forEach var="location" items="${entry.value}">
                  <option value="${location.url}" <c:if test="${location.current}">selected</c:if>>${entry.key} <bm:message code="website"/> (${location.label})</option>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <option value="${entry.value[0].url}" <c:if test="${entry.value[0].current}">selected</c:if>>${entry.key} <bm:message code="website"/></option>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </select>
      </li>
    </c:if>
  </ul>
</div>

<div id="biomart-login-dialog" style="display: none" title="Sign In">
  <p class="info login-info"><bm:message code="Sign in using your openid account"/></p>
	<form id="biomart-login-form" action="/martservice/user/auth" method="GET"></form>
  <p class="login-info error invisible">&nbsp;</p>
  <div class="login-help clearfix">
    <bm:message code="login_help_text" empty=""/>
  </div>
	<div id="biomart-openid-post-form" style="visibility: hidden"></div>
	<div class="loading" style="display: none"></div>
</div>

<div id="biomart-error-dialog" style="display:none">
  <p class="msg"></p>
  <p><bm:message code="Click ok to go home"/></p>
  <p class="button"><button>OK</button></p>
</div>

<%
  if (s != null) {
    request.setAttribute("flash", s.getAttribute("flash"));
  }
%>
<c:if test="${flash.size > 0}">
<div id="biomart-flash-message">
  <p>${flash.message}</p>
  <span class="ui-icon ui-icon-close" title="<bm:message code="Close this message"/>"></span>
</div>
</c:if>
