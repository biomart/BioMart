package org.biomart.configurator.model.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biomart.common.utils.AlphanumComparator;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Filter;
import org.jdom.Element;


public class FilterData implements Comparable<FilterData> {
	private String value;
	private String displayName;
	private boolean isSelected;
	private Map<String,List<FilterData>> pushedFilters;
	
	public FilterData(String value, String displayName, boolean isSelected) {
		this.value = value;
		this.displayName = displayName;
		this.isSelected = isSelected;
	}
	
	
	
	public String getValue() {
		return value;
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public boolean isSelected() {
		return isSelected;
	}
	
	public int hashCode() {
		return this.value.hashCode();
	}
	
	public boolean equals(Object object) {
		if(this == object)
			return true;
		if(!(object instanceof FilterData))
			return false;
		else {
			return ((FilterData)object).getValue().equals(this.getValue());
		}
	}

	public String toString() {
		return this.displayName;
	}

	public String toSavedFormat() {
		String tmpname = this.value;
		String tmpdisplayname = this.displayName;
		if(this.value.indexOf("|")>=0) {
			tmpname = tmpname.replaceAll("\\|", "\\\\|");
		}
		if(this.displayName.indexOf("|")>=0) {
			tmpdisplayname = tmpdisplayname.replaceAll("\\|", "\\\\|");
		}
		return tmpname+"|"+tmpdisplayname+"|"+Boolean.toString(isSelected);
	}


	public int compareTo(FilterData arg0) {
		AlphanumComparator alphacomp = new AlphanumComparator();
		return alphacomp.compare(this.getValue(), arg0.getValue());
	}

	public Element generateXml() {
		Element element = new Element(XMLElements.ROW.toString());
		element.setAttribute(XMLElements.VALUE.toString(), this.value);
		element.setAttribute(XMLElements.DISPLAYNAME.toString(), this.displayName);
		element.setAttribute(XMLElements.DEFAULT.toString(), Boolean.toString(this.isSelected));
		if(null!=this.pushedFilters) {
			for(Map.Entry<String, List<FilterData>> entry: this.pushedFilters.entrySet()) {
				Element filterElement = new Element(XMLElements.FILTER.toString());
				filterElement.setAttribute(XMLElements.NAME.toString(), entry.getKey());
				element.addContent(filterElement);
				for(FilterData fd: entry.getValue()) {
					Element rowElement = new Element(XMLElements.ROW.toString());
					rowElement.setAttribute(XMLElements.VALUE.toString(), fd.getValue());
					rowElement.setAttribute(XMLElements.DISPLAYNAME.toString(), fd.getDisplayName());
					rowElement.setAttribute(XMLElements.DEFAULT.toString(), Boolean.toString(fd.isSelected));
					filterElement.addContent(rowElement);
				}
			}
		}
		return element;
	}
	
	
	public List<FilterData> getPushFilterOptions(String filter) {
		if(null==this.pushedFilters)
			this.pushedFilters = new HashMap<String,List<FilterData>>();
		return this.pushedFilters.get(filter);
	}
	
	public void addPushFilterOptions(String filter, List<FilterData> subFds) {
		if(null==this.pushedFilters)
			this.pushedFilters = new HashMap<String,List<FilterData>>();
		this.pushedFilters.put(filter, subFds);
	}
	
	public void addPushFilterOptions(String filter, FilterData subFd) {
		if(null==this.pushedFilters)
			this.pushedFilters = new HashMap<String,List<FilterData>>();
		List<FilterData> fds = this.pushedFilters.get(filter);
		if(null==fds) {
			fds = new ArrayList<FilterData>();
			this.pushedFilters.put(filter, fds);
		}
		fds.add(subFd);
	}
	
	
}