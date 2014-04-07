package org.biomart.configurator.model.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import org.biomart.configurator.utils.type.PartitionColumnType;
import org.biomart.objects.objects.PartitionTable;

public class PartitionColumn extends Observable {
	private String header;
	private List<String> items;
	private PartitionTable parentPT;
	private PartitionColumnType type; //0 fixed, 1 customized

	public PartitionColumn(PartitionTable table, String header, PartitionColumnType type) {
		this.parentPT = table;
		this.header = header;
		this.type = type;
		this.items = new ArrayList<String>();
	}
	
	public void setHeader(String header) {
		this.header = header;
	}
	
	public String getHeader() {
		return header;
	}
	
	public boolean isCustomizedColumn() {
		return type==PartitionColumnType.CUSTOMIZED;
	}
	
	public PartitionTable getPartitionTable() {
		return this.parentPT;
	}
	
	public String getValue(int row) {
		return this.items.get(row);
	}
	
	public int size() {
		return this.items.size();
	}
	
	public void addNewRow(String value) {
		this.items.add(value);
	}
	
	public void setRowValue(int index, String value) {
		this.items.set(index, value);
	}
	
	public List<String> getColumnList() {
		return this.items;
	}

	public void swap(int i, int j) {
		Collections.swap(this.items, i, j);
	}
	
	public void addRow(int row, String value) {
		this.items.add(row, value);
	}
	
	public String removeRow(int row) {
		return this.items.remove(row);
	}
}