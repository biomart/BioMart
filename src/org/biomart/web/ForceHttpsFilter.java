package org.biomart.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.biomart.common.resources.Log;

// Force HTTPS for all connnections
@Singleton
public class ForceHttpsFilter implements Filter {
    private String secureUrl;

    public ForceHttpsFilter() {
        secureUrl = System.getProperty("https.url");
    }

	@Override
	public void init(FilterConfig cfg) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
        if (secureUrl != null && !request.isSecure()) {
            Log.info("Redirecting user to secure URL: " + secureUrl);
            ((HttpServletResponse)response).sendRedirect(secureUrl + ((HttpServletRequest)request).getRequestURI());
            return;
        }
        chain.doFilter(request, response);
        return;
	}

	@Override
	public void destroy() {}
}