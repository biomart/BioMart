package org.biomart.configurator.view.component;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JComboBox;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.objects.objects.PartitionTable;

public class PartitionDropDown extends JComboBox implements ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final PartitionTable partitionTable;
	
	public PartitionDropDown(final PartitionTable pTable) {
		this.partitionTable = pTable;
		if(pTable.getPartitionType().equals(PartitionType.SCHEMA))
			init();
		else
			this.setEnabled(false);
	}
	
	private void init() {
		this.addItem(Resources.get("martTabAllPartitions"));
		for(List<String> rows : this.partitionTable.getTable()) {
			this.addItem(rows.get(PartitionUtils.DATASETNAME));
		}	
	}

	public void itemStateChanged(ItemEvent event) {
		if (event.getStateChange() == ItemEvent.SELECTED) {
			Object object = event.getSource();
			if(object instanceof PartitionDropDown) {
				int index = ((PartitionDropDown) object).getSelectedIndex();
				if(index>0) {
					this.setEnabled(true);
					//reload the items according to the selected value
					this.reloadItems(index-1);
				} else
					this.setEnabled(false);
			}	
		}
	}
	
	private void reloadItems(int index) {
		this.removeAllItems();
		String item = ""+index;
		this.addItem(Resources.get("martTabAllPartitions"));
		for(List<String> rows: this.partitionTable.getTable()) {
			if(rows.get(0).equals(item)) 
				this.addItem(rows.get(1));
		}
	}
	
	public PartitionTable getPartitionTable() {
		return this.partitionTable;
	}
}