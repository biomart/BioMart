package org.biomart.objects.enums;



public enum FilterDisplayType {
		
	SIMPLE			("simple"),
	TEXTFIELD		("textfield"),	
	BOOLEAN			("boolean"),
	LIST			("list"),
	TREE			("tree"),
	BOOLEANLIST 	("booleanList"),
	GROUP			("group");
	
	private String value = null;
	private FilterDisplayType() {
		this(null);
	}
	private FilterDisplayType(String value) {
		this.value = value;
	}
	public String getValue() {
		return this.value;
	}
	public static FilterDisplayType fromValue(String value) {
		for (FilterDisplayType filterDisplayType : values()) {
			if (filterDisplayType.value.equals(value)) {
				return filterDisplayType;
			}
		}
		return null;
	}
	
	public static boolean isTextfield(String value) {
		return FilterDisplayType.TEXTFIELD.getValue().equals(value);
	}
	public static boolean isBoolean(String value) {
		return FilterDisplayType.BOOLEAN.getValue().equals(value);
	}
	public static boolean isList(String value) {
		return FilterDisplayType.LIST.getValue().equals(value);
	}
	public static boolean isTree(String value) {
		return FilterDisplayType.TREE.getValue().equals(value);
	}
	public static boolean isGroup(String value) {
		return FilterDisplayType.GROUP.getValue().equals(value);
	}
}
