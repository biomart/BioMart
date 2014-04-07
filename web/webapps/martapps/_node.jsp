<!doctype html>
<%@ page language="java"%>
<%@ page session="false" %>
<%@ taglib prefix="biomart" tagdir="/WEB-INF/tags" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>

<%-- Ignore MartReport GUI containers --%>
<c:if test="${root.guiType != 'martreport'}">
  <%-- For recursion --%>
  <c:set var="oldlevel" value="${level}"/>
  <c:forEach var="container" items="${root.guiContainerList}" varStatus="status">
    <c:set var="content">
    ${content}
      <c:if test="${!container.hidden && container.guiType != 'martreport'}">
        <%-- Print container HTML if top level --%>
        <c:if test="${level == 1}">
          <h3 class="ui-widget-header ui-corner-top ui-state-active level-${level}">${container.displayName}</h3>
          <div id="${fn:replace(container.name, " ", "")}" 
            class="ui-widget-content ${not empty container.guiContainerList ?  'ui-tabs' : ''} ${container.guiType} level-${level}">
          <%-- Description at the top if not MartSearch --%>
          <c:if test="${container.guiType != 'martsearch'}">
            <c:if test="${container.description != ''}">
              <p class="description">
                <!--${container.description}-->
              </p>
            </c:if>
          </c:if>
        </c:if>
        <c:choose>
          <c:when test="${container.leaf}">
            <c:set var="item" value="${container}" scope="request"/>
            <c:if test="${container.guiType != 'martsearch'}">
              <c:if test="${level > 1}">
                <c:set var="tabs">
                  ${tabs}
                  <li class="item-${status.count} ui-state-default ui-corner-top
                      <c:if test="${status.count == 1}">ui-tabs-selected ui-state-active</c:if>">
                    <a href="#${container.name}-container">${container.displayName}</a>
                  </li>
                </c:set>
              </c:if>
              <biomart:displayContainer item="${container}" level="${level}" count="${status.count}"/>
            </c:if>
          </c:when>
          <c:otherwise>
            <c:if test="${level < 3}">
              <c:set var="root" value="${container}" scope="request"/>
              <%-- Set new level for recursive call --%>
              <c:set var="level" value="${oldlevel+1}" scope="request"/>
              <jsp:include page="_node.jsp"/>
              <%-- Set level back to previous value after recursive call --%>
              <c:set var="level" value="${oldlevel}" scope="request"/>
            </c:if>
          </c:otherwise>
        </c:choose>
        <%-- close container HTML if top level --%>
        <c:if test="${level == 1}">
          <c:choose>
            <%-- Print search form if MartSearch --%>
            <c:when test="${container.guiType eq 'martsearch'}">
              <div>
              <form action="./martsearch" type="GET">
                 <input type="hidden" name="gui" value="${container.name}"/>
                 <input class="gradient-grey-reverse query" type="text" size="30" name="q"/>
                 <button type="submit" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon">
                   <span class="ui-button-icon-primary ui-icon ui-icon-search"></span>
                   <span class="ui-button-text"><bm:message code="search" capitalize="true"/></span>
                 </button>
               </form>
               <p class="description">
                 ${container.description}
               </p>
              </div>
            </c:when>
            </c:choose>
          </div>
        </c:if>
      </c:if>
    </c:set>
  </c:forEach>
  <c:if test="${not empty tabs}">
    <ul class="container-tabs ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all">
      ${tabs}
    </ul>
    <script type="text/javascript">
      $(function() {
          $('#${fn:replace(root.name, " ", "")}').tabs()
      })
    </script>
  </c:if>
  ${content}
</c:if>

