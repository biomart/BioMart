package org.biomart.queryEngine.v2.base;

/**
 *
 * @author jhsu
 */
public class Filter {
    public final String name;
    public final String value;
    public Filter(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
