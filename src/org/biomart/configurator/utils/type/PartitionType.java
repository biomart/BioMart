package org.biomart.configurator.utils.type;

public enum PartitionType {
	SCHEMA (0),
	DATASET (1),
	DIMENSION (2),
	GROUP (0);
	
	private int level;
	
	PartitionType(int level) {
		this.level = level;
	}
	
	public int getLevel() {
		return this.level;
	}
	
	public static PartitionType fromValue(String value) {
		PartitionType type = null;
		try {
			type = PartitionType.valueOf(value);
		}catch(IllegalArgumentException e) {
			return null;
		}
		return type;
	}
}