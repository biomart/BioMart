package org.biomart.dino.cache;

import java.util.Arrays;

import com.google.common.base.Joiner;

public class GeneCallback implements CacheCallback {
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
        return tks.length > 0
                ? Joiner.on(delim).join((Arrays.copyOfRange(tks, 1, tks.length)))
                : "";
    }

}