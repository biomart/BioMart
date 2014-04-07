package org.biomart.processors.sequence;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.biomart.api.Portal;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.biomart.processors.ProcessorRegistry;
import org.biomart.processors.TSV;

/**
 * @author Anthony Cros
 * class to test sequences
 * arguments are self-explanatory
 * scp -r ~/workspace/biomart-java/ $BT:~/JIRA/DCCTEST-1198/
 * scp -r ~/workspace/biomart-java/ acros@dcc-hn.hpc:~/JIRA/DCCTEST-1198/
 * scp -r ~/workspace/biomart-java/bin/ $BT:~/JIRA/DCCTEST-1198/biomart-java/
 * scp -r ~/workspace/biomart-java/bin/org/biomart/processors/sequence/SequenceMain.class $BT:~/JIRA/DCCTEST-1198/biomart-java/bin/org/biomart/processors/sequence/
 * scp -r ~/workspace/biomart-java/bin/org/biomart/processors/sequence/SequenceMain.class acros@dcc-hn.hpc:~/JIRA/DCCTEST-1198/biomart-java/bin/org/biomart/processors/sequence/
 * java -Xmx4096m -Xms4096m -ea -cp `find $PWD/lib -type f -name '*.jar' | tr "\n" ":"`./bin org.biomart.processors.sequence.SequenceMain
 * java -Xmx4096m -Xms4096m -ea -cp `find $PWD/lib -type f -name '*.jar' | tr "\n" ":"`./bin org.biomart.processors.sequence.SequenceMain $HOME/sequence.bm8 usg 400 600 '' mmusculus ALL_CHROMOSOMES
 * java -Xmx1024m -Xms1024m -ea -cp `find $PWD/lib -type f -name '*.jar' | tr "\n" ":"`./bin org.biomart.processors.sequence.SequenceMain $HOME/sequence.bm8 usg 400 600 '' mmusculus ALL_CHROMOSOMES
 * 
 * scp -r ~/workspace/biomart-java/ biomart@bmdcc1.res:~/tmp/
 * scp -r ~/workspace/biomart-java/testdata/sequence.xml biomart@bmdcc1.res:~/tmp/biomart-java/testdata/sequence.xml
 * scp -r ~/workspace/biomart-java/bin/org/biomart/processors/sequence/SequenceMain.class biomart@bmdcc1.res:~/tmp/biomart-java/bin/org/biomart/processors/sequence/
 * bmdcc1$ cat ~/tmp/biomart-java/testdata/sequence.xml | awk '{gsub(/\|dcc_web\|/,"|bm_web|")}1' > ~/tmp/biomart-java/testdata/sequence.xml.tmp && mv ~/tmp/biomart-java/testdata/sequence.xml.tmp ~/tmp/biomart-java/testdata/sequence.xml
 * bmdcc1$ less ~/tmp/biomart-java/testdata/sequence.xml
 * 	+ change {@link org.biomart.processors.sequence.SequenceMain.USE_DCC_WEB_MYSQL_USER}
 * bmdcc1$ cd ~/tmp/biomart-java && java -cp "`DIR=./lib && find ${DIR?} -name '*.jar' | tr "\n" ":"`.:./bin" org.biomart.processors.sequence.SequenceMain
 */
public class SequenceMain {
	
	public static final boolean USE_CENTRAL_PORTAL = false;
	public static final boolean useAttributeListDirectly = false;	// debug only
	public static final int ENSEMBL_VERSION = 61;
	public static final String DATABASE_NAME = 
		"dcc-qa.oicr.on.ca"
		//"dcc-qa-db.oicr.on.ca"
	;
	public static final boolean USE_DCC_WEB_MYSQL_USER = true;
	public static final boolean TURN_ON_TIMING = false;
	
	public static String ALL_CHROMOSOMES = "ALL_CHROMOSOMES";	// [cBM1104250948]
    static {
        System.setProperty("biomart.debug", "true");
        System.setProperty("log.level", "debug");
    }

    public static void main(String[] args) throws Exception {
    	
    	//Log.configure(Settings.MARTCONFIGURATOR, new File(new File(System.getProperty("user.home"), ".biomart"), Settings.MARTCONFIGURATOR));
    	//Log.configure(app, appDir);
    	
    	String outputFile = null;
    	String operation = null;
    	Integer upstreamFlank = null;
    	Integer downstreamFlank = null;
    	String ids = null;
    	String species = null;		// like hsapiens, mmusculus, ...
    	String chromosome = null;
    	
    	if (args.length==0) {
    		outputFile = System.getProperty("user.home") + "/" + "sequence.bm8";
	    	operation = "usg";
	    	upstreamFlank = 400;
	    	downstreamFlank = 600;
	    	ids = //"ENSG00000240269";//"ENSG00000249665,ENSG00000161249";//ENSG00000229571,ENSG00000088854,"; //"ENST00000511002,ENST00000373345,";//ENSMUST00000169191"; //;
	    	species = "hsapiens";//"hsapiens";//"dmelanogaster";	//"mmusculus";
	    	chromosome = "22";//ALL_CHROMOSOMES;//"20";	//"";//YHet";//ALL_CHROMOSOMES;
    	} else {
    		outputFile = args[0];
	    	operation = args[1];
	    	upstreamFlank = Integer.valueOf(args[2]);
	    	downstreamFlank = Integer.valueOf(args[3]);
	    	ids = args[4];
	    	species = args[5];
	    	chromosome = args.length==6 ? "" : args[6];
    	}
    	
		process(outputFile, operation, upstreamFlank, downstreamFlank, ids, species, chromosome);
	}

	private static String getCode(String operation) {
		String code = null;
		if ("usg".equals(operation)) {
			code = SequenceConstants.QueryType.GENE_EXON_INTRON.getCode();
		} else if ("ust".equals(operation)) {
			code = SequenceConstants.QueryType.TRANSCRIPT_EXON_INTRON.getCode();
		} else if ("flankg".equals(operation)) {
			code = SequenceConstants.QueryType.GENE_FLANK.getCode();
		} else if ("flankt".equals(operation)) {
			code = SequenceConstants.QueryType.TRANSCRIPT_FLANK.getCode();
		} else if ("fcrg".equals(operation)) {
			code = SequenceConstants.QueryType.CODING_GENE_FLANK.getCode();
		} else if ("fcrt".equals(operation)) {
			code = SequenceConstants.QueryType.CODING_TRANSCRIPT_FLANK.getCode();
		} else if ("5utr".equals(operation)) {
			code = SequenceConstants.QueryType.FIVE_UTR.getCode();
		} else if ("3utr".equals(operation)) {
			code = SequenceConstants.QueryType.THREE_UTR.getCode();
		} else if ("exon".equals(operation)) {
			code = SequenceConstants.QueryType.GENE_EXON.getCode();	//TODO rename to transcript exon?
		} else if ("cdna".equals(operation)) {
			code = SequenceConstants.QueryType.CDNA.getCode();
		} else if ("coding".equals(operation)) {
			code = SequenceConstants.QueryType.CODING.getCode();
		} else if ("prot".equals(operation)) {
			code = SequenceConstants.QueryType.PEPTIDE.getCode();
		} else assert false;
		return code;
	}
	
	private static void process(String outputFile, String operation, int upstreamFlank, int downstreamFlank, String ids, String species2, String chromosome) throws Exception {
		List<String> idList = ids==null || ids.isEmpty() || ids.equals("\"\"") ?
			new ArrayList<String>() : 
			new ArrayList<String>(Arrays.asList(ids.split(",")));
		
		String xmlQuery = buildXmlQuery(operation, upstreamFlank, downstreamFlank, idList, species2, chromosome);
		System.err.println("xmlQuery = " + xmlQuery);
    	
		Portal portal = initialize();

		long timestamp1 = printTimestamp(">");
		FileOutputStream seqOut = new FileOutputStream(new File(outputFile));
		portal.executeQuery(xmlQuery, seqOut);
		seqOut.close();
		long timestamp2 = printTimestamp("<");
		int timestampMin = (int)((timestamp2-timestamp1)/60000);
		int timestampSec = (int)((timestamp2-timestamp1)/1000);
		System.err.println("bm8\t" + timestampMin + "\t" + timestampSec);
		
		System.err.println(outputFile);
		System.err.println("manual exit");
		System.exit(0);
	}

	private static long printTimestamp(String prefix) {
		Date date = new Date();
		Long timestamp = Long.valueOf(new SimpleDateFormat("yyMMddHHmmss").format(date));
		System.err.println(prefix + timestamp);
		return date.getTime();
	}

	private static Portal initialize() {
		System.setProperty("org.biomart.baseDir", ".");
		
		System.setProperty("processors.sequence.connection", "jdbc:mysql://" +
				DATABASE_NAME + ":3306/sequence_mart_" + ENSEMBL_VERSION);
		System.setProperty("processors.sequence.username", 
				USE_DCC_WEB_MYSQL_USER ? "dcc_web" : "bm_web");
		System.setProperty("processors.sequence.password", "sgg32fde");

		Portal portal = null;
		try {
			String xmlFilePath = null;
			String keyFilePath = null;
			if (USE_CENTRAL_PORTAL) {
				xmlFilePath = "./registry/CentralPortal.xml";
				keyFilePath = "./registry/.CentralPortal";
			} else if (USE_DCC_WEB_MYSQL_USER) {
				xmlFilePath = "./testdata/sequence.xml";
				keyFilePath = "./testdata/.sequence";
			} else {
				xmlFilePath = "./testdata/sequence2.xml";
				keyFilePath = "./testdata/.sequence2";
			}
			
			MartRegistryFactory factory = new XmlMartRegistryFactory(xmlFilePath, keyFilePath);
			portal = new Portal(factory);
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception initializing registry: " + e.getMessage());
		}
		ProcessorRegistry.register("Sequence", Sequence.class);
		ProcessorRegistry.register("TSV", TSV.class);
		return portal;
	}

	private static String buildXmlQuery(String operation, int upstreamFlank, int downstreamFlank, List<String> idList, String species2, String chromosome) {
		
		String sequenceType = getCode(operation);
		Integer id_type = getIdType(sequenceType);
	
		System.err.println("id_type = " + id_type);
		
		String ids = idList.toString().replace(", ", ",").replace("[", "").replace("]", "");
		String filter = null;
		if (chromosome.isEmpty()) {
			filter = id_type>1 ? 
				"<Filter name=\"ensembl_transcript_id\" value=\"" + ids + "\"/>" : 
				"<Filter name=\"ensembl_gene_id\" value=\"" + ids + "\"/>";
		} else if (ALL_CHROMOSOMES.equals(chromosome)) {
			filter = "";
		} else {
			filter = "<Filter name=\"chromosome_name\" value=\"" + chromosome + "\"/>";
		}
		
		String configName = USE_CENTRAL_PORTAL ? "gene_ensembl_sequence" : "gene_ensembl_config";
		String xml = 
        	 "<!DOCTYPE Query>" +
        	 "<Query client=\"biomartclient\"  limit=\"-1\" header=\"0\">" +
        	 (useAttributeListDirectly ? "" :
        	 "<Processor name=\"sequence\">" +
	        	 "<Parameter name=\"type\" value=\"" + sequenceType + "\"/>" +
	        	 "<Parameter name=\"upstreamFlank\" value=\"" + upstreamFlank + "\"/>" +
	        	 "<Parameter name=\"downstreamFlank\" value=\"" + downstreamFlank + "\"/>" +
        	 "</Processor>") +
        	 "<Dataset name=\"" + species2 + "_gene_ensembl\" config=\"" + configName + "\">" +
        	    (useAttributeListDirectly ? "<Attribute name=\"" + sequenceType + "\" value=\"\"/>" : "") +
        	    filter +
             	"<Attribute name=\"ensembl_gene_id\"/>" +
	        	(id_type>1 ? "<Attribute name=\"ensembl_transcript_id\"/>" : "") +
	        	(id_type>2 ? "<Attribute name=\"ensembl_exon_id\"/>" : "") +
        	 "</Dataset>" +
        	 "</Query>";
		return xml;
	}

	private static Integer getIdType(String code) {
		Integer id_type = null;
		if (
				SequenceConstants.QueryType.GENE_EXON_INTRON.getCode().equals(code) ||
				SequenceConstants.QueryType.GENE_FLANK.getCode().equals(code) ||
				SequenceConstants.QueryType.CODING_GENE_FLANK.getCode().equals(code)
			) {
			id_type = 1;
		} else if (
			SequenceConstants.QueryType.TRANSCRIPT_EXON_INTRON.getCode().equals(code) ||
			SequenceConstants.QueryType.TRANSCRIPT_FLANK.getCode().equals(code) ||
			SequenceConstants.QueryType.CODING_TRANSCRIPT_FLANK.getCode().equals(code) ||
			SequenceConstants.QueryType.FIVE_UTR.getCode().equals(code) ||
			SequenceConstants.QueryType.THREE_UTR.getCode().equals(code) ||
			SequenceConstants.QueryType.CDNA.getCode().equals(code) ||
			SequenceConstants.QueryType.CODING.getCode().equals(code) ||
			SequenceConstants.QueryType.PEPTIDE.getCode().equals(code)
			) {
			id_type = 2;
		} else if (
			SequenceConstants.QueryType.GENE_EXON.getCode().equals(code)
			) {
			id_type = 3;
		} else assert false;
		return id_type;
	}
	
	public static String getTimestamp() {
		return new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date());
	}
}