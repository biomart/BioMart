package org.biomart.oauth.persist;

import java.util.Set;
import net.oauth.OAuthConsumer;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * @author jhsu
 */
public class Consumer extends Model {
    private static final String NAMESPACE = "consumer";
    @JsonIgnore public final OAuthConsumer oauthConsumer;

    // JSON attributes
    @JsonProperty("description") String getDescription() {
        return (String)oauthConsumer.getProperty("description");
    }
    @JsonProperty("name") String getName() {
        return (String)oauthConsumer.getProperty("name");
    }
    @JsonProperty("key") public String getKey() {
        return (String)oauthConsumer.consumerKey;
    }
    @JsonProperty("secret") public String getSecret() {
        return (String)oauthConsumer.consumerSecret;
    }
    @JsonProperty("callback") String getCallbackURL() {
        return "".equals(oauthConsumer.callbackURL) ? "none" : (String)oauthConsumer.callbackURL;
    }

    public Consumer(OAuthConsumer c) {
        super(c.consumerKey);
        this.oauthConsumer = c;
    }

    public static Consumer get(String consumerKey) {
        return (Consumer)Model.get(consumerKey, NAMESPACE);
    }

    public static Set<Consumer> all() {
       return Model.all(NAMESPACE);
    }

    @Override
    public boolean delete() {
        Set<Accessor> accessors = Accessor.all();
        for (Accessor accessor : accessors) {
            if (accessor.oauthAccessor.consumer.consumerKey
                    .equals(oauthConsumer.consumerKey)) {
                accessor.delete();
            }
        }
        return super.delete();
    }

    protected String namespace() { return NAMESPACE; }
    private static final long serialVersionUID = 1L;
}
