package org.biomart.processors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.resources.Log;
import org.biomart.common.utils2.MyUtils;

/** 
 * @author Jonathan Guberman
 *
 * Class for retrieving various types of sequence (Transcript, gene,
 * flank regions, UTR, etc), given the appropriate rows of data
 */
public class Fasta extends Processor {
    /**
     *
     */
    public Fasta() {
		super (null,null);
	}

	/** Size of each block of sequence in the database, currently {@value}
	 */
	static final int CHUNK_SIZE = 100000;

	/** Valid types of queries
	 */
	enum QueryType{
		TRANSCRIPT_EXON_INTRON,
		GENE_EXON_INTRON,
		TRANSCRIPT_FLANK,
		GENE_FLANK,
		CODING_TRANSCRIPT_FLANK,
		CODING_GENE_FLANK,
		FIVE_UTR,
		THREE_UTR,
		GENE_EXON,
		CDNA,
		CODING,
		PEPTIDE;
	}

	/**
	 *  Array for mapping command-line integer arguments to query types,
	 *  for debugging
	 */
	static final QueryType[] queryTypeMap = {
		QueryType.TRANSCRIPT_EXON_INTRON, //0
		QueryType.GENE_EXON_INTRON, //1
		QueryType.TRANSCRIPT_FLANK, //2
		QueryType.GENE_FLANK, //3
		QueryType.CODING_TRANSCRIPT_FLANK, //4
		QueryType.CODING_GENE_FLANK, //5
		QueryType.FIVE_UTR, //6
		QueryType.THREE_UTR, //7
		QueryType.GENE_EXON, //8
		QueryType.CDNA, //9
		QueryType.CODING, //10
		QueryType.PEPTIDE} // 11
	;

	/**
	 *  The database connection for this invocation of the processor.
	 */
	private static Connection databaseConnection = null;

	/**
	 * Disconnects from the database at the end of execution.
	 */
	static void disconnectDB(){
		try {
			databaseConnection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Connects to the database at the beginning of the query.
	 * Currently the database parameters are hard-coded.
	 */
	static void connectDB(){
		// Login information for the sequence database
		// URL format: jdbc:mysql://server[:port]/[databasename]

		String URL = "jdbc:mysql://bm-test.res.oicr.on.ca/jg_sequence_mart_56";
		String username = "martadmin";
		String password = "biomart";

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			System.err.println("Failed to load JDBC/ODBC driver.");
		}

		try {
			databaseConnection = DriverManager.getConnection (URL,username,password);
		} catch (Exception e) {
			System.err.println("problems connecting to "+URL);
		}
	}

	/**
	 * Returns the reverse complement of a DNA or RNA sequence.
	 * @param sequence The input sequence as a String.
	 * @return	The reverse complement sequence as a String.
	 */
	static String reverseComplement(String sequence){
		// I'm sure there are better and faster ways to do this, but this will work for now
		if(sequence==null){
			return null;
		}
		StringBuilder reversed = new StringBuilder(sequence.length());
		for (int i = sequence.length()-1;i >= 0;--i){
			switch (sequence.charAt(i)) {
			case 'A': reversed.append('T'); break;
			case 'T': reversed.append('A'); break;
			case 'C': reversed.append('G'); break;
			case 'G': reversed.append('C'); break;
			case 'U': reversed.append('A'); break;
			case 'R': reversed.append('Y'); break;
			case 'Y': reversed.append('R'); break;
			case 'K': reversed.append('M'); break;
			case 'M': reversed.append('K'); break;
			case 'S': reversed.append('S'); break;
			case 'W': reversed.append('W'); break;
			case 'B': reversed.append('V'); break;
			case 'V': reversed.append('B'); break;
			case 'D': reversed.append('H'); break;
			case 'H': reversed.append('D'); break;
			/*			case 'N': reversed.append('N'); break;
			case 'X': reversed.append('X'); break;
			case '-': reversed.append('-'); break;*/
			default: reversed.append(sequence.charAt(i)); break;
			}
		}
		return reversed.toString();
	}

	/**
	 * Given a sequence, prints FASTA formatted output
	 * 
	 * @param sequence The sequence to be printed.
	 * @param header The header to be printed above the sequence (optional).
	 * @param lineLength The length of each sequence line (optional; default 60).
	 * @param isProtein If true, translate the DNA sequence to protein sequence (optional, default false).
	 */
	static void printFASTA(String sequence, String header, int lineLength){

		if (sequence == null || sequence.equals("") || sequence.matches("null")){
			sequence = "Sequence unavailable";
		}
		System.out.println(">" + header);

		int sequenceLength = sequence.length();
		for(int i = 0; i < sequenceLength; i+=lineLength){
			System.out.println(sequence.substring(i,Math.min(i+lineLength,sequenceLength)));
		}
	}

	/**
	 * Translates a DNA sequence to a protein sequence. Currently uses only
	 * the human translation table.
	 * @param untranslated	Sequence to be translated.
	 * @return Translated sequence.
	 */
	private static String translateSequence(String untranslated, HashSet<String> seqEdit, String codonTableID) {
		untranslated = untranslated.toUpperCase();
		final String[] aaTables = 
		{"FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 0: Standard
				"FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSS**VVVVAAAADDEEGGGG", // 1: Vertebrate mitochondrial
				"FFLLSSSSYY**CCWWTTTTPPPPHHQQRRRRIIMMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 2: Yeast Mitochondrial
				"FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 3: Mold, Protozoan, and CoelenterateMitochondrial and Mycoplasma/Spiroplasma
				"FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSSSVVVVAAAADDEEGGGG", //4: Invertebrate Mitochondrial
				"FFLLSSSSYYQQCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", //5: Ciliate, Dasycladacean and Hexamita Nuclear
				"", "",
				"FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG", // 8: Echinoderm Mitochondrial
				"FFLLSSSSYY**CCCWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 9: Euplotid Nuclear
				"FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 10: "Bacterial"
				"FFLLSSSSYY**CC*WLLLSPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 11: Alternative Yeast Nuclear
				"FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSGGVVVVAAAADDEEGGGG", // 12: Ascidian Mitochondrial
				"FFLLSSSSYYY*CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG", // 13: Flatworm Mitochondrial
				"FFLLSSSSYY*QCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 14: Blepharisma Nuclear
				"FFLLSSSSYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 15: Chlorophycean Mitochondrial
				"", "", "", "",
				"FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNNKSSSSVVVVAAAADDEEGGGG", // 20: Trematode Mitochondrial
				"FFLLSS*SYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 21: Scenedesmus obliquus Mitochondrial
				"FF*LSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG", // 22: Thraustochytrium Mitochondrial
		};
		if(codonTableID.equals("")){
			codonTableID = "1";
		}
		String aaTable = aaTables[Integer.parseInt(codonTableID)-1];

		// The codon_table_id field doesn't appear to be used, so I'm just directly checking the chromosome name as a stopgap solution
		/*if (chr.equals("MT")){
			aaTable = aaTables[1];
		} else {
			aaTable = aaTables[0];
		}*/
		final String nucleotides[] = {"T","C","A","G"};
		HashMap<String, Character> codonTable = new HashMap<String, Character>();		
		int i = 0;
		for(String x : nucleotides){
			for(String y : nucleotides){
				for(String z : nucleotides){
					codonTable.put(x+y+z, aaTable.charAt(i));
					++i;
				}
			}
		}
		HashMap<Character, String[]> nucleotideTable = new HashMap<Character,String[]>();
		nucleotideTable.put('A', new String[] {"A"}); 
		nucleotideTable.put('C', new String[] {"C"});
		nucleotideTable.put('G', new String[] {"G"});
		nucleotideTable.put('T', new String[] {"T"});
		nucleotideTable.put('U', new String[] {"U"});
		nucleotideTable.put('M', new String[] {"A", "C"});
		nucleotideTable.put('R', new String[] {"A", "G"});
		nucleotideTable.put('W', new String[] {"A", "T"});
		nucleotideTable.put('S', new String[] {"C", "G"});
		nucleotideTable.put('Y', new String[] {"C", "T"});
		nucleotideTable.put('K', new String[] {"G", "T"});
		nucleotideTable.put('V', new String[] {"A", "C", "G"});
		nucleotideTable.put('H', new String[] {"A", "C", "T"});
		nucleotideTable.put('D', new String[] {"A", "G", "T"});
		nucleotideTable.put('B', new String[] {"C", "G", "T"});
		nucleotideTable.put('X', new String[] {"G", "A", "T", "C"});
		nucleotideTable.put('N', new String[] {"G", "A", "T", "C"});


		StringBuilder translated = new StringBuilder();
		Character nextAA = null;
		for(int j = 0;j+3 <= untranslated.length();j+=3){
			String currentCodon = untranslated.substring(j, j+3);
			nextAA = codonTable.get(currentCodon);
			if(nextAA==null){
				// Ambiguous codon
				nextAA='X';
				HashSet<Character> ambiguous = new HashSet<Character>();
				for(String x : nucleotideTable.get(currentCodon.charAt(0))){
					for(String y : nucleotideTable.get(currentCodon.charAt(1))){
						for(String z : nucleotideTable.get(currentCodon.charAt(2))){
							ambiguous.add(codonTable.get(x+y+z));
						}
					}
				}
				if(ambiguous.size()==1){
					for(char dummy : ambiguous){
						nextAA = dummy;
					}
				} else if (ambiguous.size()==2){
					if(ambiguous.contains('D') && ambiguous.contains('N')){
						nextAA = 'B';
					} else if(ambiguous.contains('E') && ambiguous.contains('Q')){
						nextAA= 'Z';
					} 
				}
			}
			translated.append(nextAA);
		}
		if (untranslated.length() % 3 == 2){
			String overhang = untranslated.substring(untranslated.length()-2,untranslated.length());
			HashSet<Character> ambiguous = new HashSet<Character>();
			for(String x : nucleotides){
				ambiguous.add(codonTable.get(overhang + x));
			}
			if(ambiguous.size()==1){
				for(char dummy : ambiguous){
					translated.append(dummy);
				}
			} else if (ambiguous.size()==2){
				if(ambiguous.contains('D') && ambiguous.contains('N')){
					translated.append("B");
				} else if(ambiguous.contains('E') && ambiguous.contains('Q')){
					translated.append("Z");
				}
			}
		}
		for(String curSeqEdit : seqEdit){
			if(!(curSeqEdit.equals(""))){
				List<String> splitSeqEdit = JGUtils.splitLine(" ", curSeqEdit);
				translated.replace(Integer.parseInt(splitSeqEdit.get(0))-1, Integer.parseInt(splitSeqEdit.get(1)), splitSeqEdit.get(2));
			}
		}
		return translated.toString();
	}

	static void printFASTA(String sequence, int lineLength){
		printFASTA(sequence, "",lineLength);
	}

	static void printFASTA(String sequence, String header){
		printFASTA(sequence, header, 60);
	}

	static void printFASTA(String sequence){
		printFASTA(sequence, "",60);
	}

	/** Given a chromosome name, start coordinate, and end coordinate, 
	 * returns the corresponding sequence.
	 * 
	 * @param seqChrName	Chromosome name.
	 * @param seqStart	Sequence start position. If < 0, is changed to 0.
	 * @param seqEnd	Sequence end position.
	 * @return	Sequence from given region.
	 * @throws TechnicalException
	 */
	static String getSequence(String seqChrName, int seqStart, int seqEnd) throws TechnicalException{
		if (seqStart < 1){
			seqStart = 1;
			System.err.println("Sequence start cannot be less than 1, changing to 1");
		}
		if (seqEnd < 0 || seqChrName.equals("") || seqEnd-seqStart+1 < 1){
			return "";
		}
		/* Construct SQL queries to retrieve the sequence for the region of interest
		 * 
		 * sqlQueryStart: retrieve the sequence from the beginning of the region
		 * to the end of the chunk or the end of the region, whichever comes first.
		 * sqlQuery: retrieve the full sequences for any full chunks contained
		 * within the sequence. Query will return empty if the region doesn't span
		 * more than 2 chunks.
		 * sqlQueryEnd: retrieve the sequence from the beginning of the last
		 * chunk to the end of the region. 
		 * 
		 * sqlQuery and sqlQueryEnd will only be run if the region spans more than
		 * one chunk.
		 */

		String sqlQueryStart = String.format("SELECT substring(sequence,%d,%d) FROM hsapiens_genomic_sequence__dna_chunks__main WHERE chr_name = \"%s\" AND chr_start <= %d AND chr_start+%d >= %d;",((seqStart-1)%CHUNK_SIZE)+1,Math.min((seqEnd-seqStart/CHUNK_SIZE*CHUNK_SIZE)+1,CHUNK_SIZE+1)-seqStart%CHUNK_SIZE,seqChrName,seqStart,CHUNK_SIZE-1,seqStart);
		String sqlQuery = String.format("SELECT sequence FROM hsapiens_genomic_sequence__dna_chunks__main WHERE chr_name = \"%s\" AND chr_start <= %d AND chr_start+%d >= %d ORDER BY chr_start;",seqChrName,seqEnd/CHUNK_SIZE*CHUNK_SIZE,CHUNK_SIZE-1,1+(1+seqStart/CHUNK_SIZE)*CHUNK_SIZE);
		String sqlQueryEnd = String.format("SELECT substring(sequence,%d,%d) FROM hsapiens_genomic_sequence__dna_chunks__main WHERE chr_name = \"%s\" AND chr_start <= %d AND chr_start+%d >= %d;",1,seqEnd%CHUNK_SIZE,seqChrName,seqEnd,CHUNK_SIZE-1,seqEnd);

		StringBuilder retrievedSequence = new StringBuilder(seqEnd-seqStart+1);
		try {
			Statement stmt = null;
			stmt = databaseConnection.createStatement();

			ResultSet result = stmt.executeQuery(sqlQueryStart);
			result.next();
			retrievedSequence.append(result.getString(1));

			// If the region spans more than one chunk, execute sqlQuery and sqlQueryEnd
			if ((seqStart-1)/CHUNK_SIZE != (seqEnd-1)/CHUNK_SIZE){
				result = stmt.executeQuery(sqlQuery);

				// Stitch together all retrieved sequences
				while (result.next()){
					retrievedSequence.append(result.getString(1));
				}

				result = stmt.executeQuery(sqlQueryEnd);

				result.next();
				retrievedSequence.append(result.getString(1));
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return retrievedSequence.toString();
	}

	/**
	 * Parses input rows for QueryType TRANSCRIPT_EXON_INTRON.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, ordered by transcript_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */
	static void parseTranscriptExonIntron(List<List<String>> inputQuery, int[] flank) throws TechnicalException{
		// "Unspliced (Transcript)"
		// Doesn't yet handle flank

		// Set up the fields
		final int transcriptIDfield = 0;
		final int chrField = 1;
		final int startField = 2;
		final int endField = 3;
		final int strandField = 4;
		final int headerField = 5;

		// Read the first line of the input and initialize the variables
		List<String> firstLine = inputQuery.get(0);
		String transcriptID = firstLine.get(transcriptIDfield);
		String chr = firstLine.get(chrField);
		List<StringBuilder> headerData = new ArrayList<StringBuilder>();
		for(int i = headerField; i < firstLine.size(); i++){
			headerData.add(new StringBuilder(firstLine.get(i)));
		}
		StringBuilder headerLine = new StringBuilder();
		int start = Integer.parseInt(firstLine.get(startField));
		int end = Integer.parseInt(firstLine.get(endField));
		String strand = firstLine.get(strandField);
		for(List<String> line : inputQuery){
			// Check if the current row belongs to the same transcript
			if (transcriptID.equals(line.get(transcriptIDfield))){
				// If it does, adjust the start and end positions if needed
				start = Math.min(start, Integer.parseInt(line.get(startField)));
				end = Math.max(end, Integer.parseInt(line.get(endField)));
				// Update header as necessary
				for(int i = headerField; i < line.size(); i++){
					if(headerData.get(i-headerField).indexOf(line.get(i))<0){
						StringBuilder tempString = headerData.get(i-headerField);
						tempString.append(";" + line.get(i));
						headerData.set(i-headerField,tempString);
					}
				}
			}
			else {
				// If it doesn't, we're ready to print that sequence

				// Prepare header
				for (int i = 0; i < headerData.size(); ++i){
					headerLine.append(headerData.get(i));
					headerLine.append('|');
				}
				headerLine.deleteCharAt(headerLine.length()-1);
				printTranscriptExonIntron(headerLine.toString(), chr, start, end, strand,flank);

				// Initialize for the next transcript ID, and re-enter the loop
				transcriptID = line.get(transcriptIDfield);
				chr = line.get(chrField);
				start = Integer.parseInt(line.get(startField));
				end = Integer.parseInt(line.get(endField));
				strand = line.get(strandField);
				// Re-initialize header
				headerData.clear();
				for(int i = headerField; i < line.size(); i++){
					headerData.add(new StringBuilder(line.get(i)));
				}
				headerLine = new StringBuilder();
			}
		}
		// Print out the final sequence
		for (int i = 0; i < headerData.size(); ++i){
			headerLine.append(headerData.get(i));
			headerLine.append('|');
		}
		headerLine.deleteCharAt(headerLine.length()-1);
		printTranscriptExonIntron(headerLine.toString(), chr, start, end, strand, flank);
	}

	/**
	 * Retrieves and prints sequence for QueryType TRANSCRIPT_EXON_INTRON
	 * @param transcriptID	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param strand	Sequence strand.
	 * @throws TechnicalException
	 */
	private static void printTranscriptExonIntron(String transcriptID,
			String chr, int start, int end, String strand, int[] flank)
	throws TechnicalException {
		String sequence;
		// Take the reverse complement if necessary
		if (strand.equals("-1")){
			sequence = reverseComplement(getSequence(chr, start-flank[1], end+flank[0]));
		} else {
			sequence = getSequence(chr, start-flank[0], end+flank[1]);
		}
		printFASTA(sequence,transcriptID);
	}

	/**
	 * Parses input rows for QueryType TRANSCRIPT_FLANK.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, ordered by transcript_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */
	static void parseTranscriptFlank(List<List<String>> inputQuery, int[] flank)throws TechnicalException{
		// "Flank (Transcript)"

		// Make sure the flank region request makes sense
		checkFlank(flank);

		// Set up the fields
		final int transcriptIDField = 0;
		final int chrField = 1;
		final int startField = 2;
		final int endField = 3;
		final int strandField = 4;
		final int headerField = 5;

		// Initialize the variables for the first transcript ID
		List<String> firstLine = inputQuery.get(0);
		String transcriptID = firstLine.get(transcriptIDField);
		String chr = firstLine.get(chrField);
		String headerLine = "";
		String sequence = "";
		int start = Integer.parseInt(firstLine.get(startField));
		int end = Integer.parseInt(firstLine.get(endField));
		String strand = firstLine.get(strandField);
		for(List<String> line : inputQuery){
			// Check if the current row belongs to the same transcript
			if (transcriptID.equals(line.get(transcriptIDField))){
				// If it does, adjust start and end as needed
				start = Math.min(start, Integer.parseInt(line.get(startField)));
				end = Math.max(end, Integer.parseInt(line.get(endField)));
			}
			else {
				// If it isn't, we print the last sequence and initialize the next
				printTranscriptFlank(flank, transcriptID, chr, start, end, strand);

				transcriptID = line.get(transcriptIDField);
				chr = line.get(chrField);
				start = Integer.parseInt(line.get(startField));
				end = Integer.parseInt(line.get(endField));
				strand = line.get(strandField);
			}
		}
		// Print the final sequence
		printTranscriptFlank(flank, transcriptID, chr, start, end, strand);
	}

	/**
	 * Checks that one and only one entry of flank is greater than zero.
	 * @param flank The array containing the flank parameters
	 */
	private static void checkFlank(int[] flank) {
		if (flank[0]==0 && flank[1]==0){
			System.out.println("Validation Error: Requests for flank sequence must be accompanied by an upstream_flank or downstream_flank request");
			return;
		} else if (flank[0] > 0 && flank[1]> 0){
			System.out.println("Validation Error: For this sequence option choose upstream OR downstream gene flanking sequence, NOT both, as makes no sense to simply concatenate them together.");
			return;
		} else if (flank[0] < 0 || flank[1] < 0){
			System.out.println("Validation Error: Flank distance can not be negative.");
			return;
		}
	}

	/**
	 * Retrieves and prints sequence for QueryType TRANSCRIPT_FLANK
	 * @param flank	Array containing the upstream/downstream flank information.
	 * @param transcriptID Header for the sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param strand	Sequence strand.
	 * @throws TechnicalException
	 */
	private static void printTranscriptFlank(int[] flank, String transcriptID, String chr,
			int start, int end, String strand) throws TechnicalException {
		String sequence;
		// Check whether we're dealing with the 5' flank or the 3', and handle accordingly
		if (flank[0]>0){
			if (strand.equals("-1")){
				sequence = reverseComplement(getSequence(chr, end+1, end+flank[0]));
			} else {
				sequence = (getSequence(chr, Math.max(start-flank[0],0), start-1));
			}	
		} else {
			if (strand.equals("-1")){
				sequence = reverseComplement(getSequence(chr, Math.max(start-flank[1],0), start-1));
			} else {
				sequence = (getSequence(chr, end+1, end+flank[1]));
			}						
		}
		printFASTA(sequence,transcriptID);
	}

	/**
	 * Connects to database. If command-line arguments are present, determines the QueryType and parses the input file.
	 * Otherwise, uses hard-coded query parameters. Disconnects from database.
	 * @param args Command-line arguments (integer determining QueryType, filename containing tab-delimited data).
	 */
	public static void main(String[] args) {
		try {
			List<List<String>> inputQuery = null;
			QueryType currentQueryType;
			String searchUrl = null;
			int[] flank = { 0, 0 };

			connectDB();
			System.err.println("Connected to DB.");

			// If there is an argument, open the file given by the first and read
			// its contents into a list of lists
			if (args.length > 1){
				if (args.length > 3){
					flank[0] = Integer.parseInt(args[2]);
					flank[1] = Integer.parseInt(args[3]);
				} else {
					flank[0] = 0;
					flank[1] = 0;
				}
				BufferedReader in = null;
				try {
					inputQuery = new ArrayList<List<String>>();
					in = new BufferedReader(new FileReader(args[1]));
					String line = null;
					while ((line = in.readLine()) != null) {
						if (!MyUtils.isEmpty(line)) {			
							inputQuery.add(MyUtils.splitLine("\t", line));
						}
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (in != null){
						in.close();
					}
				}
				currentQueryType = queryTypeMap[Integer.parseInt(args[0])];
			} else {
				System.err.println("Invalid arguments: using default search");
				List<Object> parsedURL = parseQueryUrl(new StringBuilder(
						"http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22FASTA%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22cdna%22%20/%3E%3C/Dataset%3E%3C/Query%3E"
				));
				currentQueryType = (QueryType) parsedURL.get(0);//QueryType.TRANSCRIPT_EXON_INTRON;
				searchUrl = (String) parsedURL.get(1);
				System.err.println(searchUrl);
//				inputQuery = MyUtils.copyUrlContentToListStringList(
//						new URL(searchUrl), "\t");
				System.err.println("BioMart Query complete.");
			}
			switch (currentQueryType) {
			case TRANSCRIPT_EXON_INTRON:
				//String transcriptID = "ENST00000306726";
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22ensembl_transcript_id%22%20value%20=%20%22" + transcriptID + "%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseTranscriptExonIntron(inputQuery, flank);
				break;
			case GENE_EXON_INTRON:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_gene_id%22%20/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseGeneExonIntron(inputQuery, flank);
				break;
			case TRANSCRIPT_FLANK:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseTranscriptFlank(inputQuery, flank);
				break;
			case GENE_FLANK:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_gene_id%22%20/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseGeneFlank(inputQuery, flank);
				break;
			case CODING_TRANSCRIPT_FLANK:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22coding_start_offset%22%20/%3E%3CAttribute%20name%20=%20%22coding_end_offset%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3CAttribute%20name%20=%20%22exon_id%22%20/%3E%3CAttribute%20name%20=%20%22rank%22%20/%3E%3CAttribute%20name%20=%20%22start_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22end_exon_id%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseCodingTranscriptFlank(inputQuery, flank);
				break;
			case CODING_GENE_FLANK:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_gene_id%22%20/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22coding_start_offset%22%20/%3E%3CAttribute%20name%20=%20%22coding_end_offset%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3CAttribute%20name%20=%20%22exon_id%22%20/%3E%3CAttribute%20name%20=%20%22rank%22%20/%3E%3CAttribute%20name%20=%20%22start_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22end_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22transcript_count%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseCodingGeneFlank(inputQuery, flank);
				break;
			case FIVE_UTR:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22coding_start_offset%22%20/%3E%3CAttribute%20name%20=%20%22coding_end_offset%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3CAttribute%20name%20=%20%22exon_id%22%20/%3E%3CAttribute%20name%20=%20%22rank%22%20/%3E%3CAttribute%20name%20=%20%22start_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22end_exon_id%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parse5UTR(inputQuery, flank);
				break;
			case THREE_UTR:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22coding_start_offset%22%20/%3E%3CAttribute%20name%20=%20%22coding_end_offset%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3CAttribute%20name%20=%20%22exon_id%22%20/%3E%3CAttribute%20name%20=%20%22rank%22%20/%3E%3CAttribute%20name%20=%20%22start_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22end_exon_id%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parse3UTR(inputQuery, flank);
				break;
			case GENE_EXON:
				// Might need a bit of work
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22exon_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				// This query structure is exactly the same as TRANSCRIPT_EXON_INTRON, except exon_id is substituted for ensembl_transcript_id
				parseTranscriptExonIntron(inputQuery, flank);
				break;
			case CDNA:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3CAttribute%20name%20=%20%22rank%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseCDNA(inputQuery, flank);
				break;
			case CODING:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22coding_start_offset%22%20/%3E%3CAttribute%20name%20=%20%22coding_end_offset%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3CAttribute%20name%20=%20%22exon_id%22%20/%3E%3CAttribute%20name%20=%20%22rank%22%20/%3E%3CAttribute%20name%20=%20%22start_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22end_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22phase%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseCoding(inputQuery, flank, false);
				break;
			case PEPTIDE:
				//searchUrl = "http://bm-test.res.oicr.on.ca:9603/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22TSV%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2220000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22ensembl_transcript_id%22%20/%3E%3CAttribute%20name%20=%20%22chromosome_name%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_start%22%20/%3E%3CAttribute%20name%20=%20%22exon_chrom_end%22%20/%3E%3CAttribute%20name%20=%20%22coding_start_offset%22%20/%3E%3CAttribute%20name%20=%20%22coding_end_offset%22%20/%3E%3CAttribute%20name%20=%20%22strand%22%20/%3E%3CAttribute%20name%20=%20%22exon_id%22%20/%3E%3CAttribute%20name%20=%20%22rank%22%20/%3E%3CAttribute%20name%20=%20%22start_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22end_exon_id%22%20/%3E%3CAttribute%20name%20=%20%22phase%22%20/%3E%3CAttribute%20name%20=%20%22codon_table_id%22%20/%3E%3CAttribute%20name%20=%20%22seq_edits%22%20/%3E%3C/Dataset%3E%3C/Query%3E";
				parseCoding(inputQuery, flank, true);
				break;
			default:
				break;
			}
		}catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (databaseConnection != null){
				disconnectDB();
			}
		}
	}

	/**
	 * Parses input rows for QueryType CODING_GENE_FLANK.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_gene_id, ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id, transcript_count.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */
	private static void parseCodingGeneFlank(List<List<String>> inputQuery,
			int[] flank) throws TechnicalException {
		// "Flank-coding region (Gene)"
		// TODO Optimize by using the transcript_count info when initializing lists? (Except transcript_count seems to be empty)
		// TODO Clean up in general

		checkFlank(flank);

		// Set up the fields
		final int geneIDfield = 0;
		final int transcriptIDfield = 1;
		final int chrField = 2;
		final int startField = 3;
		final int endField = 4;
		final int codingStartOffsetField = 5;
		final int codingEndOffsetField = 6;
		final int strandField = 7;
		final int exonIDField = 8;
		final int rankField = 9;
		final int startExonIDField = 10;
		final int endExonIDField = 11;
		final int transcriptCountField = 12;

		int terminalExonField ;
		int codingOffsetField ;
		if (flank[0] >0 ){
			terminalExonField = startExonIDField;
			codingOffsetField = codingStartOffsetField;
		} else {
			terminalExonField = endExonIDField;
			codingOffsetField = codingEndOffsetField;
		}

		// Initialize hashmap mapping geneIDs to input lines, so we don't need to worry about the order of the input
		HashMap<String, List<List<String>>> geneMap = new HashMap<String,List<List<String>>>();
		String currentGeneID;
		List<List<String>> appendedList = null;
		for(List<String> line : inputQuery){
			currentGeneID = line.get(geneIDfield);
			appendedList = geneMap.get(currentGeneID);
			if(null==appendedList) {
				appendedList = new ArrayList<List<String>>();
			}
			appendedList.add(line);
			geneMap.put(currentGeneID, appendedList);
		}
		List<List<String>> currentGene = null;
		List<String> firstLine = null;
		for(String geneID : geneMap.keySet()){
			currentGene = geneMap.get(geneID);
			firstLine = currentGene.get(0);
			String transcriptID = firstLine.get(transcriptIDfield);
			String terminalExonID = firstLine.get(terminalExonField);
			String chr = "";
			String headerLine = "";
			Integer start = 0;
			Integer end = 0;
			String strand = firstLine.get(strandField);
			int codingOffset = 0;
			String exonID = "";
			for(List<String> line : currentGene){
				exonID = line.get(exonIDField);
				if (!(transcriptID.equals(line.get(transcriptIDfield)))){
					transcriptID = line.get(transcriptIDfield);
					terminalExonID = line.get(terminalExonField);
					strand = line.get(strandField);
				}
				if (exonID.equals(terminalExonID)){
					codingOffset = Integer.parseInt(line.get(codingOffsetField));
					if(chr.equals("")){
						start = Integer.parseInt(line.get(startField))+codingOffset;
						end = Integer.parseInt(line.get(endField))-codingOffset;
					} else {
						start = Math.min(start, Integer.parseInt(line.get(startField))+codingOffset);
						end = Math.max(end, Integer.parseInt(line.get(endField))-codingOffset);
					}
					chr = line.get(chrField);
				}
			}
			if (flank[0]>0) {
				if (strand.equals("-1")) {
					printTranscriptExonIntron(geneID, chr, end + 2,
							end + flank[0] + 1, strand, new int[] {0, 0});
				} else {
					printTranscriptExonIntron(geneID, chr,
							start - flank[0] - 1, start - 2, strand, new int[] {0, 0});
				}
			} else {
				if (strand.equals("-1")) {
					printTranscriptExonIntron(geneID, chr, end - flank[1],
							end-1, strand, new int[] {0, 0});
				} else {
					printTranscriptExonIntron(geneID, chr,
							start, start - 1 + flank[1], strand, new int[] {0, 0});
				}
			}
		}
	}

	/**
	 * Parses input rows for QueryType GENE_FLANK.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_gene_id, ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, transcript_count.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */
	private static void parseGeneFlank(List<List<String>> inputQuery,
			int[] flank) throws TechnicalException {
		// "Unspliced (Gene)"
		/* Exactly the same as parseGeneIntronExon, except it calls 
		 * printTranscriptFlank instead of printTranscriptExonIntron, and it checks
		 * for reasonable flank input.
		 */		
		// TODO Optimize by using the transcript_count info when initializing lists? (Except transcript_count seems to be empty)

		checkFlank(flank);

		// Set up the fields
		final int geneIDfield = 0;
		final int chrField = 2;
		final int startField = 3;
		final int endField = 4;
		final int strandField = 5;

		// Initialize hashmap mapping geneIDs to input lines, so we don't need to worry about the order of the input
		HashMap<String, List<List<String>>> geneMap = new HashMap<String,List<List<String>>>();
		String currentGeneID;
		List<List<String>> appendedList = null;
		for(List<String> line : inputQuery){
			currentGeneID = line.get(geneIDfield);
			appendedList = geneMap.get(currentGeneID);
			if(null==appendedList) {
				appendedList = new ArrayList<List<String>>();
			}
			appendedList.add(line);
			geneMap.put(currentGeneID, appendedList);
		}
		List<List<String>> currentGene = null;
		List<String> firstLine = null;
		for(String geneID : geneMap.keySet()){
			currentGene = geneMap.get(geneID);
			firstLine = currentGene.get(0);
			String chr = firstLine.get(chrField);
			String headerLine = "";
			int start = Integer.parseInt(firstLine.get(startField));
			int end = Integer.parseInt(firstLine.get(endField));
			String strand = firstLine.get(strandField);
			for(List<String> line : currentGene){
				start = Math.min(start, Integer.parseInt(line.get(startField)));
				end = Math.max(end, Integer.parseInt(line.get(endField)));
			}
			printTranscriptFlank(flank, geneID, chr, start, end, strand);
		}	}

	/**
	 * Parses input rows for QueryType GENE_EXON_INTRON.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_gene_id, ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, transcript_count.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */
	private static void parseGeneExonIntron(List<List<String>> inputQuery,
			int[] flank) throws TechnicalException {
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
		// TODO Doesn't yet handle flank
		// TODO Optimize by using the transcript_count info when initializing lists? (Except transcript_count seems to be empty)

		// Set up the fields
		final int geneIDfield = 0;
		final int chrField = 2;
		final int startField = 3;
		final int endField = 4;
		final int strandField = 5;

		// Initialize hashmap mapping geneIDs to input lines, so we don't need to worry about the order of the input
		HashMap<String, List<List<String>>> geneMap = new HashMap<String,List<List<String>>>();
		String currentGeneID;
		List<List<String>> appendedList = null;
		for(List<String> line : inputQuery){
			currentGeneID = line.get(geneIDfield);
			appendedList = geneMap.get(currentGeneID);
			if(null==appendedList) {
				appendedList = new ArrayList<List<String>>();
			}
			appendedList.add(line);
			geneMap.put(currentGeneID, appendedList);
		}
		List<List<String>> currentGene = null;
		List<String> firstLine = null;
		for(String geneID : geneMap.keySet()){
			currentGene = geneMap.get(geneID);
			firstLine = currentGene.get(0);
			String chr = firstLine.get(chrField);
			String headerLine = "";
			int start = Integer.parseInt(firstLine.get(startField));
			int end = Integer.parseInt(firstLine.get(endField));
			String strand = firstLine.get(strandField);
			for(List<String> line : currentGene){
				start = Math.min(start, Integer.parseInt(line.get(startField)));
				end = Math.max(end, Integer.parseInt(line.get(endField)));
			}
			printTranscriptExonIntron(geneID, chr, start, end, strand, flank);
		}
	}

	/**
	 * Parses input rows for QueryType CODING.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id, phase.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @param isProtein If TRUE, translates the resulting sequence to a protein sequence.
	 * @throws TechnicalException
	 */
	private static void parseCoding(List<List<String>> inputQuery, int[] flank, boolean isProtein) throws TechnicalException {
		// "Coding sequence" and "Protein"
		if (isProtein && (flank[0] > 0  || flank[1] > 0)){
			//TODO throw an exception
			System.out.println("Validation Error: Protein sequences can not have flanking regions.");
			return;
		}
		final int transcriptIDField = 0;
		final int chrField = 1;
		final int startField = 2;
		final int endField = 3;
		final int codingOffsetStartField = 4;
		final int codingOffsetEndField = 5;
		final int strandField = 6;
		final int exonIDField = 7;
		final int rankField = 8;
		final int startExonIDField = 9;
		final int endExonIDField = 10;
		final int phaseField = 11;
		final int codonTableField = 12;
		final int seqEditField = 13;



		// These TreeMaps will keep track of the exons
		TreeMap<Integer, Integer> start = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> end = new TreeMap<Integer, Integer>();

		List<String> firstLine = inputQuery.get(0);
		String transcriptID = firstLine.get(transcriptIDField);
		String startExonID = firstLine.get(startExonIDField);
		String endExonID = firstLine.get(endExonIDField);
		int startExonRank = 0;
		int endExonRank = 0;
		String exonID = firstLine.get(exonIDField);
		String chr = "";
		String headerLine = "";
		int codingStartOffset = 0;
		int codingEndOffset = 0;
		int startPhase = 0;
		int currentRank = Integer.parseInt(firstLine.get(rankField));
		String strand = firstLine.get(strandField);
		HashSet<String> seqEdit = new HashSet<String>();
		String codonTableID = null;
		if(isProtein){
			codonTableID = firstLine.get(codonTableField);
			seqEdit.add(firstLine.get(seqEditField));
		}

		for(List<String> line : inputQuery){
			exonID = line.get(exonIDField);
			if (!(transcriptID.equals(line.get(transcriptIDField)))){
				//If it's a new transcript, print the current sequence
				printCoding(transcriptID, chr, start, end, codingStartOffset, codingEndOffset, startExonRank, endExonRank, strand, startPhase, flank, codonTableID, seqEdit,isProtein);
				transcriptID = line.get(transcriptIDField);
				startExonID = line.get(startExonIDField);
				endExonID = line.get(endExonIDField);
				startExonRank = 0;
				endExonRank = 0;
				startPhase = 0;
				chr = "";
				start.clear();
				end.clear();
				strand = line.get(strandField);
				if(isProtein){
					codonTableID = line.get(codonTableField);
					seqEdit = new HashSet<String>();
					seqEdit.add(line.get(seqEditField));
				}
			}
			currentRank = Integer.parseInt(line.get(rankField))-1; // Subtract 1 to convert to zero indexing
			if(isProtein){
				seqEdit.add(line.get(seqEditField));
			}
			if (!startExonID.equals("")) {
				start.put(currentRank, Integer.parseInt(line
						.get(startField)));
				end.put(currentRank, Integer.parseInt(line
						.get(endField)));
			}
			if (exonID.equals(startExonID)){
				// If it's the terminal exon, record the chromosome and codingOffset and Phase
				chr = line.get(chrField);
				codingStartOffset = Integer.parseInt(line.get(codingOffsetStartField));
				startExonRank = currentRank;
				startPhase = Integer.parseInt(line.get(phaseField));
			}
			if (exonID.equals(endExonID)){
				// If it's the terminal exon, record the chromosome and codingOffset
				chr = line.get(chrField);
				codingEndOffset = Integer.parseInt(line.get(codingOffsetEndField));
				endExonRank = currentRank;
			}
			//System.out.println(terminalExonID + "\t" + exonID);
		}
		printCoding(transcriptID, chr, start, end, codingStartOffset, codingEndOffset, startExonRank, endExonRank, strand,startPhase,flank,codonTableID,seqEdit,isProtein);
	}

	/**
	 * Retrieves and prints sequence for QueryType CODING
	 * @param transcriptID Header for the sequence.
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
	 * @throws TechnicalException
	 */
	private static void printCoding(String transcriptID, String chr,
			TreeMap<Integer, Integer> start, TreeMap<Integer, Integer> end, int codingStartOffset,
			int codingEndOffset, int startExonRank, int endExonRank, String strand, int startPhase, int[] flank ,String codonTableID ,HashSet<String> seqEdit,boolean isProtein) throws TechnicalException {
		StringBuilder sequence = new StringBuilder();
		if (!chr.equals("")){
			if (strand.equals("-1")){
				if (startExonRank == endExonRank){
					sequence.append(reverseComplement(getSequence(chr,end.get(startExonRank)-codingEndOffset+1-flank[1],end.get(startExonRank)-codingStartOffset+1+flank[0])));
				} else {
					sequence.append(reverseComplement(getSequence(chr,start.get(startExonRank),end.get(startExonRank)-codingStartOffset+1+flank[0])));
					for (int i = startExonRank+1; i < endExonRank; i++){
						sequence.append(reverseComplement(getSequence(chr, start.get(i), end.get(i))));
					}
					sequence.append(reverseComplement(getSequence(chr,end.get(endExonRank)-codingEndOffset+1-flank[1],end.get(endExonRank))));
				}
			} else {
				if (startExonRank == endExonRank){
					sequence.append(getSequence(chr,start.get(startExonRank)+codingStartOffset-1-flank[0],start.get(startExonRank)+codingEndOffset-1+flank[1]));
				} else {
					sequence.append(getSequence(chr,start.get(startExonRank)+codingStartOffset-1-flank[0],end.get(startExonRank)));
					for (int i = startExonRank+1; i < endExonRank; i++){
						sequence.append((getSequence(chr, start.get(i), end.get(i))));
					}
					sequence.append(getSequence(chr,start.get(endExonRank),start.get(endExonRank)+codingEndOffset-1+flank[1]));
				}
			}	
		}
		if (sequence.length()>0){
			for(int i = startPhase; i > 0; --i){
				sequence.insert(0, 'N');
			}
		}
		if(isProtein){
			printFASTA(translateSequence(sequence.toString(), seqEdit, codonTableID),transcriptID);
		} else {
			printFASTA(sequence.toString(),transcriptID);
		}
	}

	/**
	 * Parses input rows for QueryType CDNA.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, strand, rank.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */

	private static void parseCDNA(List<List<String>> inputQuery, int[] flank) throws TechnicalException {
		// "cDNA sequences"

		final int transcriptIDfield = 0;
		final int chrField = 1;
		final int startField = 2;
		final int endField = 3;
		final int strandField = 4;
		final int rankField = 5;

		// These TreeMaps will keep track of the exons
		TreeMap<Integer, Integer> start = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> end = new TreeMap<Integer, Integer>();

		List<String> firstLine = inputQuery.get(0);

		String transcriptID = firstLine.get(transcriptIDfield);
		String chr = firstLine.get(chrField);
		String headerLine = "";
		int currentRank = Integer.parseInt(firstLine.get(rankField));
		String strand = firstLine.get(strandField);

		for(List<String> line : inputQuery){
			if (!(transcriptID.equals(line.get(transcriptIDfield)))){
				//If it's a new transcript, print the current sequence
				printCDNA(transcriptID, chr, start, end, strand,flank);
				transcriptID = line.get(transcriptIDfield);
				chr = line.get(chrField);
				start.clear();
				end.clear();
				strand = line.get(strandField);
			}
			currentRank = Integer.parseInt(line.get(rankField))-1; // Subtract 1 to convert to zero indexing
			start.put(currentRank, Integer.parseInt(line.get(startField)));
			end.put(currentRank, Integer.parseInt(line.get(endField)));
		}
		printCDNA(transcriptID, chr, start, end, strand,flank);

	}

	/**
	 * Retrieves and prints sequence for QueryType CDNA.
	 * @param transcriptID	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param strand	Sequence strand.
	 * @throws TechnicalException
	 */
	private static void printCDNA(String transcriptID, String chr,
			TreeMap<Integer,Integer> start, TreeMap<Integer,Integer> end, String strand, int[] flank) throws TechnicalException {
		StringBuilder sequence = new StringBuilder();
		if (!chr.equals("")) {
			if (strand.equals("-1")){
				sequence.append(reverseComplement(getSequence(chr,end.get(0)+1,end.get(0)+flank[0])));
				for (int i = 0; i < start.size(); i++){
					sequence.append(reverseComplement(getSequence(chr, start.get(i), end.get(i))));
				}
				sequence.append(reverseComplement(getSequence(chr,start.get(start.size()-1)-flank[1],start.get(start.size()-1)-1)));
			} else {
				sequence.append(getSequence(chr,start.get(0)-flank[0],start.get(0)-1));
				for (int i = 0; i < start.size(); i++){
					sequence.append((getSequence(chr, start.get(i), end.get(i))));
				}
				sequence.append(getSequence(chr,end.get(end.size()-1)+1,end.get(end.size()-1)+flank[1]));
			}	
		}
		printFASTA(sequence.toString(),transcriptID);
	}

	/**
	 * Parses input rows for QueryType THREE_UTR.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */
	private static void parse3UTR(List<List<String>> inputQuery, int[] flank) throws TechnicalException {
		// "3' UTR"
		final int transcriptIDField = 0;
		final int chrField = 1;
		final int startField = 2;
		final int endField = 3;
		final int codingOffsetField = 5;
		final int strandField = 6;
		final int exonIDField = 7;
		final int rankField = 8;
		final int terminalExonField = 10;

		// These TreeMaps will keep track of the exons after the end_exon
		TreeMap<Integer, Integer> start = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> end = new TreeMap<Integer, Integer>();

		List<String> firstLine = inputQuery.get(0);

		String transcriptID = firstLine.get(transcriptIDField);
		String terminalExonID = firstLine.get(terminalExonField);
		int terminalExonRank = 0;
		String exonID = firstLine.get(exonIDField);
		String chr = "";
		String headerLine = "";
		int codingOffset = 0;
		int currentRank = Integer.parseInt(firstLine.get(rankField));
		String strand = firstLine.get(strandField);

		for(List<String> line : inputQuery){
			exonID = line.get(exonIDField);
			if (!(transcriptID.equals(line.get(transcriptIDField)))){
				//If it's a new transcript, print the current sequence
				print3UTR(transcriptID, chr, start, end, codingOffset, terminalExonRank, strand, flank);
				transcriptID = line.get(transcriptIDField);
				terminalExonID = line.get(terminalExonField);
				terminalExonRank = 0;
				chr = "";
				start.clear();
				end.clear();
				strand = line.get(strandField);
			}
			currentRank = Integer.parseInt(line.get(rankField))-1; // Subtract 1 to convert to zero indexing
			{
				start.put(currentRank, Integer.parseInt(line.get(startField)));
				end.put(currentRank, Integer.parseInt(line.get(endField)));
			}
			if (exonID.equals(terminalExonID)){
				// If it's the terminal exon, record the chromosome and codingOffset
				chr = line.get(chrField);
				codingOffset = Integer.parseInt(line.get(codingOffsetField));
				terminalExonRank = currentRank;
			}
			//System.out.println(terminalExonID + "\t" + exonID);
		}
		print3UTR(transcriptID, chr, start, end, codingOffset,terminalExonRank, strand, flank);
	}

	/**
	 * Retrieves and prints sequence for QueryType TRANSCRIPT_EXON_INTRON
	 * @param transcriptID	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param terminalExonRank	Rank of the terminal exon.
	 * @param strand	Sequence strand.
	 * @param flank How much upstream/downstream flank to add
	 * @throws TechnicalException
	 */
	private static void print3UTR(String transcriptID, String chr,
			TreeMap<Integer,Integer> start, TreeMap<Integer,Integer> end, int codingOffset,
			int terminalExonRank, String strand, int[] flank) throws TechnicalException {
		StringBuilder sequence = new StringBuilder();
		if (!chr.equals("")) {
			if (strand.equals("-1")){
				if(!(getSequence(chr,start.get(terminalExonRank),end.get(terminalExonRank)-codingOffset)).equals("")){
					sequence.append(reverseComplement(getSequence(chr,start.get(terminalExonRank),end.get(terminalExonRank)-codingOffset+flank[0])));
					for (int i = terminalExonRank+1; i < start.size(); i++){
						sequence.append(reverseComplement(getSequence(chr, start.get(i), end.get(i))));
					}
					sequence.append(reverseComplement(getSequence(chr, start.get(start.size()-1)-flank[1],start.get(start.size()-1)-1)));
				}
			} else {
				if(!(getSequence(chr,start.get(terminalExonRank)+codingOffset,end.get(terminalExonRank))).equals("")){
					sequence.append(getSequence(chr,start.get(terminalExonRank)+codingOffset-flank[0],end.get(terminalExonRank)));
					for (int i = terminalExonRank+1; i < start.size(); i++){
						sequence.append((getSequence(chr, start.get(i), end.get(i))));
					}
					sequence.append((getSequence(chr, end.get(start.size()-1)+1,end.get(start.size()-1)+flank[1])));
				}
			}	
		}
		printFASTA(sequence.toString(),transcriptID);
	}

	/**
	 * Parses input rows for QueryType FIVE_UTR.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */

	private static void parse5UTR(List<List<String>> inputQuery, int[] flank) throws TechnicalException{
		// "5' UTR"

		final int transcriptIDField = 0;
		final int chrField = 1;
		final int startField = 2;
		final int endField = 3;
		final int codingOffsetField = 4;
		final int strandField = 6;
		final int exonIDField = 7;
		final int rankField = 8;
		final int terminalExonField = 9;

		// These TreeMaps will keep track of the exons before the start_exon
		TreeMap<Integer, Integer> start = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> end = new TreeMap<Integer, Integer>();

		List<String> firstLine = inputQuery.get(0);
		String transcriptID = firstLine.get(transcriptIDField);
		String terminalExonID = firstLine.get(terminalExonField);
		int terminalExonRank = 0;
		String exonID = firstLine.get(exonIDField);
		String chr = "";
		String headerLine = "";
		int codingOffset = 0;
		int currentRank = Integer.parseInt(firstLine.get(rankField));
		String strand = firstLine.get(strandField);

		for(List<String> line : inputQuery){
			exonID = line.get(exonIDField);
			if (!(transcriptID.equals(line.get(transcriptIDField)))){
				//If it's a new transcript, print the current sequence
				print5UTR(transcriptID, chr, start, end, codingOffset, terminalExonRank, strand, flank);
				transcriptID = line.get(transcriptIDField);
				terminalExonID = line.get(terminalExonField);
				terminalExonRank = 0;
				chr = "";
				start.clear();
				end.clear();
				strand = line.get(strandField);
			}
			currentRank = Integer.parseInt(line.get(rankField))-1; // Subtract 1 to convert to zero indexing
			start.put(currentRank, Integer.parseInt(line.get(startField)));
			end.put(currentRank, Integer.parseInt(line.get(endField)));
			if (exonID.equals(terminalExonID)){
				// If it's the terminal exon, record the chromosome and codingOffset
				chr = line.get(chrField);
				codingOffset = Integer.parseInt(line.get(codingOffsetField));
				terminalExonRank = currentRank;
			}
		}
		print5UTR(transcriptID, chr, start, end, codingOffset,terminalExonRank, strand, flank);
	}

	/**
	 * Retrieves and prints sequence for QueryType TRANSCRIPT_EXON_INTRON
	 * @param transcriptID	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param terminalExonRank	Rank of the terminal exon.
	 * @param strand	Sequence strand.
	 * @throws TechnicalException
	 */
	private static void print5UTR(String transcriptID, String chr, TreeMap<Integer, Integer> start, TreeMap<Integer, Integer> end, int codingOffset,int terminalExonRank, String strand, int[] flank) throws TechnicalException {
		StringBuilder sequence = new StringBuilder();
		if (!chr.equals("")){
			if (strand.equals("-1")){
				if(!(getSequence(chr,end.get(terminalExonRank)-codingOffset+2,end.get(terminalExonRank)).equals(""))){
					sequence.append(reverseComplement(getSequence(chr,end.get(0)+1,end.get(0)+flank[0])));
					for (int i = 0; i < terminalExonRank; i++){
						sequence.append(reverseComplement(getSequence(chr, start.get(i), end.get(i))));
					}
					sequence.append(reverseComplement(getSequence(chr,end.get(terminalExonRank)-codingOffset+2-flank[1],end.get(terminalExonRank))));
				}
			} else {
				if(!(getSequence(chr,start.get(terminalExonRank),start.get(terminalExonRank)+codingOffset-2).equals(""))){
					sequence.append(getSequence(chr,start.get(0)-flank[0],start.get(0)-1));
					for (int i = 0; i < terminalExonRank; i++){
						sequence.append((getSequence(chr, start.get(i), end.get(i))));
					}
					sequence.append(getSequence(chr,start.get(terminalExonRank),start.get(terminalExonRank)+codingOffset-2+flank[1]));
				}
			}	
		}
		printFASTA(sequence.toString(),transcriptID);
	}

	/**
	 * Parses input rows for QueryType CODING_TRANSCRIPT_FLANK.
	 * @param inputQuery	List containing query rows. Each Entry is expected to be a list containing the fields ensembl_transcript_id, chromosome_name, exon_chrom_start, exon_chrom_end, coding_start_offset, coding_end_offset, strand, exon_id, rank, start_exon_id, end_exon_id.
	 * @param flank	Array with position 0 containing the upstream flank size and position 1 containing the downstream flank size.
	 * @throws TechnicalException
	 */
	private static void parseCodingTranscriptFlank(List<List<String>> inputQuery,
			int[] flank) throws TechnicalException {
		// "Flank-coding region (Transcript)"
		// The coding_start_offset and coding_end_offset are both given as a distance from
		// the exon_chrom_start of the exon that has exon_id equal to start_exon_id or end_exon_id, respectively
		final int transcriptIDField = 0;
		final int chrField = 1;
		final int startField = 2;
		final int endField = 3;
		int codingOffsetField; // set depending on flank
		final int strandField = 6;
		final int exonIDField = 7;
		int terminalExonField;
		checkFlank(flank);
		if (flank[0] >0 ){
			terminalExonField = 9;
			codingOffsetField = 4;
		} else {
			terminalExonField = 10;
			codingOffsetField = 5;
		}
		List<String> firstLine = inputQuery.get(0);
		String transcriptID = firstLine.get(transcriptIDField);
		String terminalExonID = firstLine.get(terminalExonField);
		String exonID = firstLine.get(exonIDField);
		String chr = "";
		String headerLine = "";
		int start = 0;
		int end = 0;
		int codingOffset = 0;
		String strand = firstLine.get(strandField);

		for(List<String> line : inputQuery){
			exonID = line.get(exonIDField);
			if (!(transcriptID.equals(line.get(transcriptIDField)))){
				//print
				printCodingTranscriptFlank(flank, transcriptID, chr, start, end, codingOffset, strand);
				transcriptID = line.get(transcriptIDField);
				terminalExonID = line.get(terminalExonField);
				chr = "";
				start = 0;
				end = 0;
				strand = line.get(strandField);
			}
			if (exonID.equals(terminalExonID)){
				chr = line.get(chrField);
				start = Integer.parseInt(line.get(startField));
				end = Integer.parseInt(line.get(endField));
				codingOffset = Integer.parseInt(line.get(codingOffsetField));
			}
		}
		printCodingTranscriptFlank(flank, transcriptID, chr, start, end, codingOffset,strand);
	}

	/**
	 * Retrieves and prints sequence for QueryType CODING_TRANSCRIPT_FLANK
	 * @param flank	Array containing upstream/downstream flank distance
	 * @param transcriptID	Header for sequence.
	 * @param chr	Chromosome name.
	 * @param start	Sequence start position.
	 * @param end	Sequence end position.
	 * @param codingOffset Coding offset.
	 * @param strand Sequence strand.
	 * @throws TechnicalException
	 */
	private static void printCodingTranscriptFlank(int[] flank,
			String transcriptID, String chr, int start, int end, int codingOffset,String strand) throws TechnicalException {
		String sequence;
		// Check whether we're dealing with the 5' flank or the 3', and handle accordingly
		if (chr.equals("")){
			sequence = null;
		} else if (flank[0]>0){
			if (strand.equals("-1")){
				sequence = reverseComplement(getSequence(chr, end+2-codingOffset, end+flank[0]-codingOffset+1));
			} else {
				sequence = (getSequence(chr, start-flank[0]+codingOffset-1, start-2+codingOffset));
			}	
		} else {
			if (strand.equals("-1")){
				sequence = reverseComplement(getSequence(chr, end+1-codingOffset-flank[1], end-codingOffset));
			} else {
				sequence = (getSequence(chr, start+codingOffset, start-1+codingOffset+flank[1]));
			}				
		}
		printFASTA(sequence,transcriptID);
	}

	/**
	 * Takes a URL as would be passed to MartService and reformats it to output the 
	 * correct search to be used as input for testing.
	 * @param URL
	 * @return Reformatted URL
	 * @throws Exception
	 */
	static List<Object> parseQueryUrl(StringBuilder URL) throws Exception{
		List<Object> parsed = new ArrayList<Object>();

		int subIndex;
		String queryTypeStr = "";
		String[] attributes;
		if ((subIndex = URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22transcript_exon_intron%22%20/%3E") ) >= 0){
			parsed.add(QueryType.TRANSCRIPT_EXON_INTRON);
			String[] attributeList = {"ensembl_transcript_id", "chromosome_name", "exon_chrom_start", "exon_chrom_end", "strand"};
			attributes = attributeList;
		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22cdna%22%20/%3E") ) >= 0){
			parsed.add(QueryType.CDNA);
			String[] attributeList = {"ensembl_transcript_id","chromosome_name","exon_chrom_start","exon_chrom_end","strand","rank"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22coding%22%20/%3E") ) >= 0){
			parsed.add(QueryType.CODING);
			String[] attributeList = {"ensembl_transcript_id","chromosome_name","exon_chrom_start","exon_chrom_end","coding_start_offset","coding_end_offset","strand","exon_id","rank","start_exon_id","end_exon_id","phase"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22coding_gene_flank%22%20/%3E") ) >= 0){
			parsed.add(QueryType.CODING_GENE_FLANK);
			String[] attributeList = {"ensembl_gene_id","ensembl_transcript_id","chromosome_name","exon_chrom_start","exon_chrom_end","coding_start_offset","coding_end_offset","strand","exon_id","rank","start_exon_id","end_exon_id","transcript_count"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22coding_transcript_flank%22%20/%3E") ) >= 0){
			parsed.add(QueryType.CODING_TRANSCRIPT_FLANK);
			String[] attributeList = {"ensembl_transcript_id", "chromosome_name", "exon_chrom_start", "exon_chrom_end", "coding_start_offset", "coding_end_offset", "strand", "exon_id", "rank", "start_exon_id", "end_exon_id"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%225utr%22%20/%3E") ) >= 0){
			parsed.add(QueryType.FIVE_UTR);
			String[] attributeList = {"ensembl_transcript_id","chromosome_name","exon_chrom_start","exon_chrom_end","coding_start_offset","coding_end_offset","strand","exon_id","rank","start_exon_id","end_exon_id"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%223utr%22%20/%3E") ) >= 0){
			parsed.add(QueryType.THREE_UTR);
			String[] attributeList = {"ensembl_transcript_id","chromosome_name","exon_chrom_start","exon_chrom_end","coding_start_offset","coding_end_offset","strand","exon_id","rank","start_exon_id","end_exon_id"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22gene_exon%22%20/%3E") ) >= 0){
			parsed.add(QueryType.GENE_EXON);
			String[] attributeList = {"exon_id","chromosome_name","exon_chrom_start","exon_chrom_end","strand"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22gene_exon_intron%22%20/%3E") ) >= 0){
			parsed.add(QueryType.GENE_EXON_INTRON);
			String[] attributeList = {"ensembl_gene_id", "ensembl_transcript_id", "chromosome_name", "exon_chrom_start", "exon_chrom_end", "strand", "transcript_count"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22gene_flank%22%20/%3E") ) >= 0){
			parsed.add(QueryType.GENE_FLANK);
			String[] attributeList = {"ensembl_gene_id", "ensembl_transcript_id", "chromosome_name", "exon_chrom_start", "exon_chrom_end", "strand", "transcript_count"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22peptide%22%20/%3E") ) >= 0){
			parsed.add(QueryType.PEPTIDE);
			String[] attributeList = {"ensembl_transcript_id","chromosome_name","exon_chrom_start","exon_chrom_end","coding_start_offset","coding_end_offset","strand","exon_id","rank","start_exon_id","end_exon_id","phase","codon_table_id","seq_edits"};
			attributes = attributeList;

		} else	if ((subIndex =URL.indexOf(queryTypeStr="%3CAttribute%20name%20=%20%22transcript_flank%22%20/%3E") ) >= 0){
			parsed.add(QueryType.TRANSCRIPT_FLANK);
			String[] attributeList = {"ensembl_transcript_id", "chromosome_name", "exon_chrom_start", "exon_chrom_end", "strand"};
			attributes = attributeList;

		} else {
			throw new Exception("No valid sequence return type!");
		}
		URL.delete(subIndex, subIndex + queryTypeStr.length());
		for(int i = attributes.length-1; i >= 0; --i){
			URL.insert(subIndex, "%3CAttribute%20name%20=%20%22"+attributes[i]+"%22%20/%3E");
		}

		// http://www.biomart.org/biomart/martservice?query=%3C?xml%20version=%221.0%22%20encoding=%22UTF-8%22?%3E%3C!DOCTYPE%20Query%3E%3CQuery%20%20virtualSchemaName%20=%20%22default%22%20formatter%20=%20%22FASTA%22%20header%20=%20%220%22%20uniqueRows%20=%20%220%22%20count%20=%20%22%22%20datasetConfigVersion%20=%20%220.7%22%20%3E%3CDataset%20name%20=%20%22hsapiens_gene_ensembl%22%20interface%20=%20%22default%22%20%3E%3CFilter%20name%20=%20%22upstream_flank%22%20value%20=%20%2210%22/%3E%3CFilter%20name%20=%20%22chromosome_name%22%20value%20=%20%221%22/%3E%3CFilter%20name%20=%20%22with_illumina_humanwg_6_v1%22%20excluded%20=%20%220%22/%3E%3CFilter%20name%20=%20%22end%22%20value%20=%20%2210000000%22/%3E%3CFilter%20name%20=%20%22start%22%20value%20=%20%221%22/%3E%3CAttribute%20name%20=%20%22transcript_exon_intron%22%20/%3E%3CAttribute%20name%20=%20%22ensembl_gene_id%22%20/%3E%3C/Dataset%3E%3C/Query%3E
		// no Flank for now

		parsed.add(URL.toString().replaceFirst("FASTA", "TSV"));
		return parsed;
	}
}

