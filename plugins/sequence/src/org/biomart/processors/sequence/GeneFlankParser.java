package org.biomart.processors.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author jhsu, jguberman
 */
public class GeneFlankParser extends TranscriptFlankParser {
    // "Unspliced (Gene)"
    /* Exactly the same as parseGeneIntronExon, except it calls
     * printTranscriptFlank instead of printTranscriptExonIntron, and it checks
     * for reasonable flank input.
     */
    // TODO Optimize by using the transcript_count info when initializing lists? (Except transcript_count seems to be empty)

    // Set up the fields
    private static final int transcriptIDField = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static final int strandField = 4;
    private static final int geneIDfield = 5;

    private String chr = null;
    private Integer start = null;
    private Integer end = null;
    private String strand = null;

    private String currGeneID = null;

    public GeneFlankParser() {
        super(6);
    }

	/**
	 * Parses input rows for QueryType GENE_FLANK.
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
                results = getTranscriptFlank(getHeader(), chr, start, end, strand);
            }

			chr = line[chrField];
			start = Integer.parseInt(line[startField]);
			end = Integer.parseInt(line[endField]);
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
        String results = getTranscriptFlank(getHeader(), chr, start, end, strand);
        return results;
    }
}
