package org.biomart.api.lite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.enums.FilterType;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@XmlRootElement(name="filter")
@JsonPropertyOrder({"name", "displayName", "description", "type", "isHidden", "depends", "qualifier", "required", "function"})
public class Filter extends LiteMartConfiguratorObject implements Serializable {

	private static final long serialVersionUID = 3717403391079076481L;

	// From element
	private String displayType = null;			// see FilterDisplayType, possible values: boolean, textfield, list, tree & group
	private Boolean selectedByDefault = null;	// selected by default or not
	
	// From simple filters
	private String qualifier = "";		// =, <, >, ...
	private Boolean caseSensitive = null;	// for textfield, list and tree filters: whether the filtering is caseSensitive or not
	//private String orderBy = null;			// in case the GUI needs to display what it is ordered by... (probably not useful)
	
	private Boolean multiValue = null;	// for list & textfield filters (and tree?): can select only 1 or N values
	private Boolean upload = null;		// for textfield filters (and list and tree?): if a button is available to upload filtering values
	
	//private String trueValue = null;	// for boolean filters: value of true (may not be needed by GUI)
	private String trueDisplay = null;	// for boolean filters: display for true 
	//private String falseValue = null;	// for boolean filters: value for false (may not be needed by GUI)
	private String falseDisplay = null;	// for boolean filters: display for false
			// booleans are treated differently than lists or trees since they only need those 4 values, 
			// so we avoid creating "data" file with only 2 rows (faster access, less files) 
	
	private List<String> cascadeChildrenNamesList = null;	// for list filters (list is empty if not applicable): 
															// 		name of the filters affected by the choice for the current filter
															// 		(see complete structure in the data object)

	// From group filters
	private String logicalOperator = null;			// operator on group: AND or OR
	private String multipleFilter = null;			// can select only 1, only N or necessarily ALL filters in the group
	//if the range is empty, get the union
	private List<String> range;
	private Container parent;
    @XmlAttribute(name="dependsOn")
    private String depends;

	private org.biomart.objects.objects.Filter filterObject;

    public Filter() {}

	public Filter(Container parent,org.biomart.objects.objects.Filter filter) {
		super(filter);
		this.filterObject = filter;
		this.range = new ArrayList<String>();
		this.parent = parent;
		if(McUtils.isStringEmpty(this.filterObject.getPropertyValue(XMLElements.DEPENDSON)))
            this.depends = "";
        else
            this.depends = this.filterObject.getPropertyValue(XMLElements.DEPENDSON).toString();
	}

    @XmlAttribute(name="isHidden")
    @JsonProperty("isHidden")
    public boolean isHidden() {
        return this.filterObject.getParent().isHidden();
    }
    
    
    @XmlAttribute(name="function")
    @JsonProperty("function")
    public String getFunction() {
        return this.filterObject.getFunction();
    }

	public void setRange(List<String> range) {
		this.range = range;
	}

    @JsonProperty("dependsOn")
	public String dependsOn() {
        return this.depends;
	}

    @JsonIgnore @XmlTransient
	public FilterType getType() {
		return this.filterObject.getFilterType();
	}

    @XmlAttribute(name="type")
    @JsonProperty("type")
	public String getTypeName() {
		return this.filterObject.getFilterType().toString();
	}

    @JsonIgnore
	public Container getParent() {
		return this.parent;
	}

    @XmlAttribute(name="parent")
    @JsonProperty("parent")
    public String getParentName() {
        return getParent().getName();
    }
	
    @XmlElementWrapper(name="filters")
    @XmlElement(name="filter")
    @JsonProperty("filters")
	public List<Filter> getFilterList() {
		List<Filter> filterList = new ArrayList<Filter>();		
		org.biomart.objects.objects.Filter filter = null;

		filter = this.filterObject;
	
		for(org.biomart.objects.objects.Filter f: filter.getFilterList()) {
			//need to handle list of list
			if(!f.getFilterList().isEmpty()) {
				boolean inP = false;
				//add if find one filter exist
				for(org.biomart.objects.objects.Filter f2: f.getFilterList()) {
					if(f2.inPartition(this.range)) {
						inP = true;
						break;
					}
				}
				if(inP)
					filterList.add(new Filter(this.getParent(),f));
			}else if(f.inPartition(this.range))
				filterList.add(new Filter(this.getParent(),f));
		}
		return filterList;
	}
	// From element
    @JsonIgnore
	public Boolean getSelectedByDefault() {
		return selectedByDefault;
	}
	
    @JsonIgnore
	public String getOnly() {
		return this.filterObject.getOnlyValue();
	}
	
    @JsonIgnore
	public String getExcluded() {
		return this.filterObject.getExcludedValue();
	}

	// From filter
    @XmlAttribute(name="qualifier")
    @JsonProperty("qualifier")
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public String getName() {		
		if(McUtils.isCollectionEmpty(this.range))
			return super.getName();
		else {
			//get the first dataset
			String dsName = this.range.iterator().next();
			org.biomart.objects.objects.Dataset ds = this.filterObject.getParentConfig().getMart().getDatasetByName(dsName);
			return McUtils.getRealName(super.getName(), ds);
		}
	}

	@Override
	public String getDisplayName() {
		if(McUtils.isCollectionEmpty(this.range))
			return super.getDisplayName();
		else {
			//get the first dataset
			String dsName = this.range.iterator().next();
			org.biomart.objects.objects.Dataset ds = this.filterObject.getParentConfig().getMart().getDatasetByName(dsName);
			return McUtils.getRealName(super.getDisplayName(), ds);
		}
	}

    @JsonProperty("required")
    @XmlAttribute(name="required")
	public boolean isRequired() {
		return this.filterObject.isRequired();
	}
	
	// From simple filter
    @JsonIgnore
	public String getDisplayType() {
		return displayType;
	}
    @JsonIgnore
	public Boolean getMultiValue() {
		return multiValue;
	}
    @JsonIgnore
	public Boolean getUpload() {
		return upload;
	}
    @JsonIgnore
	public String getTrueDisplay() {
		return trueDisplay;
	}
    @JsonIgnore
	public String getFalseDisplay() {
		return falseDisplay;
	}

    @JsonIgnore
	public String getRDF() {
		return this.filterObject.getPropertyValue(XMLElements.RDF);
	}

	// From group filter
    @JsonIgnore
	public String getLogicalOperator() {
		return logicalOperator;
	}
    @JsonIgnore
	public String getMultipleFilter() {
		return multipleFilter;
	}

	/**
	 * @return the union of the filter data for the datasets in the range
	 */

    @XmlElementWrapper(name="values")
    @XmlElement(name="value")
    @JsonProperty("values")
	public List<FilterData> getFilterDataList() {
    	List<FilterData> fdList = new ArrayList<FilterData>();
        for (org.biomart.configurator.model.object.FilterData fd : this.filterObject.getFilterDataList(range)) {
            fdList.add(new FilterData(fd));
        }

        return fdList;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}

    @XmlAttribute(name="attribute")
    @JsonProperty("attribute")
    public String getAttributeName() {
        org.biomart.objects.objects.Attribute a = this.filterObject.getAttribute();
        return a != null ? a.getName() : "";
    }
	
	@Override
	protected Jsoml generateExchangeFormat(boolean xml) throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());
		
		// From super class
		jsoml.setAttribute("name", this.getName());
		jsoml.setAttribute("displayName", this.getDisplayName());
		jsoml.setAttribute("description", this.getDescription());
		
		// From filter
		jsoml.setAttribute("qualifier", this.qualifier);
		
		// From simple filter
		jsoml.setAttribute("type", this.getType().toString());

        for (Filter f : this.getFilterList()) {
            jsoml.addContent(f.generateExchangeFormat(xml));
        }

        for (FilterData fd : this.getFilterDataList()) {
            Jsoml tmp = new Jsoml(xml, "value");
            tmp.setAttribute("name", fd.getName());
            tmp.setAttribute("displayName", fd.getDisplayName());
            tmp.setAttribute("isSelected", fd.isSelected());
            jsoml.addContent(tmp);
        }

        jsoml.setAttribute("depends", this.dependsOn());
//        jsoml.setAttribute("parent", this.filterObject.getParent().getName());
        jsoml.setAttribute("isHidden", this.filterObject.getParent().isHidden());
//        jsoml.setAttribute("isFilterList", this.filterObject.isFilterList());
        org.biomart.objects.objects.Attribute a = this.filterObject.getAttribute();
        if (a != null)
            jsoml.setAttribute("attribute", a.getName());
		
		return jsoml;
	}
}
