package org.biomart.configurator.model.object;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;

public class GroupPartitionTable extends PartitionTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//should use treenode
	private DefaultMutableTreeNode top;
	private LinkedHashMap<PartitionTable,Integer> ptMap;
	
	public GroupPartitionTable(Mart ds, PartitionType type) {
		super(ds, type);	
		top = new DefaultMutableTreeNode(this.getName());
		ptMap = new LinkedHashMap<PartitionTable, Integer>();
	}
	
	public void setGroupValue(PartitionTable p1, PartitionTable p2, String col1, String col2) {
		//parse col number
		String[] colA = col1.split(" ");
		int colInt1 = Integer.parseInt(colA[1]);
		String[] colB = col2.split(" ");
		int colInt2 = Integer.parseInt(colB[1]);
		top.removeAllChildren();
		
        DefaultMutableTreeNode p1Node = null;
        DefaultMutableTreeNode p2Node = null;

        for(String str: p1.getCol(colInt1)) {
        	p1Node = new DefaultMutableTreeNode(str);
        	top.add(p1Node);
        	for(String p2value: p2.getCol(colInt2)) {
        		p2Node = new DefaultMutableTreeNode(p2value);
        		p1Node.add(p2Node);
        	}
        }
		ptMap.clear();
		ptMap.put(p1,colInt1);
		ptMap.put(p2,colInt2);
	}
	
	public DefaultMutableTreeNode getTreeNode() {
		return this.top;
	}
	
	public Map<PartitionTable,Integer> getOriginalPartitionMap() {
		return this.ptMap;
	}
	
}