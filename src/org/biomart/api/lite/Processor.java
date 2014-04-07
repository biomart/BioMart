package org.biomart.api.lite;

import java.io.Serializable;
import java.lang.reflect.Field;
import javax.xml.bind.annotation.XmlRootElement;

import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.processors.ProcessorInterface;
import org.biomart.processors.fields.BaseField;

@XmlRootElement(name="processor")
public class Processor extends LiteMartConfiguratorObject implements Serializable {
	private static final long serialVersionUID = 1L;

    public <T extends ProcessorInterface> Processor(Class<T> clazz) {
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            Class type = field.getType();
            if (BaseField.class.isAssignableFrom(type)) {
                String name = field.getName();
            }
        }
    }

    @Override
	protected Jsoml generateExchangeFormat(boolean xml) throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());
		return jsoml;
    }
}