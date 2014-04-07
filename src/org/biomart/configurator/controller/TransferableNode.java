package org.biomart.configurator.controller;
import java.awt.datatransfer.*;
import javax.swing.tree.*;

import org.biomart.configurator.jdomUtils.McTreeNode;

import java.util.*;
 
public class TransferableNode implements Transferable {
	public static final DataFlavor NODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "Node");
	private List<McTreeNode> nodeList;
	private DataFlavor[] flavors = { NODE_FLAVOR };
 
	public TransferableNode(List<McTreeNode> ndlist) {
		nodeList = ndlist;
	}  
 
	public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor == NODE_FLAVOR) {
			return nodeList;
		}
		else {
			throw new UnsupportedFlavorException(flavor);	
		}			
	}
 
	public DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}
 
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return Arrays.asList(flavors).contains(flavor);
	}
}
