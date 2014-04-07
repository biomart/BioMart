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

package org.biomart.oauth.provider.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;
import net.oauth.server.OAuthServlet;

/**
 * A text servlet to echo incoming "echo" param along with userId
 *
 * @author Praveen Alavilli
 * @author John Kristian
 */
public class EchoServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try{
            OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
            OAuthAccessor accessor = SimpleOAuthProvider.getAccessor(requestMessage);
            SimpleOAuthProvider.VALIDATOR.validateMessage(requestMessage, accessor);
            String userId = (String) accessor.getProperty("user");
            
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("Your OpenId: " + userId);
            out.close();

        } catch (Exception e){
            e.printStackTrace();
            SimpleOAuthProvider.handleException(e, request, response, false);
        }
    }

    private static final long serialVersionUID = 1L;

}
