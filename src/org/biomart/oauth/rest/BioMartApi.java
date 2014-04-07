package org.biomart.oauth.rest;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.services.HMACSha1SignatureService;
import org.scribe.services.SignatureService;

/**
 *
 * @author jhsu
 */
public class BioMartApi extends DefaultApi10a {
    @Override
        public String getAccessTokenEndpoint() {
        return System.getProperty("biomart.oauth.access.url", "http://localhost:8888/oauth/access");
    }

    @Override
    public String getRequestTokenEndpoint() {
        return System.getProperty("biomart.oauth.request.url", "http://localhost:8888/oauth/token");
    }

    @Override
    public SignatureService getSignatureService() {
        return new HMACSha1SignatureService();
    }
}