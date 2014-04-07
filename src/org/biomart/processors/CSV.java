package org.biomart.processors;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Function;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.biomart.common.constants.OutputConstants;
import org.biomart.processors.annotations.FieldInfo;
import org.biomart.processors.fields.CharField;
import org.biomart.common.exceptions.BioMartQueryException;

/**
 *
 * @author jhsu
 *
 * CSV Processor
 *
 * Returns data in comma-separated format.
 *
 */
public class CSV extends ProcessorImpl {
    @FieldInfo(displayName="Delimiter", defaultValue=",", required=true, clientDefined=true)
    private final CharField delimiter = new CharField();

    @FieldInfo(displayName="Quote Character", defaultValue="\"", required=true, clientDefined=true)
    private final CharField quote = new CharField();

    private final CsvCallback fn; // callback for query engine

    public CSV() {
        fn = new CsvCallback();
    }

    @Override
    public Function getCallback() {
        return fn;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        super.setOutputStream(out);
        fn.initWriter(out);
    }

    private class CsvCallback implements Function<String[],Boolean>, OutputConstants {
        private CSVWriter writer;

        public void initWriter(OutputStream out) {
            writer = new CSVWriter(new OutputStreamWriter(out), delimiter.value, quote.value);
        }

        @Override
        public Boolean apply(String [] row) {
            try {
                writer.writeNext(row);
                writer.flush();
            } catch (IOException e) {
                throw new BioMartQueryException(e);
            }
            return false;
        }
    }
}
