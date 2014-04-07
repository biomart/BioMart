package org.biomart.queryEngine.queryPlanner;

import java.util.ArrayList;

import org.biomart.objects.objects.MartRegistry;
import org.biomart.queryEngine.Query;
import org.biomart.queryEngine.SubQuery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.biomart.common.exceptions.TechnicalException;
import org.biomart.processors.JGUtils;

/**
 *
 * @author Jonathan Guberman, Syed Haider
 * 
 * This object operates on query object and rearranges the subQuery objects
 * inorder to execute queries in a most optimal way. This was supported with
 * link indices initially, however, dynmaic link indices implementation has not
 * proven to be very fruitful. Nevertheless, basic reordering of Subqueries
 * is still needed. This will be based on one time (precomputed) link indices map
 *
 */
public class QueryPlanner {
	@SuppressWarnings("unused")
	private MartRegistry martRegistry = null;
	
    /**
     *
     * @param martRegistry
     */
    public QueryPlanner(MartRegistry martRegistry) {
		this.martRegistry = martRegistry;
	}
	
    /**
     *
     * @param query
     * @return
     */
    public Query addLinks(Query query){
		for(List<SubQuery> queryPlan : query.getQueryPlans()){
			
		}
		return query;
	}

    /**
     *
     * @param subQueryList
     * @return
     */
    public ArrayList<SubQuery> reorderQueriesUsingLinkIndices(ArrayList<SubQuery> subQueryList) {
		ArrayList<SubQuery> reOrderedSubQueryList = null;
		
		// Does nothing for now
		reOrderedSubQueryList = new ArrayList<SubQuery>(subQueryList);
		
		return reOrderedSubQueryList;
	}
	
	/* For postgres, just grab the first row of the "explain" results and parse for the "rows=" string to
	 * start, and whitespace to end */

	private enum DbType {POSTGRES, MYSQL,ORACLE};
	private static DbType db = DbType.POSTGRES;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Here we use table names instead of dataset names
		String[] dataSets = {
				"hsapiens_gene_ensembl__transcript__main",
				"mmusculus_gene_ensembl__homolog_Hsap__dm",
				"acarolinensis_gene_ensembl__homolog_Hsap__dm",
				"btaurus_gene_ensembl__homolog_Hsap__dm",
				"celegans_gene_ensembl__homolog_Hsap__dm",
				"cfamiliaris_gene_ensembl__homolog_Hsap__dm",
				"cintestinalis_gene_ensembl__homolog_Hsap__dm",
				"dmelanogaster_gene_ensembl__homolog_Hsap__dm",
				"dnovemcinctus_gene_ensembl__homolog_Hsap__dm",
				"rnorvegicus_gene_ensembl__homolog_Hsap__dm"
		};
		// A link is an (unordered) pair of "dataset" (table) and key, represented for now as an array of Strings.
		String[][] manualKeys = {
				{dataSets[0],"stable_id_1023"},
				{dataSets[1],"stable_id_4016_r2"},
				{dataSets[2],"stable_id_4016_r2"},
				{dataSets[3],"stable_id_4016_r2"},
				{dataSets[4],"stable_id_4016_r2"},
				{dataSets[5],"stable_id_4016_r2"},
				{dataSets[6],"stable_id_4016_r2"},
				{dataSets[7],"stable_id_4016_r2"},
				{dataSets[8],"stable_id_4016_r2"},
				{dataSets[9],"stable_id_4016_r2"},
		};
		String[][][] manualLinks = {
				{manualKeys[0],manualKeys[1]},
				{manualKeys[0],manualKeys[2]},
				{manualKeys[0],manualKeys[3]},
				{manualKeys[0],manualKeys[4]},
				{manualKeys[0],manualKeys[5]},
				{manualKeys[0],manualKeys[6]},
				{manualKeys[0],manualKeys[7]},
				{manualKeys[0],manualKeys[8]},
				{manualKeys[0],manualKeys[9]},				
		};
		// Here, a filter is a "dataset" (table), followed by a string containing property, operation, value
		String[][] filters = {
				{dataSets[2],"chr_start_4016_r2","<","100"},
				{dataSets[1],"chr_end_4016_r2","<","20000"},
				{dataSets[2],"chr_end_4016_r2","<","10000"},
		};
		// --------- END DATA SET UP

		Boolean linkIndicesExists = false;
		int maxSize;
		if (linkIndicesExists){
			//Use linkIndices to set maxSize to the size of the query's index
		}

		// Filters, grouped by their "dataset"
		// Will be either HashMap<Dataset, HashSet<Filter>> or, more likely part of the object model
		HashMap<String, HashSet<String[]>> filterMap = new HashMap<String, HashSet<String[]>>();

		// Populate filter by dataset
		for(String dataset : dataSets){
			filterMap.put(dataset, new HashSet<String[]>());
		}
		for(String[] filter : filters){
			HashSet<String[]> filterSet = filterMap.get(filter[0]);
			/*if(filterSet == null){
				filterSet = new HashSet<String[]>();
			}*/
			filterSet.add(filter);
			filterMap.put(filter[0], filterSet);
		}

		// For each dataset involved in a link, store the set of destinations on each side of the link
		// Will be HashMap<Dataset, HashSet<Dataset>>
		HashMap<String,HashSet<String>> linkPartners = new HashMap<String, HashSet<String>>();
		for(String[][] link: manualLinks){
			HashSet<String> partners0 = linkPartners.get(link[0][0]);
			if(partners0==null){
				partners0 = new HashSet<String>();
			}
			partners0.add(link[1][0]);
			linkPartners.put(link[0][0], partners0);

			HashSet<String> partners1 = linkPartners.get(link[1][0]);
			if(partners1==null){
				partners1 = new HashSet<String>();
			}
			partners1.add(link[0][0]);
			linkPartners.put(link[1][0], partners1);
		}

		// For each link involved, store other links that connect to it
		// Will be HashMap<Link, HashSet<Link>>
		HashMap<String[][],HashSet<String[][]>> linkLinkPartners = new HashMap<String[][], HashSet<String[][]>>();
		for(String[][] keyLink: manualLinks){
			for(String[][] partnerLink: manualLinks){
				if(keyLink!=partnerLink){
					if(connectLink(keyLink,partnerLink)){
						HashSet<String[][]> joined = linkLinkPartners.get(keyLink);
						if(joined == null){
							joined = new HashSet<String[][]>();
						}
						joined.add(partnerLink);
						linkLinkPartners.put(keyLink,joined);
					}
				}
			}
		}

		try{
			connectDB();
			connectIndex();
			// Store the score for each dataset
			// Will be HashMap<Dataset,Integer>
			HashMap<String, Integer> expectedRows = new HashMap<String, Integer>();

			/*String test = runQuery("EXPLAIN SELECT * FROM dnovemcinctus_gene_ensembl__homolog_Hsap__dm;");
			System.out.println(test);*/
			for(String dataset : filterMap.keySet()){
				StringBuilder querySQL = new StringBuilder();
				querySQL.append("EXPLAIN SELECT * FROM " + dataset);
				if(filterMap.get(dataset).size() > 0){
					querySQL.append(" WHERE ");
					for(String[] filter : filterMap.get(dataset)){
						querySQL.append(filter[1]+filter[2]+filter[3] + " AND ");
					}
					querySQL.delete(querySQL.length()-4, querySQL.length());
				}
				querySQL.append(";");
				//System.out.println(querySQL);
				Integer result = runQuery(querySQL.toString());
				//System.out.println(result);
				expectedRows.put(dataset,result);
			}

			// Construct map of links to scores
			// Will be Map<Links, Integer>
			LinkedHashMap<String[][], Integer> linkScore = new LinkedHashMap<String[][], Integer>();
			for(String[][] link: manualLinks){
				linkScore.put(link, scoreLink(link,expectedRows));
			}

			// Sort output by the score
			linkScore = JGUtils.sortByValue(linkScore);

			// Retrieve the link with lowest score
			String[][] firstLink = linkScore.keySet().iterator().next();
			LinkedList<String[][]> finalOrder = new LinkedList<String[][]>();
			finalOrder.add(firstLink);

			// Construct the list of possible link partners, and their scores, for the first dataset
			LinkedHashMap<String[][], Integer> possibleLinks = new LinkedHashMap<String[][], Integer>();
			for(String[][] link:linkLinkPartners.get(finalOrder.getLast())){
				possibleLinks.put(link, linkScore.get(link));
			}
			// Sort the possibleLinks by score
			possibleLinks = JGUtils.sortByValue(possibleLinks);

			while(possibleLinks.size() > 0){
				// Retrieve the lowest-scoring possible list of the current set
				finalOrder.add(possibleLinks.keySet().iterator().next());
				// Construct the  list of possible link partners for the last added dataset
				for(String[][] link:linkLinkPartners.get(finalOrder.getLast())){
					possibleLinks.put(link, linkScore.get(link));
				}
				// Remove the already-accounted for items from the possibleLinks
				for(String[][] link: finalOrder){
					possibleLinks.remove(link);
				}
				// Sort the possibleLinks by score
				possibleLinks = JGUtils.sortByValue(possibleLinks);
			}

			System.out.println("--");
			/*// Test print
			for(String partner:possibleLinks.keySet()){
				System.out.println(partner);
			}*/
			for(String[][] link: finalOrder){
				System.out.println(link[0][0] + " " + link[1][0]);
			}


		}catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (databaseConnection != null){
				disconnectDB();
				disconnectIndex();
			}
		}
	}
	private static boolean connectLink(String[][] linkA,
			String[][] linkB) {
		// Determine is two links "connect" to one another"
		if((linkA[0][0] == null ? linkB[0][0] == null : linkA[0][0].equals(linkB[0][0])) || (linkA[0][0] == null ? linkB[1][0] == null : linkA[0][0].equals(linkB[1][0])) || (linkA[1][0] == null ? linkB[0][0] == null : linkA[1][0].equals(linkB[0][0])) || (linkA[1][0] == null ? linkB[1][0] == null : linkA[1][0].equals(linkB[1][0]))){
			return true;
		}
		return false;
	}
	static Integer scoreLink(String[][] link, HashMap<String, Integer> expectedRows){
		// Given a link and the set of expected number of rows, calculate an expectation number for the results of the join
		float score = Math.min(expectedRows.get(link[0][0]), expectedRows.get(link[1][0]));
		String[][][] wrapper = {link};
		String[][][] leftSide = {{link[0], link[0]}};
		String[][][] rightSide = {{link[1], link[1]}};
		/*try {
			Integer joinSize = runIndexQuery(LinkIndices.queryIndexCount(wrapper));
			float leftSize = joinSize/(float)runIndexQuery(LinkIndices.queryIndexCount(leftSide));
			float rightSize = joinSize/(float)runIndexQuery(LinkIndices.queryIndexCount(rightSide));
			score = Math.min(leftSize*expectedRows.get(link[0][0]), rightSize*expectedRows.get(link[1][0]));
		} catch (TechnicalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         */
		return (int) score;
	}
	private static Connection databaseConnection = null;
	private static Connection indexConnection = null;
	static void connectDB(){
		String URL;
		String username;
		String password;
		switch(db){
		case MYSQL:
			// Login information for the sequence database
			// URL format: jdbc:mysql://server[:port]/[databasename]


			URL = "jdbc:mysql://bm-test.res.oicr.on.ca/jg_fullindex_test";
			username = "martadmin";
			password = "biomart";

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
			break;
		case POSTGRES:
			// Login information for the sequence database
			// URL format: jdbc:mysql://server[:port]/[databasename]


			URL = "jdbc:postgresql://bm-test.res.oicr.on.ca:5432/jg_qptest";
			username = "martadmin";
			password = "biomart";

			try {
				DriverManager.registerDriver (new org.postgresql.Driver());
			} catch (Exception e) {
				System.err.println("Failed to load JDBC/ODBC driver.");
			}

			try {
				databaseConnection = DriverManager.getConnection (URL,username,password);
			} catch (Exception e) {
				System.err.println("problems connecting to "+URL);
			}
			break;
		} 
	}
	static void connectIndex(){
		String URL;
		String username;
		String password;
		// Login information for the sequence database
		// URL format: jdbc:mysql://server[:port]/[databasename]


		URL = "jdbc:mysql://bm-test.res.oicr.on.ca/jg_fullindex_test";
		username = "martadmin";
		password = "biomart";

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			System.err.println("Failed to load JDBC/ODBC driver.");
		}

		try {
			indexConnection = DriverManager.getConnection (URL,username,password);
		} catch (Exception e) {
			System.err.println("problems connecting to "+URL);
		}
	}

	static void disconnectDB(){
		try {
			databaseConnection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static void disconnectIndex(){
		try {
			indexConnection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static Integer runQuery(String sqlQuery) throws TechnicalException{
		switch(db){
		case MYSQL:
			try {
				Statement stmt = null;
				stmt = databaseConnection.createStatement();

				ResultSet result = stmt.executeQuery(sqlQuery);
				result.next();
				return result.getInt("rows");
				//TODO handle multi-row results

			} catch (Exception e){
				e.printStackTrace();
				return null;
			}

		case POSTGRES:
			try {
				Statement stmt = null;
				stmt = databaseConnection.createStatement();

				ResultSet result = stmt.executeQuery(sqlQuery);
				result.next();
				String findRows = result.getString(1);
				int parseStart = findRows.lastIndexOf("rows=");
				int parseEnd = findRows.indexOf(" ", parseStart);
				return Integer.parseInt( findRows.substring(parseStart+5, parseEnd) );
				//TODO handle multi-row results

			} catch (Exception e){
				e.printStackTrace();
				return null;
			}	
		}
		return null;
	}
	static Integer runIndexQuery(String sqlQuery) throws TechnicalException{
		try {
			Statement stmt = null;
			stmt = indexConnection.createStatement();

			ResultSet result = stmt.executeQuery(sqlQuery);
			result.next();
			return result.getInt("rows");
			//TODO handle multi-row results

		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
    /**
     *
     * @param query
     */
    public void planQuery(Query query){
		List<List<SubQuery>> queryPlans = query.getQueryPlans();
		for(List<SubQuery> queryPlan : queryPlans){
			
		}
	}

}
