package org.biomart.oauth.rest;

import java.util.Map;
import java.util.WeakHashMap;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

/**
 *
 * @author jhsu
 */
public class OAuthSigner {
    private static OAuthSigner instance = null;

    public final Map<String,OAuthService> services;

    private OAuthSigner() {
        services = new WeakHashMap<String,OAuthService>();
    }

    public static OAuthSigner instance() {
        if (instance == null) {
            instance = new OAuthSigner();
        }
        return instance;
    }

    public OAuthRequest buildRequest(String url, String consumerKey,
            String consumerSecret, String accessKey, String accessSecret) {
        return buildRequest(Verb.GET, url, consumerKey, consumerSecret,
                accessKey, accessSecret);
    }

    public OAuthRequest buildRequest(Verb verb, String url, String consumerKey,
            String consumerSecret, String accessKey, String accessSecret) {
        OAuthRequest request = new OAuthRequest(verb, url);
        Token accessToken = new Token(accessKey, accessSecret);
        getService(consumerKey, consumerSecret).signRequest(accessToken, request);
        return request;
    }

    private OAuthService getService(String consumerKey, String consumerSecret) {
        OAuthService service = services.get(consumerKey);
        if (service == null) {
            service = new ServiceBuilder()
                    .provider(BioMartApi.class)
                    .apiKey(consumerKey)
                    .apiSecret(consumerSecret)
                    .build();
            services.put(consumerKey, service);
        }
        return service;
    }
}
