package org.biomart.dino;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.XMLElements;
import org.biomart.dino.annotations.Func;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Element;
import org.biomart.queryEngine.QueryElement;

public class Binding {

    static final XMLElements funcKey = XMLElements.FUNCTION;

    Map<String, QueryElement> boundEls = new HashMap<String, QueryElement>();
    
    public Binding clear() {
        boundEls = new HashMap<String, QueryElement>();
        return this;
    }

    /**
     * 
     * @param fields
     *            fields bound to the query elements. From them we can retrieve
     *            the Function field values.
     * @param qel
     *            QueryElements from the query, bound to dino fields.
     */
    public Binding setBindings(List<Field> fields, List<QueryElement> queryElements) {
        
        List<Field> fieldsCp = new ArrayList<Field>(fields);
        
        Field f = null;
        for (QueryElement q : queryElements) {
            f = bindElement(q, fieldsCp);
            if (f != null) {
                // it's bound
                fieldsCp.remove(f);
            }
        }
        

        return this;
    }
    
//    public Binding setBindingsByElement(List<Field> fields, List<Element> els) {
//        
//        List<Field> fieldsCp = new ArrayList<Field>(fields);
//        Field f = null;
//        
//        for (Element e : els) {
//            f = bindElement(e, fieldsCp);
//            if (f != null) {
//                fieldsCp.remove(f);
//            }
//        }
//        
//        return this;
//    }
    
    /**
     * Bind a single Element e to a field in the fields list.
     * 
     * @param e
     * @param fields
     * @return the field that should be bound to this element, null otherwise.
     */
    private Field bindElement(QueryElement q, List<Field> fields) {
        Func a = null;
        Element e = q.getElement();
        String propVal = e.getPropertyValue(funcKey);
        
        for (Field f : fields) {
            a = f.getAnnotation(Func.class);
            if (a != null) {
                if (a.id().equalsIgnoreCase(propVal)) {
                    boundEls.put(a.id(), q);
                    return f;
                }
            } else {
                Log.error(this.getClass().getName() 
                    + "#setBingings() field "
                    + f.getName() + " has not @Func annotation");
            }
        }
        
        return null;
    }
    


    /**
     * 
     * @return a map with Key: the value of the Function field inside the
     *         configuration of the element, Value: the QueryElement that wraps
     *         this element.
     * 
     */
    public Map<String, Element> getBindings() {
        Map<String, Element> bound = new HashMap<String, Element>(boundEls.size());
        for (String k : boundEls.keySet()) {
            bound.put(k, boundEls.get(k).getElement());
        }
        return bound;
    }
    
    
    public Map<String, QueryElement> getQueryBindings() {
        return boundEls;
    }
    
    
    
    /**
     * 
     * 
     * @param b
     *            Instance on which set field values.
     * @param fields
     *            builder's class' fields annotated with the Func annotation.
     * @param qes
     *            elements coming from the query.
     * 
     * @return The QueryElements that have been bound.
     * 
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws ValidationException
     *             if there's any mandatory function parameter missing.
     */
    public static List<QueryElement> 
    setFieldValues(Object b, List<Field> fields, List<QueryElement> qes) 
                           throws IllegalArgumentException,
                                  IllegalAccessException {
        
        XMLElements key = XMLElements.FUNCTION;
        String propVal = null;
        Element e = null;
        Func a = null;
        ArrayList<Field> fieldsCp = null, currFields = new ArrayList<Field>(fields);
        ArrayList<QueryElement> boundEls = new ArrayList<QueryElement>(qes.size());

        for (QueryElement q : qes) {
            e = q.getElement();
            // Get its function name
            propVal = e.getPropertyValue(key);

            fieldsCp = new ArrayList<Field>(currFields);
            for (Field f : fieldsCp) {
                a = f.getAnnotation(Func.class);
                if (a.id().equalsIgnoreCase(propVal)) {
                    f.setAccessible(true);
                    f.set(b, getElementValue(q));
                    f.setAccessible(false);
                    currFields.remove(f);
                    boundEls.add(q);
                }
            }

            if (currFields.size() == 0)
                break;
        }

        return boundEls;
    }
    
    
    public void checkBinding(List<Field> fields) {
        for (Field f : fields) {
            Func ann = f.getAnnotation(Func.class); 
            if (!ann.optional() 
                && this.boundEls.get(ann.id()) == null) {
                throw new ValidationException("Function parameter `"
                        + f.getAnnotation(Func.class).id() + "` missing");
            }
        }
    }
    
    /**
     * Explores klass' fields and returns the ones annotated with the Func
     * annotation.
     * 
     * @param klass
     * @return a list of fields annotated with the Func annotation.
     */
    public static List<Field> getAnnotatedFields(Class<?> klass) {
        // All fields, included the interfaces' ones
        Field[] fds = klass.getDeclaredFields();
        List<Field> fields = new ArrayList<Field>(fds.length);

        for (Field f : fds) {
            if (f.isAnnotationPresent(Func.class)
                    && f.getType() == String.class) {

                fields.add(f);

            }
        }

        return fields;
    }
    
    
    private static String getElementValue(QueryElement qe) {
        String value = "";

        switch (qe.getType()) {
            case ATTRIBUTE:
                Attribute a = (Attribute) qe.getElement();
                value = a.getName();
                break;
            case FILTER:
                value = qe.getFilterValues();
                break;
            default:
                break;
        }

        return value;
    }

}

























