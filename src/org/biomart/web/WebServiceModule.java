package org.biomart.web;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import java.util.HashMap;
import java.util.Map;
import org.biomart.api.factory.XmlMartRegistryModule;
import org.biomart.galaxy.GalaxyEndpointServlet;
import org.biomart.processors.ProcessorModule;
import org.mortbay.servlet.GzipFilter;

/**
 *
 * @author jhsu
 */
public class WebServiceModule extends ServletModule {
    @Override
    protected void configureServlets() {
        install(new XmlMartRegistryModule());
        install(new ProcessorModule());

        bind(GzipFilter.class).in(Scopes.SINGLETON);

        bind(GalaxyEndpointServlet.class).in(Scopes.SINGLETON);

        filter("*").through(AuthFilter.class);
        filter("*").through(FlashMessageFilter.class);
        filter("*").through(LocationsFilter.class);
        filter("/admin/*").through(ForceHttpsFilter.class);
        filter("*").through(GzipFilter.class, new ImmutableMap.Builder<String,String>()
                .put("mimeTypes", "text/css,text/javascript,image/svg+xml")
                .build());

        filter("*").through(PortalFilter.class);

        Map<String,String> initParams = new HashMap<String,String>(){{
            put("javax.ws.rs.Application", org.biomart.api.rest.App.class.getName());
        }};

        if (!Boolean.getBoolean("biomart.debug")) {
            initParams.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, CacheControlFilter.class.getName());
        }

        initParams.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, JSONCallbackResponseFilter.class.getName());
        initParams.put(PackagesResourceConfig.PROPERTY_PACKAGES, "org.biomart.api;org.codehaus.jackson.jaxrs");

        serve("/galaxy").with(GalaxyEndpointServlet.class);

        filter("/martservice/*", "/biomart/*", "/martsemantics/*").through(GuiceContainer.class, initParams);
    }
}
