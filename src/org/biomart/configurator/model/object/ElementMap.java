package org.biomart.configurator.model.object;

import java.util.HashMap;
import java.util.Map;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Mart;

public class ElementMap {
	
	private Map<Mart, Map<Config,Map<String,Element>>> elementMap;
	
	public ElementMap() {
		this.elementMap = new HashMap<Mart,Map<Config,Map<String,Element>>>();
	}
	
	public Element getElement(Mart mart, Config config, String name) {
		Map<Config, Map<String,Element>> configMap = this.elementMap.get(mart);
		if(configMap == null)
			return null;
		Map<String,Element> eMap = configMap.get(config);
		if(eMap == null)
			return null;
		return eMap.get(name);
	}
}