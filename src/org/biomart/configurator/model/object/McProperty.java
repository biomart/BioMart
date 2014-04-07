package org.biomart.configurator.model.object;


public class McProperty {

	private String value;
	

	public McProperty(String value) {
		this.value = value;
	}
	
	
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		if(value == this.value) 
			return;
		if(value==null)
			this.value = "";
		else
			this.value = value;
	}
	

	public String toString() {
		return this.value;
	}

}