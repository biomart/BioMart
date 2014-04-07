package org.biomart.configurator.view.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.biomart.objects.objects.PartitionTable;

public class PartitionList extends JList implements ListSelectionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PartitionTable partitionTable;
//	private Set<String> selectedParentItems;
	
	public PartitionList(DefaultListModel model) {
		super(model);
//		this.selectedParentItems = new HashSet<String>();
	}
	
	public void setPartitionTable(PartitionTable ptable) {
		this.partitionTable = ptable;
	}

	public PartitionTable getPartitionTable() {
		return this.partitionTable;
	}

	public void valueChanged(ListSelectionEvent event) {
		 boolean isAdjusting = event.getValueIsAdjusting();
		 if(!isAdjusting && this.partitionTable!=null) {
			 Object object = event.getSource();
			 if(object instanceof PartitionList) {
				 Map<String, Set<String>> tmpMap = new HashMap<String, Set<String>>();
				 PartitionList pl = (PartitionList) object;
				 Object selectionValues[] = pl.getSelectedValues();
				 
				 for(Object selectedValue: selectionValues) {
					 String valueStr = (String)selectedValue;
					 Set<String> values = new HashSet<String>();
					 for(List<String> rows: this.partitionTable.getTable()) {
						 if(rows.get(0).equals(valueStr)) 
							 values.add(rows.get(1));
					 }
					 tmpMap.put(valueStr, values);
				 }
				 //get the intersection values
				 int i=0;
				 Set<String> result = null;
				 for(Set<String> setValues: tmpMap.values()) {
					 if(i==0) {
						 result = setValues;
						 i++;
					 }
					 else {
						 result = this.intersection(result, setValues);
					 }
				 }
				 //update list;
				 ((DefaultListModel)this.getModel()).clear();
				 for(String item: result)
					 ((DefaultListModel)this.getModel()).addElement(item);
			 }
		 }
	}
	
	private <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new HashSet<T>();
		for(T t: setA) {
			if(setB.contains(t))
				tmp.add(t);
		}
		return tmp;
	}
}