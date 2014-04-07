package org.biomart.objects.portal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.biomart.common.exceptions.FunctionalException;
import org.jdom.Element;

public class PortalObjectList implements Serializable {

	private static final long serialVersionUID = -8506372260965562980L;

	protected String xmlElementName = null;
	protected List<PortalObject> portalObjectList = null;
	
	public PortalObjectList(String xmlElementName) {
		this.xmlElementName = xmlElementName;
		this.portalObjectList = new ArrayList<PortalObject>();
	}
	protected void addPortalObject(PortalObject portalObject, boolean uniqueNames) throws FunctionalException {
		if (uniqueNames && (portalObject==null || this.portalObjectList.contains(portalObject))) {
			throw new FunctionalException("Invalid name (null or already an alias)");
		}
		this.portalObjectList.add(portalObject);
	}
	protected PortalObject getPortalObject(String name) {
		for (PortalObject portalObject : this.portalObjectList) {
			if (portalObject.name.equals(name)) {
				return portalObject;
			}
		}
		return null;
	}
	protected List<String> getPortalObjectNames() {
		List<String> names = new ArrayList<String>();
		for (PortalObject portalObject : this.portalObjectList) {
			names.add(portalObject.name);
		}
		return names;
	}
	
	@Override
	public String toString() {
		return "portalObjectList = " + getXmlString();
	}
	
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		PortalObjectList portalObjectList = (PortalObjectList)object;
		return (
			this.xmlElementName.equals(portalObjectList.xmlElementName)
		);
	}
	
	@Override
	public int hashCode() {
		return 0;	// always very few such objects in the portal
	}
	
	public String getXmlString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (null!=this.portalObjectList) {
			for (int i = 0; i < this.portalObjectList.size(); i++) {
				stringBuffer.append((i == 0 ? "" : ",") + this.portalObjectList.get(i).name);
			}
		}
		return stringBuffer.toString();
	}
	public Element generateXml() {
		Element element = new Element(xmlElementName);
		if (null!=this.portalObjectList) {
			for (PortalObject portalObject : this.portalObjectList) {
				element.addContent(portalObject.generateXml());
			}
		}
		return element;
	}
}
