package org.biomart.processors.sequence;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu, jguberman
 */
public class CodingParser extends SequenceParser {
    private static final int transcriptIDField = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static final int codingOffsetStartField = 4;
    private static final int codingOffsetEndField = 5;
    private static final int strandField = 6;
    private static final int exonIDField = 7;
    private static final int rankField = 8;
    private static final int startExonIDField = 9;
    private static final int endExonIDField = 10;
    private static final int phaseField = 11;
    private static final int codonTableField = 12;
    private static final int seqEditField = 13;

    private final boolean isProtein;

    // These TreeMaps will keep track of the exons
    private TreeMap<Integer, Integer> start = new TreeMap<Integer, Integer>();
    private TreeMap<Integer, Integer> end = new TreeMap<Integer, Integer>();

    private String transcriptID = null;
    private String startExonID = null;
    private String endExonID = null;
    private int startExonRank = 0;
    private int endExonRank = 0;
    private String exonID = null;
    private String chr = null;
    private int codingStartOffset = 0;
    private int codingEndOffset = 0;
    private int startPhase = 0;
    private int currentRank = 0;
    private String strand = null;
    private String codonTableID = null;

    private HashSet<String> seqEdit = new HashSet<String>();

    public CodingParser() { this(false, 12); }
    public CodingParser(boolean isProtein, int i) {
        super(i);
        this.isProtein = isProtein;
    }

    @Override
    public SequenceParser validate() throws ValidationException {
		if (isProtein && (upstreamFlank > 0  || downstreamFlank > 0)){
			throw new ValidationException("Validation Error: Protein sequences cannot have flanking regions.");
		}
        return this;
    }

	/**
	 * Parses input rows for QueryType CODING.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id, phase.
	 * @param isProtein If TRUE, translates the resulting sequence to a protein sequence.
	 * @throws IOException
	 */
	@Override
    public String parseLine(String[] line) {
        String results = "";

        exonID = line[exonIDField];

        if (!line[transcriptIDField].equals(transcriptID)) {
            //If it's a new transcript, print the current sequence
            if (transcriptID != null) {
                results = getCoding(getHeader(), chr, start, end, codingStartOffset, codingEndOffset, startExonRank, endExonRank, strand, startPhase, codonTableID, seqEdit,isProtein);
            }

            transcriptID = line[transcriptIDField];
            startExonID = line[startExonIDField];
            endExonID = line[endExonIDField];
            startExonRank = 0;
            endExonRank = 0;
            startPhase = 0;
            chr = "";
            start.clear();
            end.clear();
            strand = line[strandField];
            if(isProtein){
                codonTableID = line[codonTableField];
                seqEdit = new HashSet<String>();
                seqEdit.add(line[seqEditField]);
            }
            clearHeader();
        }
        currentRank = Integer.parseInt(line[rankField])-1; // Subtract 1 to convert to zero indexing
        if(isProtein){
            seqEdit.add(line[seqEditField]);
        }
        if (!startExonID.equals("")) {
            start.put(currentRank, Integer.parseInt(line
                    [startField]));
            end.put(currentRank, Integer.parseInt(line
                    [endField]));
        }
        if (exonID.equals(startExonID)){
            // If it's the terminal exon, record the chromosome and codingOffset and Phase
            chr = line[chrField];
            codingStartOffset = Integer.parseInt(line[codingOffsetStartField]);
            startExonRank = currentRank;
            startPhase = Integer.parseInt(line[phaseField]);
        }
        if (exonID.equals(endExonID)){
            // If it's the terminal exon, record the chromosome and codingOffset
            chr = line[chrField];
            codingEndOffset = Integer.parseInt(line[codingOffsetEndField]);
            endExonRank = currentRank;
        }

        storeHeaderInfo(line);

        return results;
	}

    @Override
    public String parseLast() {
        return getCoding(getHeader(), chr, start, end, codingStartOffset, codingEndOffset, startExonRank, endExonRank, strand, startPhase, codonTableID, seqEdit,isProtein);
    }

	/**
	 * Retrieves and prints sequence for QueryType CODING
	 * @param header Header for the sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param codingStartOffset Coding start offset.
	 * @param codingEndOffset	Coding end offset.
	 * @param startExonRank	Rank of start exon.
	 * @param endExonRank	Rank of end exon.
	 * @param strand	Sequence strand.
	 * @param startPhase
	 * @param isProtein	If TRUE, translate sequence to amino acid sequence.
	 */
	protected final String getCoding(String header, String chr,
			TreeMap<Integer, Integer> start, TreeMap<Integer, Integer> end, int codingStartOffset,
			int codingEndOffset, int startExonRank, int endExonRank, String strand, int startPhase,
            String codonTableID ,HashSet<String> seqEdit,boolean isProtein) {
        try {
            StringBuilder sequence = new StringBuilder();
            if (!chr.equals("")){
                if (strand.equals("-1")){
                    if (startExonRank == endExonRank){
                        sequence.append(getReverseComplement(getSequence(chr,end.get(startExonRank)-codingEndOffset+1-downstreamFlank,end.get(startExonRank)-codingStartOffset+1+upstreamFlank)));
                    } else {
                        sequence.append(getReverseComplement(getSequence(chr,start.get(startExonRank),end.get(startExonRank)-codingStartOffset+1+upstreamFlank)));
                        for (int i = startExonRank+1; i < endExonRank; i++){
                            sequence.append(getReverseComplement(getSequence(chr, start.get(i), end.get(i))));
                        }
                        sequence.append(getReverseComplement(getSequence(chr,end.get(endExonRank)-codingEndOffset+1-downstreamFlank,end.get(endExonRank))));
                    }
                } else {
                    if (startExonRank == endExonRank){
                        sequence.append(getSequence(chr,start.get(startExonRank)+codingStartOffset-1-upstreamFlank,start.get(startExonRank)+codingEndOffset-1+downstreamFlank));
                    } else {
                        sequence.append(getSequence(chr,start.get(startExonRank)+codingStartOffset-1-upstreamFlank,end.get(startExonRank)));
                        for (int i = startExonRank+1; i < endExonRank; i++){
                            sequence.append((getSequence(chr, start.get(i), end.get(i))));
                        }
                        sequence.append(getSequence(chr,start.get(endExonRank),start.get(endExonRank)+codingEndOffset-1+downstreamFlank));
                    }
                }
            }
            if (sequence.length()>0){
                for(int i = startPhase; i > 0; --i){
                    sequence.insert(0, 'N');
                }
                if(startPhase > 0 && upstreamFlank > 0){
                	for (int i = startPhase; i < startPhase + upstreamFlank; ++i ){
                		sequence.replace(i, i+1, "N");
                	}
                }
            }
            if(isProtein){
                return getFASTA(SequenceTranslator.translateSequence(sequence.toString(), seqEdit, codonTableID), header);
            } else {
                return getFASTA(sequence.toString(), header);
            }
        } catch (Exception e) {
            Log.debug(e);
            return getFASTA(SEQUENCE_ERROR_ENCOUNTERED, header);
        }
	}
}
