package org.biomart.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.inject.Singleton;

// Force authenticated users to use HTTPS
@Singleton
public class LocationsFilter implements Filter {
    public LocationsFilter() {}

	@Override
	public void init(FilterConfig cfg) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
        if (!GuiceServletConfig.isMediaResource(request)) {
            request.setAttribute("locations", GuiceServletConfig._locations);
            request.setAttribute("currLocation", GuiceServletConfig._currLocation);
        }
        chain.doFilter(request, response);
        return;
	}

	@Override
	public void destroy() {}
}