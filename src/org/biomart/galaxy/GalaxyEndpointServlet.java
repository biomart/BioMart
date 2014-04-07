/*
 * Copyright 2007 AOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.biomart.galaxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Galaxy endpoint for synchronous data
 * 
 * Will set a "GALAXY_URL" cookie, which will be used to send data to galaxy
 * main server.
 *
 * @author Jack Hsu <jack.hsu@oicr.on.ca
 */
public class GalaxyEndpointServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String url = request.getParameter("GALAXY_URL");

        if (url != null) {
            Cookie c = new Cookie("GALAXY_URL", url);
            c.setPath("/");
            response.addCookie(c);
        }
        response.sendRedirect(System.getProperty("https.url", System.getProperty("http.url")));
    }

    private static final long serialVersionUID = 1L;

}
