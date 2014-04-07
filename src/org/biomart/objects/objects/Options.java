package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

public class Options {
	private org.jdom.Element optionRoot;
	
	private static Options instance;
	
	public static Options getInstance() {
		if(instance == null)
			instance = new Options();
		return instance;
	}
	
	public void setOptions(org.jdom.Element root) {
		this.optionRoot = root;
		if(this.optionRoot == null)
			this.optionRoot = new Element(XMLElements.OPTIONS.toString());
		//synchronize option objects
		this.synchronizeOptionObjects(true);
		//should remove option element after synchronized
	}
	
	private Options() {
	}
	
	public Element getMartElementByName(String martName) {
		if(this.optionRoot == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		List<Element> martElementList = this.getOptionRootElement().getChildren();
		//find mart element
		Element martElement = null;
		for(Element element: martElementList) {
			if(martName.equals(element.getAttributeValue(XMLElements.NAME.toString()))) {
				martElement = element;
				break;
			}
		}
		return martElement;
	}
	
	public Element getMartElement(Mart mart) {
		return this.getMartElementByName(mart.getName());
	}
	
	public Element getConfigElement(Config config, Element martElement) {
		if(martElement == null)
			return null;
		//find config element
		@SuppressWarnings("unchecked")
		List<Element> configElementList = martElement.getChildren();
		Element configElement = null;
		for(Element element: configElementList) {
			if(config.getName().equals(element.getAttributeValue(XMLElements.NAME.toString()))) {
				configElement = element;
				break;
			}
		}
		return configElement;
	}
	
	public Element getFilterOptionElement(Config config, Filter filter) {
		Mart mart = config.getMart();
		Config mConfig = mart.getMasterConfig();
		//find mart element
		Element martElement = getMartElement(mart);
		if(martElement == null)
			return null;
		//find config element

		Element configElement = getConfigElement(mConfig, martElement);
		if(configElement == null) {			
			configElement = getConfigElement(mart.getMasterConfig(), martElement);
			if(configElement == null)
				return null;			
		}
			
		@SuppressWarnings("unchecked")
		List<Element> filterElementList = configElement.getChildren();
		Element filterElement = null;
		for(Element element: filterElementList) {
			if(filter.getName().equals(element.getAttributeValue(XMLElements.NAME.toString()))) {
				filterElement = element;
			}
		}
		return filterElement;
	}
		
	public void updateFilterOptionElement(Filter filter, Dataset ds, List<FilterData> fdList) {
		//update the master config
		Mart mart = filter.getParentConfig().getMart();
		Config config = mart.getMasterConfig();
		Filter masterFilter = config.getFilterByName(filter.getName(), null);
		
		//keep the order for the old options
		List<String> dss = new ArrayList<String>();
		dss.add(ds.getName());
		List<FilterData> oldfd = masterFilter.getFilterDataList(dss);
		oldfd.retainAll(fdList);
		fdList.removeAll(oldfd);
		List<FilterData> newFdList = new ArrayList<FilterData>();
		newFdList.addAll(oldfd);
		newFdList.addAll(fdList);
	
		masterFilter.clearOptions(ds.getName());
		for(FilterData fd: newFdList) {
			masterFilter.addOption(ds.getName(), fd);
		}						
		
	}
	
	public void addMartElement(org.jdom.Element martElement) {
		//check if it already exist
		if(this.getMartElementByName(martElement.getAttributeValue(XMLElements.NAME.toString())) !=null)
			return;
		this.getOptionRootElement().addContent(martElement);
	}
	
	public Element getOptionRootElement() {
		if(this.optionRoot!=null)
			this.optionRoot.detach();
		else
			this.optionRoot = new Element(XMLElements.OPTIONS.toString());
		return this.optionRoot;
	}
	
	public void renameDataset(String oldvalue, String newvalue) {
		Element root = this.getOptionRootElement();
		@SuppressWarnings("unchecked")
		Iterator<Element> it = root.getDescendants(new ElementFilter(XMLElements.DATASET.toString()));
		while(it.hasNext()) {
			Element e = it.next();
			if(oldvalue.equals(e.getAttributeValue(XMLElements.NAME.toString()))) {
				e.setAttribute(XMLElements.NAME.toString(), newvalue);
			}
		}
	}

	public void clear() {
		this.getOptionRootElement().removeContent();
	}
	
	/*
	 * remove non master config options
	 */
	public void clean() {
		MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
		@SuppressWarnings("unchecked")
		List<Element> martElementList = this.optionRoot.getChildren();
		List<Element> droppedMarts = new ArrayList<Element>();
		List<Element> droppedConfigs = new ArrayList<Element>();
		//find mart element
		for(Element martElement: martElementList) {
			Mart mart = registry.getMartByName(martElement.getAttributeValue(XMLElements.NAME.toString()));
			if(mart == null) {
				droppedMarts.add(martElement);
			} else {
				@SuppressWarnings("unchecked")
				List<Element> configElementList = martElement.getChildren();
				//if only one element and it is not master, rename it to master
				String masterName = mart.getMasterConfig().getName();
				if(configElementList.size()==1 && !masterName.equals(configElementList.get(0).
						getAttributeValue(XMLElements.NAME.toString()))) {
					configElementList.get(0).setAttribute(XMLElements.NAME.toString(), masterName);
				}
				for(Element configElement: configElementList) {
					if(!masterName.equals(configElement.getAttributeValue(XMLElements.NAME.toString()))) {
						droppedConfigs.add(configElement);
					}
				}
			}
		}
		//remove
		for(Iterator<Element> it = droppedMarts.iterator(); it.hasNext();) {
			Element e = it.next();
			e.getParentElement().removeContent(e);
		}
		for(Iterator<Element> it = droppedConfigs.iterator(); it.hasNext();) {
			Element e = it.next();
			e.getParentElement().removeContent(e);
		}
	}

	public void synchronizeOptionObjects(boolean full) {
		MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
		@SuppressWarnings("unchecked")
		List<Element> martElements = this.getOptionRootElement().getChildren();
		for(Element martE: martElements) {
			Mart mart = registry.getMartByName(martE.getAttributeValue(XMLElements.NAME.toString()));
			if(null==mart)
				continue;
			if("0.8".equals(martE.getAttributeValue(XMLElements.VERSION.toString()))) {
				if(full)
					this.synchronize08Option(mart, martE);
			} else {
				this.synchronize07Option(mart, martE);
			}
		}
		this.optionRoot.removeContent();
	}
	
	private void synchronize07Option(Mart mart, Element martE) {
		@SuppressWarnings("unchecked")
		List<Element> configElements = martE.getChildren();
		/*
		 * should have only one masterconfig
		 */
		for(Element configE: configElements) {
			Config config = mart.getMasterConfig();
			@SuppressWarnings("unchecked")
			List<Element> filterElements = configE.getChildren();
			for(Element filterE: filterElements) {
				Filter filter = config.getFilterByName(filterE.getAttributeValue(XMLElements.NAME.toString()), null);
				if(filter == null)
					continue;
				@SuppressWarnings("unchecked")
				List<Element> subElements = filterE.getChildren();
				for(Element subE: subElements) {
					String datasetName = null;
					//dataset?
					if(subE.getName().equals(XMLElements.DATASET.toString())) {
						datasetName = subE.getAttributeValue(XMLElements.NAME.toString());
						@SuppressWarnings("unchecked")
						List<Element> rowElements = subE.getChildren();
						for(Element rowE: rowElements) {
							//filterdata exist?
							FilterData fd = this.createFilterDataFromElement07(rowE);
							filter.addOption(datasetName,fd);
							//has push action
							@SuppressWarnings("unchecked")
							List<Element> subElement = rowE.getChildren();
							if(!McUtils.isCollectionEmpty(subElement)) {
								for(Element subFilterE: subElement) {
									String subFilter = subFilterE.getAttributeValue(XMLElements.NAME.toString());
									@SuppressWarnings("unchecked")
									List<Element> subRowElements = subFilterE.getChildren();
									for(Element subRowE: subRowElements) {
										FilterData subFd = this.createFilterDataFromElement07(subRowE);	
										List<FilterData> subFds = fd.getPushFilterOptions(subFilter);
										if(subFds == null) {
											subFds = new ArrayList<FilterData>();
											fd.addPushFilterOptions(subFilter, subFds);
										}										
										if(!subFds.contains(subFd))
											subFds.add(subFd);
									}
								}
							}
						}
					} //end of dataset
					else {
						//row
						FilterData fd = this.createFilterDataFromElement07(subE);
						for(Dataset ds: filter.getParentConfig().getMart().getDatasetList())							
							filter.addOption(ds.getName(),fd);						
					}
				}
			}
		}

	}
	
	private void synchronize08Option(Mart mart, Element martE) {

		Element configE = martE.getChild(XMLElements.CONFIG.toString());

		Config config = mart.getConfigByName(configE.getAttributeValue(XMLElements.NAME.toString()));
		@SuppressWarnings("unchecked")
		List<Element> filterElements = configE.getChildren();
		for(Element filterE: filterElements) {
			Filter filter = config.getFilterByName(filterE.getAttributeValue(XMLElements.NAME.toString()), null);
			if(filter == null)
				continue;
			@SuppressWarnings("unchecked")
			List<Element> dssElements = filterE.getChildren();
			for(Element dsE: dssElements) {
				//dataset
				@SuppressWarnings("unchecked")
				List<Element> fdElements = dsE.getChildren();
				for(Element fdE: fdElements) {
					FilterData fd = this.createFilterDataFromElement08(fdE);
					
					filter.addOption(dsE.getAttributeValue(XMLElements.NAME.toString()),fd);
					//push action?
					if(fdE.getContentSize()>0) {
						@SuppressWarnings("unchecked")
						List<Element> subFilterElements = fdE.getChildren();
						for(Element subFilterE: subFilterElements) {
							String subFilter = subFilterE.getAttributeValue(XMLElements.NAME.toString());
							@SuppressWarnings("unchecked")
							List<Element> subRowElements = subFilterE.getChildren();
							for(Element subRowE: subRowElements) {
								FilterData subFd = this.createFilterDataFromElement08(subRowE);
								List<FilterData> subFds = fd.getPushFilterOptions(subFilter);
								if(subFds == null) {
									subFds = new ArrayList<FilterData>();
									fd.addPushFilterOptions(subFilter, subFds);
								}
								if(!subFds.contains(subFd))
									subFds.add(subFd);
							}
						}
					}
					
				}				
			}
		}	
	}
	
	private FilterData createFilterDataFromElement07(Element rowElement) {
		String data = rowElement.getAttributeValue("data");
		String[] dataArray = McUtils.getOptionsDataFromString(data);
		FilterData fd = new FilterData(dataArray[0],
				dataArray[1],new Boolean(dataArray[2]));		
		return fd;
	}
	
	private FilterData createFilterDataFromElement08(Element rowElement) {
		String name = rowElement.getAttributeValue(XMLElements.VALUE.toString());
		String displayName = rowElement.getAttributeValue(XMLElements.DISPLAYNAME.toString());
		Boolean b = Boolean.parseBoolean(rowElement.getAttributeValue(XMLElements.DEFAULT.toString()));
		FilterData fd = new FilterData(name,displayName,b);

		return fd;
	}
}