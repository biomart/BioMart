package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Observer;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.MartConfiguratorUtils;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.jdomUtils.JDomUtils;
import org.biomart.configurator.model.object.PartitionColumn;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.PartitionColumnType;
import org.biomart.configurator.utils.type.PartitionType;
import org.jdom.Element;

/**
 * 
 * It is partition tree
 *
 */
public class PartitionTable extends MartConfiguratorObject {

	private PartitionTable parentPartitionTable = null;
	private List<PartitionTable> children;
	private LinkedHashMap<String,PartitionColumn> tableMap;
	
	public Collection<PartitionColumn> getPartitionColumns(){
		return tableMap.values();
	}

	public void setParentPartitionTable(PartitionTable value) {
		this.parentPartitionTable = value;
	}
	
	public PartitionTable getParentPartitionTable() {
		return this.parentPartitionTable;
	}
	
	public void addChild(PartitionTable child) {
		this.children.add(child);
		child.setParentPartitionTable(this);
	}
	
	public List<PartitionTable> getChildPartitionTables() {
		return this.children;
	}
	
	public void removeChild(PartitionTable child) {
		this.children.remove(child);
	}

	
	/**
	 * @param name
	 * @return
	 */
	public int getRowNumberByDatasetName(String name) {
		for(int i=0; i<this.getTotalRows(); i++) {
			if(this.partitionType == PartitionType.SCHEMA) {
				if(this.tableMap.get(""+PartitionUtils.DATASETNAME).getValue(i).equals(name))
					return i;
			}else {
				if(name.equals(this.tableMap.get("0").getValue(i)))
					return i;
			}
			
		}
		return -1;
	}
	
	public int getRowNumberByDatasetSuffix(String suffix) {
		for(int i=0; i<this.getTotalRows(); i++) {
			if(this.partitionType == PartitionType.SCHEMA) {
				if(this.tableMap.get(""+PartitionUtils.DATASETNAME).getValue(i).endsWith(suffix))
					return i;
			}else {
				if(this.tableMap.get("0").getValue(i).endsWith(suffix))
					return i;
			}
			
		}
		return -1;
	}
	

	public Integer getTotalRows() {
		if(this.tableMap.isEmpty())
			return 0;
		return this.tableMap.values().iterator().next().size();
	}

	public Integer getTotalColumns() {
		return this.tableMap.size();
	}


	
	public int addColumn (String defaultValue) {
		int newColumnNumber = this.tableMap.size();
		int rowCount = this.getTotalRows();
		PartitionColumn pc = new PartitionColumn(this,""+newColumnNumber,PartitionColumnType.CUSTOMIZED);
		if(rowCount==0)
			pc.addNewRow(defaultValue);
		else {
			for(int i=0; i<rowCount; i++) 
				pc.addNewRow(defaultValue);
		}
		this.tableMap.put(""+newColumnNumber, pc);
		this.setProperty(XMLElements.COLS, ""+this.getTotalColumns());
		return newColumnNumber;
	}
	
	public void setValue(int row, int col, String value) {
		updateValue(row, col, value);
	}
	public void updateValue(int row, int col, String value) {
		this.tableMap.get(""+col).setRowValue(row, value);
	}
	
	public String getValue(int row, int col) {
		String value = null;
		if (row >=0 && row<this.getTotalRows() && col<this.getTotalColumns() && col >= 0) {
			value = this.tableMap.get(""+col).getValue(row);
		}
		return value;
	}
	

	public List<String> getRowNamesList() {
		return this.getCol(0);
	}

	
	/**
	 * Only for the node, children are treated separately
	 */
	public Element generateXml() throws FunctionalException {

		Element element = new Element(XMLElements.PARTITIONTABLE.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getName());
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getInternalName());
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getDisplayName());
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getDescription());
		MartConfiguratorUtils.addAttribute(element, "rows", this.getTotalRows());
		MartConfiguratorUtils.addAttribute(element, "cols", this.getTotalColumns());
//		MartConfiguratorUtils.addAttribute(element, "flatten", this.flatten);
		if(this.parentPartitionTable!=null)
			element.setAttribute("parent", this.parentPartitionTable.getName());
		if(this.partitionType!=null)
			element.setAttribute(XMLElements.TYPE.toString(),this.partitionType.toString());
		
		for (int i = 0; i < this.getTotalRows(); i++) {
			Element row = new Element(XMLElements.ROW.toString());
			row.setAttribute("id", ""+i);
			String item = null;
			if(this.getPartitionType().equals(PartitionType.SCHEMA) && !(this.getValue(i, 0).indexOf("http")>=0)) {
				//do encryption
				List<String> tmpList = new ArrayList<String>();
				for(String s: this.getRow(i))
					tmpList.add(s);
				String source = this.getValue(i, PartitionUtils.PASSWORD);
				String encrypted = null;
				try {					
					encrypted = McUtils.encrypt(source);
				} catch (Exception e) {
					Log.error("encryption error " +source);
//					e.printStackTrace();
				}
				tmpList.set(PartitionUtils.PASSWORD, encrypted);
				item = McUtils.StrListToStr(tmpList, "|");
			} else
				item = McUtils.StrListToStr(this.getRow(i), "|");
			row.setText(item);
			element.addContent(row);
		}
		
		return element;
	}
	
	private PartitionType partitionType;
	
	/**
	 * create an empty partition table
	 * @param mart
	 * @param type
	 */
	public PartitionTable(Mart mart, PartitionType type) {		
		super(XMLElements.PARTITIONPREFIX.toString()+(new ObjectController()).getNextPartitionIntName(mart));
		this.tableMap = new LinkedHashMap<String,PartitionColumn>();
		this.parent = mart;
		this.partitionType = type;
		this.setProperty(XMLElements.TYPE, type.toString());
		this.setNodeType(McNodeType.PARTITIONTABLE);
		this.children = new ArrayList<PartitionTable>();
	}
	
	
	public PartitionTable(Element element) {
		super(element);
		this.setNodeType(McNodeType.PARTITIONTABLE);
		this.children = new ArrayList<PartitionTable>();
		this.tableMap = JDomUtils.partitionTableElement2Table(this,element);
		this.partitionType = PartitionType.fromValue(element.getAttributeValue(XMLElements.TYPE.toString()));
		this.decrypt();
	}
	
	private void decrypt() {
		//decrypt
		if(this.partitionType.equals(PartitionType.SCHEMA)) {
			for(int i=0; i<this.getTotalRows(); i++) {
				if(this.getValue(i, 0).indexOf("http")>=0)
					continue;
				String pw = this.getValue(i, PartitionUtils.PASSWORD);
				try {
					String source = McUtils.decrypt(pw);
					this.setValue(i, PartitionUtils.PASSWORD, source);
				} catch (Exception e) {
					Log.error("error in decrypting password. key is " + McUtils.getKey() + " - password was not encrypted?");
				//	e.printStackTrace();
				}
				
			}
		}
	}
	
	public Mart getMart() {
		return (Mart)this.parent;
	}
	
	public int getNameInteger() {
		String nameS = this.getPropertyValue(XMLElements.INTERNALNAME).substring(1);
		int result = -1;
		try {
			result = Integer.parseInt(nameS);
		}catch(Exception e) {
			Log.debug("partition name error");
		}
		return result;
	}
	
	public PartitionType getPartitionType() {
		return this.partitionType;
	}
	
	public List<List<String>> getTable() {
		List<List<String>> table = new ArrayList<List<String>>();
		for(int i=0; i<this.getTotalRows(); i++)
			table.add(this.getRow(i));
		return table;
	}

	public List<String> getCol(int col) {
		PartitionColumn pc = this.tableMap.get(""+col);
		if(pc!=null)
			return pc.getColumnList();
		else
			return new ArrayList<String>();
	}
	
	public HashMap<String, String> getHashCol(int col){
		HashMap<String, String> outputMap = new HashMap<String, String>();
		
		List<String> keyColumn = this.getCol(5);
		List<String> dataColumn = this.getCol(col);
		
		if(keyColumn.size()==dataColumn.size()){
			for(int i = 0; i < keyColumn.size(); i++){
				outputMap.put(keyColumn.get(i), dataColumn.get(i));
			}
		}
		
		return outputMap;
	}
	
	public HashMap<String, String> getNonEmptyHashCol(int col){
		HashMap<String, String> outputMap = new HashMap<String, String>();
		
		List<String> keyColumn = this.getCol(5);
		List<String> dataColumn = this.getCol(col);
		
		if(keyColumn.size()==dataColumn.size()){
			for(int i = 0; i < keyColumn.size(); i++){
				if(dataColumn.get(i)!="")
					outputMap.put(keyColumn.get(i), dataColumn.get(i));
			}
		}
		
		return outputMap;
	}
	
	public boolean hasCol(int col) {
		return this.tableMap.get(""+col)!=null;
	}
	
	public PartitionColumn getColumnObject(int col) {
		return this.tableMap.get(""+col);
	}


	public List<String> getRow(int row) {
		List<String> rowList = new ArrayList<String>();
		for(PartitionColumn pc: this.tableMap.values()) {
			rowList.add(pc.getValue(row));
		}
		return rowList;
	}

	public void addNewRow(String value) {
		String[] columnArray = value.split("\\|",-1);
		this.addNewRow(Arrays.asList(columnArray));
	}
	
	/**
	 * need to check the size of the value, add empty column to the end
	 * @param value
	 */
	public void addNewRow(List<String> value) {
		//if it is the first one
		if(this.getTotalColumns()>0) {
			for(int i=0; i<this.getTotalColumns(); i++) {
				if(i>=value.size())
					this.tableMap.get(""+i).addNewRow("");
				else
					this.tableMap.get(""+i).addNewRow(value.get(i));
			}
		}else {
			for(int i=0; i<value.size(); i++) {
				PartitionColumn pc = new PartitionColumn(this,""+i,PartitionColumnType.CUSTOMIZED);
				pc.addNewRow(value.get(i));
				this.tableMap.put(""+i, pc);
			}
		}
		this.setProperty(XMLElements.ROWS, ""+this.getTotalRows());
	}
	
	

	/**
	 * get the row number of in a partitiontable by a pair of two columns
	 * @param col1
	 * @param index1
	 * @param col2
	 * @param index2
	 * @return
	 */
	public Integer getRowNumberFrom2Columns(String col1, int index1, String col2, int index2) {
		int i = 0;
		for(List<String> rowList: this.getTable()) {
			if(rowList.get(index1).equals(col1) && rowList.get(index2).equals(col2))
				return i;
			i++;
		}
		return -1;
	}
	/**
	 * This would find all rows in lookupColumn that match lookupValue, 
	 * and then set the values of targetColumn in those rows to targetValue.
	 * @param lookupColumn
	 * @param lookupValue
	 * @param targetColumn
	 * @param targetValue
	 */
	public void setColumnByColumn(int lookupColumn, String lookupValue, 
			int targetColumn, String targetValue)  {
		if(lookupColumn >= this.getTotalColumns() || targetColumn >= this.getTotalColumns()) {
			return;
		}
		List<String> sourceColList = this.getCol(lookupColumn);
		List<Integer> sourceIntList = new ArrayList<Integer>();
		for(int i=0; i<sourceColList.size(); i++) {
			if(sourceColList.get(i).equals(lookupValue)) {
				sourceIntList.add(i);
			}
		}
		for(Integer i : sourceIntList) {
			this.setValue(i, targetColumn, targetValue);
		}			
	}
	
	public void setColumnValue(int index, String value) {
		if(index >= this.getTotalColumns())
			return;
		for(int i =0; i< this.getTotalRows(); i++) {
			this.setValue(i, index, value);
		}
	}
	
	public void addColumnObserver(String column, Observer o) {
		this.tableMap.get(column).addObserver(o);
	}

	@Override
	public void synchronizedFromXML() {
	}

	public boolean isRowVisible(int row) {
		if(this.getPartitionType() == PartitionType.SCHEMA) {
			if(XMLElements.TRUE_VALUE.toString().equals(
					this.getValue(row, PartitionUtils.HIDE))) {
				return false;
			}
		}
		return true;	
	}

	public void swapRows(int i, int j) {
		for(PartitionColumn pc: this.tableMap.values()) {
			pc.swap(i, j);
		}	
	}
	
	public void addRow(int row, List<String> value) {
		for(int i=0; i<this.getTotalColumns(); i++) {
			PartitionColumn pc = this.getColumnObject(i);
			pc.addRow(row, value.get(i));
		}
	}
	
	public void removeRow(int row) {
		for(PartitionColumn pc: this.tableMap.values()) {
			pc.removeRow(row);
		}
	}
	
	public void reorder(int i, int j) {
		for(PartitionColumn pc: this.tableMap.values()) {
			String value = pc.removeRow(i);
			pc.addRow(j>i?j-1:j,value);
		}
	}
}
