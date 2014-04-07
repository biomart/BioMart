package org.biomart.common.utils;


import java.util.Collection;



import org.biomart.common.constants.MartConfiguratorConstants;

import org.biomart.common.utils2.MyUtils;


import org.jdom.Element;

public class MartConfiguratorUtils {

	
	
	public static void addAttribute(Element element, String attributeName, Object attributeValue) {
		if (attributeValue!=null) {
			element.setAttribute(attributeName, attributeValue.toString());
		}
	}
	
	public static void addAttribute(Element element, String attributeName, String attributeValue) {
		if (attributeValue!=null && !MyUtils.isEmpty(attributeValue)) {
			element.setAttribute(attributeName, attributeValue);
		}
	}

	public static void addAttribute(Element element, String attributeName, Boolean attributeValue) {
		if (attributeValue!=null) {
			element.setAttribute(attributeName, String.valueOf(attributeValue));
		}
	}

	public static void addAttribute(Element element, String attributeName, Integer attributeValue) {
		if (attributeValue!=null) {
			element.setAttribute(attributeName, String.valueOf(attributeValue));
		}
	}


	public static void addAttribute(Element element, String attributeName, Collection<? extends Object> attributeValues) {
		if (attributeValues!=null && !attributeValues.isEmpty()) {
			element.setAttribute(attributeName, 
					MartConfiguratorUtils.collectionToString(attributeValues, MartConfiguratorConstants.LIST_ELEMENT_SEPARATOR));
		}
	}
	


	public static<T> String collectionToString(Collection<? extends T> c, String separator) {
		StringBuffer stringBuffer = new StringBuffer();
		int i=0;
		if (c!=null) {
			for (T t : c) {
				stringBuffer.append((i==0 ? "" : separator) + t);
				i++;
			}
		}
		return stringBuffer.toString();
	}
	

}
