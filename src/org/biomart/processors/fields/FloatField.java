package org.biomart.processors.fields;

import java.util.Map;
import org.biomart.common.exceptions.TechnicalException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author jhsu
 */
public final class FloatField extends BaseField<Float> {
    public FloatField(String label) {
        super(Float.class, null);
    }

    public FloatField(String label, Map<String,String> choices) {
        super(Float.class);
    }

    @Override
    public Float parseValue(String str) throws TechnicalException {
        if (isNullOrEmpty(str)) {
            return 0f;
        }
        return Float.parseFloat(str);
    }
}
