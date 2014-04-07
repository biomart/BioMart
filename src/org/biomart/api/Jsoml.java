package org.biomart.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.MartConfiguratorUtils;
import org.jdom.Document;
import org.jdom.Element;

public class Jsoml {

	public static final String XML_TEXT = "#text";
	public static final String JSON_EXCEPTION_MESSAGE = "Unhandled JSON object structure :";
	
	private Boolean xml = null;
	private String name = null;
	private String pluralName = null;
	
	private Element xmlElement = null;
	
	private Map<String,Object> jsonData = null;
	
	public Jsoml(Element xmlElement) {
		this.xml = true;
		this.xmlElement = xmlElement;
		this.name = xmlElement.getName();
	}
	public Jsoml(Map<String,Object> map) throws FunctionalException {
		this.xml = false;
		assignJsonObject(map);
	}
	public Jsoml(boolean xml, String name) {
        this(xml, name, null);
	}
    public Jsoml(boolean xml, String name, String pluralName) {
		this.xml = xml;
		this.name = name;
        this.pluralName = pluralName == null ? name + "s" : pluralName; 
		if (xml) {
			this.xmlElement = new Element(name);
		} else {
			this.jsonData = new LinkedHashMap<String,Object>();
		}
    }
	private Jsoml(boolean xml, Object object) throws FunctionalException {
		if (xml) {
			this.xmlElement = (Element)object;
			this.name = this.xmlElement.getName();
		} else {
			assignJsonObject((Map<String,Object>)object);
		}
	}
	@SuppressWarnings("unchecked")
	private void assignJsonObject(Map<String,Object> map) throws FunctionalException {
		this.jsonData = map;
		Iterator<String> keys = (Iterator<String>)jsonData.keySet();
		if (!keys.hasNext()) {
			throw new FunctionalException(JSON_EXCEPTION_MESSAGE + keys);
		}
		this.name = keys.next();
		if (keys.hasNext()) {
			throw new FunctionalException(JSON_EXCEPTION_MESSAGE + keys);
		}
	}
	

	public Object getXmlOrJson() {
		if (this.xml) {
			return getXmlElement();
		} else {
			return getJsonObject();
		}
	}
	
	public Element getXmlElement() {
		return this.xmlElement;
	}
	public Map<String,Object> getJsonObject() {
		return this.jsonData;
	}
	public String getName() {
		return name;
	}
	public String getPluralName() {
		return pluralName;
	}
	
	// JDOM like mehods
	public void setText(String text) {
		if (null!=text) {
			if (this.xml) {
				this.xmlElement.setText(text);
			} else {
				this.jsonData.put("text", createJsonText(text));
			}
		}
	}
	public void setAttribute(String propertyName, Collection propertyValues) {
        Object val;
        if (propertyValues == null) {
            val = Collections.EMPTY_LIST;
        } else {
            val = propertyValues;
        }
		if (this.xml) {
			MartConfiguratorUtils.addAttribute(this.xmlElement, propertyName, val);
		} else {
            this.jsonData.put(propertyName, val);
		}
	}

	public void setAttribute(String propertyName, Object propertyValue) {
        if (propertyValue == null) {
            propertyValue = "";
        }
        if (this.xml) {
            MartConfiguratorUtils.addAttribute(this.xmlElement, propertyName, propertyValue);
        } else {
            this.jsonData.put(propertyName, propertyValue);
        }
	}

	public void removeAttribute(String propertyName) {
		if (this.xml) {
			this.xmlElement.removeAttribute(propertyName);
		} else {
			this.jsonData.remove(propertyName);
		}
	}
	public void addContent(Jsoml jsoml) {
		if (this.xml) {
			this.xmlElement.addContent(jsoml.getXmlElement());
		} else {
            String contentName = jsoml.getPluralName();
            if (this.jsonData.containsKey(contentName)) {
                ((ArrayList<Object>)this.jsonData.get(contentName)).add(jsoml.getJsonObject());
            } else {
                ArrayList<Object> array = new ArrayList<Object>();
                array.add(jsoml.getJsonObject());
                this.jsonData.put(contentName, array);
            }
		}
	}
	
	// Specific to JSON
	private Object createJsonText(String text) {
		return createJsonAttribute(XML_TEXT, text);
	}
	private Object createJsonAttribute(String propertyName, Object propertyValue) {
        Map<String,Object> map = new LinkedHashMap<String,Object>();
		map.put(propertyName, propertyValue);
		return map;
	}
	
	@Override
	public String toString() {
		if (this.xml) {
            Document doc = new Document(this.xmlElement);
            return doc.toString();
		} else {
			return this.jsonData.toString();
		}
	}
}
