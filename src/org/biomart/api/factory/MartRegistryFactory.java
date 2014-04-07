package org.biomart.api.factory;

import org.biomart.api.lite.MartRegistry;


/**
 *
 * @author jhsu
 */
public interface MartRegistryFactory {
    public MartRegistry getRegistry(String username);
}
