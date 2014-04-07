package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.List;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.jdom.Element;

public class Link extends MartConfiguratorObject {

	public Link(Element element) {
		super(element);
		this.setNodeType(McNodeType.LINK);
	}
	
	public Link(String name) {
		super(name);
		this.setNodeType(McNodeType.LINK);
	}

	@Override
	public Element generateXml() {
		Element element = new Element(XMLElements.LINK.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.DESCRIPTION));
		
		element.setAttribute(XMLElements.HIDE.toString(),this.isHidden()?
				XMLElements.TRUE_VALUE.toString():XMLElements.FALSE_VALUE.toString());
		element.setAttribute(XMLElements.DATASETS.toString(), this.getPropertyValue(XMLElements.DATASETS));
		
		element.setAttribute(XMLElements.POINTEDMART.toString(),this.getPropertyValue(XMLElements.POINTEDMART));
		element.setAttribute(XMLElements.POINTEDCONFIG.toString(), this.getPropertyValue(XMLElements.POINTEDCONFIG));
		element.setAttribute(XMLElements.FILTERS.toString(), this.getPropertyValue(XMLElements.FILTERS));
		element.setAttribute(XMLElements.ATTRIBUTES.toString(),this.getPropertyValue(XMLElements.ATTRIBUTES));
		//element.setAttribute(XMLElements.POINTEDDATASET.toString(), this.getPropertyValue(XMLElements.POINTEDDATASET));
		return element;
	}

	@Override
	@Deprecated
	public void synchronizedFromXML() {
	}
	
	public void setPointerMart(Mart mart) {
		this.setProperty(XMLElements.POINTEDMART, mart.getName());
	}
	
	public Mart getPointedMart() {
		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDMART))) {
			return null;
		}
		if(this.getParentConfig() == null || this.getParentConfig().getMart() == null
				|| this.getParentConfig().getMart().getMartRegistry() == null)
			return null;
		return this.getParentConfig().getMart().getMartRegistry().
			getMartByName(this.getPropertyValue(XMLElements.POINTEDMART));
	}
	
	public Config getPointedConfig() {
		Mart pointedMart = this.getPointedMart();
		if(pointedMart == null || McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDCONFIG)))
			return null;
		return pointedMart.getConfigByName(this.getPropertyValue(XMLElements.POINTEDCONFIG));
	}
	
	public void setPointedConfig(Config config) {
		this.setProperty(XMLElements.POINTEDCONFIG, config.getName());
	}
	
	public void addFilter(Filter filter) {
		List<String> tmp = this.getPropertyList(XMLElements.FILTERS);
		List<String> filStrList = new ArrayList<String>(tmp);

		if(!filStrList.contains(filter.getName())) {
			filStrList.add(filter.getName());				
			this.setProperty(XMLElements.FILTERS, McUtils.StrListToStr(filStrList, ","));
		}
	}
	
	public void addAttribute(Attribute attribute) {
		List<String> tmp = this.getPropertyList(XMLElements.ATTRIBUTES);
		List<String> attStrList = new ArrayList<String>(tmp);

		if(!attStrList.contains(attribute.getName())) {
			attStrList.add(attribute.getName());
			this.setProperty(XMLElements.ATTRIBUTES, McUtils.StrListToStr(attStrList, ","));
		}
	}
	
	public List<Filter> getFilterList() {
		List<Filter> result = new ArrayList<Filter>();
		for(String filStr: this.getPropertyList(XMLElements.FILTERS)) {
			Filter filter = this.getPointedConfig().getFilterByName(filStr, null);
			if(filter!=null)
				result.add(filter);
		}
		return result;
	}
	
	public List<Attribute> getAttributeList() {
		List<Attribute> result = new ArrayList<Attribute>();
		for(String attStr: this.getPropertyList(XMLElements.ATTRIBUTES)) {
			Attribute attribute = this.getParentConfig().getAttributeByName(attStr, null);
			if(attribute!=null)
				result.add(attribute);
		}
		return result;
	}

	public List<String> getConfigsDropDown(String martName) {
		List<String> result = new ArrayList<String>();
		return result;
	}
	
	//update importable for all other configs
	@Deprecated
	public void updatePortable() {
/*		String portableName = this.getName()+"_"+this.getParentConfig().getMart().getName();
		//create link for all configs
		for(Config config: this.getParentConfig().getMart().getConfigList()) {
			ElementList exportable = config.getExportableByInternalName(portableName);
			if(null!=exportable) {
				config.getExportableList().remove(exportable);
			}		
			//create exportable and importable again
			ElementList newExp = new ElementList(config,portableName,PortableType.EXPORTABLE);
			for(Attribute att: this.getAttributeList()) {
				newExp.addAttribute(att);
			}
			config.addElementList(newExp);
		}
		
		//create importable for pointedconfig
		Config targetConfig = this.getPointedConfig();			
		ElementList importable = targetConfig.getImportableByInternalName(portableName);
		if(null!=importable) {
			targetConfig.getImportableList().remove(importable);
		}
		
		ElementList newImp = new ElementList(targetConfig,portableName, PortableType.IMPORTABLE);
		for(Filter fil: this.getFilterList()) {
			newImp.addFilter(fil);
		}
		targetConfig.addElementList(newImp);
	*/
		for(Config config: this.getParentConfig().getMart().getConfigList()) {
			if(this.getParentConfig().equals(config))
				continue;
			Link otherLink = config.getLinkByName(this.getName());
			if(otherLink !=null) {
				//remove
				config.getLinkList().remove(otherLink);
			}
			Element linkElement = this.generateXml();
			Link newLink = new Link(linkElement);
			config.addLink(newLink);
			newLink.synchronizedFromXML();
		}

	}

	/**
	 * @return the pointedDataset
	 */
	public String getPointedDataset() {
		return this.getPropertyValue(XMLElements.DATASETS);
	}

	/**
	 * @param pointedDataset the pointedDataset to set
	 */
	public void setPointedDataset(String pointedDataset) {
		this.setProperty(XMLElements.DATASETS, pointedDataset);
	}


	
	
}