package org.biomart.processors.formatters;

import org.apache.commons.lang.StringEscapeUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import org.biomart.common.resources.Log;

/**
 *
 * @author jbaran
 */
public class SPARQLXML extends TSVFormatter {

    protected boolean exception = false;

    private boolean[] visibleColumn;

    protected String[] variableNames;

    public SPARQLXML(OutputStream out,
            String[] variableNames,
            String[] variableProperties,
            String[] variableTypes,
            Map<String, String> variable2URIAttributes,
            String prelude,
            String exception) {
        super(out);
        try {

            if (exception != null) {
                out.write(("<exception>" + exception + "</exception>").getBytes());
                this.exception = true;
                return;
            }

            out.write("<?xml version=\"1.0\"?>\n".getBytes());

            if (prelude != null)
                out.write(prelude.getBytes());

            this.variableNames = variableNames;
            this.visibleColumn = new boolean[variableNames.length];

            out.write("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">\n\t<head>\n".getBytes());

            for (int i = 0, j = 0; i < variableNames.length; i++) {
                String variableName = variableNames[i];

                if (!variableName.startsWith("?") &&  // Un-projected query variables
                    !variableName.startsWith("!")) {  // Virtual variables for filters
                    out.write(("\t\t<variable name=\"" + variableName + "\"/>\n").getBytes());
                    visibleColumn[i] = true;
                    j = i;
                }
            }

            out.write("\t</head>\n\t<results>\n".getBytes());
        } catch (IOException e) {
            Log.error("IOException in SPARQLXML formatter constructor.", e);
        }
    }

    @Override
    public String transposeCharacter(int b) {
        if (exception) return "";

        return StringEscapeUtils.escapeHtml(Character.toString((char)b));
    }

    @Override
    public void writeStartOfLine() throws IOException {
        out.write(("\t\t<result>\n").getBytes());
    }

    @Override
    public void writeEndOfLine() throws IOException {
        out.write("\t\t</result>\n".getBytes());
    }
    
    @Override
    public void writeField(int column, String value) throws IOException {
        if (!visibleColumn[column])
            return;
        
        out.write(("\t\t\t<binding name=\"" + variableNames[column] + "\">\n\t\t\t\t<literal>").getBytes());
        out.write(value.getBytes());
        out.write(("</literal>\n\t\t\t</binding>\n").getBytes());
    }
    
    @Override
    public void close() throws IOException {
        if (exception) return;

        out.write("\t</results>\n</sparql>\n".getBytes());
        super.close();
    }
}
