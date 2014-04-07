package org.biomart.objects.enums;



public enum EntryLayout {
	LEFT_VERTICAL		("leftVertical"),	
	RIGHT_VERTICAL		("rightVertical"),
	CENTRE_VERTICAL		("centreVertical"),
	LEFT_HORIZONTAL		("leftHorizontal"),
	RIGHT_HORIZONTAL	("rightHorizontal"),
	CENTRE_HORIZONTAL	("centreHorizontal"),
	GRID				("grid"),
	SEARCH				("search"),
	DROPDOWN			("dropdown");
	
	private String value;
	private EntryLayout(String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return this.value;
	}
	
	public static EntryLayout valueFrom (String value) {
		for(EntryLayout clt: EntryLayout.values()) {
			if(clt.value.equals(value)) {
				return clt;
			}
		}
		return null;
	}
}
