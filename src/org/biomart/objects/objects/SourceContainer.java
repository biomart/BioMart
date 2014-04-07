package org.biomart.objects.objects;

import java.util.List;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.McNodeType;
import org.jdom.Element;

public class SourceContainer extends MartConfiguratorObject {
	boolean expanded = true;
	
	public SourceContainer(Element element) {
		super(element);
		this.setNodeType(McNodeType.SOURCECONTAINER);
	}
	
	public SourceContainer(String name) {
		super(name);
		this.setNodeType(McNodeType.SOURCECONTAINER);
	}
	

	public List<Mart> getMartList() {
		return ((MartRegistry)this.getParent().getParent()).getMartsInGroup(this.getName());
	}
	
	@Override
	public Element generateXml() throws FunctionalException {
		Element element = new Element(XMLElements.SOURCECONTAINER.toString());
		super.saveConfigurableProperties(element);
		return element;
	}

	@Override
	public void synchronizedFromXML() {
		
	}

	public boolean isGrouped() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.GROUP));
	}
	
	public void setGroup(boolean b) {
		this.setProperty(XMLElements.GROUP, Boolean.toString(b));
	}
	
	public boolean isExpanded() {
		return this.expanded;
	}
	
	public void setExpanded(boolean b) {
		this.expanded = b;
	}
}