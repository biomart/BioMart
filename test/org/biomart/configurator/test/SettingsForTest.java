package org.biomart.configurator.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.controller.dialects.McSQL;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.FileLinkObject;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.connection.URLConnection;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.JdbcType;
import org.biomart.objects.portal.UserGroup;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class SettingsForTest {
	private static Element rootElement;
	
	public static void loadConfigXML(String xml) {	
		File file = new File(xml);
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			Document configDoc = saxBuilder.build(file);
			rootElement = configDoc.getRootElement();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	
	public static Element getTestCase(String name) {
		@SuppressWarnings("unchecked")
		List<Element> testcases = rootElement.getChildren("testcase");
		for(Element e: testcases){
			if(e.getAttributeValue("name").equals(name))
				return e;
		}
		return null;
	}
	
	public static List<DataLinkInfo> getMaterializeDataLinkInfo(String testcase) {
		return getDataLinkInfo(testcase,true);
	}
	
	private static List<DataLinkInfo> getDataLinkInfo(String testcase, boolean materialized) {
		List<DataLinkInfo> dliList = new ArrayList<DataLinkInfo>();
		@SuppressWarnings("unchecked")
		List<Element> conElements = getTestCase(testcase).getChildren(materialized?"mconnection":"connection");
		for(Element conElement: conElements) {
			DataLinkType dlt = DataLinkType.getEnumFromValue(conElement.getAttributeValue("type"));
			DataLinkInfo dli = new DataLinkInfo(dlt);
			dli.setBCPartitioned(Boolean.parseBoolean(conElement.getAttributeValue("partitioned")));
			Map<MartInVirtualSchema,List<DatasetFromUrl>> allTablesMap = new LinkedHashMap<MartInVirtualSchema,List<DatasetFromUrl>>();
			Map<MartInVirtualSchema,List<DatasetFromUrl>> selectedTablesMap = new LinkedHashMap<MartInVirtualSchema,List<DatasetFromUrl>>();
			switch (dlt) {
			case SOURCE:
			case TARGET:
				Element dbElement = conElement.getChild("db");
				JdbcType type = JdbcType.valueByName(dbElement.getAttributeValue("type"));
				JdbcLinkObject jlo = new JdbcLinkObject(dbElement.getAttributeValue("jdbcurl"), 
						dbElement.getAttributeValue("database"), 
						dbElement.getAttributeValue("schema"), 
						dbElement.getAttributeValue("username"), 
						dbElement.getAttributeValue("password"), 
						type, "", "", "0".equals(dbElement.getAttributeValue("keyguessing"))?false:true);
				dli.setJdbcLinkObject(jlo);
				McSQL mcsql = new McSQL();
				Collection<String> allTables = mcsql.getAllTablesForDb(jlo, dbElement.getAttributeValue("schema"));
				List<DatasetFromUrl> allDatasets = new ArrayList<DatasetFromUrl>();
				for(String str: allTables) {
					DatasetFromUrl dsUrl = new DatasetFromUrl();
					dsUrl.setName(str);
					dsUrl.setDisplayName(str);
					allDatasets.add(dsUrl);
				}
				List<DatasetFromUrl> selectedTables = new ArrayList<DatasetFromUrl>();
				String[] tables = dbElement.getAttributeValue("tables").split(",");
				for(String table: tables) {
					DatasetFromUrl dsUrl = new DatasetFromUrl();
					dsUrl.setDisplayName(table);
					dsUrl.setName(table);
					selectedTables.add(dsUrl);
				}
	

				MartInVirtualSchema martV = new MartInVirtualSchema.DBBuilder().database(jlo.getDatabaseName()).schema(jlo.getSchemaName())
				.host(jlo.getHost()).port(jlo.getPort()).name(jlo.getSchemaName()).displayName(jlo.getSchemaName())
				.type(jlo.getJdbcType().toString()).username(jlo.getUserName()).password(jlo.getPassword())
				.build();

				allTablesMap.put(martV, new ArrayList<DatasetFromUrl>(allDatasets));
				selectedTablesMap.put(martV, selectedTables);
	
				
				//use oldconfig
				boolean b = false;
				if("true".equals(dbElement.getAttributeValue("naive")))
					b = true;
									
				martV.setUseBCForDB(!b);
				dli.getJdbcLinkObject().setDsInfoMap(selectedTablesMap);
				break;
			case URL:
				Map<MartInVirtualSchema,List<DatasetFromUrl>> urlInfo = new HashMap<MartInVirtualSchema,List<DatasetFromUrl>>();
				dbElement = conElement.getChild("url");
				UrlLinkObject conObject = new UrlLinkObject();
				String url = dbElement.getAttributeValue("host")+":"+dbElement.getAttributeValue("port")+dbElement.getAttributeValue("path");
				
				List<MartInVirtualSchema> virtualMarts = URLConnection.getInstance().getMartsFromURL(url);
				//get the right one from config xml
				List<Element> martElements = dbElement.getChildren("mart");
				for(Element martElement: martElements) {
					MartInVirtualSchema mart = null;
					String martName = martElement.getAttributeValue("name");
					for(MartInVirtualSchema virtualMart: virtualMarts) {
						if(virtualMart.getName().equals(martName)) {
							mart = virtualMart;
							break;
						}
					}
					if(mart!=null) {
						List<DatasetFromUrl> dss;
						try {
							dss = McGuiUtils.INSTANCE.getDatasetsFromUrlForMart(mart);
							urlInfo.put(mart,dss);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}				
				}
				
				conObject.setDsInfoMap(urlInfo);			
				String host = dbElement.getAttributeValue("host");
				int index = host.indexOf("://");
				host = host.substring(index+3);
			//	conObject.setBaseUrl(this.urlMeta.getBaseUrl());
				conObject.setHost(host);
				conObject.setFullHost(dbElement.getAttributeValue("host"));
				conObject.setPort(dbElement.getAttributeValue("port"));
				conObject.setPath(dbElement.getAttributeValue("path"));
	
				conObject.setGrouped(false/*this.groupCB.isSelected()*/);
				
				
				dli.setUrlLinkObject(conObject);
				break;
			case FILE:
				Element fileElement = conElement.getChild("file");
				Element directory = rootElement.getChild("datadirectory");
				String directoryString = directory.getAttributeValue("name");
				String fileName = directoryString+"/"+fileElement.getText();
				File file = new File(fileName);
				ObjectController oc = new ObjectController();
				List<MartInVirtualSchema> martList = null;
				Map<MartInVirtualSchema, List<DatasetFromUrl>> map = new LinkedHashMap<MartInVirtualSchema, List<DatasetFromUrl>>();
				try {
					martList = oc.getURLMartFromFile(file);
					for(MartInVirtualSchema mart: martList) {
						if(mart.isURLMart()) {
							List<DatasetFromUrl> dss = McGuiUtils.INSTANCE.getDatasetsFromUrlForMart(mart);
							//drop those not included
							for(Iterator<DatasetFromUrl> it = dss.iterator(); it.hasNext();) {
								if(!mart.getIncludeDatasets().contains(it.next().getName())) {
									it.remove();
								}
							}
							map.put(mart, dss);
						} else {
							map.put(mart, new ArrayList<DatasetFromUrl>());
						}
					}
				} catch (JDOMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				FileLinkObject linkObject = new FileLinkObject();
				linkObject.setDsInfoMap(map);
				dli.setFileLinkObject(linkObject);
				dli.setIncludePortal(true);
				dli.setSourceGrouped(false);
				dli.setBCPartitioned(false);
				break;
			}	
			//dli.setAllTables(allTablesMap);
			//dli.setSelectedTables(selectedTablesMap);
			dliList.add(dli);
		}
		return dliList;


	}
	
	public static List<DataLinkInfo> getDataLinkInfo(String testcase) {
		return getDataLinkInfo(testcase,false);
	}
	
	public static UserGroup getUserGroup(String testcase) {
		Element element = getTestCase(testcase).getChild("usergroup");
		UserGroup ug = new UserGroup(element.getText(),element.getText(),"");
		return ug;
	}
	
	public static String getAttribute(String testcase) {
		Element element = getTestCase(testcase).getChild("attribute");
		return element.getAttributeValue("name");
	}
	
	public static String getSavedXMLPath(String testcase) {
		Element element = getTestCase(testcase).getChild("sourcexml");
		String source = element.getText();
		int index = source.indexOf(".");
		String prefix = source.substring(0,index);
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");
		return directoryString+"/"+prefix+"_test.xml";
	}
	
	public static String getSavedSQLPath(String testcase) {
		Element element = getTestCase(testcase).getChild("sourcesql");
		String source = element.getText();
		int index = source.indexOf(".");
		String prefix = source.substring(0,index);
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");
		return directoryString+"/"+prefix+"_test.sql";
	}
	
	public static String getSavedXMLKeyPath(String testcase) {
		Element element = getTestCase(testcase).getChild("sourcexml");
		String source = element.getText();
		int index = source.indexOf(".");
		String prefix = source.substring(0,index);
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");
		return directoryString+"/."+prefix+"_test";
	}
	
	public static String getSourceXMLPath(String testcase) {
		Element element = getTestCase(testcase).getChild("sourcexml");
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");

		return directoryString+"/"+element.getText();
	}
	
	public static String getBaseXMLPath(String testcase) {
		Element element = getTestCase(testcase).getChild("basexml");
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");

		return directoryString+"/"+element.getText();
	}
	
	public static String getSourceSQLPath(String testcase) {
		Element element = getTestCase(testcase).getChild("sourcesql");
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");

		return directoryString+"/"+element.getText();
	}
	
	public static String getTestCaseXMLPath(String testcase){
		Element element = getTestCase(testcase).getChild("sourcexml");
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");
		String filename = directoryString+"/"+element.getText();		
		String path = filename.substring(0,filename.length()-4);
		path += "_testcase";
		return path;
	}
	
	public static String getSourceXMLKeyPath(String testcase) {
		Element element = getTestCase(testcase).getChild("sourcexml");
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");
		String filename = directoryString+"/."+element.getText();
		return filename.substring(0,filename.length()-4);
	}
	
	public static String getTestPath(){
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");
		return directoryString;
	}
	
	public static boolean isTestIngore(String testcase) {
		Element element = getTestCase(testcase);
		String s = element.getAttributeValue("ignore");
		return "true".equals(s)?true:false;
	}
	
	public static boolean isIgnoredTestPass() {
		Element element = rootElement.getChild("ignore");
		return Boolean.parseBoolean(element.getAttributeValue("pass"));
	}

	public static String getTestCategory(String name) {
		Element e = getTestCase(name);
		return e.getAttributeValue("category");
	}
	
	public static boolean isGenerateReferenceXML() {
		Element element = rootElement.getChild("referencexml");
		return Boolean.parseBoolean(element.getAttributeValue("regenerate"));
	}
	
	public static String getSourceQueryPath(String testcase){
		Element element = getTestCase(testcase).getChild("sourcequery");
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");

		return directoryString+"/"+element.getText();
	}
	
	public static String getSavedQueryPath(String testcase){
		Element element = getTestCase(testcase).getChild("sourcequery");
		String source = element.getText();
		int index = source.indexOf(".");
		String prefix = source.substring(0,index);
		Element directory = rootElement.getChild("datadirectory");
		String directoryString = directory.getAttributeValue("name");
		return directoryString+"/"+prefix+"_test.txt";
	}
}