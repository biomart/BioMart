package org.biomart.common.resources;

import java.io.File;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class ExportFormat {
	private String xmlPath = "conf/xml/export.xml";
	private Element root;
	
	public ExportFormat() {		
	    try {
	       // Build the document with SAX and Xerces, no validation
	       SAXBuilder builder = new SAXBuilder();
	       // Create the document
	       Document doc = builder.build(new File(xmlPath));
	       root = doc.getRootElement();
	    } catch (Exception e) {
	       e.printStackTrace();
	    }
	}
	
	public boolean isNodeExportable(String name) {
		String value = root.getChild("format").getAttributeValue(name);
		return (!"0".equals(value));
	}
}