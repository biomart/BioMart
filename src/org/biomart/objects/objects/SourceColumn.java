package org.biomart.objects.objects;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.jdom.Element;


public class SourceColumn extends Column {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SourceColumn(SourceTable table, String name) {
		super(table, name);
	}
	
	public SourceColumn(Element element) {
		super(element);
	}

	@Override
	public Element generateXml() {
		org.jdom.Element element = new org.jdom.Element(XMLElements.COLUMN.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getName());
		element.setAttribute(XMLElements.HIDE.toString(),this.getPropertyValue(XMLElements.HIDE));
		if(!this.getRange().isEmpty()) {
			element.setAttribute(XMLElements.INPARTITIONS.toString(),McUtils.StrListToStr(this.getRange(), ","));
		}else
			element.setAttribute(XMLElements.INPARTITIONS.toString(),"");
		return element;
	}

	@Override
	public void synchronizedFromXML() {

	}

	
	
}