package org.biomart.oauth.persist;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jhsu
 */
public abstract class Model implements Serializable {
    private static final String PREFIX = "model";
    public final String pk;

    public Model(String key) {
        pk = PREFIX + "-" + namespace() + "-" + key;
    }
    public static Model get(String key, String namespace) {
        return ModelStore.get(PREFIX + "-" + namespace + "-" + key);
    }
    public static Set<String> allKeys(String namespace) {
        Set<String> set = ModelStore.getAllKeys(PREFIX + "-" + namespace + "__all");
        if (set == null) {
            return new HashSet<String>();
        }
        return set;
    }
    public static Set all(String namespace) {
        Set<String> keys = Model.allKeys(namespace);
        return ModelStore.getAll(keys);
    }
    public void save() {
        Set modelsKeys = Model.allKeys(namespace());
        modelsKeys.add(pk);
        ModelStore.put(PREFIX + "-" + namespace() + "__all", modelsKeys);
        ModelStore.put(pk, this);
    }
    public boolean delete() {
        Set modelsKeys = Model.allKeys(namespace());
        modelsKeys.remove(pk);
        ModelStore.put(PREFIX + "-" + namespace() + "__all", modelsKeys);
        return ModelStore.delete(pk);
    }
    protected abstract String namespace();

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (!(that instanceof Model)) return false;
        return this.pk.equals(((Model)that).pk);
    }

    @Override
    public int hashCode() {
        return this.pk.hashCode();
    }
}
