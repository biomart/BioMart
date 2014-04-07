package org.biomart.persist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jhsu
 */
public class MockDataStore<K,V> implements DataStore<K,V> {
    private final Map<K,V> map;

    public MockDataStore() {
        map = new HashMap<K,V>();
    }

    public V get(K k) {
        return map.get(k);
    }

    public Set<V> getAll(Iterable<K> keys) {
        Set<V> set = new HashSet<V>();
        for (K key : keys) {
            set.add(map.get(key));
        }
        return set;
    }

    public void put(K k, V v) {
        map.put(k, v);
    }

    public boolean delete(K k) {
        return map.remove(k) != null;
    }
}
