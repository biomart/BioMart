package org.biomart.api.enums;

public enum Operation {
	SINGLESELECT ("singleselect"),
	MULTISELECT ("multiselect");
	
	private String value;
	
	private Operation(String value) {
		this.value = value;
	}
	
	public String toString() {
		return this.value;
	}
	
	public static Operation valueFrom(String value) {
		for(Operation o: Operation.values()) {
			if(o.value.equals(value))
				return o;
		}
		return null;
	}
}