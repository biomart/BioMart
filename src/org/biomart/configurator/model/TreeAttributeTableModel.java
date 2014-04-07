package org.biomart.configurator.model;

import javax.swing.tree.TreePath;

import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.MartConfiguratorObject;

public class TreeAttributeTableModel extends XMLAttributeTableModel {

	//temp added for multiselect attribute change
	private McTreeNode treeNode = null;
	private MartConfigTree mcTree = null;
	
	public TreeAttributeTableModel(MartConfigTree tree, McTreeNode treeNode,
			Dataset ds, boolean editable) {
		super(treeNode.getObject(), ds, editable);
		this.mcTree = tree;
		this.treeNode = treeNode;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * @return the treeNode
	 */
	public McTreeNode getTreeNode() {
		return treeNode;
	}
	
	@Override
	public void setValueAt(Object value, int row, int col) {
		super.setValueAt(value, row, col);
		//refresh local config change
		if(this.mcTree != null && this.treeNode != null) {
			TreePath[] tps = this.mcTree.getSelectionPaths();
			this.mcTree.getModel().reload(this.treeNode);
			this.mcTree.setSelectionPaths(tps);
		}
	}
	
	@Override
	public void setAllSelValues(String value,String oldValue, int row, int col) {
		//get all selected tree node first
		if(this.mcTree != null) {
			TreePath[] paths = this.mcTree.getSelectionPaths();
			if(paths != null) {
				for(TreePath path : paths) {
					McTreeNode node = (McTreeNode)path.getLastPathComponent();
					MartConfiguratorObject mco = node.getObject();
					McGuiUtils.ErrorMsg result = this.setValue(mco, value,oldValue, row);
					if(result == McGuiUtils.ErrorMsg.NO_MASTER_CONFIG){
						dataObj.get(row).set(col, (String)value);
						fireTableCellUpdated(row,col);
					}else {
						if(result == McGuiUtils.ErrorMsg.YES ||
								result == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
							dataObj.get(row).set(col, (String)value);
							//refresh the target configs after master config changes
							fireTableCellUpdated(row,col);							
						}			
					}
				}
			}
		}
	}
	
	/**
	 * @return the mcTree
	 */
	public MartConfigTree getMcTree() {
		return mcTree;
	}

	/**
	 * @param mcTree the mcTree to set
	 */
	public void setMcTree(MartConfigTree mcTree) {
		this.mcTree = mcTree;
	}

}