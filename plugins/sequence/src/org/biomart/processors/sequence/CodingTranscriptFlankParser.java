package org.biomart.processors.sequence;

import java.io.IOException;
import java.util.List;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu, jguberman
 */
public class CodingTranscriptFlankParser extends SequenceParser {
    // "Flank-coding region (Transcript)"
    // The coding_start_offset and coding_end_offset are both given as a distance from
    // the exon_chrom_start of the exon that has exon_id equal to start_exon_id or end_exon_id, respectively
    private static final int transcriptIDField = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static int codingOffsetField; // set depending on flank
    private static final int strandField = 6;
    private static final int exonIDField = 7;

    private int terminalExonField;

    private String transcriptID = null;
    private String chr = null;
    private String strand = null;
    private int start = 0;
    private int end = 0;
    private String terminalExonID = null;
    private String exonID = null;
    private int codingOffset = 0;

    public CodingTranscriptFlankParser() {
        super(11);
    }

    @Override
    public SequenceParser validate() throws ValidationException {
		checkFlank();
        if (upstreamFlank >0 ){
            terminalExonField = 9;
            codingOffsetField = 4;
        } else {
            terminalExonField = 10;
            codingOffsetField = 5;
        }
        return this;
    }

	/**
	 * Parses input rows for QueryType CODING_TRANSCRIPT_FLANK.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws IOException
	 */
	@Override
    public String parseLine(String[] line) {
        String results = "";

        exonID = line[exonIDField];
        if (!line[transcriptIDField].equals(transcriptID)) {
            if (transcriptID != null) {
                results = getCodingTranscriptFlank(getHeader(), chr, start, end, codingOffset, strand);
            }
            transcriptID = line[transcriptIDField];
            terminalExonID = line[terminalExonField];
            chr = "";
            start = 0;
            end = 0;
            strand = line[strandField];
            clearHeader();
        }
        if (exonID.equals(terminalExonID)){
            chr = line[chrField];
            start = Integer.parseInt(line[startField]);
            end = Integer.parseInt(line[endField]);
            codingOffset = Integer.parseInt(line[codingOffsetField]);
        }
        storeHeaderInfo(line);

        return results;
	}

    @Override
    public String parseLast() {
        return getCodingTranscriptFlank(getHeader(), chr, start, end, codingOffset, strand);
    }

	/**
	 * Retrieves and prints sequence for QueryType CODING_TRANSCRIPT_FLANK
	 * @param flank	Array containing upstream/downstream flank distance
	 * @param header	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param codingOffset Coding offset.
	 * @param strand Sequence strand.
	 * @throws IOException
	 */
	protected final String getCodingTranscriptFlank(String header,
            String chr, int start, int end, int codingOffset,String strand) {
        try {
            String sequence;
            // Check whether we're dealing with the 5' flank or the 3', and handle accordingly
            if (chr.equals("")){
                sequence = null;
            } else if (upstreamFlank>0){
                if (strand.equals("-1")){
                    sequence = getReverseComplement(getSequence(chr, end+2-codingOffset, end+upstreamFlank-codingOffset+1));
                } else {
                    sequence = (getSequence(chr, start-upstreamFlank+codingOffset-1, start-2+codingOffset));
                }
            } else {
                if (strand.equals("-1")){
                    sequence = getReverseComplement(getSequence(chr, end+1-codingOffset-downstreamFlank, end-codingOffset));
                } else {
                    sequence = (getSequence(chr, start+codingOffset, start-1+codingOffset+downstreamFlank));
                }
            }
            return getFASTA(sequence, header);
        } catch (Exception e) {
            Log.debug(e);
            return getFASTA(SEQUENCE_ERROR_ENCOUNTERED, header);
        }
	}

}
