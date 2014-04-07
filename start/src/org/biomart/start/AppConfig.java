package org.biomart.start;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.mortbay.jetty.security.Password;

/**
 *
 * @author jhsu
 */
public class AppConfig {
    public static void loadProperties() {
        String propsFilePath = System.getProperty("biomart.properties");
        System.out.println("biomart.properties="+propsFilePath);

        Properties props = new Properties();

        if (propsFilePath != null) {
            try {
                props.load(new FileInputStream(propsFilePath));

                for (String key : props.stringPropertyNames()) {
                    if (System.getProperty(key) == null) {
                        String value = props.getProperty(key);
                        if (key.endsWith(".file") || key.endsWith((".dir"))) {
                            File file = new File(value);
                            value = file.getCanonicalPath();
                        }
                        if (value.startsWith("OBF:")) {
                            value = Password.deobfuscate(value);
                        } else {
                        }
                        System.setProperty(key, value);
                    } else {
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading properties file: " + propsFilePath);
            }
        }

        if (System.getProperty("biomart.debug") == null) {
            System.setProperty("biomart.debug", "true");
        }
    }
}
