package org.biomart.web;

import com.google.common.collect.ImmutableMap;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.biomart.api.factory.XmlMartRegistryModule;
import org.biomart.processors.ProcessorRegistry;
import org.biomart.processors.TSV;

/**
 *
 * @author jhsu
 */
public class TestWebServiceModule extends ServletModule {
    @Override
    protected void configureServlets() {
        install(new XmlMartRegistryModule());

        ProcessorRegistry.register("TSV", TSV.class);

        serve("/*").with(GuiceContainer.class,
            new ImmutableMap.Builder<String,String>()
               .put(PackagesResourceConfig.PROPERTY_PACKAGES, "org.biomart.api;org.codehaus.jackson.jaxrs")
               .build());
    }
}
