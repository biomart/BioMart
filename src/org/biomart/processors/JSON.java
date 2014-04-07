package org.biomart.processors;

import com.google.common.base.Function;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.biomart.objects.objects.Attribute;
import org.biomart.processors.annotations.ContentType;
import org.biomart.processors.annotations.FieldInfo;
import org.biomart.processors.fields.BooleanField;
import org.biomart.processors.fields.StringField;
import org.biomart.queryEngine.QueryElement;
import org.biomart.common.exceptions.BioMartQueryException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdom.Document;

/**
 *
 * @author jhsu
 *
 * JSON Processor
 *
 * Returns data in JSON format. Can be optionally set to stream results.
 *
 */
@ContentType("application/json")
public class JSON extends ProcessorImpl {
    @FieldInfo(displayName="Stream Results", required=true, defaultValue="false", clientDefined=true)
    private final BooleanField streaming = new BooleanField();

    // Callback function for JSONP
    @FieldInfo(displayName="Callback Function", defaultValue="", clientDefined=true)
    private final StringField callbackFunction = new StringField();

    private final List<String> header = new ArrayList<String>();

    private final Function fn; // callback for query engine

    // For non-streaming requests
    private OutputStream originalOut;
    private boolean isJsonp;

    public JSON() {
        fn = new JsonCallback();
    }

    private class JsonCallback implements Function<String[],Boolean> {
        private int total = 0;

        public int getTotal() { return total; }

        @Override
        public Boolean apply(String[] columns) {
            Object value = new LinkedHashMap<String,Object>();
            int i = 0;

            for (String curr : columns) {
                ((Map)value).put(header.get(i), curr);
                i++;
            }

            if (streaming.value) {
                Map map = new HashMap();
                map.put("data", value);
                value = map;
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                String jsonRow = mapper.writeValueAsString(value);

                if (streaming.value && !"".equals(callbackFunction.value)) {
                    jsonRow = callbackFunction.value + "(" + jsonRow + ")";
                }

                out.write(jsonRow.getBytes());

                if (streaming.value) {
                    out.write(NEWLINE);
                } else {
                    out.write(',');
                }
            } catch (IOException e) {
                throw new BioMartQueryException(e);
            }

            total++;
            return false;
        }
    };

    @Override
    public Function getCallback() {
        return fn;
    }

    @Override
    public void preprocess(Document queryXML) {
        // No need to print header
        queryXML.getRootElement().setAttribute("header", "false");

        // If is JSONP, then we need to wrap results in callbackFunction function at the end
        isJsonp = !streaming.value && !"".equals(callbackFunction.value);
    }

    @Override
    public void setOutputStream(OutputStream outputHandle) {
        // Have to redirect output if not streaming
        originalOut = outputHandle;

        // Store data if not streaming
        if (!streaming.value) {
            out = new ByteArrayOutputStream();
        } else {
            out = originalOut;
        }

        List<QueryElement> attributes = query.getOriginalAttributeOrder();

        // Store the display names
        for (QueryElement element : attributes) {
            String displayName = ((Attribute)element.getElement()).getDisplayName();
            header.add(displayName);
        }
    }

    @Override
    public void done() {
        int total = ((JsonCallback)fn).getTotal();

        if (streaming.value) {
            if (!"".equals(callbackFunction.value)) {
                writeSilently(originalOut, (callbackFunction.value + "({\"status\":\"success\",\"total\":" + total + "})").getBytes());
            } else {
                writeSilently(originalOut, ("{\"status\":\"success\",\"total\":" + total + "}").getBytes());
            }
        } else {
            String results = out.toString();

            if (isJsonp) {
                writeSilently(originalOut, (callbackFunction.value + "(").getBytes());
            }

            // Write header
            writeSilently(originalOut, ("{\"total\":" + total + ",\"data\":[").getBytes());

            // Remove last trailing comma
            writeSilently(originalOut, results.substring(0, results.length()-1).getBytes());

            // End
            writeSilently(originalOut, "]}".getBytes());

            if (isJsonp) {
                writeSilently(originalOut, ")".getBytes());
            }
        }
    }
}
