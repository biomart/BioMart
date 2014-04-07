<%@ page language="java"%>
<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8"%>

<link rel="stylesheet" href="${requestScope.siteUrl}includes/style.css" target="biomart_out" type="text/css">
<link rel="stylesheet" href="${requestScope.siteUrl}includes/css_injector_2.css" target="biomart_out" type="text/css">
<link rel="stylesheet" href="${requestScope.siteUrl}includes/icgc.css" type="text/css"/>

<header class="clearfix">
  <h1 class="subheading clearfix">
    <a href="http://www.icgc.org/" class="ir">
      International Cancer Genome Consortium
    </a>
  </h1>

  <p>Data Portal</p>

  <div id="main-logo" class="subheading">
    <jsp:include page="_header.html"/>
  </div>

  <div id="country-flag" class="subheading">
    <img src="${requestScope.siteUrl}includes/flags/${requestScope.currLocation.code}.png"/>
  </div>
</header>

<div id="framework-subheader">
    <div class="nav-primary">  
        <div id="nav-blue" class="nav block block-menu region-odd even region-count-1 count-2" style="margin: 0pt;">
            <div class="content header-menu">
 
                <ul class="menu">
                    <li class="leaf Home"><a href="/">Home</a></li>
                    <li class="leaf first Overview" id="menu-item-custom-id-1"><a href="http://www.icgc.org/" target="biomart_out" title="" class="">ICGC Home</a></li>
                    <li class="leaf Publication-Policy"><a href="http://www.icgc.org/icgc/goals-structure-policies-guidelines/e3-publication-policy" target="biomart_out">Publication Policy</a></li>
                    <li class="leaf Download"><a href="ftp://data.dcc.icgc.org" target="biomart_out">Download Data</a></li>
                    <!--                    <li class="leaf Documentation"><a href="/pages/docs/" target="_self">Documentation</a></li> -->

                    <li class="leaf Documentation"><a href="http://dcc.icgc.org/pages/docs/" target="biomart_out">Documentation</a></li>
            <!--    <li class="leaf FAQ"><a target="_self" href="/pages/faq/">FAQ</a></li> -->
                    <li class="leaf last Help"><a href="/pages/help/" target="_self">Help</a></li>
                </ul>
            </div>
        </div>
    </div>
</div>
