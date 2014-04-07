package org.biomart.processors;

import com.google.common.io.Closeables;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.biomart.processors.annotations.ContentType;
import org.jdom.Comment;

/**
 *
 * @author Joachim Baran
 *
 */
@ContentType("text/n3")
public class RDFN3 extends ProcessorImpl {
    @Override
    public void setOutputStream(OutputStream out) {
        String prelude = null;
        Map<String, String> namespaces = new HashMap<String, String>();
        String[] variableNames = new String[0];
        String[] variableProperties = new String[0];
        String[] variableTypes = new String[0];
        Map<String, String> variable2URIAttributes = new HashMap<String, String>(); // TODO For now: only one URI attribute supported
        String exception = null;
        for (Object content : query.queryXMLobject.getContent()) {
            if (content instanceof Comment) {
                Comment comment = (Comment)content;

                // Variable names starting with a "?" are used only internally
                // and their values are not returned.

                if (comment.getText().startsWith(" VARIABLES:")) {
                    variableNames = comment.getText().substring(11).trim().split(" ");
                } else if (comment.getText().startsWith(" VARIABLEPROPERTIES:")) {
                    variableProperties = comment.getText().substring(20).trim().split(" ");
                } else if (comment.getText().startsWith(" VARIABLETYPES:")) {
                    variableTypes = comment.getText().substring(15).trim().split(" ");
                } else if (comment.getText().startsWith(" NAMESPACE:")) {
                    String[] prefix2URI = comment.getText().substring(11).trim().split(" ");
                    namespaces.put(prefix2URI[0], prefix2URI[1]);
                } else if (comment.getText().startsWith(" NODEATTRIBUTES:")) {
                    String[] nodeAttributes = comment.getText().substring(16).trim().split(" ");
                    // TODO For now: only one URI attribute supported
                    for (String uri : nodeAttributes) {
                        String[] nameURIPair = uri.trim().split(":");
                        variable2URIAttributes.put(nameURIPair[0], nameURIPair[1]);
                    }
                } else if (comment.getText().startsWith(" BioMart XML-Query:")) {
                    prelude = "<!--" + comment.getText() + "-->\n";
                } else if (comment.getText().startsWith(" SPARQL-Exception:")) {
                    exception = comment.getText().substring(18);
                }
            }
        }

        this.out = new org.biomart.processors.formatters.RDF(out,
                variableNames,
                variableProperties,
                variableTypes,
                variable2URIAttributes,
                namespaces,
                prelude,
                exception,
                org.biomart.processors.formatters.RDF.FORMAT.N3);
    }
    
    @Override
    public void done() {
        Closeables.closeQuietly(out);
    }
}
