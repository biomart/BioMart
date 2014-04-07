package org.biomart.configurator.model;

public class PTCellObject {
	private String value;
	private String partitionTableName;
	private String row;
	private String col;
	

	public String toString() {
		return this.value;
	}
	
	public PTCellObject(String value, String ptName, String row, String col) {
		this.value = value;
		this.partitionTableName = ptName;
		this.row = row;
		this.col = col;
	}
	
	public String getTableName() {
		return this.partitionTableName;
	}
	
	public String getRow() {
		return this.row;
	}
	
	public String getColumn() {
		return this.col;
	}
}