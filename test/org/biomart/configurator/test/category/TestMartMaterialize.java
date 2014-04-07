/**
 * 
 */
package org.biomart.configurator.test.category;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.biomart.common.exceptions.ListenerException;
import org.biomart.configurator.controller.MartConstructor;
import org.biomart.configurator.controller.MartConstructorListener;
import org.biomart.configurator.controller.SaveDDLMartConstructor;
import org.biomart.configurator.controller.MartConstructor.ConstructorRunnable;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.objects.objects.Mart;
import org.jdom.Element;
/**
 * @author lyao
 *
 */
public class TestMartMaterialize extends TestAddingSource {
	@Override
	public boolean test() {		
		this.testNewPortal();		
		this.testAddMart(testName);		
		this.testSaveSQL(testName);
		return this.compareSQL(testName);
	}
	
	public void testSaveSQL(String testcase){
		Mart mart = this.getMart();
		String sqlFile = SettingsForTest.getSavedSQLPath(testcase);
		final StringBuffer sb = new StringBuffer();
		MartConstructor martConstructor = new SaveDDLMartConstructor(sb);
		Element element = SettingsForTest.getTestCase(testcase);
		Element dbElement = element.getChild("connection").getChild("db");
		String targetDatabaseName = dbElement.getAttributeValue("database");
		String targetSchemaName = dbElement.getAttributeValue("schema");
		Collection<String> selectedPrefixes = new ArrayList<String>();
		try {
			final ConstructorRunnable cr = martConstructor.getConstructorRunnable(targetDatabaseName, targetSchemaName, mart, selectedPrefixes);
			
			cr.addMartConstructorListener(new MartConstructorListener() {
				public void martConstructorEventOccurred(final int event,
						final Object data, final org.biomart.configurator.model.MartConstructorAction action)
						throws ListenerException {

				}

			});
			cr.run();
			//after run save string buffer to file
			BufferedWriter out = new BufferedWriter(new FileWriter(sqlFile));
			out.write(sb.toString());
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public boolean compareSQL(String testcase){

		try {
			String file1 = SettingsForTest.getSourceSQLPath(testcase);
			String file2 = SettingsForTest.getSavedSQLPath(testcase);
			
			String strFile1 = FileUtils.readFileToString(new File(file1));
			String strFile2 = FileUtils.readFileToString(new File(file2));
			
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
