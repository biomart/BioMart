package org.biomart.configurator.view.menu;

import java.io.File;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

public class PTableMenuConfig {
	private String configXML = "conf/xml/partitionTableMenu.xml";
	private org.jdom.Element rootElement;
	
	private static PTableMenuConfig instance = null;
	
	public static PTableMenuConfig getInstance() {
		if(instance==null)
			instance = new PTableMenuConfig();
		return instance;
	}
	
	private PTableMenuConfig() {
	    try {
		       // Build the document with SAX and Xerces, no validation
		       SAXBuilder builder = new SAXBuilder();
		       // Create the document
		       Document doc = builder.build(new File(configXML));
		       rootElement = doc.getRootElement();
		    } catch (Exception e) {
		       e.printStackTrace();
		    }
	}
	
	public org.jdom.Element getMenuElement(String node) {
		return this.rootElement.getChild(node);
	}
	
}