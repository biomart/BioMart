package org.biomart.backwardCompatibility;

public class DatasetFromUrl {
	private String name;
	private String displayName;
	private boolean visible;
	private String url;
	private String virtualSchema;
	private boolean sequence;
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	public boolean isVisible() {
		return visible;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	public void setVirtualSchema(String virtualSchema) {
		this.virtualSchema = virtualSchema;
	}
	public String getVirtualSchema() {
		return virtualSchema;
	}
	public void setSequence(boolean sequence) {
		this.sequence = sequence;
	}
	public boolean isSequence() {
		return sequence;
	}
	
	
}