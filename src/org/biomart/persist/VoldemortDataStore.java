package org.biomart.persist;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import voldemort.client.ClientConfig;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.versioning.Versioned;

/**
 *
 * @author jhsu
 */
public class VoldemortDataStore<K,V> implements DataStore<K,V> {
    private final StoreClient<K,V> client;

    public VoldemortDataStore() {
        this(System.getProperty("datastore.name", DATABASE_NAME));
    }

    public VoldemortDataStore(String name) {
        this(name, System.getProperty("datastore.url", "tcp://localhost:6666"));
    }

    public VoldemortDataStore(String name, String bootstrapUrl) {
        String[] urls = bootstrapUrl.split(",");
        StoreClientFactory factory = new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls(urls));
        client = factory.getStoreClient(name);
    }

    public V get(K k) {
        return client.getValue(k);
    }

    public Set<V> getAll(Iterable<K> keys) {
        Set<V> set = new HashSet<V>();
        Map<K, Versioned<V>> map = client.getAll(keys);
        for (Entry<K,Versioned<V>> entry : map.entrySet()) {
            set.add(entry.getValue().getValue());
        }
        return set;
    }

    public void put(K k, V v) {
        client.put(k, v);
    }

    public boolean delete(K k) {
        return client.delete(k);
    }
}
