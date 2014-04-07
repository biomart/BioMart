package org.biomart.objects.enums;

public enum FilterOperation {
	AND ("and"),
	OR ("or");
	
	private String value;
	
	private FilterOperation(String value) {
		this.value = value;
	}
	
	public static FilterOperation valueFrom(String value) {
		for(FilterOperation fo: FilterOperation.values()) {
			if(fo.value.equals(value))
				return fo;
		}
		//default and
		return FilterOperation.AND;
	}
	
	public String toString() {
		return this.value;
	}
}