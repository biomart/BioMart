/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.biomart.api.rdf;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author jbaran
 */
public class Ontology {
    private Map<String, String> namespaces = new HashMap<String, String>(); // Prefix -> Namespace URI
    private Map<String, RDFClass> classes = new HashMap<String, RDFClass>(); // Class name -> Class
    private Map<String, String> superclasses = new HashMap<String, String>(); // Class name -> Super Class name

    public static String sanitizeURI(String uri) {
        if (uri == null)
            return null;
        
        if (uri.matches("^&\\w+;\\w+$"))
            return uri.substring(1).replaceFirst(";", ":");

        return uri;
    }

    public void addNamespace(String prefix, String namespace) {
        namespaces.put(prefix, namespace);
    }
    
    public Map<String, String> getNamespaces() {
        return namespaces; // TODO Make this an immutable map...
    }

    public void addClass(String className, String parentClass, String[] uriAttributes) {
        className = abbreviateURI(sanitizeURI(className));

        classes.put(className, new RDFClass(className, uriAttributes, namespaces));

        if (parentClass != null)
            superclasses.put(className, sanitizeURI(parentClass));
    }

    public void addProperty(String attributeName, String filterName, String className, String property, String range) {
        className = abbreviateURI(className);
        property = abbreviateURI(property);
        range = abbreviateURI(range);

        RDFClass rdfClass = classes.get(className);

        if (rdfClass == null)
            return; // TODO: Exception? Config is missing class definition...

        setNamespace(rdfClass);

        RDFProperty rdfProperty = new RDFProperty(attributeName, filterName, property, range, namespaces);

        setNamespace(rdfProperty);
        rdfClass.addProperty(rdfProperty);
        classes.put(className, rdfClass);
    }

    public Collection<RDFClass> getRDFClasses() { return Collections.unmodifiableCollection(classes.values()); }

    public void freeze() {
        if (superclasses == null)
            throw new IllegalStateException("Ontology can only be frozen once.");

        // Ensure inheritance is reflected correctly.
        for (String child : superclasses.keySet()) {
            RDFClass parent = classes.get(abbreviateURI(superclasses.get(child)));

            // Might be null if it refers to some RDF/RDFS/OWL class (like, owl:Thing).
            if (parent != null)
                classes.get(child).setParent(parent);
        }
    }

    private void setNamespace(RDFObject ro) {
        for (String prefix : namespaces.keySet())
            if (ro.getName().startsWith(prefix))
                ro.setNamespace(prefix, namespaces.get(prefix));
    }

    public String abbreviateURI(String uri) {
        if (uri.matches("^\\w+://.+#\\w+"))
            for (Entry<String, String> namespace : namespaces.entrySet())
                if (uri.startsWith(namespace.getValue()))
                    return uri.replaceFirst(namespace.getValue(), namespace.getKey());
        return uri;
    }
}