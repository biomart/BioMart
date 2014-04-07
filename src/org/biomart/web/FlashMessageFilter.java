package org.biomart.web;

import com.google.inject.Singleton;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Singleton
public class FlashMessageFilter implements Filter {
	@Override
	public void init(FilterConfig cfg) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
        if (!GuiceServletConfig.isMediaResource(request)) {
            HttpSession session = ((HttpServletRequest)request).getSession(false);

            if (session != null) {
                Flash f = null;

                f = (Flash)session.getAttribute("flash");

                if (f == null) {
                    session.setAttribute("flash", new Flash());
                }
            }
        }
        chain.doFilter(request, response);
        return;
	}

	@Override
	public void destroy() {}
}