/**
 * 
 */
package org.biomart.api.rest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.biomart.api.Portal;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Dataset;
import org.biomart.api.lite.Mart;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.test.category.TestAddingSource;
import org.biomart.processors.JSON;
import org.biomart.processors.ProcessorRegistry;
import org.biomart.processors.SPARQLXML;
import org.biomart.processors.TSV;
import org.biomart.processors.TSVX;
import org.biomart.web.TestServletConfig;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.junit.Test;

import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * @author lyao
 *
 */
public class RestApplicationSPARQLTest extends JerseyTest {
	static {
	        System.setProperty("biomart.registry.file", "./testdata/sparql_url_test.xml");
	        System.setProperty("biomart.registry.key.file", "./testdata/.sparql_url_test");

	        System.setProperty("jersey.test.containerFactory",
	                "com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory");
	        
	        System.setProperty("http.url", "http://localhost:9998/");
	}
	 
    public RestApplicationSPARQLTest() throws Exception {
        super(new WebAppDescriptor.Builder()
                .contextListenerClass(TestServletConfig.class)
                .filterClass(GuiceFilter.class)
                .contextPath("/")
                .servletPath("/")
                .build()
        );
        
        //ProcessorRegistry.install();


        ProcessorRegistry.register("TSV", TSV.class);
        ProcessorRegistry.register("TSVX", TSVX.class);
        ProcessorRegistry.register("SPARQL", SPARQLXML.class);
        ProcessorRegistry.register("CSV", SPARQLXML.class);
        ProcessorRegistry.register("JSON", JSON.class);
    }

    @Test
    public void testFilterQuery() throws JsonProcessingException, IOException {
        testSPARQLQuery("./testdata/sparql_url_sparql_query_filter.txt", "./testdata/sparql_url_xml_query_filter.txt");
    }

    @Test
    public void testLimitQuery() throws JsonProcessingException, IOException {
        testSPARQLQuery("./testdata/sparql_url_sparql_query_limit.txt", "./testdata/sparql_url_xml_query_limit.txt");
    }

    @Test
    public void testClassConstraintQuery() throws JsonProcessingException, IOException {
        testSPARQLQuery("./testdata/sparql_url_sparql_query_limit_class_constraint.txt", "./testdata/sparql_url_xml_query_limit.txt");
    }

    public void testSPARQLQuery(String sparqlFile, String xmlFile) throws JsonProcessingException, IOException {
    	WebResource webResource = resource();
    	String configName = "pathway_config";
    	String sparqlQuery =  FileUtils.readFileToString(new File(sparqlFile));
    	
    	//run SPARQL query
    	String sparqlMsg = webResource
	        .path("/martsemantics/"+configName+"/TSV/get/")
	        .queryParam("query", sparqlQuery)
	        .accept("application/sparql-results+xml")
	        .get(String.class);
    	
    	//run XML query
    	String xmlQuery =  FileUtils.readFileToString(new File(xmlFile));
    	String xmlMsg = webResource
	        .path("/martservice/results")
	        .queryParam("query", xmlQuery)
	        .accept("application/xml")
	        .get(String.class);
	
    	//remove the header of xml output
    	String lineSep = System.getProperty("line.separator");
    	int index = xmlMsg.indexOf(lineSep);
	    xmlMsg = xmlMsg.substring(index+1);    
    	
        assertTrue(sparqlMsg.equals(xmlMsg));
    }

	public boolean testSPARSQL(String testcase) throws FileNotFoundException, IOException {
		String xmlfile = SettingsForTest.getSavedXMLPath(testcase);
		MartRegistryFactory factory = new XmlMartRegistryFactory(xmlfile,null);
		Portal portal = new Portal(factory, null);
		ProcessorRegistry.register("TSV", TSV.class);
		Mart mart = portal.getMarts(null).get(0);
		Dataset ds = portal.getDatasets(mart.getName()).get(0);
		Attribute attr = portal.getAttributes(ds.getName(), null, null, null).get(0);
		Element element = SettingsForTest.getTestCase(testcase);
		Element queryElement = element.getChild("Query");
		XMLOutputter outputter = new XMLOutputter();
		String queryxml = outputter.outputString(queryElement);
		/*
		Query query = new Query(portal)
        .setClient("testclient")
        .setLimit(100)
        .setHeader(false)
            .addDataset(ds.getName(), mart.getConfigName())
                .addAttribute(attr.getName())
             .end();
		 */
		ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
		//query.getResults(outputstream);
		portal.executeQuery(queryxml, outputstream, false);
		
		
		Element sparqlElement = element.getChild("SPARQLQuery");
		outputstream = new ByteArrayOutputStream();
		
		try {
			
			String sparqlFile = SettingsForTest.getSourceXMLPath(testcase);
			
			RestApplicationSPARQLTest restTest = new RestApplicationSPARQLTest();
			String result =  FileUtils.readFileToString(new File(sparqlFile));

			return result.equals(outputstream.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
		
	}
}
