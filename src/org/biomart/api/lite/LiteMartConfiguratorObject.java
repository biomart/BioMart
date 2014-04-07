package org.biomart.api.lite;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.utils2.XmlUtils;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdom.Document;
import org.jdom.Element;

@XmlRootElement(name="config")
@JsonPropertyOrder({ "name", "displayName", "description" })
public abstract class LiteMartConfiguratorObject implements Serializable {

	private static final long serialVersionUID = -8493793578216273507L;
	private MartConfiguratorObject martConfiguratorObject;
    private static ObjectMapper mapper = new ObjectMapper();

    protected LiteMartConfiguratorObject() {}

	protected LiteMartConfiguratorObject(MartConfiguratorObject mcObject) {
		this.martConfiguratorObject = mcObject;
	}
		
	// only property always available for all subclasses
    @XmlAttribute(name="name")
    @JsonProperty("name")
	public String getName() {
		return this.martConfiguratorObject.getName();
	}

    @JsonIgnore
	public String getInternalName() {
		return this.martConfiguratorObject.getInternalName();
	}
	
    @XmlAttribute(name="displayName")
    @JsonProperty("displayName")
	public String getDisplayName() {
		String rval = this.martConfiguratorObject.getDisplayName();
        if ("".equals(rval)) {
            rval = getName();
        }
        return rval;
	}

    @XmlAttribute(name="description")
    @JsonProperty("description")
	public String getDescription() {
		return this.martConfiguratorObject.getDescription();
	}
	
    @JsonIgnore
	public String getXMLElementName() {
		return this.martConfiguratorObject.getNodeType().toString();
	}
	

	
	@Override
	public String toString() {
		return this.getName();			
	}

	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		LiteMartConfiguratorObject liteMartConfiguratorObject=(LiteMartConfiguratorObject)object;
		return (
			this.getClass().equals(object.getClass()) &&
			this.getName().equals(liteMartConfiguratorObject.getName())		
		);
	}

	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}
	
    /* 
     * Methods for getting JDOM or JSON object
     */
	public static Document getXmlDocument(List<? extends LiteMartConfiguratorObject> liteMartConfiguratorObjectList) throws FunctionalException {
		Document document = new Document();
		Element rootElement = new Element("list");
		document.setRootElement(rootElement);	
		for (LiteMartConfiguratorObject liteMartConfiguratorObject : liteMartConfiguratorObjectList) {
			rootElement.addContent(liteMartConfiguratorObject.getXmlElement());
		}
		return document;
	}

    @JsonIgnore
	public Document getXmlDocument() throws FunctionalException {
		Document document = new Document();
		Element rootElement = getXmlElement();
		document.setRootElement(rootElement);
		return document;
	}

	public static Object getJsonObject(List<? extends LiteMartConfiguratorObject> liteMartConfiguratorObjectList) throws FunctionalException {
        ArrayList<Object> array = new ArrayList<Object>();
		for (LiteMartConfiguratorObject liteMartConfiguratorObject : liteMartConfiguratorObjectList) {
            array.add(liteMartConfiguratorObject.getJsonObject());
		}
        return array;
	}

    @JsonIgnore
	public Object getJsonObject() throws FunctionalException {
		return generateExchangeFormat(false).getJsonObject();
	}
	
    @JsonIgnore
	private Element getXmlElement() throws FunctionalException {	//TODO need to synchronize names with objects/portal/gui
		return generateExchangeFormat(true).getXmlElement();
	}

	protected abstract Jsoml generateExchangeFormat(boolean xml) throws FunctionalException;


    /*
     * Methods for generating XML or JSON string
     */
	public String toXmlString() throws FunctionalException, TechnicalException {
        return XmlUtils.getXmlDocumentString(getXmlDocument());
	}

    public String toJsonString() throws FunctionalException {
        try {
            return mapper.writeValueAsString(getJsonObject());
        } catch (IOException e) {
        }
        return null;
    }


	public static String toXmlString(List<? extends LiteMartConfiguratorObject> liteMartConfiguratorObjectList)
               throws TechnicalException, FunctionalException {
        return XmlUtils.getXmlDocumentString(getXmlDocument(liteMartConfiguratorObjectList));
    }


    public static String toJsonString(List<? extends LiteMartConfiguratorObject> liteMartConfiguratorObject) throws FunctionalException {
        try {
            return mapper.writeValueAsString(getJsonObject(liteMartConfiguratorObject));
        } catch (IOException e) {
        }
        return null;
    }

	public static String mapToXmlString(Map<String,Object> map)
               throws TechnicalException, FunctionalException {
		Document document = new Document();
		document.setRootElement(LiteMartConfiguratorObject.mapToJsoml(map, true).getXmlElement());
        return XmlUtils.getXmlDocumentString(document);
    }


    public static String mapToJsonString(Map<String,Object> map ) throws FunctionalException {
        try {
            return mapper.writeValueAsString(LiteMartConfiguratorObject.mapToJsoml(map, false).getJsonObject());
        } catch (IOException e) {
        }
        return null;
    }

    private static Jsoml mapToJsoml(Map<String,Object> map, boolean xml) throws FunctionalException {
        Jsoml jsoml = new Jsoml(xml, "map");
        for (Map.Entry<String,Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (value instanceof List) {
                Jsoml curr = new Jsoml(xml, "dataset");
                curr.setAttribute("name", key);
                for (LiteMartConfiguratorObject obj : (List<? extends LiteMartConfiguratorObject>)value) {
                    curr.addContent(obj.generateExchangeFormat(xml));
                }
                jsoml.addContent(curr);
            } else {
                jsoml.setAttribute(key, ((LiteMartConfiguratorObject)value).generateExchangeFormat(xml));
            }
        }
        return jsoml;
    }

}
