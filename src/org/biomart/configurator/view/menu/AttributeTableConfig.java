package org.biomart.configurator.view.menu;

import java.io.File;
import java.util.List;

import org.biomart.common.utils.XMLElements;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class AttributeTableConfig {
	private String configXML = "conf/xml/AttributeTableConfig.xml";
	private org.jdom.Element rootElement;
	
	private static AttributeTableConfig instance = null;
	
	public static AttributeTableConfig getInstance() {
		if(instance==null)
			instance = new AttributeTableConfig();
		return instance;
	}
	
	private AttributeTableConfig() {
	    try {
		       // Build the document with SAX and Xerces, no validation
		       SAXBuilder builder = new SAXBuilder();
		       // Create the document
		       Document doc = builder.build(new File(System.getProperty("org.biomart.baseDir", ".") + "/" + configXML));
		       rootElement = doc.getRootElement();
		    } catch (Exception e) {
		       e.printStackTrace();
		    }
	}
	
	@SuppressWarnings("unchecked")
	public org.jdom.Element getElementByName(String name) {
		List<Element> elementList = rootElement.getChildren();
		for(Element e: elementList) {
			if(e.getAttributeValue(XMLElements.NAME.toString()).equals(name))
				return e;
		}
		return null;
	}
	
}