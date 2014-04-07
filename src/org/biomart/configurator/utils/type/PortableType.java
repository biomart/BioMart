package org.biomart.configurator.utils.type;

public enum PortableType {
	IMPORTABLE ("importable"),
	EXPORTABLE ("exportable");
	
	private String name;
	
	PortableType(String name) {
		this.name = name;
	}
	
	public String toString() {
		return this.name;
	}
}