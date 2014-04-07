package org.biomart.objects.objects;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.MartConfiguratorUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.ConnectionPool;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.enums.FilterOperation;
import org.biomart.objects.enums.FilterType;
import org.biomart.queryEngine.OperatorType;


public class Filter extends Element	implements Comparable<Filter> {
	
	private Map<String,Map<String,FilterData>> optionsList;
	public static String EMPTYDATASET="";
	
	public Filter(org.jdom.Element element) {
		super(element);
		this.optionsList = new LinkedHashMap<String,Map<String,FilterData>>();
		this.setNodeType(McNodeType.FILTER);
	}
	
	private void propagateProperty(XMLElements pro, String value) {
        // This is a hack to avoid a NullPointerException while propagating a property
        //  at MartConfigurator boot-time.
        if (pro.equals(XMLElements.FUNCTION) && this.isFilterList() && this.getParentConfig() != null) {
            Log.debug("propagateProperty() filter name = "+ this.getName());
            List<Filter> fs = this.getFilterList();
            for (Filter f : fs) {
                f.setProperty(pro, value);
            }
        }
    }
    
    @Override
    public void setProperty(XMLElements pro, String value) {
        super.setProperty(pro, value);
        
        propagateProperty(pro, value);
    }
	
	@Override
    public int compareTo(Filter f) {
        return this.getDisplayName().compareTo(f.getDisplayName());
    }

	public void setPointedInfo(String pointedFilterName, String pointedDatasetName,
			String pointedConfigName, String pointedMartName) {
		this.setPointer(true);
		this.setProperty(XMLElements.POINTEDDATASET, pointedDatasetName);
		this.setProperty(XMLElements.POINTEDFILTER, pointedFilterName);
		this.setProperty(XMLElements.POINTEDCONFIG, pointedConfigName);
		this.setProperty(XMLElements.POINTEDMART, pointedMartName);
	}
	
	
	public String getFunction() {
        return this.getPropertyValue(XMLElements.FUNCTION);
    }
	
	
	public String getPointedFilterName() {
		return this.getPropertyValue(XMLElements.POINTEDFILTER);
	}
	
	public String getPointedDatasetName() {
		return this.getPropertyValue(XMLElements.POINTEDDATASET);
	}

	public void addFilter(Filter filter)  {
		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.FILTERLIST).trim())) {
			this.setProperty(XMLElements.FILTERLIST, filter.getName());
		} else {
			String filterName = filter.getName();
			String[] existingFilters = this.getPropertyValue(XMLElements.FILTERLIST).split(",");
			List<String> filterList = new ArrayList<String>(Arrays.asList(existingFilters));
			if(!filterList.contains(filterName)) {
				filterList.add(filterName);
				this.setProperty(XMLElements.FILTERLIST, McUtils.StrListToStr(filterList, ","));
			}
		}	
	}

	public List<Filter> getFilterList() {
		List<Filter> filterList = new ArrayList<Filter>();
		String filterListStr = this.getPropertyValue(XMLElements.FILTERLIST);
		if(McUtils.isStringEmpty(filterListStr))
			return filterList;
		String[] list = filterListStr.split(",");
		List<String> range = new ArrayList<String>();
		for(String att: list) {
			Filter filter = this.getParentConfig().getFilterByName(att, range);
			if(filter!=null)
				filterList.add(filter);
		}
		return filterList;
	}
	
	public List<Filter> getFilterList(Collection<String> dss) {
		List<Filter> filterList = new ArrayList<Filter>();
		String filterListStr = this.getPropertyValue(XMLElements.FILTERLIST);
		if(McUtils.isStringEmpty(filterListStr))
			return filterList;
		String[] list = filterListStr.split(",");
		for(String att: list) {
			Filter filter = this.getParentConfig().getFilterByName(att, dss);
			if(filter!=null)
				filterList.add(filter);
		}
		return filterList;
	}
	
	public boolean isFilterList() {
		if(!McUtils.isStringEmpty(this.getPropertyValue(XMLElements.FILTERLIST)))
			return true;
		return false;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		Filter filter=(Filter)object;
		boolean sameconfig = true;
		Config con1 = filter.getParentConfig();
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

		return filter.getName().equals(this.getName()) && sameconfig;		
	}
	
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}
	

	
	/**
	 * skip the range if it is a pointer, otherwise return the intersection
	 * @param datasets
	 * @return
	 */
	public List<FilterData> getFilterDataList(Collection<String> datasets) {
		if(this.getObjectStatus().equals(ValidationStatus.VALID)) {
			if(this.isPointer()) 
				return this.getFilterDataListPointer(datasets);
			else
				return this.getFilterDataListNonPointer(datasets);			
		}
		// return an empty list
		return new ArrayList<FilterData>();
	}

	/**
	 * skip the range if it is a pointer, otherwise return the intersection
	 * @param datasets
	 * @return
	 */
	private List<FilterData> getFilterDataListPointer(Collection<String> datasets) {

		List<FilterData> filterDataList = new ArrayList<FilterData>();
		Set<String> newdsList = new HashSet<String>(datasets);
		
		if(!this.getObjectStatus().equals(ValidationStatus.VALID)) 
			return filterDataList;
		
		Filter currentFilter = this;
		//pointer skip the range
		// see the kegg example
		if(this.isPointer())  {
			newdsList.clear();
			currentFilter = this.getPointedFilter();
		}
		
		String pointedDsName = this.getPointedDatasetName();
		if(McUtils.isStringEmpty(pointedDsName))
			return filterDataList;
		
		if(McUtils.hasPartitionBinding(pointedDsName)) {
			//get new dataset list based on the pointed column
			Mart mart = this.getParentConfig().getMart();
			PartitionTable schemaPt = mart.getSchemaPartitionTable();
			newdsList.addAll(McUtils.getOtherDatasets(schemaPt, new ArrayList<String>(datasets), pointedDsName));
		}else {
			//skip the range check, see kegg example
			newdsList.add(pointedDsName.split(",")[0]);
		}
				
		//return the intersection
		if(currentFilter == null || currentFilter.getObjectStatus()!=ValidationStatus.VALID)
			return filterDataList;
		//if a pointer filter has dependson, don't go to the pointedfilter
		if(!McUtils.isStringEmpty(this.getPropertyValue(XMLElements.DEPENDSON))) 
			//don't use the newdsList in this case
			return this.getDependentFilterDataList(datasets);

		return currentFilter.getFilterDataListNonPointer(newdsList);
	}
	
	private List<FilterData> getDependentFilterDataList(Collection<String> datasets) {
		List<FilterData> fds = new ArrayList<FilterData>();
		Filter pFilter = this.getParentConfig().getFilterByName(this.getPropertyValue(XMLElements.DEPENDSON), datasets);
		if(pFilter == null)
			return fds;
		return McUtils.getSubFilterData(this.getParentConfig(), pFilter, "", datasets, this);
	}

	private List<FilterData> getFilterDataListNonPointer(Collection<String> datasets) {
		//is it a dependent filter
		if(!McUtils.isStringEmpty(this.getPropertyValue(XMLElements.DEPENDSON))) 
			return this.getDependentFilterDataList(datasets);
		
		return this.getOptionByDatasets(new ArrayList<String>(datasets));
	}

 	public OperatorType getQualifier() {
		return OperatorType.valueFrom(this.getPropertyValue(XMLElements.QUALIFIER));
	}
	
	public void setQualifier(OperatorType ot) {
		this.setProperty(XMLElements.QUALIFIER, ot.toString());
	}

	public Filter getPointedFilter() {
		Config pointedConfig = this.getPointedConfing();
		if(pointedConfig == null || McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDFILTER)))
			return null;
		return pointedConfig.getFilterByName(this.getPropertyValue(XMLElements.POINTEDFILTER), new ArrayList<String>());
	}
	
		
	public Filter(Attribute attribute, String name) {
		//check unique name
		super(name);
		this.optionsList = new LinkedHashMap<String,Map<String,FilterData>>();
		this.setProperty(XMLElements.DISPLAYNAME, attribute.getDisplayName());
		this.setProperty(XMLElements.INTERNALNAME, name);
		this.setProperty(XMLElements.DESCRIPTION, attribute.getDescription());
		this.setProperty(XMLElements.ATTRIBUTE, attribute ==null?"":attribute.getName());
		this.setNodeType(McNodeType.FILTER);
		this.setObjectStatus(ValidationStatus.VALID);  
	}
	
	public DatasetColumn getDatasetColumn() {
		if(this.getAttribute() == null)
			return null;
		return this.getAttribute().getDataSetColumn();
	}
	
	/**
	 * need to handle the partition
	 * @return
	 */
	public DatasetTable getDatasetTable() {
		return this.getAttribute().getDatasetTable();		
	}
	
	
	/**
	 * for filter list
	 * by default, use singleselect type
	 * @param name
	 * @param displayName
	 */
	public Filter(String name, String displayName) {
		super(name);
		this.optionsList = new LinkedHashMap<String,Map<String,FilterData>>();
		this.setDisplayName(displayName);
		this.setNodeType(McNodeType.FILTER);
	}
	
	public Filter(String name, String pointedFilterName, String pointedDatasetName) {
		super(name);
		this.optionsList = new LinkedHashMap<String,Map<String,FilterData>>();
		this.setPointer(true);
		this.setNodeType(McNodeType.FILTER);
		//it is not valid for now because the pointedFilter and pointedDataset may not be valid
		this.setProperty(XMLElements.POINTEDDATASET, pointedDatasetName);
		this.setProperty(XMLElements.POINTEDFILTER, pointedFilterName);
		this.setObjectStatus(ValidationStatus.INVALID);
	}
	
	public Filter(FilterType type, String name) {
		super(name);
		this.optionsList = new LinkedHashMap<String,Map<String,FilterData>>();
		this.setNodeType(McNodeType.FILTER);
		if(type==null)
			this.setFilterType(FilterType.TEXT);
		else
			this.setFilterType(type);
		this.setObjectStatus(ValidationStatus.VALID);
	}
	
	public void setAttribute(Attribute attribute) {
		this.setProperty(XMLElements.ATTRIBUTE, attribute.getName());
	}
	
	/*
	 * used by editing the attribute from the gui
	 */
	public void setAttribute(String attName) {
		//find attribute by name
		Attribute att = this.getParentConfig().getAttributeByName(null, 
				attName, true);
		if(att!=null)
			this.setAttribute(att);
		else 
			JOptionPane.showMessageDialog(null, "attribute name error", "error",JOptionPane.ERROR_MESSAGE);
	}

    public String getRDF() {
        return this.getPropertyValue(XMLElements.RDF);
    }

    public void setRDF(String rdf) {
        this.setProperty(XMLElements.RDF, rdf);
    }

	/**
	 * return the attribute object. if the filter is pointer, return the pointed filter's attribute
	 * @return
	 */
	public Attribute getAttribute() {
		if(this.isPointer() && this.getPointedFilter() !=null)
			return this.getPointedFilter().getAttribute();
		return this.getParentConfig().getAttributeByName(this.getPropertyValue(XMLElements.ATTRIBUTE), new ArrayList<String>());
	}

	public void setFilterType(FilterType type) {
		this.setProperty(XMLElements.TYPE, type.toString());
	}
	
	public FilterType getFilterType () {
		if(this.isPointer() && this.getPointedFilter() !=null) {
			return this.getPointedFilter().getFilterType();
		}
		return FilterType.valueFrom(this.getPropertyValue(XMLElements.TYPE));
	}

	public void setSplitOnValue(String value) {
		this.setProperty(XMLElements.SPLITON, value);
	}
	
	public void setFilterOperation(FilterOperation value) {
		this.setProperty(XMLElements.OPERATION, value.toString());
	}
	
	public void setDataFileUrl(String path) {
		this.setProperty(XMLElements.DATAFILE, path);
	}
	
	public void setOnlyValue(String value) {
		this.setProperty(XMLElements.ONLY, value);
	}
	
	public String getOnlyValue() {
		return this.getPropertyValue(XMLElements.ONLY);
	}
	
	public void setExcludedValue(String value) {
		this.setProperty(XMLElements.EXCLUDED, value);
	}
	
	public String getExcludedValue() {
		return this.getPropertyValue(XMLElements.EXCLUDED);
	}
	
	public boolean inPartition(String value) {
		List<String> l = new ArrayList<String>();
		l.add(value);
		return this.inPartition(l);
	}

	public boolean inPartition(Collection<String> values) {
        // First check that child filters are in partition as well
        List<Filter> childFilters = this.getFilterList();

        if (!childFilters.isEmpty()) {
            boolean hasValidChildFilters = false;
            for (Filter childFilter : childFilters) {
                if (childFilter.inPartition(values)) {
                    hasValidChildFilters = true;
                    break;
                }
            }
            if (!hasValidChildFilters)
                return false;
        }

		if(values == null || values.isEmpty())
			return true;
		//is it a pointer?
		if(this.isPointer() && this.getPointedFilter()!=null)  {
			if(McUtils.hasPartitionBinding(this.getPointedDatasetName())) {
				Filter pointedFilter = this.getPointedFilter();
				if(pointedFilter.getObjectStatus()!=ValidationStatus.VALID)
					return false;
				Set<String> targetPartitions = new HashSet<String>();
				//assume they are in schemapartitiontable
				PartitionTable pt = this.getParentConfig().getMart().getSchemaPartitionTable();
				for(String sourceDsStr: values) {
					int row = pt.getRowNumberByDatasetName(sourceDsStr);
					if(row>=0) {
						String realName = McUtils.getRealName(pt, row, this.getPointedDatasetName());
						if(McUtils.isStringEmpty(realName))
							continue;
						String[] _newDsStrs = realName.split(",");
						for(String item: _newDsStrs) {
							targetPartitions.add(item);
						}
					}
				}
				/*
				 * no value for the dataset rows
				 */
				if(targetPartitions.isEmpty())
					return false;
				else
					return pointedFilter.inPartition(targetPartitions);
			}else
				return true;
		}
		if(values == null || values.isEmpty())
			return true;
		//TODO Figure out how to make this work for both ICGC and Ensembl
		// The problem is that, in the commented code below, a check is being made to see if the pointer dataset is valid.
		// For some reason, though, this is returning "false" for ICGC Kegg Pathway. Need to better understand the logic
		// before I can make a fix.
		/*	String pointeddatasets = this.getPointedDatasetName();

			String[] dss = pointeddatasets.split(",");
			if(values.containsAll(Arrays.asList(dss))) 
				return this.getPointedFilter().inPartition(values);
			else {
				//check the name convention
				boolean found = true;
				for(String item: dss) {
					String[] _names = item.split("_");
					for(String ptStr: values) {
						if(ptStr.indexOf(_names[0])<0) {
							found = false;
							break;
						}
					}
				}
				return found;
//				return false;
			}
			//return this.getPointedFilter().inPartition(values);
		}*/
		//is it a filterlist?		
		if(!this.getFilterList().isEmpty()) {
			return true; //FIXME return true for now
		}
		
		if(this.getAttribute() == null)
			return false;
		return this.getAttribute().inPartition(values);		
	}


	//if the filter's status is incomplete, need to store this information for future reference
	public void setFilterListString(String filterListString) {
		this.setProperty(XMLElements.FILTERLIST, filterListString);
	}
	
	public void updateFilterList(String listStr) {
		this.setProperty(XMLElements.FILTERLIST, listStr);
	}
	



	//if the filter's status is incomplete, need to store this information for future reference
	public String getFilterListString() {
		return this.getPropertyValue(XMLElements.FILTERLIST);
	}

	public FilterOperation getFilterOperation() {
		return FilterOperation.valueFrom(this.getPropertyValue(XMLElements.OPERATION));
	}
	
	public String getSplitOnValue() {
		return this.getPropertyValue(XMLElements.SPLITON);
	}

	public String getRealName(int row) {
		if(row==-1 || (!McUtils.hasPartitionBinding(this.getName())))
			return this.getName();
		else {
			List<String> ptRefList = McUtils.extractPartitionReferences(this.getName());
			//if has partition references
			if(ptRefList.size()>1) {
				//assume only one partition for now
				String ptRef = ptRefList.get(1);
				String ptName = McUtils.getPartitionTableName(ptRef);
				PartitionTable pt = this.getParentConfig().getMart().getPartitionTableByName(ptName);
				if(pt == null) {
					Log.debug(Resources.get("INVALIDOBJECT",this.getName()));
					return this.getName();
				} else 
					return McUtils.replacePartitionReferences(pt, row, ptRefList);
			}
			return null;
		}
	}
	
	public String getRealDisplayName(int row) {
		if(row==-1 || (!McUtils.hasPartitionBinding(this.getName())))
			return this.getDisplayName();
		else {
			List<String> ptRefList = McUtils.extractPartitionReferences(this.getDisplayName());
			//if has partition references
			if(ptRefList.size()>1) {
				//assume only one partition for now
				String ptRef = ptRefList.get(1);
				String ptName = McUtils.getPartitionTableName(ptRef);
				PartitionTable pt = this.getParentConfig().getMart().getPartitionTableByName(ptName);
				if(pt == null) {
					Log.debug(Resources.get("INVALIDOBJECT",this.getDisplayName()));
					return this.getName();
				} else 
					return McUtils.replacePartitionReferences(pt, row, ptRefList);
			}
			return null;
		}
	}

	public void setPointedDatasetName(String value) {
/*		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.POINTEDMART))) {
			JOptionPane.showMessageDialog(null,
				    "The pointedMart is empty.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);

			return;
		}
		if(McUtils.hasPartitionBinding(value)) {
			this.setProperty(XMLElements.POINTEDDATASET, value);
		}else {
			String[] pointedDsNames = value.split(",");
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
			}
		*/
			this.setProperty(XMLElements.POINTEDDATASET, value);
		//}
	}

	public void setPointedMartName(String value) {
		//check parent config
		if(this.getParentConfig() == null)
			return;
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

	


	/*
	 * filterpointer, filterList, importable
	 */
	public List<Filter> getReferences() {
		List<Filter> references = new ArrayList<Filter>();
		Mart currentMart = this.getParentConfig().getMart();
/*		for(Config config: currentMart.getConfigList()) {
			for(ElementList imp: config.getImportableList()) {
				for(Filter fil: imp.getFilterList()) {
					if(fil.equals(this)) {
						references.add(imp);
						break;
					}
				}
			}
		}*/
		for(Mart mt: currentMart.getMartRegistry().getMartList()) {
			for(Config config: mt.getConfigList()) {
				List<Filter> ftList = config.getFilters(new ArrayList<String>(), true, true);
				for(Filter tmpFilter :ftList) {
					if(tmpFilter.isPointer() && tmpFilter.getPointedFilter()!=null && tmpFilter.getPointedFilter().equals(this))
						references.add(tmpFilter);
					else if((tmpFilter.isFilterList()) && tmpFilter.getFilterList().contains(this)) {
						references.add(tmpFilter);
					}
				}
			}
		}
		return references;
	}

	public void setPointedConfigName(String value) {
		this.setProperty(XMLElements.POINTEDCONFIG, value);
	}

	public org.jdom.Element generateXml() {
		org.jdom.Element element = new org.jdom.Element(XMLElements.FILTER.toString());
		super.saveConfigurableProperties(element);
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		MartConfiguratorUtils.addAttribute(element, XMLElements.DISPLAYNAME.toString(), this.getPropertyValue(XMLElements.DISPLAYNAME));
		MartConfiguratorUtils.addAttribute(element, XMLElements.INTERNALNAME.toString(), this.getPropertyValue(XMLElements.INTERNALNAME));
		MartConfiguratorUtils.addAttribute(element, XMLElements.DESCRIPTION.toString(), this.getPropertyValue(XMLElements.DESCRIPTION));

		MartConfiguratorUtils.addAttribute(element, XMLElements.MART.toString(), this.getParentConfig().getMart().getName());
		MartConfiguratorUtils.addAttribute(element, XMLElements.CONFIG.toString(), this.getParentConfig().getName());
		MartConfiguratorUtils.addAttribute(element, XMLElements.DEFAULT.toString(), this.getPropertyValue(XMLElements.DEFAULT));
		MartConfiguratorUtils.addAttribute(element, XMLElements.POINTER.toString(), this.isPointer().toString());
		
		element.setAttribute(XMLElements.FUNCTION.toString(), this.getPropertyValue(XMLElements.FUNCTION));
		
		if (this.isPointer()) {
			element.setAttribute(XMLElements.POINTEDDATASET.toString(),this.getPropertyValue(XMLElements.POINTEDDATASET));
			element.setAttribute(XMLElements.POINTEDFILTER.toString(),this.getPropertyValue(XMLElements.POINTEDFILTER));
			element.setAttribute(XMLElements.POINTEDMART.toString(),this.getPropertyValue(XMLElements.POINTEDMART));
			element.setAttribute(XMLElements.POINTEDCONFIG.toString(),this.getPropertyValue(XMLElements.POINTEDCONFIG));				
		}

		element.setAttribute(XMLElements.HIDE.toString(), this.isHidden() ? 
				XMLElements.TRUE_VALUE.toString() : XMLElements.FALSE_VALUE.toString());

		element.setAttribute(XMLElements.TYPE.toString(), this.getPropertyValue(XMLElements.TYPE));
		element.setAttribute(XMLElements.ATTRIBUTE.toString(), this.getPropertyValue(XMLElements.ATTRIBUTE));
		element.setAttribute(XMLElements.SPLITON.toString(), this.getPropertyValue(XMLElements.SPLITON));
		element.setAttribute(XMLElements.OPERATION.toString(), this.getPropertyValue(XMLElements.OPERATION));
		element.setAttribute(XMLElements.DATAFILE.toString(), this.getPropertyValue(XMLElements.DATAFILE));
		element.setAttribute(XMLElements.FILTERLIST.toString(), this.getPropertyValue(XMLElements.FILTERLIST));
		element.setAttribute(XMLElements.QUALIFIER.toString(), this.getPropertyValue(XMLElements.QUALIFIER));
		element.setAttribute(XMLElements.POINTER.toString(), this.isPointer().toString());

		element.setAttribute(XMLElements.REFCONTAINER.toString(), this.getPropertyValue(XMLElements.REFCONTAINER));
		


		
		
		if(this.getFilterType().equals(FilterType.BOOLEAN)) {
			element.setAttribute(XMLElements.ONLY.toString(), this.getPropertyValue(XMLElements.ONLY));
			element.setAttribute(XMLElements.EXCLUDED.toString(), this.getPropertyValue(XMLElements.EXCLUDED));
		}
		element.setAttribute(XMLElements.INUSERS.toString(),this.getPropertyValue(XMLElements.INUSERS));
		element.setAttribute(XMLElements.DEPENDSON.toString(),this.getPropertyValue(XMLElements.DEPENDSON));
		element.setAttribute(XMLElements.RDF.toString(),this.getPropertyValue(XMLElements.RDF));
		//generate option xml
		if(this.hasOption())
			this.generateOptionXml();
		return element;
	}


	@Override
	public boolean isHidden() {
		return super.isHidden();
	}
	
	public boolean hasDropDown() {
		return (this.getFilterType() == FilterType.SINGLESELECT || 
				this.getFilterType() == FilterType.MULTISELECT);
	}
	
	public void updateDropDown(List<Dataset> dsList) throws MartBuilderException {
		if(!hasDropDown())
			return;
		if(this.isPointer())
			return;
		for(Dataset ds: dsList) {
			if(ds.hideOnMaster())
				continue;
			if(!this.inPartition(ds.getName()))
				continue;
			if(ds.getDataLinkType() == DataLinkType.URL) {
				//do nothing for now
			}else {
				List<FilterData> fdList = this.getOptionDataForDataset(ds);
				Options.getInstance().updateFilterOptionElement(this, ds, fdList);
			}
		}
	}
	
	public List<FilterData> getOptionDataForDataset(Dataset ds) throws MartBuilderException {
		List<FilterData> fdl = new ArrayList<FilterData>();
		if(ds.getDataLinkType() == DataLinkType.SOURCE ||
				ds.getDataLinkType() == DataLinkType.TARGET) {
			JdbcLinkObject jdbcObj = null;
			boolean materialized = ds.isMaterialized();
			if(materialized) 
				jdbcObj = ds.getDataLinkInfoForTarget().getJdbcLinkObject();
			else
				jdbcObj = ds.getDataLinkInfoForSource().getJdbcLinkObject();
			
			String tableName = null;
			String colName = null;
			
			if(this.getDatasetTable() == null || this.getDatasetColumn()==null)
				return fdl;

			//get the tablename and columnname
			if(materialized) {
				tableName = this.getDatasetTable().getName(ds.getName());
				colName = this.getDatasetColumn().getName();
			} else {
				SourceColumn sc = this.getDatasetColumn().getSourceColumn();
				if(sc == null)
					return fdl;
				colName = sc.getName();
				tableName = sc.getTable().getName();
			}
			
			StringBuffer sqlBuilder = new StringBuffer("select distinct ");			
			sqlBuilder.append(colName+" from "+tableName);
			
			List<Map<String,String>> rs = ConnectionPool.Instance.query(jdbcObj, sqlBuilder.toString());
			for(Map<String,String> mapItem: rs) {
				String value = (String)mapItem.get(colName);
				if(McUtils.isStringEmpty(value) || value.equals("null") || value.equals("NULL"))
					continue;
				FilterData fd = new FilterData(value,value,false);
				fdl.add(fd);
			}
			Collections.sort(fdl);
			return fdl;
		}
		else
			return fdl;
		//return fdl;
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

	/**
	 * create a pointer of myself
	 * @return
	 */
	public Filter createPointer() {
		org.jdom.Element e = this.generateXml();
		Filter f = new Filter(e);
		f.setPointer(true);
		
		return f;
	}

	/**
	 * called by reflection
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

	public Filter cloneMyself() {
        org.jdom.Element e = this.generateXml();
        return new Filter(e);
    }
	
	public Filter cloneMyself(boolean rename) {
        org.jdom.Element e = this.generateXml();
        Filter newFilter = new Filter(e);
        if (rename) {
            Config cfg = this.getParentConfig();
            if (null != cfg) {
                String newName =  McUtils.getUniqueFilterName(cfg, this.getName());
                newFilter.setName(newName);
            }
        }
        
        return newFilter;
    }

	public void clearFilterList() {
		this.setProperty(XMLElements.FILTERLIST, "");
	}
	
	public void clearOptions() {
		this.optionsList.clear();
	}
	
	public void clearOptions(String dataset) {
		this.optionsList.remove(dataset);
	}
	
	public void addOption(String dataset, FilterData item) {
		Map<String,FilterData> fdMap = this.optionsList.get(dataset);
		if(fdMap == null) {
			fdMap = new LinkedHashMap<String,FilterData>();
			this.optionsList.put(dataset, fdMap);
		}
		fdMap.put(item.getValue(), item);		
	}
	
	
	public List<FilterData> getOptionByDataset(String dataset) {
		List<String> dss = new ArrayList<String>();
		dss.add(dataset);
		return this.getOptionByDatasets(dss);
	}
	
	public List<FilterData> getOptionByDatasets(Collection<String> datasets) {
		List<FilterData> result = null;
		Filter mFilter = this;
		/*
		 * use the filter in masterconfig
		 */
		Config parentConfig = this.getParentConfig();
		if(!parentConfig.isMasterConfig())
			mFilter = McUtils.findFilterInMaster(this);
		

		for(String ds: datasets) {
			Map<String,FilterData> fds = mFilter.optionsList.get(ds);
			if(null == fds)
				break;
			if(null == result) {
				result = new ArrayList<FilterData>(fds.values());
			} else {
				result.retainAll(fds.values());
			}
		}
		if(null==result)
			result = new ArrayList<FilterData>();
		return result;
	}
	
	public boolean hasOption() {
		return !this.optionsList.isEmpty();
	}
	
	private void generateOptionXml() {	
		Mart mart = this.getParentConfig().getMart();
		org.jdom.Element martElement = Options.getInstance().getMartElement(mart);
		org.jdom.Element configElement = null;
		if(null==martElement) {
			martElement = new org.jdom.Element(XMLElements.MART.toString());
			martElement.setAttribute(XMLElements.NAME.toString(), mart.getName());
			martElement.setAttribute(XMLElements.VERSION.toString(), "0.8");
			Options.getInstance().addMartElement(martElement);
			configElement = new org.jdom.Element(XMLElements.CONFIG.toString());
			configElement.setAttribute(XMLElements.NAME.toString(), mart.getMasterConfig().getName());
			martElement.addContent(configElement);
		}
		if(null == configElement)
			configElement = Options.getInstance().getConfigElement(mart.getMasterConfig(), martElement);
		/*
		 * should not happen, for legacy option xml
		 */
		if(null == configElement) {
			configElement = new org.jdom.Element(XMLElements.CONFIG.toString());
			configElement.setAttribute(XMLElements.NAME.toString(), mart.getMasterConfig().getName());
			martElement.addContent(configElement);
		}
		org.jdom.Element filterElement = Options.getInstance().getFilterOptionElement(mart.getMasterConfig(), this);
		if(null==filterElement) {
			filterElement = new org.jdom.Element(XMLElements.FILTER.toString());
			filterElement.setAttribute(XMLElements.NAME.toString(), this.getName());
			configElement.addContent(filterElement);
		}
		filterElement.removeContent();
		for(Map.Entry<String, Map<String,FilterData>> entry: this.optionsList.entrySet()) {
			org.jdom.Element dsElement = new org.jdom.Element(XMLElements.DATASET.toString());
			dsElement.setAttribute(XMLElements.NAME.toString(), entry.getKey());
			filterElement.addContent(dsElement);
			for(FilterData fd: entry.getValue().values()) {
				dsElement.addContent(fd.generateXml());
			}
		}		
	}
	
	public FilterData getOptionByName(String ds, String name) {
		return this.optionsList.get(ds).get(name);
	}
	
	public FilterData getOptionByName(Collection<String> dss, String name) {
		FilterData result = null;
		/*
		 * the filterdata must be existed in all datasets
		 */
		for(String ds: dss) {
			result = this.optionsList.get(ds).get(name);
			if(null == result) {
				break;
			}
		}
		return result;
	}
	
	public void setDependsOn(String filterName) {
		this.setProperty(XMLElements.DEPENDSON, filterName);
	}

	public boolean isRequired() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.REQUIRED));
	}
}
