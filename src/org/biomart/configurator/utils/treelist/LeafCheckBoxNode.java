package org.biomart.configurator.utils.treelist;

import java.util.ArrayList;
import java.util.List;

public class LeafCheckBoxNode  {
	
	protected String text;
	private boolean selected;
	protected Object object;
	private boolean enabled = true;
	
	/*
	 * for sub checklist
	 */
	protected List<LeafCheckBoxNode> tableList;
	
	public LeafCheckBoxNode(String text, boolean selected) {
		this.text = text;
		this.selected = selected;
		this.tableList = new ArrayList<LeafCheckBoxNode>();
	}
	
	public String getText() {
		return text;
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean newValue) {
		selected = newValue;
	}

	public void setText(String newValue) {
		text = newValue;
	}
	
	public String toString() {
		return this.text;
	}
	
	public void setUserObject(Object o) {
		this.object = o;
	}
  
	public Object getUserObject() {
		return this.object;
	}

	  public void clearTables() {
		  this.tableList.clear();
	  }
	  
	  public boolean hasSubNodes() {
		  if(this.tableList.size()>0)
			  return true;
		  else 
			  return false;
	  }
	  
	  public void addTable(LeafCheckBoxNode node) {
		  this.tableList.add(node);
	  }
	  
	  public void addTables(List<LeafCheckBoxNode> nodes) {
		  this.tableList = null;
		  this.tableList = nodes;
	  }

	  public List<LeafCheckBoxNode> getSubNodes() {
		  return this.tableList;
	  }
	  
	  /**
	   * if all = false, only selected tables return
	   * @param all
	   * @return
	   */
	  public List<String> getTable(boolean all) {
		  List<String> sl = new ArrayList<String>();
		  for(LeafCheckBoxNode node:this.tableList) {
			  if(all)
				  sl.add(node.getText());
			  else if(node.isSelected())
				  sl.add(node.getText());
		  }
		  return sl;
	  }

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}


}