package org.biomart.configurator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;

public class MultiDatasourceModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<String> header;
	private List<List<String>> table;
	private List<Integer> orderCols;
	private Map<String,List<Mart>> rowMartMap;
	
	public MultiDatasourceModel() {
		this.table = new ArrayList<List<String>>();
		this.orderCols = new ArrayList<Integer>();
		this.rowMartMap = new HashMap<String,List<Mart>>();
		this.header = new ArrayList<String>();		
	}
	
	public MultiDatasourceModel(List<List<String>> table, List<Integer> cols, Map<String,List<Mart>> rowMartMap) {
		this.table = new ArrayList<List<String>>();
		this.orderCols = new ArrayList<Integer>();
		this.rowMartMap = new HashMap<String,List<Mart>>();
		this.header = new ArrayList<String>();
		this.resetModel(table,cols,rowMartMap);
	}
	
	public void resetModel(List<List<String>> table, List<Integer> cols, Map<String,List<Mart>> rowMartMap) {
		this.clear();
		this.table = table;
		this.orderCols = new ArrayList<Integer>(cols);
		this.rowMartMap = new HashMap<String,List<Mart>>(rowMartMap);
		init();		
	}
	
	private void clear() {

		this.header.clear();
		for(List<String> item: this.table) {
			item.clear();
		}
		this.table.clear();

		this.orderCols.clear();

		for(List<Mart> martItem: this.rowMartMap.values()) {
			martItem.clear();
		}
		this.rowMartMap.clear();	
		
	}
	
	private void init() {
		header = new ArrayList<String>();
		header.add("Dataset Name");
		header.add("Dataset Display Name");
		header.add("Connection Parameters");
		header.add("Database");
		header.add("Schema");
		header.add("Hide");
		header.add("Key");
	}
	
	public String getColumnName(int col) {
		return header.get(col);
	}
	
	@Override
	public int getColumnCount() {
		if(this.table.size() == 0)
			return 0;
		else
			return this.table.get(0).size();
	}

	@Override
	public int getRowCount() {
		return this.table.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		return this.table.get(row).get(col);
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	public boolean isRowVisible(int row) {
		if("true".equals(this.table.get(row).get(5)))
			return false;
		else
			return true;
	}
	
	public boolean inMultipleMart(int row) {
		String dsName = this.table.get(row).get(PartitionUtils.DSM_DATASETNAME);
		List<Mart> martSet = this.rowMartMap.get(dsName);
		if(martSet == null || martSet.size()<=1)
			return false;
		return true;
	}
	
	// remap display column to the partition table column
	public int displayToPartitionCol(int col) {
		try {
			if(this.orderCols.isEmpty())
				return col;
			else if(col>=0 && col < this.orderCols.size())
				return this.orderCols.get(col).intValue();
			else
				return -1;
		}catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public int partitionToDisplayCol(int col) {
		if (this.orderCols.isEmpty())
			return col;
		else
			return orderCols.indexOf(new Integer(col));
	}

	public void addRow(List<String> row, List<Mart> martSet) {	
		this.table.add(row);
		this.rowMartMap.put(row.get(0), martSet);
		this.fireTableDataChanged();
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		String oldvalue = this.table.get(row).get(col);
		this.table.get(row).set(col, (String)value);
		//update hide value for all related mart and all configs
		//is it hide value?
		if(col == PartitionUtils.DSM_HIDE) {
			String dsName = this.table.get(row).get(PartitionUtils.DSM_DATASETNAME);
			//find all mart
			for(Mart mart: rowMartMap.get(dsName)) {
				PartitionTable pt = mart.getSchemaPartitionTable();
				//the row in a mart is different from the row here
				int realRow = this.getReallyRowInMart(mart, row);
				if(realRow == -1)
					continue;
				for(Config config: mart.getConfigList()) {
					String hideColumn = config.getPropertyValue(XMLElements.DATASETHIDEVALUE);
					if(McUtils.isStringEmpty(hideColumn))
						continue;
					int configCol = McUtils.getPartitionColumnValue(hideColumn);
					pt.setValue(realRow, configCol, (String)value);
				}
			}
		} //is it displayname
		else if(col == PartitionUtils.DSM_DISPLAYNAME) {
			String dsName = this.table.get(row).get(PartitionUtils.DSM_DATASETNAME);
			//find all mart
			for(Mart mart: rowMartMap.get(dsName)) {
				int realRow = this.getReallyRowInMart(mart, row);
				if(realRow == -1)
					continue;
				PartitionTable pt = mart.getSchemaPartitionTable();
				pt.setValue(realRow, PartitionUtils.DISPLAYNAME, (String)value);				
			}			
		} //dataset name
		else if(col == PartitionUtils.DSM_DATASETNAME) {
			if(rowMartMap.get(oldvalue).size() == 1) {
				Mart mart = rowMartMap.get(oldvalue).iterator().next();
				MartController.getInstance().renameDataset(mart, oldvalue, (String)value);
			} else {
				List<Mart> newMartSet = new ArrayList<Mart>();
				for(Mart mart: rowMartMap.get(oldvalue)) {
					//get the oldvalue and newvalue for the mart from the common name
					//find the first one the has a dataset name ends with _oldvalue
					String olddataset = null;
					PartitionTable pt = mart.getSchemaPartitionTable();
					for(int i=0; i<pt.getTotalRows(); i++) {
						if(pt.getValue(i, PartitionUtils.DATASETNAME).endsWith("_"+oldvalue)) {
							olddataset = pt.getValue(i, PartitionUtils.DATASETNAME);
							break;
						}
					}
					int prefixIndex = olddataset.lastIndexOf("_");
					String prefix = olddataset.substring(0,prefixIndex);
					String newdataset = prefix+"_"+(String)value;
					MartController.getInstance().renameDataset(mart, olddataset, newdataset);	
					newMartSet.add(mart);
				}
				//update rowMartMap
				rowMartMap.remove(oldvalue);
				rowMartMap.put((String)value, newMartSet);
			}
		}
		else if(col == PartitionUtils.DSM_CONNECTION) {
			//do nothing for now, handled by context menu
		} //database
		else if(col == PartitionUtils.DSM_DATABASE) {
			String dsName = this.table.get(row).get(PartitionUtils.DSM_DATASETNAME);
			//find all mart
			for(Mart mart: rowMartMap.get(dsName)) {
				int realRow = this.getReallyRowInMart(mart, row);
				if(realRow == -1)
					continue;
				PartitionTable pt = mart.getSchemaPartitionTable();
				pt.setValue(realRow, PartitionUtils.DATABASE, (String)value);				
			}						
		} //schema
		else if(col == PartitionUtils.DSM_SCHEMA) {
			String dsName = this.table.get(row).get(PartitionUtils.DSM_DATASETNAME);
			//find all mart
			for(Mart mart: rowMartMap.get(dsName)) {
				int realRow = this.getReallyRowInMart(mart, row);
				if(realRow == -1)
					continue;
				PartitionTable pt = mart.getSchemaPartitionTable();
				pt.setValue(realRow, PartitionUtils.SCHEMA, (String)value);				
			}						
		}
		else if(col == PartitionUtils.DSM_KEY) {
			String dsName = this.table.get(row).get(PartitionUtils.DSM_DATASETNAME);
			//find all mart
			for(Mart mart: rowMartMap.get(dsName)) {
				int realRow = this.getReallyRowInMart(mart, row);
				if(realRow == -1)
					continue;
				PartitionTable pt = mart.getSchemaPartitionTable();
				pt.setValue(realRow, PartitionUtils.KEY, (String)value);				
			}
		}
				
		this.fireTableCellUpdated(row, col);
	}
	
	public void removeRow(int row) {
		this.table.remove(row);
		this.fireTableRowsDeleted(row, row);
	}

	public int getReallyRowInMart(Mart mart, int row) {
		String dsName =(String)this.getValueAt(row, PartitionUtils.DSM_DATASETNAME);
		//is it the dsName a common name
		boolean inCommon = this.rowMartMap.get(dsName).size()>1;
		String realDsName = null;
		if(inCommon) {
			//get the prefix
			String fullName = mart.getSchemaPartitionTable().getValue(0, PartitionUtils.DATASETNAME);
			int index = fullName.lastIndexOf("_"); 
			if(index>=0) {
				String prefix = fullName.substring(0, index);
				realDsName = prefix+"_"+dsName;
			}
			else
				realDsName = dsName;
		}else
			realDsName = dsName;
		int result = mart.getSchemaPartitionTable().getRowNumberByDatasetName(realDsName);
		if(result == -1)
			result = mart.getSchemaPartitionTable().getRowNumberByDatasetSuffix(realDsName);
		return result;
	}

	public void reorder(int i, int j) {
		List<String> remove = this.table.remove(i);
		this.table.add(j>i?j-1:j, remove);
		this.fireTableDataChanged();
	}
	
	public int getRowByName(String name) {
		for(int i=0; i< this.table.size(); i++) {
			if(this.table.get(i).get(PartitionUtils.DSM_DATASETNAME).equals(name)) {
				return i;
			}
		}
		return -1;
	}
}