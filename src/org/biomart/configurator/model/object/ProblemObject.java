package org.biomart.configurator.model.object;

import org.biomart.configurator.jdomUtils.McTreeNode;

public class ProblemObject {
	private String description;
	private McTreeNode treeNode;
	
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setTreeNode(McTreeNode treeNode) {
		this.treeNode = treeNode;
	}
	public McTreeNode getTreeNode() {
		return treeNode;
	}
	
	
}