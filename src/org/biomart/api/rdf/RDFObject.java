/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.biomart.api.rdf;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jbaran
 */
public class RDFObject {
    private final static Pattern NAME_PATTERN = Pattern.compile("\\W(\\w+)$");

    private final String name;
    private String prefix;

    // Namespace of this particular object:
    private String namespace;
    
    // All accummulated namesspaces of the ontology:
    private Map<String, String> namespaces;

    public RDFObject(String name, Map<String, String> namespaces) {
        this.name = name;
        this.namespaces = namespaces;
    }

    public void setNamespace(String prefix, String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
    }
    
    public String getPrefix() { return prefix; }

    public static String getPrefix(String s) {
        return s.substring(0, s.length() - getShortName(s).length());
    }

    public String getNamespace() { return namespace; }

    public String getName() { return name; }

    public String getFullName() { return expandPrefix(name); }

    public String getShortName() {
        return getShortName(name);
    }

    public static String getShortName(String s) {
        Matcher nameMatcher = NAME_PATTERN.matcher(s);

        if (!nameMatcher.find())
            return s;

        if (nameMatcher.groupCount() < 1)
            return s;

        return nameMatcher.group(1);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (!(o instanceof RDFObject)) return false;

        if (!this.name.equals(((RDFObject)o).name)) return false;

        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

    protected String expandPrefix(String s) {
        String sPrefix = getPrefix(s);

        for (String namespacePrefix : namespaces.keySet()) {
            String expandedPrefix = namespaces.get(namespacePrefix);

            if (sPrefix.equals(namespacePrefix))
                return s.replaceFirst(sPrefix, expandedPrefix);
        }

        return s;
    }
}
