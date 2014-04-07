package org.biomart.processors.formatters;

import org.apache.commons.lang.StringEscapeUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.biomart.common.resources.Log;

/**
 *
 * @author jbaran
 */
public class RDF extends TSVFormatter {

    public enum FORMAT { N3 };

    private FORMAT format;
    
    protected boolean exception = false;

    private boolean[] visibleColumn;
    protected String[] variableNames;
    protected String[] variableProperties;
    protected String[] variableTypes;
    protected List<String> columnValues = new LinkedList<String>();

    public RDF(OutputStream out,
            String[] variableNames,
            String[] variableProperties,
            String[] variableTypes,
            Map<String, String> variable2URIAttributes,
            Map<String, String> namespaces,
            String prelude,
            String exception,
            FORMAT format) {
        super(out);
        
        this.format = format;
        
        try {

            if (exception != null) {
                out.write(("<exception>" + exception + "</exception>").getBytes());
                this.exception = true;
                return;
            }

            //out.write("<?xml version=\"1.0\"?>\n".getBytes());

            if (prelude != null && format != FORMAT.N3)
                out.write(prelude.getBytes());

            this.variableNames = variableNames;
            this.variableProperties = variableProperties;
            this.variableTypes = variableTypes;
            this.visibleColumn = new boolean[variableNames.length];

            //out.write("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">\n\t<head>\n".getBytes());

            for (int i = 0, j = 0; i < variableNames.length; i++) {
                String variableName = variableNames[i];

                if (!variableName.startsWith("?") &&  // Un-projected query variables
                    !variableName.startsWith("!")) {  // Virtual variables for filters
                    //out.write(("\t\t<variable name=\"" + variableName + "\"/>\n").getBytes());
                    visibleColumn[i] = true;
                    j = i;
                }
            }

            //out.write("\t</head>\n\t<results>\n".getBytes());
            switch(format) {
            case N3:
                for (String prefix : namespaces.keySet())
                    out.write(("@prefix " + prefix + " <" + namespaces.get(prefix) + ">\n").getBytes());
                break;
            default:
                break;
            }
        } catch (IOException e) {
            Log.error("IOException in RDF formatter constructor.", e);
        }
    }

    @Override
    public String transposeCharacter(int b) {
        if (exception) return "";

        return StringEscapeUtils.escapeHtml(Character.toString((char)b));
    }

    @Override
    public void writeStartOfLine() throws IOException {
        //out.write(("\t\t<result>\n").getBytes());
    }

    @Override
    public void writeEndOfLine() throws IOException {
        writeN3();
    }
    
    @Override
    public void writeField(int column, String value) throws IOException {
        columnValues.add(value);
    }
    
    @Override
    public void close() throws IOException {
        if (exception) return;

        //out.write("\t</results>\n</sparql>\n".getBytes());
        super.close();
    }
    
    private void writeN3() throws IOException {
        int variableNo = 0;
        
        for (int column = 0; column < visibleColumn.length; column++) {
            if (!visibleColumn[column])
                continue;
            
            String nodeURI = "";
            for (int i = 0; i < variableNames.length; i++) {
                String variableName = variableNames[i].replaceFirst("\\?", "");
                if (variableTypes[variableNo].equals(variableName))
                    nodeURI = "_:" + columnValues.get(i);
            }
            
            out.write(nodeURI.getBytes());
            out.write("\t".getBytes());
            out.write(variableProperties[variableNo].getBytes());
            out.write("\t\"".getBytes());
            out.write(columnValues.get(column).getBytes());
            out.write("\"\n".getBytes());
            
            variableNo++;
        }
        
        columnValues.clear();
    }
}
