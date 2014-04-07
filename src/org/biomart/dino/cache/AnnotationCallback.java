package org.biomart.dino.cache;

import com.google.common.base.Joiner;

public class AnnotationCallback implements CacheCallback {
    protected String delim = "\t";
    protected String[] tks = null;
    
    @Override
    public CacheCallback setLine(String line) {
        tks = line.split(delim);
        return this;
    }

    @Override
    public String getKey() {
        return tks.length > 0 ? tks[0] : "";
    }

    @Override
    public String getValue() {
        return tks.length > 0 ? Joiner.on(delim).join(tks) : "";
    }

}
