package org.biomart.configurator.component;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.biomart.objects.objects.PartitionTable;

public class PartitionTreeNode extends DefaultMutableTreeNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private PartitionTable partitionTable;
	private List<String> item;
	
	public PartitionTreeNode(PartitionTable pt) {
		super(pt);
		this.partitionTable = pt;
	}
	
	public PartitionTreeNode(List<String> item) {
		super(null);
		this.item = item;
	}
	
	public List<String> getItem() {
		return this.item;
	}
	
	@Override
	public String toString() {
		if(this.partitionTable!=null)
			return this.partitionTable.getName();
		else
			return this.item.get(0);
	}
	
	public PartitionTable getPartitionTable() {
		return this.partitionTable;
	}
}