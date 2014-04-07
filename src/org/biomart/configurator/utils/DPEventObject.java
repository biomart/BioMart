package org.biomart.configurator.utils;

import org.biomart.configurator.utils.type.EventType;

public class DPEventObject extends McEventObject {

	private String dataSetName;
	
	public DPEventObject(EventType type, Object obj, String datasetName) {
		super(type, obj);
		this.dataSetName = datasetName;
	}
	
	public String getDataSetName() {
		return this.dataSetName;
	}
	
}