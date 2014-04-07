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
import org.biomart.configurator.utils.type.AttributeDataType;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;


@XmlRootElement(name="attribute")
@JsonPropertyOrder({"name", "displayName", "description", "isHidden", "linkURL", "selected", "value"})
public class Attribute extends LiteMartConfiguratorObject implements Serializable {

	private static final long serialVersionUID = 7606339732698858011L;

    public Attribute() {}

    @JsonIgnore @XmlTransient
	private Integer maxLength = null;

    @JsonIgnore @XmlTransient
	private String linkURL = null;

    @JsonIgnore @XmlTransient
	private org.biomart.objects.objects.Attribute attributeObject;

    @JsonIgnore @XmlTransient
	private Container parent;

	//will changed based on the user selected datasets from the web gui;
    @JsonIgnore @XmlTransient
	private List<String> range;

    private boolean allowPartialList;
    
    @JsonIgnore @XmlTransient
	private String field;
	
    @JsonIgnore @XmlTransient
	public String getField() {
		return this.field;
	}
	
	
	/**
	 * need to remember the parent container
	 * @param attribute
	 */
	public Attribute(Container parent, org.biomart.objects.objects.Attribute attribute) {
		super(attribute);
		this.attributeObject = attribute;
		this.parent = parent;
	}
	
	/**
	 * need to remember the parent container
	 * @param attribute
	 * @param setfield, set datasetcolumn name
	 */
	public Attribute(Container parent, org.biomart.objects.objects.Attribute attribute, boolean setfield) {
		this(parent,attribute);
		if(setfield) {
			if(this.attributeObject.isPointer()) {
				org.biomart.objects.objects.Attribute pa = this.attributeObject.getPointedAttribute();
				if(pa!=null) {
					if(pa.getDataSetColumn()!=null) {
						this.field = pa.getDataSetColumn().getName();
					}
				}
			}else {
				if(this.attributeObject.getDataSetColumn()!=null)
					this.field = this.attributeObject.getDataSetColumn().getName();
			}
		}
	}
	
	@Override
	public String getName() {
		String name = super.getName();
		if(McUtils.isCollectionEmpty(this.range))
			return name;
		else {
			if(McUtils.hasPartitionBinding(name)) {
				//get the first dataset
				String dsName = this.range.iterator().next();
				org.biomart.objects.objects.Dataset ds = this.attributeObject.getParentConfig().getMart().getDatasetByName(dsName);
				return McUtils.getRealName(super.getName(), ds);
			} else
				return name;
		}
	}
	
	@Override
	public String getDisplayName() {
		String displayName = super.getDisplayName();
		if(McUtils.isCollectionEmpty(this.range))
			return displayName;
		else {
			if(McUtils.hasPartitionBinding(displayName)) {
				//get the first dataset
				String dsName = this.range.iterator().next();
				org.biomart.objects.objects.Dataset ds = this.attributeObject.getParentConfig().getMart().getDatasetByName(dsName);
				return McUtils.getRealName(super.getDisplayName(), ds);
			} else
				return displayName;
		}
	}

    @XmlAttribute(name="isHidden")
    @JsonProperty("isHidden")
    public boolean isHidden() {
        return this.attributeObject.getParent().isHidden();
    }
    
    
    @XmlAttribute(name="function")
    @JsonProperty("function")
    public String getFunction() {
        return this.attributeObject.getFunction();
    }
    
	
	public void setRange(List<String> range) {
		this.range = range;
	}

    @XmlElementWrapper(name="attributes")
    @XmlElement(name="attribute")
    @JsonProperty("attributes")
	public List<Attribute> getAttributeList() {
		List<Attribute> attributeList = new ArrayList<Attribute>();
//		boolean dropped = false;
		org.biomart.objects.objects.Attribute attribute = null;

		attribute = this.attributeObject;

		
		for(org.biomart.objects.objects.Attribute a: attribute.getAttributeList(this.range,this.allowPartialList)) {
			//if(a.inPartition(this.range))
				attributeList.add(new Attribute(this.getParent(),a));
			//else {
//				dropped = true;
//				break;
			//}
		}
//		if(dropped)
//			attributeList.clear();
		return attributeList;
	}
	
    @JsonIgnore @XmlTransient
	public Container getParent() {
		return this.parent;
	}

    @XmlAttribute(name="parent")
    @JsonProperty("parent")
    public String getParentName() {
        return getParent().getName();
    }
	
    @JsonIgnore @XmlTransient
	public Boolean getSelectedByDefault() {
		return this.attributeObject.isSelectedByDefault();
	}

    @JsonIgnore @XmlTransient
	public String getLinkURL() {
		return linkURL;
	}

	@Override
	public String toString() {
		return this.getDisplayName();
	}
	
    @JsonProperty("linkURL")
    @XmlAttribute(name="linkURL")
	public String getLinkOutUrl() {
    	String url = this.attributeObject.getLinkOutUrl();
    	if(McUtils.isCollectionEmpty(this.range))
    		return url;
    	else 
    		return this.attributeObject.getLinkOutUrl(this.range.iterator().next());
	}

    @JsonProperty("value")
    @XmlAttribute(name="value")
    public String getValue() {
        return this.attributeObject.getValue();
    }

    // If true, then this attribute should be selected by default in the UI
    @JsonProperty("selected")
    @XmlAttribute(name="selected")
    public boolean isSelected() {
        return this.attributeObject.isSelectedByDefault();
    }

    public AttributeDataType getDataType() {
    	return this.attributeObject.getDataType();
    }
    
    @JsonIgnore
    public String getRDF() {
    	return this.attributeObject.getPropertyValue(XMLElements.RDF);
    }

    @JsonIgnore
    public boolean isPseudoAttribute() {
        return this.attributeObject.isPseudoAttribute();
    }
    
	@Override
	protected Jsoml generateExchangeFormat(boolean xml) throws FunctionalException {

		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());
		
		jsoml.setAttribute("name", this.getName());
		jsoml.setAttribute("displayName", this.getDisplayName());
		jsoml.setAttribute("description", super.getDescription());
		
//		jsoml.setAttribute("maxLength", this.maxLength);
		jsoml.setAttribute("linkURL", this.linkURL);
        jsoml.setAttribute("isHidden", this.getParent().isHidden());
        jsoml.setAttribute("restriction", this.getParent().getMaxContainers());
        jsoml.setAttribute("independent", this.getParent().isIndependentQuerying());

        for (Attribute a : this.getAttributeList()) {
            jsoml.addContent(a.generateExchangeFormat(xml));
        }

		
		return jsoml;
	}


	public void setAllowPartialList(boolean allowPartialList) {
		this.allowPartialList = allowPartialList;
	}





}
