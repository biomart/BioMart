package org.biomart.processors.sequence;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu, jguberman
 */
public class ThreeUTRParser extends SequenceParser {
    // "3' UTR"
    private static final int transcriptIDField = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static final int codingOffsetField = 5;
    private static final int strandField = 6;
    private static final int exonIDField = 7;
    private static final int rankField = 8;
    private static final int terminalExonField = 10;

    // These TreeMaps will keep track of the exons after the end_exon
    private TreeMap<Integer, Integer> start = new TreeMap<Integer, Integer>();
    private TreeMap<Integer, Integer> end = new TreeMap<Integer, Integer>();

    private String transcriptID = null;
    private String terminalExonID = null;
    private int terminalExonRank = 0;
    private String exonID = null;
    private String chr = "";
    private int codingOffset = 0;
    private int currentRank = 0;
    private String strand = null;

    public ThreeUTRParser() {
        super(11);
    }

    @Override
    public SequenceParser validate() throws ValidationException {
        return this;
    }

	/**
	 * Parses input rows for QueryType THREE_UTR.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws IOException
	 */
    @Override
	public String parseLine(String[] line) {
        String results = "";

        exonID = line[exonIDField];

        if (!line[transcriptIDField].equals(transcriptID)){
            //If it's a new transcript, print the current sequence
            if (transcriptID != null) {
                results = get3UTR(getHeader(), chr, start, end, codingOffset, terminalExonRank, strand);
            }
            transcriptID = line[transcriptIDField];
            terminalExonID = line[terminalExonField];
            terminalExonRank = 0;
            chr = "";
            start.clear();
            end.clear();
            strand = line[strandField];
            clearHeader();
        }

        currentRank = Integer.parseInt(line[rankField])-1; // Subtract 1 to convert to zero indexing
        start.put(currentRank, Integer.parseInt(line[startField]));
        end.put(currentRank, Integer.parseInt(line[endField]));

        if (exonID.equals(terminalExonID)){
            // If it's the terminal exon, record the chromosome and codingOffset
            chr = line[chrField];
            codingOffset = Integer.parseInt(line[codingOffsetField]);
            terminalExonRank = currentRank;
        }

        storeHeaderInfo(line);

        return results;
	}

    @Override
    public String parseLast() {
        return get3UTR(getHeader(), chr, start, end, codingOffset, terminalExonRank, strand);
    }

	/**
	 * Retrieves and prints sequence for QueryType TRANSCRIPT_EXON_INTRON
	 * @param header	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param terminalExonRank	Rank of the terminal exon.
	 * @param strand	Sequence strand.
	 * @param flank How much upstream/downstream flank to add
	 * @throws IOException
	 */
	protected final String get3UTR(String header, String chr,
			TreeMap<Integer,Integer> start, TreeMap<Integer,Integer> end, int codingOffset,
			int terminalExonRank, String strand) {
        StringBuilder sequence = new StringBuilder();

        if (!chr.equals("")) {
            if (strand.equals("-1")){
            	for (int i = terminalExonRank+1; i < start.size(); i++){
                    sequence.append(getReverseComplement(getSequence(chr, start.get(i), end.get(i))));
                }
            	
                
                if((getSequence(chr,start.get(terminalExonRank),end.get(terminalExonRank)-codingOffset)).equals("")){
                	if(terminalExonRank + 1 < start.size())
                    	sequence.insert(0,getReverseComplement(getSequence(chr,end.get(terminalExonRank+1)+1,end.get(terminalExonRank+1)+upstreamFlank)));
                } else {
                	sequence.insert(0, getReverseComplement(getSequence(chr,start.get(terminalExonRank),end.get(terminalExonRank)-codingOffset+upstreamFlank)));
                }
                if(sequence.length()>0)
                    sequence.append(getReverseComplement(getSequence(chr, start.get(start.size()-1)-downstreamFlank,start.get(start.size()-1)-1)));
            } else {
                for (int i = terminalExonRank+1; i < start.size(); i++){
                    sequence.append((getSequence(chr, start.get(i), end.get(i))));
                }
                if((getSequence(chr,start.get(terminalExonRank)+codingOffset,end.get(terminalExonRank))).equals("")){
                	if(terminalExonRank + 1 < start.size())
                		sequence.insert(0,getSequence(chr,start.get(terminalExonRank+1)-upstreamFlank,start.get(terminalExonRank+1)-1));
                } else {
                	sequence.insert(0,getSequence(chr,start.get(terminalExonRank)+codingOffset-upstreamFlank,end.get(terminalExonRank)));
                }
                if(sequence.length()>0)
                    sequence.append((getSequence(chr, end.get(start.size()-1)+1,end.get(start.size()-1)+downstreamFlank)));
                
            }
        }
        return getFASTA(sequence.toString(), header);
	}
}
