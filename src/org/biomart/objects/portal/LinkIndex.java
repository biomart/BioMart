package org.biomart.objects.portal;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

public class LinkIndex extends MartConfiguratorObject {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LinkIndex(String name) {
		super(name);
		this.setNodeType(McNodeType.LINKINDEX);
	}
	
	public Element generateXml() {
		Element element = new Element(XMLElements.LINKINDEX.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		return element;
	}

	@Override
	public void synchronizedFromXML() {

	}

	
}