package org.biomart.api;

import org.biomart.api.lite.GuiContainer;
import org.biomart.api.lite.Container;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Filter;
import org.biomart.api.lite.Dataset;
import java.util.List;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.biomart.api.lite.Mart;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Java API (org.biomart.api.Portal object)
 *
 * @author jhsu
 */
public class PortalTest {
    private static Portal _portal;

    static {
        try {
        MartRegistryFactory factory = new XmlMartRegistryFactory("./testdata/javaapi.xml", null);
        _portal = new Portal(factory, "anonymous");
        } catch(Exception e) {
            fail("Exception initializing registry");
        }
    }

    @Test
    public void testGetRootGuiContainer() {
        GuiContainer gc = _portal.getRootGuiContainer(null);
        assertEquals("root", gc.getName());
        assertEquals(2, gc.getGuiContainerList().size());

        gc = _portal.getRootGuiContainer("martform");
        assertEquals("root", gc.getName());
        assertEquals(1, gc.getGuiContainerList().size());
    }

    @Test
    public void testGetGuiContainer() {
        GuiContainer gc = _portal.getGuiContainer("default");
        assertEquals("default", gc.getName());
    }

    @Test
    public void testGetMarts() {
        List<Mart> marts = _portal.getMarts(null);
        assertEquals(5, marts.size());

        marts = _portal.getMarts("report");
        assertEquals(1, marts.size());
        assertEquals("hsapiens_gene_ensembl_report", marts.get(0).getName());
    }

    @Test
    public void testGetDatasets() {
        List<Dataset> datasets = _portal.getDatasets("hsapiens_gene_ensembl_config");
        assertEquals(1, datasets.size());
        assertEquals("hsapiens_gene_ensembl", datasets.get(0).getName());
        assertEquals("Homo sapiens genes (GRCh37.p2)", datasets.get(0).getDisplayName());
    }

    @Test
    public void testGetFilters() {
        List<Filter> filters = _portal.getFilters("hsapiens_gene_ensembl",
                null, null);
        assertEquals(25, filters.size());

        filters = _portal.getFilters("hsapiens_gene_ensembl",
                "hsapiens_gene_ensembl_config", null);
        assertEquals(25, filters.size());

        filters = _portal.getFilters("hsapiens_gene_ensembl", 
                "hsapiens_gene_ensembl_config", "region");
        assertEquals(5, filters.size());
    }

    @Test
    public void testGetAttributes() {
        List<Attribute> attributes = _portal.getAttributes("hsapiens_gene_ensembl",
                null, null, null);
        assertEquals(1399, attributes.size());

        attributes = _portal.getAttributes("hsapiens_gene_ensembl",
                "hsapiens_gene_ensembl_config", null, null);
        assertEquals(1399, attributes.size());

        attributes = _portal.getAttributes("hsapiens_gene_ensembl",
                "hsapiens_gene_ensembl_config", "ensembl_attributes", null);
        assertEquals(23, attributes.size());
    }

    @Test
    public void testGetContainers() {
        Container container = _portal.getContainers("hsapiens_gene_ensembl", null, true, true, null);
        assertEquals("root", container.getName());
    }

//    @Test
//    public void testGetProcessorGroups() {
//        List<ProcessorGroup> groups = _portal.getProcessorGroups("hsapiens_gene_ensembl_config");
//        assertEquals(3, groups.size());
//    }
//
//    @Test
//    public void testGetProcessor() {
//        List<Processor> processors = _portal.getProcessors("hsapiens_gene_ensembl_config", "Tabular");
//        assertEquals(3, processors.size());
//    }

    /*
     * Tests for exceptions
     */
    @Test(expected=BioMartApiException.class)
    public void testGetGuiContainerWithNullName() {
        _portal.getGuiContainer(null);
    }

    @Test(expected=BioMartApiException.class)
    public void testGetDatasetsWithNullMartName() {
        _portal.getDatasets(null);
    }

    @Test(expected=BioMartApiException.class)
    public void testGetFiltersWithNullDatasets() {
        _portal.getFilters(null, null, null);
    }

    @Test(expected=BioMartApiException.class)
    public void testGetAttributesWithNullDatasets() {
        _portal.getAttributes(null, null, null, null);
    }

    @Test(expected=BioMartApiException.class)
    public void testGetContainersWithNullDatasets() {
        _portal.getContainers(null, null, null, true, null);
    }
//
//    @Test(expected=BioMartApiException.class)
//    public void testGetProcessorGroupsWithNullMartName() {
//        _portal.getProcessors(null, "Tabular");
//    }
//
//    @Test(expected=BioMartApiException.class)
//    public void testGetProcessorsWithNullMartName() {
//        _portal.getProcessors("hsapiens_gene_ensembl_config", null);
//    }

    @Test
    public void testGetLinkables() {
        List<Dataset> datasets = _portal.getLinkables("hsapiens_gene_ensembl");
        assertEquals(4, datasets.size());
    }
}
