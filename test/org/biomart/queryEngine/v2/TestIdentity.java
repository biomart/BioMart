package org.biomart.queryEngine.v2;

/**
 *
 * @author jhsu
 *
 * For testing purposes.
 *
 * Returns its input without modifications.
 */
public class TestIdentity<T> extends Job<T,T> {
    @Override
    public T apply(T input) {
        return input;
    }
}
