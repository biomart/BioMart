package org.biomart.processors.sequence;

import java.io.IOException;
import org.biomart.common.exceptions.ValidationException;

/**
 *
 * @author jhsu, jguberman
 */
public class TranscriptExonIntronParser extends SequenceParser {
    // "Unspliced (Transcript)"
    // Doesn't yet handle flank

    // Set up the fields
    private static final int transcriptIDfield = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static final int strandField = 4;

    // Read the first line of the input and initialize the variables
    private String transcriptID = null;
    private String chr = null;
    private String strand = null;
    private int start = 0;
    private int end = 0;

    public TranscriptExonIntronParser() {
        super(5);
    }

    public TranscriptExonIntronParser(int i) {
        super(i);
    }
    
    @Override
    public SequenceParser validate() throws ValidationException {
        return this;
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
        if (line[transcriptIDfield].equals(transcriptID)) {
            // If it does, adjust the start and end positions if needed
            start = Math.min(start, Integer.parseInt(line[startField]));
            end = Math.max(end, Integer.parseInt(line[endField]));
            // Update header as necessary
        } else {
            if (transcriptID != null) {
                results = getTranscriptExonIntron(getHeader(), chr, start, end, strand);
            }

            // Initialize for the next transcript ID, and re-enter the loop
            transcriptID = line[transcriptIDfield];
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

	/**
	 * Retrieves and prints sequence for QueryType TRANSCRIPT_EXON_INTRON
	 * @param header	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param strand	Sequence strand.
	 * @throws IOException
	 */
	protected final String getTranscriptExonIntron(String header,
			String chr, int start, int end, String strand) {
        String sequence;
        // Take the reverse complement if necessary
        if ("-1".equals(strand)){
            sequence = getReverseComplement(getSequence(chr, start-downstreamFlank, end+upstreamFlank));
        } else {
            sequence = getSequence(chr, start-upstreamFlank, end+downstreamFlank);
        }
        return getFASTA(sequence, header);
	}
}
