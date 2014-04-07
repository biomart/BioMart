package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.biomart.common.resources.Log;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.ValidationStatus;


public abstract class Element extends MartConfiguratorObject {

	//for those incomplete attributes, need to know which config they are in pointedConfig
	/*
	 * if an element has partition info, need to remember the row, the row will changed within the context
	 */
	protected Set<String> inSelectedDatasets;
			
	public void setConfig(Config config) {
		//update property
		if(config!=null) {
			this.setProperty(XMLElements.CONFIG, config.getName());
			this.setProperty(XMLElements.MART, config.getMart().getName());
		}
	}
	
	public Boolean isPointer() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.POINTER));
	}
	
	public void setPointedElement(Element pointedElement) {
		if(this instanceof Attribute) {
			this.setProperty(XMLElements.POINTEDATTRIBUTE, pointedElement.getName());
			this.setProperty(XMLElements.POINTEDCONFIG, pointedElement.getParentConfig().getName());
			this.setProperty(XMLElements.POINTEDMART, pointedElement.getParentConfig().getMart().getName());
		}
		else {
			this.setProperty(XMLElements.POINTEDFILTER, pointedElement.getName());
			this.setProperty(XMLElements.POINTEDCONFIG, pointedElement.getParentConfig().getName());
			this.setProperty(XMLElements.POINTEDMART, pointedElement.getParentConfig().getMart().getName());
		}
		this.setObjectStatus(ValidationStatus.VALID);
	}
	
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		Element element=(Element)object;
		return (
			(super.equals(element)) &&
			(this.getClass().equals(object.getClass())) 
		);
	}

	public void setHideValueInString(String value) {
		this.setProperty(XMLElements.HIDE, value);
	}
	
	public Element(String name) {
		super(name);
	}
	
	public Element(org.jdom.Element element) {
		super(element);
	}
	
	public Container getParentContainer() {
		return (Container)this.parent;
	}
				
	public void setPointer(boolean b) {
		this.setProperty(XMLElements.POINTER, Boolean.toString(b));
	}

	public void setInSelectedDatasets(Set<String> inSelectedDs) {
		this.inSelectedDatasets = inSelectedDs;
	}

	@Override
	public String toString() {
		if(McUtils.isCollectionEmpty(this.inSelectedDatasets)) {
			return super.toString();
		}else {
			//is displayname has partition value?
			if(McUtils.hasPartitionBinding(this.getPropertyValue(XMLElements.DISPLAYNAME))) {
				//return the value in the first dataset
				String dsName = this.inSelectedDatasets.iterator().next();
				Dataset ds = this.getParentConfig().getMart().getDatasetByName(dsName);
				return McUtils.getRealName(this.getPropertyValue(XMLElements.DISPLAYNAME), ds);
			}else
				return super.toString();			
		}			
	}

	public String getPointedMartName() {
		return this.getPropertyValue(XMLElements.POINTEDMART);
	}

	public Mart getPointedMart() {
		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDMART)))
			return null;
		return this.getParentConfig().getMart().getMartRegistry().getMartByName(this.getPropertyValue(XMLElements.POINTEDMART));
	}

	public Dataset getPointedDataset(Dataset sourceDataset) {
		if(sourceDataset == null) {
			if(McUtils.hasPartitionBinding(this.getPropertyValue(XMLElements.POINTEDDATASET))) 
				return null;
		}else {
			String dsName = null;
			String tmpDsName = this.getPropertyValue(XMLElements.POINTEDDATASET);
			if(McUtils.hasPartitionBinding(tmpDsName)) {
				//find the row for the pointeddataset
				//find the row
				int row = sourceDataset.getParentMart().getSchemaPartitionTable().getRowNumberByDatasetName(sourceDataset.getName());
				PartitionTable pt = sourceDataset.getParentMart().getSchemaPartitionTable();
				String newDsName = McUtils.getRealName(pt, row,tmpDsName);
				Mart mart = this.getPointedMart();
				if(mart == null)
					return null;
				return mart.getDatasetByName(newDsName);
				
			}else
				dsName = tmpDsName;
			Mart mart = sourceDataset.getParentMart().getMartRegistry().getMartByName(this.getPropertyValue(XMLElements.POINTEDMART));
			return mart.getDatasetByName(dsName);
		} 
		return null;
	}

 	public String getPointedConfigName() {
		return this.getPropertyValue(XMLElements.POINTEDCONFIG);
	}
 	
 	public Config getPointedConfing() {
 		if("".equals(this.getPropertyValue(XMLElements.POINTEDCONFIG))) 
 			return null;
 		Mart pointedMart = this.getPointedMart();
 		if(pointedMart == null)
 			return null;
 		return pointedMart.getConfigByName(this.getPropertyValue(XMLElements.POINTEDCONFIG));
 	}
 	
	public void addPointedDataset(Dataset ds) {
		if(ds == null) {
			Log.debug("pointed attribute error ********** " + this.getName());
			return;
		}
		List<String> pdList = this.getPointedDatasetList();
		if(!pdList.contains(ds.getName())) {
			pdList.add(ds.getName());
			//synchronize pointedDatasetName
			this.setProperty(XMLElements.POINTEDDATASET, McUtils.StrListToStr(pdList, ","));
		}
	}
	
	public void addPointedDataset(String ds) {
		List<String> pdList = this.getPointedDatasetList();
		try{
		if(pdList.isEmpty() || !pdList.contains(ds)) {
			pdList.add(ds);
			//synchronize pointedDatasetName
			this.setProperty(XMLElements.POINTEDDATASET, McUtils.StrListToStr(pdList, ","));
		}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public List<String> getPointedDatasetList() {
		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDDATASET)))
			return new ArrayList<String>();
		String[] pointedDatasets = this.getPropertyValue(XMLElements.POINTEDDATASET).split(",");
		return new ArrayList<String>(Arrays.asList(pointedDatasets));
	}

	public boolean isPointerInSource() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.POINTERINSOURCE));
	}
	
	public void setPointerInSource(boolean b) {
		this.setProperty(XMLElements.POINTERINSOURCE, Boolean.toString(b));
	}
}
