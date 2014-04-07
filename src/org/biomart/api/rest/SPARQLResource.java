package org.biomart.api.rest;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.hp.hpl.jena.query.DataSource;
import com.hp.hpl.jena.query.DatasetFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.lang.ParserSPARQL11;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.lang.StringUtils;
import org.biomart.api.Portal;
import org.biomart.api.arq.QueryEngine;
import org.biomart.api.arq.QueryMetaData;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.lite.Filter;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Dataset;
import org.biomart.api.lite.Mart;
import org.biomart.api.rdf.Ontology;
import org.biomart.api.rdf.RDFClass;
import org.biomart.api.rdf.RDFObject;
import org.biomart.api.rdf.RDFProperty;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.resources.Log;

/**
 *
 * @author Joachim Baran
 */
@Singleton
@Path("/martsemantics/")
public class SPARQLResource {
    @Context HttpServletRequest request;
    @Context UriInfo uriInfo;
    @Inject Injector injector;

    private MartRegistryFactory factory;
    
    // Cache: Mart -> Dataset -> Ontology
    private Map<Mart, Map<String, Ontology>> ontologyCache = new HashMap<Mart, Map<String, Ontology>>();

    // Used as regexp:
    static protected final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    static protected final String BIOMART_HAS_VALUE = "http://www.biomart.org/ontology#has_value";

    // Error responses:
    static protected final String RDF_MC_PROPERTY_SYNTAX_ERROR = "The property 'rdf' of '%s' is ill-formatted. Sorry.";
    static protected final String RDF_MART_UNKNOWN = "A mart with the name '%s' is not known. Sorry.";
    static protected final String RDF_SPARQL_PRINCIPAL = "The following principal mart did not return results:\n%s\n";
    static protected final String RDF_SPARQL_GROUP_SIZE_UNSUPPORTED = "Only one group is allowed per query. Sorry.";
    static protected final String RDF_SPARQL_SUBJECT_VARIABLE_UNSUPPORTED = "Variables are not allowed in the subject. Sorry.";
    static protected final String RDF_SPARQL_X_UNSUPPORTED = "This kind of SPARQL-query is not supported. Sorry.";
    static protected final String RDF_SPARQL_NOT_ELEMENTGROUP = "Only queries with a top-level element group are supported. Sorry.";
    static protected final String RDF_SPARQL_MALFORMED = "The query is malformed. Terribly sorry.";
    static protected final String RPC_QUERY_IO_FAILURE = "Query failed to execute due to an IO error. Terribly sorry.";

    private class SPARQLException extends Exception {
        public SPARQLException(String description) {
            super(description);
        }
    }

    static {
        QueryEngine.register();
    }

    @Inject
    public SPARQLResource(MartRegistryFactory factory) {
        this.factory = factory;
    }

    private class StringPair implements Comparable {
        public String[] pair = new String[2];

        public StringPair(String x, String y) {
            pair[0] = x;
            pair[1] = y;
        }

        public boolean equals(Object o) {
            if (!(o instanceof StringPair))
                return false;

            return compareTo(o) == 0;
        }

        public int compareTo(Object o) {
            if (!(o instanceof StringPair)) {
                throw new ClassCastException("Can only compare StringPairs against themselves.");
            }

            StringPair otherPair = (StringPair)o;

            return (pair[0] + pair[1]).compareTo(otherPair.pair[0] + otherPair.pair[1]);
        }
    }

    @Path("{mart}/{format}/get")
    @GET 
    public Response queryGetRequestWrapper(
            @QueryParam("callback") @DefaultValue("") String callback,
            @PathParam("format") String format,
            @PathParam("mart") String martName) throws TechnicalException, FunctionalException  {
        final StringBuilder exceptionResponse = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query>");

        try {
            return queryGetRequest(callback, format, martName);
        }
        catch(TechnicalException te) {
            exceptionResponse.append("<!-- Technical-Exception:");
            exceptionResponse.append(te.getMessage());
            exceptionResponse.append("-->\n");
        }
        catch(FunctionalException fe) {
            exceptionResponse.append("<!-- Functional-Exception:");
            exceptionResponse.append(fe.getMessage());
            exceptionResponse.append("-->\n");
        }
        catch(SPARQLException se) {
            exceptionResponse.append("<!-- SPARQL-Exception:");
            exceptionResponse.append(se.getMessage());
            exceptionResponse.append("-->\n");
        }
        catch(RuntimeException re) {
            exceptionResponse.append("<!-- Runtime-Exception:");
            exceptionResponse.append(re.getMessage());
            exceptionResponse.append("-->\n");
        }

        return Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream out) {
                try {
                    out.write(exceptionResponse.toString().getBytes());
                }
                catch(IOException ioe) {
                    // Well, if we cannot communicate, then there is
                    // little that can be done about it.
                }
            }
        }, "application/sparql-results+xml").build();
    }

    private Response queryGetRequest(
            String callback,
            String format,
            String martName) throws TechnicalException, FunctionalException, SPARQLException  {
        final Mart mart = new Portal(factory)._registry.getMartByConfigName(martName);

        if (mart == null)
            throw new SPARQLException(String.format(RDF_MART_UNKNOWN, martName));

        String queryString = request.getParameter("query");
        Query query = new com.hp.hpl.jena.query.Query();

        Log.info("Incoming SPARQL query: " + queryString);

        try {
            new ParserSPARQL11().parse(query, queryString);
        }
        catch(QueryParseException qpe) {
            throw new SPARQLException(RDF_SPARQL_MALFORMED);
        }

        // No namespaces defined, because we are getting rid of the prefix only.
        RDFObject datasetURI = new RDFObject(query.getGraphURIs().get(0), null);
        String dataset = datasetURI.getShortName();

        String ontologyUri = getOntologyUrl(martName);
        Ontology ontology = getOntology(mart, dataset, ontologyUri);

        // Execute query on an empty model to pick up on the mart-dataset(s) to use:
        //QueryExecution queryExecution = QueryExecutionFactory.create(query, ModelFactory.createDefaultModel());
        //queryExecution.execSelect();
        //final QueryMetaData preliminaryMetadata = QueryEngine.factory.getQueryMetadata(query, ontology);

        // It would be nice if it would work like this: (see also QueryEngine transform(OpGraph og, Op op))
        //String dataset = preliminaryMetadata.getGraph();

        String principalMart = getPrincipalMart(format, martName, dataset, query);

        // Run query on a "principle mart" that adheres to the real mart's structure,
        // but does not contain any real data.
        DataSource source = DatasetFactory.create();

        //Model model = ModelFactory.createDefaultModel();
        Model model = ModelFactory.createOntologyModel();
        model.read(new StringReader(principalMart), "");

        // Add named models for multiple datasets?
        source.addNamedModel(datasetURI.getName(), model);
        source.setDefaultModel(model);
        
        //QueryExecution queryExecution = QueryExecutionFactory.create(query, source);
        QueryExecution queryExecution = QueryExecutionFactory.create(query, model);

        ResultSet principalResults = queryExecution.execSelect();

        if (!principalResults.hasNext())
            throw new SPARQLException(String.format(RDF_SPARQL_PRINCIPAL, principalMart));

        QuerySolution principalSolution = principalResults.next();

        // Variable bindings to types/classes and properties:
        QueryMetaData metadata = QueryEngine.factory.getQueryMetadata(query, ontology);

        Map<String, String> variable2Filter = new HashMap<String, String>();
        Map<String, String> variable2Attribute = new HashMap<String, String>();
        StringBuilder variables = new StringBuilder();
        StringBuilder variableProperties = new StringBuilder();
        StringBuilder nodeAttributes = new StringBuilder();
        List<String> nodeMartAttributes = new LinkedList<String>();
        for (String variable : metadata.getVariables())
            for (RDFClass rdfClass : ontology.getRDFClasses()) {

                // Is this class involved?
                if (metadata.getType(variable).contains(rdfClass.getName())) {
                    
                    // Is this an inquiry about a property that we can return?
                    if (metadata.getProperty(variable) != null)
                        
                        // Yes, so lets bind it to the right attribute:
                        for (RDFProperty property : rdfClass.getProperties())
                            if (metadata.getProperty(variable).equals(property.getName())) {
                                String value = metadata.getValue(variable);

                                if (value == null) {
                                    variable2Attribute.put(variable, property.getAttribute());

                                    if (!metadata.isProjection(variable))
                                        variables.append("?");

                                    variables.append(variable);
                                    variables.append(" ");
                                    
                                    variableProperties.append(metadata.getProperty(variable));
                                    variableProperties.append(" ");
                                } else {
                                    variable2Filter.put(variable, property.getFilter());
                                }
                            }
                    // TODO If no attribute was found -> error
                }
            }

        for (String variable : metadata.getVariables())
            for (RDFClass rdfClass : ontology.getRDFClasses()) {

                // Is this class involved?
                if (metadata.getType(variable).contains(rdfClass.getName())) {
                    
                    // Is this an inquiry about a property that we can return?
                    if (metadata.getProperty(variable) != null)
                        
                        // Yes, so lets figure out the attribute that can be used to
                        // form the URI for its subjects:
                        for (RDFProperty property : rdfClass.getProperties())
                            if (metadata.getProperty(variable).equals(property.getName())) {
                                String value = metadata.getValue(variable);

                                // Only proceed if this is not a filter:
                                if (value == null) {
                                    nodeAttributes.append(variable);
                                    nodeAttributes.append(":");
                                    nodeAttributes.append(StringUtils.join(rdfClass.getURIAttributes(), ","));
                                    nodeAttributes.append(" ");
                                    
                                    // TODO For now the URI attributes have to be a singleton set:
                                    String nodeURI = StringUtils.join(rdfClass.getURIAttributes(), ",");
                                    if (nodeMartAttributes.contains(nodeURI) || variable2Attribute.containsValue(nodeURI))
                                        nodeURI = "?" + nodeURI;
                                    nodeMartAttributes.add(nodeURI);
                                }
                            }
                }
            }
        
        queryExecution.close();
        
        final StringBuffer xmlQuery = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query>");

        if (!format.equals("SPARQLXML"))
            for (String v : nodeMartAttributes) {
                if (v.startsWith("?"))
                    continue;
                variables.append("?");
                variables.append(v);
                variables.append(" ");
            }

        xmlQuery.append("<!-- VARIABLES:");
        xmlQuery.append(variables);
        xmlQuery.append("-->\n");

        xmlQuery.append("<!-- VARIABLETYPES:");
        for (String attribute : nodeMartAttributes) {
            xmlQuery.append(attribute.replaceFirst("\\?", ""));
            xmlQuery.append(" ");
        }
        xmlQuery.append("-->\n");
        
        xmlQuery.append("<!-- VARIABLEPROPERTIES:");
        xmlQuery.append(variableProperties);
        xmlQuery.append("-->\n");

        xmlQuery.append("<!-- NODEATTRIBUTES:");
        xmlQuery.append(nodeAttributes);
        xmlQuery.append("-->\n");

        Map<String, String> namespaces = metadata.getOntology().getNamespaces();
        for (String prefix : namespaces.keySet()) {
            xmlQuery.append("<!-- NAMESPACE:");
            xmlQuery.append(prefix);
            xmlQuery.append(" ");
            xmlQuery.append(namespaces.get(prefix));
            xmlQuery.append("-->\n");
        }
        
        long queryLimit = metadata.getLimit();

        StringBuffer martXMLQuery = new StringBuffer();

        StringBuffer datasets = new StringBuffer();
        for (String uri : query.getGraphURIs()) {
            datasetURI = new RDFObject(uri, null);
            dataset = datasetURI.getShortName();

            if (!datasets.toString().isEmpty())
                datasets.append(",");

            datasets.append(dataset);
        }

        martXMLQuery.append("<Query client=\"webbrowser\" processor=\"" + format + "\" limit=\"" + queryLimit + "\" header=\"0\">\n");
        martXMLQuery.append("\t<Dataset name=\"" + datasets.toString() + "\" config=\"" + martName + "\">\n");

        // Filter list of literal queries:
        for (String v : variable2Filter.keySet()) {
            String value = metadata.getValue(v);

            if (value != null)
                martXMLQuery.append("\t\t<Filter name=\"" + variable2Filter.get(v) + "\" value=\"" + value + "\"/>\n");
        }

        // Attribute list:
        for (String v : variable2Attribute.keySet()) {
            martXMLQuery.append("\t\t<Attribute name=\"" + variable2Attribute.get(v) + "\"/>\n");
        }

        // URL list for generating URIs for blank nodes:
        for (int i = 0; i < nodeMartAttributes.size() - 1; i++)
            for (int j = i + 1; j < nodeMartAttributes.size(); j++)
                if (nodeMartAttributes.get(i).equals(nodeMartAttributes.get(j))) {
                    nodeMartAttributes.remove(j);
                    i = 0;
                    j = nodeMartAttributes.size();
                }
        
        if (!format.equals("SPARQLXML"))
            for (String v : nodeMartAttributes) {
                if (v.startsWith("?"))
                    continue;
                martXMLQuery.append("\t\t<Attribute name=\"" + v + "\"/>\n");
            }
        
        martXMLQuery.append("\t</Dataset>\n</Query>\n");

        xmlQuery.append("<!-- BioMart XML-Query:\n");
        xmlQuery.append(martXMLQuery);
        xmlQuery.append("-->");
        xmlQuery.append(martXMLQuery);

        ProcessorStreamingOutput stream = new ProcessorStreamingOutput(xmlQuery.toString(),
                new Portal(factory),
                false,
                null,
                null,
                new String[] { "application/sparql-results+xml", "text/n3" });

        return Response.ok(stream, stream.getContentType()).build();
    }

    private String getOntologyUrl(String martName) {
        String ontologyUrl = System.getProperty("http.url");
        
        if(request.isSecure())
            ontologyUrl = System.getProperty("https.url");
        ontologyUrl = ontologyUrl + "martsemantics/" + martName + "/ontology";

        return ontologyUrl;
    }

    @Path("{mart}/ontology")
    @GET
    public Response metadataGetRequestWrapper(
            @QueryParam("callback") @DefaultValue("") String callback,
            @PathParam("format") String format,
            @PathParam("mart") String martName) throws TechnicalException, FunctionalException {
        final StringBuilder exceptionResponse = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query>");

        try {
            return metadataGetRequest(callback, format, martName);
        }
        catch(TechnicalException te) {
            exceptionResponse.append("A technical problem occurred.");
        }
        catch(FunctionalException fe) {
            exceptionResponse.append("A functional problem occurred.");
        }
        catch(SPARQLException se) {
            exceptionResponse.append("<!-- SPARQL-Exception:");
            exceptionResponse.append(se.getMessage());
            exceptionResponse.append("-->\n");
        }

        return Response.ok(new StreamingOutput() {
            public void write(OutputStream out) {
                try {
                    out.write(exceptionResponse.toString().getBytes());
                }
                catch(IOException ioe) {
                    // Well, if we cannot communicate, then there is
                    // little that can be done about it.
                }
            }
        }).build();
    }

    private static String site2Reference(String ontologyUri) {
        final String ref = ontologyUri.replaceFirst("^https:", "biomart:");
        return ref.replaceFirst("^http:", "biomart:");
    }

    private static void concatRDFClassList(StringBuilder string, List<RDFClass> classes, int indent) {
        RDFClass rdfClass = classes.remove(0);

        String indentString = StringUtils.repeat(" ", indent);

        string.append(indentString);
        string.append("          <rdf:List>\n");
        string.append(indentString);
        string.append("            <rdf:first>\n");
        string.append(indentString);
        string.append("              <rdf:type rdf:resource=\"" + rdfClass.getFullName() + "\" />\n");
        string.append(indentString);
        string.append("            </rdf:first>\n");

        if (!classes.isEmpty()) {
            string.append(indentString);
            string.append("            <rdf:rest>\n");

            concatRDFClassList(string, classes, indent + 2);

            string.append(indentString);
            string.append("            </rdf:rest>\n");
        } else {
            string.append(indentString);
            string.append("            <rdf:rest rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\" />\n");
        }

        string.append(indentString);
        string.append("          </rdf:List>\n");
    }

    public Response metadataGetRequest(
            @QueryParam("callback") @DefaultValue("") String callback,
            @PathParam("format") String format,
            @PathParam("mart") String martName) throws TechnicalException, FunctionalException, SPARQLException {
        Mart mart = new Portal(factory)._registry.getMartByConfigName(martName);

        if (mart == null)
            throw new SPARQLException(String.format(RDF_MART_UNKNOWN, martName));

        final String ontologyUri = getOntologyUrl(martName);

        final String rdfOntology = getOntologyRDF(mart, ontologyUri);

        return Response.ok(new StreamingOutput() {
            public void write(OutputStream out) {
                try {
                    out.write(rdfOntology.getBytes());
                }
                catch(IOException ioe) {
                    // Well, if we cannot communicate, then there is
                    // little that can be done about it.
                }
            }
        }, "application/rdf+xml").build();
    }

    protected Set<String> getLiterals(Query query) throws SPARQLException {
        Set<String> literals = new HashSet<String>();
        Element queryPattern = query.getQueryPattern();

        if (queryPattern instanceof ElementGroup) {
            List<Element> group = ((ElementGroup)queryPattern).getElements();

            for (Element element : group) {
                if (element instanceof ElementPathBlock) {
                    for (TriplePath triplePath : ((ElementPathBlock)element).getPattern()) {
                        if (triplePath.getObject().isLiteral())
                            literals.add(triplePath.getObject().getLiteral().getLexicalForm());
                    }
                }
            }

        } else {
            throw new SPARQLException(String.format(RDF_SPARQL_NOT_ELEMENTGROUP));
        }

        return literals;
    }

    protected String getOntologyRDF(Mart mart, String ontologyUri) throws SPARQLException {
        final StringBuilder rdfOntology = new StringBuilder("<rdf:RDF\n");

        rdfOntology.append("  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
        rdfOntology.append("  xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n");
        rdfOntology.append("  xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n");

        rdfOntology.append("\n");

        rdfOntology.append("  xmlns:property=\"");
        rdfOntology.append(site2Reference(ontologyUri));
        rdfOntology.append("/property#\"\n");

        rdfOntology.append("  xmlns:class=\"");
        rdfOntology.append(site2Reference(ontologyUri));
        rdfOntology.append("/class#\"\n");

        rdfOntology.append("  xmlns:dataset=\"");
        rdfOntology.append(site2Reference(ontologyUri));
        rdfOntology.append("/dataset#\"\n");

        rdfOntology.append("  xmlns:attribute=\"");
        rdfOntology.append(site2Reference(ontologyUri));
        rdfOntology.append("/attribute#\">\n\n");

        rdfOntology.append("  <owl:Ontology rdf:about=\"");
        rdfOntology.append(ontologyUri);
        rdfOntology.append("\"/>\n\n");

        HashSet<RDFClass> allClasses = new HashSet<RDFClass>();

        for (Dataset datasetLite : mart.getDatasets()) {

            String dataset = datasetLite.getName();

            Ontology ontology = getOntology(mart, dataset, ontologyUri);

            //rdfOntology.append("  <owl:Class rdf:about=\"" + site2Reference(ontologyUri) + "/datasets#" + dataset + "\">\n");
            rdfOntology.append("  <owl:Class rdf:ID=\"Dataset\">\n");
            rdfOntology.append("    <rdfs:label>Dataset</rdfs:label>\n");
            rdfOntology.append("    <rdfs:comment>Generic representation of values in the mart.</rdfs:comment>\n");
            rdfOntology.append("  </owl:Class>\n\n");

            rdfOntology.append("  <owl:Thing rdf:about=\"" + site2Reference(ontologyUri) + "/datasets#" + dataset + "\">\n");
            rdfOntology.append("    <rdfs:label>" + dataset + "</rdfs:label>\n");
            rdfOntology.append("    <rdf:type rdf:resource=\"#Dataset\" />\n");
            rdfOntology.append("  </owl:Thing>\n\n");

            rdfOntology.append("  <owl:ObjectProperty rdf:ID=\"usesClasses\">\n");
            rdfOntology.append("    <rdfs:domain rdf:resource=\"" + site2Reference(ontologyUri) + "/datasets#" + dataset + "\" />\n");
            rdfOntology.append("    <rdfs:range>\n");

            rdfOntology.append("      <owl:Class>\n");
            rdfOntology.append("        <owl:unionOf rdf:parseType=\"Collection\">\n");
            for (RDFClass rdfClass : ontology.getRDFClasses())
                rdfOntology.append("          <owl:Class rdf:about=\"" + rdfClass.getFullName() + "\" />\n");
            rdfOntology.append("        </owl:unionOf>\n");
            rdfOntology.append("      </owl:Class>\n");

            rdfOntology.append("    </rdfs:range>\n");

            rdfOntology.append("  </owl:ObjectProperty>\n\n");

            allClasses.addAll(ontology.getRDFClasses());
        }

        for (RDFClass rdfClass : allClasses) {
            rdfOntology.append("  <owl:Class rdf:about=\"" + rdfClass.getFullName() + "\">\n");
            rdfOntology.append("    <rdfs:label>" + rdfClass.getShortName() + "</rdfs:label>\n");
            rdfOntology.append("    <rdfs:comment>URI over mart attributes: " + rdfClass.getURIAttributes().toString() + "</rdfs:comment>\n");
            rdfOntology.append("  </owl:Class>\n\n");

            for (RDFProperty property : rdfClass.getProperties()) {
                String attribute = property.getAttribute();
                String filter = property.getFilter();

                boolean hasAttribute = false, hasFilter = false;

                if (attribute == null || attribute.isEmpty())
                    attribute = "-";
                else
                    hasAttribute = true;

                if (filter == null || filter.isEmpty())
                    filter = "-";
                else
                    hasFilter = true;

                rdfOntology.append("  <owl:DatatypeProperty rdf:about=\"" + property.getFullName() + "\">\n");
                rdfOntology.append("    <rdfs:label>" + property.getShortName() + "</rdfs:label>\n");
                rdfOntology.append("    <rdfs:domain rdf:resource=\"" + rdfClass.getFullName() + "\" />\n");
                rdfOntology.append("    <rdfs:range rdf:resource=\"" + property.getFullRange() + "\" />\n");
                rdfOntology.append("    <rdfs:comment>Mart attribute: " +
                                        attribute +
                                        ", mart filter: " +
                                        filter +
                                        "</rdfs:comment>\n");
                if (hasFilter)
                    rdfOntology.append("    <property:filter>" + filter + "</property:filter>\n");
                if (hasAttribute)
                    rdfOntology.append("    <property:attribute>" + attribute + "</property:attribute>\n");
                rdfOntology.append("  </owl:DatatypeProperty>\n\n");
            }
        }

        rdfOntology.append("</rdf:RDF>\n");

        return rdfOntology.toString();
    }

    protected String getPrincipalMart(String format, String martName, String dataset, Query query) throws SPARQLException {
        Mart mart = new Portal(factory)._registry.getMartByConfigName(martName);

        if (mart == null)
            throw new SPARQLException(String.format(RDF_MART_UNKNOWN, martName));

        Set<String> literals = getLiterals(query);
        literals.add("");

        String ontologyUri = getOntologyUrl(martName);
        Ontology ontology = getOntology(mart, dataset, ontologyUri);

        final StringBuilder rdfOntology = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rdf:RDF\n");
        rdfOntology.append("  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
        rdfOntology.append("  xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n");
        rdfOntology.append("  xmlns:accesspoint=\"" + ontologyUri + "#\"\n");
        rdfOntology.append("  xmlns:class=\"" + site2Reference(ontologyUri) + "/class#\"\n");
        rdfOntology.append("  xmlns:dataset=\"" + site2Reference(ontologyUri) + "/dataset#\"\n");
        rdfOntology.append("  xmlns:attribute=\"" + site2Reference(ontologyUri) + "/attribute#\">\n\n");

        for (RDFClass rdfClass : ontology.getRDFClasses()) {
            rdfOntology.append("  <class:" + rdfClass.getShortName() + ">\n");

            for (RDFProperty property : rdfClass.getProperties()) {

                for (String literal : literals) {
                    rdfOntology.append("    <attribute:" + property.getShortName() + ">");
                    rdfOntology.append(literal);
                    rdfOntology.append("</attribute:" + property.getShortName() + ">\n");
                }
                
            }

            rdfOntology.append("  </class:" + rdfClass.getShortName() + ">\n\n");
        }

        rdfOntology.append("</rdf:RDF>");

        return rdfOntology.toString();
    }

    protected Ontology getOntology(Mart mart, String dataset, String ontologyUri) throws SPARQLException {
        Map<String, Ontology> dataset2Ontology = ontologyCache.get(mart);

        if (dataset2Ontology != null) {
            Ontology ontology = dataset2Ontology.get(dataset);

            if (ontology != null)
                return ontology;
        }
        
        // Include hidden attributes, filters and containers.
        List<Filter> filters = mart.getFilters(dataset, true, true);
        List<Attribute> attributes = mart.getAttributes(dataset, true);

        Ontology ontology = new Ontology();

        ontology.addNamespace("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ontology.addNamespace("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");

        // Protege may add this prefix:
        ontology.addNamespace("ontology:", ontologyUri + "#");

        // BioMart prefixes:
        ontology.addNamespace("accesspoint:", ontologyUri + "#");
        ontology.addNamespace("class:", site2Reference(ontologyUri) + "/class#");
        ontology.addNamespace("dataset:", site2Reference(ontologyUri) + "/dataset#");
        ontology.addNamespace("attribute:", site2Reference(ontologyUri) + "/attribute#");

        for (org.biomart.objects.objects.RDFClass rdfClass : mart.getRDFClasses())
            ontology.addClass(rdfClass.getName(), rdfClass.getSubClassOf(), rdfClass.getUID().split(","));

        // Look for attribute RDF-defs *after* the classes have been picked up...
        for (Attribute attribute : attributes) {
            String rdfProperty = attribute.getRDF();

            if (!attribute.getRDF().isEmpty())
                addProperty(ontology, attribute.getName(), "", rdfProperty);
        }

        // Filter settings *will* override attribute settings: they take
        // precedence. The attribute rdf-property should only be there if
        // the attribute has no filter associated with it.
        for (Filter filter : filters) {
            String rdfProperty = filter.getRDF();

            if (!rdfProperty.isEmpty())
                addProperty(ontology, filter.getAttributeName(), filter.getName(), rdfProperty);
        }

        // Fix things that are dangling around and awaiting proper configuration.
        ontology.freeze();

        return ontology;
    }

    private void addProperty(Ontology ontology,
                String attributeName,
                String filterName,
                String rdfString
            ) throws SPARQLException {
        if (!rdfString.isEmpty()) {
            String[] definitions = rdfString.split("\\|");

            for (String definition : definitions) {
                definition = definition.trim();

                String[] objectPath = definition.split(";");

                if (objectPath.length != 3) {
                    String name = filterName;

                    if (name.isEmpty())
                        name = attributeName;

                    throw new SPARQLException(String.format(RDF_MC_PROPERTY_SYNTAX_ERROR, name));
                }

                ontology.addProperty(
                        attributeName,
                        filterName,
                        objectPath[0], // class name
                        objectPath[1], // property
                        objectPath[2]  // range
                    );
            }
        }
    }
}
