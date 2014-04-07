package org.biomart.configurator.utils.type;


public enum ValidationStatus {
	VALID (0),
	INVALID (100);
	
	private int code;
	
	ValidationStatus(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;
	}
}