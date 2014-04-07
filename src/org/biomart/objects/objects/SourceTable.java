package org.biomart.objects.objects;

import java.util.List;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.jdom.Element;


public class SourceTable extends Table {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public SourceTable(SourceSchema schema, String name) {
		super(name);
		this.parent = schema;
		this.setProperty(XMLElements.ID, ""+McUtils.getNextUniqueTableId(schema));
		this.setNodeType(McNodeType.SOURCETABLE);
	}
	
	
	public SourceTable(Element element) {
		super(element);
		this.setNodeType(McNodeType.SOURCETABLE);
		//columns
		@SuppressWarnings("unchecked")
		List<Element> columnElements = element.getChildren(XMLElements.COLUMN.toString());
		for(Element columnElement: columnElements) {
			SourceColumn sc = new SourceColumn(columnElement);
			this.addColumn(sc);
		}
		
		//primary key
		Element pkElement = element.getChild(XMLElements.PRIMARYKEY.toString());
		if(pkElement!=null) {
			PrimaryKey pk = new PrimaryKey(pkElement);
			this.setPrimaryKey(pk);
		}
		//foreign key
		@SuppressWarnings("unchecked")
		List<Element> fkElementList = element.getChildren(XMLElements.FOREIGNKEY.toString());
		for(Element fkElement: fkElementList) {
			ForeignKey fk = new ForeignKey(fkElement);
			this.addForeignKey(fk);
		}

	}
	
	public SourceSchema getSchema() {
		return (SourceSchema)this.getParent();
	}
	
	public Element generateXml() throws FunctionalException {
		Element element = new Element(XMLElements.TABLE.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getName());
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getInternalName());
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getDisplayName());
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getDescription());
		element.setAttribute(XMLElements.ID.toString(),this.getPropertyValue(XMLElements.ID));
		element.setAttribute(XMLElements.HIDE.toString(),this.getPropertyValue(XMLElements.HIDE));

		//for (String field : this.fields) {
		for (Column column : this.getColumnList()) {
			element.addContent(column.generateXml());
		}
		if(this.primaryKey!=null) 
			element.addContent(this.primaryKey.generateXml());
		
		for(ForeignKey fk: this.foreignKeyList) {
			element.addContent(fk.generateXml());
		}
		return element;
	}

	@Override
	public void synchronizedFromXML() {
		//existInPts
		String[] ranges = this.getPropertyValue(XMLElements.INPARTITIONS).split(",");
		for(String range: ranges) {
			this.addInPartitions(range);
		}

		//columns
		for(Column col: this.columnList.values()) {
			col.synchronizedFromXML();
		}
		//pk
		if(this.getPrimaryKey()!=null) {
			this.getPrimaryKey().synchronizedFromXML();
		}
		//fks
		for(ForeignKey fk: this.getForeignKeys()) {
			fk.synchronizedFromXML();
		}
		this.setObjectStatus(ValidationStatus.VALID);
	}

	@Override
	public boolean hasSubPartition() {
		// TODO Auto-generated method stub
		return false;
	}

	public int getUniqueId() {
		String id = this.getPropertyValue(XMLElements.ID);
		if("".equals(id))
			return 0;
		else
			return Integer.parseInt(id);
	}
}