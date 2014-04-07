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

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;
import net.oauth.server.OAuthServlet;
import org.biomart.common.resources.Log;

/**
 * Authorization request handler.
 *
 * @author Praveen Alavilli, Jack Hsu
 */
public class AuthorizationServlet extends HttpServlet {
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // nothing at this point
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        
        try{
            OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
            OAuthAccessor accessor = SimpleOAuthProvider.getAccessor(requestMessage);
            sendToAuthorizePage(request, response, accessor);
        
        } catch (Exception e){
            Log.info(e.getClass().getName() + ": " + e.getMessage());
            SimpleOAuthProvider.handleException(e, request, response, true);
        }
    }
    
    @Override 
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException{
        
        try{
            OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
            
            OAuthAccessor accessor = SimpleOAuthProvider.getAccessor(requestMessage);
            
            String userId = request.getParameter("userId");
            if(userId == null){
                sendToAuthorizePage(request, response, accessor);
            } else {
                // set userId in accessor and mark it as authorized
                SimpleOAuthProvider.markAsAuthorized(accessor, userId);
                request.getSession().setAttribute("oauth_accessor", this);

                returnToConsumer(request, response, accessor);
            }
        } catch (Exception e){
            Log.info(e.getClass().getName() + ": " + e.getMessage());
            SimpleOAuthProvider.handleException(e, request, response, true);
        }
    }
    
    private void sendToAuthorizePage(HttpServletRequest request, 
            HttpServletResponse response, OAuthAccessor accessor)
    throws IOException, ServletException{
        String consumer_name = (String)accessor.consumer.getProperty("name");
        request.setAttribute("CONS_NAME", consumer_name);
        request.setAttribute("TOKEN", accessor.requestToken);
        request.getRequestDispatcher("/oauth/authorize.jsp").forward(request, response);
        
    }
    
    private void returnToConsumer(HttpServletRequest request, 
            HttpServletResponse response, OAuthAccessor accessor)
    throws IOException, ServletException{
        OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
        String callback = (String)accessor.getProperty(OAuth.OAUTH_CALLBACK);
        String verifier = SimpleOAuthProvider.VALIDATOR.generateVerifier(accessor, requestMessage);
        if(callback == null || "".equals(callback)
            && accessor.consumer.callbackURL != null 
                && accessor.consumer.callbackURL.length() > 0){
            // first check if we have something in our properties file
            callback = accessor.consumer.callbackURL;
        }

        if("oob".equals(callback)) {
            // no call back it must be a client
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("You have successfully authorized '" 
                    + accessor.consumer.getProperty("description") 
                    + "'. You need to enter the verifier PIN code "
                    + verifier
                    + " in the client.");
            out.close();
        } else {
            // if callback is not passed in, use the callback from config
            if(callback == null || callback.length() <=0 )
                callback = accessor.consumer.callbackURL;
            String token = accessor.requestToken;
            if (token != null) {
                callback = OAuth.addParameters(callback, new ImmutableMap.Builder<String,String>()
                   .put(OAuth.OAUTH_TOKEN, token)
                   .put(OAuth.OAUTH_VERIFIER, verifier)
                   .build().entrySet());
            }
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", callback);
        }
    }

    private static final long serialVersionUID = 1L;

}
