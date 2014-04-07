package org.biomart.configurator.utils.treelist;

import java.util.ArrayList;
import java.util.List;

import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;

public class FileCheckBoxNode extends LeafCheckBoxNode {




	public FileCheckBoxNode(MartInVirtualSchema martV, boolean selected) {
		super(martV.getName(), selected);
		this.setUserObject(martV);
	}
	
	@Override
	public String toString() {
		String type = ((MartInVirtualSchema)this.getUserObject()).isURLMart()?" (url)":" (db)";
		return this.text + type;
	}

	  /**
	   * if all = false, only selected tables return
	   * @param all
	   * @return
	   */
	  public List<DatasetFromUrl> getDatasetsForUrl(boolean all) {
		  List<DatasetFromUrl> sl = new ArrayList<DatasetFromUrl>();
		  //if(((MartInVirtualSchema)this.getUserObject()).isURLMart()) {
			  for(LeafCheckBoxNode node:this.tableList) {
				  if(all)
					  sl.add((DatasetFromUrl)node.getUserObject());
				  else if(node.isSelected())
					  sl.add((DatasetFromUrl)node.getUserObject());
			  }
		  //}
		  return sl;
	  }
}