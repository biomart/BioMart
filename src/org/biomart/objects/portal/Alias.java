package org.biomart.objects.portal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.MartConfiguratorUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.objects.Config;
import org.jdom.Element;

public class Alias extends PortalObject implements Serializable {

	private static final long serialVersionUID = -8176991179209098381L;
	
	public static final String XML_ELEMENT_NAME = "alias";
	public static final McNodeType MC_NODE_TYPE = null;
	
	private Config config = null;
	
	/*private String locationName = null;	// TODO create an object for those 5 properties? used in elements as well --> Path.java
	private String martName = null;
	private Integer version = null;*/
	private String configName = null;
	private List<String> datasetNames = null;
	
	public Alias(Config config, String name, String datasetName) {
		this(config, name, new ArrayList<String>(Arrays.asList(new String[] {datasetName})));
	}
	public Alias(Config config, String name, ArrayList<String> datasetNames) {		// all mandatory (so no setters and required by constructor)
		super(XML_ELEMENT_NAME, name);
		this.config = config;
		
		/*Dataset parentDataset = config.getParentDataset();
		OldMart parentOldMart = parentDataset.getParentMartRegistry();
		Location parentLocation = parentOldMart.getParentLocation();
		
		this.locationName = parentLocation.getName();
		this.martName = parentOldMart.getName();
		this.version = parentOldMart.getVersion();*/
		this.configName = config.getName();
		
		this.datasetNames = datasetNames;
	}
	
	/*public String getLocationName() {
		return locationName;
	}
	public String getMartName() {
		return martName;
	}
	public Integer getVersion() {
		return version;
	}*/
	public String getConfigName() {
		return configName;
	}
	public List<String> getDatasetNames() {
		return datasetNames;
	}
	
	public Config getConfig() {
		return this.config;
	}
	
	@Override
	public String toString() {
		return 
			super.toString() + ", " + 
			/*"locationName = " + locationName + ", " +
			"martName = " + martName + ", " +
			"version = " + version + ", " +*/
			"configName = " + configName + ", " +
			"datasetNames = " + datasetNames;			
	}
	
	public Element generateXml() {
		Element element = super.generateXml();
		/*MartConfiguratorUtils.addAttribute(element, "location", locationName);
		MartConfiguratorUtils.addAttribute(element, "mart", martName);
		MartConfiguratorUtils.addAttribute(element, "version", version);*/
		MartConfiguratorUtils.addAttribute(element, "config", configName);
		MartConfiguratorUtils.addAttribute(element, "datasets", datasetNames);
		return element;
	}
}
