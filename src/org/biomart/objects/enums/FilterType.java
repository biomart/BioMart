package org.biomart.objects.enums;

public enum FilterType {
	TEXT ("text"),
	BOOLEAN ("boolean"),
	UPLOAD ("upload"),
	SINGLESELECT ("singleSelect"),
	MULTISELECT ("multiSelect"),
	SINGLESELECTBOOLEAN ("singleSelectBoolean"),
	MULTISELECTBOOLEAN ("multiSelectBoolean"),
	SINGLESELECTUPLOAD ("singleSelectUpload"),
	MULTISELECTUPLOAD ("multiSelectUpload");
	
	private String value;
	
	private FilterType(String value) {
		this.value = value;
	}
	
	public String toString() {
		return this.value;
	}
	
	public static FilterType valueFrom(String value) {
		for(FilterType type: FilterType.values()) {
			if(type.value.equalsIgnoreCase(value)) {
				return type;
			}
		}
		//default return text
		return FilterType.TEXT;
	}
}