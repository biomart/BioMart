package org.biomart.dino.query;

import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Filter;

/**
 * Created by luca on 19/05/14.
 */
public class QueryElement {

    Element elmt;
    String value;

    public QueryElement() {
        elmt = null;
        String value = "";
    }

    public QueryElement(Element elmt) {
        this.elmt = elmt;
        value = "";
    }

    public QueryElement(Element elmt, String value) {
        this(elmt);
        this.value = value;
    }

    public String getName() {
        return elmt.getName();
    }

    public String getValue() {
        return isFilter() ? value : elmt.getName();
    }

    public boolean isEmpty() {
        return elmt == null && value.equals("");
    }

    protected boolean isFilter() {
        return elmt instanceof Filter;
    }


}
