package org.biomart.configurator.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.biomart.api.MartApi;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class XmlQueryTest {
	
    private static String XML_PATH = "../registry/";    
    private static MartApi api;
    private String queryStr;
    private static ByteArrayOutputStream outputstream;
	
    @BeforeClass
    public static void runBeforeClass() {
    	outputstream = new ByteArrayOutputStream(); 
    }
    
    @Before
    public void beforeTest() {
    	outputstream.reset();
    }
    
	@Parameters
	public static Collection<Object[]> data() {
		
		List<Object[]> query = new ArrayList<Object[]>();
		String configxml = "./conf/xml/TestCases.xml";
		SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
		try {
			Document document = saxBuilder.build(configxml);
			@SuppressWarnings("unchecked")
			List<org.jdom.Element> xmlElementList = document.getRootElement().getChildren();
			for(org.jdom.Element element: xmlElementList) {
				//get the first xml for now
				if("true".equals(element.getAttributeValue("enabled"))) {
					XMLOutputter outputter = new XMLOutputter();
					String xmlFileName = null;
					String keyFileName = null;
					if("default".equals(element.getAttributeValue("location"))) {
						xmlFileName = XML_PATH+element.getAttributeValue("xml");
						keyFileName = XML_PATH+element.getAttributeValue("key");
					}
					else {
						xmlFileName = element.getAttributeValue("location")+File.separator+element.getAttributeValue("xml");
						keyFileName = element.getAttributeValue("location")+File.separator+element.getAttributeValue("key");
					}
					//open xml
					File xmlFile = new File(xmlFileName);
					SAXBuilder parser = new SAXBuilder();
					Document doc = parser.build(xmlFile);
					api = new MartApi(doc,keyFileName);
					xmlFile = null;
					@SuppressWarnings("unchecked")
					List<org.jdom.Element> queryElementList = element.getChildren();
					for(org.jdom.Element caseElement: queryElementList) {
						if("true".equals(caseElement.getAttributeValue("enabled"))) {
							org.jdom.Element child = caseElement.getChild("Query");
							String[] queryArray = new String[] {outputter.outputString(child)};
							query.add(queryArray);
						}
					}
					break;
				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return query;
	}

	public XmlQueryTest(String queryStr) {
		this.queryStr = queryStr;
	}
    
    @Test  
    public void testQuery() {  
    	System.out.println("testing query : " + queryStr);
		try {
			api.prepareQuery(queryStr, "").runQuery(outputstream);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(outputstream.size()>0);
    } 
    
    @AfterClass  
    public static void runAfterClass() {  
    	api = null;
    	try {
			outputstream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.out.println("after class");
    }


}