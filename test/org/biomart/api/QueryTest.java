package org.biomart.api;

import org.biomart.processors.TSV;
import org.biomart.processors.ProcessorRegistry;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Java API (org.biomart.api.Query object)
 *
 * @author jhsu
 */
public class QueryTest {
    private static Portal _portal;
    private static Portal _portal2;

    static {
        try {
            MartRegistryFactory factory = new XmlMartRegistryFactory("./testdata/javaapi.xml", null);
            _portal = new Portal(factory);
            _portal2 = new Portal(factory, "http://jaysoo.myopenid.com/");
        } catch(Exception e) {
            fail("Exception initializing registry");
        }
        ProcessorRegistry.register("TSV", TSV.class);
    }

    @Test
    public void testDefaults() {
        Query query = new Query(_portal);

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query>"
                + "<Query client=\"biomartclient\" processor=\"TSV\" limit=\"-1\" header=\"1\" />";

        String xml = query.getXml();

        xml = replaceNewlines(xml);

        assertEquals(expected, xml);
    }

    @Test
    public void testSettings() {
        Query query = new Query(_portal)
            .setHeader(false)
            .setLimit(1337)
            .setClient("helloworld")
            .setProcessor("TSVX");

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query>"
                + "<Query client=\"helloworld\" processor=\"TSVX\" limit=\"1337\" header=\"0\" />";

        String xml = query.getXml();

        xml = replaceNewlines(xml);

        assertEquals(expected, xml);
    }

    @Test
    public void testMissingEndCallXml() {
        Query query = new Query(_portal);

        query
            .addDataset("dataset1", null)
                .addFilter("hello", "world")
                .addAttribute("bar");

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query>"
                + "<Query client=\"biomartclient\" processor=\"TSV\" limit=\"-1\" header=\"1\">"
                + "<Dataset name=\"dataset1\">"
                + "<Filter name=\"hello\" value=\"world\" />"
                + "<Attribute name=\"bar\" />"
                + "</Dataset></Query>";

        String xml = query.getXml();

        xml = replaceNewlines(xml);

        assertEquals(expected, xml);
    }

    @Test
    public void testSingleDatasetElementXml() {
        Query query = new Query(_portal)
            .setProcessor("TSV")
            .setClient("test")
            .setHeader(true)
            .setLimit(1000)
            .addDataset("dataset1", "config1")
                .addFilter("hello", "world")
                .addAttribute("foo")
                .addAttribute("bar")
                .addAttribute("faz")
            .end();

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE Query><Query client=\"test\" processor=\"TSV\" limit=\"1000\" header=\"1\">"
                + "<Dataset name=\"dataset1\" config=\"config1\">"
                + "<Filter name=\"hello\" value=\"world\" />"
                + "<Attribute name=\"foo\" />"
                + "<Attribute name=\"bar\" />"
                + "<Attribute name=\"faz\" /></Dataset></Query>";

        String xml = query.getXml();

        xml = replaceNewlines(xml);

        assertEquals(expected, xml);
    }

    @Test
    public void testMultipleDatasetElementXml() {
        Query query = new Query(_portal)
            .setProcessor("TSV")
            .setClient("test")
            .setHeader(true)
            .setLimit(1000)
            .addDataset("dataset1", "config1")
                .addFilter("hello", "world")
                .addAttribute("foo")
                .addAttribute("bar")
                .addAttribute("faz")
            .end()
            .addDataset("sample1", null)
                .addAttribute("xyz")
            .end();

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE Query><Query client=\"test\" processor=\"TSV\" limit=\"1000\" header=\"1\">"
                + "<Dataset name=\"dataset1\" config=\"config1\">"
                + "<Filter name=\"hello\" value=\"world\" />"
                + "<Attribute name=\"foo\" />"
                + "<Attribute name=\"bar\" />"
                + "<Attribute name=\"faz\" /></Dataset>"
                + "<Dataset name=\"sample1\">"
                + "<Attribute name=\"xyz\" />"
                + "</Dataset></Query>";

        String xml = query.getXml();

        xml = replaceNewlines(xml);

        assertEquals(expected, xml);
    }

    @Test
    public void testResults() {
        Query query = new Query(_portal)
                .setProcessor("TSV")
                .setClient("test")
                .setHeader(true)
                .setLimit(10)
                .addDataset("hsapiens_gene_ensembl", "hsapiens_gene_ensembl_config")
                    .addFilter("chromosome_name", "1")
                    .addAttribute("ensembl_gene_id")
                .end();

        OutputStream out = new ByteArrayOutputStream();
        query.getResults(out);

        String results = out.toString();

        assertEquals(11, results.split("\n").length);
    }


    /*
     * Default processor should be TSV if not specified
     */
    @Test
    public void testDefaultProcessorResults() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE Query><Query client=\"test\" limit=\"10\" header=\"1\">"
                + "<Dataset name=\"hsapiens_gene_ensembl\" config=\"hsapiens_gene_ensembl_config\">"
                + "<Filter name=\"chromosome_name\" value=\"1\" />"
                + "<Attribute name=\"ensembl_gene_id\" />"
                + "</Dataset></Query>";

        OutputStream out = new ByteArrayOutputStream();
        _portal.executeQuery(xml, out, false);

        String results = out.toString();

        assertEquals(11, results.split("\n").length);
    }


    @Test
    public void testAuthenticatedQuery() {
        Query query = new Query(_portal2)
                .setProcessor("TSV")
                .setClient("test")
                .setHeader(true)
                .setLimit(10)
                .addDataset("hsapiens_gene_vega", "hsapiens_gene_vega_config")
                    .addAttribute("go_id")
                .end();

        OutputStream out = new ByteArrayOutputStream();
        query.getResults(out);

        String results = out.toString();

        assertEquals(11, results.split("\n").length);
    }

    private String replaceNewlines(String str) {
        return str.replaceAll("\r", "").replaceAll("\n", "");

    }
}
