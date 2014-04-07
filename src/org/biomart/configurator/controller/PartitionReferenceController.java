package org.biomart.configurator.controller;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.McProperty;
import org.biomart.configurator.model.object.PartitionColumn;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.view.menu.AttributeTableConfig;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.PartitionTable;

public class PartitionReferenceController {
    public List<MartConfiguratorObject> searchContainerPtReferences(Container container, String type, String value, 
    		boolean caseSensitive, boolean like) {
    	List<MartConfiguratorObject> result = new ArrayList<MartConfiguratorObject>();
    	org.jdom.Element conElement = AttributeTableConfig.getInstance().getElementByName(container.getNodeType().toString());
    	List<org.jdom.Element> eList = conElement.getChildren("item");
		for(org.jdom.Element e: eList) {
			if(!"1".equals(e.getAttributeValue("visible")))
				continue;
			XMLElements xType = XMLElements.valueFrom(e.getAttributeValue(XMLElements.NAME.toString()));
			if(isObjectMatch(container,type,value,xType,true,true)) {
				result.add(container);
			}
		}
    	
		org.jdom.Element attElement = AttributeTableConfig.getInstance().getElementByName(container.getNodeType().toString());
		List<org.jdom.Element> aeList = attElement.getChildren("item");
    	for(Attribute attribute: container.getAttributeList()) {
    		for(org.jdom.Element ae: aeList) {
    			if(!"1".equals(ae.getAttributeValue("visible")))
    				continue;
    			XMLElements xType = XMLElements.valueFrom(ae.getAttributeValue(XMLElements.NAME.toString()));
    	   		if(isObjectMatch(attribute,type,value,xType, caseSensitive, like))
        			result.add(attribute);
    		}
     	}
    	
		org.jdom.Element filterElement = AttributeTableConfig.getInstance().getElementByName(container.getNodeType().toString());
		List<org.jdom.Element> feList = attElement.getChildren("item");    	
    	for(Filter filter: container.getFilterList()) {
    		for(org.jdom.Element fe: feList) {
    			if(!"1".equals(fe.getAttributeValue("visible")))
    				continue;
    			XMLElements xType = XMLElements.valueFrom(fe.getAttributeValue(XMLElements.NAME.toString()));
        		if(isObjectMatch(filter,type,value,xType, caseSensitive, like))
        			result.add(filter);
    		}
    	}
    	
    	
    	for(Container subContainer: container.getContainerList()) {
    		result.addAll(searchContainerPtReferences(subContainer, type, value, caseSensitive, like));
    	}
    	return result;
    }

    public List<McProperty> getContainerPtRefProperties(Container container, String type, String value, 
    		boolean caseSensitive, boolean like) {
    	List<McProperty> result = new ArrayList<McProperty>();
    	org.jdom.Element conElement = AttributeTableConfig.getInstance().getElementByName(container.getNodeType().toString());
    	List<org.jdom.Element> eList = conElement.getChildren("item");
		for(org.jdom.Element e: eList) {
			if(!"1".equals(e.getAttributeValue("visible")))
				continue;
			XMLElements xType = XMLElements.valueFrom(e.getAttributeValue(XMLElements.NAME.toString()));
			if(isObjectMatch(container,type,value,xType,true,true)) {
				result.add(container.getProperty(xType));
			}
		}
    	
		org.jdom.Element attElement = AttributeTableConfig.getInstance().getElementByName(container.getNodeType().toString());
		List<org.jdom.Element> aeList = attElement.getChildren("item");
    	for(Attribute attribute: container.getAttributeList()) {
    		for(org.jdom.Element ae: aeList) {
    			if(!"1".equals(ae.getAttributeValue("visible")))
    				continue;
    			XMLElements xType = XMLElements.valueFrom(ae.getAttributeValue(XMLElements.NAME.toString()));
    	   		if(isObjectMatch(attribute,type,value,xType, caseSensitive, like))
        			result.add(attribute.getProperty(xType));
    		}
     	}
    	
		org.jdom.Element filterElement = AttributeTableConfig.getInstance().getElementByName(container.getNodeType().toString());
		List<org.jdom.Element> feList = attElement.getChildren("item");    	
    	for(Filter filter: container.getFilterList()) {
    		for(org.jdom.Element fe: feList) {
    			if(!"1".equals(fe.getAttributeValue("visible")))
    				continue;
    			XMLElements xType = XMLElements.valueFrom(fe.getAttributeValue(XMLElements.NAME.toString()));
        		if(isObjectMatch(filter,type,value,xType, caseSensitive, like))
        			result.add(filter.getProperty(xType));
    		}
    	}
    	
    	
    	for(Container subContainer: container.getContainerList()) {
    		result.addAll(getContainerPtRefProperties(subContainer, type, value, caseSensitive, like));
    	}
    	return result;
    }

    
    public static boolean isObjectMatch(MartConfiguratorObject object, String type, String value, XMLElements xtype, 
    		boolean caseSensitive, boolean like) {
    	if(type.equals("all") || type.equals(object.getNodeType().toString())) {
    		{
    			if(like) {
    				if(caseSensitive)
    					return object.getPropertyValue(xtype).contains(value);
    				else
    					return object.getPropertyValue(xtype).toLowerCase().contains(value.toLowerCase());
    			}else {
    				if(caseSensitive)
    					return value.equals(object.getPropertyValue(xtype));
    				else 
    					return value.equalsIgnoreCase(object.getPropertyValue(xtype));
    			}
    		}
    	}
    	return false;
    }
    
	public void addPtReferencesforObject(MartConfiguratorObject object, PartitionTable pt, 
    		String pname) {
    	org.jdom.Element conElement = AttributeTableConfig.getInstance().getElementByName(object.getNodeType().toString());
        @SuppressWarnings("unchecked")
    	List<org.jdom.Element> eList = conElement.getChildren("item");
    	for(org.jdom.Element e: eList) {
			if(!"1".equals(e.getAttributeValue("visible")))
				continue;
			XMLElements xType = XMLElements.valueFrom(e.getAttributeValue(XMLElements.NAME.toString()));
			//check if it contains partition info
			String value = object.getPropertyValue(xType);
			if(value!=null && value.indexOf(pname)>=0) {
				List<String> ptList = McUtils.extractPartitionReferences(value);
				for(int i=1; i<ptList.size(); i++) {
					if(i%2 == 1) {
						int col = McUtils.getPartitionColumnValue(ptList.get(i));
						PartitionColumn column = pt.getColumnObject(col);
						//column.addObserver(object.getProperty(xType));
					}
				}
			}
		}
    }

    
    public void addPtReferences(Config config) {
    	PartitionTable pt = config.getMart().getSchemaPartitionTable();
    	String pname = "("+pt.getName();
    	this.addPtReferencesforObject(config, pt, pname);
     	//importable, exportable
    	for(ElementList imp: config.getImportableList()) {
    		this.addPtReferencesforObject(imp, pt, pname);
    	}
    	for(ElementList exp: config.getExportableList()) {
    		this.addPtReferencesforObject(exp, pt, pname);
    	}
    	Container rootC = config.getRootContainer();
    	this.addPtReferencesforContainer(rootC, pt, pname);
    }
    
    private void addPtReferencesforContainer(Container container, PartitionTable pt, String pname) {
    	this.addPtReferencesforObject(container, pt, pname);

    	for(Attribute attribute: container.getAttributeList()) {
    		this.addPtReferencesforObject(attribute, pt, pname);
     	}
    	for(Filter filter: container.getFilterList()) {
    		this.addPtReferencesforObject(filter, pt, pname);
    	}
    	   	
    	for(Container subContainer: container.getContainerList()) {
    		addPtReferencesforContainer(subContainer, pt, pname);
    	}
    }


}