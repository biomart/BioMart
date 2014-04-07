package org.biomart.api.arq;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.biomart.api.rdf.Ontology;
import org.biomart.api.rdf.RDFClass;
import org.biomart.api.rdf.RDFProperty;

/**
 *
 * @author jbaran
 */
public class QueryMetaData {

    // Graph on which this metadata is applicable
    private String graph;

    // Variable -> Property URI
    // The variable is denoting the property with said URI.
    private final Map<String, String> property = new HashMap<String, String>();

    // Variable -> Type URI
    // The variable is denoting the type with said URI.
    private final Map<String, Set<String>> type = new HashMap<String, Set<String>>();

    // Variable -> Literal
    // The virtual variable is bound to the given literal.
    private final Map<String, String> value = new HashMap<String, String>();

    // Variable (property known, type unknown) -> Variable (whose type will be known)
    // The left variable is denoting a property, but the type has to be derived/inferred
    // from the variable on the right.
    private final Map<String, String> typeInference = new HashMap<String, String>();

    // Variable -> boolean (whether it is a projection or not...)
    private final Map<String, Boolean> projection = new HashMap<String, Boolean>();

    // Variable -> boolean (whether it is a virtual variable or not...)
    private final Map<String, Boolean> virtual = new HashMap<String, Boolean>();

    // Query limit
    // Default: no limit
    private long limit = -1;

    private Ontology ontology;

    public QueryMetaData() {

    }

    public void setOntology(Ontology ontology) {
        this.ontology = ontology;

        for (String variable : type.keySet()) {
            Set<String> uris = type.get(variable);
            Set<String> abbreviatedUris = new HashSet<String>();

            for (String uri : uris)
                abbreviatedUris.add(ontology.abbreviateURI(uri));

            type.put(variable, abbreviatedUris);
        }

        for (String variable : property.keySet())
            property.put(variable, ontology.abbreviateURI(property.get(variable)));
    }

    public Ontology getOntology() {
        return ontology;
    }
    
    public void setGraph(String graph) {
        this.graph = graph;
    }

    public String getGraph() {
        return this.graph;
    }

    public void addProperty(String variable, String uri) {
        if (ontology != null)
            uri = ontology.abbreviateURI(uri);
        
        property.put(variable, uri);
    }

    public void addType(String variable, String uri) {
        if (ontology != null)
            uri = ontology.abbreviateURI(uri);

        Set<String> uris = type.get(variable);

        if (uris == null)
            uris = new HashSet<String>();

        uris.add(uri);

        type.put(variable, uris);
    }

    public void addTypeInference(String propertyVariable, String typeVariable) {
        typeInference.put(propertyVariable, typeVariable);
    }

    public void addValue(String variable, String literal) {
        value.put(variable, literal);
    }

    public void setProjection(String variable, boolean isProjected) {
        projection.put(variable, isProjected);
    }

    public void setVirtual(String variable, boolean isVirtual) {
        virtual.put(variable, isVirtual);
    }

    public String getProperty(String variable) {
        return property.get(variable);
    }

    public String getValue(String variable) {
        return value.get(variable);
    }

    public Set<String> getVariables() {
        Set<String> variables = new HashSet<String>();

        variables.addAll(type.keySet());
        variables.addAll(typeInference.keySet());
        variables.addAll(property.keySet());
        variables.addAll(projection.keySet());
        variables.addAll(value.keySet());
        variables.addAll(virtual.keySet());

        return variables;
    }

    public Set<String> getType(String variable) {
        Set<String> variableType = type.get(variable);

        if (variableType == null && typeInference.containsKey(variable))
            variableType = type.get(typeInference.get(variable));

        if (variableType == null && ontology != null) {
            variableType = new HashSet<String>();

            for (RDFClass rdfClass : ontology.getRDFClasses())
                for (RDFProperty rdfProperty : rdfClass.getProperties())
                    if (rdfProperty.getName().equals(property.get(variable)))
                        variableType.add(rdfClass.getName());
        }

        return variableType;
    }

    public Set<String> getProjections() {
        Set<String> projections = new HashSet<String>();

        for (String variable : getVariables())
            if (isProjection(variable))
                projections.add(variable);

        return projections;
    }

    public boolean isProjection(String variable) {
        Boolean isProjection = projection.get(variable);

        if (isProjection == null)
            return false;
        
        return isProjection;
    }

    public boolean isVirtual(String variable) {
        Boolean isVirtual = virtual.get(variable);

        if (isVirtual == null)
            return false;

        return isVirtual;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getLimit() {
        return limit;
    }
}
