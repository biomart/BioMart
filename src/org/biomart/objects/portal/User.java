package org.biomart.objects.portal;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

public class User extends MartConfiguratorObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public User(String userName, String displayName, String password) {
		super(userName);
		this.setDisplayName(displayName);
		this.setProperty(XMLElements.PASSWORD, password);
		this.setNodeType(McNodeType.USER);
	}
	
	public User(Element element) {
		super(element);
		this.setNodeType(McNodeType.USER);
	}
	
	@Override
	public Element generateXml() {
		Element element = new Element(XMLElements.USER.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.DESCRIPTION));
		element.setAttribute(XMLElements.PASSWORD.toString(),this.getPropertyValue(XMLElements.PASSWORD));
		element.setAttribute(XMLElements.HIDE.toString(),this.getPropertyValue(XMLElements.HIDE));
		element.setAttribute(XMLElements.OPENID.toString(),this.getPropertyValue(XMLElements.OPENID));
		return element;
	}

	@Override
	public void synchronizedFromXML() {

	}
	
}