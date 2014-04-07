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
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;
import net.oauth.server.OAuthServlet;
import org.biomart.common.resources.Log;
import org.biomart.oauth.persist.Accessor;
import org.biomart.oauth.persist.User;

/**
 * Access Token request handler
 *
 * @author Praveen Alavilli, Jack Hsu
 */
public class AccessTokenServlet extends HttpServlet {
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Reload all authorized OAuth tokens
        for (User u : User.all()) {
            for (String consumerKey : u.consumers) {
                try {
                    Accessor accessorModel = Accessor.get(u.openid, consumerKey);
                    if (accessorModel != null) {
                        Log.debug("[OAuth] Authorizing access token (OpenID=" + u.openid + ", consumerKey=" + consumerKey +")");
                        OAuthAccessor accessor = accessorModel.oauthAccessor;
                        SimpleOAuthProvider.markAsAuthorized(accessor, u.openid);
                    }
                } catch (OAuthException e) {
                    Log.error(e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        processRequest(request, response);
    }
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        processRequest(request, response);
    }
        
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try{
            HttpSession session = request.getSession(false);
            if (session != null) {
                String id = (String)session.getAttribute("openid_identifier");
                OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);

                OAuthAccessor accessor = SimpleOAuthProvider.getAccessor(requestMessage);
                SimpleOAuthProvider.VALIDATOR.validateMessage(requestMessage, accessor);
                SimpleOAuthProvider.VALIDATOR.validateVerifier(requestMessage);

                // Delete existing token
                Accessor existing = Accessor.get(id, accessor.consumer.consumerKey);
                Log.debug("Deleting existing OAuth Accessor");
                if (existing != null) {
                    existing.delete();
                }

                if (id == null) {
                    id = (String)accessor.getProperty("user");
                }

                // make sure token is authorized
                if (id == null || !(Boolean)accessor.getProperty("authorized")) {
                    OAuthProblemException problem = new OAuthProblemException("permission_denied");
                    throw problem;
                }
                // generate access token and secret
                SimpleOAuthProvider.generateAccessToken(accessor);

                // Store tokens and user info in data store
                User u = User.get(id);
                if (u == null) {
                    u = new User(id);
                }
                if (!u.consumers.contains(accessor.consumer.consumerKey)) {
                    u.consumers.add(accessor.consumer.consumerKey);
                }
                u.save();

                Accessor a = new Accessor(id, accessor);
                a.save();

                response.setContentType("text/plain");
                OutputStream out = response.getOutputStream();
                OAuth.formEncode(OAuth.newList("oauth_token", accessor.accessToken,
                                               "oauth_token_secret", accessor.tokenSecret),
                                 out);
                out.close();
            } else {
                OAuthProblemException problem = new OAuthProblemException("permission_denied");
                throw problem;
            }
        } catch (Exception e){
            Log.info(e.getClass().getName() + ": " + e.getMessage());
            SimpleOAuthProvider.handleException(e, request, response, true);
        }
    }

    private static final long serialVersionUID = 1L;

}
