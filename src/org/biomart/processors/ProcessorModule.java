package org.biomart.processors;

import com.google.inject.AbstractModule;

/**
 *
 * @author jhsu
 */
public class ProcessorModule extends AbstractModule {
    @Override
    protected void configure() {
        ProcessorRegistry.install(); // Might want to put this elsewhere?
    }
}
