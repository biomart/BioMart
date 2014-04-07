<%@ page language="java"%>
<%@ page session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<div id="biomart-results">
  <div class="content hidden">
    <h3><span class="loading-msg"><bm:message code="preview" capitalize="true"/></span></h3>
    <div class="actions clearfix">
      <%--<span id="biomart-count-estimate" title="<bm:message code="estimated_entries_msg"/>">
        <bm:message code="entry" plural="true" capitalize="true"/>:
        <span class="biomart-count-filtered">-</span>
        /
        <span class="biomart-count-total">-</span>
      </span> --%>
      <a href="#" class="bookmark">
        <span class="wrapper">
          <span class="ui-icon ui-icon-bookmark"></span><bm:message code="bookmark" capitalize="true"/></span>
      </a>
      <a href="#" class="explain">
        <span class="wrapper">
          <span class="ui-icon ui-icon-carat-2-e-w"></span>REST / SOAP</span>
      </a>
      <a href="#" class="explain-sparql">
        <span class="wrapper">
          <span class="ui-icon ui-icon-script"></span>SPARQL</span>
      </a>
      <a href="#" class="explain-java">
        <span class="wrapper">
          <span class="ui-icon ui-icon-script"></span>Java</span>
      </a>
      <a href="#" class="export">
        <span class="wrapper">
          <span class="ui-icon ui-icon-arrowthickstop-1-s"></span>
          <bm:message code="download data" capitalize="true"/>
        </span>
      </a>
      <a href="#" class="galaxy">
        <span class="wrapper">
          <span class="ui-icon ui-icon-transferthick-e-w"></span>
          <bm:message code="export to Galaxy" capitalize="true"/>
        </span>
      </a>
      <a href="#" class="blue large awesome edit" title="<bm:message code="go back to search options" capitalize="true"/>">
        <span class="wrapper">
          <span class="ui-icon ui-icon-arrowreturnthick-1-w"></span>
          <bm:message code="back" capitalize="true"/>
        </span>
      </a>
    </div>
    <div class="data"></div>
    <iframe name="results" class="streaming"></iframe>
  </div>
</div>

<form id="biomart-export-form" method="POST" action="${service.url}results" target="export-iframe">
	<input type="hidden" name="download" value="true"/>
	<input type="hidden" name="query"/>
</form>

<div id="biomart-view-xml" title="REST / API <bm:message code="query" capitalize="true"/>">
  <textarea></textarea>
</div>

<div id="biomart-view-sparql" title="SPARQL <bm:message code="query" capitalize="true"/>">
  <textarea></textarea>
</div>

<div id="biomart-view-java" title="Java <bm:message code="query" capitalize="true"/>">
  <textarea></textarea>
</div>
<div id="biomart-view-bookmark" title="<bm:message code="bookmark" capitalize="true"/> URL">
  <input type="text"/>
</div>

