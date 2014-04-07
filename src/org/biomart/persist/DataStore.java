package org.biomart.persist;

import java.util.Set;

/**
 *
 * @author jhsu
 */
public interface DataStore<K,V> {
    public static final String DATABASE_NAME = "biomart";
    public V get(K k);
    public Set<V> getAll(Iterable<K> k);
    public void put(K k, V v);
    public boolean delete(K k);
}
