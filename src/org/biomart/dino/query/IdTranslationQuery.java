package org.biomart.dino.query;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.biomart.common.resources.Log;
import org.biomart.dino.Binding;
import org.biomart.dino.Utils;
import org.biomart.dino.querybuilder.QueryBuilder;
import org.biomart.objects.objects.Attribute;

import java.io.OutputStream;
import java.util.*;

/**
 * Created by luca on 19/05/14.
 */
public class IdTranslationQuery {

    public enum ElementType {ATTRIBUTE_LIST, FILTER, DATASET, CONFIGURATION};

    @Inject @Named("java_api")
    private QueryBuilder queryBuilder;
    private EnumMap<ElementType, Collection<QueryElement>> context;

    public IdTranslationQuery() {}

    public IdTranslationQuery setContext(EnumMap<ElementType, Collection<QueryElement>> context) {
        this.context = context;
        return this;
    }

    /**
     *
     * @param out
     * @throws BadQueryFormatException
     */
    public void send(OutputStream out) {
        addFilters();
        addTranslAttr();
        addDataset();
        queryBuilder.getResults(out);
    }

    private void addFilters() {
        Iterator<QueryElement> it = iterator(ElementType.FILTER);
        while (it.hasNext()) {
            QueryElement elmt = it.next();
            queryBuilder.addFilter(elmt.getName(), elmt.getValue());
        }
    }

    private void addTranslAttr() {
        Iterator<QueryElement> it = iterator(ElementType.ATTRIBUTE_LIST);
        if (it.hasNext()) {
            QueryElement elmt = it.next();
            try {
                Attribute attr = Utils.getAttributeForEnsemblGeneIdTranslation(Attribute.class.cast(elmt));
                queryBuilder.addAttribute(attr.getName());
                queryBuilder.addAttribute("ensembl_gene_id");
            } catch (ClassCastException e) {
                BadQueryFormatException badFormat =
                        new BadQueryFormatException("The element "+ elmt.getName() + "is not an attribute list", e);
                Log.error(badFormat);
                throw badFormat;
            }
        }
    }

    private void addDataset() {
        Iterator<QueryElement> datasetit = iterator(ElementType.DATASET);
        Iterator<QueryElement> confit = iterator(ElementType.CONFIGURATION);
        if (datasetit.hasNext() && confit.hasNext()) {
            queryBuilder.setDataset(datasetit.next().getName(), confit.next().getName());
        } else {
            BadQueryFormatException badFormat = new BadQueryFormatException("Missing dataset or dataset configuration");
            Log.error(badFormat);
            throw badFormat;
        }
    }

    private Collection<QueryElement> filterBy(ElementType type) {
        Collection<QueryElement> coll = context.get(type);
        return coll != null ? coll : new ArrayList<QueryElement>();
    }

    private Iterator<QueryElement> iterator (ElementType type) {
        return filterBy(type).iterator();
    }

    private QueryBuilder initBuilder() {
        return queryBuilder
                .init()
                .setUseDino(false)
                .setHeader(false);
    }
}
