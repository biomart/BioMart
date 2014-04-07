/**
 * 
 */
package org.biomart.configurator.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.DropMode;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.controller.TableRowTransferHandler;
import org.biomart.configurator.model.MultiDatasourceModel;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.view.MultiDatasourceTableRenderer;
import org.biomart.configurator.view.menu.MultiDatasourceContextMenu;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;


public class MultiDatasourcePanel extends JPanel implements TableModelListener {

	/* (non-Javadoc)
	 * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
	 */
	
	private static final long serialVersionUID = 1L;
	private MultiDatasourceContextMenu popupMenu;
	private JTable table;
	private List<Mart> martList;
	private List<Integer> cols;
	private Map<String,List<Mart>> rowMarts;
	private List<List<String>> commonTable;
	
	public MultiDatasourcePanel(List<Mart> martList, List<Integer> cols) {
		this.martList = martList;
		this.cols = cols;
		this.rowMarts = new LinkedHashMap<String,List<Mart>>();
		this.init();
	}
	
	private void init() {
		this.setLayout(new BorderLayout());	
		MultiDatasourceModel model = new MultiDatasourceModel();
		this.reorderModel(model,this.martList);
		model.addTableModelListener(this);
		
		JTable tmpTable = new JTable(model);
		table = this.autoResizeColWidth(tmpTable, model);
		
		table.setShowGrid(true);
		table.setGridColor(Color.BLACK);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.setDragEnabled(true);
		table.setDropMode(DropMode.INSERT);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setTransferHandler(new TableRowTransferHandler(this.martList));	
		
		MouseListener popupListener = new PopupListener();
		table.addMouseListener(popupListener);
		table.getTableHeader().addMouseListener(popupListener);
		table.setPreferredScrollableViewportSize(table.getPreferredSize());
		table.setDefaultRenderer(Object.class, new MultiDatasourceTableRenderer());
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		//sort column
		JTableHeader header = table.getTableHeader() ;
		header.addMouseListener(
		  new MouseAdapter() {
			    public void mouseClicked(MouseEvent e)
			    {
			      JTableHeader h = (JTableHeader)e.getSource() ;
			      int nColumn = h.columnAtPoint(e.getPoint());
			      sortColumn(nColumn);
			    }
		  }
		);
		JScrollPane scrollPane = new JScrollPane(table);

		this.add(scrollPane,BorderLayout.CENTER);	
	}
	
	/*
	 * reorder the dataset based on the first mart
	 */
	public void reorderModel(MultiDatasourceModel model, List<Mart> martList) {
		this.martList = martList;
		this.rowMarts.clear();
	
		Map<String,List<Mart>> tmpRowMarts = new LinkedHashMap<String,List<Mart>>();
		//get the union of partition table
		commonTable = new ArrayList<List<String>>();
		//FIXME still have problem when a mart doesn't follow the name convention
		boolean singleMart = martList.size() == 1;
		for(Mart mart: martList) {
			//the dataset order is based on the first mart
			PartitionTable pt = mart.getSchemaPartitionTable();
			for(int i=0; i<pt.getTotalRows(); i++) {
				String ds = pt.getValue(i, PartitionUtils.DATASETNAME);
				String commonDs = null;
				if(singleMart)
					commonDs = ds;
				else {
					//find the last 
					int index = ds.lastIndexOf("_");
					if(index>=0) {
						commonDs = ds.substring(index+1);
					}else
						commonDs = ds;
				}
				List<Mart> martset = tmpRowMarts.get(commonDs);
				if(martset == null) {
					martset = new ArrayList<Mart>();
					tmpRowMarts.put(commonDs,martset);
					//add to common table
					List<String> row = new ArrayList<String>();
					//row.add(commonDs);
					row.add(ds);
					row.add(pt.getValue(i, PartitionUtils.DISPLAYNAME));
					row.add(pt.getValue(i, PartitionUtils.CONNECTION));
					row.add(pt.getValue(i, PartitionUtils.DATABASE));
					row.add(pt.getValue(i, PartitionUtils.SCHEMA));
					row.add(pt.getValue(i, PartitionUtils.HIDE));
					row.add(pt.getValue(i, PartitionUtils.KEY));
					commonTable.add(row);
				} else {
					//check if the value are the same, if not, change it to (multiple value)
					int k = this.getRowByDatasetName(commonDs);
					//change the value to common name
					this.commonTable.get(k).set(0, commonDs);
					int oldColumn = PartitionUtils.DISPLAYNAME;
					String oldValue = pt.getValue(i, oldColumn);
					if(!oldValue.equals(commonTable.get(k).get(cols.indexOf(oldColumn)))) {
						commonTable.get(k).set(cols.indexOf(oldColumn), "(multiple value)");
					}
					
					oldColumn = PartitionUtils.CONNECTION;
					oldValue = pt.getValue(i, oldColumn);
					if(!oldValue.equals(commonTable.get(k).get(cols.indexOf(oldColumn)))) {
						commonTable.get(k).set(cols.indexOf(oldColumn), "(multiple value)");
					}
					
					oldColumn = PartitionUtils.DATABASE;
					oldValue = pt.getValue(i, oldColumn);
					if(!oldValue.equals(commonTable.get(k).get(cols.indexOf(oldColumn)))) {
						commonTable.get(k).set(cols.indexOf(oldColumn), "(multiple value)");
					}
					
					oldColumn = PartitionUtils.SCHEMA;
					oldValue = pt.getValue(i, oldColumn);
					if(!oldValue.equals(commonTable.get(k).get(cols.indexOf(oldColumn)))) {
						commonTable.get(k).set(cols.indexOf(oldColumn), "(multiple value)");
					}
					
					oldColumn = PartitionUtils.HIDE;
					oldValue = pt.getValue(i, oldColumn);
					if(!oldValue.equals(commonTable.get(k).get(cols.indexOf(oldColumn)))) {
						commonTable.get(k).set(cols.indexOf(oldColumn), "(multiple value)");
					}
					
					oldColumn = PartitionUtils.KEY;
					oldValue = pt.getValue(i, oldColumn);
					if(!oldValue.equals(commonTable.get(k).get(cols.indexOf(oldColumn)))) {
						commonTable.get(k).set(cols.indexOf(oldColumn), "(multiple value)");
					}
					
				}
				martset.add(mart);
			}
		}
		//rename rowMarts
		for(Map.Entry<String, List<Mart>> entry: tmpRowMarts.entrySet()) {
			if(entry.getValue().size() == 1) {
				List<Mart> martSet = entry.getValue();
				String key = entry.getKey();
				String dsName = this.getFullDatasetName(martSet.iterator().next(), key);
				this.rowMarts.put(dsName, martSet);
			}else
				this.rowMarts.put(entry.getKey(), entry.getValue());
		}
		
		//generate new PtModel based on new column order
		model.resetModel(commonTable, this.cols,this.rowMarts);
		model.fireTableDataChanged();
	}
	
	
 	private void createContextMenu(int row, int col) {
		popupMenu = new MultiDatasourceContextMenu(this.table, this.rowMarts, row, col);
	}
	
	
	class PopupListener implements MouseListener {
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				int[] selectedRows = table.getSelectedRows();
				int row = table.rowAtPoint(e.getPoint());
				if(selectedRows.length<=0) { //select the row
					ListSelectionModel selectionModel = table.getSelectionModel();
						selectionModel.setSelectionInterval(row, row);					
				} else { //check if row is selected
					boolean rowSelected = false;
					for(int i: selectedRows) {
						if(i == row) {
							rowSelected = true;
							break;
						}						
					}
					if(!rowSelected) {
						//unselect all and select the current one
						ListSelectionModel selectionModel = table.getSelectionModel();
						selectionModel.setSelectionInterval(row, row);											
					}
				}
				MultiDatasourcePanel.this.createContextMenu(table.rowAtPoint(e.getPoint()),table.columnAtPoint(e.getPoint()));			
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		public void mouseClicked(MouseEvent e) {
			if(e.getClickCount() == 2){
				MultiDatasourcePanel.this.createContextMenu(table.rowAtPoint(e.getPoint()),table.columnAtPoint(e.getPoint()));
				popupMenu.EditConnection();
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}


	public void tableChanged(TableModelEvent e) {
		this.table.setPreferredScrollableViewportSize(this.table.getPreferredSize());		
	}
	

	private JTable autoResizeColWidth(JTable table, MultiDatasourceModel model) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(model);
 
        int margin = 5;
        for (int i = 0; i < table.getColumnCount(); i++) {
            int  vColIndex = i;
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn  col = colModel.getColumn(vColIndex);
            int width  = 0;
 
            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer(); 
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            } 
            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;
            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, vColIndex);
                comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false,
                        r, vColIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            } 
            // Add margin
            width += 2 * margin;
            // Set the width
            col.setPreferredWidth(width);
        }
 
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(
            SwingConstants.LEFT);
 
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

	public void hideDataset(boolean b) {
/*		int[] selectedRows = this.table.getSelectedRows();
		for(Config config: this.ptable.getMart().getConfigList()) {
			String hideColumn = config.getPropertyValue(XMLElements.DATASETHIDEVALUE);
			int col = McUtils.getPartitionColumnValue(hideColumn);
			// added reordered column for hideDataset
			DataSourceModel model = (DataSourceModel)this.table.getModel();
			 col = model.partitionToDisplayCol(col);
			
			for(int i: selectedRows) {
				if(b)
					this.table.setValueAt(XMLElements.TRUE_VALUE.toString(), i, col);
				else
					this.table.setValueAt(XMLElements.FALSE_VALUE.toString(), i, col);
			}
		}*/
	}
	public boolean getHideStatus() {
/*		int[] selectedRows = this.table.getSelectedRows();
		if(selectedRows.length > 0) {
			return this.ptable.isRowVisible(selectedRows[0]);
		}else
			return false;
			*/
		return false;
	}
	
	private int getRowByDatasetName(String name) {
		for(int i=0; i<this.commonTable.size(); i++) {
			List<String> row = this.commonTable.get(i);
			if(row.get(0).endsWith("_"+name) || row.get(0).equals(name)) {
				return i;
			}
		}
		return -1;
	}
	
	public void addRow(List<String> row,List<Mart> allMart) {
		MultiDatasourceModel model = (MultiDatasourceModel)this.table.getModel();
		this.rowMarts.put(row.get(0), allMart);
		model.addRow(row, allMart);
	}

	public String getFullDatasetName(Mart mart, String commonName) {
		PartitionTable pt = mart.getSchemaPartitionTable();
		for(int i=0; i<pt.getTotalRows(); i++) {
			String dsName = pt.getValue(i, PartitionUtils.DATASETNAME);
			if(dsName.endsWith("_"+commonName) || dsName.equals(commonName))
				return dsName;
		}
		return null;
	}

	/**
	 * @return the table
	 */
	public JTable getTable() {
		return table;
	}
	
	public Collection<Mart> getMartsFromRow(int row) {
		String dsName =(String)this.table.getValueAt(row, PartitionUtils.DSM_DATASETNAME);		
		return this.rowMarts.get(dsName);
	}

	public void addSelectionListener(ListSelectionListener listener) {
		this.table.getSelectionModel().addListSelectionListener(listener);
	}

	private void sortColumn(int col) {
		int column = -1;
		if(col==PartitionUtils.DSM_DATASETNAME) {
			column = PartitionUtils.DATASETNAME;
		} else if(col == PartitionUtils.DSM_DISPLAYNAME) {
			column = PartitionUtils.DISPLAYNAME;
		}
		for(Mart mart: martList) {
			McGuiUtils.INSTANCE.sortPartitionTable(mart.getSchemaPartitionTable(), column, true);
		}
		MultiDatasourceModel model = (MultiDatasourceModel)this.table.getModel();
		this.reorderModel(model, martList);
	}
}
