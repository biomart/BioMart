package org.biomart.dino.dinos.enrichment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.resources.Log;
import org.biomart.dino.Binding;
import org.biomart.dino.Utils;
import org.biomart.dino.annotations.EnrichmentConfig;
import org.biomart.dino.annotations.Func;
import org.biomart.dino.cache.AnnotationCallback;
import org.biomart.dino.cache.Cache;
import org.biomart.dino.cache.GeneCallback;
import org.biomart.dino.command.HypgCommand;
import org.biomart.dino.command.ShellException;
import org.biomart.dino.command.ShellRunner;
import org.biomart.dino.dinos.Dino;
import org.biomart.dino.exceptions.ConfigException;
import org.biomart.dino.querybuilder.QueryBuilder;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Element;
import org.biomart.queryEngine.Query;
import org.biomart.queryEngine.QueryElement;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * NOTE This implementation assumes the incoming query has only one attribute!
 *
 * @author luca
 *
 */
public class EnrichmentDino implements Dino {
    static public final String BACKGROUND = "background",
                               SETS = "sets",
                               ANNOTATION = "annotation",
                               CUTOFF = "cutoff",
                               BONF = "bonferroni",
                               EN_BIN_OPT = "enrichment_bin",
                               DISPL_OPT = "display",
                               GENE_OPT = "gene",
                               ANN_OPT = "annotation",
                               GENE_A_OPT = "gene_attribute",
                               ANN_A_OPT = "annotation_attribute",
                               DESC_A_OPT = "description_attribute",
                               FILT_OPT = "filter",
                               OTHER_A_OPT = "other_attributes",
                               APP_OPT = "front-end",
                               
                               FF_GENE_LIMIT = "gene_limit",
                               FF_GENE_TYPE = "gene_type",
                               FF_HOMOLOG = "homolog";

    // Key: dataset_config_name + attribute_list name
    // Value: path of the annotation file
    static private Map<String, String> 
        annotationsFilePaths = new HashMap<String, String>(),
        defaultBackgrounds = new HashMap<String, String>();
    static private Map<String, Cache> cache = new HashMap<String, Cache>();

    static ObjectMapper mapper = new ObjectMapper();
    
    static File workingDir = null;

    // NOTE: these will contain filter values and attribute names.
    @Func(id = BACKGROUND, optional = true)
    String background;
    @Func(id = SETS)
    String sets;
    @Func(id = ANNOTATION)
    String annotation;
    @Func(id = CUTOFF)
    String cutoff;
    @Func(id = BONF, optional = true)
    String bonferroni;
    
    // TODO: [1] make possible to have requirements to be a list of values
    @Func(id = FF_GENE_LIMIT, optional = true)
    String ffGeneLimit;
    @Func(id = FF_GENE_TYPE, optional = true)
    String ffGeneType;
    @Func(id = FF_HOMOLOG, optional = true)
    String ffHomolog;
    // end
    
    // This is the name of the attribute used for translating annotations to
    // ensembl ids.
    String a2Name;

    String client;
    Query q;

    HypgCommand cmd;
    ShellRunner cmdRunner;
    QueryBuilder qbuilder;
    Binding metadata;

    // Temporary files.
    File backgroundInput, setsInput;
    

    // These are dataset and configuration used for annotation retrieval
    // at the time of this request.
    String annotationDatasetName = "", annotationConfigName = "";

    Map<String, List<List<String>>> results = new HashMap<String, List<List<String>>>();

    
    // These are collections to use when the request comes from a web browser


    // Links are segregated per attribute list
    Map<String, List<Map<String, Object>>> 
        nodes = new HashMap<String, List<Map<String, Object>>>(),
        edges = new HashMap<String, List<Map<String, Object>>>();
    String idHeader = "id", typeHeader = "type", descripHeader = "description", 
            pvalueHeader = "pvalue", bpvalueHeader = "bpvalue", 
            linkSourceHeader = "source", linkTargetHeader = "target",
            termType = "term", geneType = "gene";

    OutputStream sink;
    
    // TODO set config as static and cache it.
    JsonNode config;
    GuiResponseCompiler compiler;
    Graph graph;

    @Inject
    public EnrichmentDino(HypgCommand cmd,
                          @EnrichmentConfig
                          ShellRunner cmdRunner,
                          @Named("java_api")
                          QueryBuilder qbuilder,
                          @EnrichmentConfig
                          String configPath,
                          GuiResponseCompiler compiler,
                          Graph graph) throws IOException {
        
        this.cmd = cmd;
        this.cmdRunner = cmdRunner;
        this.qbuilder = qbuilder;

        config = mapper.readTree(new File(configPath));
        
        this.compiler = compiler;
        this.graph = graph;
    }

    @Override
    public void run(OutputStream out) throws TechnicalException, IOException {
        Log.debug(this.getClass().getName() + "#run(OutputStream) invoked");
        
        // This is an ugly trick to avoid undesired html...
        if (out instanceof org.biomart.api.rest.IframeOutputStream)
            ((org.biomart.api.rest.IframeOutputStream)out).useIframe(false);

        sink = out;

        if (workingDir == null) {
            workingDir = new File(System.getProperty("biomart.dir"), ".enrichment");
            if (! workingDir.exists()) {
                if (! workingDir.mkdir()) {
                    throw new IOException("Cannot create working directory "+ workingDir.getPath());
                }
            }
        }
        
        iterate();
    }

    /**
     *
     * For each Attribute List we:
     * + create a binding on fields of this class using this attribute list and then filters and filter lists.
     * + translate filter values
     * + get annotations
     * + run enrichment and get results
     * @throws TechnicalException
     * @throws IOException
     *
     */
    private void iterate() throws IOException {
        // Interrupt if there's any problem with one of the queries...
        try {
            for (QueryElement attrList : q.getAttributeListList()) {
                    iteration(attrList);
            }
            
            if (this.isGuiClient()) {
                results = null;
                sendGuiResponse(sink);
            }
        } catch (IllegalArgumentException e) {
            sink.write("The request cannot be processed due to an internal error".getBytes());
            Log.error(e);
        } catch (IllegalAccessException e) {
            sink.write("The request cannot be processed due to an internal error".getBytes());
            Log.error(e);
        } catch (ShellException e) {
            sink.write("The request cannot be processed due to an internal error".getBytes());
            Log.error(e);
        } catch (ConfigException e) {
            sink.write(e.getMessage().getBytes());
            Log.error(e);
        } 
    }

    private void iteration(QueryElement queryAttrList) throws IllegalArgumentException, IllegalAccessException, IOException, ShellException, ConfigException {

        this.metadata.clear();

        List<Field> myFields = Binding.getAnnotatedFields(this.getClass());

        List<QueryElement> attrs = new ArrayList<QueryElement>();
        attrs.add(queryAttrList);

        List<QueryElement> filters = this.q.getFilters();

        List<QueryElement> boundAttrs, boundFilts;

        // This is necessary since the only way to get hold of filter
        // values is from a QueryElement and we have a unique method for
        // Attributes and Filters.
        // The translation isn't even expensive since there are usually
        // few Attributes.
        boundAttrs = Binding.setFieldValues(this, myFields, attrs);
        boundFilts = Binding.setFieldValues(this, myFields, filters);

        this.metadata.setBindings(myFields, boundAttrs);
        this.metadata.setBindings(myFields, boundFilts);

        // It throws a ValidationException if any field wasn't bound.
        this.metadata.checkBinding(myFields);
        
//        if ((this.sets == null || this.sets.isEmpty())) {
//            if (this.regions == null || this.regions.isEmpty()) {
//                throw new ConfigException("`sets` or `regions` filter must be provided with the query");
//            } else {
//                this.sets = this.regions;
//            }
//        }

        tasks();
    }

    private void tasks() throws IOException, ShellException, ConfigException {
        long start = System.nanoTime();
        translateFilters();
        long end = System.nanoTime();
        
        Log.info("ENRICHMENT TIMES:"+annotation+": ensembl translation took "+ ((end - start) / 1000000.0) + "ms");
        
        enrich();
        
        if (isGuiClient()) {
            handleGuiRequest();
        } else {
            handleWebServiceRequest();
        }
    }


    private void handleWebServiceRequest() throws ConfigException {
        String processor = this.q.getProcessor();
        List<List<String>> res = results.get(annotation);
        long start = System.nanoTime();
        // This modifies res as we want
        this.webServiceToAnnotationHgncSymbol(res);
        long end = System.nanoTime();
        
        Log.info("ENRICHMENT TIMES:"+annotation+": hgnc translation took "+ ((end - start) / 1000000.0) + "ms");
        
        List<String[]> ares = new ArrayList<String[]>(res.size());

        if (this.q.hasHeader())
            ares.add(new String[] { "Annotation", "Description", "P-Value", "Bonferroni p-value", "Genes" });

        for (List<String> r : res) {
            ares.add(r.toArray(new String[r.size()]));
        }

//        results.remove(res);
        res = null;
        results.remove(annotation);

            start = System.nanoTime(); 
            try {
                org.biomart.dino.Processor.runProcessor(ares, processor, q, sink);
            } catch (IllegalArgumentException e) {
                Log.error("EnrichmentDino#handleWebServiceRequest(): cannot send results: ",e);
            } catch (InstantiationException e) {
                Log.error("EnrichmentDino#handleWebServiceRequest(): cannot send results: ",e);
            } catch (IllegalAccessException e) {
                Log.error("EnrichmentDino#handleWebServiceRequest(): cannot send results: ",e);
            } catch (InvocationTargetException e) {
                Log.error("EnrichmentDino#handleWebServiceRequest(): cannot send results: ",e);
            }
            end = System.nanoTime();
            Log.info("ENRICHMENT TIMES:"+annotation+": sending the result through processor took "+ ((end - start) / 1000000.0) + "ms");
    }


    private void handleGuiRequest() throws ConfigException, IOException {
        List<List<String>> data = results.get(annotation);

        // Truncate results
        if (data.size() > 50) {
            data = data.subList(0, 50);
        }

        // Translate ensembl ids into hgnc symbols and gather further attributes
        // specified within the configuration.
        this.guiToAnnotationHgncSymbol(data);
    }
    
    
    private void sendGuiResponse(OutputStream sink) throws IOException, ConfigException {
        ByteArrayOutputStream out = null;
        try {
            mkJson(nodes, edges, sink);
            sink.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    

    private void 
    mkJson(Map<String, List<Map<String, Object>>> nodes, 
           Map<String, List<Map<String, Object>>> links, 
           OutputStream out) 
            throws JsonGenerationException, JsonMappingException, IOException {
        
        Map<String, Object> root = getScope(nodes, links);
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        
        m.writeValue(out, root);
    }
    
    
    private Map<String, Object>
    getScope(Map<String, List<Map<String, Object>>> nodes, 
             Map<String, List<Map<String, Object>>> links) {
        
        Map<String, Object> root = new HashMap<String, Object>(),
                graphs = new HashMap<String, Object>(),
                g;
        
        
        for (String ann : nodes.keySet()) {
            g = new HashMap<String, Object>();
            g.put("nodes", nodes.get(ann) == null ? new ArrayList<Object>() : nodes.get(ann));
            g.put("edges", links.get(ann) == null ? new ArrayList<Object>() : links.get(ann));
            graphs.put(ann, g);
        }
        
        root.put("graphs", graphs);
        return root;
    }


    @SuppressWarnings("unchecked")
    private void enrich() throws IOException, ShellException, ConfigException {
        long start, end;
        
        String annPath = this.getAnnotationsFilePath(ANNOTATION);
        String baseDir = System.getProperty("biomart.basedir");
        JsonNode jBin = getOpt(config, EN_BIN_OPT);
        
        File bin = new File(baseDir, jBin.asText());
        
        try {
            if (annPath.isEmpty()) {
                throw new IOException("Cannot find annotations file nor retrieve them");
            }

            if (!this.backgroundExists())
                this.useDefaultBackground();
            // The bin path is sat within the DinoModule as a constant.
            cmd.setAnnotations(new File(annPath))
                .setBackground(backgroundInput)
                .setSets(setsInput)
                .setCutoff(cutoff)
                .setCmdBinPath(bin)
                // boolean filters has "only" as truth value...
                .setBonferroni(bonferroni != null && bonferroni.equalsIgnoreCase("only"));

            start = System.nanoTime();
            List<List<String>> newResult =
                    (List<List<String>>) cmdRunner.setCmd(cmd)
                                                  .setWorkingDir(workingDir)
                                                  .run().getResults();
            end = System.nanoTime();
            
            Log.info("ENRICHMENT TIMES:"+annotation+": running hpgy took "+ ((end - start) / 1000000.0) + "ms");
            
            start = System.nanoTime();
            results.put(annotation, newResult);
            end = System.nanoTime();
            
            Log.info("ENRICHMENT TIMES:"+annotation+": parsing results took "+ ((end - start) / 1000000.0) + "ms");
            
        } finally {
            this.deleteTempFiles();
        }

    }
    
    
    private void deleteTempFiles() throws FileNotFoundException, ConfigException, IOException {
        if (backgroundInput != null) {
            String dfp = this.getDefaultBackgroundPath(), 
                   p = backgroundInput.getPath();
            if (! dfp.equals(p)) {
                backgroundInput.delete();
            }
        }
            
        if (setsInput != null) setsInput.delete();
    }
    
    
    private boolean backgroundExists() {
        return this.background != null && !this.background.isEmpty();
    }
    
    
    private void useDefaultBackground() throws ConfigException, FileNotFoundException, IOException {
        backgroundInput = new File(this.getDefaultBackgroundPath());
    }
    
    
    private String getDefaultBackgroundFileNameKey() {
        Map<String, Element> m = this.metadata.getBindings();
        Element eff = null;
    	List<String> tksName = new ArrayList<String>(10);
    	tksName.add(annotationDatasetName);
    	if ((eff = m.get(FF_GENE_LIMIT)) != null) {
    	    tksName.add(eff.getName());
    	    tksName.add(ffGeneLimit);
    	}
    	if ((eff = m.get(FF_GENE_TYPE)) != null) {
    	    tksName.add(eff.getName());
    	    tksName.add(ffGeneType);
    	}
    	if ((eff = m.get(FF_HOMOLOG)) != null) {
    	    tksName.add(eff.getName());
    	    tksName.add(ffHomolog);
    	}
        tksName.add("default_background");
        
        return StringUtils.join(tksName, "_");
    }
    
    
    private String getDefaultBackgroundPath() throws FileNotFoundException, IOException {
        String fileName = this.getDefaultBackgroundFileNameKey(), 
        	   path =  defaultBackgrounds.get(fileName);

        if (path == null) {
        	
            File defbk = new File(workingDir, fileName);
            
            if (!defbk.exists()) {
                this.initQueryBuilder();
                qbuilder.setHeader(false)
                        .setDataset(annotationDatasetName, annotationConfigName)
                        .addAttribute("ensembl_gene_id");
                this.addFfFilters(qbuilder);
                
                OutputStream out = null;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(defbk));
                    qbuilder.getResults(out);
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
                
            }
            
            path = defbk.getCanonicalPath();
            defaultBackgrounds.put(fileName, path);
        }
        
        return path;
    }
    

    private void translateFilters() throws IOException, ConfigException {

        try {
            if (this.backgroundExists()) {
                backgroundInput = File.createTempFile("background", "filter");
                translateBackgroundFilter(BACKGROUND, background, backgroundInput);
            }
            setsInput = File.createTempFile("sets", "filter");
            translateSetsFilter(SETS, sets, setsInput);

        } catch (IOException e) {
            Log.error(this.getClass().getName() + "#translateFilters() "
                    + "impossible to write on temporary file or the file is missing .", e);

            this.deleteTempFiles();

            throw e;
        }

    }

    private void translateSetsFilter(String filter, String filterValue, File outFile) throws IOException {
        FileOutputStream oStream = null;
        try {
            oStream = new FileOutputStream(outFile);
            String setName = "set";
            oStream.write((">" + setName + "\n").getBytes());
            translateSingleFilter(filter, filterValue, oStream);
            oStream.write(("<" + setName + "\n").getBytes());
        } finally {
            if (oStream != null) {
                oStream.close();
            }
        }
    }

    private void translateBackgroundFilter(String filter, String filterValue, File outFile) throws IOException {
        FileOutputStream oStream = null;
        try {
            oStream = new FileOutputStream(outFile);
            translateSingleFilter(filter, filterValue, oStream);
        } finally {
            if (oStream != null) {
                oStream.close();
            }
        }
    }

    private void translateSingleFilter(String filter, String filterValue, FileOutputStream out) {
        Map<String, Element> bind = this.metadata.getBindings();
        Element e = null;

        e = bind.get(filter);
        String filterName = e.getName();

        toEnsemblGeneId(ANNOTATION, filterName, filterValue, out);
    }

    /**
     * Submits a query for gene id translation.
     *
     * @param attributeList Attribute name that includes info mandatory for translation.
     * @param filterName Name of the filter to translate.
     * @param filterValue Value to translate.
     * @param o Stream through which send results.
     */
    private void toEnsemblGeneId(String attributeList,
                                String filterName,
                                String filterValue,
                                OutputStream o) {

        Attribute forTransAttr = null, elem = null;

        Map<String, Element> bindings = this.metadata.getBindings();

        // Get the attribute object.
        elem = (Attribute)bindings.get(attributeList);

        forTransAttr = getAttributeForIdTranslation(elem);

        // It means it didn't find any filter list or elem isn't an attribute
        // list.
        if (forTransAttr == null) {
            Log.error(this.getClass().getName()
                    + "#toEnsemblGeneId(): "
                    + "cannot get the necessary attribute needed for translation. "
                    + "Maybe " + attributeList + " is not an attribute list?");
            return;
        }

        submitToEnsemblIdQuery(forTransAttr, filterName, filterValue, o);
    }
    

    private void guiToAnnotationHgncSymbol(List<List<String>> data) throws ConfigException, IOException {

        Log.debug("guiToAnnotationHgncSymbol "+ annotation);
        
        Cache annCache = getAnnCache();
        Cache gCache = getGeneCache();

        String colDelim = "\t", annId, geneId;

        String[] geneVals, annVals, annHeader, unknownHeader;
        List<String> geneHeader;
        
        annHeader = annCache.getHeader().split(colDelim);
        unknownHeader = Arrays.copyOfRange(annHeader, 2, annHeader.length);
        geneHeader = new ArrayList<String>(Arrays.asList(gCache.getHeader().split(colDelim)));
        geneHeader.set(0, idHeader); geneHeader.set(1, descripHeader);
        
        for (List<String> line : data) {

            annVals = annCache.get(line.get(0)).split(colDelim);
            annId = annVals[0];
            
            if (!graph.containsNode(idHeader, annId)) {
        // Building the annotation node
                graph.initNode()
                    .addNodeProp(idHeader, annVals[0])
                    .addNodeProp(pvalueHeader, line.get(1))
                    .addNodeProp(bpvalueHeader, line.get(2))
                    .addNodeProp(descripHeader, annVals[1])
                    .addNodeProp(typeHeader, termType);
                for (int i = 2; i < annVals.length; ++i) {
                    graph.addNodeProp(unknownHeader[i], annVals[i]);
                }
                graph.addNode();
        // end building
            }
            

            if (line.size() > 3) {

                String[] origGenes = line.get(3).split(",");
                
                
                for (String og : origGenes) {
                    String geneLine = gCache.get(og);
                    geneVals = geneLine.split(colDelim);
                    geneId = geneVals[0];
                    if (!graph.containsNode(idHeader, geneId)) {
                // Build the gene node
                        graph.initNode();
                        for (int i = 0; i < geneVals.length; ++i) {
                            graph.addNodeProp(geneHeader.get(i), geneVals[i]);
                        }
                        graph.addNodeProp(typeHeader, geneType)
                            .addNode();
                // end building
                    }
                    Map<String, Object> edge = new HashMap<String, Object>(2);
                    edge.put(linkSourceHeader, geneId);
                    edge.put(linkTargetHeader, annId);
                    
                    graph.addEdge(edge);
                }
            }
        }
        
        nodes.put(annotation, graph.getNodes());
        edges.put(annotation, graph.getEdges());
        graph.clear();
    }
    
    
    private void handleCache() throws ConfigException, IOException {
        createGeneCacheInstance(geneCacheKey());
        createAnnotationCacheInstance(annotationCacheKey(annotation, annotationDatasetName));
    }
    
    
    private String annotationCacheKey(String annotation, String dataset) {
        return annotation + dataset;
    }
    

    private String geneCacheKey() {
        return annotationDatasetName;
    }
    
    
    private Cache getGeneCache() {
        return cache.get(geneCacheKey());
    }
    
    
    private Cache getAnnCache() {
        return cache.get(annotationCacheKey(annotation, annotationDatasetName));
    }
    
    
    private void createAnnotationCacheInstance(String key) throws ConfigException, IOException {
        
        if (cache.get(key) == null) {
            JsonNode display = getOpt(config, DISPL_OPT);
            JsonNode displayAnnOpt = getOpt(display, ANN_OPT);
            JsonNode ann = getOpt(displayAnnOpt, annotation);
            
            QueryBuilder qb = qbuilder.clone();
            initQueryBuilder(qb);
            qb.setHeader(true)
              .setDataset(annotationDatasetName, annotationConfigName)
              .addAttribute(a2Name)
//              .addAttribute(getOpt(ann, ANN_A_OPT).asText())
              .addAttribute(getOpt(ann, DESC_A_OPT).asText());
            Cache c = new Cache(qb, new AnnotationCallback()).getResults();
            cache.put(key, c);
        }
    }
    
    
    private void createGeneCacheInstance(String key) throws ConfigException, IOException {
        
        if (cache.get(key) == null) {
            JsonNode display = getOpt(config, DISPL_OPT);
            JsonNode gene = getOpt(display, GENE_OPT);
            JsonNode oa = gene.get(OTHER_A_OPT);
            
            String ga = getOpt(gene, GENE_A_OPT).asText(),
                   dg = getOpt(gene, DESC_A_OPT).asText();
            
            List<String> geneAtts = new ArrayList<String>();
            
            geneAtts.add("ensembl_gene_id"); geneAtts.add(ga); geneAtts.add(dg);
            
            if (oa.isArray()) {
                for (JsonNode el : (ArrayNode) oa) {
                    geneAtts.add(el.asText());
                }
            }
            
            
            QueryBuilder qb = qbuilder.clone();
            initQueryBuilder(qb);
            qb.setHeader(true)
              .setDataset(annotationDatasetName, annotationConfigName);
            
            for (String a : geneAtts) { qb.addAttribute(a); }
            Cache c = new Cache(qb, new GeneCallback()).getResults();
            cache.put(key, c);
        }
    }
    
    
    private JsonNode getOpt(JsonNode opts, String k) throws ConfigException {
        JsonNode n = opts.get(k);
        
        if (n == null) 
            throw new ConfigException("Cannot find "+ k + " within the configuration");
        
        return n;
    }


    private void webServiceToAnnotationHgncSymbol(List<List<String>> data) throws ConfigException {

        Log.debug("webServiceToAnnotationHgncSymbol "+ annotation);
        
        long start, end;
        
        Cache annCache = getAnnCache(), geneCache = getGeneCache();
        
        String atmp[], genes[] = null, delim = "\t";
        List<String> geneTks;
        

        for (List<String> line : data) {
            start = System.nanoTime();

            atmp = annCache.get(line.get(0)).split(delim);

            line.set(0, atmp[0]);
            
            if (line.size() > 3)
                genes = line.get(3).split(",");
            
            // Description
            if (atmp.length > 1) {
                line.add(1, atmp[1]);
            }
            
            end = System.nanoTime();
            
            Log.info("ENRICHMENT TIMES:"+annotation+": annotation translation query took "+ ((end - start) / 1000000.0) + "ms");
            if (genes != null && genes.length > 0) {

                start = System.nanoTime();
                geneTks = new ArrayList<String>(genes.length);

                for (String og : genes) {
                    String gLine = geneCache.get(og);
                    atmp = gLine.split(delim);
                    if (! geneTks.contains(atmp[0])) {
                        geneTks.add(atmp[0]);
                    }
                }
                
                line.set(line.size() - 1, StringUtils.join(geneTks, ","));
                atmp = null;
                geneTks = null;
                genes = null;
                
                end = System.nanoTime();
                Log.info("ENRICHMENT TIMES:"+annotation+": genes translation query for this annotation took "+ ((end - start) / 1000000.0) + "ms");

            }
        }
    }


//    private void submitToHgncSymbolQuery(
//                              String datasetName,
//                              String configName,
//                              String filterName,
//                              String filterValue,
//                              List<String> attributes,
//                              boolean header,
//                              OutputStream out) {
//        initQueryBuilder();
//        qbuilder.setHeader(header)
//                .setDataset(datasetName, configName)
//                .addFilter(filterName, filterValue);
//        for (String att : attributes) {
//            qbuilder.addAttribute(att);
//        }
//
//        qbuilder.getResults(out);
//    }

    private Attribute getAttributeForIdTranslation(Attribute attr) {
        return Utils.getAttributeForEnsemblGeneIdTranslation(attr);
    }

    private String getDatasetName() {
        return this.q.getQueryElementList()
                .get(0)
                .getDataset()
                .getName();
    }

    private String getConfigName() {
        return this.q.getQueryElementList()
                .get(0)
                .getConfig()
                .getName();
    }

    /**
     * It returns the absolute path to the annotation file if already present.
     * Otherwise, it gathers and put them on disk as temporary file.
     *
     * @param attributeList
     * @return a path or empty list if something went wrong.
     * @throws IOException 
     * @throws ConfigException 
     */
    private String getAnnotationsFilePath(String attributeList) throws ConfigException, IOException {
        // 1. check if already present on disk
        Attribute attrListElem = (Attribute) this.metadata.getBindings().get(attributeList);
        String datasetName = "", configName = "";

        if (isSpecieTranslation(attrListElem)) {
            datasetName = getDatasetNameForSpecieTranslation(attrListElem);
            configName = getConfigNameForSpecieTranslation(attrListElem);
        } else {
            datasetName = getDatasetName();
            configName = getConfigName();
        }

        annotationDatasetName = datasetName;
        annotationConfigName = configName;

        Attribute a2 = Utils.getAttributeForAnnotationRetrieval(attrListElem);
        a2Name = a2.getName();
        String key = annFilePathMapKey(datasetName, configName, a2Name),
               path = annotationsFilePaths.get(key);
        
        // Here because we need a2 name
        handleCache();

        if (path == null) {
            File annotationFile = new File(workingDir, key);
            path = annotationFile.getCanonicalPath();
         
            if (!annotationFile.exists()) {
                // 1.1 get annotations and put them on disk

                org.biomart.dino.SkipEmptyOutputStream oStream = null;
                try {
                    oStream = new org.biomart.dino.SkipEmptyOutputStream(new FileOutputStream(annotationFile));
                    submitAnnotationsQuery(datasetName,
                                           configName,
                                           a2.getName(),
                                           oStream);

                } catch (IOException ex) {
                    Log.error(this.getClass().getName()
                            + "#getAnnotationsFilePath(" + attributeList
                            + ") impossible to write on temporary file.", ex);
                    return "";
                } finally {
                    if (oStream != null) {
                        oStream.close();
                    }
                }
            }

            annotationsFilePaths.put(key, path);
        }

        return path;
    }


    private boolean isSpecieTranslation(Attribute attrListElem) {

        Attribute a1 = Utils.getAttributeForEnsemblGeneIdTranslation(attrListElem);

        return a1.getName().contains("homolog");
    }

    private String getDatasetNameForSpecieTranslation(Attribute attrListElem) {
        return Utils.getDatasetName(attrListElem);
    }

    private String getConfigNameForSpecieTranslation(Attribute attrListElem) {
        return Utils.getConfigName(attrListElem);
    }

    /**
     * Provides the key for the annotations to file path map.
     *
     * @param datasetConfigName
     *            Name of the dataset config the attribute list belongs to.
     * @param attributeName
     *            The name of the second attribute within the attribute list
     *            (See documentation).
     * @return Key for the annotations to file path map.
     */
    private String annFilePathMapKey(String datasetName,
                                     String datasetConfigName,
                                     String attributeName) {
        return datasetName + datasetConfigName + attributeName;
    }

    private void submitAnnotationsQuery(String dataset,
                                        String config,
                                        String attribute,
                                        OutputStream o) {
        initQueryBuilder();
        qbuilder.setDataset(dataset, config)
                .addAttribute("ensembl_gene_id")
                .addAttribute(attribute)
                .getResults(o);
    }

    
    private void addFfFilters(QueryBuilder q) {
    	// TODO: remove when [1] is done
        Map<String, Element> m = this.metadata.getBindings();
        Element ffe = null;
        if ((ffe = m.get(FF_GENE_LIMIT)) != null)
            q.addFilter(ffe.getName(), ffGeneLimit);
        if ((ffe = m.get(FF_GENE_TYPE)) != null)
            q.addFilter(ffe.getName(), ffGeneType);
        if ((ffe = m.get(FF_HOMOLOG)) != null)
            q.addFilter(ffe.getName(), ffHomolog);
        // end 
    }
    
    
    private void submitToEnsemblIdQuery(Attribute transAttr,
                                        String filterName,
                                        String filterValue,
                                        OutputStream o) {

        initQueryBuilder();
        qbuilder.setDataset(getDatasetName(), getConfigName())
        .addAttribute(transAttr.getName())
        .addFilter(filterName, filterValue);
        
        this.addFfFilters(qbuilder);
        
        qbuilder.getResults(o);
    }

    private void initQueryBuilder() {
        initQueryBuilder(qbuilder);
    }
    
    private void initQueryBuilder(QueryBuilder qb) {
        qb.init().setUseDino(false);
    }

    private ByteArrayOutputStream byteStream() {
        return new ByteArrayOutputStream();
    }

    public String toString() {
        return "EnrichmentDino(background = " + background + "  " + "sets = "
                + sets + "  " + "annotation = " + annotation + "  "
                + "cutoff = " + cutoff + "  " + "client = " + client + ")";
    }


    @Override
    public Dino setQuery(org.biomart.queryEngine.Query query) {
        q = query;
        client = this.q.getClient();

        return this;
    }

    @Override
    public Dino setMimes(String[] mimes) {
        return this;
    }

    @Override
    public Dino setMetaData(Binding md) {
        this.metadata = md;
        return this;
    }

    public Binding getMetaData() {
        return this.metadata;
    }

    public QueryBuilder getQueryBuilder() {
        return this.qbuilder;
    }

    private boolean isGuiClient() {
        return Boolean.valueOf(client) || client.equalsIgnoreCase("webbrowser");
    }

    @Override
    public Query getQuery() {
        return this.q;
    }


}









































