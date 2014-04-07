package org.biomart.oauth.rest;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author jhsu
 */
public class OAuthApplication extends Application {
    public OAuthApplication() {
    }

    @Override
    public Set<Object> getSingletons() {
        return new HashSet<Object>(){{
            add(new OAuthAdminResource());
        }};
    }
}
