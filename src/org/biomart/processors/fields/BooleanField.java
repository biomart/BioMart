package org.biomart.processors.fields;

import java.util.Map;
import org.biomart.common.exceptions.TechnicalException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author jhsu
 */
public final class BooleanField extends BaseField<Boolean> {
    public BooleanField() {
        super(Boolean.class, null);
    }

    public BooleanField(Map<String,String> choices) {
        super(Boolean.class);
    }

    @Override
    public Boolean parseValue(String str) throws TechnicalException {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }
}
