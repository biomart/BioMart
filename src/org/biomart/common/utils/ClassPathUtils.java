
package org.biomart.common.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu
 */
public class ClassPathUtils {
    
    private static final Class[] parameters = new Class[]{URL.class};

    /*
     * Adds a filesystem path to the classpath
     * Path must be ab absolute path (e.g. /home/user/thepath)
     */
    public static void addToClassPath(String str) throws IOException {
        URI uri = new File(str).toURI();
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{uri.toURL()});
            Log.info("Added to classpath " + uri.toString());
        } catch (Exception e) {
            throw new IOException("Error, could not add URL to system classloader", e);
        }

    }
}
