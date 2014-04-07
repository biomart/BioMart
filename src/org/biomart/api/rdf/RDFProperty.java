/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.biomart.api.rdf;

import java.util.Map;

/**
 *
 * @author jbaran
 */
public class RDFProperty extends RDFObject {
    private final String range;
    private final String attribute;
    private final String filter;

    public RDFProperty(String attribute,
                String filter,
                String name,
                String range,
                Map<String, String> namespaces
            ) {
        super(name, namespaces);

        this.attribute = attribute;
        this.filter = filter;
        this.range = range;
    }

    public String getAttribute() { return attribute; }

    public String getFilter() { return filter; }

    public String getRange() { return range; }

    public String getFullRange() { return expandPrefix(range); }

    // Similar if everything matches but the filter is empty
    // in this and/or the other object.
    public boolean isSimilar(RDFProperty property) {
        return super.equals(property) &&
            attribute.equals(property.getAttribute()) &&
            range.equals(property.getRange()) &&
            (filter.isEmpty() || property.getFilter().isEmpty());
    }
}
