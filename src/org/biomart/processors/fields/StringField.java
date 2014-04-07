package org.biomart.processors.fields;

import java.util.Map;
import org.biomart.common.exceptions.TechnicalException;

/**
 *
 * @author jhsu
 */
public final class StringField extends BaseField<String> {
    public StringField() {
        super(String.class, null);
    }

    public StringField(Map<String,String> choices) {
        super(String.class);
    }

    @Override
    public String parseValue(String str) throws TechnicalException {
        return str;
    }
}
