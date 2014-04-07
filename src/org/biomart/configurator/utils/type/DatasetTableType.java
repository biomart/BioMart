package org.biomart.configurator.utils.type;

public enum DatasetTableType {
	MAIN(0),
	MAIN_SUBCLASS(1),
	DIMENSION(2);
	
	private String description;
	private int id;
	
	DatasetTableType(int id) {
		this.id = id;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public int getId() {
		return this.id;
	}
	
	public static DatasetTableType valueFrom(String value) {
		DatasetTableType dst = null;
		try {
			dst = DatasetTableType.valueOf(value);
		}catch(IllegalArgumentException e) {
			return null;
		}
		return dst;
	}
}