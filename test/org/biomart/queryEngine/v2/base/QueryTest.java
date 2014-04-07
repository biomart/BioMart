package org.biomart.queryEngine.v2.base;

import org.biomart.common.exceptions.BioMartException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author jhsu
 */
public class QueryTest {
    @Test
    public void testQueryBuild() {
        String xml = "<Query processor=\"TSV\" limit=\"100\" header=\"true\" client=\"testclient\">"
                + "<Dataset name=\"mydataset\" config=\"myconfig\">"
                + "<Filter name=\"myfilter\" value=\"foobar\"/>"
                + "<Attribute name=\"myattribute\"/>"
                + "</Dataset>"
                + "</Query>";

        Query query = new Query(xml);

        assertEquals(100, query.limit);
        assertEquals(true, query.header);
        assertEquals("testclient", query.client);

        assertEquals(1, query.datasets.size());

        Dataset ds = query.datasets.get(0);

        assertEquals("mydataset", ds.name);
        assertEquals(1, ds.filters.size());
        assertEquals(1, ds.attributes.size());
        assertEquals("myfilter", ds.filters.get(0).name);
        assertEquals("foobar", ds.filters.get(0).value);
        assertEquals("myattribute", ds.attributes.get(0).name);
    }

    @Test
    public void testHeaderValues() {
        String xmlTmpl = "<Query processor=\"TSV\" limit=\"100\" header=\"%s\" client=\"testclient\">"
                + "<Dataset name=\"mydataset\" config=\"myconfig\">"
                + "<Filter name=\"myfilter\" value=\"foobar\"/>"
                + "<Attribute name=\"myattribute\"/>"
                + "</Dataset>"
                + "</Query>";

        Query query = new Query(String.format(xmlTmpl, "true"));
        assertEquals(true, query.header);

        query = new Query(String.format(xmlTmpl, "false"));
        assertEquals(false, query.header);

        query = new Query(String.format(xmlTmpl, "1"));
        assertEquals(true, query.header);

        query = new Query(String.format(xmlTmpl, "0"));
        assertEquals(false, query.header);

        query = new Query(String.format(xmlTmpl, "TRUE"));
        assertEquals(true, query.header);

        query = new Query(String.format(xmlTmpl, "True"));
        assertEquals(true, query.header);

        query = new Query(String.format(xmlTmpl, "FALSE"));
        assertEquals(false, query.header);

        query = new Query(String.format(xmlTmpl, "False"));
        assertEquals(false, query.header);

        query = new Query(String.format(xmlTmpl, "foo"));
        assertEquals(false, query.header);
    }

    @Test(expected=BioMartException.class)
    public void testBadLimitValue() {
        String xml = "<Query processor=\"TSV\" limit=\"abc\" header=\"true\" client=\"testclient\">"
                + "<Dataset name=\"mydataset\" config=\"myconfig\">"
                + "<Filter name=\"myfilter\" value=\"foobar\"/>"
                + "<Attribute name=\"myattribute\"/>"
                + "</Dataset>"
                + "</Query>";
        Query query = new Query(xml);
    }
}
