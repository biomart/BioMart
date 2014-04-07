package org.biomart.objects.enums;

public enum RelationType {
	ONE_TO_ONE	("1:1"),
	ONE_TO_MANY	("1:M"),
	MANY_TO_ONE	("M:1");
	
	private String xmlValue = null;
	private RelationType(String xmlValue) {
		this.xmlValue = xmlValue;
	}
	public String getXmlValue() {
		return xmlValue;
	}
}
