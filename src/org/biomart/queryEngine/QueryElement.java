package org.biomart.queryEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Filter;

/**
 *
 * @author Syed Haider, Jonathan Guberman, jhsu
 *
 * This class handles Attributes, Filters, Exportables and Importables objects.
 * A Subquery object contains objects of type QueryElement to store Attributes, Filters
 * Exportables and Importables. QueryElement List is composed of >1 QueryElements in order
 * to store AttributeLists, FilterLists, Exportables and Importables based on Lists.
 */
public class QueryElement {
	private QueryElementType type = null;
	private int position = -1;
    private HashMap <String, String> PseudoAttributeValue = new HashMap<String, String>();

	private Integer portablePosition = null;
	private ElementList portable = null;

	private Element element = null;
	private List<Element> elementList = null;

	private String filterValues = null;	// only if !isAttribute

	private boolean newRow;
	private Config config;

	private Dataset dataset;
	private Dataset linkDataset;

    public QueryElement(Attribute attribute, Dataset dataset){
		this(attribute, dataset, false);
	}

    public QueryElement(Attribute attribute, Dataset dataset, boolean newRow){
		this.type = QueryElementType.ATTRIBUTE;
		this.element = attribute;
		this.dataset = dataset;
		this.config = attribute.getParentConfig();
		this.newRow = newRow;
	}

    public QueryElement(ElementList portable, Dataset dataset, Dataset linkDataset, QueryElementType type){
		List<? extends Element> list = new ArrayList<Element>();
		this.setPortable(portable);
		if(type==QueryElementType.ATTRIBUTE || type==QueryElementType.FILTER){
			//TODO throw an error
		}
		else if (type==QueryElementType.IMPORTABLE_FILTER){
			list = portable.getFilterList();
		}
		else if (type==QueryElementType.EXPORTABLE_ATTRIBUTE){
			list = portable.getAttributeList();
		}
		this.type = type;
		this.elementList = new ArrayList<Element>();
		for (Element el : list){
			this.elementList.add(el);
			this.config = el.getParentConfig();
		}
		this.dataset = dataset;
		this.linkDataset = linkDataset;
	}


	/*public QueryElement(ArrayList<Filter> elementList, String dataset){
		this.type = QueryElementType.IMPORTABLE_FILTER;
		this.elementList = new ArrayList<Element>();
		for (Filter filter : elementList)
			this.elementList.add(filter);
		this.dataset = dataset;
	}*/

    public QueryElement(Filter filter, String filterValues, Dataset dataset, boolean newRow){
		this.type = QueryElementType.FILTER;
		this.element = filter;
		this.filterValues = filterValues;
		this.dataset = dataset;
		this.config = filter.getParentConfig();
		this.newRow = newRow;
	}

    public QueryElement(Filter filter, String filterValues, Dataset dataset){
		this(filter, filterValues, dataset, false);
	}

    public QueryElement() {}

    public int getPosition() {
		return position;
	}

    public void setPosition(int position) {
		this.position = position;
	}

    public QueryElementType getType() {
		return type;
	}

    public List<Element> getElementList() {
		return elementList;
	}

    public Element getElement() {
		return element;
	}

    public String getFilterValues() {
		return filterValues;
	}

    public void setFilterValues(String filterValues) {
		this.filterValues = filterValues;
	}

    public Dataset getLinkDataset() {
		return linkDataset;
	}
    public Config getConfig() {
		return config;
	}

    public boolean isNewRow() {
		return newRow;
	}

    public Dataset getDataset() {
		return dataset;
	}

    public boolean isPointer(){
		if(this.element==null)
			return false;
		else
			return this.element.isPointer();
	}

    public void setPortablePosition(Integer portablePosition) {
		this.portablePosition = portablePosition;
	}

    public Integer getPortablePosition() {
		return portablePosition;
	}

    public void setPortable(ElementList portable) {
		this.portable = portable;
	}

    public ElementList getPortable() {
		return portable;
	}

    public void setPseudoAttributeValue(Dataset dataset){
        this.PseudoAttributeValue.put(dataset.getDisplayName(), ((Attribute)this.element).getValue(dataset));
    }

    public String getPseudoAttributeValue(String datasetDisplayName){
        // union case
        if (this.PseudoAttributeValue.size() > 1)
            return this.PseudoAttributeValue.get(datasetDisplayName);
        else
            return this.PseudoAttributeValue.get(this.dataset.getDisplayName());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 97 * hash + (this.element != null ? this.element.hashCode() : 0);
        hash = 97 * hash + (this.dataset != null ? this.dataset.hashCode() : 0);
        return hash;
    }

    @Override
	public String toString() {
		String returnString = null;
			switch(this.type){
			case ATTRIBUTE:
				if(this.element == null)
					returnString = "NULL";
				else
					returnString = ((Attribute)this.element).getName(dataset);
				break;
			case FILTER:
				if(this.element == null)
					returnString = "NULL";
				else
					returnString = this.element.getName() + ": " + this.filterValues;
				break;
			case EXPORTABLE_ATTRIBUTE:
			case IMPORTABLE_FILTER:
				returnString = "<*";
				for(Element el : this.elementList){
					returnString += (el.getName() +":" + this.portablePosition + " ");
				}
				returnString += ("*>");
				break;
			}

		return returnString;
	}

    @Override
	public boolean equals(Object compareTo) {
		if ( !(compareTo instanceof QueryElement) ) return false;
		QueryElement compQE = (QueryElement) compareTo;
		if (this.type == compQE.type && this.dataset.equals(compQE.dataset) && this.element.equals(compQE.element))
			return true;
		return false;
	}
    
    public boolean isPointerInSource(){
    	if(!(this.element.isPointer()) || dataset.getDataLinkType() != DataLinkType.URL)
    		return false;
    	return this.element.isPointerInSource();
    }
}
