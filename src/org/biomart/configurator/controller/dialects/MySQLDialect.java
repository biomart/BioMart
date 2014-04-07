package org.biomart.configurator.controller.dialects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.configurator.model.MartConstructorAction;
import org.biomart.configurator.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.configurator.utils.ConnectionPool;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.JdbcType;

/**
 * Mart can be an any database (unlike for other rdbms)
 */
class MySQLDialect extends DatabaseDialect {
	public Map<String,List<String>> getMartTablesInfo(JdbcLinkObject conObj, String dsName) throws MartBuilderException {
		Map<String, List<String>> tblColMap = new TreeMap<String, List<String>>(McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
		List<String> colList = new ArrayList<String>();
		StringBuffer sb = new StringBuffer("select table_name,column_name,column_key " +
				"from information_schema.columns where table_schema='"+conObj.getDatabaseName()+"' and " +
						"table_name like '"+dsName+"%' order by " +
				"table_name, ordinal_position");
		String lastTableName = "";			
		List<Map<String,String>> resultSet = ConnectionPool.Instance.query(conObj, sb.toString());
				
		for(Map<String,String> mapItem: resultSet) {
			String tableName = (String)mapItem.get("table_name");
			if(!(tableName.endsWith("__main") || tableName.endsWith("__dm"))) {
				continue;
			}
			//finish all columns in one table and move to the next, if previous table doesn't have a PK, 
			//create using keyguessing
			if(!lastTableName.equals(tableName)) {
				if(!lastTableName.equals("")) {
					tblColMap.put(lastTableName, colList);
					colList = new ArrayList<String>();
				}
				//move to next table					
				//clean flags
				lastTableName = tableName;
			}				
			colList.add((String)mapItem.get("column_name"));
		}
		

		// sort column list
		Collections.sort(colList, McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
			

		if(!McUtils.isStringEmpty(lastTableName))
			tblColMap.put(lastTableName, colList);

		return tblColMap;
	}
	
	@Override
	public Set<String> getPartitionedTables(JdbcLinkObject conObj, String partitionBase) throws MartBuilderException {
		Set<String> tables = new TreeSet<String>(McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
		StringBuffer sb = new StringBuffer("select table_name " +
				"from information_schema.columns where table_schema='"+conObj.getDatabaseName()+"' and " +
						"table_name like '"+partitionBase+"%'");	
		
		List<Map<String,String>> resultSet = ConnectionPool.Instance.query(conObj, sb.toString());
				
		for(Map<String,String> mapItem: resultSet) {
			String tableName = (String)mapItem.get("table_name");			
			tables.add(tableName);
		}

		return tables;
	}
	

	public void setMetaInfo(JdbcLinkObject conObj,String dbName,String schemaName, Map<String,List<String>> colMap,
			Map<String,List<String>> pkMap) throws MartBuilderException {
		List<String> colList = new ArrayList<String>();
		List<String> pkList = new ArrayList<String>();
		StringBuffer sb = new StringBuffer("select table_schema,table_name,column_name,column_key from information_schema.columns where ");
		/*if(this.isSchemaPartitioned()) {
			for(Iterator<String> i = this.selectedTablesMap.keySet().iterator();i.hasNext();) {
				sb.append("table_schema='"+i.next()+"' " );
				if(i.hasNext())
					sb.append(" or ");
			}
		} else*/
			sb.append(" table_schema='"+schemaName+"' ");
		sb.append("order by table_schema, table_name, ordinal_position");

		String lastTableName = "";		
		String lastSchema = "";
		List<Map<String,String>> resultSet = ConnectionPool.Instance.query(conObj, sb.toString());

		for(Map<String,String> mapItem: resultSet) {
			String tableName = (String)mapItem.get("table_name");
			String schemaStr = (String)mapItem.get("table_schema");
			//finish all columns in one table and move to the next, if previous table doesn't have a PK, 
			//create using keyguessing
			if(!(lastTableName.equals(tableName) && lastSchema.equals(schemaStr))) {
				if(!lastTableName.equals("")) {
					colMap.put(lastSchema+"."+lastTableName, colList);
					pkMap.put(lastTableName, pkList);
					//no fk for MyISAM;
					colList = new ArrayList<String>();
					pkList = new ArrayList<String>();
				}
				//this.createPKforTable(currentTable, pkCols);
				//move to next table
				
				//clean flags
				lastTableName = tableName;
				lastSchema = schemaStr;
			}
			
			colList.add((String)mapItem.get("column_name"));

			//PK?
			String priStr = (String)mapItem.get("column_key");
			//PRI is the value return from MySQL
			if("PRI".equals(priStr)) {
				pkList.add((String)mapItem.get("column_name"));
			}

		}

		colMap.put(lastSchema+"."+lastTableName, colList);
		pkMap.put(lastTableName, pkList);		
	}

	public static final String MAX_ROWS = "MAX_ROWS";
	public static final String GROUP_CONCAT = "GROUP_CONCAT";
	public static final String SEPARATOR = "SEPARATOR";
	public MySQLDialect() {
		super(JdbcType.MySQL);
	}
	protected void checkValidMartBuilderCatalog(MartBuilderCatalog martBuilderCatalog) throws MartBuilderException {}	// no restriction
	@Override
	protected String quote(String value) {	// overriden by mysql and mssql
		return value!=null ? MYSQL_QUOTE + value + MYSQL_QUOTE : null;
	}
	protected final String buildGroupConcat(final UpdateOptimiser action) {
		return buildGroupConcat(action, null);
	}
	@Override
	protected final String buildGroupConcat(final UpdateOptimiser action, final String fromTable) {
		return GROUP_CONCAT + "(" + ALIAS2 + "." + action.getValueColumnName() + " " + SEPARATOR + " " + "'" + action.getValueColumnSeparator() + "'" + ")";
	}
	@Override
	protected String getSourceTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699
		return quote(action.getMartBuilderCatalog().getSourceDatabaseName()) + "." + quote(tableName);	// because in mysql schema<=>database
	}
	@Override
	protected String getTargetTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699
		return quote(action.getMartBuilderCatalog().getTargetDatabaseName()) + "." + quote(tableName);	// because in mysql schema<=>database
	}
	@Override
	protected final String addRenameStatement(final String schemaName, final String oldTableName, final String newTableName) {
		return RENAME + " " + TABLE + " " + quote(schemaName) + "." + quote(oldTableName) + " " + TO + " " + quote(schemaName) + "." + quote(newTableName);
	}
	@Override
	protected final String addBigTableHandling(final int bigTable) {
		return bigTable > 0 ? " " + MAX_ROWS + "=" + bigTable : "";
	}

	/**
	 * hardcoded for icgc update command line
	 * @param conObj
	 * @return
	 * @throws MartBuilderException
	 */
	public List<String> getMainTables(JdbcLinkObject conObj) throws MartBuilderException {
		StringBuffer sb = new StringBuffer("select table_name,column_name,column_key " +
				"from information_schema.columns where table_schema='"+conObj.getSchemaName()+"' and " +
						"table_name like '%__main' order by table_name");
		List<Map<String,String>> resultSet = ConnectionPool.Instance.query(conObj, sb.toString());
		Set<String> tmp = new HashSet<String>();
		for(Map<String,String> map: resultSet) {
			String mainTableName = (String)map.get("table_name");
			String tmpMartName = mainTableName.split("__")[0];			
			tmp.add(tmpMartName);
		}
		List<String> result = new ArrayList<String>(tmp);
		return result;
	}
}