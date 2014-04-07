/**
 * 
 */
package org.biomart.configurator.test.category;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.biomart.api.Portal;
import org.biomart.api.Query;

import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Dataset;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.processors.ProcessorRegistry;
import org.biomart.processors.TSV;
import org.biomart.api.lite.*;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author lyao
 *
 */
public class TestQuery extends TestAddingSource {
	@Override
	public boolean test() {
		
		this.testNewPortal();		
		this.testAddMart(testName);	
		this.testSaveXML(testName);
		
		try {
			this.testQuery(testName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return this.compareQuery(testName);
	}

	
	public void testQuery(String testcase) throws IOException, JDOMException{
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
		outputstream.writeTo(new FileOutputStream(new File(SettingsForTest.getSavedQueryPath(testcase))));
	}
	
	public boolean compareQuery(String testcase){
		try {
			String file1 = SettingsForTest.getSourceQueryPath(testcase);
			String file2 = SettingsForTest.getSavedQueryPath(testcase);

			String strFile1 = FileUtils.readFileToString(new File(file1));
			String strFile2 = FileUtils.readFileToString(new File(file2));
			
			/*System.out.println("|" + file1 + ", " + strFile1 + "|");
			System.out.println("|" + file2 + ", " + strFile2 + "|");*/
			
			return strFile1.equals(strFile2);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
