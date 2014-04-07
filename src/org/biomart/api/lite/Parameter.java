package org.biomart.api.lite;

import java.io.Serializable;
import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;

/**
 *
 * @author jhsu
 */
public class Parameter extends LiteMartConfiguratorObject implements Serializable {
	private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;
    private String description;
    private boolean clientDefined;
    private boolean isRequired;
    private Class type;

    public Parameter(String name, String displayName, String description,
            boolean clientDefined, boolean isRequired, Class type) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.clientDefined = clientDefined;
        this.isRequired = isRequired;
        this.type = type;
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
    public String getDescription() {
        return description;
    }

    @Override
	protected Jsoml generateExchangeFormat(boolean xml) throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());
		return jsoml;
    }
}
