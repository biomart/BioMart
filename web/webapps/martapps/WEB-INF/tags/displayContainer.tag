<%@ tag import="javax.servlet.jsp.JspWriter" %>
<%@ tag import="java.net.URLEncoder" %>
<%@ tag import="java.util.regex.Pattern" %>
<%@ tag import="java.util.regex.Matcher" %>
<%@ tag import="java.io.IOException" %>
<%@ tag import="java.util.Map" %>
<%@ tag import="java.util.HashMap" %>
<%@ tag import="java.util.Set" %>
<%@ tag import="java.util.HashSet" %>
<%@ tag import="org.biomart.objects.enums.GuiType" %>
<%@ tag import="org.biomart.api.lite.Mart" %>
<%@ tag import="org.biomart.api.lite.GuiContainer" %>
<%@ attribute name="item" required="true" %> 
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> 

<%!
  String getMartHTML(GuiContainer container) throws IOException {
    StringBuilder builder = new StringBuilder();
    Set<String> groups = new HashSet<String>();
    builder.append("<ul class=\"marts\">");
      for (Mart mart : container.getMartList()) {
        String url = null;

        if (!mart.isHidden()) {
          GuiType type = container.getGuiType();
          String containerName;
          String martName;
          String displayName;

          if (type != null) {
            url = type.getUrl();
          }

          if (url != null) {
            if (mart.getGroupName() != null && !"".equals(mart.getGroupName())) {
              if (!groups.contains(mart.getGroupName())) {
                containerName = container.getName();
                martName = mart.getGroupName();
                displayName = mart.getGroupName();
                groups.add(mart.getGroupName());
              } else {
                continue;
              }
            } else {
              containerName = container.getName().replaceFirst("%s", mart.getName());
              martName = mart.getName();
              displayName = mart.getDisplayName();
            }
            builder.append("<li><a rel=\"noindex nofollow\" href=\"" + 
				url.replaceFirst("%s", containerName).replaceFirst(
					Pattern.quote("%s"), Matcher.quoteReplacement( martName )) + "\">");
            builder.append(displayName);
            builder.append("</a></li>\n");
          } else {
            builder.append("<li class=\"error\">Bad configuration: " + mart.getName() + "</li>");
          }
        }
      }
    builder.append("</ul>");
    return builder.toString();
  }

  void handleRootGuiContainer(JspWriter out, GuiContainer root) throws IOException {
    for (GuiContainer container : root.getGuiContainerList()) {
      GuiType type = container.getGuiType();

      if (type == null) {
        out.println("Bad configuration: " + container.getDisplayName());
        continue;
      }

      // Skip MartReports
      if (type.equals(GuiType.get("martreport"))) {
        continue;
      }

      String id = container.getName().replaceAll(" ", "");

      out.println("<h3 class=\"ui-widget-header ui-corner-top level-1\">" + container.getDisplayName() + "</h3>");

      // MartSearch 
      if (type.equals(GuiType.get("martsearch"))) {
        out.println("<div class=\"ui-widget-content  MART_SEARCH level-1\" id=\"hsapiens_gene_ensembl_2\">"
          + "<div>"
            + "<form type=\"GET\" action=\"./martsearch\">"
            + "<input type=\"hidden\" value=\"" + container.getName() + "\" name=\"gui\">"
            + "<input type=\"text\" name=\"q\" size=\"30\" class=\"gradient-grey-reverse query\">"
            + "<button class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon\" type=\"submit\">"
            + "<span class=\"ui-button-icon-primary ui-icon ui-icon-search\"></span>"
            + "<span class=\"ui-button-text\">Go</span>"
            + "</button>"
            + "</form>"
            + "<p class=\"description\">"
            + container.getDescription()
            + "</p>"
          + "</div>"
        + "</div>");
        continue;
      }

      out.println("<div id=\"" + id + 
        "\" class=\"ui-widget-content " + (container.getGuiContainerList().isEmpty() ?  "" : "ui-tabs ") + 
        type.getName() + " level-1 clearfix\">");

      if (container.isLeaf()) {
        out.println(getMartHTML(container));
      } else {
        handleSecondLevelGuiContainer(out, id, container);
      }

      out.println("</div>");
    }
  }

  void handleSecondLevelGuiContainer(JspWriter out, String parent, GuiContainer container) throws IOException {
    StringBuilder menuBuilder = new StringBuilder();
    StringBuilder containerBuilder = new StringBuilder();

    menuBuilder
      .append("<ul class=\"container-tabs ui-tabs-nav ui-helper-reset ui-helper-clearfix ")
      .append("ui-widget-header ui-corner-all\">");

    int i = 1;
    for (GuiContainer child : container.getGuiContainerList()) {
      String id = child.getName().replaceAll(" ", "") + "-container";
      menuBuilder
        .append("<li class=\"item-").append(""+i)
        .append(" ui-state-default ui-corner-top ui-corner-top")
        .append(i==1 ? " ui-tabs-selected ui-state-active" : "")
        .append("\">")
        .append("<a rel=\"noindex nofollow\" href=\"#").append(id)
        .append("\">").append(child.getDisplayName())
        .append("</a>")
        .append("</li>");
      containerBuilder
        .append("<div class=\"subcontainer ui-widget-content ui-corner-bottom item-"+i)
        .append(" ui-tabs-panel ")
        .append(i==1 ? "" : "ui-tabs-hide").append(" clearfix\"")
        .append(" id=\"").append(id).append("\">");
      
      if (child.isLeaf()) {
        containerBuilder.append(getMartHTML(child));
      } else {
        containerBuilder
          .append("<div class=\"accordion\">")
          .append(handleThirdLevelGuiContainer(container.getName(), child))
          .append("</div>");
      }

      containerBuilder.append("</div>");
      i++;
    }

    menuBuilder.append("</ul>");

    out.println(menuBuilder.toString());
    out.println(containerBuilder.toString());
    out.println("<script>"
      + "$(function() {"
        + "$('#" + parent + "').tabs()"
      + "})"
      + "</script>");
  }

  String handleThirdLevelGuiContainer(String parent, GuiContainer container) throws IOException {
    StringBuilder builder = new StringBuilder();
    int i=0;
    for (GuiContainer child : container.getGuiContainerList()) {
      String id = child.getName().replaceAll(" ", "") + "-container";
      builder
        .append("<h4>").append(child.getDisplayName()).append("</h4>")
        .append("<div>")
        .append(getMartHTML(child))
        .append("</div>");
      i++;
    }
    return builder.toString();
  }
%>

<%
  GuiContainer root = (GuiContainer)request.getAttribute(item);
  handleRootGuiContainer(out, root);
%>
