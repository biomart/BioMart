package org.biomart.objects.objects;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.biomart.common.resources.Log;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Element;


public final class Config extends MartConfiguratorObject {
	
	private final List<ElementList> importableList;
	private final List<ElementList> exportableList;
	private final Map<String,Attribute> attributeMap;
	private final Map<String,Filter> filterMap;
	private final Map<String,Container> containerMap;
	private final List<Link> linkList;
	private final List<RDFClass> rdflist;

	private Container rootContainer;
	
	private String dino;

	/**
	 * @param parentMart
	 * @param name
	 */
 	public Config(String name) {
		super(name);			
		this.importableList = new ArrayList<ElementList>();
		this.exportableList = new ArrayList<ElementList>();
		this.linkList = new ArrayList<Link>();
		//create maps
		this.attributeMap = new HashMap<String,Attribute>();
		this.filterMap = new HashMap<String,Filter>();
		this.containerMap = new HashMap<String,Container>();
		this.rdflist = new ArrayList<RDFClass>();
		this.setNodeType(McNodeType.CONFIG);
	}
	
	public Config(Element element) {
		super(element);
		this.importableList = new ArrayList<ElementList>();
		this.exportableList = new ArrayList<ElementList>();
		this.linkList = new ArrayList<Link>();
		//create maps
		this.attributeMap = new HashMap<String,Attribute>();
		this.filterMap = new HashMap<String,Filter>();
		this.containerMap = new HashMap<String,Container>();
		
		this.rdflist = new ArrayList<RDFClass>();
		this.setNodeType(McNodeType.CONFIG);
		
		//backwardcompatibility for old xml that doesn't have annotation tag
		Element annotationsElement = element.getChild(XMLElements.ANNOTATIONS.toString());
		if(null!=annotationsElement) {
			Element exportableElement = annotationsElement.getChild(XMLElements.EXPORTABLE.toString());
			Element importableElement = annotationsElement.getChild(XMLElements.IMPORTABLE.toString());
			if(null!=exportableElement) {
				@SuppressWarnings("unchecked")
				List<Element> expElementList = exportableElement.getChildren();
				for(Element expElement: expElementList) {
					//for AttributeTableConfig.xml
					expElement.setName(exportableElement.getName()+"."+expElement.getName());
					ElementList exportable = new Exportable(expElement);
					this.addElementList(exportable);
				}
			}
			if(null!=importableElement) {
				@SuppressWarnings("unchecked")
				List<Element> impElementList = importableElement.getChildren();
				for(Element impElement: impElementList) {
					impElement.setName(importableElement.getName()+"."+impElement.getName());
					ElementList importable = new Importable(impElement);
					this.addElementList(importable);
				}
			}
		} else {
			//old xml
			//importable
			@SuppressWarnings("unchecked")
			List<Element> impElementList = element.getChildren(XMLElements.IMPORTABLE.toString());
			for(Element impElement: impElementList) {
				ElementList importable = new Importable(impElement);
				this.addElementList(importable);
			}
			//exportable
			@SuppressWarnings("unchecked")
			List<Element> expElementList = element.getChildren(XMLElements.EXPORTABLE.toString());
			for(Element expElement: expElementList) {
				ElementList exportable = new Exportable(expElement);
				this.addElementList(exportable);
			}
		}
		//container
		Element containerElement = element.getChild(XMLElements.CONTAINER.toString());
		Container container = new Container(containerElement);
		this.addRootContainer(container);
		
		@SuppressWarnings("unchecked")
		List<Element> linkElementList = element.getChildren(XMLElements.LINK.toString());

		
		for(Element linkElement: linkElementList) {
			Link link = new Link(linkElement);
			this.addLink(link);
		}
		@SuppressWarnings("unchecked")
		List<Element> rdfElementList = element.getChildren(XMLElements.RDFCLASS.toString());
		for(Element rdfElement: rdfElementList) {
			RDFClass rdf = new RDFClass(rdfElement);
			this.addRDF(rdf);
		}
		
		org.jdom.Attribute dinoEl = element.getAttribute(XMLElements.DINO.toString());
		if (dinoEl != null) { 
			Log.debug(this.getClass().getName() + " setting dino with name "+ dinoEl.getValue());
			dino = dinoEl.getValue();
		}
		
		//TODO tmp
		//change master from "1" to "true"
		this.setMaster(this.isMasterConfig());
	}
	
	private void addImportable(ElementList importable) {
		if(!this.importableList.contains(importable)) {
			this.importableList.add(importable);
			importable.setParent(this);
		}
	}
	
	private void addExportable(ElementList exportable) {
		if(!this.exportableList.contains(exportable)) {
			this.exportableList.add(exportable);
			exportable.setParent(this);
		}
	}

	public Mart getMart() {
		return (Mart)this.parent;
	}

	public List<ElementList> getImportableList() {
		return this.importableList;
	}
	
	public ElementList getImportableByInternalName(String name) {
		for(ElementList imp: this.importableList) {
			if(imp.getInternalName().equals(name))
				return imp;
		}
		return null;
	}

	public ElementList getExportableByInternalName(String name) {
		for(ElementList exp: this.exportableList) {
			if(exp.getInternalName().equals(name))
				return exp;
		}
		return null;
	}
	
	public List<ElementList> getExportableList() {
		return this.exportableList;
	}
	
	public void addElementList(ElementList el) {
		if(el.isImportable()) 
			this.addImportable(el);
		else
			this.addExportable(el);
	}

	public Container getContainerByName(String name) {
		return this.containerMap.get(name);
		//return this.rootContainer.getContainerByNameResursively(name);
	}
	
	public Container getRootContainer() {
		if(this.rootContainer == null) {
			Container container = new Container("root");
			this.addRootContainer(container);
		}
		return this.rootContainer;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		Config config=(Config)object;
		return (
			super.equals(config) &&
			(this.parent==config.parent || (this.parent!=null && parent.equals(config.parent)))	// compare parent mart
		);
	}

    @Override
	public Element generateXml() {
    	return this.generateXml(true);
	}
    
    public Element generateXml(boolean includeImpExp) {
		Element element = new Element(XMLElements.CONFIG.toString());
		super.saveConfigurableProperties(element);
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.DESCRIPTION));
		
		element.setAttribute(XMLElements.HIDE.toString().toString(),this.isHidden()?
				XMLElements.TRUE_VALUE.toString():XMLElements.FALSE_VALUE.toString());
		element.setAttribute(XMLElements.DEFAULT.toString(),this.getPropertyValue(XMLElements.DEFAULT));
		element.setAttribute(XMLElements.METAINFO.toString(),this.getPropertyValue(XMLElements.METAINFO));
		element.setAttribute(XMLElements.DATASETHIDEVALUE.toString(), this.getPropertyValue(XMLElements.DATASETHIDEVALUE));
		element.setAttribute(XMLElements.DATASETDISPLAYNAME.toString(), this.getPropertyValue(XMLElements.DATASETDISPLAYNAME));
		element.setAttribute(XMLElements.MASTER.toString(), this.getPropertyValue(XMLElements.MASTER));
		element.setAttribute(XMLElements.READONLY.toString(), this.getPropertyValue(XMLElements.READONLY));
		element.setAttribute(XMLElements.PASSWORD.toString(), this.getPropertyValue(XMLElements.PASSWORD));
		element.setAttribute(XMLElements.RDFCLASS.toString(),this.getPropertyValue(XMLElements.RDFCLASS));
		
		Log.debug(this.getClass().getName() + "#generateXml() setting Dino attribute to "+ this.getPropertyValue(XMLElements.DINO));
		element.setAttribute(XMLElements.DINO.toString(), this.getPropertyValue(XMLElements.DINO));
		
		if(includeImpExp && this.isMasterConfig()) {
			//annotations
			Element annoElement = new Element(XMLElements.ANNOTATIONS.toString());
			element.addContent(annoElement);
			
			//importable/exportable
			Element impElement = new Element(XMLElements.IMPORTABLE.toString());
			annoElement.addContent(impElement);
			
			Element expElement = new Element(XMLElements.EXPORTABLE.toString());
			annoElement.addContent(expElement);
			
			for(ElementList importable: this.importableList) {
				impElement.addContent(importable.generateXml());
			}
	
			
			for (ElementList exportable : this.exportableList) {
				expElement.addContent(exportable.generateXml());
			}
			
			for(Link link: this.linkList) {
				element.addContent(link.generateXml());
			}
		}
		
		for(RDFClass rdf: this.rdflist) {
			element.addContent(rdf.generateXml());
		}


		element.addContent(this.rootContainer.generateXml());
		return element;
    }
    
	public void addRootContainer(Container rootContainer) {
		this.rootContainer = rootContainer;
		this.rootContainer.setParentConfig(this);
		this.containerMap.clear();
		this.attributeMap.clear();
		this.filterMap.clear();
		this.addComponentToMap4Container(rootContainer);
	}


	public void addComponentToMap4Container(Container container) {
		List<Container> allContainers = container.getAllContainers();
		for(Container c: allContainers) {
			//should we check duplciate name?
			if(this.containerMap.get(c.getName())!=null) {
				//change to a unique name
				//try containerName+"_"+parentContainerName first if the parent is not root
				String newName = c.getName();
				if(!c.getParentContainer().getName().equals("root")) {
					newName = c.getName()+"_"+c.getParentContainer().getName();
				}
				String nextName = McGuiUtils.INSTANCE.getNextUniqueContainerName(this,newName);
				Log.debug("change container " + c.getName() + " to "+nextName);
				c.setName(nextName);
			}
			this.containerMap.put(c.getName(), c);
		}
		List<Attribute> allAttributes = container.getAllAttributes(
				new ArrayList<String>(), true, true);
		for(Attribute att: allAttributes) {
			this.attributeMap.put(att.getName(), att);
		}
		List<Filter> allFilters = container.getAllFilters(
				new ArrayList<String>(), true, true);
		for(Filter filter: allFilters) {
			this.filterMap.put(filter.getName(), filter);
		}		
	}
	
	public void removeComponentFromMap4Container(Container container) {
		List<Attribute> allAttributes = container.getAllAttributes(
				new ArrayList<String>(), true, true);
		for(Attribute att: allAttributes) {
			this.attributeMap.remove(att.getName());
		}
		List<Filter> allFilters = container.getAllFilters(
				new ArrayList<String>(), true, true);
		for(Filter filter: allFilters) {
			this.filterMap.remove(filter.getName());
		}		
		List<Container> allContainers = container.getAllContainers();
		for(Container subC: allContainers) {
			this.containerMap.remove(subC.getName());
		}
	}


	public void setDefaultConfig(boolean isDefaultConfig) {
		this.setProperty(XMLElements.DEFAULT, Boolean.toString(isDefaultConfig));
	}

	public boolean isDefaultConfig() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.DEFAULT));
	}
	
	public void addToContainerMap(Container c) {
		this.containerMap.put(c.getName(), c);
	}
	
	public void addToFilterMap(Filter f) {
		this.filterMap.put(f.getName(), f);
	}
	
	public void removeFromFilterMap(Filter f) {
		this.filterMap.remove(f.getName());
	}
	
	public void addToAttributeMap(Attribute a) {
		this.attributeMap.put(a.getName(), a);
	}
	
	public void removeFromAttributeMap(Attribute a) {
		this.attributeMap.remove(a.getName());
	}
	
	public Attribute getAttributeByName(String name, Collection<String> range) {
		Attribute a = this.attributeMap.get(name);
		if(a==null)
			return null;
		if(!a.inPartition(range))
			return null;
		return a;
	}

	public Attribute getAttributeByInternalName(String name, Collection<String> range) {
		Attribute a = null;
		for(Attribute attr : this.attributeMap.values())
		{
			if(attr.getInternalName().equals(name)){
				a = attr;
				break;
			}
		}
		if(a==null)
			return null;
		if(!a.inPartition(range))
			return null;
		return a;
	}
	
	public Attribute getAttributeByName(Dataset dataset, String name,  boolean includeHiddenAttributes){
		List<String> dsl = new ArrayList<String>();
		if(dataset!=null)
			dsl.add(dataset.getName());
		Attribute att = this.getAttributeByName(name, dsl);
		if(att!=null) {
			if(includeHiddenAttributes) 
				return att;
			else 
				return att.isHidden()?null:att;
				
		}else {
			if(dataset == null)
				return null;			
			//check partitions
			for(String attName: this.attributeMap.keySet()) {
				if(McUtils.hasPartitionBinding(attName)) {
					List<String> ptRefList = McUtils.extractPartitionReferences(attName);
					//assume only one partition for now
					String ptRef = ptRefList.get(1);
					String ptName = McUtils.getPartitionTableName(ptRef);
					PartitionTable pt = this.getMart().getPartitionTableByName(ptName);
					//use first available row
					for(int i=0; i<pt.getTotalRows(); i++) {
						//for the same dataset only
						if(pt.getPartitionType()==PartitionType.SCHEMA) {
							if(!dataset.getName().equals(pt.getValue(i, PartitionUtils.DATASETNAME)))
								continue;
						}else {
							if(!dataset.getName().equals(pt.getValue(i,0)))
								continue;
						}
						String realName = McUtils.replacePartitionReferences(pt, i, ptRefList);
						if(realName !=null && realName.equals(name))  {
							Set<String> dss = new HashSet<String>();
							dss.add(dataset.getName());
							this.attributeMap.get(attName).setInSelectedDatasets(dss);
							return this.attributeMap.get(attName);	
						}
					}
				}
			}
		}
		return null;
	}

	
	public Filter getFilterByName(String name, Collection<String> range) {
		Filter f = this.filterMap.get(name);
		if(f == null)
			return null;
		if(!f.inPartition(range))
			return null;
		return f;
	}
	public Filter getFilterByInternalName(String name, Collection<String> range) {
		Filter f = null;
		for(Filter fil : this.filterMap.values()){
			if(fil.getInternalName().equals(name)){
				f = fil;
			}				
		}
		if(f == null)
			return null;
		if(!f.inPartition(range))
			return null;
		return f;
	}
	
	public Filter getFilterByName(Dataset dataset, String name, boolean includeHiddenFilters){
		List<String> dsl = new ArrayList<String>();
		if(dataset!=null)
			dsl.add(dataset.getName());

		Filter filter = this.getFilterByName(name, dsl);
		if(filter!=null) {
			if(includeHiddenFilters) 
				return filter;
			else 
				return filter.isHidden()?null:filter;
		}else {
			if(dataset == null)
				return null;
			//check partition
			for(String filName: this.filterMap.keySet()) {
				if(McUtils.hasPartitionBinding(filName)) {
					List<String> ptRefList = McUtils.extractPartitionReferences(filName);
					//assume only one partition for now
					String ptRef = ptRefList.get(1);
					String ptName = McUtils.getPartitionTableName(ptRef);
					PartitionTable pt = this.getMart().getPartitionTableByName(ptName);
					//use first available row
					for(int i=0; i<pt.getTotalRows(); i++) {
						//for the same dataset only
						if(pt.getPartitionType()==PartitionType.SCHEMA) {
							if(!dataset.getName().equals(pt.getValue(i, PartitionUtils.DATASETNAME)))
								continue;
						}else {
							if(!dataset.getName().equals(pt.getValue(i,0)))
								continue;
						}
						String realName = McUtils.replacePartitionReferences(pt, i, ptRefList);
						if(realName !=null && realName.equals(name)) {
							Set<String> dss = new HashSet<String>();
							dss.add(dataset.getName());
							this.filterMap.get(filName).setInSelectedDatasets(dss);
							return this.filterMap.get(filName);	
						}
					}					
				}
			}
		}
		return null;
	}


	@Override
	public boolean isHidden() {
		if(super.isHidden())
			return true;
		if(this.getParent() == null)
			return false;
		else
			return this.getParent().isHidden();
	}
	
	public List<Attribute> getAttributes(List<String> datasetList, boolean includeHiddenContainer, boolean includeHiddenAttributes) {
		return rootContainer.getAllAttributes(datasetList, includeHiddenContainer, includeHiddenAttributes);
	}
	
	public List<Filter> getFilters(List<String> datasetList, boolean includeHiddenContainer, boolean includeHiddenFilters) {
		return rootContainer.getAllFilters(datasetList, includeHiddenContainer, includeHiddenFilters);
	}
	
	public Collection<Filter> getAllFilters() {
		return this.filterMap.values();
	}
	

	@Override
	public List<MartConfiguratorObject> getChildren() {
		List<MartConfiguratorObject> children = new ArrayList<MartConfiguratorObject>();
		for(ElementList el: this.importableList)
			children.add(el);
		
		for(ElementList el: this.exportableList)
			children.add(el);
		
		children.add(this.rootContainer);
		return children;
	}

	public boolean isMasterConfig() {
		return "1".equals(this.getPropertyValue(XMLElements.MASTER)) || Boolean.parseBoolean(this.getPropertyValue(XMLElements.MASTER));
	}
	
	public void setMaster(boolean b) {
		this.setProperty(XMLElements.MASTER, Boolean.toString(b));
	}

	public void addLink(Link link) {
		if(!this.linkList.contains(link)) {
			this.linkList.add(link);
			link.setParent(this);
			this.setProperty(XMLElements.LINK, McUtils.listToStr(this.linkList, ","));
		}
	}
	
	public void addRDF(RDFClass rdf) {
		if(!this.rdflist.contains(rdf)) {
			this.rdflist.add(rdf);
			rdf.setParent(this);
			this.setProperty(XMLElements.RDFCLASS, McUtils.listToStr(this.rdflist, ","));
		}
	}
	
	public List<RDFClass> getRDFClasses() {
		return this.rdflist;
	}
	
	public void removeAllRDF() {
		this.rdflist.clear();
		this.setProperty(XMLElements.RDFCLASS, "");
        for (Filter filter : this.filterMap.values())
            filter.setRDF("");
        for (Attribute attribute : this.attributeMap.values())
            attribute.setRDF("");
	}
	public void removeRDF(RDFClass rdf) {
		if(this.rdflist.remove(rdf))
			this.setProperty(XMLElements.RDFCLASS, McUtils.listToStr(this.rdflist, ","));
	}

	
	public void removeLink(Link link) {
		if(this.linkList.remove(link))
			this.setProperty(XMLElements.LINK, McUtils.listToStr(this.linkList, ","));
	}

	public void clearLink() {
		this.linkList.clear();
		this.setProperty(XMLElements.LINK, "");
	}
	
	public List<Link> getLinkList() {
		return this.linkList;
	}

	public Collection<MartPointer> getReferencedMartPointer() {
		Set<MartPointer> mpSet = new HashSet<MartPointer>();
		List<MartPointer> mpList = this.getMart().getMartRegistry().getPortal().getRootGuiContainer().getAllMartPointerListResursively();
		for(MartPointer mp: mpList) {
			if(mp.getConfig().equals(this))
				mpSet.add(mp);
		}
		return mpSet;
	}

	public boolean isReadOnly() {
		return XMLElements.TRUE_VALUE.toString().equals(this.getPropertyValue(XMLElements.READONLY));
	}
	
	public void setReadOnly(boolean b) {
		this.setProperty(XMLElements.READONLY, Boolean.toString(b));
	}

	public Link getLinkByName(String name) {
		for(Link link: this.linkList) {
			if(name.equals(link.getName()))
				return link;
		}
		return null;
	}

	public boolean searchFromTarget() {
		return this.getMart().searchFromTarget();
	}

	public String getConsumerKey(String ds) {
		PartitionTable pt = this.getMart().getSchemaPartitionTable();
		int row = pt.getRowNumberByDatasetName(ds);
		int col = PartitionUtils.KEY;
		String value = pt.getValue(row, col);
		if(McUtils.isStringEmpty(value))
			return "";
		String[] keys = value.split(",");
		if(keys.length!=4)
			return "";
		else
			return keys[0];
	}
	
	public String getConsumerSecret(String ds) {
		PartitionTable pt = this.getMart().getSchemaPartitionTable();
		int row = pt.getRowNumberByDatasetName(ds);
		int col = PartitionUtils.KEY;
		String value = pt.getValue(row, col);
		if(McUtils.isStringEmpty(value))
			return "";
		String[] keys = value.split(",");
		if(keys.length!=4)
			return "";
		else
			return keys[1];
	}
	
	public String getDino() {
		Log.debug(this.getClass().getName() + "#getDino()");
		return dino;
	}
	
	public String getAccessKey(String ds) {
		PartitionTable pt = this.getMart().getSchemaPartitionTable();
		int row = pt.getRowNumberByDatasetName(ds);
		int col = PartitionUtils.KEY;
		String value = pt.getValue(row, col);
		if(McUtils.isStringEmpty(value))
			return "";
		String[] keys = value.split(",");
		if(keys.length!=4)
			return "";
		else
			return keys[2];
	}
	
	public String getAccessSecret(String ds) {
		PartitionTable pt = this.getMart().getSchemaPartitionTable();
		int row = pt.getRowNumberByDatasetName(ds);
		int col = PartitionUtils.KEY;
		String value = pt.getValue(row, col);
		if(McUtils.isStringEmpty(value))
			return "";
		String[] keys = value.split(",");
		if(keys.length!=4)
			return "";
		else
			return keys[3];
	}
	
	public boolean containLink(String name){
		for(Link link : this.getLinkList()){
			if(link.getName().equals(name))
				return true;
		}
		return false;
	}
	
	public boolean syncWithMasterconfig(){
		//check if it is master config itself
		if(this.isMasterConfig() || this.getMart() == null)
			return false;
		//only sync between derived config and its own master config
		Config masterConfig = this.getMart().getMasterConfig();
		if(masterConfig != null){
			//sync link first
			for(Link link : this.getLinkList()){
				if(masterConfig.getLinkByName(link.getName()) == null){
					masterConfig.addLink(link);
				}
			}
			//sync containers
			return this.rootContainer.syncWithMasterconfig();
		}
		return false;		
	}
	
	public boolean hasPointedAttribute(Attribute attr){
		//check if the pointer exist or not
		//String baseName = McGuiUtils.INSTANCE.getPointedAttributeName(attr);
		Attribute a = this.getAttributeByName(attr.getName(), null);
		if(a !=null){
			if(a.getPointedMartName().equals(attr.getParentConfig().getMart().getName())
					&& a.getPointedConfigName().equals(attr.getParentConfig().getName())
					&& attr.equals(a.getPointedAttribute()))
			{
				JOptionPane.showMessageDialog(null, "pointed attribute already exists!");
				return true;
			}
		}
		return false;
	}
	
	public boolean hasPointedFilter(Filter filter){
		//check if the pointer exist or not
		//String baseName = McGuiUtils.INSTANCE.getPointedFilterName(filter);
		Filter f = this.getFilterByName(filter.getName(), null);
		if(f !=null){
			if(f.getPointedMartName().equals(filter.getParentConfig().getMart().getName())
					&& f.getPointedConfigName().equals(filter.getParentConfig().getName())
					&& filter.equals(f.getPointedFilter()))
			{
				JOptionPane.showMessageDialog(null, "pointed filter already exists!");
				return true;
			}
		}
		return false;
	}
	
	public Collection<Attribute> getAllAttributes() {
		return this.attributeMap.values();
	}

	public Filter getFirstFilterByAttributeName(String attributeName) {
		for(Filter fil: this.getAllFilters()) {
			if(fil.getAttribute()!=null && fil.getAttribute().getName().equals(attributeName))
				return fil;
		}
		return null;
	}
	
	public boolean containAttributebyName(Attribute att){
		for(Attribute a : this.attributeMap.values()){
			if(a.getName().equals(att.getName()))
				return true;
		}
		return false;
	}
	
	public boolean containFilterByName(Filter filter){
		for(Filter f:this.filterMap.values()){
			if(f.getName().equals(filter.getName()))
				return true;
		}
		return false;
	}

    /*
     * TODO: (jhsu - 03/30/2011) Need to return actual config for processor
     * 
     * This is just a stub for now, but it needs to be completed ASAP
     *
     */
    public Map<String,String> getProcessorConfig() {
        return Collections.<String,String>emptyMap();
    }

}
