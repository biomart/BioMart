package org.biomart.processors.sequence;

import java.io.IOException;

/**
 *
 * @author jhsu, jguberman
 */
public class GeneExonIntronParser extends TranscriptExonIntronParser {
    // "Unspliced (Gene)"
    /*	This code theoretically could be exactly the same as parseTranscriptExonIntron,
     *  with the field numbering changed to accommodate the geneID. However,
     *  parseTranscriptExonIntron relies on the ordering of the input by transcriptID.
     *  Since the output of martservice can't be reordered by geneID, the more complicated
     *  parsing is necessary. On the other hand, this method is much more robust, as it doesn't
     *  depend on the input ordering.
     *
     *  Note that this implementation STILL relies on the ordering by transcriptID, but it does
     *  demonstrate how a generalized parser might be made to deal with unordered input in any field.
     */

    // Set up the fields
    private static final int geneIDfield = 0;
    private static final int transcriptIDField = 1;
    private static final int chrField = 2;
    private static final int startField = 3;
    private static final int endField = 4;
    private static final int strandField = 5;

    private String chr = null;
    private Integer start = null;
    private Integer end = null;
    private String strand = null;

    private String currGeneID = null;

    public GeneExonIntronParser() {
        super(6);
    }

	/**
	 * Parses input rows for QueryType GENE_EXON_INTRON.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_gene_id, ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, transcript_count.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws IOException
	 */
	@Override
    public String parseLine(String[] line) {
        String results = "";
        String geneID = line[geneIDfield];

        if (!geneID.equals(currGeneID)) {
            if (currGeneID != null) {
                results = getTranscriptExonIntron(getHeader(), chr, start, end, strand);
            }

            chr = line[chrField];
			start = Integer.MAX_VALUE;
			end = Integer.MIN_VALUE;
            strand = line[strandField];

            clearHeader();

            currGeneID = geneID;
		}

        storeHeaderInfo(line);
        start = Math.min(start, Integer.parseInt(line[startField]));
        end = Math.max(end, Integer.parseInt(line[endField]));

        return results;
	}

    @Override
    public String parseLast() {
        return getTranscriptExonIntron(getHeader(), chr, start, end, strand);
    }
}
