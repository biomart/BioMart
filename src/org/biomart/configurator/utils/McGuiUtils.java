package org.biomart.configurator.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import net.infonode.docking.RootWindow;
import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.XMLElements;
import org.biomart.common.utils2.MyUtils;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.type.McGuiType;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.McViewType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.objects.PrimaryKey;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * this class should not be used for web, should only be used for
 * MartConfigurator GUI.
 */
public enum McGuiUtils {
	INSTANCE;

	private McGuiType guiType;

	private Map<McViewType, Map<McNodeType, Map<String, String>>> filterMap = new HashMap<McViewType, Map<McNodeType, Map<String, String>>>();
	private MartRegistry registryObject;
	private RootWindow rootWindow;
	
	private int cutCopyOperation;

	public int getCutCopyOperation() {
		return cutCopyOperation;
	}

	public void setCutCopyOperation(int cutCopyOperation) {
		this.cutCopyOperation = cutCopyOperation;
	}

	public void setGuiType(McGuiType guiType) {
		this.guiType = guiType;
	}

	public McGuiType getGuiType() {
		return guiType;
	}
	
	public void setRegistry(MartRegistry registry) {
		this.registryObject = registry;
	}
	
	public MartRegistry getRegistryObject() {
		if(this.registryObject == null)
			this.registryObject = new MartRegistry(XMLElements.MARTREGISTRY.toString());
		return this.registryObject;
	}

	public boolean checkUniqueFilterInMart(Config mart, String name) {
		if (mart.getFilterByName(null, name, true) != null)
			return false;
		else
			return true;
	}

	public String getUniqueMartPointerName(GuiContainer rootGc, String baseName) {
		String tmpName = baseName;
		int i = 1;
		while (rootGc.getMartPointerByNameRecursively(tmpName) != null) {
			tmpName = baseName + "_" + i++;
		}
		return tmpName;
	}
	
	public String getUniqueMartName(MartRegistry registry, String base) {
		String tmpName = base;
		int i=1;
		while(registry.getMartByName(tmpName)!=null) {
			tmpName = base + "_"+i++;
		}
		return tmpName;
	}

	public String getUniqueFilterName(Config mart, String baseName) {
		String tmpName = baseName;
		int i = 1;
		while (mart.getFilterByName(null, tmpName, true) != null) {
			tmpName = baseName + "_" + i++;
		}
		return tmpName;
	}
	public String getPointedFilterName(Filter filter, Config parentConfig){
		try{
			StringBuilder result = new StringBuilder();		
			result.append(parentConfig.getMart().getName());
			result.append('.');
			result.append(parentConfig.getName());
			result.append('.');
			result.append(filter.getName());
			return result.toString();
		}catch(NullPointerException e){
			e.printStackTrace();
			return "";
		}
	}
	public String getPointedFilterName(Filter filter){
		try{
			StringBuilder result = new StringBuilder();		
			result.append(filter.getParentConfig().getMart().getName());
			result.append('.');
			result.append(filter.getParentConfig().getName());
			result.append('.');
			result.append(filter.getName());
			return result.toString();
		}catch(NullPointerException e){
			e.printStackTrace();
			return "";
		}
	}

	public boolean checkUniqueAttributeInConfig(Config config, String name) {
		if (config.getAttributeByName(null, name, true) != null)
			return false;
		else
			return true;
	}


	public String getPointedAttributeName(Attribute attr, Config parentConfig){
		try{
			StringBuilder result = new StringBuilder();
			result.append(parentConfig.getMart().getName());
			result.append('.');
			result.append(parentConfig.getName());
			result.append('.');
			result.append(attr.getName());
			return result.toString();
		}catch(NullPointerException e){
			e.printStackTrace();
			return "";
		}
	}
	public String getPointedAttributeName(Attribute attr){
		try{
			StringBuilder result = new StringBuilder();
			result.append(attr.getParentConfig().getMart().getName());
			result.append('.');
			result.append(attr.getParentConfig().getName());
			result.append('.');
			result.append(attr.getName());
			return result.toString();
		}catch(NullPointerException e){
			e.printStackTrace();
			return "";
		}
	}

	public String getUniqueConfigName(Mart mart, String baseName) {
		String tmpName = baseName;
		int i = 1;
		while (mart.getConfigByName(tmpName) != null) {
			tmpName = baseName + "_" + i++;
		}
		return tmpName;
	}

	public void setRootWindow(RootWindow root) {
		this.rootWindow = root;
	}

	public RootWindow getRootWindow() {
		return this.rootWindow;
	}

	public McTreeNode findTreeNodeRecursively(McTreeNode parent,
			McNodeType type, String name) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			McTreeNode child = (McTreeNode) parent.getChildAt(i);
			if (child.getObject().getNodeType() == type
					&& child.getObject().getName().equals(name))
				return child;
			McTreeNode tmp = this.findTreeNodeRecursively(child, type, name);
			if (tmp != null)
				return tmp;
		}
		return null;
	}

	public McTreeNode findTreeNodeRecursively(McTreeNode parent,
			MartConfiguratorObject mcObj) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			McTreeNode child = (McTreeNode) parent.getChildAt(i);
			//if (child.getObject().equals(mcObj))
			if(child.getObject().getName().equals(mcObj.getName()))
				return child;
			McTreeNode tmp = this.findTreeNodeRecursively(child, mcObj);
			if (tmp != null)
				return tmp;
		}
		return null;
	}

	public List<MartConfiguratorObject> getPathToNode(McTreeNode root,
			MartConfiguratorObject mcObj) {
		List<MartConfiguratorObject> resultList = new ArrayList<MartConfiguratorObject>();
		MartConfiguratorObject tmp = mcObj;
		while (!tmp.equals(root.getObject())) {
			resultList.add(tmp);
			tmp = tmp.getParent();
		}
		resultList.add(tmp);
		// reverse
		Collections.reverse(resultList);
		return resultList;
	}

	public void setFilterMap() {
		this.filterMap = this.createFilterMap();
	}

	public Map<McNodeType, Map<String, String>> getFilterMap(McViewType view) {
		return this.filterMap.get(view);
	}

	private Map<McViewType, Map<McNodeType, Map<String, String>>> createFilterMap() {
		Map<McViewType, Map<McNodeType, Map<String, String>>> filterMap = new HashMap<McViewType, Map<McNodeType, Map<String, String>>>();
		// configuration
		Map<McNodeType, Map<String, String>> config = new HashMap<McNodeType, Map<String, String>>();
		config.put(McNodeType.SCHEMA, null);
		config.put(McNodeType.PARTITIONTABLE, null);
		if (Boolean.parseBoolean(Settings.getProperty("impexp.hide"))) {
			config.put(McNodeType.IMPORTABLE, null);
			config.put(McNodeType.EXPORTABLE, null);
		}
		if(!Boolean.parseBoolean(Settings.getProperty("rdf.show"))) {
			config.put(McNodeType.RDFCLASS,null);
		}
		
		config.put(McNodeType.TABLE, null);
		config.put(McNodeType.RELATION, null);
		config.put(McNodeType.LINK, null);
		filterMap.put(McViewType.CONFIGURATION, config);

		Map<McNodeType, Map<String, String>> target = new HashMap<McNodeType, Map<String, String>>();
		target.put(McNodeType.SCHEMA, null);
		target.put(McNodeType.PARTITIONTABLE, null);

		target.put(McNodeType.IMPORTABLE, null);
		target.put(McNodeType.EXPORTABLE, null);
		target.put(McNodeType.CONFIG, null);

		filterMap.put(McViewType.TARGET, target);

		return filterMap;
	}


	// ***********************************
 	public MartPointer getMartPointerCopy(MartPointer source, boolean rename) {
		String name = source.getName();
		Element mpElement = source.generateXml();
		MartPointer newMP = new MartPointer(mpElement);
		newMP.setParent(source.getParent());
		newMP.synchronizedFromXML();
		if (rename) {
			name = this.getUniqueMartPointerName(source.getGuiContainer()
					.getRootGuiContainer(), source.getName());
			newMP.setName(name);
		}
		return newMP;
	}

	public Config getConfigCopy(Config source, boolean rename) {
		String name = source.getName();
		Element configElement = source.generateXml();
		Config newConfig = new Config(configElement);
		newConfig.setParent(source.getParent());
		newConfig.synchronizedFromXML();
		if (rename) {
			name = this.getUniqueConfigName(source.getMart(), name);
			newConfig.setName(name);
		}
		return newConfig;
	}

	public List<Container> findContainerInOtherConfigs(Container c) {
		List<Container> clist = new ArrayList<Container>();
		Config sourceConfig = c.getParentConfig();
		for (Config config : sourceConfig.getMart().getConfigList()) {
			if (config.equals(sourceConfig))
				continue;
			Container c1 = config.getContainerByName(c.getName());
			if (c1 != null)
				clist.add(c1);
		}
		return clist;
	}
	
	public List<Attribute> findAttributeInOtherConfigs(Attribute source) {
		List<Attribute> alist = new ArrayList<Attribute>();
		
		if(source.getParentConfig().isMasterConfig()){
			for( Config config :source.getParentConfig().getMart().getConfigList()){
				Attribute a = config.getAttributeByName(source.getName(), null);
				if(a != null){
					alist.add(a);
				}
			}
		}
		
		return alist;
	}

	public List<Filter> findFilterInOtherConfigs(Filter source) {
		List<Filter> flist = new ArrayList<Filter>();		
		Config sourceConfig = source.getParentConfig();
		if(sourceConfig.isMasterConfig()){
			for (Config config : sourceConfig.getMart().getConfigList()) {
				if (config.equals(sourceConfig))
					continue;
				Filter f1 = config.getFilterByName(null, source.getName(), true);
				if (f1 != null)
					flist.add(f1);
			}
		}
		return flist;
	}


	public enum ErrorMsg {
		YES,NO_MORE_WARNING,CANCEL,NO_CONFIG,NO_MASTER_CONFIG,DEFAULT,INVALID_QUERY_RESTRICTION,
		TABLE_NAME_ERROR, TABLE_IS_NULL, COLUMN_NAME_ERROR , NULL_MART
	}
	public ErrorMsg needSynchronized(MartConfiguratorObject mcObj) {
		//if mart config object is null
		if(mcObj == null)
			return ErrorMsg.NULL_MART; 
		// is it in master config
		Config config = mcObj.getParentConfig();
		if (config == null)
			return ErrorMsg.NO_CONFIG;
		/*
		 * int n = JOptionPane.showConfirmDialog( null,
		 * "Would you like to synchronize other configs?", "Question",
		 * JOptionPane.YES_NO_CANCEL_OPTION);
		 */
		if (config.isMasterConfig()) {
			String isSync = Settings.getProperty("syncchange");
			
			if ("1".equals(isSync)) {
				JCheckBox synccb = new JCheckBox("do not warn again");
				synccb.setSelected("0".equals(Settings.getProperty("syncchange")));
				synccb.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						boolean isSelected = ((JCheckBox)e.getSource()).isSelected();
						Settings.setProperty("syncchange", isSelected?"0":"1");	
					}			
				});
				String message = "Changes to the source will affect all derived configurations. Proceed?"; 
				Object[] params = {message,synccb};
				
				
				int n = JOptionPane
						.showConfirmDialog(
								null,params,
								"Warning",  JOptionPane.YES_NO_OPTION);
				switch (n) {
					case 0:
						return ErrorMsg.YES;
					/*case 1:
						Settings.setProperty("syncchange", "0");						
						return ErrorMsg.NO_MORE_WARNING;*/
					case 2:
						return ErrorMsg.CANCEL;
					default:
						return ErrorMsg.DEFAULT;	
				}
			}else
				return ErrorMsg.NO_MORE_WARNING;
		}else
			return ErrorMsg.NO_MASTER_CONFIG;

	}

	public boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.indexOf("win") >= 0;
	}

	public List<MartConfiguratorObject> getPartitionReferences(
			PartitionTable ptable, int col) {
		List<MartConfiguratorObject> result = new ArrayList<MartConfiguratorObject>();
		// get mart
		Mart mart = ptable.getMart();

		return result;
	}

	public String getNextUniqueContainerName(Config config, String baseName) {
		String tmpName = baseName;
		int i=1;
		while(config.getContainerByName(tmpName)!=null) {
			tmpName = baseName + "_"+i++;
		}
		return tmpName;
	}
	
	public <T, E> T getKeysByValue(Map<T, E> map, E value) {
	     for (Map.Entry<T, E> entry : map.entrySet()) {
	         if (entry.getValue().equals(value)) {
	             return entry.getKey();
	         }
	     }
	     return null;
	}	
	
	public String getDatasetNameInMart(Mart mart, String commonDsName) {
		PartitionTable pt = mart.getSchemaPartitionTable();
		for(int i=0; i<pt.getTotalRows(); i++) {
			
		}
		return "";
	}
	
	public File getIndicesDirectory() {
		//check ../registry/linkindices exist
		File file = new File("../registry/linkindices");
		if(file.exists())
			return file;
		else
			return null;
	}
	
	public File getDistIndicesDirectory() {		
		final File registryDir = new File(".", "registry");
		if(!registryDir.exists())
			registryDir.mkdir();
		File linkindicesDir = new File(registryDir,"linkindices");
		if(!linkindicesDir.exists())
			linkindicesDir.mkdir();
		return linkindicesDir;		
	}

	/**
	 * assume that no dataset name conflict, only return the first mart found
	 * @param datasetName
	 * @return
	 */
	public Mart getMartFromDataset(String datasetName) {
		for(Mart mart: this.registryObject.getMartList()) {
			for(org.biomart.objects.objects.Dataset ds: mart.getDatasetList()) {
				if(ds.getName().equals(datasetName)) {
					return mart;
				}
			}
		}
		return null;
	}

	public boolean hasIndexInDataset(Mart mart, String dataset) {	

		File dir = null;
		try {
			dir = new File(McGuiUtils.INSTANCE.getDistIndicesDirectory().getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		String[] children = dir.list(); 
		if (children == null) { 
			// Either dir does not exist or is not a directory 
		} else { 
			for (String fileName: children) {
				if(fileName.indexOf(mart.getName()+"_"+dataset+"__") == 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	/*
	 * FIXME will merge with getDatasetsFromURL later
	 */
	public List<DatasetFromUrl> getDatasetsFromUrlForMart(MartInVirtualSchema virtualMart) throws IOException {
		List<DatasetFromUrl> dss = new ArrayList<DatasetFromUrl>();
		String dsUrl = "";
		//replace all space with +
		String virtualSchema = virtualMart.getServerVirtualSchema().replaceAll(" ", "+");
		//replace all space with %20
		String martname = virtualMart.getName().trim().replaceAll(" ", "%20");
		if(virtualMart.getHost().indexOf("http")==0)
			dsUrl = virtualMart.getHost()+":"+virtualMart.getPort()+virtualMart.getPath()+"?type=datasets&mart="+
				martname+"&virtualSchema="+virtualSchema;
		else
			dsUrl = "http://"+virtualMart.getHost()+":"+virtualMart.getPort()+virtualMart.getPath()+"?type=datasets&mart="+
				martname+"&virtualSchema="+virtualSchema;
		URL getDsUrl = null;
		try {
			getDsUrl = new URL(dsUrl);
		} catch (MalformedURLException e) {
			Log.error("connection error "+dsUrl);
			return dss;
		}
		StringBuffer dsUrlContent = null;
		try {
			dsUrlContent = MyUtils.copyUrlContentToStringBuffer(getDsUrl);
		} catch (IOException e) {
			throw e;
		}
		StringTokenizer martStringTokenizer = new StringTokenizer(dsUrlContent.toString(), MyUtils.LINE_SEPARATOR);
		while (martStringTokenizer.hasMoreTokens()) {
			String line = martStringTokenizer.nextToken();
			String[] values = line.split(MyUtils.TAB_SEPARATOR);
			if(values!=null && values.length>=4) {
				DatasetFromUrl ds = new DatasetFromUrl();
				if(!"TableSet".equals(values[0]))
					ds.setSequence(true);
				ds.setName(values[1]);
				ds.setDisplayName(values[2]);
				ds.setVisible("1".equals(values[3]) ? true: false);
				String hostStr;
				if(virtualMart.getHost().indexOf("http")==0)
					hostStr = virtualMart.getHost();
				else
					hostStr = "http://"+virtualMart.getHost();
				String registryUrl = hostStr+":"+virtualMart.getPort()+virtualMart.getPath()
					+"?type=configuration&dataset="+values[1]+"&virtualschema="+virtualMart.getServerVirtualSchema();
				ds.setUrl(registryUrl);
				ds.setVirtualSchema(virtualMart.getServerVirtualSchema());
				dss.add(ds);
			}
		}

		return dss;
	}

	public String getNextGuiContainerName(String baseName) {
		if(registryObject == null || registryObject.getPortal() == null)
			return baseName;
		GuiContainer root = this.registryObject.getPortal().getRootGuiContainer();
		String tmpName = baseName;
		int i=1;
		while(root.getGCByNameRecursively(tmpName)!=null) {
			tmpName = baseName + "_"+i++;
		}
		return tmpName;

	}

	/**
	 * most mc property has ',' separated string, we need to replace one part of the string
	 * for example, listStr = 'a,b,c,d'  oldvalue = 'a'  newvalue = 'x'
	 * return x,b,c,d

	 */
	public String replaceValueInListStr(String listStr, String oldvalue, String newvalue) {		
		String[] items = listStr.split(",");
		List<String> list = Arrays.asList(items);
		int index = list.indexOf(oldvalue);
		list.set(index, newvalue);
		return McUtils.StrListToStr(list, ",");
	}
	
	public boolean inProperty(String listStr, String value) {
		if(listStr.indexOf(value)>=0) {
			String[] items = listStr.split(",");
			List<String> list = Arrays.asList(items);
			return list.contains(value);			
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public List<DatasetFromUrl> getDatasetsFromURL(String url, String dsName){
		List<DatasetFromUrl> dsFromUrlList = new ArrayList<DatasetFromUrl>();
		String martUrl = url+"?type=registry";
		URL registryURL = null;
		try {
			registryURL = new URL(martUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return dsFromUrlList;
		}
		SAXBuilder builder = new SAXBuilder();
		Document registryDocument = null;
		try {
			registryDocument = builder.build(registryURL);
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(registryDocument==null) {
			return dsFromUrlList;
		}
		Element rootElement = registryDocument.getRootElement();
		List<Element> virtualSchemaList = rootElement.getChildren();
		List<String> martNameList = new ArrayList<String>();
		List<MartInVirtualSchema> martList = new ArrayList<MartInVirtualSchema>();
		for (Element virtualSchema : virtualSchemaList) {
			MartInVirtualSchema mart = new MartInVirtualSchema.URLBuilder().database(virtualSchema.getAttributeValue("database"))
			.defaultValue(virtualSchema.getAttributeValue("default"))
			.displayName(virtualSchema.getAttributeValue("displayName"))
			.host(virtualSchema.getAttributeValue("host"))
			.includeDatasets(virtualSchema.getAttributeValue("includeDatasets"))
			.martUser(virtualSchema.getAttributeValue("martUser"))
			.name(virtualSchema.getAttributeValue("name"))
			.path(virtualSchema.getAttributeValue("path"))
			.port(virtualSchema.getAttributeValue("port"))
			.serverVirtualSchema(virtualSchema.getAttributeValue("serverVirtualSchema"))
			.visible(virtualSchema.getAttributeValue("visible"))
			.build();
			martList.add(mart);	
			martNameList.add(virtualSchema.getAttributeValue("name"));
		}
		for(String mName: martNameList) {
			dsFromUrlList.addAll(this.getDataSets(mName, martList, dsName));
		}
		return dsFromUrlList;
	}

	private List<DatasetFromUrl> getDataSets(String martName, List<MartInVirtualSchema> martList, String datasetName) {
		List<DatasetFromUrl> dsList = new ArrayList<DatasetFromUrl>();
		MartInVirtualSchema mart = null;
		for(MartInVirtualSchema vs: martList) {
			if(vs.getName().equals(martName))
				mart = vs;
		}
		if(mart == null) 
			return dsList;
		String dsUrl = "http://"+mart.getHost()+":"+mart.getPort()+mart.getPath()+"?type=datasets&mart="+martName+"&virtualSchema="+mart.getServerVirtualSchema();
		URL getDsUrl = null;
		try {
			getDsUrl = new URL(dsUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StringBuffer dsUrlContent = null;
		try {
			dsUrlContent = MyUtils.copyUrlContentToStringBuffer(getDsUrl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(dsUrlContent == null)
			return dsList;
		StringTokenizer martStringTokenizer = new StringTokenizer(dsUrlContent.toString(), MyUtils.LINE_SEPARATOR);
		while (martStringTokenizer.hasMoreTokens()) {
			String line = martStringTokenizer.nextToken();
			String[] values = line.split(MyUtils.TAB_SEPARATOR);
			if(values!=null && values.length>=4) {
				if(values[1].indexOf(datasetName)>=0) {
					DatasetFromUrl ds = new DatasetFromUrl();
					ds.setName(values[1]);
					ds.setDisplayName(values[2]);
					ds.setVisible("1".equals(values[3]) ? true: false);
					String registryUrl = "http://"+mart.getHost()+":"+mart.getPort()+mart.getPath()
					+"?type=configuration&dataset="+values[1]+"&virtualschema="+mart.getServerVirtualSchema();
					ds.setUrl(registryUrl);
					ds.setVirtualSchema(mart.getServerVirtualSchema());
					dsList.add(ds);
				}
			}
		}
		return dsList;
	}

	public String getMcXMLVersion(Document document) throws MartBuilderException {
		String version = document.getRootElement().getAttributeValue(XMLElements.VERSION.toString());
		if(Resources.BIOMART_VERSION.equals(version))
			return version;
		//if no version attribute
		Element root = document.getRootElement();
		if(!root.getName().equalsIgnoreCase(XMLElements.MARTREGISTRY.toString())) {
			throw new MartBuilderException("invalide xml");
		}
		if(root.getChild(XMLElements.PORTAL.toString())==null)
			return "0.7";
		else
			return "0.8";
	}

	/**
	 * use bubble sort to sort the partition table
	 * @param table
	 * @param col
	 * @param ascend
	 */
	public void sortPartitionTable(PartitionTable table, int col, boolean ascend) {
		int count = table.getTotalRows();
		for(int i=0; i<count; i++) {
			for(int j=i+1; j<count; j++) {
				String s0 = table.getValue(i, col);
				String s1 = table.getValue(j, col);
				if(ascend) {
					if(s0.compareTo(s1)>0) {
						table.swapRows(i, j);
					}
				} else {
					if(s0.compareTo(s1)<0) {
						table.swapRows(i, j);
					}
				}
			}
		}
	}
	
	public List<Attribute> getAttributesFromLinkOutUrl(String linkOutURL, Config config) {
		List<Attribute> attributes = new ArrayList<Attribute>();
		Config masterConfig = config.getMart().getMasterConfig();
		String[] parts = linkOutURL.split("%");
		for(String name : parts){			
			Attribute att = masterConfig.getAttributeByName(name, null);
			if(att != null && name != "s"){
				attributes.add(att);
			}
		}
		return attributes;
	}

	public DatasetTable getMainTableByKeyName(Mart mart, String keyName) {
		List<DatasetTable> tableList = mart.getOrderedMainTableList();
		for(DatasetTable dsTable: tableList) {
			PrimaryKey pk = dsTable.getPrimaryKey();
			if(pk!=null && (keyName+"_pk").equalsIgnoreCase((pk.getName())))
				return dsTable;
		}
		return null;
	}
	
	public void orderMartPointerByGroup(GuiContainer gc, List<String> group) {
		Map<String,List<MartPointer>> groupMap = new HashMap<String,List<MartPointer>>();
		for(String g: group) {
			List<MartPointer> mpList = new ArrayList<MartPointer>();
			groupMap.put(g, mpList);
		}
		
		List<MartPointer> oldMPList = gc.getMartPointerList();
		List<MartPointer> newMPList = new ArrayList<MartPointer>();
		for(MartPointer mp: oldMPList) {
			String gstr = mp.getPropertyValue(XMLElements.GROUP);
			if(McUtils.isStringEmpty(gstr)) 
				newMPList.add(mp);
			else {
				groupMap.get(gstr).add(mp);
			}
		}
		oldMPList.clear();
		for(MartPointer mp: newMPList) {
			gc.addMartPointer(mp);
		}
		for(String g: group) {
			for(MartPointer mp: groupMap.get(g)) {
				gc.addMartPointer(mp);
			}
		}
	}
}