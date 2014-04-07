package org.biomart.configurator.utils.type;

/**
 * assuming that the global operation is only for attribute and filter
 * may be replaced by reflection
 * @author yliang
 *
 */
public enum GlobalOperationAttribute {
	DISPLAYNAME("displayName"),
	HIDE("hide");
	
	private String name;
	private GlobalOperationAttribute(String value) {
		this.name = value;
	}
	
	public String toString() {
		return this.name;
	}
}