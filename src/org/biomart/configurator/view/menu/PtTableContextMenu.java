package org.biomart.configurator.view.menu;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.component.PtModel;
import org.biomart.configurator.controller.TreeNodeHandler;
import org.biomart.configurator.update.UpdateMart;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.view.gui.dialogs.AddDatasetDialog;
import org.biomart.configurator.view.gui.dialogs.EditDsNameDialog;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;
import org.jdom.Element;

public class PtTableContextMenu extends JPopupMenu implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTable table;
	private int selectedCol;
	private int selectedRow;
	private final PartitionTable ptable;
	
	public PtTableContextMenu(JTable table, PartitionTable ptable, int row, int col) {
		this.ptable = ptable;
		this.table = table;
		this.selectedRow = row;
		this.selectedCol = col;
		init();
	}
	

	private void init() {
		@SuppressWarnings("unchecked")
		List<Element> elementList = PTableMenuConfig.getInstance().getMenuElement("ptmenu").getChildren();
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
		this.addSeparator();
		JCheckBoxMenuItem hideItem = new JCheckBoxMenuItem(XMLElements.HIDE.toString());
		hideItem.setActionCommand(XMLElements.HIDE.toString());
		hideItem.addActionListener(this);
		hideItem.setSelected(!this.ptable.isRowVisible(selectedRow));
		this.add(hideItem);
		//edit connection parameters
		if(this.selectedCol== PartitionUtils.CONNECTION) {
			JMenuItem editcsItem = new JMenuItem("edit connection");
			editcsItem.setActionCommand("editconnection");
			editcsItem.addActionListener(this);
			this.add(editcsItem);
		}else if(this.selectedCol == PartitionUtils.DATASETNAME) {
			JMenuItem editcsItem = new JMenuItem("edit name");
			editcsItem.setActionCommand("editname");
			editcsItem.addActionListener(this);
			this.add(editcsItem);			
		}else if(this.selectedCol == PartitionUtils.DISPLAYNAME) {
			JMenuItem editcsItem = new JMenuItem("edit displayname");
			editcsItem.setActionCommand("editdisplayname");
			editcsItem.addActionListener(this);
			this.add(editcsItem);			
		}else if(this.selectedCol == PartitionUtils.DATABASE) {
			JMenuItem editcsItem = new JMenuItem("edit database name");
			editcsItem.setActionCommand("editdatabasename");
			editcsItem.addActionListener(this);
			this.add(editcsItem);						
		}else if(this.selectedCol == PartitionUtils.SCHEMA) {
			JMenuItem editcsItem = new JMenuItem("edit schema name");
			editcsItem.setActionCommand("editschemaname");
			editcsItem.addActionListener(this);
			this.add(editcsItem);						
		}else {
			JMenuItem editcsItem = new JMenuItem("edit");
			editcsItem.setActionCommand("editCol");
			editcsItem.addActionListener(this);
			this.add(editcsItem);
		}
	}

	/**
	 * make a clone of the first column
	 */
	private void columnClone(int index) {
		PtModel model = (PtModel)this.table.getModel();
		model.cloneColumn(index);
	}
		

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("newColumn")) {
			this.columnClone(-1);
		}else if(e.getActionCommand().equals("clonecolumn")) {
			this.columnClone(this.selectedCol);
		}else if(e.getActionCommand().equals("references")) {
			this.showReferences();
		}else if(e.getActionCommand().equals(XMLElements.HIDE.toString())) {
			if(this.ptable.isRowVisible(selectedRow))
				this.hideDataset(true);
			else
				this.hideDataset(false);			
		}else if(e.getActionCommand().equals("editconnection")) {
			String col0 = (String)this.table.getValueAt(selectedRow, selectedCol);
			String col1 = (String)this.table.getValueAt(selectedRow,PartitionUtils.DATABASE);
			String col2 = (String)this.table.getValueAt(selectedRow,PartitionUtils.SCHEMA);
			String col3 = (String)this.table.getValueAt(selectedRow,PartitionUtils.USERNAME);
			String col4 = (String)this.table.getValueAt(selectedRow,PartitionUtils.PASSWORD);
			String col5 = (String)this.table.getValueAt(selectedRow,PartitionUtils.DATASETNAME);
			String col7 = (String)this.table.getValueAt(selectedRow,PartitionUtils.DISPLAYNAME);

			AddDatasetDialog dialog = new AddDatasetDialog(this.getParentDialog(),col0,col1,col2,col3,col4,col5,col7,false,true);
			if(dialog.changed()) {
				int[] selectedRows = this.table.getSelectedRows();
				List<String> result = dialog.getResult();
				if(result.get(0).equals(AddDatasetDialog.DBPANEL)) {
					for(int i: selectedRows) {
						this.table.setValueAt(result.get(1), i, 0);
						this.table.setValueAt(result.get(2), i, PartitionUtils.USERNAME);
						this.table.setValueAt(result.get(3), i, PartitionUtils.PASSWORD);
					}
				}else {
					for(int i: selectedRows) {
						this.table.setValueAt(result.get(1), i, 0);
						this.table.setValueAt(result.get(2), i, 1);
						this.table.setValueAt(result.get(3), i, 2);
					}					
				}
			}
		}else if(e.getActionCommand().equals("editname") || e.getActionCommand().equals("editdisplayname")
				|| e.getActionCommand().equals("editdatabasename") || e.getActionCommand().equals("editschemaname")) {
			//get selected rows and names
			int[] selectedRows = this.table.getSelectedRows();
			LinkedHashMap<Integer,String> lhm = new LinkedHashMap<Integer,String>();
			for(int i: selectedRows) {
				lhm.put(i, (String)this.table.getValueAt(i, this.selectedCol));
			}
			
			EditDsNameDialog dialog;
			if(e.getActionCommand().equals("editname")){
				dialog  = new EditDsNameDialog(this.getParentDialog(),lhm,false,false);
			}else
				dialog = new EditDsNameDialog(this.getParentDialog(),lhm,false,true);
			if(dialog.changed()) {
				for(Map.Entry<Integer, String> entry: lhm.entrySet()) {
					this.table.setValueAt(entry.getValue(), entry.getKey(), this.selectedCol);
				}
			}
		}
		else if(e.getActionCommand().equals("newrow")) {
			this.ptable.addNewRow("");
		}
		else if(e.getActionCommand().equals("editCol")) {
			int[] selectedRows = this.table.getSelectedRows();
			LinkedHashMap<Integer,String> lhm = new LinkedHashMap<Integer,String>();
			for(int i: selectedRows) {
				lhm.put(i, (String)this.table.getValueAt(i, this.selectedCol));
			}
			EditDsNameDialog dialog = new EditDsNameDialog(this.getParentDialog(),lhm,true,true);
			if(dialog.changed()) {
				for(Map.Entry<Integer, String> entry: lhm.entrySet()) {
					this.table.setValueAt(entry.getValue(), entry.getKey(), this.selectedCol);
				}
			}
		}
		else if(e.getActionCommand().equals("references")) {
			//get name
			String name= "("+this.ptable.getName()+"c"+this.selectedCol+")";
			
		}
	}
	
	private void showReferences() {
     	String refStr = "("+this.ptable.getName()+"c"+this.selectedCol+")";
    	TreeNodeHandler tnh = new TreeNodeHandler();
    	tnh.requestPartitionReferences(refStr,this.ptable.getMart());
	}

	/*
	 * 1. when set hide=true in the partition table (col6), we have to set hide=true for all 
	 * access points in other columns of the partition table
	 * 2. when set hide=false in the partition table, there is no need to make any change to visibility for 
	 * other access points
	 * 3. For a given access point, its visibility is depending on both the 'datasethidevalue' 
	 * of its own and col6 in partition table. 
	 * when either of them sets hide=true, the dataset is hidden for this access point; 
	 * when both of them set hide=false, then the dataset is unhidden for this access point
	 */
	private void hideDataset(boolean b) {
		int[] selectedRows = this.table.getSelectedRows();
		if(b) {
			for(Config config: this.ptable.getMart().getConfigList()) {
				String hideColumn = config.getPropertyValue(XMLElements.DATASETHIDEVALUE);
				int col = McUtils.getPartitionColumnValue(hideColumn);
				for(int i: selectedRows) {
					if(b)
						this.table.setValueAt(XMLElements.TRUE_VALUE.toString(), i, col);
					else
						this.table.setValueAt(XMLElements.FALSE_VALUE.toString(), i, col);
				}
			}
		} else {
			for(int i: selectedRows) {
				this.table.setValueAt(XMLElements.FALSE_VALUE.toString(), i, PartitionUtils.HIDE);
			}
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
}