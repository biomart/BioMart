<%@ page language="java" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page session="false" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.biomart.web.GuiceServletConfig" %>
<%
		// Labels
		request.setAttribute("labels", GuiceServletConfig.getLabelMap());

		// Service
		HashMap service = (HashMap)request.getAttribute("service");
		service = new HashMap(){{
			put("url", "/martservice/");
			put("type", "json");
		}};
		request.setAttribute("service", service);
%>
