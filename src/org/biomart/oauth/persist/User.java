package org.biomart.oauth.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jhsu
 */
public class User extends Model {
    private static final String NAMESPACE = "user";
    public final String openid;
    public final List<String> consumers;

    public User(String openid) {
        super(openid);
        this.openid = openid;
        this.consumers = new ArrayList<String>();
    }

    public static User get(String openid) {
        return (User)Model.get(openid, NAMESPACE);
    }

    public static Set<User> all() {
       return Model.all(NAMESPACE);
    }

    protected String namespace() { return NAMESPACE; }
    private static final long serialVersionUID = 1L;
}
