package org.biomart.queryEngine.v2.base;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author jhsu
 */
public class DatasetTest {
    @Test
    public void testToString() {
        Dataset dataset = new Dataset("mydataset", "myconfig");
        dataset.filters.add(new Filter("myfilter1", "foo"));
        dataset.filters.add(new Filter("myfilter2", "bar"));
        dataset.attributes.add(new Attribute("myattribute1"));
        dataset.attributes.add(new Attribute("myattribute2"));
        dataset.attributes.add(new Attribute("myattribute3"));

        String xml = "<Dataset name=\"mydataset\" config=\"myconfig\">"
                + "<Filter name=\"myfilter1\" value=\"foo\"/>"
                + "<Filter name=\"myfilter2\" value=\"bar\"/>"
                + "<Attribute name=\"myattribute1\"/>"
                + "<Attribute name=\"myattribute2\"/>"
                + "<Attribute name=\"myattribute3\"/>"
                + "</Dataset>";

        assertEquals(xml, dataset.toString());
    }
}
