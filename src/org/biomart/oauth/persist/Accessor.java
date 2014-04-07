package org.biomart.oauth.persist;

import java.util.Set;
import net.oauth.OAuthAccessor;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;

/**
 *
 * @author jhsu
 */
public class Accessor extends Model {
    private static final String NAMESPACE = "accessor";
    public final String openId;
    public final OAuthAccessor oauthAccessor;

    public Accessor(String openId, OAuthAccessor oauthAccessor) {
        super(openId + "-" + oauthAccessor.consumer.consumerKey);
        this.openId = openId;
        this.oauthAccessor = oauthAccessor;
    }


    public static Accessor get(OAuthAccessor a) {
        return (Accessor)Model.get(a.getProperty("user") + "-" + a.consumer.consumerKey, NAMESPACE);
    }

    public static Accessor get(String openid, String consumerKey) {
        return (Accessor)Model.get(openid + "-" + consumerKey, NAMESPACE);
    }

    public static Set<Accessor> all() {
       return Model.all(NAMESPACE);
    }

    @Override
    public boolean delete() {
        SimpleOAuthProvider.removeAuthorization(oauthAccessor);
        return super.delete();
    }

    protected String namespace() { return NAMESPACE; }
    private static final long serialVersionUID = 1L;
}

