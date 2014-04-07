<%@ tag import="javax.servlet.http.HttpSession" %>
<%@ tag import="org.biomart.api.lite.Dataset" %>
<%@ tag import="org.biomart.api.factory.MartRegistryFactory" %>
<%@ tag import="org.biomart.api.Portal" %>
<%@ tag import="java.util.List" %>
<%@ tag import="java.util.ArrayList" %>
<%@ tag import="org.codehaus.jackson.map.ObjectMapper"%>
<%@ attribute name="var" rtexprvalue="true" required="true" %> 
<%@ attribute name="mart" required="true" %> 
<%@ attribute name="json" required="false" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 
<%
  HttpSession s = request.getSession(false);
  String username = null;
  if (s != null) {
    username = (String)s.getAttribute("openid_identifier");
  }
  if (username == null) username = "";
  MartRegistryFactory factory = (MartRegistryFactory)request.getAttribute("registryFactoryObj");
  Portal portal = new Portal(factory, username);
  List<Dataset> datasets = portal.getDatasets(mart);

  if (Boolean.parseBoolean(json)) {
    ObjectMapper mapper = new ObjectMapper();
    request.setAttribute(var + "Json", mapper.writeValueAsString(datasets));
  }
    
  request.setAttribute(var, datasets);
%>    


