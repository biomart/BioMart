package org.biomart.common.utils2;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.biomart.common.exceptions.TechnicalException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XmlUtils {

	public static String getXmlDocumentString(String queryString) throws TechnicalException {
		ByteArrayOutputStream baos;
		try {
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(new StringReader(queryString));
			
			XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
			baos = new ByteArrayOutputStream();
			fmt.output(document, baos);
		} catch (JDOMException e) {
			throw new TechnicalException(e);
		} catch (IOException e) {
			throw new TechnicalException(e);
		}
        
        return baos.toString();
	}
	
	public static String getXmlDocumentString(Document document) throws TechnicalException {
		XMLOutputter prettyFormat = new XMLOutputter(Format.getPrettyFormat());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String xmlDocumentString = null;
		if (document!=null) {
			try {
				prettyFormat.output(document, baos);
				xmlDocumentString = baos.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new TechnicalException(e);
			} catch (IOException e) {
				throw new TechnicalException(e);
			}
		}
		return xmlDocumentString;
	}

	// Xml validation
	public static String validationXml(Document document) {
		SAXBuilder validator = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
		validator.setFeature("http://apache.org/xml/features/validation/schema", true);
	
		String errorMessage = null;
		try {
			XMLOutputter fmt = new XMLOutputter(Format.getCompactFormat());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			fmt.output(document, baos);
			StringReader stringReader = new StringReader(baos.toString());
			validator.build(stringReader);
			
			System.out.println("successful validation!");
			System.out.println();
		} catch (JDOMException e) {
			e.printStackTrace();
			errorMessage = e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			errorMessage = e.getMessage();
		}
		return errorMessage;
	}

	/**
	 * Doesn't not display the content
	 */
	public static String getJdomElementString(Element element) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("<" + element.getName() + " ");
		@SuppressWarnings("unchecked")
		List<Attribute> attributeList = element.getAttributes();
		for (int i = 0; i < attributeList.size(); i++) {
			Attribute attribute = attributeList.get(i);
			stringBuffer.append((i==0 ? "" : " ") + attribute.getName() + "=\"" + attribute.getValue() + "\"");
		}
		stringBuffer.append(">");
		return stringBuffer.toString();
	}

	public static void writeXmlFile(Element root) throws TechnicalException {
		XmlUtils.writeXmlFile(root, null);
	}

	public static Document writeXmlFile(Element root, String outputXmlFilePathAndName) throws TechnicalException {
		Document newDocument = new Document(root);
		XmlUtils.writeXmlFile(newDocument, outputXmlFilePathAndName);
		return newDocument;
	}

	public static void writeXmlFile(Document document, String outputXmlFilePathAndName) throws TechnicalException {
		try {
			XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
			if (null!=outputXmlFilePathAndName) {
				FileOutputStream fos = new FileOutputStream(outputXmlFilePathAndName);
				fmt.output(document, fos);
			} else {
				fmt.output(document, System.out);
			}
		} catch (FileNotFoundException e) {
			throw new TechnicalException(e);
		} catch (IOException e) {
			throw new TechnicalException(e);
		}
	}

}
