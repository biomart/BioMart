package org.biomart.api.lite;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.biomart.api.Jsoml;
import org.biomart.api.enums.Operation;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;


@XmlRootElement(name="mart")
@JsonPropertyOrder({"name", "displayName", "description", "config", "isHidden", "operation", "meta"})
public class Mart extends LiteMartConfiguratorObject implements Serializable {
	
	private static final long serialVersionUID = 7412915241915040487L;

	private MartPointer martpointerObject;
	private String icon = null;
	private String operation = null;
	private boolean includeDatasets;
	private UserGroup currentUser;

    public Mart() {}

	public Mart(MartPointer martpObject, boolean includeDatasets, UserGroup user) {
		super(martpObject);
		this.martpointerObject = martpObject;
        this.includeDatasets = includeDatasets;
        this.currentUser = user;
	}

    @XmlTransient @JsonIgnore
	public List<org.biomart.objects.objects.RDFClass> getRDFClasses(){
		return this.martpointerObject.getConfig().getRDFClasses();
	}
	/**
	 * 
	 * @return a list of datasets belong to current mart. 
	 */
    @JsonIgnore
	public ArrayList<Dataset> getDatasets() {
		ArrayList<Dataset> result = new ArrayList<Dataset>();
		List<org.biomart.objects.objects.Dataset> dsList = this.martpointerObject.getDatasetList(false);
		for(org.biomart.objects.objects.Dataset ds: dsList) {
			if(ds.hideOnConfig(this.martpointerObject.getConfig()))
				continue;
			Dataset liteDs = new Dataset(ds);
			result.add(liteDs);
		}
		return result;
	}

	/**
	 * @return a list of processorGroup
	 */
//    @JsonIgnore
//	public List<ProcessorGroup> getProcessorGroups() {
//		List<ProcessorGroup> result = new ArrayList<ProcessorGroup>();
//		List<org.biomart.processors.ProcessorGroup> list = this.martpointerObject.getProcessorGroupList();
//		for(org.biomart.processors.ProcessorGroup pg: list) {
//			result.add(new ProcessorGroup(pg,this.currentUser));
//		}
//		return result;
//	}
	
	@Override
	public String getDisplayName() {
		return this.martpointerObject.getConfig().getDisplayName();
	}
	
	/**
	 * get all processors in a specified processorGroup
	 * @param pg
	 * @return
	 */
//    @JsonIgnore
//	public List<Processor> getProcessors(org.biomart.processors.ProcessorGroup pg) {
//		List<Processor> result = new ArrayList<Processor>();
//		List<org.biomart.processors.Processor> list = pg.getProcessorList();
//		for(org.biomart.processors.Processor p: list) {
//			result.add(new Processor(p,this.currentUser));
//		}
//		return result;
//	}

//    @JsonIgnore
//	public List<Processor> getProcessors(String groupName) {
//        ProcessorGroup group = null;
//
//        for (ProcessorGroup g : getProcessorGroups()) {
//            if (groupName.equals(g.getName())) {
//                group = g;
//                break;
//            }
//        }
//
//		return group.getProcessorList();
//	}
	
	/**
	 * get all processors within all processorGroups
	 * @return
	 */
//    @JsonIgnore
//	public List<Processor> getProcessors() {
//		List<Processor> result = new ArrayList<Processor>();
//		for(org.biomart.processors.ProcessorGroup pg: this.martpointerObject.getProcessorGroupList()) {
//			List<org.biomart.processors.Processor> list = pg.getProcessorList();
//			for(org.biomart.processors.Processor p: list) {
//				result.add(new Processor(p,this.currentUser));
//			}
//
//		}
//		return result;
//	}
//
//    @JsonIgnore
//    public Processor getProcessorByName(String processorName) {
//		List<Processor> processors = this.getProcessors();
//        for (Processor p : processors) {
//            if (processorName.equals(p.getName())) return p;
//        }
//        return null;
//    }
	
    @JsonIgnore
	public String getQueryOperation() {
		return operation;
	}
    @JsonIgnore
	public String getIcon() {
		return icon;
	}

    @JsonIgnore
	public List<Dataset> getLinkableDatasets(String datasetName) {
		String[] datasets = datasetName.split(",");
		return getLinkableDatasets(new ArrayList<String>(Arrays.asList(datasets)));
	}
	
	/**
	 * 
	 * @param datasetNameList
	 * @return
	 */
    @JsonIgnore
	public List<Dataset> getLinkableDatasets(List<String> datasetNameList) {
		List<Dataset> result = new ArrayList<Dataset>();
		List<Link> linkList = this.martpointerObject.getMart().getMasterConfig().getLinkList();
		for(Link link: linkList) {
			if(link.getObjectStatus() != ValidationStatus.VALID)
				continue;
			String pointeddataset = link.getPointedDataset();
			if(McUtils.hasPartitionBinding(pointeddataset)) {
				org.biomart.objects.objects.Mart pointedMart = link.getPointedMart();
				int col = McUtils.getPartitionColumnValue(pointeddataset);
				for(String dsName: datasetNameList) {
					int row = pointedMart.getSchemaPartitionTable().getRowNumberByDatasetName(dsName);
					String value = pointedMart.getSchemaPartitionTable().getValue(row, col);
					if(McUtils.isStringEmpty(value))
						continue;
					//multiple
					String[] ss = pointeddataset.split(",");
					for(String item: ss) {
						org.biomart.objects.objects.Dataset ds = link.getPointedMart().getDatasetByName(item);
						result.add(new Dataset(ds));
					}					
				}
			}else {
				String[] ss = pointeddataset.split(",");
				for(String item: ss) {
					org.biomart.objects.objects.Dataset ds = link.getPointedMart().getDatasetByName(item);
					result.add(new Dataset(ds));
				}
			}
			List<org.biomart.objects.objects.Dataset> dsList = link.getPointedMart().getDatasetList();
			for(org.biomart.objects.objects.Dataset ds: dsList) {
				result.add(new Dataset(ds));	
			}
		}
		return result;
	}
	
    @JsonIgnore
	public Dataset getDatasetByName(String datasetName) {
		for (Dataset dataset : this.getDatasets()) {
			if (dataset.getName().equals(datasetName)) {
				return dataset;
			}
		}
		return null;
	}
	
	/**
	 * get the root container 
	 * @param datasetName can be multiple datasets separate by ","
	 * @param includeAttributes
	 * @param includeFilters
	 * @param allowPartialList allowPartialList in attribute.getAttributeList();
	 * @return
	 */
    @JsonIgnore
	public Container getRootContainer(String datasetName, Boolean includeAttributes, Boolean includeFilters, boolean allowPartialList) {
		List<String> dsList = null;
		if(McUtils.isStringEmpty(datasetName))
			dsList = new ArrayList<String>();
		else 
			dsList = Arrays.asList(datasetName.split(","));

		org.biomart.objects.objects.Container container = this.martpointerObject.getConfig().getRootContainer();
		Container rootContainer = new Container(container, dsList, includeAttributes, includeFilters, false, this.currentUser, allowPartialList);
		return rootContainer;
    }
	
    @XmlTransient @JsonIgnore
	public String getRDF() {
		return this.martpointerObject.getConfig().getPropertyValue(XMLElements.RDF);
	}

	/**
	 * FIXME should rename it to getContainerForProcessor
	 * @param datasetName
	 * @param includeAttributes
	 * @param includeFilters
	 * @param processor
	 * @return
	 */
//    @JsonIgnore
//
//	public Container getRootContainer(String datasetName, boolean includeAttributes, boolean includeFilters, Processor processor) {
//		List<String> dsList = null;
//		if(McUtils.isStringEmpty(datasetName))
//			dsList = new ArrayList<String>();
//		else
//			dsList = Arrays.asList(datasetName.split(","));
//
//		return processor.getContainer(dsList, includeAttributes, includeFilters);
//
//	}

	/**
	 * get attributes for datasets, can be multiple datasets separated by ,
	 * @param datasetName
	 * @return
	 */
    @JsonIgnore
	public List<Attribute> getAttributes(String datasetName, boolean allowPartialList) {
		return getAttributes(datasetName,false, allowPartialList);
	}
	
    @JsonIgnore
	public List<Attribute> getAttributes(String datasetName, boolean includeHiddenAttributes, boolean allowPartialList) {
		List<String> dslist = new ArrayList<String>();
		if(!McUtils.isStringEmpty(datasetName)) {
			String[] datasets = datasetName.split(",");
			dslist.addAll(Arrays.asList(datasets));
		}
		return getAttributes(dslist, includeHiddenAttributes, includeHiddenAttributes,true, allowPartialList);		
	}
	
    /**
     * 
     * @param datasetNames
     * @param includeHiddenContainers
     * @param includeHiddenAttriubtes
     * @param setField set datasetcolumn name for the attribute
     * @param allowPartialList a flag for getting partial/full attributelist
     * @return
     */
    @JsonIgnore
	private List<Attribute> getAttributes(List<String> datasetNames, boolean includeHiddenContainers, boolean includeHiddenAttriubtes, 
			boolean setField, boolean allowPartialList) {
		List<org.biomart.api.lite.Attribute> liteAttributeList = new ArrayList<org.biomart.api.lite.Attribute>();
		List<org.biomart.objects.objects.Attribute> fullAttributeList = this.martpointerObject.getConfig().getAttributes(datasetNames,includeHiddenContainers,includeHiddenAttriubtes);
		//need all parent containers
		Map<String, Container> containerMap = new HashMap<String, Container>();
		for(org.biomart.objects.objects.Attribute attribute: fullAttributeList) {
			org.biomart.objects.objects.Container containerObj = attribute.getParentContainer();
			Container liteContainer = containerMap.get(containerObj.getName());
			if(liteContainer == null) {
				liteContainer = new Container(containerObj,datasetNames,true,true,this.currentUser, allowPartialList);
				containerMap.put(containerObj.getName(), liteContainer);
			}
			if(!attribute.inUser(this.currentUser.getName(),datasetNames)) {
				continue;
			}
			if(attribute.getObjectStatus()!=ValidationStatus.VALID) {
				continue;
			}
			//create multiple one if it is partitioned
			if(McUtils.hasPartitionBinding(attribute.getName())) {
				List<String> ptRefList = McUtils.extractPartitionReferences(attribute.getName());
				//assume only one partition for now
				String ptRef = ptRefList.get(1);
				String ptName = McUtils.getPartitionTableName(ptRef);
				PartitionTable pt = this.martpointerObject.getMart().getPartitionTableByName(ptName);
				//use first row			
				for(int i=0; i<pt.getTotalRows(); i++) {
					String dsName = null;
					if(pt.getPartitionType() == PartitionType.SCHEMA) 
						dsName = pt.getValue(i, PartitionUtils.DATASETNAME);
					else
						dsName = pt.getValue(i, 0);
					if(!datasetNames.contains(dsName))
						continue;
					String realName = McUtils.replacePartitionReferences(pt, i, ptRefList);
					if(realName !=null) {
						org.biomart.api.lite.Attribute liteAttribute = null;
						if(setField)
							liteAttribute = new org.biomart.api.lite.Attribute(liteContainer,attribute,setField);
						else
							liteAttribute = new org.biomart.api.lite.Attribute(liteContainer,attribute);
						liteAttribute.setRange(datasetNames);
						liteAttribute.setAllowPartialList(allowPartialList);
						liteAttributeList.add(liteAttribute);
						break;
					}
				}
				
			} else { //no partition binding
				org.biomart.api.lite.Attribute liteAttribute = null;
				if(setField)
					liteAttribute = new org.biomart.api.lite.Attribute(liteContainer,attribute,setField);
				else
					liteAttribute = new org.biomart.api.lite.Attribute(liteContainer,attribute);
				liteAttribute.setRange(datasetNames);
				liteAttribute.setAllowPartialList(allowPartialList);
				liteAttributeList.add(liteAttribute);
			}		
		}
		return liteAttributeList;
	}

    @JsonIgnore
	public String getMartInXML() {
		String result = "";
		try {
			XMLOutputter outputter = new XMLOutputter();
			Element root = new Element("root");
			Element mpElement = this.martpointerObject.generateXml();
			Element martElement = this.martpointerObject.getMart().generateXml(null,true);
			root.addContent(mpElement);
			root.addContent(martElement);
			result = outputter.outputString(root);
		} catch (FunctionalException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * if datasetName is empty, get the union of filters
	 * @param datasetName
	 * @param includeHiddenContainer
	 * @return
	 */

    @JsonIgnore
	public ArrayList<Filter> getFilters(String datasetName, boolean includeHiddenContainer, boolean includeHiddenFilter)  {
		if(McUtils.isStringEmpty(datasetName))
			return this.getFilters(new ArrayList<String>(), includeHiddenContainer,includeHiddenFilter);

		String[] datasets = datasetName.split(",");
		return this.getFilters(new ArrayList<String>(Arrays.asList(datasets)), includeHiddenContainer,includeHiddenFilter);
	}
	
	/**
	 * if datasetNames is empty, get the union of filters
	 * @param datasetNames
	 * @param includeHiddenContainer
	 * @return
	 */
    @JsonIgnore
	private ArrayList<Filter> getFilters(ArrayList<String> datasetNames, boolean includeHiddenContainer, boolean includeHiddenFilter)  {
		ArrayList<Filter> liteFilterList = new ArrayList<Filter>();
		List<org.biomart.objects.objects.Filter> fullFilterList = this.martpointerObject.getConfig().getFilters(datasetNames, includeHiddenContainer,includeHiddenFilter);
		Map<String,Container> containerMap = new HashMap<String,Container>();
		for(org.biomart.objects.objects.Filter filter: fullFilterList) {
			org.biomart.objects.objects.Container containerObj = filter.getParentContainer();
			Container liteContainer = containerMap.get(containerObj.getName());
			if(liteContainer == null) {
				liteContainer = new Container(containerObj,datasetNames,true,true,this.currentUser,true);
				containerMap.put(containerObj.getName(), liteContainer);
			}
			if(!filter.inUser(this.currentUser.getName(),datasetNames))
				continue;
			if(filter.getObjectStatus()!=ValidationStatus.VALID)
				continue;
			//create multiple one if it is partitioned
			if(McUtils.hasPartitionBinding(filter.getName())) {
				List<String> ptRefList = McUtils.extractPartitionReferences(filter.getName());
				//assume only one partition for now
				String ptRef = ptRefList.get(1);
				String ptName = McUtils.getPartitionTableName(ptRef);
				PartitionTable pt = this.martpointerObject.getMart().getPartitionTableByName(ptName);
				//use first row
				for(int i=0; i<pt.getTotalRows(); i++) {
					String dsName = null;
					if(pt.getPartitionType() == PartitionType.SCHEMA) 
						dsName = pt.getValue(i, PartitionUtils.DATASETNAME);
					else
						dsName = pt.getValue(i, 0);
					if(!datasetNames.contains(dsName))
						continue;
					
					String realName = McUtils.replacePartitionReferences(pt, i, ptRefList);
					if(realName !=null) {
						org.biomart.api.lite.Filter liteFilter = null;
							liteFilter = new org.biomart.api.lite.Filter(liteContainer,filter);
						liteFilter.setRange(datasetNames);
						liteFilterList.add(liteFilter);
						break;
					}
				}
				
			} else { //no partition binding


				org.biomart.api.lite.Filter liteFilter = new org.biomart.api.lite.Filter(liteContainer,filter);
				liteFilter.setRange(datasetNames);
				liteFilterList.add(liteFilter);
			 
			}
		}
		return liteFilterList;
	}
	
    @XmlAttribute(name="isHidden")
    @JsonProperty("isHidden")
	public boolean isHidden() {
		return this.martpointerObject.getMart().isHidden() || this.martpointerObject.getConfig().isHidden();
	}

    @JsonIgnore
	public Operation getOperation() {
		return this.martpointerObject.getOperation();
	}

    @XmlAttribute(name="operation")
    @JsonProperty("operation")
    public String getOperationName() {
        return getOperation().name();
    }

    /*
     * Returns null if config is default
     */
    @XmlAttribute(name="config")
    @JsonProperty("config")
    public String getConfigName() {
    	org.biomart.objects.objects.Config cfg = this.martpointerObject.getConfig();
        return cfg.getName();
    }

    @JsonIgnore
    public Map<String,Object> getConfigMetaInfoAsMap() {
        String metaInfo = getConfigMetaInfoAsString();
        if (!"".equals(metaInfo)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String,Object> map = mapper.readValue(metaInfo, new TypeReference<Map<String,Object>>(){});
                return map;
            } catch (IOException e) {
                Log.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @JsonIgnore
    public String getConfigMetaInfoAsString() {
        org.biomart.objects.objects.Config cfg = this.martpointerObject.getConfig();
        return cfg.getPropertyValue(XMLElements.METAINFO);
    }

	public List<FilterData> getFilterDataList(String value, Collection<String> datasets, String subFilter) {
        return this.martpointerObject.getFilterDataList(value, datasets, subFilter);
    }

    @XmlAttribute(name="meta")
    @JsonProperty("meta")
    public String getMetaInfo() {
        return getConfigMetaInfoAsString();
    }

    @XmlAttribute(name="mart")
    @JsonIgnore
    public String getActualMartName() {
        return this.martpointerObject.getMart().getName();
    }

    @XmlAttribute(name="group")
    @JsonProperty("group")
    public String getGroupName() {
        return this.martpointerObject.getGroupName();
    }
    
	@Override
	protected Jsoml generateExchangeFormat(boolean xml) throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, McNodeType.MART.toString());

		jsoml.setAttribute("name", super.getName());
		jsoml.setAttribute("displayName", this.getDisplayName());
		jsoml.setAttribute("operation", this.getOperation());
		jsoml.setAttribute("isHidden", isHidden());
		jsoml.setAttribute("config", getConfigName());
		jsoml.setAttribute("meta", xml ? getConfigMetaInfoAsString() : getConfigMetaInfoAsMap());
		jsoml.setAttribute("independent", this.martpointerObject.getIndependentQuery());
        if (this.includeDatasets) {
            for (Dataset dataset : this.getDatasets()) {
                jsoml.addContent(dataset.generateExchangeFormat(xml));
            }
        }
		return jsoml;
	}
	
}