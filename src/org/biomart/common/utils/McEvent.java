package org.biomart.common.utils;

public class McEvent<S> {

    private S source;
    private String property;

    public McEvent(final String property, final S source) {
    	this.property = property;
        this.source = source;
    }

    public S getSource() {
        return source;
    }

    public String getProperty() {
    	return this.property;
    }

}