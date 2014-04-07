package org.biomart.processors.sequence;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.biomart.common.constants.OutputConstants;
import org.biomart.common.exceptions.BioMartException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.processors.ProcessorImpl;
import org.biomart.processors.annotations.FieldInfo;
import org.biomart.processors.fields.IntegerField;
import org.biomart.processors.fields.StringField;
import org.biomart.common.exceptions.BioMartQueryException;

import org.jdom.Document;
import org.jdom.Element;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author jhsu, jguberman
 *
 */
public class Sequence extends ProcessorImpl implements SequenceConstants {
    private Connection conn = null;

    // Parser lookup map
    private static final Map<String,Class<? extends SequenceParser>> lookup;

    // Track count
    private int limit = Integer.MAX_VALUE;
    private int total = 0;
    private boolean hasResults = false;
    private boolean isDone = false;

    private String databaseTableName;
    private int headerAttributes = 0;

    private SequenceParser parser;

    // Cache rows until BATCH_SIZE is reached, then do the actual sequence retrieval
    private static final int BATCH_SIZE = 100;
    private List<String[]> buffer = new ArrayList<String[]>();
    private int count = 0;

    protected class SequenceCallback implements Function<String[],Boolean>, OutputConstants {
        @Override
        public Boolean apply(String[] row) {
            hasResults = true;

            if (!isDone) {

                buffer.add(row);

                if (++count > BATCH_SIZE-1) {
                    retrieveSequences();
                }
            }

            return isDone;
        }
    }

    private void retrieveSequences() {
        try {
            if (!isDone) {
                for (String[] row : buffer) {
                    String results = parser.parseLine(row);
                    if (!"".equals(results)) {
                        out.write((results + "\n").getBytes());
                        out.flush();
                        if (++total >= limit) {
                            Log.debug("Reached sequence limit");
                            isDone = true; // Yes, we're done!
                            break;
                        }
                    }
                }
            }
            buffer.clear();
            count = 0;
        } catch (Exception e) {
            throw new BioMartQueryException("Error during retrieveSequences()", e);
        }
    }

    static {
        lookup  = new ImmutableMap.Builder<String,Class<? extends SequenceParser>>()
            .put(QueryType.CDNA.getCode(), CDNAParser.class)
            .put(QueryType.CODING.getCode(), CodingParser.class)
            .put(QueryType.CODING_GENE_FLANK.getCode(), CodingGeneFlankParser.class)
            .put(QueryType.CODING_TRANSCRIPT_FLANK.getCode(), CodingTranscriptFlankParser.class)
            .put(QueryType.FIVE_UTR.getCode(), FiveUTRParser.class)
            .put(QueryType.GENE_EXON.getCode(), ExonParser.class)
            .put(QueryType.GENE_EXON_INTRON.getCode(), GeneExonIntronParser.class)
            .put(QueryType.GENE_FLANK.getCode(), GeneFlankParser.class)
            .put(QueryType.PEPTIDE.getCode(), PeptideParser.class)
            .put(QueryType.THREE_UTR.getCode(), ThreeUTRParser.class)
            .put(QueryType.TRANSCRIPT_EXON_INTRON.getCode(), TranscriptExonIntronParser.class)
            .put(QueryType.TRANSCRIPT_FLANK.getCode(), TranscriptFlankParser.class)
            .build();
    }

    // Parameters and config
    @FieldInfo(displayName="JDBC Connection URL")
    private StringField jdbcConnectionURL = new StringField();

    @FieldInfo(displayName="Database Username")
    private StringField username = new StringField();

    @FieldInfo(displayName="Database Password")
    private StringField password = new StringField();

    @FieldInfo(displayName="Query Type", required=true, clientDefined=true)
    public StringField type = new StringField(
            new LinkedHashMap<String,String>(){{
                put("transcript_exon_intron", "Unspliced (Transcript)");
                put("gene_exon_intron", "Unspliced (Gene)");
                put("transcript_flank", "Flank (Transcript)");
                put("gene_flank", "Flank (Gene)");
                put("coding_transcript_flank", "Flank-coding region (Transcript)");
                put("coding_gene_flank", "Flank-coding region (Gene)");
                put("5utr", "5 UTR");
                put("3utr", "3 UTR");
                put("gene_exon", "Exon Sequences");
                put("cdna", "cDNA Sequences");
                put("coding", "Coding Sequences");
                put("peptide", "Protein");
            }});

    @FieldInfo(displayName="Upstream Flank", clientDefined=true)
    public IntegerField upstreamFlank = new IntegerField();

    @FieldInfo(displayName="Downstream Flank", clientDefined=true)
    public IntegerField downstreamFlank = new IntegerField();

    /*
     * Add additional attribute list for sequence retrieval
     */
    @Override
    public void preprocess(final Document queryXML) {
        List<Element> datasets = queryXML.getRootElement().getChildren("Dataset");
        int userLimit;

        // Force headers to not return
        queryXML.getRootElement().setAttribute("header", "0");

        // set to not limit, but remember orignal limit for later
        userLimit = Integer.parseInt(queryXML.getRootElement().getAttributeValue("limit"));
        queryXML.getRootElement().setAttribute("limit", "-1");
        if (userLimit > 0) {
            limit = userLimit;
        }

        if (datasets.size() > 1) {
            throw new ValidationException("Sequence processor cannot take more than one dataset");
        }

        List<Element> header = new ArrayList<Element>();

        for (Element element : (List<Element>)datasets.get(0).getChildren("Attribute")) {
            header.add((Element)element.clone());
            headerAttributes++;
        }

        String[] datasetNames = datasets.get(0).getAttributeValue("name").split(",");

        if (datasetNames.length > 1) {
            throw new ValidationException("Sequence processor cannot take more than one dataset");
        }

        String speciesName = datasetNames[0].split("_")[0];

        databaseTableName = speciesName + "_genomic_sequence__dna_chunks__main";

        // Remove header temporarily so attribute list comes first
        datasets.get(0).removeChildren("Attribute");

        Element attribute = new Element("Attribute");
        attribute.setAttribute("name", type.value);
        datasets.get(0).addContent(attribute);

        // Add back header
        for (Element element : header) {
            datasets.get(0).addContent(element);
        }

        Log.debug("Adding attribute " + type.value);

        QueryType queryType = QueryType.get(this.type.value);

        // Check DB settings
        validateDatabaseSettings();

        try {
            // Setup database pool
//            pooledDataSource = new ComboPooledDataSource();
//            pooledDataSource.setDriverClass("com.mysql.jdbc.Driver"); //loads the jdbc driver
//            pooledDataSource.setJdbcUrl(jdbcConnectionURL.value);
//            pooledDataSource.setUser(username.value);
//            pooledDataSource.setPassword(password.value);

            // Setting parser
            if (lookup.containsKey(queryType.getCode())) {
                Class<? extends SequenceParser> clazz = lookup.get(queryType.getCode());
                parser = clazz.newInstance();

                if (downstreamFlank.value != null) {
                    parser.setDownstreamFlank(downstreamFlank.value);
                }

                if (upstreamFlank.value != null) {
                    parser.setUpstreamFlank(upstreamFlank.value);
                }

            } else {
                throw new ValidationException("Cannot find sequence type: " + this.type.value);
            }
        } catch (Exception e) {
            throw new BioMartException("Problem Instantiating sequence processor: " + this.type.value, e);
        }
    }

    @Override
	public void setOutputStream(OutputStream out) {
        this.out = out;
        // Start parser
        try {
            conn = (Connection)DriverManager.getConnection(jdbcConnectionURL.value, username.value, password.value);
//            conn = pooledDataSource.getConnection();

            parser
                .setOutputStream(out)
                .prepareStatements(conn, databaseTableName)
                .setExtraAttributes(headerAttributes)
                .validate()
                .startup();

        } catch (BioMartException e) {
            throw e;
        } catch (Exception e) {
            throw new BioMartException(e);
        }
    }

    @Override
    public Function getCallback() {
        return new SequenceCallback();
    }

    @Override
	public void done() {
        boolean hasWritten = false;

        // Write out last batch
        if (hasResults) {
            retrieveSequences();
            hasWritten = parser.done(isDone);
        }

        if (total == 0 && !hasWritten) {
            try {
                out.write(SEQUENCE_UNAVAILABLE_BYTES);
            } catch (IOException e) {
                Log.error("Problem writing to OutputStream", e);
            }
        }

        closeSilently(conn);
    }

    private void validateDatabaseSettings() {
        if (isNullOrEmpty(this.jdbcConnectionURL.value)) {
            this.jdbcConnectionURL.value = System.getProperty("processors.sequence.connection");
        }
        if (isNullOrEmpty(this.username.value)) {
            this.username.value = System.getProperty("processors.sequence.username");
        }
        if (isNullOrEmpty(this.password.value)) {
            this.password.value = System.getProperty("processors.sequence.password");
        }
    }

    // Helpers for closing connections
    protected static void closeSilently(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            Log.error("Error closing connection");
        }
    }
    protected static void closeSilently(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            Log.error("Error closing statement");
        }
    }
    protected static void closeSilently(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            Log.error("Error closing result set");
        }
    }
}

