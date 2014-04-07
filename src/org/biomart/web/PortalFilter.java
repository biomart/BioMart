package org.biomart.web;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.biomart.api.factory.MartRegistryFactory;

@Singleton
public class PortalFilter implements Filter {
    @Inject Injector injector;

    @Inject
    private PortalFilter() {}

	@Override
	public void init(FilterConfig cfg) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
        if (!GuiceServletConfig.isMediaResource(request)) {
            request.setAttribute("registryFactoryObj", GuiceServletConfig.getMartRegistryFactory());
        }
        chain.doFilter(request, response);
        return;
	}

	@Override
	public void destroy() {}
}