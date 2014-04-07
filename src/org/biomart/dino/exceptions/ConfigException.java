package org.biomart.dino.exceptions;

public class ConfigException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 8973329725426726140L;
    
    public ConfigException(String message) {
        super(message);
    }
    
    /**
     * 
     * @param opt option searched
     * @param config string representation of the configuration.
     */
    public ConfigException(String opt, String config) {
        super(opt + " has not beend found within configuration "+ config);
    }
}
