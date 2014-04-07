package org.biomart.api.factory;

import com.google.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.biomart.api.BioMartApiException;
import org.biomart.configurator.test.MartConfigurator;
import org.biomart.objects.objects.MartRegistry;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author jhsu
 */
@Singleton
public class XmlMartRegistryFactory implements MartRegistryFactory {
    private final MartRegistry _registry;

    public XmlMartRegistryFactory(@Nonnull String xmlFilePath, @Nullable String keyFilePath) {
        MartConfigurator.initForWeb();
        try {
            SAXBuilder parser = new SAXBuilder();
            FileInputStream in = new FileInputStream(new File(xmlFilePath));
            Document doc = parser.build(in);
            in.close();
            MartConfigurator.initForWeb();
            if (keyFilePath == null) keyFilePath = "";
            _registry = new MartRegistry(doc, keyFilePath);
        } catch (Exception e) {
            throw new BioMartApiException(e);
        }
    }

    @Override
    public org.biomart.api.lite.MartRegistry getRegistry(String username) {
        return new org.biomart.api.lite.MartRegistry(_registry, username, null);
    }
}
