package org.biomart.configurator.component;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Options;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Element;

public class PtModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String col = "col";
	private List<String> columns;
	private PartitionTable partitionTableObject;
	private List<List<String>> table;
	private int startcol;
	private int endcol;

	
	public PtModel(PartitionTable object, int startcol, int endcol) {
		this.startcol = startcol;
		this.endcol = endcol;
		this.partitionTableObject = object;
		List<List<String>> tmpTable = object.getTable();
		this.table = this.subTable(tmpTable, startcol, endcol);
		this.recalculateColumns();
	}
	
	private List<List<String>> subTable(List<List<String>> sourceTable, int startCol, int endCol) {
		List<List<String>> subTable = new ArrayList<List<String>>();
		for(List<String> row: sourceTable) {
			List<String> newRow = new ArrayList<String>();
			int tmpEndCol = endCol;
			if(tmpEndCol == -1)
				tmpEndCol = row.size();
			for(int i=startCol; i<tmpEndCol; i++) {
				newRow.add(row.get(i));
			}
			subTable.add(newRow);
		}
		return subTable;
	}
		
	public int getColumnCount() {
		if(this.table.size()==0)
			return 0;
		return this.table.get(0).size();
		//return this.partitionTableObject.getTotalColumns();
	}

	public int getRowCount() {
		return this.table.size();
	}

	public Object getValueAt(int x, int y) {
		return this.table.get(x).get(y);
	}
	
	public String getColumnName(int col) {
		return columns.get(col);
	}
	
	private void recalculateColumns() {
		columns = new ArrayList<String>();
		if(this.partitionTableObject.getTotalRows()>0) {
			this.table = this.subTable(this.partitionTableObject.getTable(),startcol,endcol);
			//int colSize = this.partitionTableObject.getTotalColumns();
			for(int i=0; i<this.table.get(0).size(); i++) {
				//hardcode some colname for p0
				if(this.partitionTableObject.getPartitionType() == PartitionType.SCHEMA) {
					if(i == PartitionUtils.CONNECTION)
						columns.add("connection");
					else if(i == PartitionUtils.DATABASE)
						columns.add("database");
					else if(i == PartitionUtils.SCHEMA)
						columns.add("schema");
					else if(i == PartitionUtils.USERNAME)
						columns.add("username");
					else if(i == PartitionUtils.PASSWORD)
						columns.add("password");
					else if(i == PartitionUtils.DATASETNAME)
						columns.add("dataset name");
					else if(i == PartitionUtils.HIDE)
						columns.add("hide");
					else if(i == PartitionUtils.DISPLAYNAME)
						columns.add("dataset displayname");
					else if(i == PartitionUtils.VERSION)
						columns.add("version");
					else if(i==PartitionUtils.KEY)
						columns.add("key");
					else
						columns.add(this.col+i);
				}else
					columns.add(this.col+i);
			}
		}else
			this.table = this.partitionTableObject.getTable();
	}
	
	public List<String> getColumn(int col) {
		return this.partitionTableObject.getCol(col);
	}
	
	public void addColumn(List<String> colList) {
		for(int i=0; i<this.getRowCount(); i++) {
			this.partitionTableObject.getTable().get(i).add(colList.get(i));
		}
		this.recalculateColumns();
		this.fireTableStructureChanged();
	}
	
	public void cloneColumn(int sourceCol) {
		if(sourceCol>=0) {
			int newCol = this.partitionTableObject.addColumn("");
			for(int i=0; i<this.partitionTableObject.getTotalRows(); i++) {
				this.partitionTableObject.setValue(i, newCol, this.partitionTableObject.getValue(i, sourceCol));
			}
		}else
			this.partitionTableObject.addColumn("");

		this.recalculateColumns();
		this.fireTableStructureChanged();
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		int col0 = 0;
		int col1 = PartitionUtils.DATASETNAME;
		int col2 = PartitionUtils.DISPLAYNAME;
		if(columnIndex == col1 || columnIndex == col2 || columnIndex == col0)
			return false;
		//Returns true if the cell at rowIndex and columnIndex is editable.
		//if (columnIndex == 0)
			//return false;
		return true;
	}
	
	@Override
	public void setValueAt(Object value, int row, int col) {
		//check if it is the dataset name, update martpointer, 
		//range in datasettable, column, importabledatasets/exportabledatasets
		boolean changable = true;
		String oldValue = this.partitionTableObject.getValue(row, col);

		if(this.partitionTableObject.getPartitionType() == PartitionType.SCHEMA && 
				col==PartitionUtils.DATASETNAME) {
			Mart mart = this.partitionTableObject.getMart();
			//change datasettable, column
			for(DatasetTable dst: mart.getDatasetTables()) {
				List<String> range = dst.getRange();
				if(range.contains(oldValue)) {
					range.remove(oldValue);
					dst.addInPartitions((String)value);
				}
				
				//update column
				for(Column column: dst.getColumnList()) {
					List<String> colRange = ((DatasetColumn) column).getRange();
					if(colRange.contains(oldValue)) {
						colRange.remove(oldValue);
						((DatasetColumn)column).addInPartitions((String)value);
					}
				}
			}
			
			//update importables/exportables, check all martpointers
			List<MartPointer> allmpList = mart.getMartRegistry().getPortal().getRootGuiContainer().getAllMartPointerListResursively();
/*			for(MartPointer mp: allmpList) {
				for(Link link: mp.getLinkList()) {
					if(link.getImportableMartPointer().getMart().equals(mart)) {					
						List<Dataset> dsList = link.getDatasetsForImportable();
						for(Dataset ds: dsList) {
							if(ds.getName().equals(oldValue)) {
								dsList.remove(ds);
								dsList.add(mart.getDatasetByName((String)value));
								break;
							}
						}
					}
					if(mp.getMart().equals(mart)) {
					List<Dataset> exDsList = link.getDatasetsForExportable();
						for(Dataset ds: exDsList) {
							if(ds.getName().equals(oldValue)) {
								exDsList.remove(ds);
								exDsList.add(mart.getDatasetByName((String)value));
								break;
							}
						}
					}
				}
			}*/
			//change options
			Element martElement = Options.getInstance().getMartElement(mart);
			for(Config config: mart.getConfigList()) {
				Element configElement = Options.getInstance().getConfigElement(config, martElement);
				configElement.getChildren();
			}
		}
		if(changable) {
			this.partitionTableObject.setValue(row, col, (String)value);
			//refresh table
			this.table = this.partitionTableObject.getTable();
			fireTableCellUpdated(row,col);
		}
	}
	
	public String getPartitionTableName() {
		return this.partitionTableObject.getName();
	}
	
	public boolean isRowVisible(int row) {
		return this.partitionTableObject.isRowVisible(row);
	}
	
	

}