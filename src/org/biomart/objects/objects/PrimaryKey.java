package org.biomart.objects.objects;

import java.util.List;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;

public class PrimaryKey extends Key {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PrimaryKey(final List<Column> columns) {
		super(columns, McUtils.listToStr(columns, ",")+Resources.get("PKPREFIX"));
		this.setNodeType(McNodeType.PRIMARYKEY);
	}
	
	public PrimaryKey(final Column column) {
		super(column, column.getName()+Resources.get("PKPREFIX"));
		this.setNodeType(McNodeType.PRIMARYKEY);
	}

	public PrimaryKey(org.jdom.Element element) {
		super(element);
		this.setNodeType(McNodeType.PRIMARYKEY);
		this.setProperty(XMLElements.COLUMN, element.getAttributeValue(XMLElements.COLUMN.toString()));
	}

	
}