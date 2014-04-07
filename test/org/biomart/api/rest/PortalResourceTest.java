package org.biomart.api.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Iterator;
import org.biomart.web.TestServletConfig;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfree.util.Log;
import org.junit.Test;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * 
 * @author jhsu
 */
public class PortalResourceTest extends JerseyTest {
    static {
        System.setProperty("biomart.registry.file", "./testdata/restapi.xml");

        System.setProperty("jersey.test.containerFactory",
                "com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory");
    }

    public PortalResourceTest() throws Exception {
        super(new WebAppDescriptor.Builder().contextListenerClass(TestServletConfig.class)
                .filterClass(GuiceFilter.class).contextPath("/").servletPath("/").build());
    }

    private boolean assertPropertiesNotNull(JsonNode node, String... args) {
        boolean rval = true;
        for (String arg : args) {
            if (node.get(arg) == null) {
                fail("Property \"" + arg + "\" not found for node.");
            }
        }
        return rval;
    }

    @Test
    public void testGetMartList() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/marts").accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);
        assertTrue(node.isArray());

        for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "config", "operation", "meta");
        }
    }

    @Test
    public void testGetDatasetList() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/datasets").queryParam("mart", "gene")
                .accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);

        assertTrue(node.isArray());

        for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "isHidden", "description");
        }
    }

    @Test
    public void testGetAllFilters() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/filters")
                .queryParam("datasets", "homo_sapiens_core_60_37e").queryParam("config", "gene")
                .accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);

        assertTrue(node.isArray());

        for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "isHidden", "type", "description");
        }
    }

    @Test
    public void testGetAllAttributes() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/attributes")
                .queryParam("datasets", "homo_sapiens_core_60_37e").queryParam("config", "gene")
                .accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);

        assertTrue(node.isArray());

        for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "isHidden", "value", "linkURL");
        }
    }

    @Test
    public void testGetRootGuiContainer() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/portal").accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);

        JsonNode children = node.get("guiContainers");

        assertEquals("root", node.get("name").getValueAsText());

        assertTrue(children.isArray());

        for (Iterator<JsonNode> it = children.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "guiType");
            assertTrue(curr.get("marts") != null || curr.get("guiContainers") != null);
        }
    }

    @Test
    public void testGetRootContainer() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/containers")
                .queryParam("datasets", "homo_sapiens_core_60_37e").queryParam("config", "gene")
                .accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);

        JsonNode children = node.get("containers");

        assertEquals("root", node.get("name").getValueAsText());

        assertTrue(children.isArray());

        for (Iterator<JsonNode> it = children.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "independent", "maxAttributes", "maxContainers");
        }
    }

    @Test
    public void testGetMartsForGuiContainer() throws IOException {
        WebResource webResource = resource();
        try {
            Thread.sleep(1000);
            Log.debug("test");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String responseMsg = webResource.path("/martservice/marts").queryParam("guicontainer", "default")
                .accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);
        int count = 0;

        assertTrue(node.isArray());

        for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "config", "operation", "meta");
            count++;
        }

        assertTrue(count == 2);
    }

    @Test
    public void testGetFilters() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/filters")
                .queryParam("datasets", "homo_sapiens_core_60_37e").queryParam("config", "gene")
                .queryParam("container", "gene_filter").accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);
        int count = 0;

        assertTrue(node.isArray());

        for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "isHidden", "type");
            count++;
        }

        assertEquals(14, count);
    }

    @Test
    public void testGetAttributes() throws IOException {
        WebResource webResource = resource();
        String responseMsg = webResource.path("/martservice/attributes")
                .queryParam("datasets", "homo_sapiens_core_60_37e").queryParam("config", "gene")
                .queryParam("container", "gene_attribute").accept("application/json").get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseMsg);
        int count = 0;

        assertTrue(node.isArray());

        for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
            JsonNode curr = it.next();
            assertPropertiesNotNull(curr, "displayName", "name", "isHidden", "value", "linkURL");
            count++;
        }

        assertEquals(14, count);
    }

    // @Test
    // public void testGetProcessorGroups() throws IOException {
    // WebResource webResource = resource();
    // String responseMsg = webResource.path("/martservice/processorgroups")
    // .queryParam("mart", "gene")
    // .accept("application/json")
    // .get(String.class);
    // ObjectMapper mapper = new ObjectMapper();
    // JsonNode node = mapper.readTree(responseMsg);
    //
    // assertTrue(node.isArray());
    //
    // for(Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
    // JsonNode curr = it.next();
    // assertPropertiesNotNull(curr, "displayName", "name");
    // }
    // }
    //
    // @Test
    // public void testGetProcessors() throws IOException {
    // WebResource webResource = resource();
    // String responseMsg = webResource.path("/martservice/processors")
    // .queryParam("mart", "gene")
    // .queryParam("processorgroup", "Tabular")
    // .accept("application/json")
    // .get(String.class);
    // ObjectMapper mapper = new ObjectMapper();
    // JsonNode node = mapper.readTree(responseMsg);
    //
    // assertTrue(node.isArray());
    //
    // for(Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
    // JsonNode curr = it.next();
    // assertPropertiesNotNull(curr, "displayName", "name");
    // }
    // }
    //
    // @Test
    // public void testResults() {
    //
    // String xml = "<!DOCTYPE Query><Query client=\"test\" processor=\"TSV\" limit=\"10\" header=\"1\">"
    // + "<Dataset name=\"homo_sapiens_core_60_37e\" config=\"gene\">"
    // + "<Attribute name=\"gene_gene_id_1029\"/></Dataset></Query>";
    //
    // WebResource webResource = resource();
    // String responseMsg = webResource.path("/martservice/results")
    // .queryParam("query", xml)
    // .accept("text/plain")
    // .get(String.class);
    //
    // String expected = "Gene id 1029\n"
    // + "161049\n"
    // + "161050\n"
    // + "161051\n"
    // + "161052\n"
    // + "161053\n"
    // + "161054\n"
    // + "161055\n"
    // + "161056\n"
    // + "161057\n"
    // + "161058";
    //
    // System.out.println(responseMsg);
    //
    // assertEquals(expected, responseMsg);
    // }
}
