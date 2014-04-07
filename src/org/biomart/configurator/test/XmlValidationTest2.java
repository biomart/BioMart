package org.biomart.configurator.test;


import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


/**
 * see http://java.sun.com/j2ee/1.4/docs/tutorial/doc/JAXPDOM8.html
 * @author anthony
 *
 */
public class XmlValidationTest2 {

	public static void main(String[] args) {
		SAXBuilder saxBuilder = new SAXBuilder(true);		// true activates validation
		saxBuilder.setFeature("http://apache.org/xml/features/validation/schema", true);		// this line activates schema validation
		try {
			Document document = saxBuilder.build(new File("/home/anthony/workspace/MartService/martservice.xml"));
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			outputter.output(document, System.out);
	   } catch (JDOMException e) {
	     e.printStackTrace();
	   } catch (IOException e) {
	     e.printStackTrace();
	   }
	}
}
