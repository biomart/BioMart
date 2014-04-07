package org.biomart.configurator.jdomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Filter;
import org.jdom.Element;



public class McViewsFilter {

	private Map<McNodeType,Map<String, String>> filters;
	private List<String> strFilters;
	/**
	 * if true, the node will not show
	 */
	public boolean isFiltered(McTreeNode obj) { 
	    McNodeType nodeType = obj.getObject().getNodeType();
	    if(nodeType == McNodeType.ATTRIBUTE){
	    	Attribute attribute = (Attribute)obj.getObject();
	    	if(attribute.isAttributeList() && strFilters.contains("attributeList"))
	    		return true;
	    	/*else if(!attribute.hasReferenceFilters() && strFilters.contains("hasFilter"))
	    		return true;*/
	    }else if(nodeType == McNodeType.FILTER){
	    	Filter filter = (Filter)obj.getObject();
	    	if(filter.isFilterList() && strFilters.contains("filterList"))
	    		return true;
	    }
	    if(filters.containsKey(nodeType)) {
	    	Map<String, String> conditions = filters.get(nodeType);
	    	if(conditions==null)
	    		return true;
	    	else 
	    		return false;
	    }
	    return false;
	}

	/**
	 * if true, the node will show 
	 * @param node
	 * @param conditions
	 * @return
	 */
	private boolean isExclusived(Element node, Map<String, String> conditions) {
		boolean found = true;
		Iterator<Entry<String, String>> i = conditions.entrySet().iterator();
		while(i.hasNext() && found) {
			Entry<String, String> entry = i.next();
			String attribute = entry.getKey();
			String value = entry.getValue();
			//if the attribute is null, return true;
			if(node.getAttributeValue(attribute)==null)
				return true;
			//handle user
			if(attribute.equals(Resources.get("USER"))) {
				String user = node.getAttributeValue(attribute);
				String[] userArray = user.split(Resources.get("colonseparator"));
				if(!Arrays.asList(userArray).contains(value))
					found = false;
			} else if(!node.getAttributeValue(attribute).equals(value)) {
				found = false;
			}
		}
		return found;
	}

	public McViewsFilter(Map<McNodeType, Map<String, String>> filterCondition) {
		this.filters = filterCondition;
		this.strFilters = new ArrayList<String>();
	}
	
	public void addFilter(McNodeType type, Collection<String> conditions) {
		this.filters.put(type, null);
	}
	
	public void addFilter(String type) {
		this.strFilters.add(type);
	}
	
	public void removeFilter(McNodeType type) {
		this.filters.remove(type);
	}
	
	public void removeFilter(String type) {
		this.strFilters.remove(type);
	}
}
