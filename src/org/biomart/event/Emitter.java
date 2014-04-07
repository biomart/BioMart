package org.biomart.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu
 */
public class Emitter<T> {
    /* A soft max size on number of callbacks.
     * Too many callbacks may indicate a memory leak somewhere so it's good to
     * log it.
     */
    public static final int DEFAULT_MAX_LISTENERS = 10;
    private int maxSize = DEFAULT_MAX_LISTENERS;

    private final Map<String,List<Callback<T>>> events = new HashMap<String,List<Callback<T>>>();

    public void emit(String event, T t) {
        synchronized (events) {
            List<Callback<T>> list = events.get(event);
            if (list != null) {
                // Prevent concurrent modification on callbacks list
                Callback[] callbacks = list.toArray(new Callback[list.size()]);
                for (Callback callback : callbacks) {
                    callback.call(event, t);
                }
            }
        }
    }

    public void on(String event, Callback callback) {
        synchronized (events) {
            List<Callback<T>> list = events.get(event);
            if (list == null) {
                list = new ArrayList<Callback<T>>();
                events.put(event, list);
            }
            list.add(callback);
            if (list.size() > maxSize) {
                Log.warn("Max size reached for " + this.getClass().getSimpleName());
            }
        }
    }

    public void once(String event, final Callback callback) {
        Callback remover = new Callback() {
            @Override
            public void call(String event, Object obj) {
                synchronized (events) {
                    Emitter.this.remove(event, callback);
                }
            }
        };

        this.on(event, callback);
        this.on(event, remover);
    }

    public void remove(String event, Callback callback) {
        synchronized (events) {
            List<Callback<T>> list = events.get(event);
            if (list != null && list.contains(callback)) {
                list.remove(callback);
            }
            // Clearing out memory
            if (list.isEmpty()) {
                events.remove(event);
            }
        }
    }

    public void removeAll(String event) {
        synchronized (events) {
            List<Callback<T>> list = events.get(event);
            if (list != null) {
                list.clear();
            }
            // Clearing out memory
            events.remove(event);
        }
    }

    public void setMaxSize(int size) {
        this.maxSize = size;
    }
}
