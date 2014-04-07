package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.KeyNumberComparator;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.TargetSchema;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.linkIndices.LinkIndices;
import org.biomart.configurator.model.JoinTable;
import org.biomart.configurator.model.Relations;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.utils.type.ComponentStatus;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.objects.enums.DatasetOptimiserType;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Element;

public class Mart extends MartConfiguratorObject {

	private boolean hasSource;
	private LinkIndices linkIndices;
	private List<Config> configList;
	private List<PartitionTable> partitionTableList;
	private SortedMap<String,DatasetTable> tableList;
	//centralTable is a source table
	private Table centralTable;	
	private TargetSchema targetSchema;
	private List<SourceSchema> sourceSchemaList;	
	private Relations relations;
	
	//for open source xml
	private Element transformElement = null;
	
	
	public void clean(){
		configList.clear();
		partitionTableList.clear();
		tableList.clear();
		sourceSchemaList.clear();
	}
	@Deprecated
	public void addTable(DatasetTable table) {
		this.tableList.put(table.getName(), table);
		table.setParent(this);
		//set all relations' parent to this
		for(Relation r: table.getRelations()) {
			r.setParent(this);
		}
	}

	public void addConfig(Config config) {
		if(!this.configList.contains(config)) {	
			this.configList.add(config);
			config.setParent(this);
			MartController.getInstance().setChanged(true);
		}
	}

	
	public void addPartitionTable(PartitionTable partitionTable) {
		this.partitionTableList.add(partitionTable);
		partitionTable.setParent(this);
	}

	
	/**
	 * return the default config, if no, return the 1st config
	 */
	public Config getDefaultConfig() {
		for(Config config: this.configList) {
			if(config.isDefaultConfig())
				return config;
		}
		return this.configList.size()>=1 ? this.configList.get(0) : null;
	}
	public List<Config> getConfigList() {		
		return this.configList;
	}
	public List<PartitionTable> getPartitionTableList() {
		return partitionTableList;
	}

	public Collection<DatasetTable> getDatasetTables() {
		return this.tableList.values();
	}
	public DatasetTable getTableByName(String name) {
		return this.tableList.get(name);
	}

	/**
	 * get dataset list based on the schema partition table
	 * @return TODO???, return stateless dataset objects
	 */
	public List<Dataset> getDatasetList() {
		List<Dataset> dsList = new ArrayList<Dataset>();
		//get schema partition table
		PartitionTable schemaPt = this.getSchemaPartitionTable();
		if(schemaPt == null)
			return dsList;
		for(List<String> rowList: schemaPt.getTable()) {
			Dataset ds = new Dataset(this,rowList.get(PartitionUtils.DATASETNAME));
			ds.setDisplayName(rowList.get(PartitionUtils.DISPLAYNAME));
			ds.setHideValue(!(new Boolean(rowList.get(PartitionUtils.HIDE))));
			dsList.add(ds);
		}
		return dsList;
	}
	
	public Dataset getDatasetByName(String name) {
		for(Dataset ds: this.getDatasetList()) {
			if(ds.getName().equals(name))
				return ds;
		}
		return null;
	}
	
	public Dataset getDatasetBySuffix(String _suffix) {
		for(Dataset ds: this.getDatasetList()) {
			if(ds.getName().endsWith("_"+_suffix))
				return ds;
		}
		return null;		
	}

	public PartitionTable getSchemaPartitionTable() {
		for(PartitionTable pt: this.getPartitionTableList()) {
			if(pt.getPartitionType().equals(PartitionType.SCHEMA))
				return pt;
		}
		return null;
	}
	public Config getConfigByName(String name) {
		for(Config config: this.configList) {
			if(config.getName().equals(name))
				return config;
		}
		return null;
	}
	public PartitionTable getPartitionTableByName(String name) {
		for(PartitionTable pt: this.partitionTableList) {
			if(pt.getName().equals(name))
				return pt;
		}
		return null;
	}

	
	@Override
	public int hashCode() {
		int HASH_SEED = 31;
		return HASH_SEED+this.getName().hashCode();	
	}
	
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}else if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		
		Mart mart = (Mart)object;
		return this.getName().equals(mart.getName());
	}
	
	public Element generateXml() throws FunctionalException{
		return this.generateXml(null,false);
	}

	public Element generateXml(String configName,boolean hideuser) throws FunctionalException {
		Element element = new Element(XMLElements.MART.toString());
		super.saveConfigurableProperties(element);
		element.setAttribute(XMLElements.NAME.toString(), this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(), this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.DESCRIPTION));
		element.setAttribute(XMLElements.HIDE.toString(),this.getPropertyValue(XMLElements.HIDE));
		element.setAttribute(XMLElements.ID.toString(),this.getPropertyValue(XMLElements.ID));
		element.setAttribute(XMLElements.GROUP.toString(),this.getPropertyValue(XMLElements.GROUP));
		element.setAttribute(XMLElements.OPTIMISER.toString(), this.getPropertyValue(XMLElements.OPTIMISER));

		element.setAttribute(XMLElements.INDEXOPTIMISED.toString(), this.getPropertyValue(XMLElements.INDEXOPTIMISED));
		element.setAttribute(XMLElements.VIRTUAL.toString(),this.getPropertyValue(XMLElements.VIRTUAL));
		element.setAttribute(XMLElements.SEARCHFROMTARGET.toString(),this.getPropertyValue(XMLElements.SEARCHFROMTARGET));
		if(this.hasSource) {		
			element.setAttribute(XMLElements.CENTRALTABLE.toString(),this.centralTable.getName());
		} else {
			Element tablesElement = new Element(XMLElements.TABLES.toString());
			element.addContent(tablesElement);
			for (DatasetTable table : this.tableList.values()) {
				tablesElement.addContent(table.generateXml());
			}
			
			Element relationsElement = new Element(XMLElements.RELATIONS.toString());
			element.addContent(relationsElement);
			for (Relation relation : this.getRelations()) {
				relationsElement.addContent(relation.generateXml());
			}
		}
		
			
		for (PartitionTable partitionTable : this.partitionTableList) {
			element.addContent(partitionTable.generateXml());
		}
		
		if(this.getIncludedSchemas()!=null)
			for(SourceSchema schema: this.getIncludedSchemas()) {
				element.addContent(schema.generateXml());
			}

		/*
		 * save master config first
		 */
		List<Config> orderedConfig = new ArrayList<Config>();
		for(Config config: this.configList) {
			if(config.isMasterConfig())
				orderedConfig.add(0,config);
			else
				orderedConfig.add(config);
		}
		for (Config config : orderedConfig) {
			if(McUtils.isStringEmpty(configName))
				element.addContent(config.generateXml());
			else {
				if(config.getName().equals(configName))
					element.addContent(config.generateXml());					
			}
		}
		
		//selected tables
		List<DatasetTable> mainDsList = this.getOrderedMainTableList();
		if(this.hasSource && mainDsList.size()>1) {
			List<String> selectedTables = new ArrayList<String>();
			selectedTables.add(this.centralTable.getName());
			for(int i=1; i< mainDsList.size(); i++) {
				DatasetTable dst = mainDsList.get(i);
				List<TransformationUnit> tus = dst.getTransformationUnits();
				//get the last one
				TransformationUnit tu = tus.get(1);
				if(tu instanceof JoinTable) {
					selectedTables.add(((JoinTable)tu).getTable().getName());
				}
			}
			if(selectedTables.size()>1) {
				this.setProperty(XMLElements.SELECTEDTABLES, McUtils.StrListToStr(selectedTables, ","));
				element.setAttribute(XMLElements.SELECTEDTABLES.toString(), this.getPropertyValue(XMLElements.SELECTEDTABLES));
			}
		}
		Element afterTransformElement = new Element(XMLElements.TRANSFORM.toString());
		boolean hasAfterTransform = false;

		//check transform to save
		if(this.hasSource) {
			for (DatasetTable table : this.tableList.values()) {
				if(table.isHidden()) {
					hasAfterTransform = true;
					Element tableElement = new Element(XMLElements.TABLE.toString());
					tableElement.setAttribute(XMLElements.NAME.toString(),table.getName());
					tableElement.setAttribute(XMLElements.HIDE.toString(),XMLElements.TRUE_VALUE.toString());
					afterTransformElement.addContent(tableElement);
				}  else {
					for(Column column: table.getColumnList()) {
						if(column.isHidden()) {
							hasAfterTransform = true;
							Element columnElement = new Element(XMLElements.COLUMN.toString());
							columnElement.setAttribute(XMLElements.NAME.toString(),column.getName());
							columnElement.setAttribute(XMLElements.TABLE.toString(),table.getName());
							columnElement.setAttribute(XMLElements.HIDE.toString(),XMLElements.TRUE_VALUE.toString());
							afterTransformElement.addContent(columnElement);
						}
					}
				}			
			}

			if(hasAfterTransform) {
				element.addContent(afterTransformElement);
			}

		}
		return element;		
	}
	
 	public Mart(MartRegistry registry, String name, Table centralTable) {
		super(registry.getNextMartName(name));
		this.parent = registry;
		this.tableList = new TreeMap<String, DatasetTable>(McUtils.BETTER_STRING_COMPARATOR);
		this.configList = new ArrayList<Config>();
		this.partitionTableList = new ArrayList<PartitionTable>();
		this.sourceSchemaList = new ArrayList<SourceSchema>();
		this.relations = new Relations();
		this.setNodeType(McNodeType.MART);		
		this.setDatasetOptimiserType(DatasetOptimiserType.NONE);
		this.setProperty(XMLElements.INDEXOPTIMISED, XMLElements.FALSE_VALUE.toString());
		this.centralTable = centralTable;
		this.setProperty(XMLElements.ID, ""+McUtils.getNextUniqueMartId(registry));
		//registry.addMart(this);
	}
 	
	public Mart(Element element) {
		super(element);
		this.tableList = new TreeMap<String,DatasetTable>(McUtils.BETTER_STRING_COMPARATOR);
		this.configList = new ArrayList<Config>();
		this.partitionTableList = new ArrayList<PartitionTable>();
		this.sourceSchemaList = new ArrayList<SourceSchema>();
		this.relations = new Relations();
		this.setNodeType(McNodeType.MART);

		//partitiontable
		@SuppressWarnings("unchecked")		
		List<Element> ptElementList = element.getChildren(XMLElements.PARTITIONTABLE.toString());
		for(Element ptE: ptElementList) {
			PartitionTable pt = new PartitionTable(ptE);
			this.addPartitionTable(pt);
		}
		
		//source schema
		Element ssElement = element.getChild(XMLElements.SOURCESCHEMA.toString());
		if(ssElement!=null) {
			SourceSchema ss = new SourceSchema(ssElement);
			this.addSourceSchema(ss);
		}
		
		//backwardcompatibility for old xml that doesn't have 'tables' tag
		List<Element> dstElementList = null;
		Element tablesElement = element.getChild(XMLElements.TABLES.toString());
		if(null!=tablesElement) {
			dstElementList = tablesElement.getChildren();
		} else		
			dstElementList = element.getChildren(XMLElements.TABLE.toString());
		
		if(null!=dstElementList) {
			for(Element dstE: dstElementList) {
				DatasetTable dst = new DatasetTable(dstE);
				this.addTable(dst);
			}
		}
		
		//backwardcompatibility for old xml that doesn't have 'relations' tag
		//relation
		List<Element> relationElementList = null;
		Element relationsElement = element.getChild(XMLElements.RELATIONS.toString());
		if(null!=relationsElement) {
			relationElementList = relationsElement.getChildren();
		} else {
			relationElementList = element.getChildren(XMLElements.RELATION.toString());
		}
		
		this.tmpRelations = new ArrayList<Relation>();
		if(null!=relationElementList) {
			for(Element relationElement: relationElementList) {
				RelationTarget relation = new RelationTarget(relationElement);
				relation.setParent(this);
				this.relations.addRelation(relation);
				this.tmpRelations.add(relation);
			}
		}
		
		//config
		@SuppressWarnings("unchecked")
		List<Element> configElementList = element.getChildren(XMLElements.CONFIG.toString());
		for(Element configElement: configElementList) {
			Config config = new Config(configElement);
			this.addConfig(config);
		}
		
		//transform
		this.transformElement = element.getChild(XMLElements.TRANSFORM.toString());
		if(this.transformElement!=null)
			this.transformElement.detach();

	}
	
	
	public List<DatasetTable> getDatasetTablesForDataset(String dsName) {
		List<DatasetTable> result = new ArrayList<DatasetTable>();
		for(DatasetTable dst: this.tableList.values()) {
			if(dst.inPartition(dsName))
				result.add(dst);
		}
		return result;
	}
	
	public MartRegistry getMartRegistry() {
		return (MartRegistry)this.parent;
	}
	
	/**
	 * Returns the post-creation optimiser type this dataset will use.
	 * 
	 * @return the optimiser type that will be used.
	 */
	public DatasetOptimiserType getDatasetOptimiserType() {
		return DatasetOptimiserType.valueFrom(this.getPropertyValue(XMLElements.OPTIMISER));
	}
	
	/**
	 * Sets the post-creation optimiser type this dataset will use.
	 * 
	 * @param optimiser
	 *            the optimiser type to use.
	 */
	public void setDatasetOptimiserType(DatasetOptimiserType optimiser) {
		if(optimiser == null)
			optimiser = DatasetOptimiserType.NONE;
		Log.debug("Setting optimiser to " + optimiser + " in " + this);
		final DatasetOptimiserType oldValue = this.getDatasetOptimiserType();
		if (oldValue!=null && oldValue.equals(optimiser))
			return;
		this.setProperty(XMLElements.OPTIMISER, optimiser.toString());
	}

	/**
	 * Sees if the optimiser will index its columns.
	 * 
	 * @return <tt>true</tt> if it will.
	 */
	public boolean isIndexOptimiser() {
		return Boolean.parseBoolean(this.getPropertyValue(XMLElements.INDEXOPTIMISED));
	}
	
	/**
	 * Sets the optimiser index type.
	 * 
	 * @param index
	 *            the optimiser index if <tt>true</tt>.
	 */
	public void setIndexOptimiser(final boolean index) {
		Log.debug("Setting optimiser index to " + index + " in " + this);
		this.setProperty(XMLElements.INDEXOPTIMISED, Boolean.toString(index));
	}

	public List<SourceSchema> getIncludedSchemas() {
		return this.sourceSchemaList;
	}
	
	public void addSourceSchema(SourceSchema schema) {
		if(!this.sourceSchemaList.contains(schema)) {
			int id = McUtils.getNextUniqueSchemaId(this);
			schema.setUniqueId(id);
			this.sourceSchemaList.add(schema);
			schema.setParent(this);
		}
	}

	public Collection<Relation> getRelations() {
		return this.relations.getRelations();
	}
	

	public Table getCentralTable() {
		return this.centralTable;
	}
	
	public Table getRealCentralTable() {
		Log.debug("Finding actual central table");
		// Identify main table.
		final Table realCentralTable = this.getCentralTable();
		Table centralTable = realCentralTable;
		// If central table has subclass relations and is at the M key
		// end, then follow them to the real central table.
		boolean found;
		do {
			found = false;
			for (final Iterator<ForeignKey> i = centralTable.getForeignKeys().iterator(); i
					.hasNext()
					&& !found;)
				for (final Iterator<Relation> j = i.next().getRelations()
						.iterator(); j.hasNext() && !found;) {
					final Relation rel =  j.next();
					if (rel.isSubclassRelation(null)) {
						centralTable = rel.getFirstKey().getTable();
						found = true;
					}
				}
		} while (found && centralTable != realCentralTable);
		Log.debug("Actual central table is " + centralTable);
		return centralTable;
	}
	
	/**
	 * 
	 * @param table a sourcetable
	 */
	public void setCentralTable(Table table) {
		this.centralTable = table;
	}

	public void removePartitionTable(PartitionTable pt) {
		this.partitionTableList.remove(pt);
	}
	
	public DatasetTable getMainTable() {
		for(DatasetTable table: this.tableList.values()) {
			if(table.getType().equals(DatasetTableType.MAIN))
				return table;
		}
		return null;
	}
	

	public LinkIndices getLinkIndices() {
		if(this.linkIndices == null)
			this.linkIndices = new LinkIndices();
		return this.linkIndices;
	}
	
	public int getDatasetRowNumber(Dataset ds) {
		int i=0;
		for(List<String> rowList: this.getSchemaPartitionTable().getTable()) {
			if(rowList.get(5).equals(ds.getName()))
				return i;
			i++;
		}
		return -1;
	}
	
	
	/**
	 * return a list of main table, ordered by main, submain sub_submain
	 */
	public List<DatasetTable> getOrderedMainTableList() {
		List<DatasetTable> dstList = new ArrayList<DatasetTable>();
		List<DatasetTable> tmpList = new ArrayList<DatasetTable>();
		for(DatasetTable dst: this.getDatasetTables()) {
			if(dst.getType() == DatasetTableType.MAIN)
				dstList.add(dst);
			else if(dst.getType() == DatasetTableType.MAIN_SUBCLASS)
				tmpList.add(dst);
		}
		if(tmpList.size()>0) {
			if(tmpList.size() == 1)
				dstList.add(tmpList.get(0));
			else {
				Collections.sort(tmpList, new KeyNumberComparator());
				for(DatasetTable dst: tmpList) 
					dstList.add(dst);
			}
		}
		return dstList;
	}

	public void removeDatasetTable(DatasetTable table) {
		this.tableList.remove(table.getName());
	}
	
	public void removeConfig(Config config) {
		if(this.configList.remove(config)) {
			MartController.getInstance().setChanged(true);
		}
	}

	public List<MartConfiguratorObject> getReferences() {
		List<MartPointer> mpList = this.getMartRegistry().getPortal().getRootGuiContainer().getMartPointerListforMart(this);
		List<MartConfiguratorObject> references = new ArrayList<MartConfiguratorObject>();
		for(MartPointer mp: mpList) {
			references.add(mp);
		}
		return references; 
	}

	@Deprecated
	public void synchronizedFromXML() {		
		//partitiontable
		
		//source table
		if(this.getIncludedSchemas()!=null)
			for(SourceSchema schema: this.getIncludedSchemas()) {
				schema.synchronizedFromXML();
			}
		
		//centraltable
		for(SourceSchema schema: this.getIncludedSchemas()) {
			Table table = schema.getTableByName(this.getPropertyValue(XMLElements.CENTRALTABLE));
			if(table!=null) {
				this.centralTable = table;
				this.hasSource = true;
			}
		}
		if(hasSource) {
			//restore the target, first dataset for now
			DataLinkInfo dli = this.getDatasetList().get(0).getDataLinkInfoNonFlip();
			Map<MartInVirtualSchema,List<DatasetFromUrl>> allTablesMap = new HashMap<MartInVirtualSchema,List<DatasetFromUrl>>();
			JdbcLinkObject jdbcLinkObj = dli.getJdbcLinkObject();
			for(SourceSchema schema: this.getIncludedSchemas()) {
				MartInVirtualSchema martV = new MartInVirtualSchema.DBBuilder().name(schema.getName()).displayName(schema.getName())
					.schema(schema.getName()).database(jdbcLinkObj.getDatabaseName()).host(jdbcLinkObj.getHost()).port(jdbcLinkObj.getPort())
					.username(jdbcLinkObj.getUserName()).password(jdbcLinkObj.getPassword()).type(jdbcLinkObj.getJdbcType().toString()).build();
				List<DatasetFromUrl> tables = new ArrayList<DatasetFromUrl>();
				for(Table table: schema.getTables()) {
					DatasetFromUrl dsUrl = new DatasetFromUrl();
					dsUrl.setName(table.getName());
					dsUrl.setDisplayName(table.getDisplayName());
					tables.add(dsUrl);
				}
				allTablesMap.put(martV, tables);
			}
			dli.getJdbcLinkObject().setFullDsInfoMap(allTablesMap);
			
			Map<MartInVirtualSchema,List<DatasetFromUrl>> selectedMap = new HashMap<MartInVirtualSchema,List<DatasetFromUrl>>();
			List<DatasetFromUrl> tables = new ArrayList<DatasetFromUrl>();
			DatasetFromUrl dsUrl = new DatasetFromUrl();
			dsUrl.setDisplayName(this.centralTable.getName());
			dsUrl.setName(this.centralTable.getName());
			tables.add(dsUrl);	
			if(!McUtils.isStringEmpty(this.getPropertyValue(XMLElements.SELECTEDTABLES))) {
				String[] selectedTables = this.getPropertyValue(XMLElements.SELECTEDTABLES).split(",");
				for(int i=1; i<selectedTables.length; i++) {
					DatasetFromUrl ds = new DatasetFromUrl();
					ds.setName(selectedTables[i]);
					ds.setDisplayName(selectedTables[i]);
					tables.add(ds);
				}
			}
			//should be schema name
			MartInVirtualSchema martV = new MartInVirtualSchema.DBBuilder().name(jdbcLinkObj.getSchemaName()).displayName(jdbcLinkObj.getSchemaName())
				.schema(jdbcLinkObj.getSchemaName()).database(jdbcLinkObj.getDatabaseName()).host(jdbcLinkObj.getHost()).port(jdbcLinkObj.getPort())
				.username(jdbcLinkObj.getUserName()).password(jdbcLinkObj.getPassword()).type(jdbcLinkObj.getJdbcType().toString()).build();
			selectedMap.put(martV,tables);
			dli.getJdbcLinkObject().setDsInfoMap(selectedMap);
	    	Collection<Mart> dss = null;
			try {
				dss = MartController.getInstance().requestCreateMartsFromSource(this.getMartRegistry(), dli,this.getIncludedSchemas());
			} catch (MartBuilderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	if(dss.size()>0) {
	    		//this.tableList.clear();
	    		for(Table dst: dss.iterator().next().getDatasetTables()) {
	    			this.addTable((DatasetTable)dst);
	    		}
	    		for(Relation r: dss.iterator().next().getRelations()) {
	    			this.addRelation(r);
	    		}
	    	}
	    	//check transform
	    	if(this.transformElement!=null) {
	    		List<Element> elementList = this.transformElement.getChildren(XMLElements.TABLE.toString());
	    		for(Element tableElement: elementList) {
	    			DatasetTable dst = this.getTableByName(tableElement.getAttributeValue(XMLElements.NAME.toString()));
	    			if(dst!=null) {
	    				dst.setHideValue(true);
	    				for(Column column: dst.getColumnList()) {
	    					column.setHideValue(true);
	    				}
	    			}
	    		}
	    		List<Element> colElements = this.transformElement.getChildren(XMLElements.COLUMN.toString());
	    		for(Element colElement: colElements) {
	    			DatasetTable dst = this.getTableByName(colElement.getAttributeValue(XMLElements.TABLE.toString()));
	    			if(dst!=null) {
	    				DatasetColumn column = dst.getColumnByName(colElement.getAttributeValue(XMLElements.NAME.toString()));
	    				if(column!=null)
	    					column.setHideValue(true);
	    			}
	    		}
	    	}
		} else {
		
			//datasettable
			for(DatasetTable dst: this.tableList.values()) {
				dst.synchronizedFromXML();
			}
			//relation
			if(null!=this.tmpRelations)
				for(Relation rel: this.tmpRelations) {
					rel.synchronizedFromXML();
				}
		}

		
		this.setObjectStatus(ValidationStatus.VALID);
	}
		
	public void setHasSource(boolean b) {
		this.hasSource = b;
	}

	public boolean hasSource() {
		return hasSource;
	}



	public void setTargetSchema(TargetSchema jdbcSchema) {
		this.targetSchema = jdbcSchema;
		if(jdbcSchema.getSourceSchema()!=null)
			jdbcSchema.getSourceSchema().setParent(this);
	}

	public TargetSchema getTargetSchema() {
		return targetSchema;
	}

	/*
	 * for reading from element only
	 */
	private List<Relation> tmpRelations;

	@Override
	public List<MartConfiguratorObject> getChildren() {
		List<MartConfiguratorObject> children = new ArrayList<MartConfiguratorObject>();
		for(PartitionTable pt: this.partitionTableList)
			children.add(pt);
		
		for(Table dst: this.tableList.values()) 
			children.add(dst);
		
		for(Relation r: this.getRelations())
			children.add(r);
		
		for(Config config: this.configList)
			children.add(config);
		
		return children;
	}

	public Config getMasterConfig() {
		for(Config config: this.configList) {
			if(config.isMasterConfig())
				return config;
		}
		return null;
	}

	public void setVirtual(boolean b) {
		this.setProperty(XMLElements.VIRTUAL, Boolean.toString(b));
	}
	
	public boolean isVirtual() {
		return this.getPropertyValue(XMLElements.VIRTUAL).equals(XMLElements.TRUE_VALUE.toString());
	}

	public boolean searchFromTarget() {
		if(!hasSource)
			return true;
		else
			return Boolean.parseBoolean(this.getPropertyValue(XMLElements.SEARCHFROMTARGET));
	}
	
	public void setSearchFromTarget(boolean b) {
		this.setProperty(XMLElements.SEARCHFROMTARGET, Boolean.toString(b));
	}

	public int getUniqueId() {
		String id = this.getPropertyValue(XMLElements.ID);
		if("".equals(id))
			return 0;
		else
			return Integer.parseInt(id);
	}

	public boolean isURLbased() {
		for(Dataset ds:this.getDatasetList()){
			if(!ds.getDataLinkType().equals(DataLinkType.URL))
				return false;
		}
		return true;
	}

	
	/**
	 * return default is it is empty
	 */
	public String getGroupName() {
		if(McUtils.isStringEmpty(this.getPropertyValue(XMLElements.GROUP))) {
			return XMLElements.DEFAULT.toString();
		} else
			return this.getPropertyValue(XMLElements.GROUP);
	}

	public void setGroupName(String name) {
		this.setProperty(XMLElements.GROUP, name);
		if(null==this.getMartRegistry().getSourcecontainers().getSourceContainerByName(name)) {
			SourceContainer sc = new SourceContainer(name);
			this.getMartRegistry().getSourcecontainers().addSourceContainer(sc);
		}
	}

	public SourceContainer getSourceContainer() {
		return this.getMartRegistry().getSourcecontainers().
			getSourceContainerByName(this.getGroupName());
	}

	public List<Link> getLinkList() {
		return this.getMasterConfig().getLinkList();
	}
	
	public void addRelation(Relation r) {
		this.relations.addRelation(r);
	}
	
	public void removeRelation(Relation r) {
		this.relations.removeRelation(r);
	}
	
	public Collection<Relation> getRelationByFirstTable(String tableName) {
		return this.relations.getRelationByFirstTable(tableName);
	}
	
	public Collection<Relation> getRelationBySecondTable(String tableName) {
		return this.relations.getRelationBySecondTable(tableName);
	}
}
