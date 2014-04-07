package org.biomart.oauth.persist;

import java.util.Set;
import org.biomart.common.resources.Log;
import org.biomart.persist.VoldemortDataStore;
import org.biomart.persist.DataStore;
import org.biomart.persist.MockDataStore;

/**
 *
 * @author jhsu
 */
public class ModelStore {
    private static ModelStore instance = new ModelStore();

    private DataStore<String,Object> dataStore;

    private ModelStore() {
        if ("voldemort".equals(System.getProperty("datastore.engine"))) {
            Log.info("Using Project Voldemort backend");
            this.dataStore = new VoldemortDataStore<String,Object>();
        } else {
            Log.info("Using Mock backend");
            this.dataStore = new MockDataStore<String,Object>();
        }
    }

    public static Model get(String key) {
        return (Model)instance.dataStore.get(key);
    }

    public static Set<String> getAllKeys(String key) {
        return (Set<String>)instance.dataStore.get(key);
    }

    public static void put(String key, Object obj) {
        instance.dataStore.put(key, obj);
    }

    public static boolean delete(String key) {
        return instance.dataStore.delete(key);
    }

    public static Set getAll(Iterable<String> keys) {
        return instance.dataStore.getAll(keys);
    }
}
