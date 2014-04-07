package org.biomart.queryEngine;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.resources.Log;
import org.biomart.objects.objects.Filter;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 *
 * @author Syed Haider
 *
 * This object represents each individual chain of Subqueries connected through links.
 * A Query object is composed of multiple Subqueries chained together (horizontally).
 * A Query object can have 1 or more such chains as explained above, in a vertical order.
 * All vertical chains are independent of each other, hence end up getting executed
 * independently. Each of these chains are called QueryRunnerThread. A QueryRunnerThread
 * is executed independently as a Java thread without having any knowledge of other threads.
 * However, all threads write to the same output handle in a synchronised fashion
 * using state-of-the-art synchronised functions and is no different to Volatile variables
 * in low level languages.
 * Each thread can be perceived as Union of results.
 * Components (Subquery) objects within a thread give rise to intersection of results.
 */
public final class QueryRunnerThread implements Callable {
    /**
     *
     */
    public final int batchSize;
    /**
     *
     */
    public final List<SubQuery> sub_queries;
    /**
     *
     */
    public QueryRunner qRunnerObj;

    private final String name;
    private boolean success = false;

    /**
     *
     * @param qRunnerObj
     * @param sub_queries
     * @param batchSize
     * @param key
     */
    public QueryRunnerThread(QueryRunner qRunnerObj, List<SubQuery> sub_queries,
             int batchSize, String key) {
        this.batchSize = batchSize;
        this.sub_queries = sub_queries;
        this.qRunnerObj = qRunnerObj;
        this.name = key;
    }

    @Override
    public Boolean call() {
        Log.debug("Launching Thread run() method: " + this.name);
		try {
            this.recurseSubQueries(this.sub_queries, 0, this.sub_queries.size(), null);
            this.success = true;
        } catch (IOException ie) {
            if (ie instanceof EOFException) throw ie;
            else ie.printStackTrace();
        } catch (SQLException se) {
            se.printStackTrace();
        } catch (TechnicalException te) {
            te.printStackTrace();
        } catch (Exception e){
        	e.printStackTrace();
        }
        finally {
            this.qRunnerObj = null;
            return this.success;
        }
    }


    /**
     *
     * @param sub_queries
     * @param current_sq
     * @param total_sq
     * @param interimRT
     * @throws IOException
     * @throws SQLException
     * @throws TechnicalException
     */
    @SuppressWarnings("unchecked")
	public void recurseSubQueries(List<SubQuery> sub_queries, int current_sq, int total_sq,
        List<List<String>> interimRT) throws IOException, SQLException, TechnicalException {

        final SubQuery sqObj = sub_queries.get(current_sq++);
        int exp_position = 0;
        QueryElement importable = new QueryElement();
        try {
            // UPDATE THE QUERY IF NEEDED
            // for the first sub-query, it can have importable only subqueries
            // like bands etc, but interimRT would be NULL
            // however for second query onwards, if interimRT == NULL, no need to
            // to process anything downstream
            // ideally, the following piece of code should be run just once
            // and then cached rather time working out imp/exp logic
            Log.debug("UPDATING subquery..."+ (current_sq-1));
            if ((current_sq <= 1) || ((current_sq > 1) && (interimRT != null))) {
                List<QueryElement> importables = sqObj.getImportables();
                Iterator it_imps = importables.iterator();
                Log.debug("Importables size: "+sqObj.getImportables().size());
                while (it_imps.hasNext()) {
                    QueryElement imp = (QueryElement) it_imps.next();
                    //imp_name = imp
                    String exporting_ds = imp.getLinkDataset().getName();
                    String importing_ds = sqObj.getDataset().getName();

                    Log.debug("importing dataset: "+importing_ds);

                    // FIRSTLY lets first find the importable only subqueries eg. bands etc
                    ArrayList<SubQuery> impOnly_sqObjs = sqObj.getImportableOnlySubQueries();
                    Iterator it_impOnly_sqObjs = impOnly_sqObjs.iterator();
                    while (it_impOnly_sqObjs.hasNext()) {
                        SubQuery impOnly_sqObj = (SubQuery) it_impOnly_sqObjs.next();
                        Log.debug("importable only subQuery dataset: "+impOnly_sqObj.getDataset().getName());
                        ArrayList<QueryElement> exportables = impOnly_sqObj.getExportables();
                        Iterator it_exps = exportables.iterator();
                        while (it_exps.hasNext()) {
                            QueryElement exp = (QueryElement) it_exps.next();
                            Log.debug("importable only subQuery's exportable dataset: "+exp.getLinkDataset().getName());
                            Log.debug("exportable name: " + exp.getPortable().getName());
                            Log.debug("importable name: " + imp.getPortable().getName());
                            if (exp.getLinkDataset().getName().equals(importing_ds) && exp.getPortable().getName().equals(imp.getPortable().getName())) {
                                // retrieve resultsTable. Under assumption that all such
                                // importable only subqueries are executed right at the begining of
                                // query running
                                // all results, no batching,
                                Log.debug("UPDATING with 'Importable ONLY' subqueries...");
                                sqObj.updateQuery(impOnly_sqObj.getResults(1, 999999999), exp.getPortablePosition(), imp, true);
                            }
                        }
                    }
                    // SECONDLY lets find the subquery in usual chain that exports to this subquery
                    // only for second query onwards
                    if (current_sq > 1) {
                        Log.debug("UPDATING regular join subqueries...");
                        Integer exportable_position = this.getExportablePositionInResults(exporting_ds, sqObj.getDataset().getName(), current_sq - 1);
                        if (exportable_position != null) {
                            sqObj.updateQuery(interimRT, exportable_position, imp, false);
                            // keep them safe, will need during merging
                            exp_position = exportable_position;
                            importable = imp;
                        }
                    }
                }
            }
            // only execute if link index find some overlap
            if (!sqObj.isEmptyBatch()) {
                // EXECUTE QUERY
                Log.debug("EXECUTING...");
                sqObj.executeQuery(current_sq-1);
            }
            else {
                // DO NOT EXECUTE QUERY
                Log.debug("NOT EXECUTING [instructed by LinkIndices]...");
                // reset link index for next round
                sqObj.setEmptyBatch(false);
                return;
            }
        } catch (TechnicalException ex) {
            Logger.getLogger(QueryRunnerThread.class.getName()).log(Level.SEVERE, null, ex);
        }

        int batchStart = 1;
        int batchEnd = this.batchSize;

        // GO OVER RESULTS IN BATCHES
        while (true) {
            Log.debug("GET RESULTS - DS: " + sqObj.getDataset().getName() + " (Start: " + batchStart + " End: " + batchEnd + ") " + new Timestamp((new java.util.Date()).getTime()));
            List<List<String>> retRT = new ArrayList<List<String>>();
            List<List<String>> tempRT = sqObj.getResults(batchStart, batchEnd);
            int r_size = tempRT.size();

            // DO MERGING, UPDATE RESULTS TABLE
            Log.debug("DO MERGING [start] " + new Timestamp((new java.util.Date()).getTime()));
            retRT = this.mergeResults(interimRT, tempRT, exp_position, importable);
            Log.debug("DO MERGING [end] " + new Timestamp((new java.util.Date()).getTime()));

            if (current_sq < total_sq) {
                this.recurseSubQueries(sub_queries, current_sq, total_sq, retRT);
            }

            // LETS PRINTOUT FINAL RESULTS IF ITS LAST DATASET
             Log.debug("PRINTING RESULTS");
            if (current_sq == total_sq) {
                this.qRunnerObj.printResults(retRT, this.name);
                retRT = null;
            }

            // what happens for empty batches ? should work i guess as it only returns empty rows, but there are rows!
            if (r_size < this.batchSize || this.qRunnerObj.isDone()) {
                break;
            }

            batchStart += this.batchSize;
            batchEnd += this.batchSize;
        }
        sqObj.cleanUp();
    }

    /**
     *
     * @param interimRT
     * @param tempRT
     * @param exp_position
     * @param importable
     * @return
     */
    public List<List<String>> mergeResults(List<List<String>> interimRT, List<List<String>> tempRT,
        int exp_position, QueryElement importable) {

        List<List<String>> retRT = new ArrayList<List<String>>();
        if (interimRT == null) {
            retRT = tempRT;
        } else {
            int imp_position = importable.getPortablePosition();
            int length = importable.getElementList().size();
            int clean_flag = 1;
            int init_loop = 0;
            OperatorType [] qualifier = new OperatorType[length];
            for (int x = 0 ; x < length; x++) {
                Filter filter = (Filter) importable.getElementList().get(x);
                qualifier[x] = filter.getQualifier();
            }

            List<String> temp_row1 = new ArrayList<String>();
            List<String> temp_row2 = new ArrayList<String>();

            // Iterator only for forward traversal, listIterator for two way traversal + modifications
            int interim_size = interimRT.size()-1;
            int temp_size = tempRT.size()-1;
            Log.debug("JOIN A with B where A=" + (interim_size+1) + " B=" + (temp_size+1));
            int temp_row1_size = 0;
            int temp_row2_size = 0;
            if(interim_size >= 0)
                temp_row1_size = interimRT.get(0).size();
            if(temp_size >= 0)
                temp_row2_size = tempRT.get(0).size();

            // SUPER FAST MERGING, only applicable if exportable/importable list is of size 1
            // and operator type is =
            if (length == 1 && qualifier[0].equals(OperatorType.E)) {
                // lets create a look up table for the tempRT
                // two separate hashes for fastest response since HASHSET is faster lookup only
                HashMap <String, List<Integer>> tempRTLookUps = new HashMap<String, List<Integer>>();
                Set<String> alreadySeen = new HashSet<String>();
                for(int j = 0; j <= temp_size ; j++) {
                    String imp_value = tempRT.get(j).get(imp_position);
                    if(imp_value != null) {
                        imp_value = imp_value.toLowerCase();
                        if (alreadySeen.contains(imp_value)) {
                            tempRTLookUps.get(imp_value).add(j);
                            Log.debug("multiple rows with same imp value: "+ imp_value);
                        }
                        else {
                            Log.debug("adding entry for the first time "+imp_value);
                            List <Integer> lookUpIndices = new ArrayList<Integer>();
                            lookUpIndices.add(j);
                            tempRTLookUps.put(imp_value, lookUpIndices);
                            alreadySeen.add(imp_value);
                        }
                    }
                }
                // lets traverse only on interimRT now
                for(int i = interim_size; i >= 0 ; i--) {
                	String exp_value = interimRT.get(i).get(exp_position);
                    if (exp_value != null) {
                        exp_value = exp_value.toLowerCase();
                        if(alreadySeen.contains(exp_value)) {
                            temp_row1 = interimRT.get(i);
                            for (int tempRTIndex : tempRTLookUps.get(exp_value)) {
                                temp_row2 = tempRT.get(tempRTIndex);
                                List<String> merge = new ArrayList<String>();
                                for (int x = 0; x < temp_row1_size; x++) {
                                    merge.add(temp_row1.get(x));
                                }
                                for (int y = 0; y < temp_row2_size; y++) {
                                    merge.add(temp_row2.get(y));
                                }
                                // it adds the reference of merge
                                retRT.add(merge);
                            }
                        }
                    }
                }
            }
            else {
                // This ELSE should be able to handle the IF above as well by literally
                // deleting the IF part above. The only reason IF is separate is for
                // better performance for 90% of the cases that are not exp/imp lists
                for(int i = interim_size; i >= 0 ; i--) {
                    temp_row1 = interimRT.get(i);
                    for(int j = temp_size; j >= 0 ; j--) {
                        temp_row2 = tempRT.get(j);

                        // with null checks - basically(if a!=null AND b!=null AND a==b)
                        clean_flag = 1;
                        init_loop = 0;
                        try {
                            while (init_loop < length) {
                                if (temp_row1.get(exp_position + init_loop) == null || temp_row2.get(imp_position + init_loop) == null) {
                                    clean_flag = 0;
                                    break;
                                }
                                if (qualifier[init_loop].equals(OperatorType.E) &&
                                        !temp_row1.get(exp_position + init_loop).toLowerCase().equals(temp_row2.get(imp_position + init_loop).toLowerCase())) {
                                    clean_flag = 0;
                                    break;
                                }
                                else if (qualifier[init_loop].equals(OperatorType.LTE) &&
                                        Double.valueOf(temp_row1.get(exp_position + init_loop))
                                            >= Double.valueOf(temp_row2.get(imp_position + init_loop))) {
                                    clean_flag = 0;
                                    break;
                                }
                                else if (qualifier[init_loop].equals(OperatorType.GTE) &&
                                        Double.valueOf(temp_row1.get(exp_position + init_loop))
                                            <= Double.valueOf(temp_row2.get(imp_position + init_loop))) {
                                    clean_flag = 0;
                                    break;
                                }
                                else if (qualifier[init_loop].equals(OperatorType.LT) &&
                                        Double.valueOf(temp_row1.get(exp_position + init_loop))
                                            > Double.valueOf(temp_row2.get(imp_position + init_loop))) {
                                    clean_flag = 0;
                                    break;
                                }
                                else if (qualifier[init_loop].equals(OperatorType.GT) &&
                                        Double.valueOf(temp_row1.get(exp_position + init_loop))
                                            < Double.valueOf(temp_row2.get(imp_position + init_loop))) {
                                    clean_flag = 0;
                                    break;
                                }
                                else if (qualifier[init_loop].equals(OperatorType.LIKE) &&
                                        temp_row2.get(exp_position + init_loop).toLowerCase().contains(temp_row2.get(imp_position + init_loop).toLowerCase())) {
                                    clean_flag = 0;
                                    break;
                                }
                                else{

                                }
                                init_loop++;
                            }
                        }
                        catch (NumberFormatException ex) {
                            Log.debug("<,>,<=,>= expects Double, not strings: ");
                        }
                        if (clean_flag==1) {
                            List<String> merge = new ArrayList<String>();
                            for (int x = 0; x < temp_row1_size; x++) {
                                merge.add(temp_row1.get(x));
                            }
                            for (int y = 0; y < temp_row2_size; y++) {
                                merge.add(temp_row2.get(y));
                            }
                            // it adds the reference of merge
                            retRT.add(merge);
                        }
                    }
                }
            }
        }

        return retRT;
    }


    /**
     *
     * @param expDSName
     * @param impDSName
     * @param current_sq
     * @return
     */
    public Integer getExportablePositionInResults(String expDSName, String impDSName, int current_sq) {
        Integer position = 0;
        for (int i = 0; i < current_sq; i++) {
            SubQuery temp_sq = this.sub_queries.get(i);
            if (temp_sq.getDataset().getName().equals(expDSName)) {
                ArrayList<QueryElement> exportables = temp_sq.getExportables();
                Iterator it_exps = exportables.iterator();
                while (it_exps.hasNext()) {
                    QueryElement exp = (QueryElement) it_exps.next();
                    String importing_ds = exp.getLinkDataset().getName();
                    if (importing_ds.equals(impDSName)) {
                        position += exp.getPortablePosition();
                        return position;
                    }
                }
            }
            position += temp_sq.totalCols;
        }
        // this statement is reached only for those importables that are from band type datasets
        return null;
    }

     /**
     *
     * @param expDSName
     * @param impDSName
     * @param current_sq
     * @return
     */
    public QueryElement getExportableObj(String expDSName, String impDSName, int current_sq) {
        for (int i = 0; i < current_sq; i++) {
            SubQuery temp_sq = this.sub_queries.get(i);
            if (temp_sq.getDataset().getName().equals(expDSName)) {
                ArrayList<QueryElement> exportables = temp_sq.getExportables();
                Iterator it_exps = exportables.iterator();
                while (it_exps.hasNext()) {
                    QueryElement exp = (QueryElement) it_exps.next();
                    String importing_ds = exp.getLinkDataset().getName();
                    if (importing_ds.equals(impDSName)) {
                        return exp;
                    }
                }
            }
        }
        return null;
    }

    /**
     *
     * @param dsName
     * @return
     */
    public SubQuery getSubQueryObjectByDatasetName(String dsName) {

        int sq_len = this.sub_queries.size();
        for (int i = 0; i < sq_len; i++) {
            if (this.sub_queries.get(i).getDataset().getName().equals(dsName)) {
                return this.sub_queries.get(i);
            }
        }
        return null;
    }

}
