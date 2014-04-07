package org.biomart.processors;

import com.google.common.base.Function;
import java.io.OutputStream;
import org.biomart.queryEngine.Query;
import org.jdom.Document;

/**
 *
 * @author Syed Haider
 *
 * This class is the entry point to query engine. All processors implements the
 * printResults method to comply with ONE generic way of returning the results
 * Processor specific logic of reorganising the results sit within each processor
 */

public interface ProcessorInterface {
    public String getContentType();

    public Function<String,Boolean> getCallback();

    public Function<String,Boolean> getErrorHandler();

    public void preprocess(Document queryXML);

    public void setQuery(Query query);

    public void setOutputStream(OutputStream out);

    public void done();

    public boolean accepts(String[] accepts);

    public String getDefaultValueForField(String name);

    public boolean isClientDefined(String name);

    public boolean isRequired(String name);

    public String[] getFieldNames();

    public void setFieldValue(String name, String value);
}
