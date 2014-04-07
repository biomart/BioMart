package org.biomart.queryEngine.v2.base;

import com.google.common.collect.ImmutableList;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.biomart.common.exceptions.BioMartException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author jhsu
 *
 * Holds relevant information about a Query. Constructed from an XML string.
 *
 */
public class Query {
    public final boolean header; // Whether to include header
    public final int limit; // Number of rows to return (excluding header)
    public final String client; // Client name that is making the request
                                // Not relevant to query execution, purely for analytics
    public final List<Dataset> datasets;

    public Query(String xml) {
        datasets = new ArrayList<Dataset>();

        SAXBuilder builder = new SAXBuilder();
        Reader in = new StringReader(xml);

        try {
            Document doc = builder.build(in);
            Element root = doc.getRootElement();
            String limitValue, headerValue;

            // Set query options
            if ((limitValue = root.getAttributeValue("limit")) != null) {
                limit = Integer.parseInt(limitValue);
            } else {
                limit = Integer.MAX_VALUE;
            }

            if ((headerValue = root.getAttributeValue("header")) != null) {
                // also support 1 and 0 for true and false respectively
                if ("1".equals(headerValue)) headerValue = "true";

                header = Boolean.parseBoolean(headerValue);

            } else {
                header = true;
            }

            client = root.getAttributeValue("client");

            // Build Datasets
            List<Element> datasetElements = root.getChildren("Dataset");

            for (Element element : datasetElements) {
                Dataset ds = getDataset(element);
                datasets.add(ds);
            }

        } catch (Exception e) {
            throw new BioMartException("Problem building Query object from XML: " + xml, e);
        }
    }

    /*
     * Builds a complete Dataset object from a <Dataset/> element. Includes
     * Filters and Attributes.
     */
    private Dataset getDataset(Element datasetElement) {
        // Read Dataset properties from datasetElement
        String datasetName = datasetElement.getAttributeValue("name");
        String configName = datasetElement.getAttributeValue("config");

        Dataset ds = new Dataset(datasetName, configName);

        // Add filters
        for (Element filterElement : (List<Element>)datasetElement.getChildren("Filter")) {
            String name = filterElement.getAttributeValue("name");
            String value = filterElement.getAttributeValue("value");
            Filter filter = new Filter(name, value);
            ds.filters.add(filter);
        }

        // Add attributes
        for (Element attributeElement : (List<Element>)datasetElement.getChildren("Attribute")) {
            String attributeName = attributeElement.getAttributeValue("name");
            Attribute attribute = new Attribute(attributeName);
            ds.attributes.add(attribute);
        }

        return ds;
    }
}