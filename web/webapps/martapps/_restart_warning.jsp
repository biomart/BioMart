<!doctype html>
<%@ page language="java" %>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="/WEB-INF/bmtaglib.tld" prefix="bm" %>
<div id="biomart-warning" title="Warning">
	<p>
		<img src="../images/icon-alert.png" alt="" style="float: left; margin: 0 1em 3em 0"/>
    <bm:message code="new_query_warning_msg"/>
	</p>
  <p><bm:message code="new_query_continue_msg" capitalize="true"/></p>
  <p class="remember"><input type="checkbox" id="biomart-no-warning"/><label for="biomart-no-warning"><bm:message code="new_query_dont_warn_msg" capitalize="true"/></label></p>
</div>

