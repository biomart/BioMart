package org.biomart.queryEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.biomart.common.utils2.MyUtils;
import org.biomart.objects.objects.Dataset;

/**
 *
 * @author Jonathan Guberman, Syed Haider
 *
 * The query object is an encapsulation of SubQuery objects linked to form a chain.
 * Each chain (horizontally linked) refers to a QueryRunnerThread.
 * Several chains are independent Threads that are executed simultaneously, writing to
 * the same output buffer. Importable only subqueries (where there is only filters
 * eg bands, ontology) however are only executed ONCE right at the begining without
 * any batching and their resultset is shared between multiple threads.
 * There is no need to execute importable only subqueries multiple times as their
 * resultset remains the same
 */
public class Query {

	private final String processor;
	private final String dino;
    private final Map<Dataset, SubQuery> importableOnlySubqueries = new HashMap<Dataset,SubQuery>();
	private Map<Dataset, Set<SubQuery>> datasetSubqueries;
	//private LinkIndex linkIndex = new LinkIndex();

    protected Map<String,List<SubQuery>> queryPlanMap = null;	// output of QueryPlanner
    protected final int limit;
    protected final int[] outputOrder;
    protected final Integer[] isAttributeList;
    protected final String[] outputDisplayNames;
	// protected ArrayList<java structure for graph> graphList = null;	// to use as input for QueryPlanner
    protected final int numDatasets;

	private List<QueryElement> queryElementList;
	private List<QueryElement> originalAttributeOrder;
	private String client = "";
    private boolean hasHeader;

    public List<QueryElement> pseudoAttributes = new ArrayList<QueryElement>();

	private final boolean isCountQuery;
	
	private List<QueryElement> attributeListList = null;
	private List<QueryElement> filtersGroup = null;

    public Query(QueryValidator qv, boolean isCountQuery) {
		this.isCountQuery = isCountQuery;
		List<String> queryStartingPoints = qv.getQueryStartingPoints();
        numDatasets = queryStartingPoints.size();
		this.queryPlanMap = new LinkedHashMap<String,List<SubQuery>>();
		//this.queryPlanPseudos = new LinkedHashMap<String, ArrayList<Attribute>>();
		for(String key: queryStartingPoints){
			this.queryPlanMap.put(key, new ArrayList<SubQuery>());
			//this.queryPlanPseudos.put(key, new ArrayList<Attribute>());
		}
		this.datasetSubqueries = new HashMap<Dataset, Set<SubQuery>>();
		this.limit = (qv.getLimit());
		this.processor = (qv.getProcessor());
		this.dino = qv.getDino();
		this.queryElementList = qv.getQueryElementList();
		this.pseudoAttributes =  qv.getPseudoAttributes();
		this.originalAttributeOrder = qv.getOriginalAttributeOrder();
		this.client = qv.getClient();
        this.hasHeader = qv.hasHeader();
        this.outputOrder = new int[this.originalAttributeOrder.size()];
        this.isAttributeList = qv.getAttributeListSizes().toArray(new Integer[qv.getAttributeListSizes().size()]);
		this.outputDisplayNames = new String[this.originalAttributeOrder.size()];
		
		attributeListList = qv.getAttributeListList();
		filtersGroup = qv.getFilters();
	}

    public String getDino() {
    		return dino;
    }
    
    public List<QueryElement> getAttributeListList() {
        return attributeListList;
    }
    
    public List<QueryElement> getFilters() {
        return filtersGroup;
    }
    
    
    public void addFilter(QueryElement f) {
        this.filtersGroup.add(f);
    }
    
    
    public Map<Dataset, SubQuery> getImportableOnlySubqueries() {
		return importableOnlySubqueries;
	}

    public Map<Dataset, Set<SubQuery>> getDatasetSubqueries() {
		return datasetSubqueries;
	}

    public void addImportableOnlySubquery(Dataset ds, SubQuery sq){
    	this.importableOnlySubqueries.put(ds,sq);
    }
    
    public SubQuery getImportableOnlySubquery(Dataset ds){
    	return this.importableOnlySubqueries.get(ds);
    }

    public Document queryXMLobject;

    public int getLimit() {
		return limit;
	}

    public String getProcessor() {
		return processor;
	}

    public List<QueryElement> getQueryElementList() {
		return queryElementList;
	}

    public String getClient(){
		return client;
	}

    public boolean hasHeader(){
		return hasHeader;
	}
    
    public List<QueryElement> getOriginalAttributeOrder() {
		return originalAttributeOrder;
	}

    public void setPseudoAttributes(List<QueryElement> psAtts){
        this.pseudoAttributes = psAtts;
    }

    public List<QueryElement> getPseudoAttributes(){
        return this.pseudoAttributes;
    }

    public boolean hasSubquery(Dataset dataset){
		return this.datasetSubqueries.containsKey(dataset);
	}

    public void planQuery(){
		for(String key: this.queryPlanMap.keySet()){
			List<SubQuery> queryPlan = this.queryPlanMap.get(key);
			List<SubQuery> plannedQuery = new ArrayList<SubQuery>();
			for(SubQuery subquery : queryPlan){
				// Only rearrange the queries if it's not the first one
				if(!(subquery.getDataset().getDisplayName().equals(key))){
					int i = 0;
					while(i < plannedQuery.size()){
						if(subquery.getQueryFilterList().size() > plannedQuery.get(i).getQueryFilterList().size()){
							break;
						}
						++i;
					}
					plannedQuery.add(i,subquery);
					/*
					if(subquery.getQueryFilterList().size() > maxScore){
						plannedQuery.add(0, subquery);
						maxScore = subquery.getQueryFilterList().size();
					} else {
						plannedQuery.add(subquery);
					}*/
				} 
			}
			// Add first dataset to the beginning
			if(queryPlan.get(0).getDataset().getDisplayName().equals(key))
				plannedQuery.add(0,queryPlan.get(0));
			this.queryPlanMap.put(key, plannedQuery);
		}
	}

    public void addNewSubquery(QueryElement queryElement){

		List<SubQuery> firstList = this.queryPlanMap.get(queryElement.getDataset().getDisplayName());
		Set<SubQuery> sqSet = new HashSet<SubQuery>();
		if(firstList!=null){
			SubQuery subquery = new SubQuery(queryElement, isCountQuery);
			subquery.setProcessor(this.processor);
			subquery.setLimit(this.limit);
			//subquery.setLinkIndex(this.linkIndex);
			subquery.setClient(this.client);
			firstList.add(subquery);
			sqSet.add(subquery);
//			subquery.setPosition(firstList.size());
		} else {
			for(String key : this.queryPlanMap.keySet()){
				List<SubQuery> subqueryList = this.queryPlanMap.get(key);
				SubQuery subquery = new SubQuery(queryElement, isCountQuery);
				subquery.setProcessor(this.processor);
				subquery.setLimit(this.limit);
//				subquery.setPosition(subqueryList.size());
				//subquery.setLinkIndex(this.linkIndex);
				subquery.setClient(this.client);
				subqueryList.add(subquery);
				sqSet.add(subquery);

				//TODO set query operation
				//queryElement.getDataset().getMartPointer().getOperation()
			}
		}
		this.datasetSubqueries.put(queryElement.getDataset(),sqSet);
	}

    public void addImportableSubquery(SubQuery subquery){
		subquery.setProcessor(this.processor);
		subquery.setLimit(this.limit);
		//subquery.setLinkIndex(this.linkIndex);
		HashSet<SubQuery> sqSet = new HashSet<SubQuery>();
		sqSet.add(subquery);
		this.datasetSubqueries.put(subquery.getDataset(),sqSet);
	}

    public Set<SubQuery> getSubquery(Dataset dataset){
		return this.datasetSubqueries.get(dataset);
	}

	/*public SubQuery getSubquery(Dataset dataset){
		return this.datasetSubqueries.get(dataset);
	}

	public SubQuery getSubquery(String datasetName){
		return this.datasetSubqueries.get(datasetName);
	}*/

    public boolean getOperation(){
		return true;
	}

    public List<List<SubQuery>> getQueryPlans(){
		List<List<SubQuery>> queryPlans = new ArrayList<List<SubQuery>>();
		for(String key: queryPlanMap.keySet()){
			queryPlans.add(queryPlanMap.get(key));
		}
		return queryPlans;
	}

    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for(String key : this.queryPlanMap.keySet()){
			List<SubQuery> subQueries = this.queryPlanMap.get(key);
			for (SubQuery subQuery : subQueries) {
				sb.append(subQuery);
			}
		}

		return sb.toString();
	}

    public void setClient(String client) {
		this.client = client;
	}

    public void setHasHeader(boolean hasHeader) {
		this.hasHeader = hasHeader;
	}

    public int getNumDatasets() {
        return numDatasets;
    }
}
