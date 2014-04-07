package org.biomart.api.lite;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

@XmlRootElement(name="dataset")
public class Dataset extends LiteMartConfiguratorObject implements Serializable {

	private static final long serialVersionUID = -3575300976527663289L;

	private org.biomart.objects.objects.Dataset datasetObject;

    public Dataset() {}
	
	public Dataset(org.biomart.objects.objects.Dataset ds) {
		super(ds);
		this.datasetObject = ds;
	}
	

    @JsonIgnore
	private String getParentMartName() {
		return this.datasetObject.getParentMart().getName();
	}

    @XmlAttribute(name="isHidden")
    @JsonProperty("isHidden")
	public boolean isHidden() {
		return false;
	}
	
	@Override
	protected Jsoml generateExchangeFormat(boolean xml)
			throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());
		jsoml.setAttribute("name", super.getName());
		jsoml.setAttribute("displayName", super.getDisplayName());
		jsoml.setAttribute("isHidden", isHidden());
		jsoml.setAttribute("parent", getParentMartName());
		return jsoml;
	}
}
