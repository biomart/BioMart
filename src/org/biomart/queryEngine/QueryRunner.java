package org.biomart.queryEngine;

import org.biomart.common.exceptions.BioMartQueryException;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.EOFException;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.biomart.common.constants.OutputConstants;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Attribute;
import org.biomart.common.resources.Log;
import org.biomart.util.String2LongSet;


/**
 *
 * @author Syed Haider
 *
 * Takes in a query object as input. Launches independent threads (QueryRunnerThread)
 * for each chain of SubQuery objects. The synchronised output stream sharing between
 * various QueryRunnerThread objects is also handled here.
 * All DB platform specific drivers are also loaded here.
 * Attribute reordering and Pseudo attribute only query are handled here.
 */
public final class QueryRunner implements OutputConstants {
    public static final int BATCH_SIZE = 5000;

    public final Query query;
    public final int [] outputOrder;
    public final String2LongSet uniqueResults = new String2LongSet();

    public Function<String[],Boolean> callback;
    public Function<String,Boolean> errorHandler;

    public final ArrayList<ArrayList<String>> finalRT = new ArrayList<ArrayList<String>>();

    private int numRowsRemaining = -1;

    boolean isDone = false;
    protected static final ScheduledThreadPoolExecutor executor;

	static {
		int poolSize = 100;

		ThreadFactory tf = new ThreadFactoryBuilder()
			.setDaemon(false)
			.setNameFormat("Job-%d")
			.setThreadFactory(Executors.defaultThreadFactory())
			.build();

		executor = new ScheduledThreadPoolExecutor(poolSize, tf, new ThreadPoolExecutor.AbortPolicy());
	}

    protected static final CompletionService<Boolean> COMP_SERVICE = new ExecutorCompletionService<Boolean>(executor);

    private boolean hasError = false; // this will be true when one ore more queries fail
	private final boolean isCountQuery;

    /**
     *
     * @param query
     * @throws SQLException
     * @throws TechnicalException
     */
    public QueryRunner(Query query, Function callback, Function errorHandler, boolean isCountQuery) throws SQLException, TechnicalException {
        this.query = query;
        this.numRowsRemaining = query.limit;
        this.callback = callback;
        this.errorHandler = errorHandler;
		this.isCountQuery = isCountQuery;

        this.outputOrder = query.outputOrder;

        try {
        	DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            DriverManager.registerDriver(new org.postgresql.Driver());
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
        } catch (SQLException e) {
            throw new TechnicalException(e);
        }
    }

    /**
     *
     * @throws IOException
     */
    public void printHeader() throws IOException {
		// Don't print header for count queries
        if (!isCountQuery && query.hasHeader())
            callback.apply(query.outputDisplayNames);
    }

    //TODO: Note: for special datasets, you launch them all at the begining,
    /**
     *
     * @throws TechnicalException
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     */
    public void runQuery() throws TechnicalException, SQLException, IOException, InterruptedException {
        Collection<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
        // print Header if its set
        this.printHeader();

        // if ONLY pseudos in the query, just print them and return
        if (this.pseudoOnlyQuery()) {
            return;
        }

//        this.outputHandle.flush();

        // lets first launch all the importable only subQueries
        // note, only one row on union is enough as importable only subqueries will be share across
        // all the union rows
        Iterator it_1 = this.query.queryPlanMap.entrySet().iterator();
        Map.Entry pair_1 = (Map.Entry) it_1.next();
        List <SubQuery> subQueries = this.query.queryPlanMap.get(pair_1.getKey().toString());
        for(SubQuery subQuery : subQueries) {
            ArrayList<SubQuery> impOnly_sqObjs = subQuery.getImportableOnlySubQueries();
            // remove duplicated entries in sub query list
            HashSet hs = new HashSet();
            hs.addAll(impOnly_sqObjs);
            impOnly_sqObjs.clear();
            impOnly_sqObjs.addAll(hs);
            
            for(SubQuery impOnly_subQuery : impOnly_sqObjs){
                Log.debug("EXECUTING (Importable Only SubQuery: "+ impOnly_subQuery.getDataset().getName() +" ) of dataset : " + subQuery.getDataset().getName());
                impOnly_subQuery.caching = true;
                impOnly_subQuery.executeQuery();
            }
        }

        for (Entry<String,List<SubQuery>> pair : query.queryPlanMap.entrySet()) {
            Log.debug("KEY: " + pair.getKey().toString());

            // submit will return a SoftReference<Future<Boolean>> object
            tasks.add(COMP_SERVICE.submit(
                new QueryRunnerThread(
                    this,
                    query.queryPlanMap.get(pair.getKey()),
                    BATCH_SIZE,
                    pair.getKey().toString()
                )
            ));
        }

        try {
            // Go through all future objects, which will block until a result is returned
            for (Future<Boolean> task : tasks) {
                boolean success = task.get();
                this.hasError = this.hasError || !success;
            }
        } catch (ExecutionException e) {
        	Log.error("Error during runQuery", e);
            throw new BioMartQueryException(e.getMessage(), e);
        } finally {
            new Thread() {
                public void run() {
                    // lets kill connection for all subqueries
                    try {
                        Iterator it_conn = query.queryPlanMap.entrySet().iterator();
                        while(it_conn.hasNext()) {
                            Map.Entry pair = (Map.Entry) it_conn.next();
                            List<SubQuery> subQueries_conn = query.queryPlanMap.get(pair.getKey().toString());
                            for(SubQuery subQuery_conn : subQueries_conn) {
                                subQuery_conn.closeDBConnection();

                                 ArrayList<SubQuery> impOnlySubQueries = subQuery_conn.getImportableOnlySubQueries();
                                 for(SubQuery impOnly_sq: impOnlySubQueries) {
                                     impOnly_sq.closeDBConnection();
                                 }

                            }
                        }
                        for(String key1 : query.queryPlanMap.keySet()){
                            List<SubQuery> sqList = query.queryPlanMap.get(key1);
                            for(SubQuery sq : sqList){
                                for(int tablePartition = 0; tablePartition <  sq.resultSet.size(); tablePartition++) {
                                    if(sq.resultSet.get(tablePartition) != null)
                                        sq.resultSet.get(tablePartition).close();
                                }
                            }
                        }
                        for(Dataset ds : query.getImportableOnlySubqueries().keySet()){
                            SubQuery sq = query.getImportableOnlySubqueries().get(ds);
                            for(int tablePartition = 0; tablePartition <  sq.resultSet.size(); tablePartition++) {
                                if(sq.resultSet.get(tablePartition) != null)
                                    sq.resultSet.get(tablePartition).close();
                            }
                        }
                        for(Dataset ds : query.getDatasetSubqueries().keySet()){
                            Set<SubQuery> sqSet = query.getDatasetSubqueries().get(ds);
                            for(SubQuery sq: sqSet){
                                for(int tablePartition = 0; tablePartition <  sq.resultSet.size(); tablePartition++) {
                                    if(sq.resultSet.get(tablePartition) != null)
                                        sq.resultSet.get(tablePartition).close();
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        // Ignore. The user gets his results anyway, even though
                        // we should be able to properly close all resources.
                    }
                }
            }.start();

            if (this.hasError) {
                errorHandler.apply(ERROR_STRING);
            }
        }
    }


    /**
     *
     * @param interimRT
     * @param threadName
     * @throws IOException
     */
    public synchronized void printResults(List<List<String>> interimRT, String threadName) throws IOException {
        int rows = interimRT.size();

        int cols = isCountQuery ? 1 : this.query.outputOrder.length;
		
        // int unions = this.query.queryPlanMap.size();
        List<String> res_row = new ArrayList<String>();

        int j=0;
        if (!isDone) {
            for (int i = 0; i < rows; i++) {
                res_row = interimRT.get(i);

                String[] curr_row = new String[cols];

				if (isCountQuery) {
					curr_row[0] = res_row.get(0) == null ? "" : res_row.get(0);
				} else {
					//Log.debug("output atts length: " +this.outputOrder.length);
					for (j = 0; j < cols; j++) {
						// its a pseudo att
						if (this.outputOrder[j] < 0)
							curr_row[j] = this.query.getPseudoAttributes().get(this.outputOrder[j]+1000).getPseudoAttributeValue(threadName) == null
								? "" : this.query.getPseudoAttributes().get(this.outputOrder[j]+1000).getPseudoAttributeValue(threadName);
						else
							curr_row[j] = res_row.get(this.outputOrder[j]) == null ? "" : res_row.get(this.outputOrder[j]);
					}
				}

                String row = Joiner.on('\t').join(curr_row);
                byte[] bytes = row.getBytes(); // For tracking uniqueness

                // If the row is unique then apply it to the callback function
                // If callback returns true then we're done
                // If limit is reached then we're also done
                if (!this.uniqueResults.contains(bytes)) {
                    try {
                        isDone = callback.apply(curr_row);
                    } catch (BioMartQueryException e) {
                        if (e.getCause() instanceof EOFException) {
                            isDone = true; // client closed connection... stop querying
                        } else {
                            isDone = true;
                            throw e;
                        }
                    }

                    // Log.debug("row: "+out_row.toString());
                    this.uniqueResults.add(bytes);

                    if (!isDone) {
                        isDone = --this.numRowsRemaining == 0;
                    }
                }

                if (isDone) {
                    break;
                }
            }
        }
    }

     /**
     * @return Number of remaining rows that need to be retrieved, or -1 if no query numRowsRemaining was set.
     */
    public synchronized boolean isDone() {
        return this.isDone;
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public boolean pseudoOnlyQuery() throws IOException {
        boolean pseudo_flag = true;
        Iterator it_1 = this.query.queryPlanMap.entrySet().iterator();
        Map.Entry pair1 = (Map.Entry) it_1.next();
        List <SubQuery> subQueries = this.query.queryPlanMap.get(pair1.getKey().toString());
        for(SubQuery subQuery : subQueries) {
            for(QueryElement attribute : subQuery.getQueryAttributeList()){
                if(attribute.getType() == QueryElementType.ATTRIBUTE){
                    Attribute attObj =  (Attribute) attribute.getElement();
                    if(attObj.getValue().length() == 0){
                        pseudo_flag = false;
                        continue;
                    }
                }
            }
        }

       if(pseudo_flag) {
           Iterator it = this.query.queryPlanMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                Log.debug("KEY: " + pair.getKey().toString());
                String threadName = pair.getKey().toString();
                int cols = this.query.outputOrder.length;
                String[] row = new String[cols];

                for (int j=0; j < cols; j++) {
                    // its a pseudo att
                    if (this.outputOrder[j] < 0) {
                        row[j] = (this.query.getPseudoAttributes().get(this.outputOrder[j]+1000).getPseudoAttributeValue(threadName) == null)
                                ? "" : this.query.getPseudoAttributes().get(this.outputOrder[j]+1000).getPseudoAttributeValue(threadName) +"\t";
                    }
                }
                callback.apply(row);
            }

           return true;
        }

        return false;
    }
}
