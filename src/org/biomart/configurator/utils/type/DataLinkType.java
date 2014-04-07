package org.biomart.configurator.utils.type;

public enum DataLinkType {
	RDBMS ("RDBMS"), //just for display in the GUI, default equals to SOURCE
	SOURCE ("Source Schema"),
	TARGET ("Relational Mart"),
	URL ("URL"),
	FILE ("Registry File"),
	FAKE ("Fake");
	
	private String description;
	
	DataLinkType(String des) {
		this.description = des;
	}
	
	public String toString() {
		return this.description;
	}
	
	public static DataLinkType getEnumFromValue(String value) {
		for(DataLinkType dlt: DataLinkType.values()) {
			if(dlt.description.equals(value))
				return dlt;
		}
		return null;
	}
}
