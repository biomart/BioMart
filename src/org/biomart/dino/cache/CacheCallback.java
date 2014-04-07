package org.biomart.dino.cache;

public interface CacheCallback {
    
    public CacheCallback setLine(String line);
    
    public String getKey();
    
    public String getValue();

}
