package org.biomart.processors.fields;

import java.util.Map;
import org.biomart.common.exceptions.TechnicalException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author jhsu
 */
public final class IntegerField extends BaseField<Integer> {
    public IntegerField() {
        super(Integer.class, null);
    }

    public IntegerField(Map<String,String> choices) {
        super(Integer.class);
    }

    @Override
    public Integer parseValue(String str) throws TechnicalException {
        if (isNullOrEmpty(str)) {
            return 0;
        }
        return Integer.parseInt(str);
    }
}
