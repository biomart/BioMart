package org.biomart.api.factory;

import com.google.inject.AbstractModule;
import java.io.File;
import org.biomart.api.rest.App;

/**
 *
 * @author jhsu
 */
public class XmlMartRegistryModule extends AbstractModule {
    private static MartRegistryFactory factory;

    static {
        String xmlPath = System.getProperty("biomart.registry.file", null);
        String keyFile = System.getProperty("biomart.registry.key.file", null);
        boolean xmlPathValid = true;

        if (xmlPath != null) {
            File f = new File(xmlPath);
            if (!f.exists()) {
                xmlPathValid = false;
                System.err.println("XML path not valid: " + xmlPath);
            }
        } else {
            xmlPathValid = false;
        }

        if (!xmlPathValid) {
            xmlPath = App.class.getClassLoader().getResource("./registry/default.xml").getFile();
        }

        factory = new XmlMartRegistryFactory(xmlPath, keyFile);
    }

    @Override
    protected void configure() {
        try {
            bind(MartRegistryFactory.class).toInstance(factory);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
