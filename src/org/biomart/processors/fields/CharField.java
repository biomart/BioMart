package org.biomart.processors.fields;

import java.util.Map;
import org.biomart.common.exceptions.TechnicalException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author jhsu
 */
public final class CharField extends BaseField<Character> {
    public CharField() {
        super(Character.class, null);
    }

    public CharField(Map<String,String> choices) {
        super(Character.class);
    }

    @Override
    public Character parseValue(String str) throws TechnicalException {
        if (isNullOrEmpty(str)) {
            return 0;
        }
        return str.charAt(0);
    }
}
