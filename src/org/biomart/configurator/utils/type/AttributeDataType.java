package org.biomart.configurator.utils.type;

public enum AttributeDataType {
	STRING,
	INTEGER,
	FLOAT,
	BOOLEAN;
	
	public static AttributeDataType valueFrom(String value) {
		try {
			AttributeDataType adt = AttributeDataType.valueOf(value);
			return adt;
		} catch(IllegalArgumentException e) {
			return AttributeDataType.STRING;
		}
	}
}