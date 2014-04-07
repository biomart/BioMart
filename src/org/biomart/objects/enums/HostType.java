package org.biomart.objects.enums;

public enum HostType {
	URL		("url"),
	RDBMS	("rdbms");
	
	private String xmlValue = null;
	private HostType(String xmlValue) {
		this.xmlValue = xmlValue;
	}
	public String getXmlValue() {
		return xmlValue;
	}
}
