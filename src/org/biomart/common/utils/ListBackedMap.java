/*
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.biomart.common.utils;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles list backed maps.
 */
public class ListBackedMap<K,V> extends AbstractMap<K,V> implements Serializable {
	private static final long serialVersionUID = 1L;

	private final List<K> keys = new ArrayList<K>();

	private final List<V> values = new ArrayList<V>();

	/**
	 * Construct a new list backed map. Keys will be returned in the order they
	 * were added.
	 */
	public ListBackedMap() {
		super();
	}

	/**
	 * Construct a new list backed map. Keys will be returned in the order they
	 * were added.
	 * 
	 * @param map
	 *            a set of initial entries, to be added in the order returned by
	 *            the {@link Map#entrySet()} iterator.
	 */
	public ListBackedMap(final Map<K,V> map) {
		super();
		this.putAll(map);
	}

	public Set<Map.Entry<K,V>> entrySet() {
		return new AbstractSet<Map.Entry<K,V>>() {
			public Iterator<Map.Entry<K,V>> iterator() {
				return new Iterator<Map.Entry<K,V>>() {
					private int i = -1;

					public boolean hasNext() {
						return this.i < size() - 1;
					}

					public Map.Entry<K,V> next() {
						++i;
						return new Map.Entry<K,V>() {
							public K getKey() {
								return ListBackedMap.this.keys.get(i);
							}

							public V getValue() {
								return ListBackedMap.this.values.get(i);
							}

							public V setValue(final V value) {
								return ListBackedMap.this.values.set(i, value);
							}
						};
					}

					public void remove() {
						ListBackedMap.this.keys.remove(this.i);
						ListBackedMap.this.values.remove(this.i);
					}
				};
			}

			public int size() {
				return ListBackedMap.this.keys.size();
			}
		};
	}

	public V put(final K key, final V value) {
		if (this.keys.contains(key))
			// Replace.
			return this.values.set(this.keys.indexOf(key), value);
		else {
			// Append.
			this.keys.add(key);
			this.values.add(value);
			return value;
		}
	}

	/**
	 * Inserts the key after the previousKey, or at the beginning of the map if
	 * previousKey is null. Otherwise, see {@link #put(Object, Object)}.
	 * 
	 * @param previousKey
	 *            the previousKey to insert after.
	 * @param key
	 *            the key to insert.
	 * @param value
	 *            the value to insert.
	 * @return the previous value for that key, if any.
	 */
	public V put(final K previousKey, final K key,
			final V value) {
		// Remove existing if exists.
		V returnObj = null;
		final int existingIndex = this.keys.indexOf(key);
		if (existingIndex >= 0) {
			this.keys.remove(existingIndex);
			returnObj = this.values.remove(existingIndex);
		}
		if (previousKey == null) {
			// Prepend.
			this.keys.add(0, key);
			this.values.add(0, value);
		} else {
			// Insert.
			final int index = this.keys.indexOf(previousKey) + 1;
			this.keys.add(index, key);
			this.values.add(index, value);
		}
		return returnObj;
	}
}
