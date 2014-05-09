package org.biomart.dino;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.aliasi.lm.LanguageModel;
import org.apache.commons.lang.StringUtils;
import org.biomart.common.resources.Log;
import org.biomart.processors.ProcessorImpl;
import org.biomart.processors.ProcessorRegistry;
import org.biomart.queryEngine.Query;

import com.google.common.base.Function;


public class Processor {

    ProcessorImpl proc;

    public Processor(String type) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        proc = initProcessor(type);
    }

    // We must use ProcessorImpl instead of ProcessorImpl because they have
    // getClassback method with two different signatures, and most of processors
    // extends ProcessorImpl. The greatness and mysteries of Biomart code...
    private ProcessorImpl
    initProcessor(String type) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ProcessorImpl proc = getProcessorInstance(getProcessorClass(type));

        String value = "";
        for (String field : proc.getFieldNames()) {
            value = proc.getDefaultValueForField(field);
            if (value != null)
                proc.setFieldValue(field, value);
        }

        return proc;
    }

    public String getContentType() {
        return proc.getContentType();
    }

    public void send(List<String[]> data) {
        Function<String[], Boolean> fn = proc.getCallback();
        for (String[] line : data) {
            fn.apply(line);
        }

        proc.done();
    }

    public void setQuery(Query q) {
        proc.setQuery(q);
    }

    public void setOutput(OutputStream out) {
        proc.setOutputStream(out);
    }

    private Class<? extends ProcessorImpl>
    getProcessorClass(String type) {
        return ProcessorRegistry.get(type);
    }

    private ProcessorImpl getProcessorInstance(Class<? extends ProcessorImpl> klass)
            throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Constructor<?>[] ctors = klass.getConstructors();
        Constructor<?> ctor = null;
        for (int i = 0; i < ctors.length; ++i) {
            ctor = ctors[i];
            if (ctor.getGenericParameterTypes().length == 0) {
                break;
            }
        }

        ctor.setAccessible(true);
        ProcessorImpl proc = ProcessorImpl.class.cast(ctor.newInstance());

        return proc;
    }













    // We must use ProcessorImpl instead of ProcessorImpl because they have 
    // getClassback method with two different signatures, and most of processors
    // extends ProcessorImpl. The greatness and mysteries of Biomart code...
    private static ProcessorImpl 
    initProc(String type, Query q, OutputStream out) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ProcessorImpl proc = getProcInstance(getProcClass(type));
        
        String value = "";
        for (String field : proc.getFieldNames()) {
            value = proc.getDefaultValueForField(field);
            if (value != null)
                proc.setFieldValue(field, value);
        }
        
        proc.setQuery(q);
        proc.setOutputStream(out);
        return proc;
    }
    
    public static void 
    runProcessor(List<String[]> data, String type, Query q, OutputStream out) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        
        ProcessorImpl proc = initProc(type, q, out);
        Function<String[], Boolean> fn = proc.getCallback();
        
        for (String[] line : data) {
//            Log.debug("Processor: writing line: "+ StringUtils.join(line, " "));
            fn.apply(line);
        }
        
        proc.done();
    }
    
    private static Class<? extends ProcessorImpl> 
    getProcClass(String type) {
        return ProcessorRegistry.get(type);
    }
    
    private static 
    ProcessorImpl getProcInstance(Class<? extends ProcessorImpl> klass)
            throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Constructor<?>[] ctors = klass.getConstructors();
        Constructor<?> ctor = null;
        for (int i = 0; i < ctors.length; ++i) {
            ctor = ctors[i];
            if (ctor.getGenericParameterTypes().length == 0) {
                break;
            }
        }

        ctor.setAccessible(true);
        ProcessorImpl proc = ProcessorImpl.class.cast(ctor.newInstance());

        return proc;
    }

}
