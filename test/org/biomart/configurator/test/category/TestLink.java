package org.biomart.configurator.test.category;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

import org.biomart.api.MartApi;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.MessageConfig;
import org.biomart.configurator.view.gui.dialogs.MatchDatasetDialog;
import org.biomart.configurator.view.menu.McMenus;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.portal.UserGroup;
import org.biomart.queryEngine.QueryController;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class TestLink extends TestQuery{

	public boolean testCreateLink(String mart1,String sourceAttrName,List<String> sourceAttrList,List<String> sourceFilterList, String mart2,String targetAttrName,List<String> targetAttrList,List<String> targetFilterList){
		List<Mart> marts = McGuiUtils.INSTANCE.getRegistryObject().getMartList();
		Mart sourceMart=null;
		Mart targetMart=null;
		for(Mart mart: marts){
			if(mart.getName().equals(mart1)){
				sourceMart = mart;
			}
			if(mart.getName().equals(mart2)){
				targetMart = mart;
			}
		}
		if(sourceMart == null || targetMart == null)
			return false;
		//check if dataset matching is 1 to 1 or multiple first
		List<Dataset> sourceDataset = sourceMart.getDatasetList();
		List<Dataset> targetDataset = targetMart.getDatasetList();
		
		//create pointed attribute list
		Config sourceConfig = sourceMart.getMasterConfig();
		Config targetConfig = targetMart.getMasterConfig();
		
		List<Object> sourcePtrList = new ArrayList<Object>();
		
		//add pointed attribute to all the configs
		for(String attrName : sourceAttrList){
			sourcePtrList.add(sourceConfig.getAttributeByName(attrName, null));		
		}
		for(String filterName: sourceFilterList){
			sourcePtrList.add(sourceConfig.getFilterByName(filterName, null));
		}
		
		List<Object> targetPtrList = new ArrayList<Object>();
		for(String attrName : targetAttrList){
			targetPtrList.add(targetConfig.getAttributeByName(attrName, null));		
		}
		for(String filterName: targetFilterList){
			targetPtrList.add(targetConfig.getFilterByName(filterName, null));
		}
		
		if(sourceDataset.size() >1 && targetDataset.size() >1) {
			Map<Dataset,List<Dataset>> sourceDatasetMapping = new Hashtable<Dataset,List<Dataset>>();
			Map<Dataset,List<Dataset>> targetDatasetMapping = new Hashtable<Dataset,List<Dataset>>();
			int createdSourceCol;
			int createdTargetCol;
			
			//matching according to display names for source dataset
			for(Dataset sourceDs: sourceDataset){
				int targetIndex = Collections.binarySearch(targetDataset, sourceDs);
				if(targetIndex <0 || targetIndex >=targetDataset.size())
					continue;
				Dataset targetDs = targetDataset.get(targetIndex);
				//int targetIndex = this.targetDataset.indexOf(targetDs);
				if(sourceDatasetMapping.containsKey(sourceDs)){
					//if dataset mapping already has the source mapping, then add to the target list
					sourceDatasetMapping.get(sourceDs).add(targetDs);
				}else{
					//create a new list and add target to the list
					List<Dataset> targetList = new ArrayList<Dataset>();
					targetList.add(targetDs);
					sourceDatasetMapping.put(sourceDs, targetList);
				}
			}
			//do the same for target dataset
			for(Dataset targetDs: targetDataset){
				int sourceIndex = Collections.binarySearch(sourceDataset, targetDs);
				if(sourceIndex < 0 || sourceIndex >= sourceDataset.size())
					continue;
				Dataset sourceDs = sourceDataset.get(sourceIndex);
				
				if(targetDatasetMapping.containsKey(targetDs)){
					//if dataset mapping already has the source mapping, then add to the target list
					targetDatasetMapping.get(targetDs).add(sourceDs);
				}else{
					//create a new list and add source to the list
					List<Dataset> sourceList = new ArrayList<Dataset>();
					sourceList.add(sourceDs);
					targetDatasetMapping.put(targetDs, sourceList);
				}
			}
			
			PartitionTable sourcePT = sourceMart.getSchemaPartitionTable();
			PartitionTable targetPT = targetMart.getSchemaPartitionTable();
			createdSourceCol = sourcePT.addColumn("");
			for(Dataset sourceDs : sourceDataset){
				int srow = sourcePT.getRowNumberByDatasetName(sourceDs.getName());
				StringBuilder value = new StringBuilder();
				List<Dataset> targetDSs = sourceDatasetMapping.get(sourceDs);
				if(targetDSs == null){
					continue;
				}
				for(Dataset targetDs : targetDSs) {
					value.append(targetDs.getName());
					if(targetDSs.indexOf(targetDs) != targetDSs.size()-1)
						value.append(',');
				}
				
				sourcePT.setValue(srow, createdSourceCol, value.toString());
			}
			//create a col in the target partition table
			createdTargetCol = targetPT.addColumn("");
			for(Dataset targetDs : targetDataset){
				int trow = targetPT.getRowNumberByDatasetName(targetDs.getName());
				StringBuilder value = new StringBuilder();
				List<Dataset> sourceDSs = targetDatasetMapping.get(targetDs);
				if(sourceDSs == null){
					continue;
				}
				for(Dataset sourceDs : sourceDSs){
					value.append(sourceDs.getName());
					if(sourceDSs.indexOf(sourceDs) != sourceDSs.size()-1)
						value.append(',');
				}
				targetPT.setValue(trow, createdTargetCol, value.toString());
			}
			
			//create link based on attribute list
			createLink(1,createdTargetCol, createdSourceCol,sourceMart,sourceAttrName,sourcePtrList,targetMart,targetAttrName,targetPtrList);
		}
		else{
			//create link based on attribute list
			createLink(1, 0,0,sourceMart,sourceAttrName,sourcePtrList,targetMart,targetAttrName,targetPtrList);
		}
		
		return true;
	}

	public boolean createPointers(Link link, List<Object> nodeList, Config sourceConfig, Config targetConfig){
		//create a copy the source to the target, replace attribute with pointers
		for(Object node: nodeList) {
			if(node instanceof Attribute) {
				Attribute attr = (Attribute)node;
				
				/*if(targetConfig.getMart().getMasterConfig().hasPointedAttribute(attr))
					continue;*/
				if(targetConfig.hasPointedAttribute(attr))
					continue;
				String baseName = McUtils.getUniqueAttributeName(targetConfig, attr.getName());
				//baseName = McGuiUtils.INSTANCE.getPointedAttributeName(attr);
	    		Attribute attPointer = new Attribute(baseName,attr);
	    		//use the old displayname
	    		attPointer.setDisplayName(attr.getDisplayName());
	    		
	    		Container targetCon = targetConfig.getRootContainer();
	    		if(targetConfig.isMasterConfig()){
	    			Container newCon = targetConfig.getContainerByName("newAttributePointer");
	    			if(newCon == null){
	    				newCon = new Container("newAttributePointer");
	    				targetConfig.getRootContainer().addContainer(newCon);
	    			}
	    			newCon.addAttribute(attPointer);
	    		}else{
	    			targetCon.addAttribute(attPointer);
	    		}
	    		//((Container)newParentNode.getObject()).addAttribute(attPointer);
	    		//set pointed dataset
	    		if(link != null)
	    			attPointer.addPointedDataset(link.getPointedDataset());
	    			//attPointer.setPointedDatasetName(link.getPointedDataset());
	    		
	    		attPointer.synchronizedFromXML();
	    		
			}else if(node instanceof Filter) {
				Filter filter = (Filter)node;
				/*
				if(targetConfig.getMart().getMasterConfig().hasPointedFilter(filter))
					continue;*/
				if(targetConfig.hasPointedFilter(filter))
					continue;
				//get unique name
				String baseName = McGuiUtils.INSTANCE.getUniqueFilterName(targetConfig, filter.getName());
	    		//baseName = McGuiUtils.INSTANCE.getPointedFilterName(filter);
				
	    		Filter filPointer = new Filter(baseName,filter.getName(),sourceConfig.getName());
	    		//filPointer.setPointedInfo(draggedNodes.get(0).getObject().getName(), 
	    			//	leftDs.getName(), config.getName(), config.getMart().getName());
	    		filPointer.setDisplayName(filter.getDisplayName());
	    		filPointer.setPointedElement(filter);
	    		Container targetCon = targetConfig.getRootContainer();
	    		if(targetConfig.isMasterConfig()){
	    			Container newCon = targetConfig.getContainerByName("newFilterPointer");
	    			if(newCon == null){
	    				newCon = new Container("newFilterPointer");
	    				targetConfig.getRootContainer().addContainer(newCon);
	    			}
	    			newCon.addFilter(filPointer);
	    		}else{
	    			targetCon.addFilter(filPointer);
	    		}
	    		//((Container)newParentNode.getObject()).addFilter(filPointer);
	    		if(link != null)
	    			filPointer.addPointedDataset(link.getPointedDataset());
	    			//filPointer.setPointedDatasetName(link.getPointedDataset());
	    		
	    		filPointer.synchronizedFromXML();
	    		
			}else if(node instanceof Container) {
				try {
					boolean rename = false;
					if(targetConfig.getContainerByName(((MartConfiguratorObject)node).getName())!=null) {
						int n = JOptionPane.showConfirmDialog(
							    null,
							    "Container already exist, create a new container?",
							    "Question",
							    JOptionPane.YES_NO_OPTION);
						if(n==0) {
							rename = true;
						}else
							return false;
					}
					Container oldCon = (Container)node;
					Element containerElement = ((MartConfiguratorObject)node).generateXml();
					Container newCon = new Container(containerElement);
					//recursively set all child nodes to the same config
					List<Attribute> attrList = oldCon.getAllAttributes(null, true, true);
					//List<Attribute> newAttList = newCon.getAllAttributes(null, true, true);
					for(Attribute attr : attrList) {
						Attribute newAttr = newCon.getAttributeByName(attr.getName());
						/*
						if(targetConfig.getMart().getMasterConfig().hasPointedAttribute(attr)){
							newCon.removeAttribute(newAttr);
							continue;							
						}*/
						if(targetConfig.hasPointedAttribute(attr)){
							newCon.removeAttribute(newAttr);
							continue;
						}
						
						
						newAttr.setConfig(targetConfig);
						newAttr.setParent(newCon);
						newAttr.setPointer(true);
						newAttr.setPointedElement(attr);
						newAttr.setPointedConfigName(sourceConfig.getName());
						newAttr.setPointedMartName(sourceConfig.getMart().getName());
						if(link != null)
							newAttr.addPointedDataset(link.getPointedDataset());
							//newAttr.setPointedDatasetName(link.getPointedDataset());
						
						//newAttr.synchronizedFromXML();
						
					}
					List<Filter> filterList = oldCon.getAllFilters(null, true, true);
					for(Filter filter : filterList) {
						Filter newFilter = newCon.getFilterByName(filter.getName());
						/*
						if(targetConfig.getMart().getMasterConfig().hasPointedFilter(filter)){
							newCon.removeFilter(newFilter);
							continue;
						}*/
						if(targetConfig.hasPointedFilter(filter)){
							newCon.removeFilter(newFilter);
							continue;
						}
						
						
						newFilter.setConfig(targetConfig);
						newFilter.setParent(newCon);
						newFilter.setPointer(true);
						newFilter.setPointedElement(filter);
						newFilter.setPointedConfigName(sourceConfig.getName());
						newFilter.setPointedMartName(sourceConfig.getMart().getName());
						if(link != null)
							newFilter.addPointedDataset(link.getPointedDataset());
							//newFilter.setPointedDatasetName(link.getPointedDataset());
						
						//newFilter.synchronizedFromXML();							
					}
					
					if(rename) {
						//TODO
						
					}
					if(!newCon.isEmpty()){
						Container targetCon = targetConfig.getRootContainer();
			    		targetCon.addContainer(newCon);
						//((Container)newParentNode.getObject()).addContainer(newCon);
						newCon.synchronizedFromXML();
					}
				} catch (FunctionalException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	public void createLink(int numSourceAttr, int sourceCol, int targetCol, Mart sourceMart,String sourceAttrName,List<Object> sourceList, Mart targetMart,String targetAttrName,List<Object> targetList) {
		List<Dataset> sourceDataset = sourceMart.getDatasetList();
		List<Dataset> targetDataset = targetMart.getDatasetList();
		
		Config sourceConfig = sourceMart.getMasterConfig();
		Config targetConfig = targetMart.getMasterConfig();
		
		Link sourceLink = new Link(McUtils.getLinkName(sourceConfig, targetConfig));		
		Link targetLink = new Link(McUtils.getLinkName(targetConfig, sourceConfig));
		
		
		sourceLink.setPointerMart(targetConfig.getMart());
		sourceLink.setPointedConfig(targetConfig.getMart().getMasterConfig());		
		
		targetLink.setPointerMart(sourceConfig.getMart());
		targetLink.setPointedConfig(sourceConfig.getMart().getMasterConfig());
		//create pointed dataset based on source dataset size and target dataset size
		if(sourceDataset.size() >1 && targetDataset.size() >1) {
			//create pointer for pointed dataset
			sourceLink.setPointedDataset("(p0c"+Integer.toString(targetCol)+")");
			targetLink.setPointedDataset("(p0c"+Integer.toString(sourceCol)+")");
		}else{
			//create names for pointed dataset
			StringBuilder sDatasetName = new StringBuilder();
			for(Dataset ds:targetDataset){
				sDatasetName.append(ds.getName());
				if(targetDataset.indexOf(ds) != targetDataset.size()-1)
					sDatasetName.append(',');
			}
			sourceLink.setPointedDataset(sDatasetName.toString());
			
			StringBuilder tDatasetName = new StringBuilder();
			for(Dataset ds:sourceDataset){
				tDatasetName.append(ds.getName());
				if(sourceDataset.indexOf(ds) != sourceDataset.size()-1)
					tDatasetName.append(',');
			}
			targetLink.setPointedDataset(tDatasetName.toString());
		}
		
		for(int i=0;i<numSourceAttr;i++){						
			Attribute sourceAttr = sourceConfig.getAttributeByName(sourceAttrName, null);
			Attribute targetAttr = targetConfig.getAttributeByName(targetAttrName, null);
			if(sourceAttr != null && targetAttr != null){
				//add source attr and target filter to source link
				sourceLink.addAttribute(sourceAttr);
				List<Filter> srefs = targetAttr.getReferenceFilters();
				for(MartConfiguratorObject ref : srefs){						
					sourceLink.addFilter((Filter)ref);
					break;//only add the first one for now
				}
				//add source filter and target attr to target link
				targetLink.addAttribute(targetAttr);
				List<Filter> trefs = sourceAttr.getReferenceFilters();
				for(MartConfiguratorObject ref : trefs){						
					targetLink.addFilter((Filter)ref);
					break;
				}				
			}			
		}
		//add link to all the configs
		try{
			List<Config> sconfigs = sourceMart.getConfigList();
			for(Config config : sconfigs){
				org.jdom.Element e = sourceLink.generateXml();
				Link link = new Link(e);
				config.addLink(link);
				link.synchronizedFromXML();				
			}
			List<Config> tconfigs = targetMart.getConfigList();
			for(Config config : tconfigs){
				org.jdom.Element e = targetLink.generateXml();
				Link link = new Link(e);
				config.addLink(link);
				link.synchronizedFromXML();				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		List<Config> tconfigs = targetMart.getConfigList();
		for(Config config : tconfigs){
			this.createPointers(targetLink, sourceList, sourceConfig, config);
		}
		
		List<Config> sconfigs = sourceMart.getConfigList();
		for(Config config : sconfigs){
			this.createPointers(sourceLink, targetList, targetConfig, config);
		}
	}
	
	@Override
	public boolean test() {
		boolean result = false;
    	this.testNewPortal();
    	this.testAddMart(testName);
    	
		Element e = SettingsForTest.getTestCase(testName);
		
		//create link between tow mart
    	List<String> sourceAttrList = new ArrayList<String>();
    	List<String> sourceFilterList = new ArrayList<String>();
    	List<String> targetAttrList = new ArrayList<String>();
    	List<String> targetFilterList = new ArrayList<String>();
    	String sourceMart ="";
		String sourceAttribute ="";
		String targetMart="";
		String targetAttribute="";
		
		
		@SuppressWarnings("unchecked")
		List<Element> createElements = e.getChildren("create");
		for(Element createElement: createElements) {
			
			String type = createElement.getAttributeValue("type");
			if(type.equals("link")){
				Element sourceElement = createElement.getChild("source");
				sourceMart = sourceElement.getAttributeValue("mart");
				sourceAttribute = sourceElement.getAttributeValue("attribute");
				List<Element> sourceAttributeListElement = sourceElement.getChildren("attribute");
				for(Element attrElement : sourceAttributeListElement){
					sourceAttrList.add(attrElement.getAttributeValue("name"));
				}
				List<Element> sourceFilterListElement = sourceElement.getChildren("filter");
				for(Element filterElement : sourceFilterListElement){
					sourceFilterList.add(filterElement.getAttributeValue("name"));
				}
				Element targetElement = createElement.getChild("target");
				targetMart = targetElement.getAttributeValue("mart");
				targetAttribute = targetElement.getAttributeValue("attribute");
				List<Element> targetAttributeListElement = targetElement.getChildren("attribute");
				for(Element attrElement : targetAttributeListElement){
					targetAttrList.add(attrElement.getAttributeValue("name"));
				}
				List<Element> targetFilterListElement = targetElement.getChildren("filter");
				for(Element filterElement: targetFilterListElement){
					targetFilterList.add(filterElement.getAttributeValue("name"));
				}
			}
		}
    	
		result = this.testCreateLink(sourceMart, sourceAttribute , sourceAttrList,sourceFilterList, targetMart, targetAttribute, targetAttrList,targetFilterList);
		
		//sourceAttrList.add("stableidentifier_identifier");
		//sourceFilterList.add("_displayname");
		//targetAttrList.add("vega_gene_id");
		//targetFilterList.add("chromosome_name");  
    	//result = this.testCreateLink("pathway", "referencedatabase_ensembl" , sourceAttrList,sourceFilterList, "hsapiens_gene_vega","ens_hs_gene", targetAttrList,targetFilterList);
    	
    	if(!result)
    		return result;
    	this.testSaveXML(testName);
    	//compare xml
    	//result = this.compareXML(testName);
    	//if(!result)
    	//	return result;
    	try {
			this.testQuery(testName);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JDOMException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	return this.compareQuery(testName);
    		
	}
	
	
}
