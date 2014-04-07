package org.biomart.queryEngine.v2.base;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import org.biomart.common.exceptions.BioMartIOException;

/**
 *
 * @author jhsu
 *
 * Fetches results from web resource; data from resource assumed to be in TSV format.
 *
 * The input to WebQueryJob is the query XML.
 */
public class WebQueryJob extends QueryJob {
    // For splitting each row into columns
    protected final Splitter splitter = Splitter.on('\t').trimResults();

    protected final String url;

    public WebQueryJob(String url) {
        this.url = url;
    }

    @Override
    public List<String[]> apply(Dataset dataset) {
        // Get the InputStream using a combination of the query XML and URL
        String queryXml = "<Query processor=\"TSV\" limit=\"-1\" header=\"true\">" + dataset.toString() + "</Query>";
        InputStream is = getInputStream(queryXml);

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        // The result to return
        List<String[]> rows = new ArrayList<String[]>();

        String line = null;
        int count = 0;

        try {
            // Read in each line, and break if we reach the fetchSize
            while ((line = reader.readLine()) != null) {
                String[] row = Iterables.toArray(splitter.split(line), String.class);
                rows.add(row);

                // Increment and break if the incremented count > fetchSize
                if (++count >= super.fetchSize) {
                    break;
                }
            }
        } catch (IOException e) {
            // We can't handle this here, so we'll wrap it in a runtime exception
            throw new BioMartIOException(e);
        } finally {
            Closeables.closeQuietly(is);
        }

        return rows;
    }

    protected InputStream getInputStream(String queryXml) {
        // Using Jersey client
        Client c = new Client();
        WebResource resource = c.resource(url);

        // Adding query as form data
        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("query", queryXml);

        // Read as InputStream
        return resource.type("application/x-www-form-urlencoded").post(InputStream.class, formData);
    }
}
