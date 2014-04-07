package org.biomart.backwardCompatibility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.biomart.common.exceptions.backwardscompatibility.BackwardsCompatibilityException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.objects.enums.FilterOperation;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Exportable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.ForeignKey;
import org.biomart.objects.objects.Importable;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.objects.PrimaryKey;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.RelationTarget;
import org.biomart.queryEngine.OperatorType;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

public class BackwardCompatibility {
	private Element allRoot = new Element("options");
	
	private HashMap<String, HashMap<String, String>> aliases = new HashMap<String, HashMap<String,String>>();
	
	private List<String> tableList = null;
	
	private HashMap<String, HashSet<String>> columnList = new HashMap<String, HashSet<String>>();
	
	private HashMap<String, HashSet<Filter>> filterPushedBy;

	private URL currentDatasetURL;
	
	private DatasetColumn createColumn(String name, DatasetTable table, String datasetName){
		DatasetColumn newColumn = new DatasetColumn(table, name);
		addToPartition(newColumn, datasetName);
		table.addColumn(newColumn);
		return newColumn;
	}
	
	private DatasetTable createTable(Mart mart, String name, DatasetTableType type, String datasetName) throws BackwardsCompatibilityException{
		String partitionedName = getPartitionedName(name);
		if(partitionedName == null){
			Log.warn("Couldn't create table named " + name);
			return null;
		}
		DatasetTable newTable = new DatasetTable(mart, partitionedName, type);
		if(mart.getDatasetByName(checkDatasetName(name, datasetName)) == null)
			throw new BackwardsCompatibilityException("Problem creating table " + name + ": No dataset named " + datasetName + "is present in the mart");
		addToPartition(newTable, checkDatasetName(name, datasetName));
		return newTable;
	}
	
	private void addToPartition(DatasetTable table, String datasetName){
		if(isWebService || this.tableList == null) {
			table.addInPartitions(datasetName);
		}
		else if (this.tableList.contains(table.getName().replace("(p0c5)",datasetName))){
			table.addInPartitions(datasetName);
		}
	}
	
	private void addToPartition(DatasetColumn column, String datasetName){
		String tableName = column.getTable().getName().replace("(p0c5)",datasetName);
		if(isWebService || this.tableList == null) {
			column.addInPartitions(datasetName);
		}
		else if (this.tableList.contains(tableName)){
			HashSet<String> currentColumnList = this.columnList.get(tableName);
			if(currentColumnList == null){
				currentColumnList = getColumns(tableName);
				this.columnList.put(tableName, currentColumnList);
			}
			if(currentColumnList.contains(column.getName()))
				column.addInPartitions(datasetName);
			else
				Log.debug("Column " + column.getName() + " not found in table " + tableName + " in dataset " + datasetName + ". Checking case insensitive variations.");
				for(String dbColumn : currentColumnList){
					if(dbColumn.equalsIgnoreCase(column.getName())){
						column.setName(dbColumn);
						Log.debug("Found match: " + dbColumn);
						column.addInPartitions(datasetName);
						break;
					}
					Log.debug("No matching column found.");
				}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createMainTables(Element rootXML, Mart mart, String datasetName) throws BackwardsCompatibilityException{
		List<Element> mainTables = rootXML.getChildren("MainTable");
		List<Element> mainTableKeys = rootXML.getChildren("Key");
		
		// XML must specify at least one main table, and the number of main tables must equal the number of keys
		if((mainTables.size() != mainTableKeys.size()) || mainTables.size() == 0){
			throw new BackwardsCompatibilityException("FATAL ERROR: Problem with MainTables and/or Keys. Please check XML");
		}
		
		// Set up the main table and primary key. We are guaranteed that these are here by the previous check
		DatasetTable mainTable = mart.getTableByName(getPartitionedName(mainTables.get(0).getText()));
		if(mainTable == null)
			mainTable = createTable(mart, mainTables.get(0).getText(), DatasetTableType.MAIN, datasetName);
		else
			addToPartition(mainTable, datasetName);

		DatasetColumn mainPKColumn = createPartitionedColumn(mainTableKeys.get(0).getText(), mainTable, datasetName);
		
		PrimaryKey mainPK = mainTable.getPrimaryKey();
		if(mainPK == null){
			mainPK = new PrimaryKey(mainPKColumn);
			mainTable.setPrimaryKey(mainPK);
		}
		
		PrimaryKey lastPK = mainPK;
		
		// Now add all the submains, if they exist, in order
		for(int i = 1; i < mainTables.size(); ++i){
			DatasetTable subMainTable = mart.getTableByName(getPartitionedName(mainTables.get(i).getText()));
			if(subMainTable == null) 
				subMainTable = createTable(mart, mainTables.get(i).getText(), DatasetTableType.MAIN_SUBCLASS, datasetName);
			else
				addToPartition(subMainTable, datasetName);
			
			DatasetColumn subMainPKColumn = createPartitionedColumn(mainTableKeys.get(i).getText(), subMainTable, datasetName);
			PrimaryKey subMainPK = subMainTable.getPrimaryKey();
			if(subMainPK == null){
				subMainPK = new PrimaryKey(subMainPKColumn);
				subMainTable.setPrimaryKey(subMainPK);
			}
			
			DatasetColumn subMainFKColumn = createPartitionedColumn(mainTableKeys.get(i-1).getText(), subMainTable, datasetName);
			
			ForeignKey subMainFK = new ForeignKey(subMainFKColumn);
			subMainTable.addForeignKey(subMainFK);
			
			if(!Relation.isRelationExist(lastPK, subMainFK))
				// Create a relation between this and the last main table
				new RelationTarget(lastPK, subMainFK, Cardinality.MANY_A);
			
			// Set the current primary key as the "last" primary key
			lastPK = subMainPK;
		}
	}
	
	private String getPartitionedName(String originalName) throws BackwardsCompatibilityException{
		if(this.datasetList != null && this.datasetList.size()==1)
			return originalName;
		String[] splitName = originalName.split("__");
		if(splitName.length == 3)
			return "(p0c5)__" + originalName.split("__", 2)[1];
		if(splitName.length == 2)
			return "(p0c5)__" + originalName;
		if(splitName.length == 1)
			return "(p0c5)";
			//throw new BackwardsCompatibilityException("The name " + originalName + " is invalid: no double underscores");
		else {
			Log.warn("The name " + originalName + " is invalid: too many double underscores");
			return null;
		}
			//throw new BackwardsCompatibilityException("The name " + originalName + " is invalid: too many double underscores");
	}
	
	private String checkDatasetName(String originalName, String datasetName) throws BackwardsCompatibilityException{
		if(this.datasetList != null && this.datasetList.size()==1)
			return datasetName;
		String[] splitName = originalName.split("__");
		if(splitName.length == 3){
			if(originalName.split("__", 2)[0].toLowerCase().equals(datasetName.toLowerCase()))
				return datasetName;
			else
				throw new BackwardsCompatibilityException("The name " + originalName + " does not match the dataset name " + datasetName);
		}
		if(splitName.length == 2 || splitName.length == 1)
			return datasetName;
		//if(splitName.length == 1)
		//	throw new BackwardsCompatibilityException("The name " + originalName + " is invalid: no double underscores");
		else
			throw new BackwardsCompatibilityException("The name " + originalName + " is invalid: too many double underscores");
	}
	
	private List<Mart> createMartList() {
		List<Mart> martList = new ArrayList<Mart>();
		
		HashMap<DatasetFromUrl, Document> datasetTemplates = new HashMap<DatasetFromUrl, Document>();
		HashSet<String> datasetNames = new HashSet<String>();
		if(isWebService){
			for(DatasetFromUrl dataset : this.datasetList){
				try{	
					SAXBuilder builder = new SAXBuilder();
					Document document = null;
					URL url = new URL( dataset.getUrl());
					document = builder.build(url);
										
					datasetTemplates.put(dataset, document);
					datasetNames.add(dataset.getName());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (JDOMException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			
			}
			martList.add(createMart(datasetNames, datasetTemplates));
		} else { //DB source
			HashSet<String> tableQuery = new HashSet<String>();
			tableList = new ArrayList<String>();
			HashMap<String, byte[]> xmlList = new HashMap<String, byte[]>();

			try {
				ResultSet result = this.databaseConnection.getMetaData().getTables(null, this.schema, null, null);
				while (result.next()){
					//System.out.println(result.getString(1));
					tableQuery.add(result.getString(3));
				}

				//tableQuery = queryDB("SHOW TABLES");
				for (String table : tableQuery) {
					if (!table.startsWith("meta") && (table.endsWith("__main") || table.endsWith("__dm"))) {
						tableList.add(table);
					}
				}
				// Get the list of template names
				Statement stmt = null;
				stmt = databaseConnection.createStatement();
				result = stmt.executeQuery("SELECT * FROM meta_template__template__main");

				StringBuilder templateInListQuery = new StringBuilder(
				"SELECT * FROM meta_template__xml__dm WHERE template IN (");
				while (result.next()) {
					String templateName = result.getString("template");
					templateInListQuery.append("'" + templateName + "',");
				}
				// For all the templates present in the template__main table, get the GZipped XML
				templateInListQuery.delete(templateInListQuery.length() - 1,
						templateInListQuery.length());
				templateInListQuery.append(")");
				//System.out.println(templateInListQuery);
				xmlList = queryDBbytes(templateInListQuery.toString());
				//System.err.println("Data retrieved.");
				
				for(String xmlGZkey : xmlList.keySet()){					
					this.aliases = new HashMap<String, HashMap<String,String>>();
					datasetTemplates = new HashMap<DatasetFromUrl, Document>();
					
					InputStream rstream = null;
					SAXBuilder builder = new SAXBuilder();
					Document document = null;
					byte[] xmlGZ = xmlList.get(xmlGZkey);

					rstream = new GZIPInputStream(new ByteArrayInputStream(xmlGZ));
					InputSource is = new InputSource(rstream);
					document = builder.build(is);
					
					datasetNames = parseDynamicDatasets(document);

					for(String datasetName : datasetNames){
						rstream = new GZIPInputStream(new ByteArrayInputStream(xmlGZ));
						is = new InputSource(rstream);
						document = builder.build(is);
						document.getRootElement().removeChildren("Key");
						document.getRootElement().removeChildren("MainTable");

						if(true /*datasetName.endsWith(xmlGZkey)*/){
							HashMap<Integer, String> mainTableOrder = new HashMap<Integer, String>();
							HashMap<Integer, HashSet<String>> mainTableColumns = new HashMap<Integer, HashSet<String>>();
							for(String table : tableList){
								if(table.startsWith(datasetName) && table.endsWith("__main")){
									HashSet<String> columns  = getColumns(table);
									// Find keys
									HashSet<String> keySet = new HashSet<String>();
									for(String columnName : columns){
										if(columnName.endsWith("_key")){
											keySet.add(columnName);
										}
									}
									Integer position = keySet.size() - 1;
									mainTableOrder.put(position, table);
									mainTableColumns.put(position, columns);
								}
							}

							for(Integer i = 0; i< mainTableOrder.size(); ++i){
								Element mainTable = new Element("MainTable");
								mainTable.addContent(mainTableOrder.get(i));
								document.getRootElement().addContent(mainTable);
							}
							for(Integer i = 0; i < mainTableColumns.size(); ++i){
								HashSet<String> currentMainColumns = mainTableColumns.get(i);
								if(i > 0)
									currentMainColumns.removeAll(mainTableColumns.get(i-1));
								for(String columnName : currentMainColumns){
									if(columnName.endsWith("_key")){
										Element keyColumn = new Element("Key");
										keyColumn.addContent(columnName);
										document.getRootElement().addContent(keyColumn);
										break;
									}
								}
								if(i > 0)
									currentMainColumns.addAll(mainTableColumns.get(i-1));
							}

							DatasetFromUrl dataset = new DatasetFromUrl();
							dataset.setName(datasetName);
							datasetTemplates.put(dataset, document);
						}		
					}
					martList.add(createMart(datasetNames, datasetTemplates));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return martList;
	}

	
	private Mart createMart(HashSet<String> datasetNames, HashMap<DatasetFromUrl, Document> datasetTemplates){
		String templateName = findCommonSuffix(datasetNames);
		if(!templateName.equals("")){
			templateName = templateName.substring(templateName.indexOf("_")+1);
		} else if (datasetNames.size()==1){
			templateName = datasetNames.iterator().next();
		}else{
			templateName = this.martRegistry.getName();
		}
		
		Mart mart = new Mart(this.martRegistry, templateName, null);
		mart = createSchema(mart, datasetTemplates);
		try {
			mart = createConfig(mart, datasetTemplates);
		} catch (BackwardsCompatibilityException e) {
			e.printStackTrace();
		}
		
		return mart;
	}
	
	@SuppressWarnings("unchecked")
	private HashSet<String> parseDynamicDatasets(Document document) {
		HashSet<String> datasetNames = new HashSet<String>();
		List dynamicDatasetList = document.getRootElement().getChildren("DynamicDataset");

		Iterator dynamicDatasetIterator = dynamicDatasetList.iterator();

		while(dynamicDatasetIterator.hasNext()){
			Element dynamicDataset = (Element) dynamicDatasetIterator.next();
			HashMap<String, String> currentAliases = new HashMap<String, String>();
			String datasetName = dynamicDataset.getAttributeValue("internalName");
			datasetNames.add(datasetName);
			String[] aliases = dynamicDataset.getAttributeValue("aliases","").split(",");
			for(String alias : aliases){
				String[] splitAlias = alias.split("=",-1);
				currentAliases.put(splitAlias[0], splitAlias[1]);
			}
			this.aliases.put(datasetName,currentAliases);
		}
		return datasetNames;
	}
	
	private String processAliases(String inputString, String datasetName){
		HashMap<String, String> currentAliases = this.aliases.get(datasetName);
		String newString = inputString;
		if(currentAliases != null){
			for(String key : currentAliases.keySet()){
				newString = newString.replace("*" + key + "*", currentAliases.get(key));
			}
		}
		return newString;
	}
	
	private String parseAliasesToPartitionColumn(String inputString, Config config){
		if(inputString == null)
			return null;
		StringBuilder newString = new StringBuilder();
		
		String[] splitString = inputString.split("\\*",-1);
		for(int i = 0; i < splitString.length; i++){
			if(i % 2 == 0){
				newString.append(splitString[i]);
			} else {
				org.biomart.objects.objects.Attribute pseudo = config.getAttributeByInternalName(splitString[i], null);
				if(pseudo != null){
					newString.append(pseudo.getValue());
				} else {
					newString.append("*" + splitString[i] + "*");
				}
			}
		}
		return newString.toString();
	}

	private Mart createConfig(Mart mart, HashMap<DatasetFromUrl, Document> datasetTemplates) throws BackwardsCompatibilityException{
		this.linkOutURLpartition = new HashMap<org.biomart.objects.objects.Attribute, HashMap<String,String>>();
		this.pointerFilterDatasetPartition = new HashMap<Filter, HashMap<String,String>>();
		this.pointerAttributeDatasetPartition = new HashMap<org.biomart.objects.objects.Attribute, HashMap<String,String>>();
		this.filterPushedBy = new HashMap<String, HashSet<Filter>>();
		
		HashSet<String> datasetNames = new HashSet<String>();
		for(DatasetFromUrl dataset: datasetTemplates.keySet()){
			datasetNames.add(dataset.getName());
		}
		
		String templateName = findCommonSuffix(datasetNames);
		templateName = templateName.equals("") ? "unknown" : templateName.substring(templateName.indexOf("_")+1);
		
		Config config = new Config(templateName);
		mart.addConfig(config);
		config.setProperty(XMLElements.METAINFO, "");
		config.setProperty(XMLElements.DATASETDISPLAYNAME,"(p0c7)");
		config.setProperty(XMLElements.DATASETHIDEVALUE,"(p0c6)");
		
		
		Container rootContainer = config.getRootContainer();
		config.addRootContainer(rootContainer);
		
		for(DatasetFromUrl dataset : datasetTemplates.keySet()){
			String datasetName = dataset.getName();
			Element rootXML = datasetTemplates.get(dataset).getRootElement();
			
			config.setHideValue(rootXML.getAttributeValue("visible","1").equals("0") || rootXML.getAttributeValue("visible","1").equals("false"));
			try {
				this.currentDatasetURL = new URL(dataset.getUrl());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			createAttributeHierarchy(datasetName, rootXML, config);
			createFilterHierarchy(datasetName, rootXML, config);
			createImportables(datasetName, rootXML, config);
			createExportables(datasetName, rootXML, config);
		}
		renameFilters(config.getRootContainer());
		renameContainersWithMax(config.getRootContainer());
		setFilterDependsOn(config);
		populatePartitionTable(config);
		McGuiUtils.INSTANCE.sortPartitionTable(mart.getPartitionTableByName("p0"), 7, true);
		
		return mart;
	}
	
	private void renameContainersWithMax(Container rootContainer) {
		if (!rootContainer.isHidden()){
			List<Container> subContainers = rootContainer.getContainerList();
			if(subContainers.size() == 0){
				Integer max = rootContainer.getMaxAttributes();
				if(max > 0)
					rootContainer.setDisplayName(rootContainer.getDisplayName() + " (max " + max.toString() + ")");
			} else {
				for(Container subContainer: subContainers){
					renameContainersWithMax(subContainer);
				}
			}
		}
	}

	private void setFilterDependsOn(Config config) {
		for(String pushedFilterName : this.filterPushedBy.keySet()){
			Filter pushedFilter = config.getFilterByName(pushedFilterName, null);
			if(pushedFilter!=null){
				for(Filter pushingFilter: this.filterPushedBy.get(pushedFilterName)){
					pushedFilter.setDependsOn(pushingFilter.getName());
					setPushedFilterType(pushedFilter);
				}
			}
		}
	}
	
	private void setPushedFilterType(Filter pushedFilter){
		if(pushedFilter!=null){
			FilterType pushedFilterType = pushedFilter.getFilterType();
			switch(pushedFilterType){
			case MULTISELECT:
			case MULTISELECTUPLOAD:
			case UPLOAD:
			case MULTISELECTBOOLEAN:
				pushedFilter.setFilterType(FilterType.MULTISELECT);
				break;
			default:
				pushedFilter.setFilterType(FilterType.SINGLESELECT);
				break;
			}
		}
	}

	private void renameFilters(Container container) {
		List<Container> childContainers = new ArrayList<Container>(container.getContainerList());
		for(Container childContainer : childContainers){
			if (!(childContainer.isHidden())){
				if(childContainer.getContainerList().size() == 0){
					if(renameChildFilters(childContainer)){
						for(Filter filter: new ArrayList<Filter>(childContainer.getFilterList())){
							childContainer.removeFilter(filter);
							container.addFilter(filter);
						}
					}
				}
				if(childContainer.isEmpty()){
					container.removeContainer(childContainer);
				} else {
					renameFilters(childContainer);
				}
			}
		}
	}
	
	private boolean renameChildFilters(Container filterContainer) {
		List<Filter> originalList = new ArrayList<Filter>(filterContainer.getFilterList());

		if(originalList.size() == 0)
			return false;

		List<Filter> renameList = new ArrayList<Filter>();		
		String newName = filterContainer.getDisplayName();

		for(Filter filter : filterContainer.getFilterList()){
			if(!filter.isHidden())
				renameList.add(filter);
		}

		if(renameList.size() == 1){
			renameList.get(0).setDisplayName(newName);
		} else {
			for(Filter filter: originalList){
				if(!(filter.getDisplayName().startsWith(newName + " - ")))
					filter.setDisplayName(newName + " - " + filter.getDisplayName());
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void createImportables(String datasetName, Element rootXML,
			Config config) {
		List importableList = rootXML.getChildren("Importable");
		Iterator importableIterator = importableList.iterator();
		while (importableIterator.hasNext()) {
			Element importableXML = (Element) importableIterator.next();
			
			String name = importableXML.getAttributeValue("linkName");
			String[] filters = importableXML.getAttributeValue("filters","").split(",");
			if(name!=null){
				Importable importable = new Importable(config, name);
				
				for(String filterName: filters){
					Filter filter = config.getFilterByName(filterName, null);
					if(filter!= null){
						importable.addFilter(filter);
					} else {
						Log.warn("Filter " + filterName + " not included in importable " + name + " because it does not exist. Please check the original config.");
					}
				}
				importable.setProperty(XMLElements.ORDERBY, importableXML.getAttributeValue("orderBy",""));
				importable.setLinkVersion(importableXML.getAttributeValue("linkVersion", ""));
				importable.setProperty(XMLElements.TYPE, importableXML.getAttributeValue("type","link"));
				config.addElementList(importable);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createExportables(String datasetName, Element rootXML,
			Config config) {
		List exportableList = rootXML.getChildren("Exportable");
		Iterator exportableIterator = exportableList.iterator();
		while (exportableIterator.hasNext()) {
			Element exportableXML = (Element) exportableIterator.next();
			
			String name = exportableXML.getAttributeValue("linkName");
			String[] attributes = exportableXML.getAttributeValue("attributes","").split(",");
			if(name!=null){
				Exportable exportable = new Exportable(config, name);
				for(String attributeName: attributes){
					org.biomart.objects.objects.Attribute attribute = config.getAttributeByName(attributeName, null);
					if(attribute!=null){
						exportable.addAttribute(attribute);
					} else {
						Log.warn("Attribute " + attributeName + " not included in exportable " + name + " because it does not exist. Please check the original config.");
					}
				}
				exportable.setProperty(XMLElements.ORDERBY, exportableXML.getAttributeValue("orderBy",""));
				exportable.setLinkVersion(exportableXML.getAttributeValue("linkVersion", ""));
				exportable.setProperty(XMLElements.TYPE, exportableXML.getAttributeValue("type","link"));
				config.addElementList(exportable);
			}
		}
	}

	private void populatePartitionTable(Config config) {
		PartitionTable p0 = config.getMart().getPartitionTableByName("p0");
		
		// Do linkOutURLs
		for(org.biomart.objects.objects.Attribute attribute : this.linkOutURLpartition.keySet()){
			attribute.setLinkOutUrl(optimizePartitionValues(p0,this.linkOutURLpartition.get(attribute)));
		}
		// Pointer filter datasets
		for(Filter filter : this.pointerFilterDatasetPartition.keySet()){
			filter.setPointedDatasetName(optimizePartitionValues(p0,this.pointerFilterDatasetPartition.get(filter),true));
		}
		// Pointer attribute datasets
		for(org.biomart.objects.objects.Attribute attribute : this.pointerAttributeDatasetPartition.keySet()){
			attribute.setPointedDatasetName(optimizePartitionValues(p0, this.pointerAttributeDatasetPartition.get(attribute),true));
		}
	}
	
	private String optimizePartitionValues(PartitionTable pt, HashMap<String, String> rawPartitionTable){
		return optimizePartitionValues(pt, rawPartitionTable, false);
	}

	private String optimizePartitionValues(PartitionTable pt, HashMap<String, String> rawPartitionTable, boolean singletonsInPT){
		Collection<String> values = rawPartitionTable.values();
		// The singletonsInPT value determines whether values that only appear in one dataset are placed in the partition table or not.
		//  This is important for pointer filters/attributes, which will otherwise be displayed in ALL datasets.
		if(new HashSet<String>(values).size() == 1 && !(singletonsInPT && values.size() == 1) ){
			return (values.iterator().next());
		} else if(!(values.contains(""))){
			String prefix;
			String suffix;
			HashMap<String, String> optimizedColumn ;
			
			if(new HashSet<String>(values).size() == 1){
				prefix = "";
				suffix = "";
				optimizedColumn = rawPartitionTable;
			} else {
				prefix = findCommonPrefix(values);
				suffix = findCommonSuffix(values);
				optimizedColumn = optimizePartitionColumn(rawPartitionTable, prefix, suffix);
			}
			
			for(Integer column = 0; column < pt.getTotalColumns(); ++column){
				HashMap<String, String> curCol = pt.getNonEmptyHashCol(column);
				curCol.keySet().retainAll(optimizedColumn.keySet());
				
				// Check if any of the existing columns are consistent with the new optimized column
				if(optimizedColumn.entrySet().containsAll(curCol.entrySet())){
					
					// If the new column has any entries that the existing one doesn't, add those in
					optimizedColumn.keySet().removeAll(curCol.keySet());
					for(String dataset: optimizedColumn.keySet()){
						pt.setColumnByColumn(5, dataset, column, optimizedColumn.get(dataset));
					}
					return (prefix + "(p0c" + column.toString() + ")" + suffix);
				}

			}
			// No existing column similarity, so create a new column
			Integer column = pt.addColumn("");
			for(String dataset: optimizedColumn.keySet()){
				pt.setColumnByColumn(5, dataset, column, optimizedColumn.get(dataset));
			}
			return (prefix + "(p0c" + column.toString() + ")" + suffix);
		}
		return null;
	}
	
	private HashMap<String, String> optimizePartitionColumn(HashMap<String, String> originalColumn, String prefix, String suffix){
		HashMap<String, String> optimizedColumn = new HashMap<String, String>();
		
		for(String dataset : originalColumn.keySet()){
			String originalString = originalColumn.get(dataset);
			optimizedColumn.put(dataset, originalString.substring(prefix.length(), originalString.length() - suffix.length()));
		}
		
		return optimizedColumn;
	}
	
	private String findCommonPrefix(Collection<String> collection){
		List<String> list = new ArrayList<String>(collection);
		java.util.Collections.sort(list);
		String first = list.get(0);
		String last = list.get(list.size()-1);
		int index = 0;
		if(first.equals(last))
			return first;
		while( first.charAt(index) == last.charAt(index) )
			index++;
		
		
		return first.substring(0, index);
	}
	
	private String findCommonSuffix(Collection<String> collection){
		HashSet<String> reversedStrings = new HashSet<String>();
		for(String reverseString: collection){
			reversedStrings.add(new StringBuilder(reverseString).reverse().toString());
		}
		return new StringBuilder(findCommonPrefix(reversedStrings)).reverse().toString();
	}
	
	private HashMap<org.biomart.objects.objects.Attribute,HashMap<String, String>> linkOutURLpartition;
	private HashMap<Filter,HashMap<String, String>> pointerFilterDatasetPartition;
	private HashMap<org.biomart.objects.objects.Attribute,HashMap<String, String>> pointerAttributeDatasetPartition;


	@SuppressWarnings("unchecked")
	private void createAttributeHierarchy(String datasetName,
			Element rootXML, Config config) {
		Container rootContainer = config.getRootContainer();
		
		createAliasPseudoAttributes(datasetName, config, rootContainer);
		
		Container attributeContainer = config.getContainerByName("attributes_root");
		if(attributeContainer == null){	
			attributeContainer = new Container("attributes_root");
			attributeContainer.setMaxContainers(1);

			attributeContainer.setDisplayName("Attributes");
			rootContainer.addContainer(attributeContainer);
		}
		
		List attributePages = rootXML.getChildren("AttributePage");
		Iterator attributePageIterator = attributePages.iterator();
		while (attributePageIterator.hasNext()) {
			Element attributePage = (Element) attributePageIterator.next();

			if(attributePage.getAttributeValue("hidden","false").equals("false")){
				Container attributePageContainer = createContainer(attributePage, config, attributeContainer);

				List attributeGroups = attributePage.getChildren("AttributeGroup");
				Iterator attributeGroupIterator = attributeGroups.iterator();
				while (attributeGroupIterator.hasNext()){
					Element attributeGroup = (Element) attributeGroupIterator.next();
					
					if(attributeGroup.getAttributeValue("hidden","false").equals("false")){
						Container attributeGroupContainer = createContainer(attributeGroup, config, attributePageContainer);
						
						List attributeCollections = attributeGroup.getChildren("AttributeCollection");
						Iterator attributeCollectionIterator = attributeCollections.iterator();
						while (attributeCollectionIterator.hasNext()){
							Element attributeCollection = (Element) attributeCollectionIterator.next();
							
							if(attributeCollection.getAttributeValue("hidden","false").equals("false")){
								Container attributeCollectionContainer = createContainer(attributeCollection,config, attributeGroupContainer);
								
								List attributeDescriptions = attributeCollection.getChildren("AttributeDescription");
								Iterator attributeDescriptionIterator = attributeDescriptions.iterator();
								while(attributeDescriptionIterator.hasNext()){
									Element attributeDescription = (Element) attributeDescriptionIterator.next();
									if(attributeDescription.getAttributeValue("hidden","false").equals("false")){
										org.biomart.objects.objects.Attribute attribute = createAttribute(attributeDescription, config, datasetName);
										if(attribute!=null){
											attributeCollectionContainer.addAttribute(attribute);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void createAliasPseudoAttributes(String datasetName, Config config,
			Container rootContainer) {
		if(aliases.size() > 0){
			HashMap<String, String> currentAliases = aliases.get(datasetName);
			if (currentAliases != null){
				Container aliasesContainer = config.getContainerByName("aliases_container");
				if (aliasesContainer == null){
					aliasesContainer = new Container("aliases_container");
					aliasesContainer.setHideValue(true);
					aliasesContainer.setDisplayName("Aliases created by BC");
					aliasesContainer.setDescription("This container contains pseudoattributes derived from the Aliases in BioMart 0.7");

					rootContainer.addContainer(aliasesContainer);
				}
				
				PartitionTable pt = config.getMart().getPartitionTableByName("p0");
				for(String alias : currentAliases.keySet()){
					org.biomart.objects.objects.Attribute aliasAttribute = config.getAttributeByInternalName(alias, null);
					Integer columnNumber;
					if(aliasAttribute == null){
						aliasAttribute = new org.biomart.objects.objects.Attribute(alias, alias);
						columnNumber = pt.addColumn("");
						aliasAttribute.setValue("(p0c" + columnNumber.toString() +")");
						aliasesContainer.addAttribute(aliasAttribute);
					}
					String aliasValue = aliasAttribute.getValue();
					columnNumber = Integer.valueOf(aliasValue.substring(aliasValue.indexOf("c") + 1, aliasValue.indexOf(")")));
					
					pt.setColumnByColumn(5, datasetName, columnNumber, currentAliases.get(alias));
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createFilterHierarchy(String datasetName,
			Element rootXML, Config config) throws BackwardsCompatibilityException {
		Container rootContainer = config.getRootContainer();
		Container filterPageContainer = config.getContainerByName("filters");
		if(filterPageContainer == null)	
			filterPageContainer = new Container("filters");
		filterPageContainer.setDisplayName("Filters");
		rootContainer.addContainer(filterPageContainer);
		
		List filterPages = rootXML.getChildren("FilterPage");
		Iterator filterPageIterator = filterPages.iterator();
		while (filterPageIterator.hasNext()) {
			Element filterPage = (Element) filterPageIterator.next();

			if(filterPage.getAttributeValue("hidden","false").equals("false")){
				//Container filterPageContainer = createContainer(filterPage, config, rootContainer);

				List filterGroups = filterPage.getChildren("FilterGroup");
				Iterator filterGroupIterator = filterGroups.iterator();
				while (filterGroupIterator.hasNext()){
					Element filterGroup = (Element) filterGroupIterator.next();
					
					if(filterGroup.getAttributeValue("hidden","false").equals("false")){
						Container filterGroupContainer = createContainer(filterGroup, config, filterPageContainer);
						if(filterPage.getAttributeValue("hideDisplay","false").equals("true"))
							filterGroupContainer.setHideValue(true);
						
						List filterCollections = filterGroup.getChildren("FilterCollection");
						Iterator filterCollectionIterator = filterCollections.iterator();
						while (filterCollectionIterator.hasNext()){
							Element filterCollection = (Element) filterCollectionIterator.next();
							
							if(filterCollection.getAttributeValue("hidden","false").equals("false")){
								Container filterCollectionContainer = createContainer(filterCollection,config, filterGroupContainer);
								
								List filterDescriptions = filterCollection.getChildren("FilterDescription");
								Iterator filterDescriptionIterator = filterDescriptions.iterator();
								while(filterDescriptionIterator.hasNext()){
									Element filterDescription = (Element) filterDescriptionIterator.next();
									if(filterDescription.getAttributeValue("hidden","false").equals("false")){
										org.biomart.objects.objects.Filter filter = createFilter(filterDescription, config, datasetName);
										if(filter!=null){
											filterCollectionContainer.addFilter(filter);
										}
									}
								}								
							}
						}
					}
				}
			}
		}
	}
	


	private FilterType getFilterType(Element filterXML){
		FilterType filterType = FilterType.TEXT;
		String displayType = filterXML.getAttributeValue("displayType","");
		String multipleValues = filterXML.getAttributeValue("multipleValues","");
		String style = filterXML.getAttributeValue("style","");
		String type = filterXML.getAttributeValue("type","");
		
		String tableConstraint = filterXML.getAttributeValue("tableConstraint");
		
		if(displayType.equals("container")){
			if(type.startsWith("boolean")){
				if(multipleValues.equals("1")){
					filterType = FilterType.MULTISELECTBOOLEAN;
				} else {
					filterType = FilterType.SINGLESELECTBOOLEAN;
				}
			} else if( tableConstraint == null){
				filterType = FilterType.SINGLESELECTUPLOAD;
			} else  {
				filterType = FilterType.SINGLESELECT;
			}
		} else if (displayType.equals("text")){
			if(multipleValues.equals("1")){
				filterType = FilterType.UPLOAD;
			} else {
				filterType = FilterType.TEXT;
			}
		} else if (displayType.equals("list")){
			if(style.equals("radio") && type.startsWith("boolean")){
				filterType = FilterType.BOOLEAN;
			} else /*if (style.equals("menu"))*/{
				if(multipleValues.equals("1")){
					filterType = FilterType.MULTISELECT;
				} else {
					filterType = FilterType.SINGLESELECT;
				}
			}
		}
		
		
		return filterType;
	}
	
	@SuppressWarnings("unchecked")
	private Filter createFilter(Element filterXML, Config config, String datasetName) throws BackwardsCompatibilityException {
		Filter filter = null;
		org.biomart.objects.objects.Attribute attribute = null;
		String name = filterXML.getAttributeValue("internalName");
		String displayName = filterXML.getAttributeValue("displayName");
		String field = filterXML.getAttributeValue("field");
		String table = filterXML.getAttributeValue("tableConstraint");
		String key = filterXML.getAttributeValue("key");
		String displayType = filterXML.getAttributeValue("displayType","");
		String filterList = filterXML.getAttributeValue("filterList");
		String qualifier = filterXML.getAttributeValue("qualifier","=");
		
		FilterType type = getFilterType(filterXML);
		
		if(filterXML.getAttributeValue("pointerFilter")==null){
			filter = config.getFilterByName(name, null);
			if(type.equals(FilterType.MULTISELECTUPLOAD) || type.equals(FilterType.SINGLESELECTUPLOAD) || type.equals(FilterType.MULTISELECTBOOLEAN) || type.equals(FilterType.SINGLESELECTBOOLEAN)){
				// Filter list
				if(filter==null)
					filter = new Filter(type, name);

				Container filterListContainer = config.getContainerByName(name + " list container");
				if(filterListContainer == null){
					filterListContainer = new Container(name + " list container");
					filterListContainer.setHideValue(true);
					config.getRootContainer().addContainer(filterListContainer);
				}

				List optionsList = filterXML.getChildren("Option");
				Iterator optionsIterator = optionsList.iterator();
				while(optionsIterator.hasNext()){
					Element optionXML = (Element) optionsIterator.next();
					Filter optionFilter = createFilter(optionXML, config, datasetName);
					if(optionFilter!=null){
						filterListContainer.addFilter(optionFilter);
						filter.addFilter(optionFilter);
					}
				}
			} else if (filterList!=null) {
				// Special filter list (e.g. Multiple Chromosomal regions)
				if(filter==null)
					filter = new Filter(type,name);
				for(String subFilterName:filterList.split(",")){
					Filter subFilter = config.getFilterByName(subFilterName, null);
					if(subFilter!=null)
						filter.addFilter(subFilter);
					filter.setSplitOnValue(":"); // Hard-code ":", because the delimiter isn't specified in 0.7 and the only known use-case uses ":"
					filter.setFilterOperation(FilterOperation.AND); // Again: hard-coded because the only known 0.7 use-case uses "AND"
				}
			} else {
				// Normal filter creation		
				if(filter==null){
					if(field != null && table != null && key != null){
						field = field.trim(); table = table.trim(); key = key.trim();
						attribute = getAttributeFromField(config.getMart(), field, table, key);
					}
					if(attribute == null){
						// If the attribute doesn't already exist, create it and put it in a hidden container
						attribute = createAttribute(filterXML, config, datasetName, McUtils.getUniqueAttributeName(config, name));
						if (attribute !=null){
							attribute.setHideValue(true);
							Container bcAttributes = config.getContainerByName("Attributes created by BC");
							if(bcAttributes == null){
								bcAttributes = new Container("Attributes created by BC");
								config.getRootContainer().addContainer(bcAttributes);
							}
							bcAttributes.addAttribute(attribute);
						}
					}
					if(attribute != null)
						filter = new Filter(attribute, name);
				}
				// Option creation
				if(filter!=null){
					List optionsList = filterXML.getChildren("Option");
					if(optionsList.size() == 0){
						List specificFilterContentList = filterXML.getChildren("SpecificFilterContent");
						Iterator specificFilterContentIterator = specificFilterContentList.iterator();
						
						while(specificFilterContentIterator.hasNext()){
							Element specificFilterContentXML = (Element) specificFilterContentIterator.next();
							if(specificFilterContentXML.getAttributeValue("internalName", "").equals(datasetName)){
								optionsList = specificFilterContentXML.getChildren("Option");
								break;
							}
						}
					}
					Iterator optionsIterator = optionsList.iterator();
					
					while(optionsIterator.hasNext()){
						Element optionXML = (Element) optionsIterator.next();
						
						String value = optionXML.getAttributeValue("value", "");
						String optionDisplayName = optionXML.getAttributeValue("displayName", "");
						Boolean isSelected = optionXML.getAttributeValue("isSelectable","true").equals("true");
						
						FilterData option = new FilterData(value, optionDisplayName, isSelected);
						filter.addOption(datasetName,option);
						
						// Add push options
						List pushActionList = optionXML.getChildren("PushAction");
						Iterator pushActionIterator = pushActionList.iterator();
						while(pushActionIterator.hasNext()){
							Element pushActionXML = (Element) pushActionIterator.next();
							String pushedFilterName = pushActionXML.getAttributeValue("ref");
							if(pushedFilterName!=null){
								List pushOptionsList = pushActionXML.getChildren("Option");
								Iterator pushOptionsIterator = pushOptionsList.iterator();
								
								while(pushOptionsIterator.hasNext()){
									Element pushOptionsXML = (Element) pushOptionsIterator.next();
									
									String pushOptionValue = pushOptionsXML.getAttributeValue("value", "");
									String pushOptionDisplayName = pushOptionsXML.getAttributeValue("displayName", "");
									Boolean pushOptionIsSelected = pushOptionsXML.getAttributeValue("isSelectable","true").equals("true");
									
									FilterData pushOption = new FilterData(pushOptionValue, pushOptionDisplayName, pushOptionIsSelected);
									option.addPushFilterOptions(pushedFilterName, pushOption);
								}
								
								// Update pushed list
								HashSet<Filter> pushingSet = this.filterPushedBy.get(pushedFilterName);
								if(pushingSet==null)
									pushingSet = new HashSet<Filter>();
								pushingSet.add(filter);
								this.filterPushedBy.put(pushedFilterName, pushingSet);
							}
						}
					}
					
				}
			}
		} else { // Pointer filter creation
			filter = config.getFilterByName(name, null);
			String pointedFilterName = filterXML.getAttributeValue("pointerFilter","").trim();
			String pointedDatasetName = parseAliasesToPartitionColumn(filterXML.getAttributeValue("pointerDataset","").trim(), config);
			if(filter == null){
				filter = new Filter(name, pointedFilterName, pointedDatasetName);
			}
			
			filter.setPointerInSource(true);
			filter.setInternalName(pointedFilterName);
			
			// Store in the partition table
			HashMap<String, String> datasetPartitionColumn = this.pointerFilterDatasetPartition.get(filter);
			if(datasetPartitionColumn==null){
				datasetPartitionColumn = new HashMap<String, String>();
				this.pointerFilterDatasetPartition.put(filter,datasetPartitionColumn);
			}
			datasetPartitionColumn.put(datasetName, pointedDatasetName);
		}
		
		if(filter!=null){
			if(displayName!=null)
				filter.setDisplayName(displayName);
			filter.setFilterType(type);
			filter.setHideValue(filterXML.getAttributeValue("hidden","false").equals("true") || filterXML.getAttributeValue("hideDisplay","false").equals("true"));
			filter.setQualifier(OperatorType.valueFrom(qualifier));
		}
		return filter;
	}
	
	
	private org.biomart.objects.objects.Attribute getAttributeFromField(Mart mart, String field, String table, String key) throws BackwardsCompatibilityException{
		org.biomart.objects.objects.Attribute attribute = null;
		
		List<org.biomart.objects.objects.Attribute> attributeList = null;
		if(table.equals("main") || table.endsWith("__main")){
			try{
				DatasetTable mainTable = McGuiUtils.INSTANCE.getMainTableByKeyName(mart, key);
				if(mainTable == null){
					List<DatasetTable> mainTableList = mart.getOrderedMainTableList();
					mainTable = mainTableList.get(mainTableList.size()-1);
					Log.warn("Problem finding main table with key " + key + ". Defaulting to table " + mainTable.getName() + ". Please check that the original config is valid.");

				}	
				attributeList = mainTable.getColumnByName(field).getReferences();	
			} catch (NullPointerException e) {
				Log.warn("Problem finding main table with key " + key + " and field " + field + ". Please check that the original config is valid.");
				return null;
			}
		} else {
				if(getPartitionedName(table) != null && 
						mart.getTableByName(getPartitionedName(table))!=null && 
						mart.getTableByName(getPartitionedName(table)).getColumnByName(field)!= null)
					attributeList = mart.getTableByName(getPartitionedName(table)).getColumnByName(field).getReferences();
			
		}
		
		if(attributeList != null){
			for(org.biomart.objects.objects.Attribute candidate : attributeList){
				if(candidate.getName().equals(candidate.getInternalName()))
					return candidate;
				attribute = candidate;
			}
		}
		
		return attribute;
	}
	
	private org.biomart.objects.objects.Attribute createAttribute(Element attributeXML, Config config, String datasetName) {
		String name = attributeXML.getAttributeValue("internalName");
		if(name == null){
			Log.warn("Attribute name is null!");
			return null;
		}
		return createAttribute( attributeXML,  config,  datasetName,  name);
	}
	
	private org.biomart.objects.objects.Attribute createAttribute(Element attributeXML, Config config, String datasetName, String name) {
		org.biomart.objects.objects.Attribute attribute = null;
		name = name.toLowerCase();
		
		if(attributeXML.getAttributeValue("pointerAttribute")==null){
			// Normal attribute creation
			attribute = config.getAttributeByName(name, null);
			if(attribute == null){
				DatasetTable table = getTable(attributeXML, config.getMart());
				if (table == null){
					Log.warn("Attribute " + name + " can not be created, because the table is invalid");
					return null;
				}
			
				DatasetColumn column = getColumn(attributeXML, table);
				if(column == null){
					Log.warn("Attribute " + name + " can not be created, because the column   is invalid");
					return null;
				}
				
				attribute = new org.biomart.objects.objects.Attribute(column, name);

				attribute.setDisplayName(attributeXML.getAttributeValue("displayName",name));
				attribute.setDescription(attributeXML.getAttributeValue("description",""));

				attribute.setHideValue(attributeXML.getAttributeValue("hidden","false").equals("true") || attributeXML.getAttributeValue("hideDisplay","false").equals("true"));
			}
			// Store in the partition table
			HashMap<String, String> partitionColumn = this.linkOutURLpartition.get(attribute);
			if(partitionColumn==null){
				partitionColumn = new HashMap<String, String>();
				this.linkOutURLpartition.put(attribute,partitionColumn);
			}
			partitionColumn.put(datasetName, processLinkoutURL(attributeXML, config, true));
			
			
		} else { // Pointer attribute
			if(config.getMart().getPartitionTableByName("p0").getCol(5).contains( processAliases(attributeXML.getAttributeValue("pointerDataset","").trim(),datasetName) )){
			// Internal pointer
				org.biomart.objects.objects.Attribute originalAttribute =  config.getAttributeByName(attributeXML.getAttributeValue("pointerAttribute"), null);
				if(originalAttribute != null){
					// This next section is necessary to remove the old pointer attribute, otherwise there will be duplicates
					org.biomart.objects.objects.Attribute oldPointerAttribute = config.getAttributeByName(name, null);
					if(oldPointerAttribute!=null){
						oldPointerAttribute.getParentContainer().removeAttribute(oldPointerAttribute);
					}	
					
					attribute = originalAttribute.cloneMyself(false);
					attribute.setName(name);
				}
			} else {
			// External pointer
				attribute = config.getAttributeByName(name, null);
				
				String pointedAttributeName = attributeXML.getAttributeValue("pointerAttribute","").trim();
				String pointedDatasetName = parseAliasesToPartitionColumn(attributeXML.getAttributeValue("pointerDataset","").trim(), config);
				
				if(attribute == null){
					attribute = new org.biomart.objects.objects.Attribute(name, pointedAttributeName, pointedDatasetName);
				}
				attribute.setPointerInSource(true);
				attribute.setInternalName(pointedAttributeName);

				
				// Store in the partition table
				HashMap<String, String> datasetPartitionColumn = this.pointerAttributeDatasetPartition.get(attribute);
				if(datasetPartitionColumn==null){
					datasetPartitionColumn = new HashMap<String, String>();
					this.pointerAttributeDatasetPartition.put(attribute,datasetPartitionColumn);
				}
				datasetPartitionColumn.put(datasetName, pointedDatasetName);
			}
		}
		return attribute;
	}
	
	private DatasetColumn getColumn(Element element, DatasetTable table){
		String columnName = element.getAttributeValue("field");
		if (columnName==null || table == null)
			return null;
		else 
			return table.getColumnByName(columnName.trim());
	}
	
	private DatasetTable getTable(Element element, Mart mart){
		String tableName = element.getAttributeValue("tableConstraint");
		if(tableName==null){
			Log.warn("tableConstraint of element " + element.getAttributeValue("internalName","") + " should not be null; check the 0.7 config");
			return null;
		} else {
			tableName = tableName.trim();
			if(tableName.equals("main")){
				String keyName = element.getAttributeValue("key");
				if(keyName==null){
					Log.warn("key of element " + element.getAttributeValue("internalName","") + " should not be null; check the 0.7 config");
					return null;
				} else {
					return McGuiUtils.INSTANCE.getMainTableByKeyName(mart, keyName.trim());
				}
			} else {
				try {
					if(getPartitionedName(tableName)==null)
						return null;
					return mart.getTableByName(getPartitionedName(tableName));
				} catch (BackwardsCompatibilityException e) {
					Log.warn("Problem creating attribute with tableName value " + tableName);
					e.printStackTrace();
					return null;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Mart createSchema(Mart mart, HashMap<DatasetFromUrl,Document> datasetTemplates){		
		PartitionTable partitionTable = new PartitionTable(mart, PartitionType.SCHEMA);
		mart.addPartitionTable(partitionTable);
		for(DatasetFromUrl dataset : datasetTemplates.keySet()){
			String datasetName = dataset.getName();
			try {
				Element rootXML = datasetTemplates.get(dataset).getRootElement();
				
				addDatasetToMart(mart, dataset, rootXML);

				createMainTables(rootXML, mart, datasetName);

				// Iterate over all of the attributes, filters, and options (which can be filters) to populate the DM tables and columns
				Iterator allColumns = rootXML.getDescendants(new ElementFilter("AttributeDescription").or(new ElementFilter("FilterDescription").or(new ElementFilter("Option"))));
				while(allColumns.hasNext()){
					Element originalElement = (Element) allColumns.next();
					String tableName = originalElement.getAttributeValue("tableConstraint");
					String columnName = originalElement.getAttributeValue("field");
					String keyName = originalElement.getAttributeValue("key");

					 if(tableName != null && columnName != null && keyName != null){
						tableName = tableName.trim(); columnName = columnName.trim(); keyName = keyName.trim();
						
						DatasetTable whichMain = McGuiUtils.INSTANCE.getMainTableByKeyName(mart, keyName);

						if(whichMain == null) {
							//throw new BackwardsCompatibilityException("FATAL ERROR: No main table exists for key " + keyName + " in dataset " + datasetName);
							List<DatasetTable> mainTableList = mart.getOrderedMainTableList();
							whichMain = mainTableList.get(mainTableList.size()-1);
							Log.error("No main table exists for key " + keyName + " in dataset " + datasetName + ". Defaulting to table " + whichMain.getName() + ". Please check that the original config is valid.");
							continue;
						}

						// Populate main tables
						if(tableName.equals("main")){	
							createPartitionedColumn(columnName, whichMain, datasetName);
						}
						// Create and populate DM tables
						else if(getPartitionedName(tableName)!=null){
							DatasetTable dmTable = mart.getTableByName(getPartitionedName(tableName));
							if (dmTable==null){
								dmTable = createTable(mart, tableName, DatasetTableType.DIMENSION, datasetName);

								//TODO double check this for partition-safety
								DatasetColumn dmFKColumn = createPartitionedColumn(keyName, dmTable, datasetName);
								ForeignKey dmFK = new ForeignKey(dmFKColumn);
								dmTable.addForeignKey(dmFK);
								new RelationTarget(whichMain.getPrimaryKey(), dmFK, Cardinality.MANY_A);
							} else {
								addToPartition(dmTable, datasetName);
							}
							createPartitionedColumn(keyName, dmTable, datasetName);
							createPartitionedColumn(columnName, dmTable, datasetName);
						}
					}
					
				}
				
				// Add all main table columns to subMains
				List<DatasetTable> mainTableList = mart.getOrderedMainTableList();
				for(int i = 1; i < mainTableList.size(); ++i){
					for(Column column: mainTableList.get(i-1).getColumnList())
						mainTableList.get(i).addColumn(column);
				}
			} catch (BackwardsCompatibilityException e) {
				e.printStackTrace();
				return null;
			} 
		}
		return mart;
	}

	private DatasetColumn createPartitionedColumn(String columnName,
			DatasetTable datasetTable, String datasetName) {
		DatasetColumn newColumn = datasetTable.getColumnByName(columnName);
		if(newColumn == null)
			newColumn = createColumn(columnName, datasetTable, datasetName);
		else
			addToPartition(newColumn, datasetName);
		return newColumn;
	}
	
	private void addDatasetToMart(Mart mart, DatasetFromUrl dataset, Element rootXML) {
		PartitionTable partitionTable = mart.getPartitionTableByName("p0");
		if(mart.getDatasetByName(dataset.getName())==null){
			if(this.dataLinkInfo.getDataLinkType().equals(DataLinkType.SOURCE) ||
					this.dataLinkInfo.getDataLinkType().equals(DataLinkType.TARGET)){					
				partitionTable.addNewRow(
						this.dataLinkInfo.getJdbcLinkObject().getConnectionBase() + separator + // c0
						this.dataLinkInfo.getJdbcLinkObject().getDatabaseName() +separator + // c1
						this.dataLinkInfo.getJdbcLinkObject().getSchemaName() + separator +  // c2
						this.dataLinkInfo.getJdbcLinkObject().getUserName() +separator + // c3
						this.dataLinkInfo.getJdbcLinkObject().getPassword() + separator + // c4
						dataset.getName() + separator + // c5
						"false" + separator + // c6
						processAliases( rootXML.getAttributeValue("displayName"), dataset.getName() ) + separator // c7
						+ "0.7" + separator +  // c8
						separator + // c9
						separator + // c10
						separator + // c11
						separator + // c12
						separator + // c13
						separator ); // c14
			}
			else if(this.dataLinkInfo.getDataLinkType().equals(DataLinkType.URL)){
				URL datasetURL;
				try {
					datasetURL = new URL(dataset.getUrl());
					partitionTable.addNewRow(
							/*this.dataLinkInfo.getUrlLinkObject().getFullHost() + separator + // c0
							this.dataLinkInfo.getUrlLinkObject().getPort() +separator +  // c1
							this.dataLinkInfo.getUrlLinkObject().getPath() + separator + // c2*/
							
							datasetURL.getProtocol() + "://" + datasetURL.getHost() + separator + // c0
							datasetURL.getPort() +separator +  // c1
							datasetURL.getPath() + separator + // c2
							separator +  // c3
							separator + // c4
							dataset.getName() + separator+ // c5
							"false" + separator + // c6
							/*rootXML.getAttributeValue("displayName")*/ dataset.getDisplayName() + separator + //c7
							"0.7" + separator + // c8
							separator + // c9
							separator + // c10
							dataset.getVirtualSchema() + separator + // c11
							separator + // c12
							separator + // c13
							separator // c14
							);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		}
	}

	public Element getOptions() {
		return allRoot;
	}
	
	private HashMap<String, Set<String>> pointedDatasets = new HashMap<String, Set<String>>();
	
	public HashMap<String, Set<String>> getPointedDatasets(){
		return pointedDatasets;
	}
	
	
	@SuppressWarnings("unchecked")
	public List<Mart> parseOldTemplates(){
		
		List<Mart> martList = new ArrayList<Mart>();
		
		if(true){
			martList = createMartList();
			return martList;
		}

		
		
		HashSet<String> tableQuery = new HashSet<String>();
		//TableNameSet tableList = new TableNameSet();
		List<String> simpleTableList = new ArrayList<String>();
		HashMap<String, byte[]> xmlList = new HashMap<String, byte[]>();
		HashMap<String, URL> xmlList2 = new HashMap<String, URL>();
		HashMap<String, HashSet<Integer>> templateMap = new HashMap<String, HashSet<Integer>>();

		if (!isWebService) {
			// Get the list of all tables in the database and store it for later
			try {
				ResultSet result = this.databaseConnection.getMetaData().getTables(null, this.schema, null, null);
				while (result.next()){
					//System.out.println(result.getString(1));
					tableQuery.add(result.getString(3));
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			//tableQuery = queryDB("SHOW TABLES");
			for (String table : tableQuery) {
				if (table.endsWith("__main") || table.endsWith("__dm")) {
					//tableList.add(new TableName(table, isDMPartitioned));
					if(!table.startsWith("meta"))
						simpleTableList.add(table);
				}
			}
			// Get the list of template names and their corresponding set of ID numbers
			try {
				Statement stmt = null;
				stmt = databaseConnection.createStatement();
				ResultSet result = stmt
				.executeQuery("SELECT * FROM meta_template__template__main");

				while (result.next()) {
					//System.out.println(result.getString(1));
					String templateName = result.getString("template");
					HashSet<Integer> datasetIDs = templateMap.get(templateName);
					if (datasetIDs == null) {
						datasetIDs = new HashSet<Integer>();
					}
					datasetIDs.add(result.getInt("dataset_id_key"));
					templateMap.put(templateName, datasetIDs);
				}
				// For all the templates present in the template__main table, get the GZipped XML
				StringBuilder templateInListQuery = new StringBuilder(
				"SELECT * FROM meta_template__xml__dm WHERE template IN (");
				for (String templateName : templateMap.keySet()) {
					templateInListQuery.append("'" + templateName + "',");
				}
				templateInListQuery.delete(templateInListQuery.length() - 1,
						templateInListQuery.length());
				templateInListQuery.append(")");
				//System.out.println(templateInListQuery);
				xmlList = queryDBbytes(templateInListQuery.toString());
				//System.err.println("Data retrieved.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {

			for(DatasetFromUrl dataset : this.datasetList){
				xmlList.put(dataset.getName(), null);
				try {
					xmlList2.put(dataset.getName(), new URL( dataset.getUrl()));
					//Log.info("Trying to connect to: " + dataset.getUrl() + " for dataset " + dataset.getDisplayName());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}

		// For each template with retrieved XML
		for(String xmlGZkey : xmlList.keySet()){
			LinkedHashMap<String, HashMap<String, String>> oldPartitionAliases = new LinkedHashMap<String, HashMap<String,String>>();
			HashSet<String> internalNames = new HashSet<String>();
			// Parse the XML
			try {
				InputStream rstream = null;
				SAXBuilder builder = new SAXBuilder();
				Document document = null;
				byte[] xmlGZ = xmlList.get(xmlGZkey);
				if(!isWebService){
					rstream = new GZIPInputStream(new ByteArrayInputStream(xmlGZ));
					InputSource is = new InputSource(rstream);
					document = builder.build(is);
				} else {
					rstream = xmlList2.get(xmlGZkey).openStream();
					Log.info("Trying to connect to: " + xmlGZkey + " " + xmlList2.get(xmlGZkey).toString());
					//System.err.println(xmlList2.get(xmlGZkey).getContent().toString());
					document = builder.build(xmlList2.get(xmlGZkey));
				}
				Element root = document.getRootElement();



				//System.out.println(root.getAttributeValue("template"));
				//String partitionName = root.getAttributeValue("template");
				//System.out.println("Partition table \"" + partitionName + "\":");
				List dynamicDatasets = root.getChildren("DynamicDataset");
				Iterator dynamicDatasetsIterator = dynamicDatasets.iterator();
				// Get the aliases from each dataset in the old template
				while (dynamicDatasetsIterator.hasNext()) {
					Element child = (Element) dynamicDatasetsIterator.next();
					//System.out.print(child.getAttributeValue("internalName"));
					//System.out.print(": ");
					//System.out.println(child.getAttributeValue("aliases"));
					String internalName = child.getAttributeValue("internalName");
					internalNames.add(internalName);
					String aliases = child.getAttributeValue("aliases");
					if(aliases!=null){
						String[] aliasPairs = aliases.split(",", -1);
						for(String aliasPair:aliasPairs){
							String[] aliasPairSplit = aliasPair.split("=");
							String aliasValue;
							if(aliasPairSplit.length > 1)
							{
								aliasValue = aliasPairSplit[1];
							} else { 
								aliasValue = null;
							}
							HashMap<String,String> aliasValuesByDataset = oldPartitionAliases.get(aliasPairSplit[0]);
							if(aliasValuesByDataset==null){
								aliasValuesByDataset = new HashMap<String, String>();
							}
							aliasValuesByDataset.put(internalName,aliasValue);
							oldPartitionAliases.put(aliasPairSplit[0], aliasValuesByDataset);

						}
					}
				}
				// Create a map of the old partition value names to the new column values
				HashMap<String,Integer> oldPartitionToColumn = new HashMap<String, Integer>();
				int firstRow=14; // Start at 14 because of the connection data that's present for all rows, and reserved cols
				for(String aliasName: oldPartitionAliases.keySet()){
					oldPartitionToColumn.put(aliasName,firstRow);
					firstRow++;
				}

				ArrayList<String> rowList = new ArrayList<String>();
				String templateName = xmlGZkey;
				String webTemplateName = templateName;
				boolean hasTemplate = false;

				// Create the new main partition table
				if(!isWebService){
					for(String rowName: internalNames){
						// For every row in the partition table, only include those those that have corresponding
						//  tables in the dataset
						String suffix = rowName.substring(rowName.indexOf('_')+1);
						templateName = suffix;
						if(rowName.indexOf('_') >= 0)
							hasTemplate = true;
						StringBuilder rowData = new StringBuilder();
						boolean hasPartitionedTable = false;
						for(String tableName : simpleTableList){
							if(tableName.startsWith(rowName)){
								hasPartitionedTable = true;
								break;
							}
						}
						if(hasPartitionedTable/*tableList.hasPartitionedTable(rowName)*/){
							/*System.out.print('\t');
						System.out.print(rowName + ": ");
						System.out.print(jdbcLinkObject.getJdbcUrl() + separator + jdbcLinkObject.getDatabaseName() +
								separator + jdbcLinkObject.getSchemaName() + separator + jdbcLinkObject.getUserName() +
								separator + jdbcLinkObject.getPassword() + separator);
							 */
							//TODO DB partition table
							if(this.dataLinkInfo.getDataLinkType().equals(DataLinkType.SOURCE) ||
									this.dataLinkInfo.getDataLinkType().equals(DataLinkType.TARGET))
								rowData.append(this.dataLinkInfo.getJdbcLinkObject().getConnectionBase() + separator + 
										this.dataLinkInfo.getJdbcLinkObject().getDatabaseName() +separator + 
										this.dataLinkInfo.getJdbcLinkObject().getSchemaName() + separator + 
										this.dataLinkInfo.getJdbcLinkObject().getUserName() +separator + 
										this.dataLinkInfo.getJdbcLinkObject().getPassword() + separator);
							else if(this.dataLinkInfo.getDataLinkType().equals(DataLinkType.URL))
								rowData.append(this.dataLinkInfo.getUrlLinkObject().getFullHost()+ separator + 
										this.dataLinkInfo.getUrlLinkObject().getPort() +separator + 
										this.dataLinkInfo.getUrlLinkObject().getPath() + separator + 
										"" + separator + "" + separator);
							if(rowName.indexOf('_') >= 0){
								//rowData.append(rowName + separator);//.substring(0,rowName.indexOf('_')) + separator);
								rowData.append(rowName + separator);
							} else {
								//rowData.append(templateName + separator);
								rowData.append(templateName + separator);
							}
							rowData.append("false" + separator);
							String displayName = root.getAttributeValue("displayName");
							for(String aliasName : oldPartitionAliases.keySet()){
								String value = oldPartitionAliases.get(aliasName).get(rowName);
								if(value==null){
									value="";
								}

								displayName = displayName.replace("*" + aliasName + "*", value);
							}
							rowData.append(displayName + separator);
							rowData.append("0.7" + separator);
							rowData.append(separator + separator + separator + separator + separator); // reserved for future use!

							for(String aliasName : oldPartitionAliases.keySet()){
								String value = oldPartitionAliases.get(aliasName).get(rowName);
								if(value==null){
									value="";
								}
								//System.out.print(value + separator);
								rowData.append(value + separator);
							}

							//System.out.println();
							rowList.add(rowData.toString());
						}
					}
				}

				Mart mart = new Mart(this.martRegistry,templateName,null);
				
				this.pointedDatasets.put(mart.getName(),new HashSet<String>());


				if(datasetMap!=null && datasetMap.get(xmlGZkey)!=null){
					mart.setDisplayName(this.datasetMap.get(xmlGZkey).getDisplayName());
					mart.setHideValue(!(this.datasetMap.get(xmlGZkey).isVisible()));
				}
				martList.add(mart);
				HashMap<String,DatasetTable> mainTableByKey = new HashMap<String, DatasetTable>();

				// If this is a webservice, populate the table lists
				if(isWebService){
					webTemplateName = root.getAttributeValue("dataset",templateName);
					//TODO Webservice
					//Iterator mainTables = root.getDescendants((new ElementFilter("MainTable")).or(new ElementFilter("Key")));
					Iterator mainTables = root.getDescendants(new ElementFilter("MainTable"));
					Iterator mainTableKeys = root.getDescendants(new ElementFilter("Key"));

					ArrayList<Element> mainTableList = new ArrayList<Element>();
					ArrayList<Element> mainTableKeyList = new ArrayList<Element>();
					while(mainTables.hasNext()){
						mainTableList.add((Element) mainTables.next());
					}
					while(mainTableKeys.hasNext()){
						mainTableKeyList.add((Element) mainTableKeys.next());
					}
					for(int i = 0; i < mainTableList.size(); ++i){
						DatasetTableType type = DatasetTableType.MAIN_SUBCLASS;
						if(i==0){
							type = DatasetTableType.MAIN;
						}
						DatasetTable table = mainTableByKey.get(mainTableKeyList.get(i).getValue());
						if(table==null){
							String partitionedName = null;
							if(mainTableList.get(i).getValue().split("__").length >= 3)
								partitionedName = "(p0c5)__" + mainTableList.get(i).getValue().split("__",2)[1];
							else
								partitionedName = mainTableList.get(i).getValue();
							table = new DatasetTable(mart, partitionedName,type);
							DatasetColumn column = new DatasetColumn(table, mainTableKeyList.get(i).getValue());
							column.addInPartitions(webTemplateName);
							table.addColumn(column);
							PrimaryKey pKey = new PrimaryKey(column);
							table.setPrimaryKey(pKey);
							if(i>0){	
								DatasetColumn keyColumn = table.getColumnByName(mainTableKeyList.get(i-1).getValue());
								if(keyColumn==null){
									keyColumn = new DatasetColumn(table, mainTableKeyList.get(i-1).getValue());
									table.addColumn(keyColumn);
									keyColumn.addInPartitions(webTemplateName);
								}
								ForeignKey fKey = table.getFirstForeignKey();
								if(fKey == null) {
									fKey = new ForeignKey(keyColumn);
								}
								table.addForeignKey(fKey);

								/*DatasetTable lastTable = mainTableByKey.get(mainTableKeyList.get(i-1).getValue());
								//DatasetColumn lastColumn = new DatasetColumn(lastTable, mainTableKeyList.get(i-1).getValue());
								PrimaryKey lastKey = lastTable.getPrimaryKey();
								if( (table.getType()==DatasetTableType.MAIN || table.getType()==DatasetTableType.MAIN_SUBCLASS) && (lastTable.getType()==DatasetTableType.MAIN || lastTable.getType()==DatasetTableType.MAIN_SUBCLASS) ){
									Relation relation = new Relation(lastKey, fKey, Cardinality.MANY_A);
								} else {
									Relation relation = new Relation(lastKey, pKey, Cardinality.MANY_A);
								}*/
								//lastKey.addRelation(relation);
								//pKey.addRelation(relation);
							}
							mainTableByKey.put(mainTableKeyList.get(i).getValue(), table);
							mart.addTable(table);
						}
					}
					Iterator allAttributes = root.getDescendants(new ElementFilter("AttributeDescription").or(new ElementFilter("FilterDescription").or(new ElementFilter("Option"))));
					while(allAttributes.hasNext()){
						Element attribute = (Element) allAttributes.next();
						String tableName = attribute.getAttributeValue("tableConstraint");
						String columnName = attribute.getAttributeValue("field");
						String keyName = attribute.getAttributeValue("key");
						if(tableName!=null && columnName!=null && keyName!=null){
							if(tableName.endsWith("__dm")){
								if(tableName.split("__").length < 3)
									tableName = webTemplateName + "__" + tableName;
								DatasetTable table = mart.getTableByName(tableName);
								if(table==null){
									table = new DatasetTable(mart, tableName, DatasetTableType.DIMENSION);
									mart.addTable(table);
								}
								table.addInPartitions(webTemplateName);
								DatasetColumn column = table.getColumnByName(columnName);
								if(column==null){
									column = new DatasetColumn(table, columnName);
									table.addColumn(column);
								}
								column.addInPartitions(webTemplateName);
								DatasetColumn keyColumn = table.getColumnByName(keyName);
								if(keyColumn == null){
									keyColumn = new DatasetColumn(table, keyName);
									table.addColumn(keyColumn);
								}
								keyColumn.addInPartitions(webTemplateName);
								ForeignKey fKey = table.getFirstForeignKey();
								if(fKey == null) {
									fKey = new ForeignKey(keyColumn);
								}
								table.addForeignKey(fKey);
								/*if(mainTableByKey.get(keyName)!=null) {
									PrimaryKey pk = mainTableByKey.get(keyName).getPrimaryKey();
									if(pk!=null && fKey!=null && !Relation.isRelationExist(pk, fKey)) {
										Relation relation = new Relation(pk, fKey, Cardinality.MANY_A);
										//fKey.addRelation(relation);
										//pk.addRelation(relation);
									}
								}*/
							} else if(tableName.equals("main") || tableName.endsWith("__main")){
								DatasetTable table = mainTableByKey.get(attribute.getAttributeValue("key"));
								if(table!=null){
									table.addInPartitions(webTemplateName);
									DatasetColumn column = table.getColumnByName(columnName);
									if(column==null){
										column = new DatasetColumn(table, columnName);
										table.addColumn(column);
									}
									column.addInPartitions(webTemplateName);
									mainTableByKey.put(attribute.getAttributeValue("key"), table);
								} else {
									//System.err.println("Problem finding main table with key: " + attribute.getAttributeValue("key"));
								}
							}
						}
					}
					for(int i = 1; i < mainTableList.size(); ++i){
						DatasetTable table = mainTableByKey.get(mainTableKeyList.get(i).getValue());
						if(table!=null){			
							DatasetTable lastTable = mainTableByKey.get(mainTableKeyList.get(i-1).getValue());
							for(Column lastColumn : lastTable.getColumnList()){
								table.addColumn(lastColumn);
							}
							mainTableByKey.put(mainTableKeyList.get(i).getValue(), table);
							mart.addTable(table);
						}
					}
				}
				// End webservice
				else {
					//TODO Database
					Iterator mainTables = root.getDescendants(new ElementFilter("MainTable"));
					Iterator mainTableKeys = root.getDescendants(new ElementFilter("Key"));

					ArrayList<Element> mainTableList = new ArrayList<Element>();
					ArrayList<Element> mainTableKeyList = new ArrayList<Element>();
					while(mainTables.hasNext()){
						mainTableList.add((Element) mainTables.next());
					}
					while(mainTableKeys.hasNext()){
						mainTableKeyList.add((Element) mainTableKeys.next());
					}
					for(int i = 0; i < mainTableList.size(); ++i){
						DatasetTableType type = DatasetTableType.MAIN_SUBCLASS;
						if(i==0){
							type = DatasetTableType.MAIN;
						}
						DatasetTable table = mainTableByKey.get(mainTableKeyList.get(i).getValue());
						if(table==null){
							String partitionedName = null;
							if(mainTableList.get(i).getValue().split("__").length >= 3)
								partitionedName = "(p0c5)__" + mainTableList.get(i).getValue().split("__",2)[1];
							else
								partitionedName = mainTableList.get(i).getValue();
							table = new DatasetTable(mart, partitionedName,type);
							DatasetColumn column = new DatasetColumn(table, mainTableKeyList.get(i).getValue());
							table.addColumn(column);
							PrimaryKey pKey = new PrimaryKey(column);
							table.setPrimaryKey(pKey);
							if(i>0){	
								DatasetColumn keyColumn = table.getColumnByName(mainTableKeyList.get(i-1).getValue());
								if(keyColumn==null){
									keyColumn = new DatasetColumn(table, mainTableKeyList.get(i-1).getValue());
									table.addColumn(keyColumn);
								}
								ForeignKey fKey = table.getFirstForeignKey();
								if(fKey == null) {
									fKey = new ForeignKey(keyColumn);
								}
								table.addForeignKey(fKey);

								/*DatasetTable lastTable = mainTableByKey.get(mainTableKeyList.get(i-1).getValue());
								//DatasetColumn lastColumn = new DatasetColumn(lastTable, mainTableKeyList.get(i-1).getValue());
								PrimaryKey lastKey = lastTable.getPrimaryKey();
								if( (table.getType()==DatasetTableType.MAIN || table.getType()==DatasetTableType.MAIN_SUBCLASS) && (lastTable.getType()==DatasetTableType.MAIN || lastTable.getType()==DatasetTableType.MAIN_SUBCLASS) ){
									Relation relation = new Relation(lastKey, fKey, Cardinality.MANY_A);
								} else {
									Relation relation = new Relation(lastKey, pKey, Cardinality.MANY_A);
								}*/
								//lastKey.addRelation(relation);
								//pKey.addRelation(relation);
							}
							mainTableByKey.put(mainTableKeyList.get(i).getValue(), table);
							mart.addTable(table);
							String[] splitName = mainTableList.get(i).getValue().split("__");
							table.addInPartitions(splitName[0]);
						}
					}
					for(String tableName : simpleTableList){
						if(tableName.endsWith("__dm")){
							String[] splitName = tableName.split("__");
							if(splitName.length == 3){
								String partitionedName = "(p0c5)__" + splitName[1] + "__dm";
								DatasetTable table = mart.getTableByName(partitionedName);
								if(table==null){
									table = new DatasetTable(mart, partitionedName, DatasetTableType.DIMENSION);
									mart.addTable(table);
								}
								table.addInPartitions(splitName[0]);
								//HashSet<String> currentColumns = queryDB("DESCRIBE "+ tableName);
								//BEGIN NEW
								HashSet<String> currentColumns = getColumns(tableName);
								//END NEW
								DatasetColumn keyColumn = null;
								for(String columnName : currentColumns){
									DatasetColumn column = table.getColumnByName(columnName);
									if(column==null){
										column = new DatasetColumn(table, columnName);
										table.addColumn(column);
									}
									column.addInPartitions(splitName[0]);
									if(columnName.endsWith("_key"))
										keyColumn = column;
								}
								ForeignKey fKey = table.getFirstForeignKey();
								if(fKey == null) {
									if(keyColumn==null)
										Log.warn("No key: " + tableName);
									else
										fKey = new ForeignKey(keyColumn);
								}
								table.addForeignKey(fKey);
								/*if(mainTableByKey.get(keyColumn.getName())!=null) {
									PrimaryKey pk = mainTableByKey.get(keyColumn.getName()).getPrimaryKey();
									if(pk!=null && fKey!=null && !Relation.isRelationExist(pk, fKey)) {
										Relation relation = new Relation(pk, fKey, Cardinality.MANY_A);
										//fKey.addRelation(relation);
										//pk.addRelation(relation);
									}
								}*/
							} else {
								Log.warn("Weird naming convention: " + tableName);
							}
						} else if(tableName.endsWith("__main")){
//							String[] splitName = tableName.split("__");
//							if(splitName.length == 3){
//								String partitionedName = "(p0c5)__" + splitName[1] + "__main";
//								DatasetTable table = mart.getTableByName(partitionedName);
//								mart.getM
//								if(table!=null){
//									table.addInPartitions(splitName[0]);
//									HashSet<String> currentColumns = queryDB("DESCRIBE "+ tableName);
//									DatasetColumn keyColumn = null;
//									for(String columnName : currentColumns){
//										DatasetColumn column = table.getColumnByName(columnName);
//										if(column==null){
//											column = new DatasetColumn(table, columnName);
//											table.addColumn(column);
//										}
//										column.addInPartitions(splitName[0]);
//										if(columnName.endsWith("_key"))
//											keyColumn = column;
//									}
//								}
//							}
						}
					}
					for(int i = 1; i < mainTableList.size(); ++i){
						DatasetTable table = mainTableByKey.get(mainTableKeyList.get(i).getValue());
						if(table!=null){			
							DatasetTable lastTable = mainTableByKey.get(mainTableKeyList.get(i-1).getValue());
							for(Column lastColumn : lastTable.getColumnList()){
								table.addColumn(lastColumn);
							}
							mainTableByKey.put(mainTableKeyList.get(i).getValue(), table);
							mart.addTable(table);
						}
					}
				}

				// Populate main partition table object
				if(rowList.size()>0){
					PartitionTable partitionTable = new PartitionTable(mart, PartitionType.SCHEMA);
					for(String row:rowList){
						partitionTable.addNewRow(row);
					}
					mart.addPartitionTable(partitionTable);
				} else {
					//System.err.println("No partition table! " + templateName);
					//TODO webservice partition table
					PartitionTable partitionTable = new PartitionTable(mart, PartitionType.SCHEMA);
					if(this.dataLinkInfo.getDataLinkType().equals(DataLinkType.SOURCE) ||
							this.dataLinkInfo.getDataLinkType().equals(DataLinkType.TARGET))					
						partitionTable.addNewRow(this.dataLinkInfo.getJdbcLinkObject().getConnectionBase() + separator + 
								this.dataLinkInfo.getJdbcLinkObject().getDatabaseName() +separator + 
								this.dataLinkInfo.getJdbcLinkObject().getSchemaName() + separator + 
								this.dataLinkInfo.getJdbcLinkObject().getUserName() +separator + 
								this.dataLinkInfo.getJdbcLinkObject().getPassword() + separator + 
								templateName + separator + 
								"false" + separator +
								root.getAttributeValue("displayName") + separator + "0.7" + separator);
					else if(this.dataLinkInfo.getDataLinkType().equals(DataLinkType.URL))
						partitionTable.addNewRow(this.dataLinkInfo.getUrlLinkObject().getFullHost() + separator + 
								this.dataLinkInfo.getUrlLinkObject().getPort() +separator + 
								this.dataLinkInfo.getUrlLinkObject().getPath() + separator + 
								"" +separator + ""+ separator + 
								templateName + separator+ 
								"false" + separator +
								root.getAttributeValue("displayName") + separator + "0.7" + separator);

					mart.addPartitionTable(partitionTable);
				}
				// Now: figure out which tables exist in the dataset, and get their information
				// This is to create the dm partition tables
				// HashMap<String,HashSet<String>> dmPartitionColumns = new HashMap<String, HashSet<String>>();
				// a DM partition table is uniquely defined by the dmPartitionTable and the key name : hence Map of a Map of a Set
				//HashMap<String,HashMap<String,HashSet<String>>> dmPartitionColumns = new HashMap<String, HashMap<String,HashSet<String>>>();
				//HashMap<String,HashSet<TableName>> tablesInDMPartiton = new HashMap<String,HashSet<TableName>>();

				// For main tables, we have a list of columns for each, plus an ordering based on number of keys
				

				//Integer numberOfColsP0 = 1;
				String mainTableName = "";
				if(mart.getPartitionTableByName("p0")!=null && mart.getPartitionTableByName("p0").getTotalRows() > 0){
					//Integer numberOfColsP0 = (mart.getPartitionTableByName("p0").getTotalColumns()-1);
					//mainTableName = "(p0c" + numberOfColsP0.toString() + ")";
					mainTableName = "(p0c5)";
				}
				//HashMap<String,PartitionTable> partitionByTable = new HashMap<String,PartitionTable>();
				/*for(String tableName : dmPartitionColumns.keySet()){
					for(String keyName : dmPartitionColumns.get(tableName).keySet()){
						PartitionTable dmPartition = new PartitionTable(mart, PartitionType.DIMENSION);
						for(TableName table : tablesInDMPartiton.get(tableName + keyName)){
							if(table.getDmPartitionName()!=null)
								dmPartition.addNewRow(table.getFullMainName() + separator + table.getDmPartitionName());
						}
						if(dmPartition.getRowNamesList().size() > 0 && isDMPartitioned){
							mart.addPartitionTable(dmPartition);
							//System.err.println(tableName +":");
							String name;
							if(mainTableName.equals("")){
								name = templateName + "__" 
								+ tableName + "_(" + dmPartition.getName() + "c1)" + "__dm";
							} else {
								name = "(" + dmPartition.getName() + "c0)" + "__" 
								+ tableName + "_(" + dmPartition.getName() + "c1)" + "__dm";
							}
							DatasetTable table = new DatasetTable(mart, name , DatasetTableType.DIMENSION);
							DatasetColumn keyColumn= new DatasetColumn(table, keyName);
							ForeignKey fKey = new ForeignKey(keyColumn);
							table.addForeignKey(fKey);
							//System.err.print('\t');
							for(String columnName : dmPartitionColumns.get(tableName).get(keyName)){
								//System.err.print(columnName + ", ");
								DatasetColumn column = new DatasetColumn(table, columnName);
								table.addColumn(column);
							}
							//System.err.println("");
							mart.addTable(table);
							//System.err.println(tableName + " " + keyName + ": " + dmPartition.getName());
							partitionByTable.put(tableName + keyName,dmPartition);
						} else { // This table isn't part of a dmPartition
							String name = mainTableName + "__" 
							+ tableName + "__dm";

							DatasetTable table = new DatasetTable(mart, name , DatasetTableType.DIMENSION);
							DatasetColumn keyColumn= new DatasetColumn(table, keyName);
							ForeignKey fKey = new ForeignKey(keyColumn);
							table.addForeignKey(fKey);
							//System.err.print('\t');
							for(String columnName : dmPartitionColumns.get(tableName).get(keyName)){
								//System.err.print(columnName + ", ");
								DatasetColumn column = new DatasetColumn(table, columnName);
								table.addColumn(column);
							}
							//System.err.println("");
							mart.addTable(table);
							//System.err.println(tableName + " " + keyName + ": " + dmPartition.getName());
							partitionByTable.put(tableName + keyName,dmPartition);
						}
					}
				}*/

				HashMap<String, HashMap<String, HashSet<String>>> mainPartitionColumns = new HashMap<String, HashMap<String, HashSet<String>>>();
				TreeMap<Integer,String> mainTableOrder = new TreeMap<Integer, String>();
				for(String table : simpleTableList){
					if(hasTemplate && !table.split("__")[0].endsWith("_"+templateName))
						continue;
					if(table.endsWith("__main")){
						HashSet<String> columns  = getColumns(table);
						// Find keys
						HashSet<String> keySet = new HashSet<String>();
						for(String columnName : columns){
							if(columnName.endsWith("_key")){
								keySet.add(columnName);
							}
						}
						HashMap<String,HashSet<String>> existingTables = mainPartitionColumns.get(table.split("__", -1)[1]);
						
						if(existingTables == null){
							existingTables = new HashMap<String, HashSet<String>>();
						}
						existingTables.put(table, columns);
						mainPartitionColumns.put(table.split("__", -1)[1], existingTables);
						
						mainTableOrder.put(keySet.size(), table.split("__", -1)[1]);
					}
				}
				
				HashSet<String> usedKeys = new HashSet<String>();
				String lastKey = null;
				PrimaryKey lastPrimaryKey = null;
				for(Integer order : mainTableOrder.keySet()){
					String tableName = mainTableOrder.get(order);
					HashMap<String, HashSet<String>> columnsMap = mainPartitionColumns.get(tableName);
					HashSet<String> keySet = new HashSet<String>();

					// If this is not the first main table (i.e. the main table with only one key), it is a submain
					DatasetTableType type = DatasetTableType.MAIN_SUBCLASS;
					if(order==1){
						type = DatasetTableType.MAIN;
					}

					// Find all the keys
					for(String partitionedTable: columnsMap.keySet()){
						HashSet<String> columns = columnsMap.get(partitionedTable);
						for(String columnName : columns){
							if(columnName.endsWith("_key")){
								keySet.add(columnName);
							}
						}
					}

					// Remove the primary keys from main tables earlier in the hierarchy
					keySet.removeAll(usedKeys);
					// Add this table's primary key to the set of used keys, for the next iteration of the loop
					usedKeys.addAll(keySet);
					// Get the primary key for this table; keySet now only has one member, but an iterator is the only way I know to retrieve it
					String primaryKey = null;
					for(String key:keySet){
						primaryKey = key;
					}
					// Remove both the primary key and the last table's primary key (lastKey) from the set of columns
					//columns.remove(primaryKey);
					//columns.remove(lastKey);

					// Set up the table object
					DatasetTable table = new DatasetTable(mart, mainTableName + "__" + tableName + "__main", type);
					DatasetColumn primaryKeyColumn = new DatasetColumn(table, primaryKey);
					PrimaryKey pKey = new PrimaryKey(primaryKeyColumn);
					table.setPrimaryKey(pKey);
					// If this isn't the first table, it's a submain, and therefore has a foreign key pointing to the previous main
					if(order > 1){
						DatasetColumn foreignKeyColumn = new DatasetColumn(table, lastKey);
						ForeignKey fKey = table.getFirstForeignKey();
						if(fKey == null) {
							fKey = new ForeignKey(foreignKeyColumn);
						}
						table.addForeignKey(fKey);
						if(!Relation.isRelationExist(lastPrimaryKey, fKey))
							new RelationTarget(lastPrimaryKey, fKey, Cardinality.MANY_A);
						//lastPrimaryKey.addRelation(relation);
						//fKey.addRelation(relation);
					}
					mainTableByKey.put(primaryKey,table);
					// Set the last key to this columns key, for the next iteration of the loop
					lastKey = primaryKey;
					lastPrimaryKey = pKey;

					// Add all the columns to the table object
					for(String partitionedTable: columnsMap.keySet()){
						HashSet<String> columns = columnsMap.get(partitionedTable);
						for(String columnName : columns){
							DatasetColumn column = table.getColumnByName(columnName);
							if (column==null)
								column = new DatasetColumn(table, columnName);
							column.addInPartitions(partitionedTable.split("__")[0]);
							table.addColumn(column);
						}
					}

					// Figure out all the relations pointing to this main table
					for(DatasetTable dmTable : mart.getDatasetTables()){
						if(dmTable.getForeignKeys().size() >0){
							ForeignKey fKey = dmTable.getFirstForeignKey();
							if(fKey ==null)
								fKey = dmTable.getForeignKeys().iterator().next();
							if(fKey.equalsByName(pKey)){
								if(!Relation.isRelationExist(pKey, fKey))
									new RelationTarget(pKey, fKey, Cardinality.MANY_A);
								//pKey.addRelation(relation);
								//fKey.addRelation(relation);
							}
						}
					}

					// Add the table to the mart object
					mart.addTable(table);
					for(String pName : mart.getSchemaPartitionTable().getCol(PartitionUtils.DATASETNAME)){
						table.addInPartitions(pName);
					}
				}
				Config config = new Config(templateName);
				mart.addConfig(config);
				String configHide = root.getAttributeValue("visible","1");
				if (configHide.equals("0"))
					config.setHideValue(true);
				config.setProperty(XMLElements.METAINFO, "");
				config.setProperty(XMLElements.DATASETDISPLAYNAME,"(p0c7)");
				//config.setName("(p0c5)");
				config.setProperty(XMLElements.DATASETHIDEVALUE,"(p0c6)");
				Container rootContainer = config.getRootContainer();
				config.addRootContainer(rootContainer);

				Element dataRoot = new Element("config");
				dataRoot.setAttribute(new Attribute("name", templateName));
				Element martOptions = new Element("mart");
				martOptions.setAttribute(new Attribute("name", templateName));
				martOptions.addContent(dataRoot);

				//Document xmlOptions = new Document(dataRoot);

				//				//TODO Process all attributes to create DM partition tables
				//				Iterator allAttributes = root.getDescendants(new ElementFilter("AttributeDescription").or(new ElementFilter("Option")));
				//				//HashMap<String,HashMap<String,HashSet<String>>> partitionAttributeList = new HashMap<String, HashMap<String,HashSet<String>>>();
				//				//Partition Table name -> FieldName -> column number
				//				//HashMap<String,HashMap<String,HashMap<String,Integer>>> locateAttributeInPartitionTable = new HashMap<String, HashMap<String,HashMap<String,Integer>>>();
				//				HashMap<String, HashMap<String,Integer>> attributeInPartitionTable = new HashMap<String, HashMap<String,Integer>>();
				//				if(!isWebService && isDMPartitioned){
				//					while(allAttributes.hasNext()){
				//						Element oldAttribute = (Element) allAttributes.next();
				//						if(oldAttribute.getName().equals("AttributeDescription") || ((Element) oldAttribute.getParent()).getName().equals("FilterDescription")){
				//							String tableName = oldAttribute.getAttributeValue("tableConstraint");
				//							String hidden = oldAttribute.getAttributeValue("hidden","false");
				//							String field = oldAttribute.getAttributeValue("field");
				//
				//							if(oldAttribute.getAttributeValue("pointerAttribute")==null && hidden.equals("false")){
				//								if(tableName!=null && !(tableName.equalsIgnoreCase("main"))){
				//									if(tableName.split("__").length > 3){
				//										//System.err.println("Illegal tableConstraint! Too many double underscores! " + tableName);
				//									}
				//									int whichSection = 0;
				//									if(tableName.split("__").length == 3)
				//										whichSection = 1;
				//									String[] splitName = tableName.split("__")[whichSection].split("_",2);
				//
				//									if(splitName.length > 1 && !isWebService && isDMPartitioned){
				//										//This table that this attribute refers to is part of a dm partition
				//										PartitionTable currentPartition = partitionByTable.get(splitName[0] + oldAttribute.getAttributeValue("key"));
				//										//System.err.println(tableName + ": ");//+ splitName[0] +", " + attribute.getAttributeValue("key") );
				//										//System.err.println("\t" + currentPartition.getName());
				//										if(currentPartition!=null){
				//											HashMap<String,Integer> columnsByFieldName = attributeInPartitionTable.get(currentPartition.getName() + tableName + field);
				//											if(columnsByFieldName==null){
				//												columnsByFieldName = new HashMap<String, Integer>();
				//											}
				//											for(Object tempObject : oldAttribute.getAttributes()){
				//												Attribute xmlAttribute = (Attribute) tempObject;
				//												String propertyName = xmlAttribute.getName();
				//												if(columnsByFieldName.get(propertyName)==null)
				//													columnsByFieldName.put(propertyName, currentPartition.addColumn(""));
				//
				//												currentPartition.setColumnByColumn(1, splitName[1], columnsByFieldName.get(propertyName), xmlAttribute.getValue());
				//											}
				//											if(oldAttribute.getChildren("SpecificAttributeContent").size()>0){
				//												if(columnsByFieldName.get("hidden")==null)
				//													columnsByFieldName.put("hidden", currentPartition.addColumn("Xtrue"));
				//												currentPartition.setColumnValue(columnsByFieldName.get("hidden"), "Xtrue");
				//											}
				//											attributeInPartitionTable.put(currentPartition.getName() +tableName + field,columnsByFieldName);
				//
				//											// Now, for each partition table, we have a map of which xmlAttribute names have more than one value,
				//											//  which we can use later to determine whether it needs to be added to the partition table or not.
				//										} else {
				//											//System.err.println("Partition for " + tableName + " doesn't appear to exist!");
				//										}
				//									}
				//								}
				//							}
				//						}
				//					}
				//				}


				List attributePages = root.getChildren("AttributePage");
				Container tabContainer = null;
				if(attributePages.size()>1){
					tabContainer = new Container("ATTRIBUTES");
					rootContainer.addContainer(tabContainer);
					tabContainer.setProperty(XMLElements.MAXCONTAINERS, "1");
				}
				Iterator attributePageIterator = attributePages.iterator();

				HashMap<String, org.biomart.objects.objects.Attribute> fieldTableKeyToAttribute = new HashMap<String, org.biomart.objects.objects.Attribute>();
				while (attributePageIterator.hasNext()) {
					Element attributePage = (Element) attributePageIterator.next();
					//String internalName = attributePage.getAttributeValue("internalName");
					if(attributePage.getAttributeValue("hidden","false").equals("false")){
						Container attributePageContainer = populateContainer(attributePage);
						if(tabContainer!=null)
							tabContainer.addContainer(attributePageContainer);
						else
							rootContainer.addContainer(attributePageContainer);
						
						List attributeGroups = attributePage.getChildren("AttributeGroup");
						Iterator attributeGroupIterator = attributeGroups.iterator();
						while (attributeGroupIterator.hasNext()){
							Element attributeGroup = (Element) attributeGroupIterator.next();
							if(attributeGroup.getAttributeValue("hidden","false").equals("false")){

								Container attributeGroupContainer = populateContainer(attributeGroup);
								attributePageContainer.addContainer(attributeGroupContainer);
								List attributeCollections = attributeGroup.getChildren("AttributeCollection");
								Iterator attributeCollectionIterator = attributeCollections.iterator();
								while (attributeCollectionIterator.hasNext()){
									Element attributeCollection = (Element) attributeCollectionIterator.next();
									if(attributeCollection.getAttributeValue("hidden","false").equals("false")){

										Container attributeCollectionContainer = populateContainer(attributeCollection);
										attributeGroupContainer.addContainer(attributeCollectionContainer);
										List attributeDescriptions = attributeCollection.getChildren("AttributeDescription");
										Iterator attributeDescriptionIterator = attributeDescriptions.iterator();
										while(attributeDescriptionIterator.hasNext()){
											Element attributeDescription = (Element) attributeDescriptionIterator.next();
											if(attributeDescription.getAttributeValue("hidden","false").equals("false")){

												if(attributeDescription.getAttributeValue("pointerAttribute")==null){
													String tableName = attributeDescription.getAttributeValue("tableConstraint");
													String keyName = attributeDescription.getAttributeValue("key");
													String fieldName = attributeDescription.getAttributeValue("field");

													if(tableName!=null && !(tableName.equalsIgnoreCase("main") || tableName.endsWith("__main"))){
														// Attribute is not a member of a DM partition, because it's "_" split only has one section
														//String currentTableName = mainTableName + templateName + "__" + splitName[0] + "__dm";
														String currentTableName = tableName;
														DatasetTable currentTable = mart.getTableByName(currentTableName);
														if(currentTable==null){
															Log.warn("Can't create attribute, because table " + currentTableName + " not found; trying " + mart.getName() + "__" + currentTableName);
															currentTable = mart.getTableByName("(p0c5)__" + currentTableName);
														}
														if(currentTable==null && currentTableName.split("__",2).length>1){
															currentTable = mart.getTableByName("(p0c5)__" + currentTableName.split("__",2)[1]);
														}
														if(currentTable==null){
															Log.warn("Nope, still missing");
														} else {
															DatasetColumn currentColumn = currentTable.getColumnByName(fieldName);
															if(currentColumn!=null){
																org.biomart.objects.objects.Attribute attribute = new org.biomart.objects.objects.Attribute(currentColumn, attributeDescription.getAttributeValue("internalName"));
																attributeCollectionContainer.addAttribute(attribute);

																attribute.setHideValue(attributeDescription.getAttributeValue("hideDisplay","false").equals("true"));
																if(attributeDescription.getAttributeValue("displayName")!=null){
																	attribute.setDisplayName(attributeDescription.getAttributeValue("displayName"));
																}
																attribute.setDescription(attributeDescription.getAttributeValue("description",""));
																attribute.setLinkOutUrl(processLinkoutURL(attributeDescription, config));

																attribute.setName(attribute.getInternalName());

																fieldTableKeyToAttribute.put(fieldName + tableName + keyName, attribute);
															}
														}

													} else if (tableName!=null && (tableName.equalsIgnoreCase("main") || tableName.endsWith("__main"))){
														DatasetTable currentTable= mainTableByKey.get(attributeDescription.getAttributeValue("key"));
														if (currentTable != null) {
															DatasetColumn currentColumn = currentTable.getColumnByName(fieldName);
															if (currentColumn == null) {
																//System.err.println("Can't create attribute, because column " + attributeDescription.getAttributeValue("field") + " wasn't found in table " + currentTable.getName());
															} else {
																org.biomart.objects.objects.Attribute attribute = new org.biomart.objects.objects.Attribute(currentColumn,attributeDescription.getAttributeValue("internalName",""));
																attributeCollectionContainer.addAttribute(attribute);

																attribute.setHideValue(attributeDescription.getAttributeValue("hideDisplay","false").equals("true"));
																if(attributeDescription.getAttributeValue("displayName")!=null){
																	attribute.setDisplayName(attributeDescription.getAttributeValue("displayName"));
																}
																attribute.setDescription(attributeDescription.getAttributeValue("description",""));
																attribute.setLinkOutUrl(processLinkoutURL(attributeDescription, config));

																attribute.setName(attribute.getInternalName());

																fieldTableKeyToAttribute.put(fieldName + tableName + keyName, attribute);
															}
														} else {
															//System.err.println(mart.getName() + ": ENTERED MAIN ATTRIBUTE " + attributeDescription.getAttributeValue("internalName") + " " + attributeDescription.getAttributeValue("key"));
															//System.err.println("Main table reference with non-existing key.");
														}
													}
												} else { // It's a pointer attribute
													//TODO Pointer Attribute code
												
													//if(!(attributeDescription.getAttributeValue("pointerDataset","").endsWith(templateName))){ // It's a non-local pointer
													if(isRemotePointer(mart.getPartitionTableByName("p0"), internalNames, attributeDescription.getAttributeValue("pointerDataset",""), oldPartitionToColumn)){
														String[] pointerDataset = attributeDescription.getAttributeValue("pointerDataset").split("\\*",-1);
														if(pointerDataset.length <= 3){
															org.biomart.objects.objects.Attribute pointerAttribute = new org.biomart.objects.objects.Attribute(attributeDescription.getAttributeValue("internalName"),attributeDescription.getAttributeValue("pointerAttribute"),replaceAliases(attributeDescription.getAttributeValue("pointerDataset"),oldPartitionToColumn));
															attributeCollectionContainer.addAttribute(pointerAttribute);

															this.pointedDatasets.get(mart.getName()).add(pointerAttribute.getPointedDatasetName());
															//pointerAttribute.setInternalName(attributeDescription.getAttributeValue("pointerAttribute"));
															//pointerAttribute.setConfigName("POINTER");
															//pointerAttribute.setName(pointerAttribute.getInternalName());
															pointerAttribute.setLinkOutUrl(processLinkoutURL(attributeDescription, config));

														} else if (pointerDataset.length > 3){
															Log.warn("Too many aliases in pointerDataset property!");
														} else {
															PartitionTable mainPartition = mart.getPartitionTableByName("p0");
															HashMap<String, String> internalNameToValue = oldPartitionAliases.get(pointerDataset[1]);
															for(String internalName : internalNameToValue.keySet()){
																org.biomart.objects.objects.Attribute pointerAttribute = new org.biomart.objects.objects.Attribute(attributeDescription.getAttributeValue("internalName"),attributeDescription.getAttributeValue("pointerAttribute"), pointerDataset[0] + internalNameToValue.get(internalName) + pointerDataset[2]);
																attributeCollectionContainer.addAttribute(pointerAttribute);
																this.pointedDatasets.get(mart.getName()).add(pointerAttribute.getPointedDatasetName());
																//pointerAttribute.setInternalName(attributeDescription.getAttributeValue("pointerAttribute"));
																//pointerAttribute.setConfigName("POINTER");
																//pointerAttribute.setName(pointerAttribute.getInternalName());
																pointerAttribute.setLinkOutUrl(processLinkoutURL(attributeDescription, config));

																int hideColumn = mainPartition.addColumn("true");
																int row = mainPartition.getRowNumberByDatasetName(internalName);
																if(row >= 0)
																	mainPartition.updateValue(row, hideColumn, "false");

																pointerAttribute.setHideValueInString("(p0c" + Integer.toString(hideColumn) + ")");

															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				List filterPages = root.getChildren("FilterPage");
				Iterator filterPageIterator = filterPages.iterator();
				while (filterPageIterator.hasNext()) {
					Element filterPage = (Element) filterPageIterator.next();
					//String internalName = filterPage.getfilterValue("internalName");
					if(filterPage.getAttributeValue("hidden","false").equals("false")){
						Container filterPageContainer = populateContainer(filterPage);
						rootContainer.addContainer(filterPageContainer);
						List filterGroups = filterPage.getChildren("FilterGroup");
						Iterator filterGroupIterator = filterGroups.iterator();
						while (filterGroupIterator.hasNext()){
							Element filterGroup = (Element) filterGroupIterator.next();
							if(filterGroup.getAttributeValue("hidden","false").equals("false")){
								Container filterGroupContainer = populateContainer(filterGroup);
								filterPageContainer.addContainer(filterGroupContainer);
								List filterCollections = filterGroup.getChildren("FilterCollection");
								Iterator filterCollectionIterator = filterCollections.iterator();
								while (filterCollectionIterator.hasNext()){
									Element filterCollection = (Element) filterCollectionIterator.next();
									if(filterCollection.getAttributeValue("hidden","false").equals("false")){
										
										//Container filterCollectionContainer = populateContainer(filterCollection);
										List filterDescriptions = filterCollection.getChildren("FilterDescription");
										
										String filterCollectionName = filterCollection.getAttributeValue("displayName", "ERROR");
										String filterCollectionPrefix = filterCollectionName + " - ";
										
										Iterator filterDescriptionIterator = filterDescriptions.iterator();
										while(filterDescriptionIterator.hasNext()){
											Element filterDescription = (Element) filterDescriptionIterator.next();
											if(filterDescription.getAttributeValue("hidden","false").equals("false") && filterDescription.getAttributeValue("pointerFilter")==null){
												String displayType = filterDescription.getAttributeValue("displayType");
												if(displayType==null){
													Log.warn("Filter "+ filterDescription.getAttributeValue("internalName") + " is missing a displayType");
												
												} else {
													if(displayType.equals("container")){
														FilterType containerType;
														if(filterDescription.getAttributeValue("type","").startsWith("boolean")){
															containerType = FilterType.SINGLESELECTBOOLEAN;
														} else {
															containerType = FilterType.SINGLESELECTUPLOAD;
														}
														Filter filterList = new Filter(containerType, filterDescription.getAttributeValue("internalName"));
														if(filterDescription.getAttributeValue("displayName")!=null){
															filterList.setDisplayName((filterDescriptions.size() == 1) ? filterCollectionName : filterCollectionPrefix +filterDescription.getAttributeValue("displayName"));
														} else if (filterCollection.getChildren().size() == 1 && filterCollection.getAttributeValue("displayName")!=null ){
															filterList.setDisplayName((filterDescriptions.size() == 1) ? filterCollectionName : filterCollectionPrefix +filterCollection.getAttributeValue("displayName"));
														}
														filterGroupContainer.addFilter(filterList);

														//filterList.setName(filterDescription.getAttributeValue("internalName"));
														Container filterListContainer = new Container(filterDescription.getAttributeValue("internalName") + "_listContainer");
														filterPageContainer.addContainer(filterListContainer);
														filterListContainer.setHideValue(true);
														List filterListFilters = filterDescription.getChildren("Option");
														Iterator filterListFiltersIterator = filterListFilters.iterator();
														while(filterListFiltersIterator.hasNext()){
															Element filterListFilter = (Element) filterListFiltersIterator.next();
															if(filterListFilter.getAttributeValue("hidden","false").equals("false")){
																String subDisplayType = filterListFilter.getAttributeValue("displayType","text");

																if(subDisplayType==null){
																	//System.err.println("FilterList: No display type in old XML!");
																}

																String fieldName = filterListFilter.getAttributeValue("field");
																String tableName = filterListFilter.getAttributeValue("tableConstraint");
																String keyName = filterListFilter.getAttributeValue("key");
																String multipleValues = filterListFilter.getAttributeValue("multipleValues");
																String style = filterListFilter.getAttributeValue("style");
																FilterType newType = null;
																if(fieldName!=null){
																	if (subDisplayType.equals("text")){
																		if(multipleValues!=null && multipleValues.equals("1")){
																			newType = FilterType.UPLOAD;
																		} else {
																			newType = FilterType.TEXT;
																		}
																	} else if (subDisplayType.equals("list")){
																		if(style.equals("radio")){
																			newType = FilterType.BOOLEAN;
																		} else if (style.equals("menu")){
																			if(multipleValues!=null && multipleValues.equals("1")){
																				newType = FilterType.MULTISELECT;
																			} else {
																				newType = FilterType.SINGLESELECT;
																			}
																		}
																	}
																	if (newType == null){
																		Log.warn("FilterList: Invalid type in old XML!");
																	}

																	org.biomart.objects.objects.Attribute attribute = fieldTableKeyToAttribute.get(fieldName+tableName+keyName);
														
																	if (attribute == null)
																		attribute = fieldTableKeyToAttribute.get(fieldName+templateName+"__"+tableName+keyName);
																	if (attribute == null)
																		attribute = config.getAttributeByName(filterListFilter.getAttributeValue("internalName"), null);
																	
																	if(attribute==null) { // The attribute for this filter doesn't exist, so we need to create it and hide it
																		//System.err.println("FilterListOption: no attribute for " + fieldName + " " + tableName + " " + keyName);
																		if(tableName!=null && !(tableName.equalsIgnoreCase("main") || tableName.endsWith("__main"))){
																			// Attribute is not a member of a DM partition, because it's "_" split only has one section
																			//String currentTableName = mainTableName + templateName + "__" + splitName[0] + "__dm";
																			String currentTableName = tableName;
																			DatasetTable currentTable = mart.getTableByName(currentTableName);
																			// If the table doesn't exist, try adding the prefix for the webservice (UGLY)
																			if(currentTable==null){
																				currentTable = mart.getTableByName(webTemplateName + "__" + currentTableName);
																			}
																			if(currentTable==null){
																				Log.warn("Can't create attribute, because table " + currentTableName + " not found; trying " + mart.getName() + "__" + currentTableName);
																				currentTable = mart.getTableByName("(p0c5)__" + currentTableName);
																			}
																			if(currentTable==null && currentTableName.split("__",2).length>1){
																				currentTable = mart.getTableByName("(p0c5)__" + currentTableName.split("__",2)[1]);
																			}
																			if(currentTable==null){
																				Log.warn("Nope, still missing");
																			}else {
																				DatasetColumn currentColumn = currentTable.getColumnByName(fieldName);
																				if(currentColumn==null){
																					//System.err.println("*" + mart.getName() + " " + filterListFilter.getAttributeValue("internalName") + " FILTERLIST Can't create attribute, because column " + fieldName + " in table " + currentTableName + " not found");
																				} else {
																					org.biomart.objects.objects.Attribute hiddenAttribute = new org.biomart.objects.objects.Attribute(currentColumn, filterListFilter.getAttributeValue("internalName"));
																					hiddenAttribute.setHideValue(true);
																					filterListContainer.addAttribute(hiddenAttribute);
																					fieldTableKeyToAttribute.put(fieldName + tableName + keyName, hiddenAttribute);
																				
																					if(filterListFilter.getAttributeValue("displayName")!=null){
																						hiddenAttribute.setDisplayName(filterListFilter.getAttributeValue("displayName"));
																					}
																					hiddenAttribute.setDescription(filterListFilter.getAttributeValue("description",""));

																					hiddenAttribute.setName(hiddenAttribute.getInternalName());

																					attribute = hiddenAttribute;
																				}
																			}

																		} else if (tableName!=null && (tableName.equalsIgnoreCase("main") || tableName.endsWith("__main"))){
																			DatasetTable currentTable= mainTableByKey.get(filterListFilter.getAttributeValue("key"));

																			if (currentTable != null) {
																				DatasetColumn currentColumn = currentTable.getColumnByName(fieldName);
																				if (currentColumn == null) {
																					//System.err.println("MAIN FILTERLIST Can't create attribute, because column " + filterListFilter.getAttributeValue("field") + " wasn't found in table " + currentTable.getName());
																				} else {
																					org.biomart.objects.objects.Attribute hiddenAttribute = new org.biomart.objects.objects.Attribute(currentColumn,filterListFilter.getAttributeValue("internalName"));

																					filterListContainer.addAttribute(hiddenAttribute);
																					fieldTableKeyToAttribute.put(fieldName + tableName + keyName, attribute);	hiddenAttribute.setHideValue(true);
																					if(filterListFilter.getAttributeValue("displayName")!=null){
																						hiddenAttribute.setDisplayName(filterListFilter.getAttributeValue("displayName"));
																					}
																					hiddenAttribute.setName(hiddenAttribute.getInternalName());

																					hiddenAttribute.setDescription(filterListFilter.getAttributeValue("description",""));


																					attribute = hiddenAttribute;
																				}
																			} else {
																				//System.err.println("MAIN FILTERLIST Main table reference with non-existing key.");
																			}
																		}

																	}

																	if(attribute!=null){									
																		Filter filter = new Filter(attribute,filterListFilter.getAttributeValue("internalName"));
																		if (attribute.getName().split("__").length > 1){
																			//filter = new Filter(attribute,attribute.getName().split("__")[0] + "__" + filterListFilter.getAttributeValue("internalName"));
																			filter = new Filter(attribute, filterListFilter.getAttributeValue("internalName"));

																		}
																		filterList.addFilter(filter);


																		if(filterListFilter.getAttributeValue("displayName")!=null){
																			filter.setDisplayName(filterListFilter.getAttributeValue("displayName"));
																		}
																		// Need to set filter's hidden status to be the same as the attached attribute
																		//filter.setHideValueInString(attribute.getHideString());
																		filter.setFilterType(newType);
																		if(newType==FilterType.BOOLEAN){
																			filter.setOnlyValue("Only");
																			filter.setExcludedValue("Excluded");
																		}
																		filterListContainer.addFilter(filter);

																		String legalQualifiers = filterListFilter.getAttributeValue("legal_qualifiers");
																		if(legalQualifiers!=null){
																			if(legalQualifiers.contains(">=")){
																				filter.setQualifier(OperatorType.GTE);
																			} else if (legalQualifiers.contains("<=")){
																				filter.setQualifier(OperatorType.LTE);
																			} else if (legalQualifiers.contains(">")){
																				filter.setQualifier(OperatorType.GT);
																			} else if (legalQualifiers.contains("<")){
																				filter.setQualifier(OperatorType.LT);
																			} else if (legalQualifiers.contains("in") || legalQualifiers.contains("=")){
																				filter.setQualifier(OperatorType.E);
																			} else if (legalQualifiers.contains("like")){
																				filter.setQualifier(OperatorType.LIKE);
																			} else {
																				filter.setQualifier(OperatorType.E);
																			}
																		}
																		filter.setDescription(filterListFilter.getAttributeValue("description",""));


																		parseSpecificFilterContent(dataRoot, filterListFilter, filter, "SpecificOptionContent");
																		if(newType!=FilterType.BOOLEAN){
																			parseFilterOptions(dataRoot,filterListFilter,filter);
																		}

																	}
																}
															}
														}
														//filterCollectionContainer.addContainer(filterListContainer);
														//filterCollectionContainer.addFilter(filterList);
														//filterGroupContainer.addContainer(filterListContainer);
													} else { // Not a container, therefore a normal filter
														String fieldName = filterDescription.getAttributeValue("field");
														String tableName = filterDescription.getAttributeValue("tableConstraint");
														String keyName = filterDescription.getAttributeValue("key");
														String multipleValues = filterDescription.getAttributeValue("multipleValues");
														String style = filterDescription.getAttributeValue("style","");
														FilterType newType = null;
														if (displayType.equals("text")){
															if(multipleValues!=null && multipleValues.equals("1")){
																newType = FilterType.UPLOAD;
															} else {
																newType = FilterType.TEXT;
															}
														} else if (displayType.equals("list")){
															if(style.equals("radio")){
																newType = FilterType.BOOLEAN;
															} else /*if (style.equals("menu"))*/{
																if(multipleValues!=null && multipleValues.equals("1")){
																	newType = FilterType.MULTISELECT;
																} else {
																	newType = FilterType.SINGLESELECT;
																}
															}
														}
														if (newType == null){
															Log.warn("Invalid filter type in old XML! Setting to text");
															newType = FilterType.TEXT;
														}
														org.biomart.objects.objects.Attribute attribute = fieldTableKeyToAttribute.get(fieldName+tableName+keyName);
														if (attribute == null)
															attribute = fieldTableKeyToAttribute.get(fieldName+templateName+"__"+tableName+keyName);
														if (attribute == null)
															attribute = config.getAttributeByName(filterDescription.getAttributeValue("internalName"), null);

														if(attribute==null) { // The attribute for this filter doesn't exist, so we need to create it and hide it
															if(tableName!=null && !(tableName.equalsIgnoreCase("main") || tableName.endsWith("__main"))){													
																{ // Attribute is not a member of a DM partition, because it's "_" split only has one section
																	//String currentTableName = mainTableName + templateName + "__" + splitName[0] + "__dm";
																	String currentTableName = tableName;
																	DatasetTable currentTable = mart.getTableByName(currentTableName);
																	if(currentTable==null){
																		Log.warn("Can't create filter, because table " + currentTableName + " not found; trying " + mart.getName() + "__" + currentTableName);
																		currentTable = mart.getTableByName("(p0c5)__" + currentTableName);
																	}
																	if(currentTable==null && currentTableName.split("__",2).length>1){
																		currentTable = mart.getTableByName("(p0c5)__" + currentTableName.split("__",2)[1]);
																	}
																	if(currentTable==null){
																		Log.warn("Nope, still missing");
																	} else {
																		DatasetColumn currentColumn = currentTable.getColumnByName(fieldName);
																		if(currentColumn!=null){
																			org.biomart.objects.objects.Attribute hiddenAttribute = new org.biomart.objects.objects.Attribute(currentColumn, filterDescription.getAttributeValue("internalName"));
																			filterGroupContainer.addAttribute(hiddenAttribute);
																			fieldTableKeyToAttribute.put(fieldName + tableName + keyName, hiddenAttribute);
																			hiddenAttribute.setHideValue(true);
																			if(filterDescription.getAttributeValue("displayName")!=null){
																				hiddenAttribute.setDisplayName(filterDescription.getAttributeValue("displayName"));
																			}
																			hiddenAttribute.setName(hiddenAttribute.getInternalName());

																			hiddenAttribute.setDescription(filterDescription.getAttributeValue("description",""));


																			
																			attribute = hiddenAttribute;
																		}
																	}
																	if(attribute==null){
																		Log.debug("Something's wrong");
																	}
																}
															} else if (tableName!=null && (tableName.equalsIgnoreCase("main") || tableName.endsWith("__main") )){
																DatasetTable currentTable= mainTableByKey.get(filterDescription.getAttributeValue("key"));

																if (currentTable != null) {
																	DatasetColumn currentColumn = currentTable.getColumnByName(fieldName);
																	if (currentColumn == null) {
																		//System.err.println("FILTER Can't create attribute, because column " + filterDescription.getAttributeValue("field") + " wasn't found in table " + currentTable.getName());
																	} else {
																		org.biomart.objects.objects.Attribute hiddenAttribute = new org.biomart.objects.objects.Attribute(currentColumn,filterDescription.getAttributeValue("internalName"));
																		filterGroupContainer.addAttribute(hiddenAttribute);
																		hiddenAttribute.setHideValue(true);
																		if(filterDescription.getAttributeValue("displayName")!=null){
																			hiddenAttribute.setDisplayName(filterDescription.getAttributeValue("displayName"));
																		}
																		hiddenAttribute.setName(hiddenAttribute.getInternalName());

																		fieldTableKeyToAttribute.put(fieldName + tableName + keyName, attribute);
																		attribute = hiddenAttribute;
																	}
																} else {
																	//System.err.println("FILTER Main table reference with non-existing key.");
																}
															}

														} else {
															//System.err.println("======> " + tableName+" " +fieldName+" "+keyName+" "+attribute.getName());
														}
														//TODO filter creation
														if(attribute!=null){ // If it's possible to find or make an attribute filter at this point, we've done it
															// so we can move on and create the filter, and populate it's data tree

															//Check if the attribute is part of a partition table and if it is get the current partition table

															Filter filter = new Filter(attribute,filterDescription.getAttributeValue("internalName"));

															if (attribute.getName().split("__").length > 1){
																//filter = new Filter(attribute,attribute.getName().split("__")[0] + "__" + filterDescription.getAttributeValue("internalName"));
																filter = new Filter(attribute,filterDescription.getAttributeValue("internalName"));
															}
															filterGroupContainer.addFilter(filter);


															if(filterDescription.getAttributeValue("displayName")!=null){

																filter.setDisplayName((filterDescriptions.size() == 1) ? filterCollectionName : filterCollectionPrefix + filterDescription.getAttributeValue("displayName"));

															}
															filter.setDescription(filterDescription.getAttributeValue("description", ""));

															String legalQualifiers = filterDescription.getAttributeValue("legal_qualifiers");
															if(legalQualifiers!=null){
																if(legalQualifiers.contains(">=")){
																	filter.setQualifier(OperatorType.GTE);
																} else if (legalQualifiers.contains("<=")){
																	filter.setQualifier(OperatorType.LTE);
																} else if (legalQualifiers.contains(">")){
																	filter.setQualifier(OperatorType.GT);
																} else if (legalQualifiers.contains("<")){
																	filter.setQualifier(OperatorType.LT);
																} else if (legalQualifiers.contains("in") || legalQualifiers.contains("=")){
																	filter.setQualifier(OperatorType.E);
																} else if (legalQualifiers.contains("like")){
																	filter.setQualifier(OperatorType.LIKE);
																} else {
																	filter.setQualifier(OperatorType.E);
																}
															}
															// Need to set filter's hidden status to be the same as the attached attribute
															//filter.setHideValueInString(attribute.getHideString());
															filter.setFilterType(newType);
															if(newType==FilterType.BOOLEAN){
																filter.setOnlyValue("Only");
																filter.setExcludedValue("Excluded");
															}
															//filterCollectionContainer.addFilter(filter);

															parseSpecificFilterContent(dataRoot, filterDescription, filter, "SpecificFilterContent");
															if(newType!=FilterType.BOOLEAN){
																parseFilterOptions(dataRoot,filterDescription,filter);
															}
														} else {
															//System.err.println("Filter " + filterDescription.getAttributeValue("internalName") + " couldn't be created, because no matching column was found");
														}
													}
												}
											} else if(filterDescription.getAttributeValue("hidden","false").equals("false")){ //TODO add in Filter Pointers
												if(filterDescription.getAttributeValue("pointerDataset") == null){
													Log.warn("Invalid pointer filter: " + config.getName() + " " + filterDescription.getAttributeValue("internalName"));
												} else if(isRemotePointer(mart.getPartitionTableByName("p0"), internalNames, filterDescription.getAttributeValue("pointerDataset",""), oldPartitionToColumn)){ // It's a non-local pointer
													String[] pointerDataset = filterDescription.getAttributeValue("pointerDataset").split("\\*",-1);
													if(pointerDataset.length <= 3){
														Filter pointerFilter = new Filter(filterDescription.getAttributeValue("internalName"), filterDescription.getAttributeValue("pointerFilter"), replaceAliases(filterDescription.getAttributeValue("pointerDataset"),oldPartitionToColumn));
														filterGroupContainer.addFilter(pointerFilter);
														this.pointedDatasets.get(mart.getName()).add(pointerFilter.getPointedDatasetName());
														//pointerFilter.setInternalName(filterDescription.getAttributeValue("pointerFilter"));
														//pointerAttribute.setConfigName("POINTER");

														//pointerFilter.setName(pointerFilter.getInternalName());


													} else if (pointerDataset.length > 3){
														Log.warn("Too many aliases in pointerDataset property!");
													} else {
														PartitionTable mainPartition = mart.getPartitionTableByName("p0");
														HashMap<String, String> internalNameToValue = oldPartitionAliases.get(pointerDataset[1]);
														for(String internalName : internalNameToValue.keySet()){
															Filter pointerFilter = new Filter(filterDescription.getAttributeValue("internalName"), filterDescription.getAttributeValue("pointerFilter"),  pointerDataset[0] + internalNameToValue.get(internalName) + pointerDataset[2]);
															filterGroupContainer.addFilter(pointerFilter);
															//pointerFilter.setInternalName(filterDescription.getAttributeValue("pointerFilter"));
															this.pointedDatasets.get(mart.getName()).add(pointerFilter.getPointedDatasetName());

															//pointerAttribute.setConfigName("POINTER");
															//pointerFilter.setName(pointerFilter.getInternalName());
															int hideColumn = mainPartition.addColumn("true");
															int row = mainPartition.getRowNumberByDatasetName(internalName);
															if(row >= 0)
																mainPartition.updateValue(row, hideColumn, "false");

															pointerFilter.setHideValueInString("(p0c" + Integer.toString(hideColumn) + ")");


														}
													}

												}

											}

											//filterGroupContainer.addContainer(filterCollectionContainer);
										}
									}
								}
							}
						}
					}
				}
				List importables = root.getChildren("Importable");
				Iterator importablesIterator = importables.iterator();
				while (importablesIterator.hasNext()) {
					boolean include = true;
					Element oldImportable = (Element) importablesIterator.next();
					String[] filters = oldImportable.getAttributeValue("filters").split(",");
					ElementList newImportable = new Importable(config, replaceAliases(oldImportable.getAttributeValue("linkName", "!NONAME!"),oldPartitionToColumn));
					newImportable.setProperty(XMLElements.ORDERBY, oldImportable.getAttributeValue("orderBy",""));
					newImportable.setLinkVersion(replaceAliases(oldImportable.getAttributeValue("linkVersion", ""), oldPartitionToColumn));
					newImportable.setProperty(XMLElements.TYPE,oldImportable.getAttributeValue("type","link"));

					for(String filterName : filters){
						Filter filterObject = rootContainer.getFilterRecursively2(filterName);
						if(filterObject == null){
							include = false;
							break;
						} else {
							newImportable.addFilter(filterObject);
						}
					}
					if(include && newImportable.getFilterList().size()>0){
						config.addElementList(newImportable);
					} else {
						Log.warn("Importable warning: " + newImportable.getName() + " not added");
						Log.warn("\t" + oldImportable.getAttributeValue("filters"));
					}

				}
				List exportables = root.getChildren("Exportable");
				Iterator exportablesIterator = exportables.iterator();
				while (exportablesIterator.hasNext()) {
					boolean include = true;
					Element oldExportable = (Element) exportablesIterator.next();
					String[] attributes = oldExportable.getAttributeValue("attributes").split(",");
					ElementList newExportable = new Exportable(config, replaceAliases(oldExportable.getAttributeValue("linkName", "!NONAME!"),oldPartitionToColumn));
					newExportable.setProperty(XMLElements.ORDERBY, oldExportable.getAttributeValue("orderBy",""));
					newExportable.setLinkVersion(replaceAliases(oldExportable.getAttributeValue("linkVersion", ""), oldPartitionToColumn));
					newExportable.setProperty(XMLElements.TYPE,oldExportable.getAttributeValue("type","link"));
					if(newExportable.getName().endsWith("uniprot_id"))
						Log.debug("Checking exportable");
					String defaultState = oldExportable.getAttributeValue("default","false");
					if(defaultState.equals("true") || defaultState.equals("1"))
						defaultState = "true";
					else
						defaultState = "false";
					newExportable.setDefaultState(new Boolean(defaultState));

					for(String attributeName : attributes){
						org.biomart.objects.objects.Attribute attributeObject = rootContainer.getAttributeRecursively2(attributeName);
						if(attributeObject == null){
							include = false;
							Log.warn("Attribute not found: " + attributeName);
							break;
						} else {
							attributeObject.setName(attributeObject.getInternalName());
							newExportable.addAttribute(attributeObject);
						}
					}
					if(include && newExportable.getAttributeList().size()>0){
						config.addElementList(newExportable);
					} else {
						Log.warn("Exportable warning: " + newExportable.getName() + " not added");
						Log.warn("\t" + oldExportable.getAttributeValue("attributes"));
					}
				}
				if(dataRoot.getChildren().size()>0){
					allRoot.addContent(martOptions);
					/*try {
						XMLOutputter outputter = new XMLOutputter();
						FileWriter writer = new FileWriter(Settings.getStorageDirectory().getCanonicalPath() + "/option/" + templateName + ".xml");
						outputter.output(xmlOptions, writer);
						writer.close();
					} catch (java.io.IOException e) {
						e.printStackTrace();
					}*/
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		Log.debug("Done");
		return martList;
	}

	private String processLinkoutURL(Element attributeDescription, Config config) {
		String linkOut = attributeDescription.getAttributeValue("linkoutURL","").replaceAll("%[sS]", "%s%");

		try{
			String attributeName = attributeDescription.getAttributeValue("internalName");

			if(linkOut.startsWith("exturl|http://") || linkOut.startsWith("exturl|https://") || linkOut.startsWith("exturl|ftp://"))
				linkOut = linkOut.replaceFirst("exturl\\|","");
			//linkOut = linkOut.replaceFirst("exturl\\|","%exturl%");
			Matcher m = Pattern.compile("^exturl[0-9]*?\\|").matcher(linkOut);
			if(m.find()) {
				String prefix = m.group();
				linkOut = linkOut.replaceFirst(Pattern.quote(prefix),"%" + prefix.replace("|", "") + "%");
				prefix = prefix.replace("|", "");
				if(config.getAttributeByName(prefix, null) == null){

					org.biomart.objects.objects.Attribute exturl = new org.biomart.objects.objects.Attribute(prefix, "External URL prefix");
					exturl.setHideValue(true);
					exturl.setDescription("This pseudoattribute is created by Backwards compatibility to allow linkOutURLs to function. Please manually check that the value is correct.");
					if(isWebService){
						exturl.setValue(this.dataLinkInfo.getUrlLinkObject().getFullHost());
					} else {
						exturl.setValue("");
					}
					config.getRootContainer().addAttribute(exturl);

				}
			}

			String[] splitLink = linkOut.split("\\|+",0);
			if(splitLink.length > 1){
				String[] splitURL = splitLink[0].split("%s%",-1);
				StringBuilder tempURL = new StringBuilder(splitURL[0]);
				for(int i = 1; i < splitLink.length; ++i){
					if(splitLink[i].equals(attributeName))
						splitLink[i] = "s";
					tempURL.append("%" + splitLink[i] + "%");
					tempURL.append(splitURL[i]);
				}
				linkOut = tempURL.toString();
			}
			return linkOut;

		} catch (ArrayIndexOutOfBoundsException e){
			return linkOut;
		}

	}
	
	//TODO replace the original processLinkOutURL with this one
	private String processLinkoutURL(Element attributeDescription, Config config, boolean useDataset) {
		String linkOut = attributeDescription.getAttributeValue("linkoutURL","").replaceAll("%[sSdDiIfF]", "%s%");

		try{
			String attributeName = attributeDescription.getAttributeValue("internalName");

			if(Pattern.compile("^exturl[0-9]*?\\|(http|https|ftp)://").matcher(linkOut).find())
				linkOut = linkOut.replaceFirst("exturl[0-9]*?\\|","");			
			Matcher m = Pattern.compile("^exturl[0-9]*?\\|").matcher(linkOut);
			if(m.find()) {
				String prefix = m.group();
				linkOut = linkOut.replaceFirst(Pattern.quote(prefix),"%" + prefix.replace("|", "") + "%");
				prefix = prefix.replace("|", "");
				if(config.getAttributeByName(prefix, null) == null){

					org.biomart.objects.objects.Attribute exturl = new org.biomart.objects.objects.Attribute(prefix, "External URL prefix");
					exturl.setHideValue(true);
					exturl.setDescription("This pseudoattribute is created by Backwards compatibility to allow linkOutURLs to function. Please manually check that the value is correct.");
					if(isWebService){
						exturl.setValue(this.currentDatasetURL.getProtocol() + "://" + this.currentDatasetURL.getHost());
					} else {
						exturl.setValue("");
					}
					config.getRootContainer().addAttribute(exturl);

				}
			}

			String[] splitLink = linkOut.split("\\|+",0);
			if(splitLink.length > 1){
				String[] splitURL = splitLink[0].split("%s%",-1);
				StringBuilder tempURL = new StringBuilder(splitURL[0]);
				for(int i = 1; i < splitLink.length; ++i){
					if(splitLink[i].equals(attributeName))
						splitLink[i] = "s";
					tempURL.append("%" + splitLink[i] + "%");
					tempURL.append(splitURL[i]);
				}
				linkOut = tempURL.toString();
			}
			linkOut = linkOut.replace("*", "%");
			return linkOut;

		} catch (ArrayIndexOutOfBoundsException e){
			return linkOut;
		}

	}

	private HashSet<String> getColumns(String tableName) {
		HashSet<String> currentColumns = new HashSet<String>();
		try {
			ResultSet result = this.databaseConnection.getMetaData().getColumns(null, this.schema, tableName, null);
			while (result.next()){
				//System.out.println(result.getString(1));
				currentColumns.add(result.getString(4));
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return currentColumns;
	}

	private void parseFilterOptions(Element dataRoot,
			Element parentNode, Filter filter) {
		List optionList = parentNode.getChildren("Option");
		if(optionList.size() > 0){
			Element filterXML = new Element("filter");
			filterXML.setAttribute(new Attribute("name",filter.getName()));
			parseFilterOptionsRecursive(parentNode,filterXML);
			dataRoot.addContent(filterXML);
		}
	}

	private void parseSpecificFilterContent(Element dataRoot,
			Element parentNode, Filter filter, String name) {
		List optionList = parentNode.getChildren(name);
		Iterator optionListIterator = optionList.iterator();
		if(optionList.size() > 0){
			Element filterXML = new Element("filter");
			filterXML.setAttribute(new Attribute("name",filter.getName()));
			while(optionListIterator.hasNext()){
				Element currentOption = (Element) optionListIterator.next();
				Element newPartition = new Element("dataset").setAttribute(new Attribute("name",currentOption.getAttributeValue("internalName","OOPS!")));
				parseFilterOptionsRecursive(currentOption, newPartition);
				filterXML.addContent(newPartition);
			}
			dataRoot.addContent(filterXML);
		}
	}
	/*if (optionList.size() > 0){
			Element filterXML = new Element("Filter");
			filterXML.setAttribute(new Attribute("name",filter.getName()));
			while(optionListIterator.hasNext()){
				Element filterXML = new Element("Filter");
				Element currentOption = (Element) optionListIterator.next();
				filterXML.setAttribute(new Attribute("name",filter.getName() + "___" + currentOption.getAttributeValue("internalName","OOPS!")));
				parseFilterOptionsRecursive(currentOption, filterXML);
			}
			dataRoot.addContent(filterXML);

		}
	}*/

	private void parseFilterOptionsRecursive(
			Element parentNode, Element rowNode) {
		List pushList = parentNode.getChildren("PushAction");
		Iterator pushListIterator = pushList.iterator();
		while(pushListIterator.hasNext()){
			Element currentPush = (Element) pushListIterator.next();
			String optionData = currentPush.getAttributeValue("ref","NULL"); // + separator + currentOption.getAttributeValue("displayName","NULL") + separator + currentOption.getAttributeValue("default","NULL");
			Element newPush = new Element("filter").setAttribute(new Attribute("name",optionData));
			parseFilterOptionsRecursive(currentPush, newPush);
			rowNode.addContent(newPush);
		}
		List optionList = parentNode.getChildren("Option");
		Iterator optionListIterator = optionList.iterator();
		while(optionListIterator.hasNext()){
			Element currentOption = (Element) optionListIterator.next();
			String optionData = currentOption.getAttributeValue("value","NULL").replace("|", "\\|") + separator + currentOption.getAttributeValue("displayName","NULL").replace("|", "\\|") + separator + currentOption.getAttributeValue("default","false");
			Element newRow = new Element("row").setAttribute(new Attribute("data",optionData));
			parseFilterOptionsRecursive(currentOption, newRow);
			rowNode.addContent(newRow);
		}
	}

	private Container populateContainer(Element element){
		Container container = new Container(element.getAttributeValue("internalName"));
		container.setInternalName(element.getAttributeValue("internalName"));
		if(element.getAttributeValue("hidden","false").equals("true")){
			container.setHideValue(true);
		}
		//String displayName = child.getAttributeValue("displayName");
		if(element.getAttributeValue("displayName")!=null){
			container.setDisplayName(element.getAttributeValue("displayName"));
		}
		container.setMaxAttributes(Integer.parseInt(element.getAttributeValue("maxSelect","0")));
		container.setDescription(element.getAttributeValue("description","NULL"));
		container.setHideValue(element.getAttributeValue("hideDisplay","false").equals("true"));
		return container;
	}
	
	private Container createContainer(Element element, Config config, Container parent){
		String name = element.getAttributeValue("internalName");
		String displayName = element.getAttributeValue("displayName", name);

		
		// Must add parent container's name to ensure uniqueness
		if(!(parent.getName().endsWith("root"))){
			name += ("__" + parent.getName());
		}
		
		Container container = config.getContainerByName(name);
		if(container == null){
			container = new Container(name);
			container.setInternalName(name);
			container.setHideValue(element.getAttributeValue("hidden","false").equals("true"));
			
			Integer maxSelect = Integer.parseInt(element.getAttributeValue("maxSelect","0"));
			container.setMaxAttributes(maxSelect);
			
			if(displayName !=null){
				container.setDisplayName(displayName);
			}
			
			container.setDescription(element.getAttributeValue("description",""));
			container.setHideValue(element.getAttributeValue("hideDisplay","false").equals("true"));
			parent.addContainer(container);
		}
		return container;
	}
	

	/**
	 *  The database connection for this invocation of the processor.
	 */
	private Connection databaseConnection = null;
	private String schema = null;

	private HashMap<String, byte[]> queryDBbytes(String query){
		HashMap<String, byte[]> resultList = new HashMap<String, byte[]>();
		try {
			Statement stmt = null;
			stmt = databaseConnection.createStatement();
			ResultSet result = stmt.executeQuery(query);

			while (result.next()){
				//System.out.println(result.getString(1));
				resultList.put(result.getString(1), result.getBytes(2));
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return resultList;
	}
	private static String replaceAliases(String oldText, HashMap<String, Integer> map){
		String newText = oldText;
		for(String alias: map.keySet()){
			newText = newText.replaceAll("\\*" + alias + "\\*", "(p0c" + map.get(alias).toString() + ")");
		}
		return newText;
	}

	private boolean isRemotePointer(PartitionTable partitionTable, HashSet<String> internalNames, String templateName, HashMap<String, Integer> map){
		if(templateName.contains("*")){
			String[] splitName = templateName.split("\\*");
			for(String prefix : partitionTable.getCol(map.get(splitName[1]))){
			if(internalNames.contains((splitName[0] + prefix + splitName[2])))
				return false;
			}
			return true;
		} else {
			if(internalNames.contains(templateName))
				return false;
			return true;
		}
	}
	private String separator = Resources.get("COLUMNSEPARATOR");

	private boolean isWebService = false;

	private boolean isDMPartitioned = false;

	private List<DatasetFromUrl> datasetList;
	private LinkedHashMap<String, DatasetFromUrl> datasetMap;

	//added by Yong
	private MartRegistry martRegistry;
	private DataLinkInfo dataLinkInfo;


	public void setDatasetsForUrl(List<DatasetFromUrl> dsList) {
		this.datasetList = dsList;
		datasetMap = new LinkedHashMap<String,DatasetFromUrl>();
		for(DatasetFromUrl ds: dsList) {
			datasetMap.put(ds.getName(), ds);
		}
	}

	public void setMartRegistry(MartRegistry martRegistry) {
		this.martRegistry = martRegistry;
	}


	public void setConnectionObject(Connection conObj) {
		this.databaseConnection = conObj;
	}
	
	
	
	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setDataLinkInfoObject(DataLinkInfo dlink) {
		this.dataLinkInfo = dlink;
		if(this.dataLinkInfo.getDataLinkType().equals(DataLinkType.URL)) {
			this.isWebService = true;
		}
	}
}
