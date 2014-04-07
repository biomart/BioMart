package org.biomart.api;

import javax.servlet.http.HttpServletRequest;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;
import org.biomart.common.resources.Log;
import org.biomart.oauth.persist.Accessor;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;

/**
 *
 * @author jhsu
 */
public final class Authenticator {
    public static String getUsername(HttpServletRequest request) throws BioMartUnauthorizedException {
        String userId = null;
        String authHeader = request.getHeader("Authorization");
        String token = request.getParameter("oauth_token");

        // OAuth used?
        if (token != null || (authHeader != null && authHeader.startsWith("OAuth"))) {
            Log.info("OAuth authorization required");

            // Force HTTPS for OAuth communication?
            if (Boolean.parseBoolean(System.getProperty("oauth.forcehttps"))) {
                if (!request.isSecure()) {
                    throw new BioMartUnauthorizedException("Server requires secure connection for OAuth communication");
                }
            }

            try {
                OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
                Accessor accessor = Accessor.get(SimpleOAuthProvider.getAccessor(requestMessage));
                SimpleOAuthProvider.VALIDATOR.validateMessage(requestMessage, accessor.oauthAccessor);
                if (accessor != null) {
                    userId = (String)accessor.oauthAccessor.getProperty("user");
                    Log.info("Using user="+userId);
                    return userId != null ? userId : "";
                }
            } catch (Exception e) {
                Log.info(e.getClass().getName() + ": " + e.getMessage());
            }
            throw new BioMartUnauthorizedException("Unauthorized");
        } 

        return userId; // returns null
    }

}
