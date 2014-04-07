/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.biomart.api.arq;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.ARQInternalErrorException;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.util.Context;
import java.util.IdentityHashMap;
import java.util.Map;
import org.biomart.api.rdf.Ontology;

/**
 *
 * @author jbaran
 */
public class QueryEngineFactory implements com.hp.hpl.jena.sparql.engine.QueryEngineFactory {
    private Map<Query, QueryMetaData> queryMetadata = new IdentityHashMap<Query, QueryMetaData>();

    public QueryMetaData getQueryMetadata(Query query, Ontology ontology) {
        if (!queryMetadata.containsKey(query))
            throw new ARQInternalErrorException("No metadata available for the given query.");

        QueryMetaData metadata = queryMetadata.remove(query);

        metadata.setOntology(ontology);

        return metadata;
    }

    public boolean accept(Query query, DatasetGraph dataset, Context context) {
        return true;
    }

    public Plan create(Query query, DatasetGraph dataset, Binding initial, Context context) {
        QueryEngine engine = new QueryEngine(query, dataset, initial, context);

        queryMetadata.put(query, engine.getQueryMetaData());

        return engine.getPlan();
    }

    public boolean accept(Op op, DatasetGraph dataset, Context context) {
        return false;
    }

    public Plan create(Op op, DatasetGraph dataset, Binding inputBinding, Context context) {
        throw new ARQInternalErrorException("Factory was called directly.");
    }
}
