package org.biomart.api;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author jhsu
 */
public class Query {
    private final Portal _portal;

    private String _processor = "TSV";
    private int _limit = -1;
    private boolean _header = true;
    private String _client = "biomartclient";
    private final List<Dataset> _datasets = new ArrayList<Dataset>();
    private boolean _useDino = true;

	private boolean isCountQuery;

    /*
     * Inner query objects
     */
    public class Dataset {
        private final Query query;
        private final String name;
        @CheckForNull private final String config;
        private final List<Attribute> attributes = new ArrayList<Attribute>();
        private final List<Filter> filters = new ArrayList<Filter>();

        private Dataset(final Query query, @Nonnull String name, String config) {
            this.query = query;
            this.name = name;
            this.config = config;
            query._datasets.add(this);
        }

        public Dataset addAttribute(@Nonnull String name) {
            attributes.add(new Attribute(name));
            return this;
        }

        public Dataset addFilter(@Nonnull String name, @Nonnull String value) {
            filters.add(new Filter(name, value));
            return this;
        }

        public Query end() {
            return query;
        }
    }

    private class Attribute {
        private final String name;
        private Attribute(String name) { this.name = name; }
    }

    private class Filter {
        private final String name;
        private final String value;
        private Filter(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /*
     * Public methods
     */
    public Query(Portal portal) {
        _portal = portal;
    }

    public Query setProcessor(@Nonnull String processor) {
        _processor = processor;
        return this;
    }

    public Query setLimit(int limit) {
        _limit = limit;
        return this;
    }

    public Query setHeader(boolean header) {
        _header = header;
        return this;
    }

    public Query setClient(@Nonnull String client) {
        _client = client;
        return this;
    }

    public Dataset addDataset(@Nonnull String name, @Nullable String config) {
        return new Dataset(this, name, config);
    }

    public void getResults(OutputStream out) {
        String xml = getXml();
        _portal.executeQuery(xml, out, isCountQuery);
    }

	public void getisCountQuery(boolean isCountQuery) {
		this.isCountQuery = isCountQuery;
	}

	public boolean getisCountQuery() {
		return isCountQuery;
	}
	
	public Query setUseDino(boolean use) {
	    _useDino = use;
	    return this;
	}
	
	public boolean getUseDino() {
	    return _useDino;
	}

    /*
     * e.g.
     * <?xml version="1.0" encoding="UTF-8"?>
     * <!DOCTYPE Query>
     * <Query client="" processor="TSV" limit="-1" header="1">
     * <Dataset name="hsapiens_gene_ensembl" config="hsapiens_gene_ensembl_config">
     * <Filter name="chromosome_name" value="1"/><Attribute name="ensembl_gene_id"/>
     * <Attribute name="canonical_transcript_stable_id"/>
     * <Attribute name="start_position"/>
     * <Attribute name="end_position"/>
     * </Dataset>
     * </Query>
     */
    public String getXml() {
        try {
            Element root = new Element("Query");
            Document doc = new Document(root)
                .setDocType(new DocType("Query"));

            root
                .setAttribute("client", _client)
                .setAttribute("processor", _processor)
                .setAttribute("limit", ""+_limit)
                .setAttribute("header", _header ? "1" : "0")
                .setAttribute("useDino", Boolean.toString(_useDino));

            for (Dataset d : _datasets) {
                Element ds = new Element("Dataset").setAttribute("name", d.name);

                if (d.config != null) ds.setAttribute("config", d.config);

                for (Filter f : d.filters) {
                    ds.addContent(new Element("Filter")
                            .setAttribute("name", f.name)
                            .setAttribute("value", f.value));
                }

                for (Attribute a : d.attributes) {
                    ds.addContent(new Element("Attribute").setAttribute("name", a.name));
                }

                root.addContent(ds);
            }

            return new XMLOutputter().outputString(doc);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BioMartApiException(e.getMessage());
        }
    }
}
