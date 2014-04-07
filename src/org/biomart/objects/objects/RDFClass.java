package org.biomart.objects.objects;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.McNodeType;
import org.jdom.Element;

public class RDFClass extends MartConfiguratorObject {

	public String getValue() {
		return this.getPropertyValue(XMLElements.VALUE);
	}
	
	public void setValue(String value) {
		this.setProperty(XMLElements.VALUE, value);
	}

    public String getSubClassOf() {
        return this.getPropertyValue(XMLElements.SUBCLASSOF);
    }

    public void setSubClassOf(String parentClass) {
        this.setProperty(XMLElements.SUBCLASSOF, parentClass);
    }
	
	public String getUID() {
		return this.getPropertyValue(XMLElements.UNIQUEID);
	}
	
	public void setUID(String uid) {
		this.setProperty(XMLElements.UNIQUEID, uid);
	}
	
	public RDFClass(Element element) {
		super(element);
		this.setNodeType(McNodeType.RDFCLASS);
	}
	
	public RDFClass(String name) {
		super(name);
		this.setNodeType(McNodeType.RDFCLASS);
	}

	@Override
	public Element generateXml() {
		org.jdom.Element element = new org.jdom.Element(XMLElements.RDFCLASS.toString());
		super.saveConfigurableProperties(element);
		element.setAttribute(XMLElements.VALUE.toString(), this.getValue());
		element.setAttribute(XMLElements.UNIQUEID.toString(), this.getUID());
        if (this.getSubClassOf() != null && !this.getSubClassOf().isEmpty())
            element.setAttribute(XMLElements.SUBCLASSOF.toString(), this.getSubClassOf());
		return element;
	}

	@Override
	public void synchronizedFromXML() {
		
	}
	
}
