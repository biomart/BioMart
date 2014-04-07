package org.biomart.api.lite;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

@XmlRootElement(name="filterData")
public class FilterData extends LiteMartConfiguratorObject {
    private String name;

	private String displayName;

	@XmlAttribute(name="isSelected")
	@JsonProperty("isSelected")
	private boolean isSelected;

    public FilterData() {}

	public FilterData(org.biomart.configurator.model.object.FilterData fd) {
		this.name = fd.getValue();
		this.displayName = fd.getDisplayName();
		this.isSelected = fd.isSelected();
	}

    @Override
	public String getName() {
		return name;
	}

    @Override
	public String getDisplayName() {
		return displayName;
	}

    @Override
    @XmlTransient
    public String getDescription() {
        return null;
    }

    @JsonIgnore
	public boolean isSelected() {
		return isSelected;
	}

	@Override
	protected Jsoml generateExchangeFormat(boolean xml) throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());

		jsoml.setAttribute("name", this.name);
		jsoml.setAttribute("displayName", this.displayName);
		jsoml.setAttribute("isSelected", this.isSelected);

        return jsoml;
    }
}