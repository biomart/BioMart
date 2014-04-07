<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>

<%-- 
  Handlebars.js templates
  http://handlebars.strobeapp.com/
--%>

<script type="text/html" id="mart">
  <option value="{{name}}" class="model-mart {{name}}">{{displayName}}</option>
</script>

<script type="text/html" id="selectBox">
  <label class="item-name" for="{{id}}">{{label}}:</label>
  <select class="{{className}}" id="{{id}}"></select>
</script>

<script type="text/html" id="dataset">
  <option value="{{name}}" class="model-dataset {{name}}">{{displayName}}</option>
</script>

<script type="text/html" id="attribute">
  <input type="checkbox" id="{{id}}"/>
  <label for="{{id}}">{{displayName}}</label>
</script>

<script type="text/html" id="filter">
  <label class="filter-item-name">{{displayName}}</label>
  {{#if values.length}}
    <select class="filter-field filter-field-values filter-value" {{#isMultiple}}multiple{{/isMultiple}}>
      {{#values}}
        <option value="{{name}}">{{displayName}}</option>
      {{/values}}
    </select>
  {{/if}}
  {{#if filters.length}}
  <select class="filter-field filter-field-filters" {{#isMultiple}}multiple{{/isMultiple}}>
      {{#filters}}
        <option value="{{name}}">{{displayName}}</option>
      {{/filters}}
    </select>
  {{/if}}
  {{#hasText}}
    <input type="text" class="filter-field filter-field-text filter-value"/>
  {{/hasText}}
  {{#hasUpload}}
    <div class="filter-field-upload">
      <textarea class="filter-field filter-field-upload-text filter-value"></textarea>
      <input type="file" class="filter-field filter-field-upload-file"/>
    </div>
  {{/hasUpload}}
</script>

<script type="text/html" id="container">
  <div class="model-container model-container-{{name}}" id="{{id}}">
    <h4>{{displayName}}</h4>
  </div>
</script>

<script type="text/html" id="pages">
  <a href="javascript:void(0)" class="ui-page {{#unless prevPage}}invisible{{/unless}}" data-page="{{prevPage}}">
    &laquo; Previous
  </a>
  {{#each pages}}
    <a href="javascript:void(0)" class="ui-page {{#if this.isActive }}ui-page-active{{/if}}" data-page="{{this.num}}">
      {{this.num}}
    </a>
  {{/each}}
  <a href="javascript:void(0)" class="ui-page {{#unless nextPage}}invisible{{/unless}}" data-page="{{nextPage}}">
    Next &raquo;
  </a>
</script>

<script type="text/html" id="resultsMeta">
  <div class="biomart-info">
    <p>
      Displaying results <span class="start">{{start}}</span>-<span class="end">{{end}}</span> out of 
      <span class="total">{{total}}</span>
    </p>
    {{#if hasMoreData}}
      <p class="biomart-fyi">
        Results beyond {{limit}} are not displayed, use the download link to retrieve the complete results. 
        Click on a column heading to sort.
      </p>
    {{/if}}
  </div>
</script>

<script type="text/html" id="tableHeader">
  <tr>
    {{#each header}}
    <td {{#if this.sortable}}class="ui-table-sortable"{{/if}}>
      <p>
        <span class="ui-icon"></span>
        {{this.text}}
      </p>
    </td>
    {{/each}}
  </tr>
</script>

<script type="text/html" id="tableRow">
  <tr>
    {{#each row}}
    <td>
        {{#if this}}
          {{{this}}}
        {{else}}
          <span class="empty">
            <bm:message code="no_data" capitalize="true"/>
          </span>
        {{/if}}
    </td>
    {{/each}}
  </tr>
</script>

<script type="text/html" id="query">
  <div class="biomart-query-toolbar ui-fixed-bl ">
    <div class="biomart-query-content ui-corner-top clearfix">
      <div class="biomart-quer-explain clearfix">
        {{#each compilationTargets}}
          <span class="biomart-compile-target {{this.target}}" data-target="{{this.target}}">
            <span class="ui-icon ui-icon-script"></span>
            <span class="ui-text">{{this.label}}</span>
          </span>
        {{/each}}
      </div>
    </div>
  </div>
</script>

<script type="text/html" id="queryDialog">
  <div class="query-dialog" title="{{title}}">
    <textarea></textarea>
  </div>
</script>
