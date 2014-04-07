package org.biomart.configurator.controller.dialects;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.configurator.utils.ConnectionPool;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.treelist.LeafCheckBoxNode;
import org.biomart.configurator.utils.type.JdbcType;

public class McSQL {

	public Map<String,List<String>> getMartTablesInfo(JdbcLinkObject conObj, String dsName) throws MartBuilderException {
		if(conObj.getJdbcType() == JdbcType.MySQL && conObj.isKeyGuessing())
			return ((MySQLDialect)DialectFactory.getDialect(conObj.getJdbcType())).getMartTablesInfo(conObj, dsName);
		else 
		{
			Map<String, List<String>> tblColMap = new TreeMap<String, List<String>>(McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
			Connection con = ConnectionPool.Instance.getConnection(conObj);
			if (null==con) {
				String message = "connection is null: " + conObj.toString() + ", " + conObj.toString();
				Log.error(message);
				new Throwable().printStackTrace();
				JOptionPane.showMessageDialog(null, message);
			}
			try {
				ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), conObj.getSchemaName(), dsName + "__" + "%", 
						"%"); 
				while(rs.next()) {
					String tableName = rs.getString("TABLE_NAME").toLowerCase();	
					if (null!=tableName && tableName.contains("$")) {	// exclude ORACLE's temporary tables
						continue;
					}
					if(!(tableName.endsWith("__main") || tableName.endsWith("__dm"))) {
						continue;
					}
					List<String> colList = tblColMap.get(tableName);
					if (null==colList) {
						colList = new ArrayList<String>();
						tblColMap.put(tableName, colList);
					}
					String columnName = rs.getString("COLUMN_NAME").toLowerCase();
					if (!colList.contains(columnName)) {
						colList.add(columnName);
					}
				}
				rs.close();

				// sort column lists
				for (List<String> colList : tblColMap.values()) {
					Collections.sort(colList, McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new MartBuilderException("database error", e);
			}	
			ConnectionPool.Instance.releaseConnection(conObj);
			return tblColMap;
		}
	}
	
	public Set<String> getPartitionedTables(JdbcLinkObject conObj, String partitionBase) throws MartBuilderException {
		if(conObj.getJdbcType() == JdbcType.MySQL)
			return ((MySQLDialect)DialectFactory.getDialect(conObj.getJdbcType())).getPartitionedTables(conObj, partitionBase);
		else
			return new TreeSet<String>(McUtils.BETTER_STRING_COMPARATOR);		// see DCCTEST-491
	}

	public List<LeafCheckBoxNode> getTablesNodeFromTarget(JdbcLinkObject conObject, String schemaName, boolean isSelected) {		
		Set<LeafCheckBoxNode> tables = new TreeSet<LeafCheckBoxNode>(new Comparator<LeafCheckBoxNode>() {
		      public int compare(LeafCheckBoxNode a, LeafCheckBoxNode b) {
		          LeafCheckBoxNode itemA = (LeafCheckBoxNode) a;
		          LeafCheckBoxNode itemB = (LeafCheckBoxNode) b;
		          return itemA.getText().compareTo(itemB.getText());
		        }
		      });

		JdbcLinkObject dbConObj = new JdbcLinkObject(conObject.getConnectionBase(),
				conObject.getDatabaseName(),schemaName,conObject.getUserName(),conObject.getPassword(),
				conObject.getJdbcType(), conObject.getPartitionRegex(),conObject.getPtNameExpression(),
				conObject.isKeyGuessing());
		
		Connection con = ConnectionPool.Instance.getConnection(dbConObj);
		if (null==con) {
			String message = "connection is null: " + conObject.toString() + ", " + dbConObj.toString();
			Log.error(message);
			new Throwable().printStackTrace();
			JOptionPane.showMessageDialog(null, message);
		}
		try {
			String catalog = con.getCatalog();
			if(conObject.getJdbcType() == JdbcType.MySQL)
				catalog = schemaName;
			ResultSet rs2 = con.getMetaData().getTables(catalog, schemaName, "%", new String[]{"TABLE"}); // "SELECT TABNAME AS TABLE_NAME, COLNAME AS COLUMN_NAME FROM SYSCAT.COLUMNS WHERE TABSCHEMA='" + schemaName + "'";
			while (rs2.next()) {
				String tableName = rs2.getString("TABLE_NAME");
				if((tableName.endsWith("__main") || tableName.endsWith("__MAIN")) 
						&& !tableName.contains("$")	// exclude ORACLE's temporary tables (although unlikely to happen here)
						) {
					LeafCheckBoxNode cbn = new LeafCheckBoxNode(tableName,isSelected);
					DatasetFromUrl dsUrl = new DatasetFromUrl();
					dsUrl.setName(tableName);
					dsUrl.setDisplayName(tableName);
					cbn.setUserObject(dsUrl);
					tables.add(cbn);
				}
			}	
			rs2.close();
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		ConnectionPool.Instance.releaseConnection(dbConObj);
		List<LeafCheckBoxNode> resList = new ArrayList<LeafCheckBoxNode>();
		for(LeafCheckBoxNode cbn: tables) 
			resList.add(cbn);
		return resList;
	}

	public List<LeafCheckBoxNode> getTablesNodeFromSource(JdbcLinkObject conObject, String schemaName, boolean isSelected) {
		Set<LeafCheckBoxNode> tables = new TreeSet<LeafCheckBoxNode>(new Comparator<LeafCheckBoxNode>() {
		      public int compare(LeafCheckBoxNode a, LeafCheckBoxNode b) {
		          LeafCheckBoxNode itemA = (LeafCheckBoxNode) a;
		          LeafCheckBoxNode itemB = (LeafCheckBoxNode) b;
		          return itemA.getText().compareTo(itemB.getText());
		        }
		      });

		Collection<String> tableNames = this.getAllTablesForDb(conObject, schemaName);
		for(String table: tableNames) {			
			LeafCheckBoxNode cbn = new LeafCheckBoxNode(table,isSelected);
			DatasetFromUrl dsUrl = new DatasetFromUrl();
			dsUrl.setName(table);
			dsUrl.setDisplayName(table);
			cbn.setUserObject(dsUrl);
			tables.add(cbn);
		}
		List<LeafCheckBoxNode> resList = new ArrayList<LeafCheckBoxNode>();
		for(LeafCheckBoxNode cbn: tables) 
			resList.add(cbn);
		return resList;
	}

	public Collection<String> getAllTablesForDb(JdbcLinkObject conObject, String schemaName) {
		Set<String> allTables = new HashSet<String>();
		Connection con = ConnectionPool.Instance.getConnection(conObject);
		try {
			String catalog = con.getCatalog();
			if(conObject.getJdbcType() == JdbcType.MySQL)
				catalog = schemaName;
			ResultSet rs2 = con.getMetaData().getTables(catalog, schemaName, "%", new String[]{"TABLE"}); //"SELECT TABNAME AS TABLE_NAME FROM SYSCAT.TABLES WHERE TABSCHEMA='" + schemaName + "'";

			while (rs2.next()) {
				String tableName = rs2.getString("TABLE_NAME");
				if (null!=tableName && tableName.contains("$")) {	// exclude ORACLE's temporary tables (although unlikely to happen here)
					continue;
				}
				allTables.add(tableName);
			}	
			rs2.close();
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		ConnectionPool.Instance.releaseConnection(conObject);
		
		return allTables;
	}
	
	public void setMetaInfo(JdbcLinkObject conObj,String dbName,String schemaName, Map<String,List<String>> colMap,
			Map<String,List<String>> pkMap) throws MartBuilderException {
		if("0".equals(Settings.getProperty("usejdbcformyisam")) && conObj.getJdbcType() == JdbcType.MySQL && conObj.isKeyGuessing()) {
			((MySQLDialect)DialectFactory.getDialect(conObj.getJdbcType())).setMetaInfo(conObj, dbName, schemaName, colMap, pkMap);
		}else {
			Connection con = ConnectionPool.Instance.getConnection(conObj);
			if (null==con) {
				String message = "connection is null: " + conObj.toString();
				Log.error(message);
				new Throwable().printStackTrace();
				JOptionPane.showMessageDialog(null, message);
			}
			Set<String> tableSet = new TreeSet<String>(McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
			ResultSet rs;
			try {
				rs = con.getMetaData().getTables(con.getCatalog(), schemaName, "%", new String[]{"TABLE"});
				while(rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					if (tableName!=null && tableName.contains("$")) {	// exclude ORACLE's temporary tables
						continue;
					}
					tableSet.add(tableName);
				}
				rs.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {				
				rs = con.getMetaData().getColumns(con.getCatalog(), schemaName, "%", null); // "SELECT TABNAME AS TABLE_NAME, COLNAME AS COLUMN_NAME " + "FROM SYSCAT.COLUMNS WHERE TABSCHEMA='" + schemaName + "'"
				while(rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					if(!tableSet.contains(tableName) ||
							 tableName.contains("$")) 	// exclude ORACLE's temporary tables
						continue;
					List<String> colList = colMap.get(tableName);
					if (null==colList) {
						colList = new ArrayList<String>();
						colMap.put(tableName, colList);
					}
					String columnName = rs.getString("COLUMN_NAME");
					if (!colList.contains(columnName)) {
						colList.add(columnName);
					}
				}
				rs.close();
	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//get all PK
			try {
				for(Map.Entry<String, List<String>> entry: colMap.entrySet()) {
					rs = con.getMetaData().getPrimaryKeys(con.getCatalog(), schemaName, entry.getKey()); 
					while(rs.next()) {
						String tableName = rs.getString("TABLE_NAME");
						if (null!=tableName && tableName.contains("$")) {
							continue;
						}
						List<String> pkList = pkMap.get(tableName);
						if(McUtils.isStringEmpty(tableName))
							continue;
						if (null==pkList) {
							pkList = new ArrayList<String>();
							pkMap.put(tableName, pkList);
						}
						String columnName = rs.getString("COLUMN_NAME");
						if (!pkList.contains(columnName)) {
							pkList.add(columnName);
						}
					}
					rs.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// sort column lists
		for (List<String> colList : colMap.values()) {
			Collections.sort(colList, McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
		}
		
		// sort pk lists
		for (List<String> pkList : pkMap.values()) {
			Collections.sort(pkList, McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
		}
	}

	public boolean hasOldConfigInTarget(JdbcLinkObject conObject, String schemaName) {
		return DialectFactory.getDialect(conObject.getJdbcType()).hasOldConfigInTarget(conObject, schemaName);
/*		if(conObject.getJdbcType() == JdbcType.MySQL)
			return ((MySQLDialect)DialectFactory.getDialect(conObject.getJdbcType())).hasOldConfigInTarget(conObject, schemaName);
		else if(conObject.getJdbcType() == JdbcType.PostGreSQL)
			return ((PgDialect)DialectFactory.getDialect(conObject.getJdbcType())).hasOldConfigInTarget(conObject, schemaName);
		else
			return false;*/
	}

	public List<String> getMainTableInfo(JdbcLinkObject conObj) throws MartBuilderException {
		return ((MySQLDialect)DialectFactory.getDialect(JdbcType.MySQL)).getMainTables(conObj);
	}

	public List<String> getTablesFromTarget(JdbcLinkObject conObject, String schemaName) {		
		List<String> result = new ArrayList<String>();
		JdbcLinkObject dbConObj = new JdbcLinkObject(conObject.getConnectionBase(),
				conObject.getDatabaseName(),schemaName,conObject.getUserName(),conObject.getPassword(),
				conObject.getJdbcType(), conObject.getPartitionRegex(),conObject.getPtNameExpression(),
				conObject.isKeyGuessing());
		
		Connection con = ConnectionPool.Instance.getConnection(dbConObj);
		if (null==con) {
			String message = "connection is null: " + conObject.toString() + ", " + dbConObj.toString();
			Log.error(message);
			new Throwable().printStackTrace();
			JOptionPane.showMessageDialog(null, message);
		}
		try {
			String catalog = con.getCatalog();
			if(conObject.getJdbcType() == JdbcType.MySQL)
				catalog = schemaName;
			ResultSet rs2 = con.getMetaData().getTables(catalog, schemaName, "%", new String[]{"TABLE"}); // "SELECT TABNAME AS TABLE_NAME, COLNAME AS COLUMN_NAME FROM SYSCAT.COLUMNS WHERE TABSCHEMA='" + schemaName + "'";
			while (rs2.next()) {
				String tableName = rs2.getString("TABLE_NAME");
				if((tableName.endsWith("__main") || tableName.endsWith("__MAIN")) 
						&& !tableName.contains("$")	// exclude ORACLE's temporary tables (although unlikely to happen here)
						) {
					result.add(tableName);
				}
			}	
			rs2.close();
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		ConnectionPool.Instance.releaseConnection(dbConObj);
		return result;
	}

}