package org.biomart.configurator.test.category;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.biomart.api.Portal;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Dataset;
import org.biomart.api.lite.Mart;
import org.biomart.common.exceptions.ListenerException;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartConstructor;
import org.biomart.configurator.controller.MartConstructorListener;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.controller.SaveDDLMartConstructor;
import org.biomart.configurator.controller.MartConstructor.ConstructorRunnable;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.objects.portal.UserGroup;
import org.biomart.processors.ProcessorRegistry;
import org.biomart.processors.TSV;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.mortbay.log.Log;

public class TestFullMaterialize extends TestMartMaterialize {

	@Override
	public boolean test() {
		this.testNewPortal();		
		this.testAddMart(testName);		
		this.testSaveMaterializeSQL(testName);
		
		//materailize the SQL back to the database
		this.testMaterializeSQL(testName);
		//reload portal with the materialized database
		this.testNewPortal();		
		this.testAddMaterializeMart(testName);
		this.testSaveXML(testName);
		try {
			this.testQuery(testName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return this.compareQuery(testName);
	}
	
	public void testSaveMaterializeSQL(String testName) {
		// TODO Auto-generated method stub
		
			org.biomart.objects.objects.Mart mart = this.getMart();
			String sqlFile = SettingsForTest.getSavedSQLPath(testName);
			final StringBuffer sb = new StringBuffer();
			MartConstructor martConstructor = new SaveDDLMartConstructor(sb);
			Element element = SettingsForTest.getTestCase(testName);
			Element dbElement = element.getChild("connection").getChild("db");
			Element targetElement = element.getChild("mconnection").getChild("db");
			String targetDatabaseName = targetElement.getAttributeValue("database");
			String targetSchemaName = targetElement.getAttributeValue("schema");
			Collection<String> selectedPrefixes = new ArrayList<String>();
			try {
				final ConstructorRunnable cr = martConstructor.getConstructorRunnable(targetDatabaseName, targetSchemaName, mart, selectedPrefixes);
				
				cr.addMartConstructorListener(new MartConstructorListener() {
					public void martConstructorEventOccurred(final int event,
							final Object data, final org.biomart.configurator.model.MartConstructorAction action)
							throws ListenerException {

					}

				});
				cr.run();
				//after run save string buffer to file
				BufferedWriter out = new BufferedWriter(new FileWriter(sqlFile));
				out.write(sb.toString());
				out.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
	}

	public void testAddMaterializeMart(String testcase) {
		List<DataLinkInfo> dliList = SettingsForTest.getMaterializeDataLinkInfo(testcase);
		for(DataLinkInfo dli: dliList) {
			dli.setIncludePortal(true);
			UserGroup ug = SettingsForTest.getUserGroup(testcase);
			ObjectController oc = new ObjectController();
			try {
				oc.initMarts(dli, ug, XMLElements.DEFAULT.toString());
			} catch (MartBuilderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Connection getConnection() throws SQLException  
    {
		Element element = SettingsForTest.getTestCase(testName);
		Element conElement = element.getChild("mconnection");
		Element dbElement = conElement.getChild("db");
		String jdbcURL = dbElement.getAttributeValue("jdbcurl") + dbElement.getAttributeValue("database");
		String user = dbElement.getAttributeValue("username");
		String password = dbElement.getAttributeValue("password");
        
		return DriverManager.getConnection(jdbcURL, user, password);
	
    }
	
	public void testMaterializeSQL(String testcase){
		try {
			Connection con = this.getConnection();
			Statement st = con.createStatement();
			String sqlFile = SettingsForTest.getSavedSQLPath(testcase);
			FileReader fr = new FileReader(new File(sqlFile));  
            // be sure to not have line starting with "--" or "/*" or any other non aplhabetical character  
			String s;
			StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(fr);  
  
            while((s = br.readLine()) != null)  
            {  
                sb.append(s);  
            }  
            br.close();  
            
            String[] inst = sb.toString().split(";");
    
    		Element element = SettingsForTest.getTestCase(testName);
    		Element conElement = element.getChild("mconnection");
    		Element dbElement = conElement.getChild("db");
    		String dbType = dbElement.getAttributeValue("type");
            //drop database if exists
    		String schemaName = dbElement.getAttributeValue("schema");
    		String targetDatabaseName = dbElement.getAttributeValue("database");
    		
    		//TODO use enum...
			String ORACLE = "oracle";
			String DB2 = "DB2";
			String SQL_SERVER = "SQL Server";
			String POSTGRES = "PostGreSQL";
			String MYSQL = "mysql";
			
			boolean isMysql = dbType.equals(MYSQL);
			boolean isPostgres = dbType.equals(POSTGRES);
			boolean isOracle = dbType.equals(ORACLE);
			boolean isDB2 = dbType.equals(DB2);
			boolean isMssql = dbType.equals(SQL_SERVER);
			
			String dropSchemaQuery = null;
			if (isMysql) {
				dropSchemaQuery = "drop schema "+ schemaName;
    			st.executeUpdate(dropSchemaQuery);
			} else if(isPostgres) {
				dropSchemaQuery = "drop schema "+ schemaName + " CASCADE";
    			st.executeUpdate(dropSchemaQuery);
    		} /*else if (isDB2) {
    			dropSchemaQuery = "CALL SYSPROC.ADMIN_DROP_SCHEMA('" + schemaName + "', NULL, 'ERRORSCHEMA', 'ERRORTABLE')";
				st.executeUpdate(dropSchemaQuery);
    		} */ else {	// easier to drop all objects in schema for oracle and sql server than drop/recreate schema
    			String catalog = con.getCatalog();
				ResultSet tableResultSet = con.getMetaData().getTables(catalog, schemaName, "%", new String[] {"TABLE"});
				
				List<String> tableNameList = new ArrayList<String>();
				while (tableResultSet.next()) {
					String tableName = tableResultSet.getString("TABLE_NAME");
					tableNameList.add(tableName);
				}
				tableResultSet.close();
				
				// only need to drop tables, since no constraints in marts (indices are dropped along with tables)
				for (String tableName : tableNameList) {
					
					// TODO reuse same mechanism to fully qualify a table than martbuilder, instead of duplicate logic
					// ! careful that not prefixing table properly may result in deleting source tables from default schema !
					String fullyQualifiedTableName = null;
					if (isMssql) {
						fullyQualifiedTableName = "[" + targetDatabaseName + "]" + "." + "[" + schemaName + "]" + "." + "[" + tableName + "]";
					} else if (isOracle || isDB2) {	// these two share a lot
						fullyQualifiedTableName = "\"" + schemaName + "\"" + "." + "\"" + tableName + "\"";
					} else assert false;
					String dropTableQuery = "DROP TABLE " + fullyQualifiedTableName;
					st.executeUpdate(dropTableQuery);
				}
    		}
    		
            //create schema (database for mysql)
    		if (!isDB2 && !isOracle && !isMssql) {
    			String createSchemaQuery = "create schema "+ schemaName;
				st.executeUpdate(createSchemaQuery);
    		}
            //use database
            //st.execute("use "+ dbElement.getAttributeValue("database"));
            
            for(int i = 0; i<inst.length; i++)  
            {  
                // we ensure that there is no spaces before or after the request string  
                // in order to not execute empty statements  
                String sql = inst[i];
				if(!sql.trim().equals("") && 
						(!isMssql || !"GO".equalsIgnoreCase(sql)) &&	// JDBC does not support the GO instruction (used implicitely instead)
						(!isDB2 || !sql.toUpperCase().startsWith("ECHO"))	// JDBC does not support the ECHO instruction
					) {
                    st.execute(sql);
                    Log.debug(sql);
                }  
            }  
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void testQuery(String testcase) throws IOException, JDOMException{
		String xmlfile = SettingsForTest.getSavedXMLPath(testcase);
		MartRegistryFactory factory = new XmlMartRegistryFactory(xmlfile,null);
		Portal portal = new Portal(factory, null);
		ProcessorRegistry.register("TSV", TSV.class);
		Mart mart = portal.getMarts(null).get(0);
		Dataset ds = portal.getDatasets(mart.getName()).get(0);
		Attribute attr = portal.getAttributes(ds.getName(), null, null, null).get(0);
		Element element = SettingsForTest.getTestCase(testcase);
		Element queryElement = element.getChild("Query");
		XMLOutputter outputter = new XMLOutputter();
		String queryxml = outputter.outputString(queryElement);
		ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
		//query.getResults(outputstream);
		portal.executeQuery(queryxml, outputstream, false);
		outputstream.writeTo(new FileOutputStream(new File(SettingsForTest.getSavedQueryPath(testcase))));
	}
	
	public boolean compareQuery(String testcase){
		try {
			String file1 = SettingsForTest.getSourceQueryPath(testcase);
			String file2 = SettingsForTest.getSavedQueryPath(testcase);

			String strFile1 = FileUtils.readFileToString(new File(file1));
			String strFile2 = FileUtils.readFileToString(new File(file2));
			
			return strFile1.equals(strFile2);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
