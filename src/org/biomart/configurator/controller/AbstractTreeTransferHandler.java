package org.biomart.configurator.controller;
import java.awt.*;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.jdomUtils.McTreeModel;
import org.biomart.configurator.jdomUtils.McTreeNode;
 
public abstract class AbstractTreeTransferHandler extends TransferHandler implements 
	DragGestureListener, DragSourceListener{
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	protected MartConfigTree tree;
	
	//private DragSource dragSource; // dragsource
	//private DropTarget dropTarget; //droptarget
	private List<McTreeNode> draggedNodes = new ArrayList<McTreeNode>(); 
	private List<McTreeNode> draggedNodeParents = new ArrayList<McTreeNode>();
 
	protected AbstractTreeTransferHandler(MartConfigTree tree, int action) {
		this.tree = tree;
		//this.dragSource = new DragSource();
		//this.dragSource.createDefaultDragGestureRecognizer(tree, action, this);
		//this.dropTarget = new DropTarget(tree,action,this);
	}
 
	/* Methods for DragSourceListener */
	public void dragDropEnd(DragSourceDropEvent dsde) {
		if (dsde.getDropSuccess() && dsde.getDropAction()==DnDConstants.ACTION_MOVE && draggedNodeParents != null ) {
			for(McTreeNode node: draggedNodeParents)
				((McTreeModel)tree.getModel()).nodeStructureChanged(node);
		}
	}
	public final void dragEnter(DragSourceDragEvent dsde)  {
		int action = dsde.getDropAction();
		
		
		if (action == DnDConstants.ACTION_COPY)  {
			dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
		} 
		else {
			if (action == DnDConstants.ACTION_MOVE) {
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
			} 
			else {
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
			}
		}
	}
	public final void dragOver(DragSourceDragEvent dsde) {
		int action = dsde.getDropAction();
		if (action == DnDConstants.ACTION_COPY) {
			dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
		} 
		else  {
			if (action == DnDConstants.ACTION_MOVE) {
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
			} 
			else  {
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
			}
		}
	}
	public final void dropActionChanged(DragSourceDragEvent dsde)  {
		int action = dsde.getDropAction();
		if (action == DnDConstants.ACTION_COPY) {
			dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
		}
		else  {
			if (action == DnDConstants.ACTION_MOVE) {
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
			} 
			else {
				dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
			}
		}
	}
	public final void dragExit(DragSourceEvent dse) {
	   dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
	}	
		
	/* Methods for DragGestureListener */
	public final void dragGestureRecognized(DragGestureEvent dge) {
		TreePath[] paths = tree.getSelectionPaths();
		if (paths != null && paths.length!=0) {
			//draggedNodes = new ArrayList<McTreeNode>();
			//draggedNodeParents = new ArrayList<McTreeNode>();
			draggedNodes.clear();
			draggedNodeParents.clear();
			for(TreePath path: paths) {
				McTreeNode node = (McTreeNode)path.getLastPathComponent();
				draggedNodes.add(node);
				draggedNodeParents.add((McTreeNode)node.getParent());
			}
			//dragSource.startDrag(dge, DragSource.DefaultMoveNoDrop, new TransferableNode(draggedNodes), this);			
		}	 
	}
	
 

	public abstract boolean canPerformAction(MartConfigTree target, List<McTreeNode> draggedNodes, int action, Point location);
 
	public abstract boolean executeDrop(MartConfigTree tree, List<McTreeNode> draggedNode, McTreeNode newParentNode, int action);
	
	public abstract boolean executeInsert(MartConfigTree tree, List<McTreeNode> draggedNode, McTreeNode newParentNode, int action,int childIndex);

	/* (non-Javadoc)
	 * @see javax.swing.TransferHandler#canImport(javax.swing.TransferHandler.TransferSupport)
	 */
	@Override
	public boolean canImport(TransferSupport support) {
		// TODO Auto-generated method stub
		//return super.canImport(support);
		
		if(support.isDrop())
		{
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.TransferHandler#importData(javax.swing.TransferHandler.TransferSupport)
	 */
	@Override
	public boolean importData(TransferSupport support) {
		// TODO Auto-generated method stub
		Transferable transferable = support.getTransferable();
		JTree.DropLocation location = (JTree.DropLocation)support.getDropLocation();
		/*int dropIndex = location.getChildIndex();
		TreePath dropPath = location.getPath();
		McTreeModel model = (McTreeModel)this.tree.getModel();*/
		
		int childIndex = location.getChildIndex();
		//Point pt = location.getDropPoint();
		int action = support.getDropAction();
		try {
			Object obj = transferable.getTransferData(TransferableNode.NODE_FLAVOR);
			if(obj != null) {
						
				if (transferable.isDataFlavorSupported(TransferableNode.NODE_FLAVOR) 
						/*&& canPerformAction(tree, (List<McTreeNode>) obj, action, pt)*/) {
					
					/*TreePath pathTarget = tree.getPathForLocation(pt.x, pt.y);
					if(pathTarget == null)
						return false;*/
					//obj is a list of McTreeNode
					List<McTreeNode> nodes = (List<McTreeNode>) obj;
					TreePath pathTarget = location.getPath();
					if(pathTarget == null)
						return false;
					McTreeNode newParentNode =(McTreeNode)pathTarget.getLastPathComponent();
					// if child index equals -1 which means it is a move on to a tree node operation
					if(childIndex == -1){
						if (newParentNode != null && executeDrop(tree, nodes, newParentNode, action)) {
							return true;
						}
					}
					//if child index != -1, means it is a insertion operation
					else {
						if (newParentNode != null && executeInsert(tree, nodes, newParentNode, action,childIndex)) {
							return true;
						}
					}
				}
			}
		} catch (UnsupportedFlavorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}

	/**
	 * @return the draggedNodes
	 */
	public List<McTreeNode> getDraggedNodes() {
		return draggedNodes;
	}

	/**
	 * @return the draggedNodeParents
	 */
	public List<McTreeNode> getDraggedNodeParents() {
		return draggedNodeParents;
	}

	/* (non-Javadoc)
	 * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
	 */
	@Override
	protected Transferable createTransferable(JComponent arg0) {
		TreePath[] paths = tree.getSelectionPaths();
		if (paths != null && paths.length!=0) {
			//draggedNodes = new ArrayList<McTreeNode>();
			//draggedNodeParents = new ArrayList<McTreeNode>();
			draggedNodes.clear();
			draggedNodeParents.clear();
			//reversely add tree path to draggedNodes
			for(TreePath path: paths) {
				McTreeNode node = (McTreeNode)path.getLastPathComponent();
				draggedNodes.add(node);
				draggedNodeParents.add((McTreeNode)node.getParent());
			}
		}
		return  new TransferableNode(draggedNodes);
	}

	/* (non-Javadoc)
	 * @see javax.swing.TransferHandler#exportDone(javax.swing.JComponent, java.awt.datatransfer.Transferable, int)
	 */
	@Override
	protected void exportDone(JComponent c, Transferable t, int action) {
		
	}

	/* (non-Javadoc)
	 * @see javax.swing.TransferHandler#getSourceActions(javax.swing.JComponent)
	 */
	@Override
	public int getSourceActions(JComponent arg0) {		
		return COPY_OR_MOVE;
	}

	/**
	 * @return the tree
	 */
	public MartConfigTree getTree() {
		return tree;
	}

	/**
	 * @param tree the tree to set
	 */
	public void setTree(MartConfigTree tree) {
		this.tree = tree;
	}
}
