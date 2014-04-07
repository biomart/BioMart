package org.biomart.queryEngine;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringEscapeUtils;

import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.MartRegistry;
import org.jdom.Document;
import org.jdom.Element;

/**
 *
 * @author Jonathan Guberman, Syed Haider
 *
 * Incoming query in its raw form (XML) is first validated and interpreted here.
 *
 * TODO: strict validation of attributes and filters has to be implemented here.
 * Queries having any type of problems should not able to go further into QuerySplitter
 * and the rest of QueryEngine if they dont pass the validation here.
 * Additionally, this class should be carefully extended to throw back meaningful
 * granular exceptions about WHATS WRONG WITH THE QUERY, as opposed to a blanket
 * excpetion (INVALID QUERY)
 */
public class QueryValidator {

    private MartRegistry registryObj;
    private Document queryXMLobject;
    private List<QueryElement> queryElementList;
    private List<String> queryStartingPoints;
    private int limit;
    private String processor;
    private String dino = null;
    private List<QueryElement> originalAttributeOrder = new ArrayList<QueryElement>();
    private List<QueryElement> pseudoAttributes = new ArrayList<QueryElement>();
    private List<Integer> attributeList = new ArrayList<Integer>();
    private String client;
    private boolean hasHeader;
    private String userGroup;
    
    private boolean useDino;

    private Map<String,String> processorParams;
    private Map<String,String> processorConfig;
    
    private List<QueryElement> attributeListList = new ArrayList<QueryElement>();
    private List<QueryElement> filtersGroup = new ArrayList<QueryElement>();

    public boolean getUseDino() {
        return useDino;
    }
    
    public String getClient() {
        return client;
    }

    public boolean hasHeader() {
        return hasHeader;
    }

    public int getLimit() {
        return limit;
    }

    public String getProcessor() {
        return processor;
    }

    public List<String> getQueryStartingPoints() {
        return queryStartingPoints;
    }

    public List<QueryElement> getQueryElementList() {
        return queryElementList;
    }
    
    public boolean hasDino() {
    		Log.debug(this.getClass().getName() + "#hasDino() == "+ (dino != null && !dino.isEmpty()));
    		return dino != null && !dino.isEmpty();
    }
    
    public String getDino() {
    		Log.debug(this.getClass().getName() + "#getDino");
		return dino;
    }
    
    public List<QueryElement> getAttributeListList() {
        return this.attributeListList;
    }
    
    public List<QueryElement> getFilters() {
        return this.filtersGroup;
    }

    public QueryValidator(Document queryXMLobject, MartRegistry registryObj, String userGroup) {
        this.registryObj = registryObj;
        this.queryXMLobject = queryXMLobject;
        this.userGroup = userGroup;
    }

    public void validateProcessor() {
        final Element root = queryXMLobject.getRootElement();
        validateProcessorConfig(root);
        processorParams = buildProcessorParams(root);
        // TODO: Implement processor configs
        processorConfig = buildProcessorConfig(null);
    }

    @SuppressWarnings("unchecked")
    public void validateQuery() {
        final Element root = queryXMLobject.getRootElement();

        limit = Integer.parseInt(root.getAttributeValue("limit", "-1"));

        client = root.getAttributeValue("client", "");
        String headerAttributeValue = root.getAttributeValue("header", "");
        
        useDino = Boolean.parseBoolean(root.getAttributeValue("useDino", "true"));

        if ("1".equals(headerAttributeValue) || Boolean.parseBoolean(headerAttributeValue)) {
            hasHeader = true;
        }

        final boolean processLinkOut = processor.toUpperCase().equals("TSVX") ||
                processor.toUpperCase().startsWith("HTML");

        List<Element> datasetElements = root.getChildren("Dataset");

        if (datasetElements.isEmpty()) {
            throw new ValidationException("No datasets found in query XML");
        }

        boolean isFirst = true;
        String firstName = null;
        queryStartingPoints = new ArrayList<String>();

        // Get and iterate over each Dataset object in the XML
        queryElementList = new ArrayList<QueryElement>();

        for (Element datasetElement : datasetElements) {
            final String datasetNamesUnsplit = datasetElement.getAttributeValue("name");

            String configName = datasetElement.getAttributeValue("config");
            boolean isMultiple = false;

            if (datasetNamesUnsplit == null) {
                //throw new Exception("No dataset name!");
            }
            String datasetNames[] = datasetNamesUnsplit.split(",");
            Map<Dataset,List<org.biomart.objects.objects.Element>> datasets =
                    new HashMap<Dataset,List<org.biomart.objects.objects.Element>>();

            // Do we actually need anything from the dataset objects?
            for (String datasetName : datasetNames) {
                // TODO get dataset objects corresponding to names, populate some sort of list (ArrayList, I assume)
                // Maybe a hashmap, to map attributes and filters to all of the relevant datasets
                // if(this.registryObj.getDatasetByName("anonymous", "", datasetName)!=null)
                // datasets.put(this.registryObj.getDatasetByName("anonymous", "", datasetName), new ArrayList<org.biomart.objects.objects.Element>());

                Dataset curDataset = registryObj.getDatasetByName(datasetName, configName);

                if (curDataset != null) {
                    datasets.put(curDataset, new ArrayList<org.biomart.objects.objects.Element>());
                    if (isFirst) {
                        firstName = curDataset.getDisplayName();
                    }
                }
            }

            if (datasetNames.length > 1) {
                if (!queryStartingPoints.isEmpty()) {
                    // There are already multiple starting datasets, so we can't have another one: throw an error
                    //TODO throw an error
                } else {
                    isMultiple = true; // This flag allows us to put the attributes and filters for the starting data-points first
                    for (Dataset dataset : datasets.keySet()) {
                        queryStartingPoints.add(dataset.getDisplayName());
                    }
                }
            }

            List<Element> attributesXML = datasetElement.getChildren("Attribute");
            
            for (Element attributeXML : attributesXML) {
                List<String> linkAttributeNames = new ArrayList<String>();
                Set<String> seenAttributes = new HashSet<String>();
                Map<String,QueryElement> pseudoAttributeMap = new HashMap<String,QueryElement>();

                for (final Dataset dataset : datasets.keySet()) {
                    if (configName == null) {
                        configName = dataset.getParent().getName();
                    }
                    String name = StringEscapeUtils.escapeSql(attributeXML.getAttributeValue("name"));
                    Attribute attribute = dataset.getAttributeByName(name, configName, userGroup);

                    if (attribute == null) {
                        throw new ValidationException("Attribute " + attributeXML.getAttributeValue("name") + " not found in " + dataset.getDisplayName());
                    }

                    if (attribute.isAttributeList()) {
                        attributeListList.add(new QueryElement(attribute, dataset));
                        for (Attribute subAttribute : attribute.getAttributeList()) {
                            processAttribute(seenAttributes, pseudoAttributeMap, dataset, subAttribute, isMultiple, processLinkOut, linkAttributeNames);
                        }
                    } else {
                        processAttribute(seenAttributes, pseudoAttributeMap, dataset, attribute, isMultiple, processLinkOut, linkAttributeNames);
                    }
                }

                seenAttributes.clear();
                pseudoAttributeMap.clear();

                // This if loop adds the attributes that are present in the linkOutURL property of the just-added attribute
                // Note that special handling is required for pseudo-attributes present in the links to work properly
                // The entire block is essentially a repetition of the code for normal attributes immediately above
                // The list of linkAttributes must be created in the previous loop because of the presence of the dataset object, which are ignored below
                if (processLinkOut) {
                    for (String linkAttributeName : linkAttributeNames) {
                        for (final Dataset dataset : datasets.keySet()) {

                            Attribute linkAttribute = dataset.getAttributeByName(linkAttributeName, configName, userGroup);

                            if (linkAttribute == null) {
                                throw new ValidationException("Link attribute " + linkAttributeName + " from " + attributeXML.getAttributeValue("name") + " not found in " + dataset.getDisplayName());
                            }
                            if (linkAttribute.isAttributeList()) {
                                throw new ValidationException("Link attribute cannot be a list: " + linkAttributeName);
                            }

                            processAttribute(seenAttributes, pseudoAttributeMap, dataset, linkAttribute, isMultiple, false, null);
                        }
                    }
                }

                seenAttributes.clear();
                pseudoAttributeMap.clear();
            }

            // Check and add filters
            List filtersXML = datasetElement.getChildren("Filter");
            Iterator filtersXMLIterator = filtersXML.iterator();
            QueryElement ff;
            // Get and iterate over the filter objects in the XML
            while (filtersXMLIterator.hasNext()) {
                Element filterXML = (Element) filtersXMLIterator.next();
                for (Dataset dataset : datasets.keySet()) {
                	
                		dino = dataset.getDino(configName);
                	
                    String name = StringEscapeUtils.escapeSql(filterXML.getAttributeValue("name"));
                    String value = StringEscapeUtils.escapeSql(filterXML.getAttributeValue("value"));
                    if (value == null) {
                        value = StringEscapeUtils.escapeSql(filterXML.getText());
                    }

                    Filter filter = dataset.getFilterByName(name, configName, userGroup);

                    //LinkIndices test = new LinkIndices(this.registryObj, filter, dataset.getDisplayName()());

                    if (filter == null) {
                        throw new ValidationException("Filter " + name + " not found in " + dataset.getDisplayName());
                    }
                    if (isMultiple) {
                        queryElementList.add(0, ff = new QueryElement(filter, value, dataset));
                    } else {
                        queryElementList.add(ff = new QueryElement(filter, value, dataset));
                    }
                    
                    filtersGroup.add(ff);
                }
            }
            isFirst = false;
        }
        if (queryStartingPoints.isEmpty()) {
            queryStartingPoints.add(firstName);
        }
    }

    private QueryElement processAttribute(final Set<String> seenAttributes, 
                final Map<String,QueryElement> pseudoAttributeMap, final Dataset dataset, final Attribute attribute,
                final boolean isMultiple, final boolean processLinkOut, final List<String> linkAttributeNames) {

        final boolean isPseudoAttribute = !"".equals(attribute.getValue());
        final boolean isNew = !seenAttributes.contains(attribute.getName());
        QueryElement pseudoAttObj = pseudoAttributeMap.get(attribute.getName());

        if (isMultiple) {
            queryElementList.add(0, new QueryElement(attribute, dataset));
        } else {
            queryElementList.add(new QueryElement(attribute, dataset));
        }

        // add the atts only once if its a Union, not as many times as number of datasets in union
        if (isNew) {
            seenAttributes.add(attribute.getName());

            if (isPseudoAttribute) {
                pseudoAttObj = new QueryElement(attribute, dataset);
                if (!this.pseudoAttributes.contains(pseudoAttObj)) {
                    pseudoAttributeMap.put(attribute.getName(), pseudoAttObj);
                    pseudoAttributes.add(pseudoAttObj);
                }
            }

            attributeList.add(attribute.getAttributeList(new ArrayList<String>(){{add(dataset.getName());}},true).size());
            originalAttributeOrder.add(new QueryElement(attribute, dataset));

            // Add all of the linkAttributes for this attribute to the set of linkAttributes, for use in the code below outside the loop
            if (processLinkOut) {
                String[] splitLinkOut = attribute.getLinkOutUrl(dataset.getName()).split("%", -1);
                if ((splitLinkOut.length % 2) == 0) {
                    Log.error("Wrong number of delimiters in linkoutURL");
                    throw new ValidationException("wrong number of delimiters in linkoutURL!");
                } else {
                    for (int i = 1; (i + 1) < splitLinkOut.length; i += 2) {
                        if (!splitLinkOut[i].equals("s")) {
                            linkAttributeNames.add(splitLinkOut[i]);
                        }
                    }
                }
            }
        }

		if (pseudoAttObj != null) {
			pseudoAttObj.setPseudoAttributeValue(dataset);
			Log.info("PSEUDO attribute's value: " + attribute.getValue(dataset));
		}

        return pseudoAttObj;
    }

    public List<QueryElement> getOriginalAttributeOrder() {
        return originalAttributeOrder;
    }

    public List<Integer> getAttributeListSizes() {
        return attributeList;
    }

    public List<QueryElement> getPseudoAttributes() {
        return pseudoAttributes;
    }

    public Map<String,String> getProcessorParams() {
        return processorParams;
    }

    public Map<String,String> getProcessorConfig() {
        return processorConfig;
    }

    private void validateDatasets(final Element root) {
    }

    private void validateAttributes(final Element root) {
    }

    private void validateFilters(final Element root) {
    }

    private void validateQueryRestrictions(final Element root) {
    }

    private void validateLinks(final Element root) {
    }

    private void validateProcessorConfig(final Element root) {
        processor = root.getAttributeValue("processor");

        if (processor == null) {
            processor = root.getAttributeValue("formatter");
        }

        Element processorEl = root.getChild("Processor");

        if (processor == null && processorEl != null) {
            processor = processorEl.getAttributeValue("name");
        }

        if (processor == null) {
            processor = "TSV";
        }
    }

    private Map<String,String> buildProcessorParams(final Element root) {
        final Element processorEl = root.getChild("Processor");
        final ImmutableMap.Builder<String,String> builder =
                new ImmutableMap.Builder<String,String>();

        if (processorEl != null) {
            for (Element el : (List<Element>)processorEl.getChildren("Parameter")) {
                builder.put(el.getAttributeValue("name"), el.getAttributeValue("value"));
            }
        }

        return builder.build();
    }

    private Map<String,String> buildProcessorConfig(final Config config) {
        final ImmutableMap.Builder<String,String> builder =
                new ImmutableMap.Builder<String,String>();

        if (config == null) {
            return Collections.EMPTY_MAP;
        }

        final Map<String,String> map = config.getProcessorConfig();

        if (!map.isEmpty()) {
            for (String key : map.keySet()) {
                builder.put(key, map.get(key));
            }
        }

        return builder.build();
    }

    protected void setQueryDocument(Document doc) {
        queryXMLobject = doc;
    }
}
