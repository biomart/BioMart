package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.PortableType;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Element;

public abstract class ElementList extends MartConfiguratorObject implements Comparable<ElementList>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected PortableType portableType;

	//hack for group
	private Map<String,String> linkVersionMap;
	
	public boolean isDefault() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.DEFAULT));
	}

	public void setDefaultState(boolean b) {
		this.setProperty(XMLElements.DEFAULT, Boolean.toString(b));
	}

	public ElementList(Config config, String name, PortableType impExpType) {
		super(name);
		this.parent = config;
		this.portableType = impExpType;
		if(impExpType.equals(PortableType.IMPORTABLE)) {
			this.setNodeType(McNodeType.IMPORTABLE);
		//	config.addImportable(this);
		}
		else {
			this.setNodeType(McNodeType.EXPORTABLE);
		//	config.addExportable(this);
		}
		
		//set default value
		this.setProperty(XMLElements.TYPE, "link");
		this.setProperty(XMLElements.ORDERBY, "");
		this.setProperty(XMLElements.VERSION, "0");
		this.linkVersionMap = new HashMap<String,String>();
	}
	
	public ElementList(org.jdom.Element element) {
		super(element);
		this.linkVersionMap = new HashMap<String,String>();
	}
	
	public boolean isImportable() {
		return (this.portableType.equals(PortableType.IMPORTABLE));
	}
	
	public List<Filter> getFilterList() {
		List<Filter> result = new ArrayList<Filter>();
		Config config = this.getParentConfig();
		String filters = this.getPropertyValue(XMLElements.FILTERS);
		if(!McUtils.isStringEmpty(filters)) {
			String[] _filters = filters.split(",");
			for(String _filter: _filters) {
				Filter f = config.getFilterByName(_filter, null);
				if(null!=f)
					result.add(f);
			}
		}		
		return result;
	}

	
	public void addFilter(Filter filter) {
		String filters = this.getPropertyValue(XMLElements.FILTERS);
		if(McUtils.isStringEmpty(filters))
			this.setProperty(XMLElements.FILTERS, filter.getName());
		else {
			String[] _filters = filters.split(",");
			List<String> list = new ArrayList<String>(Arrays.asList(_filters));
			if(!list.contains(filter.getName())) {
				list.add(filter.getName());
				this.setProperty(XMLElements.FILTERS, McUtils.StrListToStr(list, ","));
			}
		}
	}
	
	public void addAttribute(Attribute attribute) {
		String attributes = this.getPropertyValue(XMLElements.ATTRIBUTES);
		if(McUtils.isStringEmpty(attributes))
			this.setProperty(XMLElements.ATTRIBUTES, attribute.getName());
		else {
			String[] _attributes = attributes.split(",");
			List<String> list = new ArrayList<String>(Arrays.asList(_attributes));
			if(!list.contains(attribute.getName())) {
				list.add(attribute.getName());
				this.setProperty(XMLElements.ATTRIBUTES, McUtils.StrListToStr(list, ","));
			}
		}
	}
	
	public List<Attribute> getAttributeList() {
		List<Attribute> result = new ArrayList<Attribute>();
		Config config = this.getParentConfig();
		String attributes = this.getPropertyValue(XMLElements.ATTRIBUTES);
		if(!McUtils.isStringEmpty(attributes)) {
			String[] _attributes = attributes.split(",");
			for(String _attribute: _attributes) {
				Attribute f = config.getAttributeByName(_attribute, null);
				if(null!=f)
					result.add(f);
			}
		}		
		return result;
	}
	
	public Element generateXml() {
		Element element = null;
		if(this.portableType.equals(PortableType.IMPORTABLE)) {
			element = new Element(XMLElements.ITEM.toString());
			element.setAttribute(XMLElements.FILTERS.toString(),this.getPropertyValue(XMLElements.FILTERS));
		} else {
			element = new Element(XMLElements.ITEM.toString());
			element.setAttribute(XMLElements.ATTRIBUTES.toString(), this.getPropertyValue(XMLElements.ATTRIBUTES));
			element.setAttribute(XMLElements.DEFAULT.toString(),this.getPropertyValue(XMLElements.DEFAULT));
		}
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getInternalName());
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getDisplayName());
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getDescription());
		
		element.setAttribute(XMLElements.VERSION.toString(),this.getPropertyValue(XMLElements.VERSION));
		element.setAttribute(XMLElements.ORDERBY.toString(),this.getPropertyValue(XMLElements.ORDERBY));
		element.setAttribute(XMLElements.LINKVERSION.toString(), this.getPropertyValue(XMLElements.LINKVERSION));
		element.setAttribute(XMLElements.TYPE.toString(),this.getPropertyValue(XMLElements.TYPE));
		element.setAttribute(XMLElements.LINKVERSIONS.toString(),this.MapToStr(this.linkVersionMap));
		return element;
	}
	
	private String MapToStr(Map<String,String> map) {
		if(map.isEmpty())
			return "";
		StringBuffer sb = new StringBuffer();
		for(Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, String> entry = it.next();
			sb.append(entry.getKey()+","+entry.getValue());
			if(it.hasNext())
				sb.append(";");
		}
		return sb.toString();
	}

	public void setLinkVersion(String linkVersion) {
		this.setProperty(XMLElements.LINKVERSION, linkVersion);
	}

	public String getLinkVersion() {
		return this.getPropertyValue(XMLElements.LINKVERSION);
	}

	/**
	 * check if the importable/exportable is in the partition
	 * if all filter/attribute are in the partition, true
	 * @param value
	 * @return
	 */
	public boolean inPartition(String value) {
		if(this.portableType.equals(PortableType.IMPORTABLE)) {
			for(Filter filter: this.getFilterList()) {
				if(!filter.inPartition(value))
					return false;
			}
			return true;
		}else {
			for(Attribute attribute: this.getAttributeList()) {
				if(!attribute.inPartition(value))
					return false;
			}
			return true;
		}
		
	}

	public void addLinkVersion(String datasetName, String version) {
		this.linkVersionMap.put(datasetName,version);
	}
		
	public int compareTo(ElementList arg0) {
		ElementList e1 = (ElementList)arg0;
		if(this.isDefault() && (!e1.isDefault()))
			return 1;
		else if((!this.isDefault()) && e1.isDefault())
			return -1;
		else
			return this.getName().compareTo(e1.getName());

	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		else if(obj == null || obj.getClass()!=this.getClass())
			return false;
		else if(((ElementList)obj).portableType!=this.portableType)
			return false;
		else if(((ElementList)obj).getName().equals(this.getName()))
			return true;
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return this.getName().hashCode()+this.portableType.hashCode();
	}
	
	public List<MartConfiguratorObject> getReferences() {
		List<MartConfiguratorObject> references = new ArrayList<MartConfiguratorObject>();
		//find all links
		List<MartPointer> mpList = ((Config)this.getParent()).getMart().getMartRegistry()
			.getPortal().getRootGuiContainer().getAllMartPointerListResursively();
		return references;
	}
	

	@Override
	@Deprecated
	public  void synchronizedFromXML() {

	}
}