package org.biomart.configurator.test.category;

import java.io.IOException;

import org.jdom.JDOMException;


public class TestOpenXmlLinkQuery extends TestQuery {

	@Override
	public boolean test() {
		
		this.testNewPortal();
		this.testOpenXML(testName);
		this.testSaveXML(testName);
		
		try {
			this.testQuery(testName);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (JDOMException e) {
			e.printStackTrace();
			return false;
		}
				
		return this.compareQuery(testName);
	}	
}