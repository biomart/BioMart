package org.biomart.objects.portal;

import java.io.Serializable;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.MartConfiguratorUtils;
import org.biomart.common.utils2.XmlUtils;
import org.jdom.Document;
import org.jdom.Element;

public class PortalObject implements Serializable {	// TODO merge with MartConfiguratorObject?

	private static final long serialVersionUID = 4832749515681067224L;
	
	public static void main(String[] args) {}

	private String xmlElementName = null;
	protected String name = null;
	protected PortalObject parent = null;	//TODO assess if always known
	
	public PortalObject(String xmlElementName, String name) {
		this.xmlElementName = xmlElementName;
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "name = " + name;
	}

	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		PortalObject portalObject=(PortalObject)object;
		return (
			this.getClass().equals(object.getClass()) &&
			(this.name==portalObject.name || (this.name!=null && name.equals(portalObject.name))) &&
			(this.parent==null && portalObject.parent==null || 
					((this.parent!=null && portalObject.parent!=null) && 
							(this.parent.getName()==portalObject.parent.getName() ||	// check that parent name is equal if both are not null
							(this.parent.getName().equals(portalObject.parent.getName())))))
		);
	}
	
	@Override
	public int hashCode() {
		/*int hash = MartConfiguratorConstants.HASH_SEED1;
		hash = MartConfiguratorConstants.HASH_SEED2 * hash + (null==name? 0 : name.hashCode());	// Sufficient for our system
		return hash;*/
		return 0;	// no risks for now TODO
	}
	
	public Element generateXml() {
		return generateXml(true);
	}
	protected Element generateXml(boolean includeName)  {
		Element element = new Element(xmlElementName);
		if (includeName) {
			MartConfiguratorUtils.addAttribute(element, "name", this.name);
		}
		return element;
	}
	public Document getXmlDocument() throws FunctionalException {
		Document document = new Document();
		Element rootElement = generateXml();
		document.setRootElement(rootElement);
		return document;
	}
	
	/**
	 * To help debug
	 */
	public String generateXmlString() throws FunctionalException {
		Document document = getXmlDocument();
		String xmlDocumentString = null;
		try {
			xmlDocumentString = XmlUtils.getXmlDocumentString(document);
		} catch (Exception e) {	// Can't happen
			e.printStackTrace();
		}
		return xmlDocumentString;
	}
}
