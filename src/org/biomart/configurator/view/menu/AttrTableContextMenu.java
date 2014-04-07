package org.biomart.configurator.view.menu;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.tree.TreePath;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.model.XMLAttributeTableModel;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.GlobalOperationAttribute;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.gui.dialogs.MultiChangesDialog;
import org.biomart.objects.enums.EntryLayout;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.GuiContainer;

public class AttrTableContextMenu extends JPopupMenu implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTable table;
	private MartConfiguratorObject mcObj;
	private String selectedAttribute;
	private MartConfigTree tree;
	
	public AttrTableContextMenu(JTable table, MartConfigTree tree) {
		this.table = table;
		this.tree = tree;
	}
		
	public void setContextObj(MartConfiguratorObject treeNode) {
		this.mcObj = treeNode;
	}
	
	public void setPoint(Point point) {
		this.setMenu(point);
			//this.addSeparator();
		this.addGlobalMenu(point);
	}
	
	private boolean setMenu(Point point) {
		this.removeAll();
		MartConfiguratorObject mcObject = mcObj;
		switch(mcObject.getNodeType()) {
		case FILTER:
			int index = this.table.rowAtPoint(point);
			String itemStr = (String)this.table.getModel().getValueAt(index, 0);
			if(itemStr.equals(XMLElements.TYPE.toString())) {
				JMenu setTypeMenu = new JMenu("Set Type");
				for(FilterType ft: FilterType.values()) {
					JCheckBoxMenuItem gloItem = new JCheckBoxMenuItem(ft.toString());
					gloItem.setActionCommand(ft.toString());
					gloItem.setSelected(ft.equals(((Filter)mcObject).getFilterType()));
					gloItem.addActionListener(this);	
					setTypeMenu.add(gloItem);
				}
				this.add(setTypeMenu);
			}
		case ATTRIBUTE:
/*			JMenuItem changeMenu = new JMenuItem("Add Partition Suffix");
			changeMenu.setActionCommand("change");
			changeMenu.addActionListener(this);
			this.add(changeMenu);
			*/
			//which row is selected
			int selectedRow = this.table.rowAtPoint(point);
			this.selectedAttribute = (String)this.table.getModel().getValueAt(selectedRow, 0);
			//check if the attribute is one of the global operation attribute
			try{
				GlobalOperationAttribute operation = GlobalOperationAttribute.valueOf(this.selectedAttribute.toUpperCase());
				if(operation == null)
					return false;
			} catch(Exception e) {
				return false;
			}
			//dsTableElement is null when the filter/attribute is dragged from other containers
			//skip this case for now TODO
			//rule: dm partitionTable should not applied to a main table
			//rule: one dm partition should not affect other dms
//			if(obj == null || obj.getDatasetColumn()==null)
//				return false;
//			DatasetTableController dst = obj.getDatasetColumn().getDatasetTableController();
//			MartController ds = dst.getMartController();
/*			JMenu changeNameMenu = new JMenu("bind with");
			for(PartitionTable pt:ds.getObject().getPartitionTableList()) {
				if(pt.getType().equals(PartitionType.DIMENSION)) {
					if(!((PartitionTableController)pt.getWrapper()).getDatasetTable().equals(dst))
						continue;
				}
				String ptName = pt.getName();
				String nameWithPt = obj.getDisplayName();
				JMenu ptMenu = new JMenu(ptName);
				changeNameMenu.add(ptMenu);
				if(pt.getType().equals(PartitionType.GROUP)) {
					String colName = "c0";
					JCheckBoxMenuItem item = new JCheckBoxMenuItem(colName);
					item.setSelected(nameWithPt.indexOf(ptName+colName)>=0);
					item.setActionCommand(colName);
					item.addActionListener(this);
					ptMenu.add(item);										
				}else {
					for(int i=0; i<pt.getTotalColumns(); i++) {
						String colName = XMLElements.PARTITIONCOLUMNPREFIX+i;
						JCheckBoxMenuItem item = new JCheckBoxMenuItem(colName);
						item.setSelected(nameWithPt.indexOf(ptName+colName)>=0);
						item.setActionCommand(colName);
						item.addActionListener(this);
						ptMenu.add(item);					
					}
				}
			}

			if(changeNameMenu.getItemCount()>0)
				this.add(changeNameMenu);
				*/
			JMenuItem reorderAttributeListMenu = new JMenuItem("reorder attribute list");
			reorderAttributeListMenu.setActionCommand("reorderattributelist");
			reorderAttributeListMenu.addActionListener(this);
			this.add(reorderAttributeListMenu);
			break;
		case GUICONTAINER:
			JMenu setGuiTypeMenu = new JMenu("Set GUIType");
			for(GuiType glo: GuiType.values()) {
				JCheckBoxMenuItem gloItem = new JCheckBoxMenuItem(glo.getDisplayName());
				gloItem.setActionCommand(glo.toString());
				gloItem.setSelected(glo.equals(((GuiContainer)mcObject).getGuiType()));
				gloItem.addActionListener(this);	
				setGuiTypeMenu.add(gloItem);
			}
						
			JMenu setConfigTypeMenu = new JMenu("Set EntryLayout");
			for(EntryLayout clo: EntryLayout.values()) {
				JCheckBoxMenuItem configItem = new JCheckBoxMenuItem(clo.toString());
				configItem.setActionCommand(clo.toString());
				configItem.setSelected(clo.equals(((GuiContainer)mcObject).getEntryLayout()));
				configItem.addActionListener(this);
				setConfigTypeMenu.add(configItem);
			}
			this.add(setGuiTypeMenu);
			this.add(setConfigTypeMenu);
			break;
		}	
		return true;
	}
	
	private void addGlobalMenu(Point point) {
		int selectedRow = this.table.rowAtPoint(point);
		this.selectedAttribute = (String)this.table.getModel().getValueAt(selectedRow, 0);
		if(this.selectedAttribute.equalsIgnoreCase(XMLElements.DISPLAYNAME.toString()) ||
				this.selectedAttribute.equalsIgnoreCase(XMLElements.NAME.toString()) ||
				this.selectedAttribute.equalsIgnoreCase(XMLElements.HIDE.toString())) {
			JMenuItem setPropertyMenu = new JMenuItem("Set "+this.selectedAttribute);
			setPropertyMenu.setActionCommand(this.selectedAttribute);
			setPropertyMenu.addActionListener(this);		
			this.add(setPropertyMenu);
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String actionString = e.getActionCommand();
		
		if(actionString.equals("change")) {
/*			GlobalOperationAttribute operationAtt = null;
			try {
				operationAtt = GlobalOperationAttribute.valueOf(this.selectedAttribute.toUpperCase());
			} catch(IllegalArgumentException exeption) {
				return;
			}

			MartConfiguratorObject mcObject = treeNode.getObject();			
			GlobalOperationWizard wizard = new GlobalOperationWizard(mcObject, operationAtt);
			Element element = (Element)mcObject;
			if(wizard.getResultString()!=null) {
				//update the value
				switch(operationAtt) {
				case DISPLAYNAME:
					element.setDisplayName(wizard.getResultString());
					break;
				case HIDE:
					break;
				}
				String tmpResult = wizard.getResultString(); 
				String bindName = tmpResult.substring(1,tmpResult.length()-1);
				((ElementController)element.getWrapper()).applyPartition(bindName, operationAtt);
				this.table.updateUI();
				McGuiUtils.refreshGui(treeNode);
			}
*/
		} else if(actionString.equals("reorderattributelist")) {
			
		} else if(actionString.equals(this.selectedAttribute)) {
			//may have multiple selected treenode
			TreePath[] treePaths = tree.getSelectionPaths();
			List<McTreeNode> nodeList = new ArrayList<McTreeNode>();
			for(TreePath tp: treePaths) {
				nodeList.add((McTreeNode)tp.getLastPathComponent());
			}
			MultiChangesDialog mcd = new MultiChangesDialog(nodeList, this.selectedAttribute);
			if(mcd.isSaved()) {
				//refresh all treenode
				for(McTreeNode node: nodeList) {
					tree.getModel().nodeStructureChanged(node);
				}
				//refresh table for the last selected node
				McTreeNode lastnode = ((McTreeNode)tree.getLastSelectedPathComponent());
			}
		} else if(GuiType.get(actionString)!=null) {
			MartConfiguratorObject mcObject = mcObj;	
			((GuiContainer)mcObject).setGuiType(GuiType.get(actionString));
			//update table
			XMLAttributeTableModel tModel = new XMLAttributeTableModel(mcObj,null,true);
			this.table.setModel(tModel);			
		} else if(EntryLayout.valueFrom(actionString)!=null) {
			MartConfiguratorObject mcObject = mcObj;	
			((GuiContainer)mcObject).setEntryLayout(EntryLayout.valueFrom(actionString));		
			XMLAttributeTableModel tModel = new XMLAttributeTableModel(mcObj,null,true);
			this.table.setModel(tModel);			
		} else if(FilterType.valueFrom(actionString)!=null) {
			TreePath[] treePaths = tree.getSelectionPaths();
			for(TreePath tp: treePaths) {
				MartConfiguratorObject mcObject = ((McTreeNode)tp.getLastPathComponent()).getObject();
				((Filter)mcObject).setFilterType(FilterType.valueFrom(actionString));
			}
		//	McGuiUtils.refreshTable(treeNode);
		}
		/*else {
			JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
			JPopupMenu pop = (JPopupMenu)item.getParent();
			String ptName = ((JMenu)pop.getInvoker()).getText()+actionString;
	
			//update Element object
			MartConfiguratorObject mcObject = treeNode.getObject();
			ElementController obj = (ElementController)((Element)mcObject).getWrapper();
	

			obj.applyPartition(ptName,operationAtt);
			
			//update treeNode and McElement object
			this.table.updateUI();
			McGuiUtils.refreshGui(treeNode);
		}*/
	}
}