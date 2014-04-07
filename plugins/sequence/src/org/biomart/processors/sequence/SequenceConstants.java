package org.biomart.processors.sequence;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jhsu
 */
public interface SequenceConstants {
    // Valid types of queries
	public enum QueryType {
		TRANSCRIPT_EXON_INTRON("transcript_exon_intron"),
		GENE_EXON_INTRON("gene_exon_intron"),
		TRANSCRIPT_FLANK("transcript_flank"),
		GENE_FLANK("gene_flank"),
		CODING_TRANSCRIPT_FLANK("coding_transcript_flank"),
		CODING_GENE_FLANK("coding_gene_flank"),
		FIVE_UTR("5utr"),
		THREE_UTR("3utr"),
		GENE_EXON("gene_exon"),
		CDNA("cdna"),
		CODING("coding"),
		PEPTIDE("peptide");

        private String code;

        private static final Map<String,QueryType> lookup = new HashMap<String,QueryType>();

        static {
            for(QueryType t: EnumSet.allOf(QueryType.class))
                lookup.put(t.getCode(), t);
        }

        private QueryType(String code) { this.code = code; }

        public String getCode() { return this.code; }

        public static QueryType get(String code) { return lookup.get(code); }
	}

    public static final String HEADER_COLUMN_DELIMITER = "|";
    public static final String HEADER_VALUE_DELIMITER = ";";

    public static final String SEQUENCE_UNAVAILABLE = "Sequence unavailable";
    public static final byte[] SEQUENCE_UNAVAILABLE_BYTES = SEQUENCE_UNAVAILABLE.getBytes();

    public static final String SEQUENCE_ERROR_ENCOUNTERED = "Error encountered\n";
}
