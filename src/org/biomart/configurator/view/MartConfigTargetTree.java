/**
 * 
 */
package org.biomart.configurator.view;

import javax.swing.JDialog;

import org.biomart.configurator.jdomUtils.McTreeNode;

/**
 * @author lyao
 *	added for sync master config tree with target config tree after drag and drop
 */
public class MartConfigTargetTree extends MartConfigTree {

	//keep a reference to sourceTree when start config dialog
	private MartConfigTree sourceTree;
	
	public MartConfigTargetTree(McTreeNode root, boolean acceptDrop,
			boolean enableSelectionEvent, boolean enableMouse, boolean editTable, JDialog parent) {
		super(root, acceptDrop, enableSelectionEvent, enableMouse, editTable, parent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the sourceTree
	 */
	public MartConfigTree getSourceTree() {
		return sourceTree;
	}

	/**
	 * @param sourceTree the sourceTree to set
	 */
	public void setSourceTree(MartConfigTree sourceTree) {
		this.sourceTree = sourceTree;
	}

	@Deprecated 
	/*
	 * should use EventBus
	 */
	public void refreshSourceTree(){
		if(this.sourceTree == null)
			return;
		McTreeNode root = (McTreeNode)this.sourceTree.getModel().getRoot();
		root.synchronizeNode();
		this.sourceTree.getModel().reload(root);
	}
}
