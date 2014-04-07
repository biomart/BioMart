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
import javax.servlet.http.HttpSession;
import org.biomart.common.resources.Log;

// 1. Force authenticated users to use HTTPS
// 2. Set authenticated attribute on all requests
@Singleton
public class AuthFilter implements Filter {
    private String secureUrl;

    public AuthFilter() {
        secureUrl = System.getProperty("https.url");
    }

	@Override
	public void init(FilterConfig cfg) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
        if (!GuiceServletConfig.isMediaResource(request)) {
            // If user is logged
            HttpSession session = ((HttpServletRequest)request).getSession(false);
            if (session != null && session.getAttribute("openid_identifier") != null) {
                request.setAttribute("authenticated", true);
                // If secure url is declared and request not secured, redirect user
                if (secureUrl != null && !request.isSecure()) {
                    Log.info("Redirecting user to secure URL: " + secureUrl);
                    ((HttpServletResponse)response).sendRedirect(secureUrl);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
        return;
	}

	@Override
	public void destroy() {}
}