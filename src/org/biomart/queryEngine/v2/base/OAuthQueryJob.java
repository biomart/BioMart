package org.biomart.queryEngine.v2.base;

import java.io.InputStream;
import org.biomart.oauth.rest.OAuthSigner;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;

/**
 *
 * @author jhsu
 *
 * Extends WebQueryJob with OAuth support for authenticated queries.
 *
 * InputStream comes from OAuthRequest.getStream().
 */
public class OAuthQueryJob extends WebQueryJob {
    private final String consumerKey;
    private final String consumerSecret;
    private final String tokenKey;
    private final String tokenSecret;

    public OAuthQueryJob(String url, String consumerKey, String consumerSecret,
            String tokenKey, String tokenSecret) {
        super(url);
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.tokenKey = tokenKey;
        this.tokenSecret = tokenSecret;
    }

    @Override
    public InputStream getInputStream(String queryXml) {
        OAuthRequest req = OAuthSigner.instance().buildRequest(Verb.POST,
                super.url, consumerKey, consumerSecret, tokenKey, tokenSecret);

        req.addBodyParameter("query", queryXml);

        return req.send().getStream();
    }
}
