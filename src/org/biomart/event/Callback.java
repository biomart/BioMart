package org.biomart.event;

/**
 *
 * @author jhsu
 */
public abstract class Callback<T> {
    public abstract void call(String event, T t);
}
