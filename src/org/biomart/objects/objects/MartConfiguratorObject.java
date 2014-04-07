package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.McProperty;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.menu.AttributeTableConfig;
import org.jdom.Element;


public abstract class MartConfiguratorObject extends AbstractModel {

	private static final long serialVersionUID = 3168050129952793078L;
	private ValidationStatus mcObjectStatus = ValidationStatus.VALID;		

	protected MartConfiguratorObject parent;	

	public String getInternalName() {
		return this.getPropertyValue(XMLElements.INTERNALNAME);
	}
	
	public String getName() {
		return this.getPropertyValue(XMLElements.NAME);
	}

	public void setName(String name) {
		this.setProperty(XMLElements.NAME, name);
	}
	
	public void setInternalName(String name) {
		this.setProperty(XMLElements.INTERNALNAME, name);
	}

	public void setDescription(String description) {
		this.setProperty(XMLElements.DESCRIPTION, description);
	}

	public void setDisplayName(String displayName) {
		this.setProperty(XMLElements.DISPLAYNAME, displayName);
	}

	public void setHideValue(Boolean hide) {
		this.setProperty(XMLElements.HIDE, hide.toString());
	}

	public String getDescription() {
		return this.getPropertyValue(XMLElements.DESCRIPTION);
	}

	public String getDisplayName() {
		return this.getPropertyValue(XMLElements.DISPLAYNAME);
	}
	
	public String getDisplayName(Dataset ds) {
		return McUtils.getRealName(this.getDisplayName(), ds);
	}

	public boolean isHidden() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.HIDE));
	}

	@Override
	public String toString() {
		return this.getPropertyValue(XMLElements.DISPLAYNAME);
	}
	
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		
		// If not the same type or different names -> not equal
		MartConfiguratorObject martConfiguratorObject=(MartConfiguratorObject)object;
		return (this.getName().equals(martConfiguratorObject.getName())
		);
	}

	@Override
	public int hashCode() {
		int HASH_SEED = 31;
		return HASH_SEED + this.getPropertyValue(XMLElements.NAME).hashCode();
	}
	
	/**
	 * For XML generation
	 */
	public abstract Element generateXml() throws FunctionalException;

	protected MartConfiguratorObject(String name) {
		//by default, displayName equals name
		this.mcObjectStatus = ValidationStatus.VALID;
		//TODO for MC GUI only
		this.properties = new HashMap<XMLElements,McProperty>();
		//set initial ones
		this.setProperty(XMLElements.ID,name);
		this.setProperty(XMLElements.NAME, name);
		this.setProperty(XMLElements.INTERNALNAME, name);
		this.setProperty(XMLElements.DISPLAYNAME, name);
		this.setProperty(XMLElements.DESCRIPTION, name);
		this.setProperty(XMLElements.HIDE, XMLElements.FALSE_VALUE.toString());
	}
	
	protected MartConfiguratorObject(Element element) {
		this.mcObjectStatus = ValidationStatus.VALID;
		//TODO for MC GUI only should be removed from here
		this.properties = new HashMap<XMLElements,McProperty>();
		//load xml properties

		String name = element.getName();
		
		Element propertyElement = AttributeTableConfig.getInstance().getElementByName(name);
		if(propertyElement!=null) {
			@SuppressWarnings("unchecked")
			List<Element> properties = propertyElement.getChildren("item");
			for(Element property: properties) {
				XMLElements p = XMLElements.valueFrom(property.getAttributeValue("name"));
				//hardcode for now, backwardcompatibility with rdf
				if(p.toString().equals("uniqueid")) {
					if(element.getAttribute("uniqueid")!=null) {
						this.setProperty(p, element.getAttributeValue(p.toString()));
					} else
						this.setProperty(p, element.getAttributeValue("uniqueId"));
				}
				else 
					this.setProperty(p, element.getAttributeValue(p.toString()));
			}
		}
	}
	
	public void setParent(MartConfiguratorObject parent) {
		this.parent = parent;
	}
	
	public MartConfiguratorObject getParent() {
		return this.parent;
	}

	public void setObjectStatus(ValidationStatus mcObjectStatus) {
		if(this.mcObjectStatus == mcObjectStatus)
			return;
		this.mcObjectStatus = mcObjectStatus;
		if(mcObjectStatus == ValidationStatus.VALID)
			this.setProperty(XMLElements.ERROR, "");
	}

	public ValidationStatus getObjectStatus() {
		return mcObjectStatus;
	}

	public void setVisibleModified(boolean b) {
		this.setProperty(XMLElements.VISIBLEMODIFIED, Boolean.toString(b));
	}
		
	public void setMcValue(String property, Object value) {
		//hardcode for now
		if(property.equalsIgnoreCase(XMLElements.NAME.toString()))
			this.setName((String)value);
		else if(property.equalsIgnoreCase(XMLElements.DISPLAYNAME.toString()))
			this.setDisplayName((String)value);
		else if(property.equalsIgnoreCase(XMLElements.HIDE.toString()))
			this.setHideValue((Boolean)value);
	}
			
	protected Map<XMLElements,McProperty> properties;
	
	public McProperty getProperty(XMLElements key) {
		return this.properties.get(key);
	}
	
	/**
	 * if it is null, return ""
	 * @param key
	 * @return
	 */
	public String getPropertyValue(XMLElements key) {
		if(key==null)
			return "";
		McProperty property = this.properties.get(key);
		if(property == null)
			return "";
		else
			return property.getValue() == null?"":property.getValue();
	}
	
	public boolean getPropertyBoolean(XMLElements key) {
		if(key==null)
			return false;
		McProperty property = this.properties.get(key);
		if(property == null)
			return false;
		else
			return Boolean.parseBoolean(property.getValue());
	}
	
	public List<String> getPropertyList(XMLElements key) {
		List<String> result = new ArrayList<String>();
		String s = this.getPropertyValue(key);
		if(McUtils.isStringEmpty(s))
			return result;
		String[] _s = s.split(",");
		for(String item: _s) {
			result.add(item);
		}
		return result;
	}


	public void setProperty(XMLElements pro, String value) {
		McProperty mcProperty = this.properties.get(pro);
		if(mcProperty == null) {
			mcProperty = new McProperty(value);
			this.properties.put(pro, mcProperty);
		} else
			mcProperty.setValue(value);		
	}
	
	//FIXME remove
	@Deprecated
	public void synchronizedFromXML() {
		
	}

	/*
	 * return empty list by default;
	 */
	public List<MartConfiguratorObject> getChildren() {
		List<MartConfiguratorObject> children = new ArrayList<MartConfiguratorObject>();
		return children;
	}

	public Config getParentConfig() {
		if(this instanceof Config)
			return (Config)this;
		MartConfiguratorObject myParent = this.parent;
		while(null!=myParent && !(myParent instanceof Config)) {
			myParent = myParent.getParent();
		}
		if(myParent==null)
			return null;
		else
			return (Config)myParent;
	}

	public Collection<McProperty> getMcProperties() {
		return this.properties.values();
	}

	public boolean isVisibleModified() {
		return this.getPropertyBoolean(XMLElements.VISIBLEMODIFIED);
	}

	protected void saveConfigurableProperties(Element generatedElement) {
		Element configElement = AttributeTableConfig.getInstance().getElementByName(this.getNodeType().toString());
		//get all autosave elements
		@SuppressWarnings("unchecked")
		List<Element> elements = configElement.getChildren("item");
		for(Element element: elements) {
			if(Boolean.parseBoolean(element.getAttributeValue("autosave"))) {
				generatedElement.setAttribute(element.getAttributeValue("name"), 
						this.getPropertyValue(XMLElements.valueFrom(element.getAttributeValue("name"))));
			}
		}
	}

}
