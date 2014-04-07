package org.biomart.configurator.view.menu;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.TreeNodeHandler;
import org.biomart.configurator.model.MultiDatasourceModel;
import org.biomart.configurator.update.UpdateMart;
import org.biomart.configurator.view.gui.dialogs.AddDatasetDialog;
import org.biomart.configurator.view.gui.dialogs.EditDsNameDialog;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;
import org.jdom.Element;

public class MultiDatasourceContextMenu extends JPopupMenu implements
		ActionListener {

	private static final long serialVersionUID = 1L;
	private JTable table;
	private int selectedCol;
	private int selectedRow;
	private Map<String,List<Mart>> rowMarts;
	
	public MultiDatasourceContextMenu(JTable table, Map<String,List<Mart>> martList, int row, int col) {
		this.rowMarts = martList;
		this.table = table;
		this.selectedRow = row;
		this.selectedCol = col;
		init();
	}
	

	private void init() {	
		@SuppressWarnings("unchecked")
		List<Element> elementList = PTableMenuConfig.getInstance().getMenuElement("datasourcemanagement").getChildren();
		for(Element element: elementList) {
			if(element.getName().equals("Separator"))
				this.addSeparator();
			else {
				JMenuItem menuItem = new JMenuItem(element.getAttributeValue("title"));
				menuItem.addActionListener(this);
				menuItem.setActionCommand(element.getAttributeValue("name"));
				this.add(menuItem);
			}
		}
		createCustomizedMenu();
	}
	
	private void createCustomizedMenu() {
		MultiDatasourceModel model = (MultiDatasourceModel)this.table.getModel();
		
		if(model.displayToPartitionCol(this.selectedCol)== PartitionUtils.CONNECTION) {
			JMenuItem editcsItem = new JMenuItem("edit connection");
			editcsItem.setActionCommand("editconnection");
			editcsItem.addActionListener(this);
			this.add(editcsItem);
		}else if(model.displayToPartitionCol(this.selectedCol) == PartitionUtils.DATASETNAME) {
			JMenuItem editcsItem = new JMenuItem("edit name");
			editcsItem.setActionCommand("editname");
			editcsItem.addActionListener(this);
			this.add(editcsItem);			
		}else if(model.displayToPartitionCol(this.selectedCol) == PartitionUtils.DISPLAYNAME) {
			JMenuItem editcsItem = new JMenuItem("edit displayname");
			editcsItem.setActionCommand("editdisplayname");
			editcsItem.addActionListener(this);
			this.add(editcsItem);			
		}else if(model.displayToPartitionCol(this.selectedCol) == PartitionUtils.DATABASE) {
			JMenuItem editcsItem = new JMenuItem("edit database name");
			editcsItem.setActionCommand("editdatabasename");
			editcsItem.addActionListener(this);
			this.add(editcsItem);						
		}else if(model.displayToPartitionCol(this.selectedCol) == PartitionUtils.SCHEMA) {
			JMenuItem editcsItem = new JMenuItem("edit schema name");
			editcsItem.setActionCommand("editschemaname");
			editcsItem.addActionListener(this);
			this.add(editcsItem);						
		}else if(model.displayToPartitionCol(this.selectedCol) == PartitionUtils.HIDE) {
			JCheckBoxMenuItem hideItem = new JCheckBoxMenuItem(XMLElements.HIDE.toString());
			hideItem.setActionCommand(XMLElements.HIDE.toString());
			hideItem.addActionListener(this);
			//check the first mart in the selectedRow
			Mart mart = this.getMartsFromRow(this.selectedRow).iterator().next();
			hideItem.setSelected(!mart.getSchemaPartitionTable().isRowVisible(selectedRow));
			this.add(hideItem);						
		}else if(model.displayToPartitionCol(this.selectedCol) == PartitionUtils.KEY) {
			JMenuItem editkeyItem = new JMenuItem("edit key");
			editkeyItem.setActionCommand("editkeyname");
			editkeyItem.addActionListener(this);
			this.add(editkeyItem);
		}
	}

	/**
	 * make a clone of the first column
	 */
	private void columnClone(int index) {
//		MultiDatasourceModel model = (MultiDatasourceModel)this.table.getModel();
//		model.cloneColumn(index);
	}
		

	public void actionPerformed(ActionEvent e) {
		MultiDatasourceModel model = (MultiDatasourceModel)this.table.getModel();
		if(e.getActionCommand().equals("newColumn")) {
			this.columnClone(-1);
		}else if(e.getActionCommand().equals("clonecolumn")) {
			this.columnClone(this.selectedCol);
		}else if(e.getActionCommand().equals("references")) {
			this.showReferences();
		}else if(e.getActionCommand().equals(XMLElements.HIDE.toString())) {
			Collection<Mart> martList = this.getMartsFromRow(this.selectedRow);
			Mart firstMart = martList.iterator().next();		
			if(firstMart.getSchemaPartitionTable().isRowVisible(model.getReallyRowInMart(firstMart, this.selectedRow)))
				this.hideDataset(true);
			else
				this.hideDataset(false);			
		}else if(e.getActionCommand().equals("editconnection")) {
			this.EditConnection();
		}else if(e.getActionCommand().equals("editname") || e.getActionCommand().equals("editdisplayname")
				|| e.getActionCommand().equals("editdatabasename") || e.getActionCommand().equals("editschemaname")
				|| e.getActionCommand().equals("editkeyname")) {
			//get selected rows and names
			int[] selectedRows = this.table.getSelectedRows();
			LinkedHashMap<Integer,String> lhm = new LinkedHashMap<Integer,String>();
			for(int i: selectedRows) {
				lhm.put(i, (String)this.table.getValueAt(i, this.selectedCol));
			}
			EditDsNameDialog dialog = new EditDsNameDialog(this.getParentDialog(),lhm, true, true);
			if(dialog.changed()) {
				for(Map.Entry<Integer, String> entry: lhm.entrySet()) {
					this.table.setValueAt(entry.getValue(), entry.getKey(), this.selectedCol);
				}
			}
		}else if(e.getActionCommand().equals("remove")) {
			removeDataset();
		}
	}
	
	public void EditConnection() {
		// TODO Auto-generated method stub
		MultiDatasourceModel model = (MultiDatasourceModel)this.table.getModel();
		//get the first mart's conStr
		Collection<Mart> marts = this.getMartsFromRow(this.selectedRow);
		Mart firstMart = marts.iterator().next();
		PartitionTable firstPT = firstMart.getSchemaPartitionTable();
		int realRow = model.getReallyRowInMart(firstMart, this.selectedRow);
		String col0 = firstPT.getValue(realRow, PartitionUtils.CONNECTION);
		String col1 = firstPT.getValue(realRow, PartitionUtils.DATABASE);
		String col2 = firstPT.getValue(realRow, PartitionUtils.SCHEMA);
		String col3 = firstPT.getValue(realRow, PartitionUtils.USERNAME);
		String col4 = firstPT.getValue(realRow, PartitionUtils.PASSWORD);
		String col5 = firstPT.getValue(realRow, PartitionUtils.DATASETNAME);
		String col7 = firstPT.getValue(realRow, PartitionUtils.DISPLAYNAME);
		boolean multiRows = this.table.getSelectedRowCount()>1;
		AddDatasetDialog dialog = new AddDatasetDialog(this.getParentDialog(),col0,col1,col2,col3,col4,col5,col7,
				multiRows,true);
		if(dialog.changed()) {
			int[] selectedRows = this.table.getSelectedRows();
			List<String> result = dialog.getResult();
			
			if(result.get(0).equals(AddDatasetDialog.DBPANEL)) {
				for(int i: selectedRows) {
					model.setValueAt(result.get(1), i, PartitionUtils.DSM_CONNECTION);
					if(!multiRows) {
						model.setValueAt(result.get(2),i,PartitionUtils.DSM_DATABASE);
						model.setValueAt(result.get(3), i, PartitionUtils.DSM_SCHEMA);
						model.setValueAt(result.get(7), i, PartitionUtils.DSM_DISPLAYNAME);
					}
					//update connection, username, password for all mart
					Collection<Mart> martSet = this.getMartsFromRow(i);
					for(Mart mart: martSet) {
						realRow = model.getReallyRowInMart(mart, i);
						mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.CONNECTION, result.get(1));
						//if(!multiRows) {
						//	mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.DATABASE, result.get(2));
						//	mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.SCHEMA, result.get(3));
						//}
						mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.USERNAME, result.get(4));
						mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.PASSWORD, result.get(5));
						//mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.DISPLAYNAME, result.get(7));
					}
				}
			}else {
				for(int i: selectedRows) {
					model.setValueAt(result.get(1), i, PartitionUtils.DSM_CONNECTION);
					model.setValueAt(result.get(2), i, PartitionUtils.DSM_DATABASE);
					model.setValueAt(result.get(3), i, PartitionUtils.DSM_SCHEMA);
					if(!multiRows) {
						model.setValueAt(result.get(7), i, PartitionUtils.DSM_DISPLAYNAME);
					}

					//	update connection, port, path for all mart
					Collection<Mart> martSet = this.getMartsFromRow(i);
					for(Mart mart: martSet) {
						realRow = model.getReallyRowInMart(mart, i);
						mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.CONNECTION, result.get(1));
						mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.USERNAME, result.get(4));
						mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.PASSWORD, result.get(5));
						//mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.DATABASE, result.get(2));
						//mart.getSchemaPartitionTable().setValue(realRow, PartitionUtils.SCHEMA, result.get(3));
					}
				}					
			}
		}
	
	}


	private void showReferences() {
		if(this.getMartsFromRow(this.selectedRow).size()!=1) {
			JOptionPane.showMessageDialog(null, "cannot show references in multiple marts");
			return;
		}
     	String refStr = "(p0c"+this.selectedCol+")";
    	TreeNodeHandler tnh = new TreeNodeHandler();
    	tnh.requestPartitionReferences(refStr,this.getMartsFromRow(this.selectedRow).iterator().next());
	}

	private void hideDataset(boolean b) {
		int[] selectedRows = this.table.getSelectedRows();

		for(int row: selectedRows) {
			this.table.setValueAt(Boolean.toString(b), row, PartitionUtils.DSM_HIDE);
		}		
	}
	
	private JDialog getParentDialog() {
		Container parent = this.table.getParent();
		while(parent!=null && !(parent instanceof JDialog)) {
			parent = parent.getParent();
		}
		if(parent == null)
			return null;
		else
			return (JDialog)parent;
	}
	
	private void removeDataset() {
		int[] selectedRows = this.table.getSelectedRows();
		Arrays.sort(selectedRows);
		int dsm_dscol = PartitionUtils.DSM_DATASETNAME;
		MultiDatasourceModel model = (MultiDatasourceModel)this.table.getModel();
		for(int i = selectedRows.length-1; i>=0; i--) {
			model.removeRow(selectedRows[i]);
		}
	}

	private Collection<Mart> getMartsFromRow(int row) {
		String dsName =(String)this.table.getValueAt(row, PartitionUtils.DSM_DATASETNAME);		
		return this.rowMarts.get(dsName);
	}
	
}
