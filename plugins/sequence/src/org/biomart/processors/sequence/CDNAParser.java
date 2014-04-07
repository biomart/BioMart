package org.biomart.processors.sequence;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.biomart.common.exceptions.ValidationException;

/**
 *
 * @author jhsu, jguberman
 */
public class CDNAParser extends SequenceParser {
    private static final int transcriptIDfield = 0;
    private static final int chrField = 1;
    private static final int startField = 2;
    private static final int endField = 3;
    private static final int strandField = 4;
    private static final int rankField = 5;

    // These TreeMaps will keep track of the exons
    private Map<Integer,Integer> start = new TreeMap<Integer,Integer>();
    private Map<Integer,Integer> end = new TreeMap<Integer,Integer>();

    private String transcriptID = null;
    private String chr = null;
    private String strand = null;

    public CDNAParser() {
        super(6);
    }

    @Override
    public SequenceParser validate() throws ValidationException {
        return this;
    }

	/**
	 * Parses input rows for QueryType CDNA.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, rank.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws IOException
	 */
    @Override
	public String parseLine(String[] line) {
        int currentRank = Integer.parseInt(line[rankField])-1; // Subtract 1 to convert to zero indexing
        String results = "";

        // If new transcript ID or last line
        if (!line[transcriptIDfield].equals(transcriptID)) {
            if (transcriptID != null) {
                results = getCDNA(getHeader(), chr, start, end, strand);
                start.clear();
                end.clear();
            }

            chr = line[chrField];
            transcriptID = line[transcriptIDfield];
            strand = line[strandField];

            clearHeader();
        }

        start.put(currentRank, Integer.parseInt(line[startField]));
        end.put(currentRank, Integer.parseInt(line[endField]));

        storeHeaderInfo(line);

        return results;
	}

    @Override
    public String parseLast() {
        return getCDNA(getHeader(), chr, start, end, strand);
    }


	/**
	 * Retrieves and prints sequence for QueryType CDNA.
	 * @param transcriptID	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param strand	Sequence strand.
	 * @throws IOException
	 */
	protected String getCDNA(String header, String chr,
			Map<Integer,Integer> start, Map<Integer,Integer> end, String strand) {
        StringBuilder sequence = new StringBuilder();
        if (!chr.equals("")) {
            if (strand.equals("-1")){
                sequence.append(getReverseComplement(getSequence(chr,end.get(0)+1,end.get(0)+upstreamFlank)));
                for (int i = 0; i < start.size(); i++){
                    sequence.append(getReverseComplement(getSequence(chr, start.get(i), end.get(i))));
                }
                sequence.append(getReverseComplement(getSequence(chr,start.get(start.size()-1)-downstreamFlank,start.get(start.size()-1)-1)));
            } else {
                sequence.append(getSequence(chr,start.get(0)-upstreamFlank,start.get(0)-1));
                for (int i = 0; i < start.size(); i++){
                    sequence.append((getSequence(chr, start.get(i), end.get(i))));
                }
                sequence.append(getSequence(chr,end.get(end.size()-1)+1,end.get(end.size()-1)+downstreamFlank));
            }
        }
        return getFASTA(sequence.toString(), header);
	}
}
