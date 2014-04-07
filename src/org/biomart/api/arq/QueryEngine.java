package org.biomart.api.arq;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.Transform;
import com.hp.hpl.jena.sparql.algebra.TransformBase;
import com.hp.hpl.jena.sparql.algebra.Transformer;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpDiff;
import com.hp.hpl.jena.sparql.algebra.op.OpDisjunction;
import com.hp.hpl.jena.sparql.algebra.op.OpDistinct;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpExtend;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpGraph;
import com.hp.hpl.jena.sparql.algebra.op.OpGroup;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpList;
import com.hp.hpl.jena.sparql.algebra.op.OpMinus;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpOrder;
import com.hp.hpl.jena.sparql.algebra.op.OpPath;
import com.hp.hpl.jena.sparql.algebra.op.OpProcedure;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.algebra.op.OpPropFunc;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpReduced;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.QueryEngineRegistry;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.main.QueryEngineMain;
import com.hp.hpl.jena.sparql.util.Context;
import java.util.List;

/**
 *
 * @author jbaran
 */
public class QueryEngine extends QueryEngineMain {

    private QueryMetaData metadata = new QueryMetaData();;

    public QueryEngine(Query query, DatasetGraph dataset, Binding initial, Context context) {
        super(query, dataset, initial, context);
    }

    public QueryEngine(Query query, DatasetGraph dataset) {
        this(query, dataset, null, null);
    }

    @Override
    public QueryIterator eval(Op op, DatasetGraph dsg, Binding binding, Context context) {
        Transform transform = new MartTransform(metadata);

        op = Transformer.transform(transform, op);

        return super.eval(op, dsg, binding, context);
    }

    public QueryMetaData getQueryMetaData() {
        return metadata;
    }

    public static final QueryEngineFactory factory = new QueryEngineFactory();

    public static QueryEngineFactory getFactory() {
        return factory;
    }

    public static void register() {
        QueryEngineRegistry.addFactory(factory);
    }

    public static void unregister() {
        QueryEngineRegistry.removeFactory(factory);
    }
}

class MartTransform extends TransformBase {

    static protected final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private QueryMetaData metadata;

    // Do not use this variable directly, but use the method getVirtualVariableNumber() instead.
    private int virtualVariableNo = 0;

    public MartTransform(QueryMetaData metadata) {
        this.metadata = metadata;
    }

    private int getVirtualVariableNumber() {
        return virtualVariableNo++;
    }

    // All predicates have to be URIs.
    public Op transform(OpBGP opBGP) {
        BasicPattern pattern = opBGP.getPattern();

        for (Triple triple : pattern.getList()) {
            String subjectType = null;

            if (triple.getSubject().isURI()) {
                subjectType = triple.getSubject().getURI();
            }

            if (triple.getSubject().isVariable()) {
                String variable = triple.getSubject().getName();

                if (triple.getPredicate().hasURI(RDF_TYPE) &&
                    triple.getObject().isURI())
                    metadata.addType(variable, triple.getObject().getURI());
                else if (triple.getObject().isLiteral()) {
                    String virtualVariable = "!" + variable + getVirtualVariableNumber();

                    metadata.setVirtual(virtualVariable, true);
                    metadata.addTypeInference(virtualVariable, variable);
                    metadata.addProperty(virtualVariable, triple.getPredicate().getURI());
                    metadata.addValue(virtualVariable, triple.getObject().getLiteral().getLexicalForm());
                }
            }

            if (triple.getObject().isVariable()) {
                String variable = triple.getObject().getName();

                metadata.addProperty(variable, triple.getPredicate().getURI());

                if (triple.getSubject().isVariable())
                    metadata.addTypeInference(variable, triple.getSubject().getName());
            }
        }
        
        return super.transform(opBGP);
    }

    public Op transform(OpTable ot) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpTriple ot) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpPath oppath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpDatasetNames odn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpQuadPattern oqp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpNull opnull) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpFilter of, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpGraph og, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");

        // Yes.. it would be nice if this works, but somehow Jena does
        // not pick up on the named dataset when running a SELECT query.
        /*
        metadata.setGraph(og.getNode().getURI());

        return super.transform(og, op);
         */
    }

    public Op transform(OpService os, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpProcedure op, Op op1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpPropFunc opf, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpLabel ol, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpAssign oa, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpExtend oe, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpJoin opjoin, Op op, Op op1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpLeftJoin olj, Op op, Op op1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpDiff opdiff, Op op, Op op1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpMinus om, Op op, Op op1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpUnion ou, Op op, Op op1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpConditional oc, Op op, Op op1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpSequence os, List<Op> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpDisjunction od, List<Op> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpExt opext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpList oplist, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpOrder oo, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpProject op, Op op1) {
        for (Var variable : op.getVars())
            metadata.setProjection(variable.getName(), true);

        return super.transform(op, op1);
    }

    public Op transform(OpDistinct od, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpReduced or, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Op transform(OpSlice os, Op op) {
        if (os.getStart() != Long.MIN_VALUE)
            throw new UnsupportedOperationException("Not supported yet.");

        metadata.setLimit(os.getLength());

        return super.transform(os, op);
    }

    public Op transform(OpGroup og, Op op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
