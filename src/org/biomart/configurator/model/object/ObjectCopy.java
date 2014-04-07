package org.biomart.configurator.model.object;

import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.objects.objects.MartConfiguratorObject;

public class ObjectCopy {
	private int type;
	private McTreeNode node;
	
	public ObjectCopy(McTreeNode treeNode, int type) {
		this.node = treeNode;
		this.type = type;
	}
	
	/**
	 * copy or paste
	 * @return
	 */
	public boolean isCopy() {
		return (type==0);
	}
	
	public MartConfiguratorObject getObject() {
		return this.node.getObject();
	}
	
	public McTreeNode getSourceNode() {
		return this.node;
	}
}