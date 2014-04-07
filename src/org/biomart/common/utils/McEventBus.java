package org.biomart.common.utils;

import java.lang.annotation.Annotation;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;

import org.jfree.util.Log;

public class McEventBus {

    private static McEventBus INSTANCE = null;

    private Map<String, Set<WeakReference<Object>>> listeners = new HashMap<String, Set<WeakReference<Object>>>();

    static public McEventBus getInstance() {
        if( INSTANCE == null ) {
            INSTANCE = new McEventBus();
        }
        return INSTANCE;
    }

    private McEventBus() {
    }



    public void addListener(final String property, final Object listener) {

        // Get or create the Set of listeners for this type
        Set<WeakReference<Object>> typeListeners = listeners.get(property);
        if (typeListeners == null) {
            typeListeners = new HashSet<WeakReference<Object>>();
            listeners.put(property, typeListeners);
        }

        // Add the listener
        //avoid duplicated objects
       for(WeakReference<Object> wf: typeListeners) {
    	   if(listener.equals(wf.get())) {
    		   return;
    	   }
       }
       typeListeners.add(new WeakReference<Object>(listener));

    }

    public void removeListener(final String property, final Object listener) {
    	 Set<WeakReference<Object>> typeListeners = listeners.get(property);
    	 if(typeListeners!=null) {
    		 for(Iterator<WeakReference<Object>> it=typeListeners.iterator();it.hasNext(); ) {
    			 WeakReference<Object> wf = it.next();
    			 if(listener.equals(wf.get())) {
		    		 it.remove();
		    		 if(typeListeners.isEmpty()) {
		    			 listeners.remove(property);
		    		 }
    			 }
    		 }
    	 }
    	 
    }
    
    
    public void fire(final String property, Object source) {
    	McEvent<Object> event = new McEvent<Object>(property,source);
    	Set<WeakReference<Object>> propertyListeners = listeners.get(event.getProperty());
    	if(propertyListeners == null)
    		return;
    	for(Iterator<WeakReference<Object>> it = propertyListeners.iterator(); it.hasNext();) {
    		WeakReference<Object> l = it.next();
    		Object object = l.get();
    		if(object == null) {
    			it.remove();
    		}else {
    			this.invodeDeclaredMethod(object, event);
    		}
    	}
    	if(propertyListeners.isEmpty())
    		listeners.remove(event.getProperty());

    }
    
    private void invodeDeclaredMethod(Object object, final McEvent<?> event) {
    	try {
            Class<?> c = object.getClass();
            Method m[] = c.getDeclaredMethods();
            for (Method method: m) {
            	Annotation eventListener = method.getAnnotation(McEventListener.class);
            	if(eventListener!=null) {
            		//call the method
            		Object[] args= new Object[]{event};
            		method.invoke(object, args);
            		break;
            	}
            }          
         }
         catch (Throwable e) {
            Log.error(e);
         }

    }

}