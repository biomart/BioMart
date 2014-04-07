package org.biomart.dino;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.biomart.api.factory.XmlMartRegistryModule;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.dino.annotations.Func;
import org.biomart.dino.dinos.Dino;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Element;
import org.biomart.queryEngine.Query;
import org.biomart.queryEngine.QueryElement;
import org.biomart.common.utils.XMLElements;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * 
 * @author Luca Pandini (lpand)
 * 
 *         gets hold of the Dino controller class given the dino field; it
 *         creates a new instance of the controller and hands it off to the
 *         Binder.
 * 
 *         The Binder will gather the Dino controller requirements and provide
 *         them to it. Eventually, it'll invoke the controller's create method.
 * 
 */
public class DinoHandler {

    // This is to make test easier (we must cope with all code as always...)
    static boolean initialize = false;
    static Injector inj;

    private DinoHandler() {
    }

    private static void init() {
        if (!initialize) {
            inj = Guice.createInjector(new DinoModule(),
                    new XmlMartRegistryModule());
        }
    }

    public static void runDino(Query q, String user, String[] mimes,
            OutputStream o) throws IOException {
        Log.debug("DinoHandler#runDino() invoked");

        init();

        Class<? extends Dino> dinoClass;
        Dino dino;
        String dinoName = q.getDino();

        try {

            // Get the class
            dinoClass = getDinoClass(dinoName);
            // Get the fields to bing to
//            List<Field> fields = getAnnotatedFields(dinoClass);
//            Log.debug("DinoHandler::runDino() number of annotated fields from class "+dinoClass.getName()+" is "+fields.size());
            Log.debug("DinoHandler::runDino() number query elements is "+ q.getQueryElementList().size());
            // Create an Dino instance
            dino = inj.getInstance(dinoClass);

            // Set the field values
//            List<QueryElement> boundEls = 
//                    setFieldValues(dino, fields, q.getQueryElementList());
//            Binding md = new Binding()
//                .setBindings(fields, boundEls);
            
            dino.setQuery(q)
                .setMimes(mimes)
                .setMetaData(new Binding())
                .run(o);

        } catch (Exception e) {
            Log.error("DinoHandler#runDino ", e);
            o.write(e.getMessage().getBytes());
            o.close();
            throw new IOException(e);
        }
    }

    public static Class<? extends Dino> getDinoClass(String dinoClassName)
            throws ClassNotFoundException {
        Log.debug("DinoHandler#getDinoClass() invoked");

        Class<? extends Dino> dinoClass;
        try {
            dinoClass = Class.forName(dinoClassName).asSubclass(Dino.class);
        } catch (RuntimeException re) {
            Log.error("DinoHandler#getDinoClass() Dino name `" + dinoClassName
                    + "` doesn't correspond to any class");

            // The stream should be closed by the caller.
            throw re;
        }

        return dinoClass;
    }

    public static Dino getDinoInstance(Class<? extends Dino> klass)
            throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Log.debug("DinoHandler#getDinoInstance() invoked");

        Constructor<?>[] ctors = klass.getConstructors();
        Constructor<?> ctor = null;
        for (int i = 0; i < ctors.length; ++i) {
            ctor = ctors[i];
            if (ctor.getGenericParameterTypes().length == 0) {
                break;
            }
        }

        ctor.setAccessible(true);
        Dino dino = Dino.class.cast(ctor.newInstance());

        return dino;
    }
}
