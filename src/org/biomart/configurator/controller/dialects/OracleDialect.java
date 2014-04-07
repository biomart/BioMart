package org.biomart.configurator.controller.dialects;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.configurator.model.MartConstructorAction;
import org.biomart.configurator.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.configurator.utils.ConnectionPool;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.treelist.LeafCheckBoxNode;
import org.biomart.configurator.utils.type.JdbcType;

/**
 * Must be mart within a schema (same or not) in same database, otherwise get message: 
 * 		"ORA-00922: missing or invalid option" (meaning no cross-database operations allowed)
 */
class OracleDialect extends DatabaseDialect {
	@Override
	public List<LeafCheckBoxNode> getMetaTablesFromOldConfig(JdbcLinkObject conObject, String schemaName, boolean isSelected) {
		Set<LeafCheckBoxNode> tables = new TreeSet<LeafCheckBoxNode>(new Comparator<LeafCheckBoxNode>() {
		      public int compare(LeafCheckBoxNode a, LeafCheckBoxNode b) {
		    	  LeafCheckBoxNode itemA = (LeafCheckBoxNode) a;
		    	  LeafCheckBoxNode itemB = (LeafCheckBoxNode) b;
		          return itemA.getText().compareTo(itemB.getText());
		        }
		      });

		JdbcLinkObject dbConObj = new JdbcLinkObject(conObject.getConnectionBase(),
				conObject.getJdbcType().useSchema()?conObject.getDatabaseName():schemaName,
						schemaName,conObject.getUserName(),conObject.getPassword(),
				conObject.getJdbcType(), conObject.getPartitionRegex(),conObject.getPtNameExpression(),
				conObject.isKeyGuessing());
		
		Connection con = ConnectionPool.Instance.getConnection(dbConObj);
		try {
			String catalog = con.getCatalog();
			ResultSet rs2 = con.getMetaData().getTables(catalog, schemaName, "%", new String[]{"TABLE"}); // "SELECT TABNAME AS TABLE_NAME, COLNAME AS COLUMN_NAME FROM SYSCAT.COLUMNS WHERE TABSCHEMA='" + schemaName + "'";
			while (rs2.next()) {
				String tableName = rs2.getString("TABLE_NAME");
				if((tableName.indexOf("meta_")==0 || tableName.indexOf("META_")==0) 
						&& !tableName.contains("$")	// exclude ORACLE's temporary tables (although unlikely to happen here)
						) {
					
					LeafCheckBoxNode cbn = new LeafCheckBoxNode(rs2.getString("TABLE_NAME"),isSelected);
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

	@Override
	public Set<String> getPartitionedTables(JdbcLinkObject conObj,
			String partitionBase) {
		// TODO Auto-generated method stub
		return null;
	}

	public static final String PURGE = "PURGE";
	public OracleDialect() {
		super(JdbcType.Oracle);
	}
	@Override
	protected void checkValidTableName(String newTableName) throws MartBuilderException {
		if (newTableName.length()>ORACLE_MAX_IDENTIFIER_SIZE) {
			throw new MartBuilderException("Invalid table name: " + newTableName + ". " +
					"It exceeds Oracle's maximum table name length of " + ORACLE_MAX_IDENTIFIER_SIZE);
		}
	}
	@Override
	protected String buildGroupConcat(UpdateOptimiser action, String fromTable) throws Exception {
		throw new Exception("group_concat not implemented in MartConfigurator and for oracle");	//TODO
	}
	@Override
	protected String getSourceTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699
		return quote(action.getMartBuilderCatalog().getSourceSchemaName()) + "." + quote(tableName);	// because in oracle schema<~>username
	}
	@Override
	protected String getTargetTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699
		return quote(action.getMartBuilderCatalog().getTargetSchemaName()) + "." + quote(tableName);	// because in oracle schema<~>username
	}
	
	@Override
	protected String addIndexPrefix(final String targetSchemaName) {	
		return quote(targetSchemaName) + ".";
	}
	
	@Override
	protected String getTableAliasCreationPrefix() {
		return "";
	}
	@Override
	protected final String addRenameStatement(final String schemaName, final String oldTableName, final String newTableName) {
		return ALTER + " " + TABLE + " " + quote(schemaName) + "." + quote(oldTableName) + " " + RENAME + " " + TO + " " + quote(newTableName);
	}
	@Override
	public final String addDropTableSuffix() {
		return " " + PURGE;
	};
}