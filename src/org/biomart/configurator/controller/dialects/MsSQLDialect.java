package org.biomart.configurator.controller.dialects;


import java.util.List;
import java.util.Set;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.configurator.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.treelist.LeafCheckBoxNode;
import org.biomart.configurator.utils.type.JdbcType;

/**
 * Must be mart within a schema (same or not) in same database, otherwise get message: 
 * 		"" TODO reproduce error message
 */
class MsSQLDialect extends DatabaseDialect {
	@Override
	public List<LeafCheckBoxNode> getMetaTablesFromOldConfig(
			JdbcLinkObject conObject, String schemaName, boolean isSelected) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getPartitionedTables(JdbcLinkObject conObj,
			String partitionBase) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static final String EXEC = "EXEC";
	public static final String SP_RENAME = "SP_RENAME";
	public static final String DATA_FUNCTION = "data()";	// must be lowercase
	public static final String REPLACE = "REPLACE";
	public static final String FOR = "FOR";
	public static final String XML = "XML";
	public static final String PATH = "PATH";
	public static final String GO = "GO";
	
	public MsSQLDialect() {
		super(JdbcType.MSSQL);
	}
	@Override
	protected String quote(String value) {	// overriden by mysql and mssql
		return value!=null ? MSSQL_OPEN_QUOTE + value + MSSQL_CLOSE_QUOTE : null;
	}
	@Override
	protected String getPrintCommand() {
		return MSSQL_PRINT_COMMAND;
	};
	@Override
	protected final String addRenameStatement(final String schemaName, final String oldTableName, final String newTableName) {
		return EXEC + " " + SP_RENAME + " " + 
			this.quote(schemaName + "." + oldTableName) +	// in this context, 'this.quote(schemaName) + "." + this.quote(oldTableName)' does not seem work  
			", " + this.quote(newTableName);
	}
	private static final String TEMPORARY_SEPARATOR = "-TEMPORARY_SEPARATOR_9847985-";	// unlikely to be part of the data
	protected void checkValidMartBuilderCatalog(MartBuilderCatalog martBuilderCatalog) throws MartBuilderException {}	// no restriction
	@Override
	protected String buildGroupConcat(final UpdateOptimiser action, final String fromTable) {	// significantly more complicated than mysql
		return 
			REPLACE + "(" +
				REPLACE + "(" +
					"(" +
						SELECT + " " +
							REPLACE + "(" +
								ALIAS2 + "." + action.getValueColumnName() + "," + " " +
								"'" + " " + "'" + "," + " " +
								"'" + TEMPORARY_SEPARATOR + "'" +	// replace any space within values by a temporary separator (very unlikely to appear in data)
							")" + " " +
						AS + " " + MSSQL_OPEN_QUOTE + DATA_FUNCTION + MSSQL_CLOSE_QUOTE + " " +
						FROM + " " + ALIAS2 + " " +
						FOR + " " + XML + " " + PATH + "(" + "'" + "'" + ")" +
					")" + "," + " " + 
					"'" + " " + "'" + "," + " " + 
					"'" + action.getValueColumnSeparator() + "'" +	// replace spaces (!= from spaces within values) from xml by the wanted separator
				")" + "," + " " + 
				"'" + TEMPORARY_SEPARATOR + "'" + "," + " " + // restore original spaces within values
				"'" + " " + "'" +
			")";
	}
	@Override
	protected final String addComputedTablePrefix(final String newTableName, final int bigTable) {
		return "";
	}
	@Override
	protected final String addComputedTableSuffix(final String newTableFullName) {
		return " " + INTO + " " + newTableFullName;
	}
	@Override
	protected final String addTableAliasCreationClause(final String optTableFullName, final String tableAliasName) {
		return tableAliasName;
	}
	@Override
	protected final String addExtraUpdateFromClause(final String optTableFullName) {
		return " " + FROM + " " + optTableFullName + " " + ALIAS1; // optional "AS"
	}
}