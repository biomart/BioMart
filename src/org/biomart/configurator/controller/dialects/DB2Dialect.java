package org.biomart.configurator.controller.dialects;

import java.util.List;
import java.util.Set;

import org.biomart.configurator.model.MartConstructorAction;
import org.biomart.configurator.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.treelist.LeafCheckBoxNode;
import org.biomart.configurator.utils.type.JdbcType;

/**
 * Must be mart within a schema (same or not) in same database, otherwise get message: 
 * 		"Dataset uses schemas from multiple sources that cannot be linked to each other using SQL."
 */
class DB2Dialect extends DatabaseDialect {
		
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

	public static final String SUBSTR = "SUBSTR";
	public static final String XMLSERIALIZE = "XMLSERIALIZE";
	public static final String XMLAGG = "XMLAGG";
	public static final String XMLTEXT = "XMLTEXT";
	public static final String CONCAT = "CONCAT";
	public static final String WITH_NO_DATA = "WITH NO DATA";
	public DB2Dialect() {
		super(JdbcType.DB2);
	}
	@Override
	protected String getPrintCommand() {
		return DB2_PRINT_COMMAND;
	};
	@Override
	protected final String buildGroupConcat(UpdateOptimiser action, 
				String fromTable	// TODO why not used for DB2?
			) {	// significantly more complicated than mysql
		return
			SUBSTR + "(" +
				XMLSERIALIZE + "(" +
					XMLAGG + "(" +
						XMLTEXT + "(" +
							CONCAT + "(" + 
								action.getValueColumnSeparator() + "," + ALIAS2 + "." + action.getValueColumnName() + 
							")" + 
						")" +
					")" +
				" " + AS + " " + VARCHAR + "(" + "1024" + ")" + ")" +
			"," + (action.getValueColumnSeparator().length()+1) + ")";	// +1 because starts at 1, not 0
	}
	@Override
	protected String getSourceTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699
		MartBuilderCatalog martBuilderCatalog = action.getMartBuilderCatalog();
		return quote(martBuilderCatalog.getSourceSchemaName()) + "." + quote(tableName);
	}
	@Override
	protected String getTargetTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699
		MartBuilderCatalog martBuilderCatalog = action.getMartBuilderCatalog();
		return quote(martBuilderCatalog.getTargetSchemaName()) + "." + quote(tableName);
	}
		
	@Override
	protected String addIndexPrefix(final String targetSchemaName) {	
		return quote(targetSchemaName) + ".";
	}
	@Override
	protected final String addRenameStatement(final String schemaName, final String oldTableName, final String newTableName) {
		return RENAME + " " + quote(schemaName) + "." + quote(oldTableName) + " " + TO + " " + quote(newTableName);
	}
	@Override
	protected String addComputedTablePrefix(final String newTableName, final int bigTable) {
		return "";
	}
	@Override
	protected final void postProcessComputedTableStatement(final List<String> statements, final String newTableFullName) {
		String statement = statements.remove(0);
		statements.add(CREATE + " " + TABLE + " " + newTableFullName + " " + AS + " " + "(" + statement + ")" + " " + WITH_NO_DATA);
		statements.add(INSERT + " " + INTO + " " + newTableFullName + " " + "(" + statement + ")");
	}
}