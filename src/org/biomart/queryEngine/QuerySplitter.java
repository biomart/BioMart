package org.biomart.queryEngine;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.biomart.common.exceptions.FunctionalException;

import org.biomart.common.resources.Log;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.VirtualLink;

/**
 *
 * @author Jonathan Guberman, Syed Haider
 *
 * This utility class is called by QueryController and takes in a semi-processed
 * query from QueryValidator. It creates a query object which contains SubQuery
 * objects. Linking and finding the best link if there are multiple links between
 * datasets is also done here. The overall flow is best described in QueryController.
 */
public final class QuerySplitter {

    private List<QueryElement> queryElementList;
    private Query query;
    private final MartRegistry registryObj;
	private final boolean isCountQuery;

    /**
     *
     * @return
     */
    public Query getQuery() {
        return query;
    }

    /**
     * 
     * @param qv
     * @param registryObj
     */
    public QuerySplitter(QueryValidator qv, MartRegistry registryObj, boolean isCountQuery) {
		this.isCountQuery = isCountQuery;
        this.queryElementList = qv.getQueryElementList();
        this.query = new Query(qv, isCountQuery);
        this.registryObj = registryObj;
    }

    /**
     *
     */
    public void disectQuery() throws FunctionalException {
        // First add all the non-filter pointers, then add all the filter pointers

        for (QueryElement queryElement : this.queryElementList) {
            if (queryElement.isPointer() && !(queryElement.isPointerInSource())) {
                continue;
            } else {
                this.addToQuery(queryElement, false);
            }
        }
        for (QueryElement queryElement : this.queryElementList) {
            if (queryElement.getType() != QueryElementType.FILTER && queryElement.isPointer()  && !(queryElement.isPointerInSource())) {
                this.addToQuery(queryElement, true);
            }
        }
        for (QueryElement queryElement : this.queryElementList) {
            if (queryElement.getType() == QueryElementType.FILTER && queryElement.isPointer()  && !(queryElement.isPointerInSource())) {
                this.addToQuery(queryElement, true);
            }
        }
        /*for(QueryElement queryElement : this.queryElementList){
        this.addToQuery(queryElement);
        }*/
    }

    /**
     *
     * @param queryElement
     */
    public void addToQuery(QueryElement queryElement, Boolean isPointer) throws FunctionalException {
        Query query = this.query;
        Set<SubQuery> subQuerySet = query.getSubquery(queryElement.getDataset());

        if (isPointer) {
            Dataset pointedDataset = null;
            if (queryElement.getType() == QueryElementType.ATTRIBUTE) {
                List<String> pointedDatasetList = ((Attribute) queryElement.getElement()).getPointedDatasetList();
                if (!pointedDatasetList.isEmpty()) {
                    pointedDataset = ((Attribute) queryElement.getElement()).getPointedDataset(queryElement.getDataset());
                    Log.debug(pointedDataset);
                }
                //pointedDataset = this.registryObj.getDatasetByName("anonymous", "", ((Attribute)queryElement.getElement()).getPointedAttribute().getDatasetName());
                Log.debug("Pointer Attribute validity: " + ((Attribute) queryElement.getElement()).getObjectStatus());

                if (pointedDataset == null) {
                    Log.debug("Pointer Attribute dataset is null: " + ((Attribute) queryElement.getElement()).getPointedDatasetName());
                }
                this.addToQuery(new QueryElement(((Attribute) queryElement.getElement()).getPointedAttribute(), pointedDataset), false);
            } else if (queryElement.getType() == QueryElementType.FILTER) {
                List<String> pointedDatasetList = ((Filter) queryElement.getElement()).getPointedDatasetList();
                if (!pointedDatasetList.isEmpty()) {
                    pointedDataset = ((Filter) queryElement.getElement()).getPointedDataset(queryElement.getDataset());
                }
                //pointedDataset = this.registryObj.getDatasetByName("anonymous", "", ((Filter)queryElement.getElement()).getPointedFilter().getDatasetName());
                Log.debug("Pointer Filter validity: " + ((Filter) queryElement.getElement()).getObjectStatus());

                QueryElement newFilter = new QueryElement(((Filter) queryElement.getElement()).getPointedFilter(), queryElement.getFilterValues(), pointedDataset);
                if (pointedDataset == null) {
                    Log.debug("Pointer filter dataset is null: " + ((Filter) queryElement.getElement()).getPointedDatasetName());
                }
                if (query.hasSubquery(pointedDataset)) {
                    this.addToQuery(newFilter, false);
                } else {
                    Log.debug("The pointed dataset " + pointedDataset + " has no attributes! => Importable only subquery");

                    SubQuery specialSubQuery = query.getImportableOnlySubquery(pointedDataset);
                    if (specialSubQuery == null) {
                        specialSubQuery = new SubQuery(newFilter, isCountQuery);
                        query.addImportableOnlySubquery(pointedDataset, specialSubQuery);
                    } else {
                        specialSubQuery.addFilter(newFilter);
                    }
                    for (SubQuery subQuery : subQuerySet) {
                        Log.debug("adding ImpOnly subquery to unionable dataset: " + subQuery.getDataset().getName());
                        subQuery.addImportableOnlySubQuery(specialSubQuery);
                        Dataset startDataset = specialSubQuery.getDataset();
                        Dataset endDataset = subQuery.getDataset();
                        List<VirtualLink> linkList = McUtils.getPortableList(startDataset, specialSubQuery.getConfig(), endDataset, subQuery.getConfig());
                        if (linkList.size() > 0) {
                            chooseLink(specialSubQuery, startDataset, subQuery, linkList);
                            Log.debug("[Linking] link found between " + startDataset.getName() + " and " + endDataset.getName());
                        } else {
                            Log.debug("[Linking] No link between " + startDataset.getName() + " and " + endDataset.getName());
                            throw new FunctionalException("[Linking] No link between " + startDataset.getName() + " and " + endDataset.getName());
                        }
                    }

                }
            }
        }
        else {
        	// End pointer
        	// Start normal attribute

        	if (subQuerySet == null) {
        		// Make a new subquery for this element
        		query.addNewSubquery(queryElement);
        	} else {
        		//Add element to subQuery
        		for (SubQuery subQuery : subQuerySet) {
        			subQuery.addElement(queryElement);
        		}
        	}
        	this.query = query;
        }
    }

    /**
     *
     */
    public void prepareLinks() throws FunctionalException {
        // For each queryPlan, make sure that the relevant subqueries contain the necessary filters and attributes to link
        Log.debug("Start prepareLinks");
        for (List<SubQuery> startQueryPlan : this.query.queryPlanMap.values()) {
            //		Dataset startDataset = this.registryObj.getDatasetByName("anonymous", "", datasetName);
            Dataset startDataset = startQueryPlan.get(0).getDataset();
            for (SubQuery subquery : startQueryPlan) {
                //Dataset endDataset = this.registryObj.getDatasetByName("anonymous", "", subquery.getDataset().getName());
                Dataset endDataset = subquery.getDataset();
                if (!(subquery.getDataset().equals(startDataset))) {
                    List<VirtualLink> linkList = McUtils.getPortableList(startDataset, startQueryPlan.get(0).getConfig(), endDataset, subquery.getConfig());
                    //List<VirtualLink> linkList = McUtils.getPortableList(endDataset, subquery.getConfig(), startDataset, startQueryPlan.get(0).getConfig());
                    if (linkList.size() > 0) {
                        chooseLink(startQueryPlan.get(0), startDataset, subquery, linkList);
                        //chooseLink(subquery, endDataset, startQueryPlan.get(0), linkList);
                    } else {
                        Log.debug("No link between " + startDataset.getName() + " and " + endDataset.getName());
                        throw new FunctionalException("No link between " + startDataset.getName() + " and " + endDataset.getName());
                    }
                }
            }
        }
        Log.debug("End prepareLinks");

    }

    private void chooseLink(SubQuery startQuery,
        Dataset startDataset, SubQuery subquery,
        List<VirtualLink> linkList) {

        /* TODO (by Syed):
         * 1- exportable.getDatasetTable() should be able to handle exportables whereby
         * its an attributeList, hence could have multiple tables
         * 2- same for importables, not sure why we are scanning importables here, needs revision
         * 3- at the bottom where it returns the first link i-e linkList.get(0),
         * it should return the default link when default property gets 'kicked in'
         */
        VirtualLink chosenLink = null;
        linkloop:
        for (VirtualLink link : linkList) {
            for (Attribute exportable : link.getExportable().getAttributeList()) {
                Log.debug(exportable.getDatasetTable());
                Log.debug(startQuery.getTables().size());
                if (!startQuery.getTables().contains(exportable.getDatasetTable())) {
                    continue linkloop;
                }
            }
            if (chosenLink == null) {
                chosenLink = link;
            } else {
                for (Filter importable : link.getImportable().getFilterList()) {
                    if (!startQuery.getTables().contains(importable.getDatasetTable())) {
                        continue linkloop;
                    }
                }
                chosenLink = link;
            }
        }
        if (chosenLink == null) {
            linkloop:
            for (VirtualLink link : linkList) {
                for (Filter importable : link.getImportable().getFilterList()) {
                    if (!startQuery.getTables().contains(importable.getDatasetTable())) {
                        continue linkloop;
                    }
                }
                chosenLink = link;
                break;
            }
        }
        if (chosenLink == null) {
            chosenLink = linkList.get(0);
        }

        startQuery.addElement(new QueryElement(chosenLink.getExportable(), startDataset, subquery.getDataset(), QueryElementType.EXPORTABLE_ATTRIBUTE));
        subquery.addElement(new QueryElement(chosenLink.getImportable(), subquery.getDataset(), startDataset, QueryElementType.IMPORTABLE_FILTER));
    }
}
