/**
 * 
 */
package org.biomart.configurator.view;

import javax.swing.JDialog;

import org.biomart.configurator.jdomUtils.McTreeNode;

/**
 * @author lyao
 *
 */
public class MartConfigSourceTree extends MartConfigTree {

	private MartConfigTree targetTree;
	
	public MartConfigSourceTree(McTreeNode root, boolean acceptDrop,
			boolean enableSelectionEvent, boolean enableMouse, boolean editTable,JDialog parent) {
		super(root, acceptDrop, enableSelectionEvent, enableMouse, editTable,parent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the targetTree
	 */
	public MartConfigTree getTargetTree() {
		return targetTree;
	}

	/**
	 * @param targetTree the targetTree to set
	 */
	public void setTargetTree(MartConfigTree targetTree) {
		this.targetTree = targetTree;
	}

	@Deprecated
	/*
	 * should use EventBus
	 */
	public void refreshTargetTree(){
		if(this.targetTree == null)
			return;
		McTreeNode root = (McTreeNode)this.targetTree.getModel().getRoot();
		root.synchronizeNode();
		this.targetTree.getModel().nodeStructureChanged(root);
		this.targetTree.getModel().reload(root);
	}
}
