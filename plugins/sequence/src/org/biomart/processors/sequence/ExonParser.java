package org.biomart.processors.sequence;

import java.io.IOException;

/**
 *
 * @author jhsu, jguberman
 */
public class ExonParser extends TranscriptExonIntronParser {
    // "Unspliced (Transcript)"
    // Doesn't yet handle flank

    // Set up the fields
    private static final int exonIDField = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static final int strandField = 4;

    // Read the first line of the input and initialize the variables
    private String exonID = null;
    private String chr = null;
    private String strand = null;
    private int start = 0;
    private int end = 0;

    public ExonParser() {
        super(5);
    }

	/**
	 * Parses input rows for QueryType TRANSCRIPT_EXON_INTRON.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, ordered by transcript_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws IOException
	 */
	@Override
    public String parseLine(String[] line) {
        String results = "";
        // Check if the current row belongs to the same transcript
        if (line[exonIDField].equals(exonID)) {
            // If it does, adjust the start and end positions if needed
            start = Math.min(start, Integer.parseInt(line[startField]));
            end = Math.max(end, Integer.parseInt(line[endField]));
            // Update header as necessary
        } else {
            if (exonID != null) {
                results = getTranscriptExonIntron(getHeader(), chr, start, end, strand);
            }

            // Initialize for the next transcript ID, and re-enter the loop
            exonID = line[exonIDField];
            chr = line[chrField];
            start = Integer.parseInt(line[startField]);
            end = Integer.parseInt(line[endField]);
            strand = line[strandField];
            // Re-initialize header
            clearHeader();
        }

        storeHeaderInfo(line);

        return results;
	}

    @Override
    public String parseLast() {
        return getTranscriptExonIntron(getHeader(), chr, start, end, strand);
    }
}
