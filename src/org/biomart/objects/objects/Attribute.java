package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.MartConfiguratorUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.AttributeDataType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;


/**
 * TODO consider composite patter later
 *
 */
public class Attribute extends Element implements Comparable<Attribute> {
	
    
    private void propagateProperty(XMLElements pro, String value) {
        // This is a hack to avoid a NullPointerException while propagating a property
        //  at MartConfigurator boot-time.
        if (pro.equals(XMLElements.FUNCTION) && this.isAttributeList() && this.getParentConfig() != null) {
            List<Attribute> atts = this.getAttributeList();
            for (Attribute a : atts) {
                a.setProperty(pro, value);
            }
        }
    }
    
    @Override
    public void setProperty(XMLElements pro, String value) {
        super.setProperty(pro, value);
        
        propagateProperty(pro, value);
    }
    
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.getDisplayName();
	}

    public void setValue(String value) {
        this.setProperty(XMLElements.VALUE, value);
    }

	public String getValue(){
		return this.getPropertyValue(XMLElements.VALUE);
	}

	
	/**
	 * hack for pseudo attribute, assume that it always in main table
	 * @param dataset
	 * @return
	 */
	public String getValue(Dataset dataset){
		List<String> valueList = McUtils.extractPartitionReferences(this.getPropertyValue(XMLElements.VALUE));
		if(valueList.size()<=1)
			return this.getPropertyValue(XMLElements.VALUE);
		else {
			String ptRef = valueList.get(1);
			String ptName = McUtils.getPartitionTableName(ptRef);
			PartitionTable pt = this.getParentConfig().getMart().getPartitionTableByName(ptName);
			if(pt == null) {
				Log.debug(Resources.get("INVALIDOBJECT",this.getName()));
			} else {
				int row = this.getParentConfig().getMart().getDatasetRowNumber(dataset);
				
				String value = McUtils.replacePartitionReferences(pt, row, valueList);
				if(McUtils.isStringEmpty(value))
					return "no data";
				else
					return value;
			}
		}
		return this.getPropertyValue(XMLElements.VALUE);
	}
	
	
	/**
	 * the hashcode will use super.hashcode()
	 */
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		Attribute attribute=(Attribute)object;
		boolean sameconfig = true;
		Config con1 = attribute.getParentConfig();
		Config con2 = this.getParentConfig();
		if(null!=con1) {
			if(null!=con2)
				sameconfig = con1.getName().equals(con2.getName());
			else
				sameconfig = false;
		}else {
			if(null!=con2)
				sameconfig = false;
			else
				sameconfig = true;
		}
		return this.getName().equals(attribute.getName()) && sameconfig;
	}
	
	
	public org.jdom.Element generateXml() {
		org.jdom.Element element = new org.jdom.Element(XMLElements.ATTRIBUTE.toString());
		super.saveConfigurableProperties(element);
		MartConfiguratorUtils.addAttribute(element, XMLElements.MART.toString(), this.getParentConfig().getMart().getName());
		MartConfiguratorUtils.addAttribute(element, XMLElements.CONFIG.toString(), this.getParentConfig().getName());
		MartConfiguratorUtils.addAttribute(element, XMLElements.DEFAULT.toString(), this.getPropertyValue(XMLElements.DEFAULT));
		MartConfiguratorUtils.addAttribute(element, XMLElements.POINTER.toString(), this.isPointer().toString());
		// add serialization to datatype
		MartConfiguratorUtils.addAttribute(element, XMLElements.DATATYPE.toString(), this.getPropertyValue(XMLElements.DATATYPE));
		
		element.setAttribute(XMLElements.FUNCTION.toString(), this.getPropertyValue(XMLElements.FUNCTION));

		if (this.isPointer()) {
			element.setAttribute(XMLElements.POINTEDATTRIBUTE.toString(),this.getPropertyValue(XMLElements.POINTEDATTRIBUTE));
			element.setAttribute(XMLElements.POINTEDDATASET.toString(),this.getPropertyValue(XMLElements.POINTEDDATASET));
			element.setAttribute(XMLElements.POINTEDMART.toString(), this.getPropertyValue(XMLElements.POINTEDMART));
			element.setAttribute(XMLElements.POINTEDCONFIG.toString(),this.getPropertyValue(XMLElements.POINTEDCONFIG));							
		}else {
			MartConfiguratorUtils.addAttribute(element, XMLElements.COLUMN.toString(), this.getPropertyValue(XMLElements.COLUMN));
			MartConfiguratorUtils.addAttribute(element, XMLElements.TABLE.toString(), this.getPropertyValue(XMLElements.TABLE));
		}
		return element;
	}

	/**
	 * create an attribute pointer to an existing attribute
	 * @param pointedAttribute
	 * @param name
	 */
	public Attribute(String name, Attribute pointedAttribute) {
		super(name);
		if(pointedAttribute == null) {
			Log.debug("error **** pointedAttribute is null for attribute "+name);
			this.setObjectStatus(ValidationStatus.INVALID);
		}else
			this.setObjectStatus(ValidationStatus.VALID);
		this.setPointedElement(pointedAttribute);
		this.setNodeType(McNodeType.ATTRIBUTE);
		
		this.setPointer(true);
	}
	
	
	/**
	 * for attribute list
	 * @param name
	 * @param displayName
	 */
	public Attribute(String name, String displayName) {
		super(name);
		this.setNodeType(McNodeType.ATTRIBUTE);
		this.setDisplayName(displayName);
	}
	
	/*
	 * for pointed attribute
	 */
	public Attribute(String name, String pointedAttributeName,String pointedDatasetName) {
		super(name);
		this.setProperty(XMLElements.POINTEDATTRIBUTE, pointedAttributeName);
		this.setProperty(XMLElements.POINTEDDATASET, pointedDatasetName);
		this.setPointer(true);
		this.setObjectStatus(ValidationStatus.INVALID);
		this.setNodeType(McNodeType.ATTRIBUTE);
	}
	
	public Attribute(org.jdom.Element element) {
		super(element);
		this.setNodeType(McNodeType.ATTRIBUTE);
	}

	public Attribute(DatasetColumn dsColumn, String name) {
		super(name);
		if(dsColumn==null) {
			Log.debug("error *** datasetcolumn is null for attribute "+name);
			this.setObjectStatus(ValidationStatus.INVALID);
		} else {
			this.updateDatasetColumn(dsColumn);
			this.setObjectStatus(ValidationStatus.VALID);
		}
		
		this.setNodeType(McNodeType.ATTRIBUTE);
	}
	
	public Attribute(DatasetColumn dsColumn, String name, String displayName) {
		this(name, displayName);
		if(dsColumn==null) {
			Log.debug("error *** datasetcolumn is null for attribute "+name);
			this.setObjectStatus(ValidationStatus.INVALID);
		} else {
			this.updateDatasetColumn(dsColumn);
			this.setObjectStatus(ValidationStatus.VALID);
		}
		
		this.setNodeType(McNodeType.ATTRIBUTE);
	}
	
	public Attribute getPointedAttribute() {
		Config pointedConfig = this.getPointedConfing();
		if(pointedConfig == null || McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDATTRIBUTE))) 
			return null;
		return pointedConfig.getAttributeByName(this.getPropertyValue(XMLElements.POINTEDATTRIBUTE), new ArrayList<String>());
	}
	
	
	public String getFunction() {
	    return this.getPropertyValue(XMLElements.FUNCTION);
	}
	
	
	public String getPointedAttributeName() {
		return this.getPropertyValue(XMLElements.POINTEDATTRIBUTE);
	}
	
	public String getPointedDatasetName() {
		return this.getPropertyValue(XMLElements.POINTEDDATASET);
	}
	
	public DatasetColumn getDataSetColumn() {
		if(this.isPointer()) {
			Attribute pointedAttribute = this.getPointedAttribute();
			if(pointedAttribute == null)
				return null;
			if(pointedAttribute.isPointer()) {
				Log.debug("nested pointer");
				return null;
			}
			return pointedAttribute.getDataSetColumn();			
		} else {	
			DatasetTable table =  this.getParentConfig().getMart().getTableByName(this.getPropertyValue(XMLElements.TABLE));
			if(table!=null)
				return table.getColumnByName(this.getPropertyValue(XMLElements.COLUMN));
			else
				return null;
		}
	}
	
	public String getHideString() {
		return this.getPropertyValue(XMLElements.HIDE);
	}

	public void updateDatasetColumn(DatasetColumn dsc) {
		if(this.isPointer())
			return ;
		if(dsc!=null) {
			this.setDatasetTable((DatasetTable)dsc.getTable());
			this.setProperty(XMLElements.COLUMN, dsc.getName());
		}else {
			this.setProperty(XMLElements.COLUMN, "");
		}
	}
	

	/**
	 * check if the attribute exists in partition.
	 * @param partitionValue
	 * @return
	 */
	public boolean inPartition(String partitionValue) {
		//is it a pointer
/*		if(this.isPointer() && this.getPointedAttribute()!=null)
			return this.getPointedAttribute().inPartition(partitionValue);
		if(this.getDataSetColumn() == null) //should not happen
			return false;
		return this.getDataSetColumn().inPartition(partitionValue);
		*/
		List<String> range = new ArrayList<String>();
		range.add(partitionValue);
		return this.inPartition(range);
	}
	
	public boolean inPartition(Collection<String> values) {
		if(values == null || values.isEmpty())
			return true;
		//is it a pointer, if the pointed dataset is not partitioned, return true
		if(this.isPointer() && this.getPointedAttribute()!=null) {
			if(McUtils.hasPartitionBinding(this.getPointedDatasetName())) {
				//get the otherside's partitions according to the source partitions
				Attribute targetAttribute = this.getPointedAttribute();
				if(targetAttribute.getObjectStatus()!=ValidationStatus.VALID) {
					return false;
				}
				//get the partition
				int col = McUtils.getPartitionColumnValue(this.getPointedDatasetName());
				Set<String> targetPartitions = new HashSet<String>();
				//assume they are in schemapartitiontable
				PartitionTable pt = this.getParentConfig().getMart().getSchemaPartitionTable();
				for(String sourceDsStr: values) {
					int row = pt.getRowNumberByDatasetName(sourceDsStr);
					if(row>=0) {
						String newPt = pt.getValue(row, col);
						if(McUtils.isStringEmpty(newPt))
							continue;
						String[] _newDsStrs = newPt.split(",");
						for(String item: _newDsStrs) {
							targetPartitions.add(item);
						}
					}
				}	
				/*
				 * no value in the row, which means for this input datasets, they are not valid
				 */
				if(targetPartitions.isEmpty())
					return false;
				else
					return targetAttribute.inPartition(targetPartitions);
			}					
			else
				return true;
		}
			//return this.getPointedAttribute().inPartition(values);
		if(!this.getAttributeList().isEmpty()) {
			return true;
		}
		if(this.isPseudoAttribute())
			return true;
		if(this.getDataSetColumn() == null) //should not happen
			return false;
		return this.getDataSetColumn().inPartition(values);		
	}


	public DatasetTable getDatasetTable() {
		if(this.isPointer()) {
			Attribute pointedAttribute = this.getPointedAttribute();
			if(pointedAttribute == null)
				return null;
			return pointedAttribute.getDatasetTable();
		} else
			return this.getParentConfig().getMart().getTableByName(this.getPropertyValue(XMLElements.TABLE));
	}
		
	public boolean isPseudoAttribute() {
		return !McUtils.isStringEmpty(this.getPropertyValue(XMLElements.VALUE));
		
	}

	public Dataset getDataset(String datasetName) {
		return this.getParentConfig().getMart().getDatasetByName(datasetName);
	}

	public void setLinkOutUrl(String linkOutUrl) {
		this.setProperty(XMLElements.LINKOUTURL, linkOutUrl);
	}

	public void setRDF(String rdf) {
		this.setProperty(XMLElements.RDF, rdf);
	}

	public String getRDF() {
		return this.getPropertyValue(XMLElements.RDF);
	}

	@Deprecated
	public String getLinkOutUrl() {
		return this.getPropertyValue(XMLElements.LINKOUTURL);
	}
	
	public String getLinkOutUrl(String dataset) {
    	String url = this.getPropertyValue(XMLElements.LINKOUTURL);
    	if(McUtils.isStringEmpty(dataset))
    		return url;
    	else {
    		if(McUtils.hasPartitionBinding(url)) {
				org.biomart.objects.objects.Dataset ds = this.getParentConfig().getMart().getDatasetByName(dataset);
				return McUtils.getRealName(url, ds);   			
    		}else
    			return url;
    	}
	}
	
	
	public String getName(Dataset ds) {
		return McUtils.getRealName(this.getName(), ds);
	}

	/**
	 * check if the pointedDatasetName is valid and update the pointedDataset list
	 * @param value
	 */
	public void setPointedDatasetName(String value) {
		/*if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDMART))) {
			JOptionPane.showMessageDialog(null,
				    "The pointedMart is empty.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);

			return;
		}
		if(McUtils.hasPartitionBinding(value)) {
			this.setProperty(XMLElements.POINTEDDATASET, value);	
		} else {
			String[] pointedDsNames = value.split(",");
			//check if has parent config
			if(this.getParentConfig() == null){
				return;
			}
			//check if all datasets are valid			
			Mart pointedMart = this.getParentConfig().getMart().getMartRegistry().getMartByName(this.getPropertyValue(XMLElements.POINTEDMART));
			for(String dsName : pointedDsNames) {
				if(pointedMart.getDatasetByName(dsName)==null) {
					JOptionPane.showMessageDialog(null,
						    "Dataset is not valid.",
						    "Error",
						    JOptionPane.ERROR_MESSAGE);		
					return;
				}				
			}*/
			this.setProperty(XMLElements.POINTEDDATASET, value);
			
		//}
	}
	
	/**
	 * 
	 * @param value
	 */
	public void setPointedMartName(String value) {
		//check if has parent config
		if(this.getParentConfig() == null)
		{
			return;
		}
		//check if the pointedMartName is valid
		if(this.getParentConfig().getMart().getMartRegistry().getMartByName(value)==null) {
			JOptionPane.showMessageDialog(null,
				    "Mart is not valid.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
			return;
		}
		this.setProperty(XMLElements.POINTEDMART, value);
	}

	public String getAttributeListString() {
		return this.getPropertyValue(XMLElements.ATTRIBUTELIST);
	}	
	
	public List<Attribute> getAttributeList() {
		List<Attribute> attList = new ArrayList<Attribute>();
		String attListStr = this.getPropertyValue(XMLElements.ATTRIBUTELIST);
		if(McUtils.isStringEmpty(attListStr))
			return attList;
		String[] list = attListStr.split(",");
		List<String> range = new ArrayList<String>();
		for(String att: list) {
			Attribute attribute = this.getParentConfig().getAttributeByName(att, range);
			if(attribute!=null)
				attList.add(attribute);
		}
		return attList;
	}
	
	public List<Attribute> getAttributeList(List<String> datasets, boolean allowPartialList) {
		List<Attribute> attList = new ArrayList<Attribute>();
		String attListStr = this.getPropertyValue(XMLElements.ATTRIBUTELIST);
		if(McUtils.isStringEmpty(attListStr))
			return attList;
		String[] list = attListStr.split(",");
		boolean valid = true;
		for(String att: list) {
			Attribute attribute = this.getParentConfig().getAttributeByName(att, datasets);
			if(attribute!=null)
				attList.add(attribute);
			else {
				valid = false;
			}
		}
		if(!valid && !allowPartialList)
			attList.clear();
		return attList;
	}

	/*
	 * attributepointer, attributelist, filter
	 */
	public List<MartConfiguratorObject> getAllReferences() {
		List<MartConfiguratorObject> result = new ArrayList<MartConfiguratorObject>();
		Mart currentMart = this.getParentConfig().getMart();
		//get exportable
/*		for(Config config: currentMart.getParentConfigList()) {
			for(ElementList exp: config.getExportableList()) {
				for(Attribute att: exp.getAttributeList()) {
					if(att.equals(this)) {
						result.add(exp);
						break;
					}
				}
			}
		}*/
		for(Mart mt: currentMart.getMartRegistry().getMartList()) {
			for(Config con: mt.getConfigList()) {
				List<Attribute> atList = con.getAttributes(new ArrayList<String>(), true, true);
				for(Attribute tmpAtt :atList) {
					if(tmpAtt.isPointer() && this.equals(tmpAtt.getPointedAttribute()))
						result.add(tmpAtt);
					else if(tmpAtt.getAttributeList().contains(this)) {
						result.add(tmpAtt);
					}
				}
			}
		}
		List<Filter> filterList = this.getParentConfig().getFilters(new ArrayList<String>(), true,true);
		for(Filter filter: filterList) {
			if(this.equals(filter.getAttribute()))
				result.add(filter);
		}
		return result;
	}
	
	public List<Filter> getReferenceFilters() {
		List<Filter> result = new ArrayList<Filter>();
		Collection<Filter> filterList = this.getParentConfig().getAllFilters();
		for(Filter filter: filterList) {
			if(this.equals(filter.getAttribute()))
				result.add(filter);
		}
		return result;		
	}
	
	public boolean hasReferenceFilters(){
		
		Collection<Filter> filterList = this.getParentConfig().getAllFilters();
		for(Filter filter: filterList) {
			if(this.equals(filter.getAttribute()))
				return true;
		}
		return false;
	}

	public void addAttribute(Attribute attribute) {
		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.ATTRIBUTELIST))) {
			this.setProperty(XMLElements.ATTRIBUTELIST, attribute.getName());
		} else {
			String attributeName = attribute.getName();
			String[] existingAttributes = this.getPropertyValue(XMLElements.ATTRIBUTELIST).split(",");
			List<String> attributeList = new ArrayList<String>(Arrays.asList(existingAttributes));
			if(!attributeList.contains(attributeName)) {
				attributeList.add(attributeName);
				this.setProperty(XMLElements.ATTRIBUTELIST, McUtils.StrListToStr(attributeList, ","));
			}
		}
	}
	
	public void setAttributeListString(String attributeListString) {
		this.setProperty(XMLElements.ATTRIBUTELIST, attributeListString);
	}
	
	public void updateAttributeList(String listStr) {
		this.setAttributeListString(listStr);
	}

	
	/*
	 * TODO: it may has a case that attributelist is null, but not valid
	 */
	public boolean isAttributeList() {
		if(!McUtils.isStringEmpty(this.getPropertyValue(XMLElements.ATTRIBUTELIST))) {
			return true;
		}
		return false;
	}

	public void setDatasetTable(DatasetTable table) {
		if(table!=null)
			this.setProperty(XMLElements.TABLE, table.getName());
		else
			this.setProperty(XMLElements.TABLE, "");
	}

	public void setPointedConfigName(String value) {
		this.setProperty(XMLElements.POINTEDCONFIG, value);
	}

	/**
	 * create a copy of the original attribute, all elements are the same for these two attribute 
	 * except when rename == ture, the new attribute will have a different unique name. 
	 * @param rename 
	 * @return
	 */
	public Attribute cloneMyself(boolean rename) {
		org.jdom.Element e = this.generateXml();
		Attribute newAtt = new Attribute(e);
		if(rename) {
			Config config = this.getParentConfig();
			if(null!=config) {
				String newName =  McUtils.getUniqueAttributeName(config, this.getName());
				newAtt.setName(newName);
			}
		}
		return newAtt;
	}

	/**
	 * create a pointer for myself
	 * @return
	 */
	public Attribute createPointer() {
		return null;
	}
	

	@Override
	/*
	 * check if itself is hidden, or it's upperlevel object is hidden
	 */
	public boolean isHidden() {
		boolean hide = super.isHidden();
		//modified to make check container hidden work
		return hide;
		/*
		if(hide)
			return hide;
		if(this.getParent() == null)
			return false;
		else
			return this.getParent().isHidden();
			*/
	}

	public boolean inUser(String user, Collection<String> dss) {
		String userStr = this.getPropertyValue(XMLElements.INUSERS);
		if(McUtils.hasPartitionBinding(this.getPropertyValue(XMLElements.INUSERS)) && dss!=null && dss.size()>0) {			
			PartitionTable pt = this.getParentConfig().getMart().getSchemaPartitionTable();
			
			for(String ds: dss) {
				int row = pt.getRowNumberByDatasetName(ds);
				String tmpStr = McUtils.getRealName(pt, row, userStr).trim();
				String[] users = tmpStr.split(",");
				if(!Arrays.asList(users).contains(user))
					return false;
			}
			return true;
		} else {
			userStr = userStr.trim();
			if("".equals(userStr))
				userStr = "anonymous,privileged";
			String[] users = userStr.split(",");
			return Arrays.asList(users).contains(user);
		}
	}

	@Override
	public int compareTo(Attribute a) {
		// TODO Auto-generated method stub
		//compare based on display name
		return this.getDisplayName().compareTo(a.getDisplayName());
	}
	
	/**
	 * invoked by reflection
	 * @return
	 */
	public List<String> getMartsDropDown() {
		List<String> result = new ArrayList<String>();
		if(!this.isPointer())
			return result;
		List<Mart> martList = this.getParentConfig().getMart().getMartRegistry().getMartList();
		for(Mart item: martList)
			result.add(item.getName());
		return result;
	}

/*	@Override
	public String getDisplayName() {
		if(this.isPointer() && this.getPointedAttribute()!=null)
			return this.getPointedAttribute().getDisplayName();
		else
			return super.getDisplayName();
	}*/

	public void clearAttributeList() {
		this.setProperty(XMLElements.ATTRIBUTELIST, "");
	}

	public boolean isSelectedByDefault() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.DEFAULT));
	}

	public AttributeDataType getDataType() {
		return AttributeDataType.valueFrom(this.getPropertyValue(XMLElements.DATATYPE));
	}

}
