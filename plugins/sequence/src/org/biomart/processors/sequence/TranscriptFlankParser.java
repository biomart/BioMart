package org.biomart.processors.sequence;

import java.io.IOException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu, jguberman
 */
public class TranscriptFlankParser extends SequenceParser {
    // "Flank (Transcript)"

    // Set up the fields
    private static final int transcriptIDField = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static final int strandField = 4;

    // Initialize the variables for the first transcript ID
    private String transcriptID = null;
    private String chr = null;
    private String strand = null;
    private int start = 0;
    private int end = 0;

    public TranscriptFlankParser() {
        super(5);
    }

    public TranscriptFlankParser(int i) {
        super(i);
    }

    @Override
    public SequenceParser validate() throws ValidationException {
		// Make sure the flank region request makes sense
		checkFlank();
        return this;
    }

	/**
	 * Parses input rows for QueryType TRANSCRIPT_FLANK.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, ordered by transcript_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws IOException
	 */
	@Override
    public String parseLine(String[] line) {
        String results = "";

        // Check if the current row belongs to the same transcript
        if (line[transcriptIDField].equals(transcriptID)) {
            // If it does, adjust start and end as needed
            start = Math.min(start, Integer.parseInt(line[startField]));
            end = Math.max(end, Integer.parseInt(line[endField]));
        } else {
            if (transcriptID != null) {
                // If it isn't, we print the last sequence and initialize the next
                results = getTranscriptFlank(getHeader(), chr, start, end, strand);
            }

            transcriptID = line[transcriptIDField];
            chr = line[chrField];
            start = Integer.parseInt(line[startField]);
            end = Integer.parseInt(line[endField]);
            strand = line[strandField];
            clearHeader();

        }
        storeHeaderInfo(line);

        return results;
	}

    @Override
    public String parseLast() {
        return getTranscriptFlank(getHeader(), chr, start, end, strand);
    }

	/**
	 * Retrieves and prints sequence for QueryType TRANSCRIPT_FLANK
	 * @param flank	Array containing the upstream/downstream flank information.
	 * @param header Header for the sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param strand	Sequence strand.
	 * @throws IOException
	 */
	protected final String getTranscriptFlank(String header, String chr,
			int start, int end, String strand) {
        String sequence;
        // Check whether we're dealing with the 5' flank or the 3', and handle accordingly
        if (upstreamFlank > 0){
            if (strand.equals("-1")){
                sequence = getReverseComplement(getSequence(chr, end+1, end+upstreamFlank));
            } else {
                sequence = (getSequence(chr, Math.max(start-upstreamFlank,0), start-1));
            }
        } else {
            if (strand.equals("-1")){
                sequence = getReverseComplement(getSequence(chr, Math.max(start-downstreamFlank,0), start-1));
            } else {
                sequence = (getSequence(chr, end+1, end+downstreamFlank));
            }
        }
        return getFASTA(sequence, header);
	}
}
