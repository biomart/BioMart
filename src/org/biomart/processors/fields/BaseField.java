package org.biomart.processors.fields;

import java.util.Map;
import org.biomart.common.exceptions.TechnicalException;

/**
 *
 * @author jhsu
 */
public abstract class BaseField<T> {
    protected final Class<T> clazz;
    protected final Map<T,String> choices;

    public T value;

    protected BaseField(Class<T> clazz) {
        this(clazz, null);
    }

    protected BaseField(Class<T> clazz, Map<T,String> choices) {
        this.clazz = clazz;
        this.choices = choices;
    }

    public abstract T parseValue(String str) throws TechnicalException;

    public final void setValue(String str) throws TechnicalException {
        T t = parseValue(str);

        if (choices != null && !choices.containsKey(t)) {
            throw new TechnicalException("Invalid value for field: " + value);
        }

        value = t;
    }

    public final T getValue() {
        return value;
    }

    public final Class getType() {
        return clazz;
    }

    public final Map<T,String> getChoices() {
        return choices;
    }
}
