package org.biomart.configurator.utils;

import java.util.List;
import java.util.Map;
import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;

public class FileLinkObject {
	/**
	 * if !MartInVirtualSchema.isURL, the value is an empty list
	 */
	private Map<MartInVirtualSchema, List<DatasetFromUrl>> dsInfoMap;	

	public void setDsInfoMap(Map<MartInVirtualSchema, List<DatasetFromUrl>> value) {
		this.dsInfoMap = value;
	}
	
	public Map<MartInVirtualSchema, List<DatasetFromUrl>> getDsInfoMap() {
		return this.dsInfoMap;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	private String fileName;
}