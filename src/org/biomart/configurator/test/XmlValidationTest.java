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
public class XmlValidationTest {

	public static void main(String[] args) throws Exception {

		SAXBuilder saxBuilder = 
			//org.apache.xerces.parsers.SAXParser
			//new SAXBuilder("org.apache.xerces.parsers.DOMParser", true);
			new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
			//new SAXBuilder(true);		// true activates validation
		saxBuilder.setFeature("http://apache.org/xml/features/validation/schema", true);		// this line activates schema validation
		
		/*saxBuilder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", 
				"/home/anthony/workspace/00MartConfigurator/XML/test.xsd"
			);*/
		
	
		
		/*saxBuilder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", 
				"/home/anthony/workspace/00MartConfigurator/XML/test.xsd"
			);*/
		
		/*saxBuilder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", 
				"http://www.w3.org/2001/XMLSchema"
				//+ " " + "/home/anthony/workspace/00MartConfigurator/XML/test2.xsd"
				+ " " + "http://www.w3.org/2001/XMLSchema"
				+ " " + "http://www.w3.org/ns/sawsdl"
			);*/
		
		
		/*saxBuilder.setProperty( JAXPConstants.JAXP_SCHEMA_LANGUAGE,
                JAXPConstants.W3C_XML_SCHEMA );*/

		//System.out.println(JAXPConstants.JAXP_SCHEMA_LANGUAGE);//http://java.sun.com/xml/jaxp/properties/schemaLanguage
		
		saxBuilder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", 
				"/home/anthony/workspace/00MartConfigurator/XML/test.xsd"
			);
			
		try {
			Document document = saxBuilder.build(new File("/home/anthony/workspace/MartService/test3.xml"));
			
			
			//Document document = saxBuilder.build(new URL("http://www.biomart.org/biomart/martwsdl"));
			//Document document = saxBuilder.build("http://www.w3.org/2001/XMLSchema");
			//Document document = saxBuilder.build("/home/anthony/workspace/00MartConfigurator/XML/xmlSchema.xml");
			//Document document = saxBuilder.build(new URL("http://www.biomart.org/biomart/martxsd"));
			
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			outputter.output(document, System.out);
	   } catch (JDOMException e) {
	     e.printStackTrace();
	   } catch (IOException e) {
	     e.printStackTrace();
	   }
	}
}
