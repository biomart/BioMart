package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.List;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.jdom.Element;

public class Container extends MartConfiguratorObject {

	private static final long serialVersionUID = 8818099786702183740L;

	private int uniqueId;
	private List<Container> containerList = null;
	private List<Filter> filterList = null;
	private List<Attribute> attributeList = null;

	public void addContainer(Container container) {	
		//check if the container already exist
		for(Container c: this.containerList) {
			if(c == container) 
				return;
		}		
		//check if there is one has the same name, if yes, change my name
		String oldName = container.getName();
		Config config = this.getParentConfig();
		if(config!=null) {
			String newName = McGuiUtils.INSTANCE.getNextUniqueContainerName(config,oldName);
			if(!newName.equals(oldName))
				container.setName(newName);
			//add component to map
			config.addComponentToMap4Container(container);
		} else {
			String newName = this.getNextUniqueContainerName(oldName);
			if(!newName.equals(oldName))
				container.setName(newName);			
		}
		MartController.getInstance().setChanged(true);
		this.containerList.add(container);
		container.setParentContainer(this);
	}
	public void addContainer(int index, Container container) {	
		//check if the container already exist
		for(Container c: this.containerList) {
			if(c == container) 
				return;
		}		
		//check if there is one has the same name, if yes, change my name
		String oldName = container.getName();
		Config config = this.getParentConfig();
		if(config!=null) {
			String newName = McGuiUtils.INSTANCE.getNextUniqueContainerName(config,oldName);
			if(!newName.equals(oldName))
				container.setName(newName);
			//add component to map
			config.addComponentToMap4Container(container);
		} else {
			String newName = this.getNextUniqueContainerName(oldName);
			if(!newName.equals(oldName))
				container.setName(newName);			
		}
		MartController.getInstance().setChanged(true);
		this.containerList.add(index, container);
		container.setParentContainer(this);
	}

	public void removeContainer(Container container) {
		if(this.containerList.remove(container)) {
			MartController.getInstance().setChanged(true);
			Config config = this.getParentConfig();
			if(config!=null)
				config.removeComponentFromMap4Container(container);
		}
	}

	public void removeFilter(Filter filter) {
		if(this.filterList.remove(filter)) {
			this.getParentConfig().removeFromFilterMap(filter);
			MartController.getInstance().setChanged(true);
		}
	}

	public void removeAttribute(Attribute attribute) {
		if(this.attributeList.remove(attribute)) {
			this.getParentConfig().removeFromAttributeMap(attribute);
			MartController.getInstance().setChanged(true);
		}
	}

	public void addFilter(Filter filter) {
		this.addFilter(this.filterList.size(), filter);
		if(this.getParentConfig() != null)
            this.getParentConfig().syncWithMasterconfig();
	}
	public void addFilter( int index, Filter filter) {
		filter.setParent(this);
		if(!this.filterList.contains(filter)) {
			this.filterList.add(filter);
			//add it to component map 
			if(this.getParentConfig()!=null) {
				this.getParentConfig().addToFilterMap(filter);
			}	
		}else {
			int oldIn = this.filterList.indexOf(filter);
			this.filterList.set(oldIn, filter);
		}
		MartController.getInstance().setChanged(true);
	}
	public void addAttribute(Attribute attribute) {
		this.addAttribute(this.attributeList.size(), attribute);
		if(this.getParentConfig() != null)
			this.getParentConfig().syncWithMasterconfig();
	}
	
	public void addAttribute(int index, Attribute attribute) {
		//check if this attribute exist
		if(this.attributeList.contains(attribute)) {
			//replace the old one
			int oldIn = this.attributeList.indexOf(attribute);
			this.attributeList.set(oldIn, attribute);
		} else {
			//add it to component map
			if(this.getParentConfig()!=null) {
				//check if attribute exists in config
				Attribute existAtt = this.getParentConfig().getAttributeByName(attribute.getName(), new ArrayList<String>());
				if(null!=existAtt) {
					if(existAtt == attribute) {
						/*
						 * if an attribute already exist don't change
						 */						
					} else {
						//rename attribute
						String uniqueName = McUtils.getUniqueAttributeName(this.getParentConfig(), attribute.getName()) ;
						attribute.setName(uniqueName);
					}
					//attribute.setInternalName(uniqueName);
				}
				this.getParentConfig().addToAttributeMap(attribute);
			}
			this.attributeList.add(index, attribute);
		}
		attribute.setParent(this);
		MartController.getInstance().setChanged(true);
	}

	public List<Container> getContainerList() {
		return this.containerList;
	}
	public List<Filter> getFilterList() {
		return this.filterList;
	}
	public List<Attribute> getAttributeList() {
		return this.attributeList;
	}

	public Attribute getAttributeByName(String name) {
		
		for(Attribute att: this.getAllAttributes(null, true, true)) {
		//for(Attribute att: this.attributeList) {
			if(att.getName().equals(name))
				return att;
		}
		return null;
	}

	public Filter getFilterByName(String name) {
		for(Filter fil: this.getAllFilters(null, true, true)) {
		//for(Filter fil: this.filterList) {
			if(fil.getName().equals(name))
				return fil;
		}
		return null;
	}

	public Attribute getAttributeRecursively(String name) {
		//Element element = getElementRecursively(true, name);
		//return element!=null ? (Attribute)element : null;
		for(Attribute attribute:this.attributeList) {
			if(attribute.getName().equals(name))
				return attribute;
			//
			if(McUtils.hasPartitionBinding(attribute.getName())) {
				List<String> ptRefList = McUtils.extractPartitionReferences(attribute.getName());
				//if has partition references
				if(ptRefList.size()>1) {
					//assume only one partition for now
					String ptRef = ptRefList.get(1);
					String ptName = McUtils.getPartitionTableName(ptRef);
					PartitionTable pt = this.getParentConfig().getMart().getPartitionTableByName(ptName);
					for(int i=0; i<pt.getTotalRows(); i++) {
						if(name.equals(McUtils.replacePartitionReferences(pt, i, ptRefList)))
							return attribute;
					}					
				}
				return null;

			}
		}
		for(Container subcontainer: this.containerList) {
			Attribute attribute = subcontainer.getAttributeRecursively(name);
			if(attribute != null) 
				return attribute;
		}
		return null;
	}
	
	public Attribute getAttributeRecursively2(String name) {
		//Element element = getElementRecursively(true, name);
		//return element!=null ? (Attribute)element : null;
		for(Attribute attribute:this.attributeList) {
			if(attribute.getName().equals(name))
				return attribute;
			//
			if(McUtils.hasPartitionBinding(attribute.getName())) {
				List<String> ptRefList = McUtils.extractPartitionReferences(attribute.getName());
				//if has partition references
				if(ptRefList.size()>1) {
					//assume only one partition for now
					String ptRef = ptRefList.get(1);
					String ptName = McUtils.getPartitionTableName(ptRef);
					PartitionTable pt = this.getParentConfig().getMart().getPartitionTableByName(ptName);
					for(int i=0; i<pt.getTotalRows(); i++) {
						String refName = McUtils.replacePartitionReferences(pt, i, ptRefList);
						if(refName!=null && refName.indexOf(name)>=0)
							return attribute;
					}					
				}
			}
		}
		for(Container subcontainer: this.containerList) {
			Attribute attribute = subcontainer.getAttributeRecursively2(name);
			if(attribute != null) 
				return attribute;
		}
		return null;
	}
	
	/**
	 * find a filter with the same name
	 * @param name
	 * @return
	 */
	public Filter getFilterRecursively(String name) {
//		Element element = getElementRecursively(false, name);
//		return element!=null ? (Filter)element : null;
		for(Filter filter:this.filterList) {
			if(filter.getName().equals(name))
				return filter;
			//
			if(McUtils.hasPartitionBinding(filter.getName())) {
				List<String> ptRefList = McUtils.extractPartitionReferences(filter.getName());
				//if has partition references
				if(ptRefList.size()>1) {
					//assume only one partition for now
					String ptRef = ptRefList.get(1);
					String ptName = McUtils.getPartitionTableName(ptRef);
					PartitionTable pt = this.getParentConfig().getMart().getPartitionTableByName(ptName);
					for(int i=0; i<pt.getTotalRows(); i++) {
						if(name.equals(McUtils.replacePartitionReferences(pt, i, ptRefList)))
							return filter;
					}					
				}

			}
		}
		for(Container subcontainer: this.containerList) {
			Filter filter = subcontainer.getFilterRecursively(name);
			if(filter != null) 
				return filter;
		}
		return null;
	}
	
	/**
	 * find a filter if the name is similar with the pattern
	 * @param name
	 * @return
	 */
	public Filter getFilterRecursively2(String name) {
//		Element element = getElementRecursively(false, name);
//		return element!=null ? (Filter)element : null;
		for(Filter filter:this.filterList) {
			if(filter.getName().equals(name))
				return filter;
			//
			if(McUtils.hasPartitionBinding(filter.getName())) {
				List<String> ptRefList = McUtils.extractPartitionReferences(filter.getName());
				//if has partition references
				if(ptRefList.size()>1) {
					//assume only one partition for now
					String ptRef = ptRefList.get(1);
					String ptName = McUtils.getPartitionTableName(ptRef);
					PartitionTable pt = this.getParentConfig().getMart().getPartitionTableByName(ptName);
					for(int i=0; i<pt.getTotalRows(); i++) {
						String refName = McUtils.replacePartitionReferences(pt, i, ptRefList);
						if(refName!=null && refName.indexOf(name)>=0)
							return filter;
					}					
				}
				//return null;

			}
		}
		for(Container subcontainer: this.containerList) {
			Filter filter = subcontainer.getFilterRecursively2(name);
			if(filter != null) 
				return filter;
		}
		return null;
	}



	public org.jdom.Element generateXml() {
		
		org.jdom.Element element = new org.jdom.Element(XMLElements.CONTAINER.toString());
		super.saveConfigurableProperties(element);
		element.setAttribute(XMLElements.NAME.toString(), this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(), this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.DESCRIPTION));
		element.setAttribute(XMLElements.HIDE.toString(), this.isHidden()? 
				XMLElements.TRUE_VALUE.toString(): XMLElements.FALSE_VALUE.toString());
		element.setAttribute(XMLElements.MAXCONTAINERS.toString(), this.getPropertyValue(XMLElements.MAXCONTAINERS));
		element.setAttribute(XMLElements.MAXATTRIBUTES.toString(), this.getPropertyValue(XMLElements.MAXATTRIBUTES));
		element.setAttribute(XMLElements.INDEPENDENTQUERYING.toString(),this.getPropertyValue(XMLElements.INDEPENDENTQUERYING));

		for (Container container : this.containerList) {
			element.addContent(container.generateXml());
		}

		for (Attribute att: this.attributeList) {
			element.addContent(att.generateXml());
		}

		if(this.filterList!=null)
			for(Filter tmpFil: this.filterList) {
				//check if filter has pointed attribute, if not then not saving out
				//if(tmpFil.getAttribute() != null)
					element.addContent(tmpFil.generateXml());
			}
		return element;
	}

	public Container(String name) {
		super(name);
		this.setNodeType(McNodeType.CONTAINER);
		this.uniqueId = McUtils.getNextContainerId();
		this.attributeList = new ArrayList<Attribute>();
		this.containerList = new ArrayList<Container>();
		this.filterList = new ArrayList<Filter>();
	}
	
	public Container(org.jdom.Element element) {
		super(element);
		this.setNodeType(McNodeType.CONTAINER);
		this.uniqueId = McUtils.getNextContainerId();
		this.attributeList = new ArrayList<Attribute>();
		this.containerList = new ArrayList<Container>();
		this.filterList = new ArrayList<Filter>();
		
		//sub container
		List<org.jdom.Element> subConElementList = element.getChildren(XMLElements.CONTAINER.toString());
		for(org.jdom.Element subE: subConElementList) {
			Container subCon = new Container(subE);
			this.addContainer(subCon);
		}
		
		//attribute
		List<org.jdom.Element> attElementList = element.getChildren(XMLElements.ATTRIBUTE.toString());
		for(org.jdom.Element attE: attElementList) {
			Attribute att = new Attribute(attE);
			this.addAttribute(att);
		}
		
		//filter
		List<org.jdom.Element> filterElementList = element.getChildren(XMLElements.FILTER.toString());
		for(org.jdom.Element filterE: filterElementList) {
			Filter filter = new Filter(filterE);
			this.addFilter(filter);
		}
	}

	private void setParentContainer(Container container) {
		this.parent = container;
	}

	public Container getParentContainer() {
		MartConfiguratorObject parentCon = this.parent;
		// if the parent is not a container, return null;
		if(parentCon.getNodeType().equals(McNodeType.CONTAINER))
			return (Container)parentCon;
		else
			return null;
	}

	public Config getParentConfig() {
		MartConfiguratorObject pObject = this.getParent();
		//it may be null if the container was not attached to a parent;
		if(pObject == null)
			return null;
		while(pObject!=null && pObject.getNodeType().equals(McNodeType.CONTAINER)) {
			pObject = pObject.getParent();
		}
		return (Config)pObject;
	}

	public void setParentConfig(Config config) {
		this.parent = config;
	}

	public int getUniqueId() {
		return this.uniqueId;
	}


	public boolean isEmpty() {
		for(Container subContainer: this.containerList) {
			if(!subContainer.isEmpty())
				return false;
		}

		if(this.attributeList.isEmpty() && this.filterList.isEmpty())
			return true;
		else
			return false;
	}

	public boolean isEmptyForFilter() {
		for(Container subContainer: this.containerList) {
			if(!subContainer.isEmptyForFilter())
				return false;
		}

		if(this.getVisibleFilterList().isEmpty())
			return true;
		else
			return false;
	}

	public boolean isEmptyForAttribute() {
		for(Container subContainer: this.containerList) {
			if(!subContainer.isEmptyForAttribute())
				return false;
		}

		if(this.getVisibleAttributeList().isEmpty())
			return true;
		else
			return false;

	}

	public List<Attribute> getVisibleAttributeList() {
		List<Attribute> aList = new ArrayList<Attribute>();
		for(Attribute a: this.attributeList) {
			if(!a.isHidden())
				aList.add(a);
		}
		return aList;
	}

	public List<Filter> getVisibleFilterList() {
		List<Filter> fList = new ArrayList<Filter>();
		for(Filter f: this.filterList) {
			if(!f.isHidden())
				fList.add(f);
		}
		return fList;
	}

	public Integer getMaxContainers() {		
		int qr = 0;
		try {
			qr =  Integer.parseInt(this.getPropertyValue(XMLElements.MAXCONTAINERS));
		}
		catch(NumberFormatException e) {
			qr = 0;
			//e.printStackTrace();
		}
		return qr;
	}

	
	public Integer getMaxAttributes() {		
		int qr = 0;
		try {
			qr =  Integer.parseInt(this.getPropertyValue(XMLElements.MAXATTRIBUTES));
		}
		catch(NumberFormatException e) {
			qr = 0;
			//e.printStackTrace();
		}
		return qr;
	}

	public void setMaxAttributes(Integer maxAttributes) {
		this.setProperty(XMLElements.MAXATTRIBUTES, ""+maxAttributes);
	}
	
	public void setMaxContainers(Integer maxContainers) {
		this.setProperty(XMLElements.MAXCONTAINERS, ""+maxContainers);
	}

	public Container getContainerByNameResursively(String name) {
		if(this.getName().equals(name))
			return this;
		for(Container c : this.containerList) {
			if (c.getName().equals(name))
				return c;
			Container tmpc =  c.getContainerByNameResursively(name);
			if(tmpc!=null)
				return tmpc;
		}
		return null;
	}

	/**
	 * return all the attributes recursively from current container till leaf.
	 * @return List<Attribute>
	 */
	public List<Attribute> getAllAttributes(List<String> datasets, boolean includeHiddenContainer, boolean includeHiddenAttributes) {
		List<Attribute> attList = new ArrayList<Attribute>();
		if(this.isHidden() && (!includeHiddenContainer))
			return attList;

		for(Attribute attribute: this.attributeList) {
			if(!includeHiddenAttributes ){
				if(attribute.isHidden())
					continue;
			}
			if(datasets == null || datasets.isEmpty())
				attList.add(attribute);
			else if(attribute.inPartition(datasets)) {
				attList.add(attribute);
			}
		}
		for(Container container: this.containerList) {
			List<Attribute> tmpAttList = container.getAllAttributes(datasets,includeHiddenContainer,includeHiddenAttributes);
			attList.addAll(tmpAttList);
		}
		return attList;
	}


	/**
	 * return all the filters recursively from current container till leaf.
	 * @return List<Filter>
	 */
	public List<Filter> getAllFilters(List<String> datasets, boolean includeHiddenContainer, boolean includeHiddenFilters) {
		List<Filter> filList = new ArrayList<Filter>();
		if(this.isHidden() && (!includeHiddenContainer))
			return filList;
		for(Filter filter: this.filterList) {
			if(!includeHiddenFilters){
				if(filter.isHidden())
					continue;
			}
			if(datasets == null || datasets.isEmpty())
				filList.add(filter);
			else if(filter.inPartition(datasets)) {
				filList.add(filter);
			}
		}
		for(Container container: this.containerList) {
			List<Filter> tmpFilterList = container.getAllFilters(datasets, includeHiddenContainer, includeHiddenFilters);
			filList.addAll(tmpFilterList);
		}
		return filList;		
	}
	

	public List<Container> getAllContainers() {
		List<Container> contList = new ArrayList<Container>();
		contList.add(this);
		for(Container container: this.containerList) {
			List<Container> tmpContList = container.getAllContainers();
			contList.addAll(tmpContList);
		}
		return contList;
	}

	private String getNextUniqueContainerName(String baseName) {
		String tmpName = baseName;
		int i=1;
		while(this.getContainerByNameResursively(tmpName)!=null) {
			tmpName = baseName + "_"+i++;
		}
		return tmpName;
	}

	public void mergeContainer(Container anotherContainer) {
		for(Attribute a1: anotherContainer.getAttributeList()) {
			if(this.getAttributeByName(a1.getName())==null) {
				//update dataset column
				if(a1.getDataSetColumn()==null) {
					Log.debug("merge container for attribute: "+a1.getName()+" could not find source datasetcollumn ");
					continue;
				}
				String dstName = a1.getDataSetColumn().getTable().getName();
				String dscName = a1.getDataSetColumn().getName();
				DatasetTable dst = this.getParentConfig().getMart().getTableByName(dstName);
				if(dst == null) {
					Log.debug("merge container for attribute: "+a1.getName()+" could not find datasettable "+dstName);
					continue;
				}
				DatasetColumn dsc = dst.getColumnByName(dscName);
				if(dsc == null) {
					Log.debug("merge container for attribute: "+a1.getName()+" could not find datasetcolumn "+dscName);
				}
				a1.updateDatasetColumn(dsc);
				this.addAttribute(a1);
			}				
		}
		for(Filter f1: anotherContainer.getFilterList()) {
			Filter oldFilter = this.getFilterByName(f1.getName());
			if(oldFilter == null) {
				this.addFilter(f1);
			} else {
				for(Filter fl: f1.getFilterList())
					oldFilter.addFilter(fl);
			}
			if(this.getFilterByName(f1.getName())==null) {
				this.addFilter(f1);
			}
		}
		for(Container c1: anotherContainer.getContainerList()) {
			if(this.getContainerByNameResursively(c1.getName())==null) {
				Container c2 = new Container(c1.getName());
				c2.setDisplayName(c1.getDisplayName());
				c2.setHideValue(c1.isHidden());
				this.addContainer(c2);
				c2.mergeContainer(c1);
			}else {
				this.getContainerByNameResursively(c1.getName()).mergeContainer(c1);
			}							
		}
	}

	public boolean isIndependentQuerying() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.INDEPENDENTQUERYING));
	}

	@Override
	public boolean isHidden() {
		return super.isHidden();
		/*
		if(super.isHidden())
			return true;
		if(this.getParent() == null)
			return false;
		else
			return this.getParent().isHidden();
			*/
	}


	public boolean syncWithMasterconfig()
	{
		Config masterConfig = this.getParentConfig().getMart().getMasterConfig();
		if(masterConfig != null){
			for(Container container : this.getContainerList()){
				container.syncWithMasterconfig();
			}
			for(Attribute attr: this.getAttributeList()){
				//if master config not contain the attribute
				if(masterConfig.getAttributeByName(attr.getName(), null) == null){
					String containerName = "";
					if(attr.isPointer()){
						containerName = "newAttributePointer";
					}else{
						containerName = "newAttribute";
					}
					Container newContainer = masterConfig.getContainerByName(containerName);
					if(newContainer == null)
					{
						newContainer = new Container(containerName);
						masterConfig.getRootContainer().addContainer(newContainer);
					}
					Element attElement = attr.generateXml();
					Attribute newAtt = new Attribute(attElement);
					newContainer.addAttribute(newAtt);
				}
			}
			for(Filter filter: this.getFilterList()){
				if(masterConfig.getFilterByName(filter.getName(), null) == null){
					//hardcode this to avoid master syc problem for testing for now
					if(filter.getName().equals("type") || filter.getName().equals("encode_region"))
						continue;
					String containerName = "";
					if(filter.isPointer()){
						containerName = "newFilterPointer";
					}else{
						containerName = "newFilter";
					}
					Container newContainer = masterConfig.getContainerByName(containerName);
					if(newContainer == null)
					{
						newContainer = new Container(containerName);
						masterConfig.getRootContainer().addContainer(newContainer);
					}
					Element filterElement = filter.generateXml();
					Filter newFil = new Filter(filterElement);
					newContainer.addFilter(newFil);
				}
			}
		}
		return false;
	}
	
}

