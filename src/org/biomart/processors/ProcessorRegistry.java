package org.biomart.processors;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.ClassPathUtils;

/**
 *
 * @author jhsu
 *
 * Processors must register itself to this class in order to be available
 * through MartConfigurator and Mart API.
 */
public class ProcessorRegistry {
    private static Map<String,Class> lookup = new HashMap<String,Class>();

    public static void register(String name, Class clazz) {
        if (ProcessorInterface.class.isAssignableFrom(clazz)) {
            lookup.put(name.toUpperCase(), clazz);
            Log.info("Registered processor " + name);
        } else {
            Log.error("Registered classes must implement ProcessorInferface");
        }
    }

    public static Map<String,Class> getAll() {
        return ImmutableMap.copyOf(lookup);
    }

    public static Class get(String name) {
        return lookup.get(name.toUpperCase());
    }

    private static boolean isInstalled = false;

    public static void install() {
        if (isInstalled) {
            return;
        }

        isInstalled = true;

        register("TSV", TSV.class);
        register("TSVX", TSVX.class);
        register("RDFN3", RDFN3.class);
        register("SPARQLXML", SPARQLXML.class);
        register("CSV", CSV.class);
        register("JSON", JSON.class);
        register("NETWORK", Network.class);
        register("ENRICHMENT", Enrichment.class);

        final FileFilter directoryFilter = new FileFilter() {
            @Override public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        final FileFilter jarFilter = new FileFilter() {
            @Override public boolean accept(File file) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".jar") || file.getName().endsWith(".JAR"))
                        return true;
                }
                return false;
            }
        };

        // Traverse plugins directory
        String homeDir = System.getProperty("biomart.basedir", ".");
        File pluginsDir = new File (homeDir + "/plugins");
        if (pluginsDir.exists()) {
            for (File file : pluginsDir.listFiles(directoryFilter)) {
                File processorProperties = new File(file, "processor.properties");
                File libDir = new File(file, "lib");
                // Add all plugins that contain public directory to a new martappsCxt
                // Pick up any JAR files in the lib directory
                if (libDir.exists()) {
                    for (File jar : libDir.listFiles(jarFilter)) {
                        try {
                            Log.info("Adding to classpath: " + jar.getCanonicalPath());
                            ClassPathUtils.addToClassPath(jar.getCanonicalPath());
                        } catch(IOException e) {
                            Log.error("Error trying to add classpath", e);
                        }
                    }
                }
                if (processorProperties.exists()) {
                    Properties props = new Properties();

                    try {
                        props.load(new FileInputStream(processorProperties));
                        for (String name : props.stringPropertyNames()) {
                            String className = props.getProperty(name);
                            Class clazz = Class.forName(className);
                            ProcessorRegistry.register(name, clazz);
                        }
                    } catch(Exception e) {
                        Log.info("Error installing plugin", e);
                    }
                }
            }
        }
    }
}
