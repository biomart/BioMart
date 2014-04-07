package org.biomart.processors;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import org.apache.commons.lang.ArrayUtils;
import org.biomart.common.constants.OutputConstants;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.processors.annotations.ContentType;
import org.biomart.processors.annotations.FieldInfo;
import org.biomart.processors.fields.BaseField;
import org.biomart.queryEngine.Query;
import org.biomart.common.exceptions.BioMartQueryException;
import org.jdom.Document;


import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu
 */
public class ProcessorImpl implements ProcessorInterface, OutputConstants {
    protected OutputStream out;
    protected String contentType = "text/plain";
    protected Query query;

    protected ProcessorImpl() {}

    protected final class DefaultWriteFunction implements Function<String[],Boolean> {
        private static final int FLUSH_INTERVAL = 5;
        private int count = 0;

        @Override
        public Boolean apply(String[] row) {
            String line = Joiner.on('\t').join(row);
            try {
                out.write(line.getBytes());
                out.write(NEWLINE);

                // Force output to be written to client's stream
                if (++count % FLUSH_INTERVAL == 0) {
                    out.flush();
                }
            } catch (IOException e) {
                throw new BioMartQueryException("Problem writing to OutputStream", e);
            }
            return false; // Let QueryRunner decide when to stop
        }
    }

    protected final class DefaultErrorHandler implements Function<String,Boolean> {
        @Override
        public Boolean apply(String reason) {
            try {
                out.write(ERROR_STRING.getBytes());
            } catch (IOException e) {
                throw new BioMartQueryException("Problem writing to OutputStream", e);
            }
            return true;
        }
    }

    @Override
    public void preprocess(Document queryXML) {}

    @Override
    public void setQuery(Query query) {
        this.query = query;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public Function getCallback() {
        return new DefaultWriteFunction();
    }


    @Override
    public Function getErrorHandler() {
        return new DefaultErrorHandler();
    }

    @Override
    public void done() {}

    @Override
    public final boolean accepts(String[] accepts) {
        boolean accepted = false;
        Class clazz = this.getClass();
        ContentType type = (ContentType)clazz
                .getAnnotation(ContentType.class);

        if (type != null && accepts != null) {
            contentType = type.value()[0];

            for (String curr : accepts) {
                if (ArrayUtils.contains(type.value(), curr)) {
                    contentType = curr;
                    accepted = true;
                    break;
                }
            }
        }

        return accepted;
    }

    @Override
    public final String getContentType() {
        return contentType;
    }

    @Override
    public final boolean isClientDefined(String name) {
        try {
            Field field = this.getClass().getDeclaredField(name);
            if (field.isAnnotationPresent(FieldInfo.class)) {
                FieldInfo info = field.getAnnotation(FieldInfo.class);
                return info.clientDefined();
            }
        } catch (NoSuchFieldException e) {
            // nothing
        }
        return false;
    }

    @Override
    public final boolean isRequired(String name) {
        try {
            Field field = this.getClass().getDeclaredField(name);
            if (field.isAnnotationPresent(FieldInfo.class)) {
                FieldInfo info = field.getAnnotation(FieldInfo.class);
                return info.required();
            }
        } catch (NoSuchFieldException e) {
            // nothing
        }
        return false;
    }

    @Override
    public final String getDefaultValueForField(String name) {
        try {
            Field field = this.getClass().getDeclaredField(name);
            if (field.isAnnotationPresent(FieldInfo.class)) {
                FieldInfo info = field.getAnnotation(FieldInfo.class);
                return info.defaultValue();
            }
        } catch (NoSuchFieldException e) {
            // nothing
        }
        return null;
    }

    @Override
    public final String[] getFieldNames() {
        Field[] fields = this.getClass().getDeclaredFields();
        String[] fieldNames = new String[fields.length];
        for (int i=0; i<fields.length; i++) {
            fieldNames[i] = fields[i].getName();
        }
        Log.info("ProcessorImpl#getFieldNames. Fields: "+ ArrayUtils.toString(fieldNames));
        return fieldNames;
    }

    @Override
    public final void setFieldValue(String name, String value) {
        try {
            Field fld = this.getClass().getDeclaredField(name);
            fld.setAccessible(true);
            BaseField field = (BaseField)fld.get(this);
            field.setValue(value);
            Log.debug("ProcessorImpl#setFieldValue set field value "+ name +" to "+ value);
        } catch (Exception e) {
            throw new ValidationException("Cannot set value for field " + name, e);
        }
    }

    public static void writeSilently(OutputStream out, byte[] bytes) {
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new BioMartQueryException(e);
        }
    }
}
