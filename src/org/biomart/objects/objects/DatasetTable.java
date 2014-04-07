package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.jdom.Element;


public class DatasetTable extends Table {

	private DatasetTableType type = null;
	//FIXME very bad design, hack for queryengine
	protected Set<String> inSelectedDatasets;
	private RelationSource focusRelation;
	private int focusRelationIteration;
	private boolean orphan = false;

	public DatasetColumn getColumnByName(String name) {
		for(Column dsc: this.getColumnList()) {
			if(dsc.getName().equalsIgnoreCase(name))
				return (DatasetColumn)dsc;
		}
		return null;
	}

	
	public DatasetTableType getType() {
		return type;
	}
	
	public String toString() {
		return this.getPropertyValue(XMLElements.INTERNALNAME);
	}

	/**
	 * name equals
	 */
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		DatasetTable table=(DatasetTable)object;
		if(null != this.getMart() && null !=table.getMart())
			return this.getName().equals(table.getName()) && this.getMart().equals(table.getMart());
		else if(null !=this.getMart() && null == table.getMart())
			return false;
		else if(null == this.getMart() && null != table.getMart())
			return false;
		else
			return this.getName().equals(table.getName()) && this.inSelectedDatasets.containsAll(table.inSelectedDatasets);
	}
	
	public Element generateXml() throws FunctionalException {
		Element element = new Element(XMLElements.TABLE.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getName());
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getInternalName());
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getDisplayName());
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getDescription());
		element.setAttribute(XMLElements.HIDE.toString(),Boolean.toString(this.isHidden()));
		
		if(this.getType()!=null)
			element.setAttribute(XMLElements.TYPE.toString(),this.getType().toString());

		element.setAttribute(XMLElements.INPARTITIONS.toString(),McUtils.StrListToStr(this.getRange(), ","));

		
		for (Column column : this.getColumnList()) {
			element.addContent(column.generateXml());
		}
		if(this.primaryKey !=null)
			element.addContent(this.primaryKey.generateXml());
		for(ForeignKey fk: this.getForeignKeys()) 
			element.addContent(fk.generateXml());
		element.setAttribute(XMLElements.SUBPARTITION.toString(), this.getPropertyValue(XMLElements.SUBPARTITION));
		return element;
	}
	
	public DatasetTable(Mart mart, String name, DatasetTableType type) {
		super(name);
		this.transformationUnits = new ArrayList<TransformationUnit>();
		this.parent = mart;
		this.setType(type);
		this.setNodeType(McNodeType.TABLE);
		this.inSelectedDatasets = new HashSet<String>();
		mart.addTable(this);
	}
	
	public DatasetTable(Element element) {
		super(element);
		this.transformationUnits = new ArrayList<TransformationUnit>();
		this.setNodeType(McNodeType.TABLE);
		this.type = DatasetTableType.valueFrom(this.getPropertyValue(XMLElements.TYPE));
		this.inSelectedDatasets = new HashSet<String>();
		//dataset column
		@SuppressWarnings("unchecked")
		List<Element> colElementList = element.getChildren(XMLElements.COLUMN.toString());
		for(Element colElement: colElementList) {
			DatasetColumn dsc = new DatasetColumn(colElement);
			this.addColumn(dsc);
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
	
	public Mart getMart() {
		return (Mart)this.parent;
	}
	
	/**
	 * if it is a submain
	 * @return
	 */
	public DatasetTable getParentMainTable() {
		for(ForeignKey fk: this.getForeignKeys()) {
			if (fk.getRelations().size() > 0)
				return (DatasetTable)fk.getRelations().iterator().next().getFirstKey().getTable();
		}
		return null;
	}
	/**
	 * Changes the type of this table specified at construction time.
	 * 
	 * @param type
	 *            the type of this table. Use with care.
	 */
	public void setType(final DatasetTableType type) {
		this.type = type;
		this.setProperty(XMLElements.TYPE,type.toString());
	}


	
	public boolean inPartition(String value) {
		if(McUtils.isStringEmpty(value)) {
			if(this.isVisibleModified())
				return false;
			else
				return true;
		}
		if(McUtils.hasPartitionBinding(this.getName())) {
			Dataset ds = this.getMart().getDatasetByName(value);
			if(ds == null)
				return false;
			String realName = McUtils.getRealName(this.getName(), ds);
			return !McUtils.isStringEmpty(realName);
		}
//		if(this.getRange().isEmpty())
//			return true;
		return this.getRange().contains(value);
	}
	
	@Override
	public String getName() {
		try{
			if(this.inSelectedDatasets.isEmpty())
				return super.getName();
			else {
				String dsName = this.inSelectedDatasets.iterator().next();
				if(this.getParent() instanceof Mart){
					Mart mart = (Mart)this.getParent();
				
					Dataset ds = mart.getDatasetByName(dsName);
					return McUtils.getRealName(this.getPropertyValue(XMLElements.DISPLAYNAME), ds);
				}else
				{
					Dataset ds = this.getParentConfig().getMart().getDatasetByName(dsName);
					return McUtils.getRealName(this.getPropertyValue(XMLElements.DISPLAYNAME), ds);
				}
			}
		}catch(NullPointerException exception){
			exception.printStackTrace();
			return "";
		}
	}
	
	/**
	 * if no partition, return the name
	 * @return
	 */
	public String getPartitionedName() {
		return super.getName();
	}
		
	public DatasetTable clone() {
		DatasetTable ds = new DatasetTable(this.getMart(),super.getName(),this.getType());
		for(Column column: this.getColumnList()) {
			//TODO don't change the parent
			DatasetTable parent = (DatasetTable)column.getTable();
			ds.addColumn(column);
			column.setParent(parent);
		}
		ds.setPrimaryKey(this.getPrimaryKey());
		for(ForeignKey fk: this.getForeignKeys()) {
			ds.addForeignKey(fk);
		}
		return ds;
		
	}

	
	private final List<TransformationUnit> transformationUnits;

	/**
	 * Adds a transformation unit to the end of the chain.
	 * 
	 * @param tu
	 *            the unit to add.
	 */
	public void addTransformationUnit(final TransformationUnit tu) {
		this.transformationUnits.add(tu);
	}

	/**
	 * Gets the ordered list of transformation units.
	 * 
	 * @return the list of units.
	 */
	public List<TransformationUnit> getTransformationUnits() {
		return this.transformationUnits;
	}

	/**
	 * Obtain the focus relation for this dataset table. The focus relation
	 * is the one which the transformation uses to reach the focus table.
	 * 
	 * @return the focus relation.
	 */
	public RelationSource getFocusRelation() {
		return this.focusRelation;
	}
	
	public void setFocusRelation(RelationSource relation) {
		this.focusRelation = relation;
	}


	/**
	 * Obtain the focus relation iteration for this dataset table.
	 * 
	 * @return the focus relation iteration.
	 */
	public int getFocusRelationIteration() {
		return 0;
	}
	
	public List<Attribute> getReferences() {
		List<Attribute> attributeList = new ArrayList<Attribute>();
		Mart mart = this.getMart();
		for(Config con: mart.getConfigList()) {
			for(Attribute attribute: con.getAttributes(new ArrayList<String>(), true, true)) {
				if(attribute.getDatasetTable()!=null && attribute.getDatasetTable().getName().equals(this.getName()))
					attributeList.add(attribute);
			}
		}
		return attributeList;
	}

	public List<String> getColumnNames() {
		List<String> result = new ArrayList<String>();
		for(Column column: this.getColumnList()) {
			result.add(column.getName());
		}
		return result;
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
	public SourceSchema getSchema() {
		// TODO Auto-generated method stub
		return this.getMart().getTargetSchema().getSourceSchema();
	}

	/**
	 * Is this a no-optimiser table?
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isSkipOptimiser() {
		return false;
	}
	
	/**
	 * Is this a no-index-optimiser table?
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isSkipIndexOptimiser() {
		return false;
	}

	/**
	 * temp for germline
	 * @param value
	 */
	public void addSubPartition(String value) {
		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.SUBPARTITION)))
			this.setProperty(XMLElements.SUBPARTITION, value);
		else {
			String[] _s = this.getPropertyValue(XMLElements.SUBPARTITION).split(",");	
			List<String> list = new ArrayList<String>(Arrays.asList(_s));
			if(!list.contains(value)) {
				list.add(value);
				this.setProperty(XMLElements.SUBPARTITION, McUtils.StrListToStr(list, ","));
			}
		}
	}
	
	public Set<String> getSubPartitions() {
		Set<String> result = new HashSet<String>();
		if(!McUtils.isStringEmpty(this.getPropertyValue(XMLElements.SUBPARTITION))) {
			String[] _s = this.getPropertyValue(XMLElements.SUBPARTITION).split(",");
			for(String item: _s) 
				result.add(item);
		}
		return result;
	}
	
	public Set<String> getSubPartitions(String datasetName) {
		if(McUtils.hasPartitionBinding(this.getPropertyValue(XMLElements.SUBPARTITION))) {
			Set<String> subPts = new HashSet<String>();
			PartitionTable spt = this.getMart().getSchemaPartitionTable();
			int row = spt.getRowNumberByDatasetName(datasetName);
			if(row<0)
				return subPts;
			int col = McUtils.getPartitionColumnValue(this.getPropertyValue(XMLElements.SUBPARTITION));
			String tableNames = spt.getValue(row, col);
			String[] _tn = tableNames.split(",");
			for(String item: _tn) {
				subPts.add(item);
			}
			return subPts;
		}else
			return this.getSubPartitions();
	}
	
	public String getSubPartitionCommaList(String datasetName){
		String tableName = null;
		if(this.getSubPartitions().size()>0){
			StringBuilder tableNameSet = new StringBuilder();
			tableNameSet.append("||");
			for(String subName : this.getSubPartitions()){
				tableNameSet.append(subName);
				tableNameSet.append(",");
			}
			tableNameSet.deleteCharAt(tableNameSet.length()-1);
			tableNameSet.append("||");
			tableName = tableNameSet.toString();
		} else
			tableName = this.getName(datasetName);
		return tableName;
	}


	@Override
	public boolean hasSubPartition() {
		return !McUtils.isStringEmpty(this.getPropertyValue(XMLElements.SUBPARTITION));
	}
	
	public void setOrphan(boolean orphan) {
		this.orphan = orphan;
	}


	public boolean isOrphan() {
		return orphan;
	}

	public void setInSelectedDatasets(Set<String> inSelectedDs) {
		this.inSelectedDatasets = inSelectedDs;
	}

	public String getName(String dataset) {
		if(McUtils.hasPartitionBinding(super.getName())) {
			Mart mart = (Mart)this.getParent();			
			Dataset ds = mart.getDatasetByName(dataset);
			return McUtils.getRealName(this.getPropertyValue(XMLElements.DISPLAYNAME), ds);
		}else 
			return super.getName();
	}

	public boolean isMain() {
		return getType().equals(DatasetTableType.MAIN) || getType().equals(DatasetTableType.MAIN_SUBCLASS) || 
			getName().endsWith("main") || getName().endsWith("main||");
	}
}
