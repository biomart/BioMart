package org.biomart.objects.objects;

import java.util.List;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;


public class ForeignKey extends Key {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ForeignKey(final List<Column> columns) {
		super(columns,McUtils.listToStr(columns, ",")+Resources.get("FKPREFIX"));
		this.setNodeType(McNodeType.FOREIGNKEY);
	}
	
	public ForeignKey(final Column column) {
		super(column,column.getName()+Resources.get("FKPREFIX"));
		this.setNodeType(McNodeType.FOREIGNKEY);
	}

	public ForeignKey(org.jdom.Element element) {
		super(element);
		this.setNodeType(McNodeType.FOREIGNKEY);
		this.setProperty(XMLElements.COLUMN, element.getAttributeValue(XMLElements.COLUMN.toString()));
	}

	
}