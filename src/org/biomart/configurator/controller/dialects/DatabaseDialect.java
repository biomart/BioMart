package org.biomart.configurator.controller.dialects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.biomart.common.exceptions.ConstructorException;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.configurator.model.MartConstructorAction;
import org.biomart.configurator.model.MartConstructorAction.CopyOptimiser;
import org.biomart.configurator.model.MartConstructorAction.Drop;
import org.biomart.configurator.model.MartConstructorAction.Index;
import org.biomart.configurator.model.MartConstructorAction.Join;
import org.biomart.configurator.model.MartConstructorAction.LeftJoin;
import org.biomart.configurator.model.MartConstructorAction.Rename;
import org.biomart.configurator.model.MartConstructorAction.Select;
import org.biomart.configurator.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.configurator.utils.ConnectionPool;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.treelist.LeafCheckBoxNode;
import org.biomart.configurator.utils.type.JdbcType;

public abstract class DatabaseDialect {

	/**
	 * Keep SQL basic keywords as constants instead of hardcoded, in order to find references easily (especially in other classes)
	 * or change case if needed. Uppercase is prefered since it's the most widely used convention, besides QueryCompiler uses it as well.
	 */
	public static final String SELECT = "SELECT";
	public static final String CREATE = "CREATE";
	public static final String TABLE = "TABLE";
	public static final String AS = "AS";
	public static final String FROM = "FROM";
	public static final String INTO = "INTO";
	public static final String DROP = "DROP";
	public static final String ON = "ON";
	public static final String INDEX = "INDEX";
	public static final String LEFT = "LEFT";
	public static final String INNER = "INNER";
	public static final String JOIN = "JOIN";
	public static final String AND = "AND";
	public static final String NOT = "NOT";
	public static final String ALTER = "ALTER";
	public static final String ADD = "ADD";
	public static final String UPDATE = "UPDATE";
	public static final String RENAME = "RENAME";
	public static final String INSERT = "INSERT";
	public static final String VARCHAR = "VARCHAR";
	public static final String TO = "TO";
	public static final String SET = "SET";
	public static final String WHERE = "WHERE";
	public static final String INTEGER = "INTEGER DEFAULT 0";
	public static final String COUNT = "COUNT(1)";
	public static final String NULL = "NULL";
	public static final String IS_NULL = "IS" + " " + NULL;
	//TODO make QueryCompiler use this as well
	
	//TODO put in enum (JDBCType?)
	public final static String ANSI_QUOTE = "\"";
	public final static String MYSQL_QUOTE = "`";
	public final static String MSSQL_OPEN_QUOTE = "[";
	public final static String MSSQL_CLOSE_QUOTE = "]";
	
	public final static String MYSQL_PRINT_COMMAND = null; //TODO
	public final static String POSTGRES_PRINT_COMMAND = null; //TODO
	public final static String ORACLE_PRINT_COMMAND = null; //TODO
	public final static String MSSQL_PRINT_COMMAND = "PRINT";
	public final static String DB2_PRINT_COMMAND = "ECHO";
	
	public final static int ORACLE_MAX_IDENTIFIER_SIZE = 30;	// other rbdms are usually >64 so rarely a problem
	
	public final static String ALIAS1 = "A";	// if upper case already then no need to quote them or oracle and db2...
	public final static String ALIAS2 = "B";
	
	public final static String INDEX_NAME_PREFIX = "I_";
	private final static int DEBUG_MESSAGE_MAX_LENGTH = 32;
	
	private Integer commandCount = null; // [MC1010221734]
	protected JdbcType jdbcType;
	private Integer indexCount = null;
	private Boolean usingMartRunner = null;
	
	protected DatabaseDialect(JdbcType jdbcType) {
		this.jdbcType = jdbcType;
		this.indexCount = 0;
		this.commandCount = 0;
	}
	protected void checkValidMartBuilderCatalog(MartBuilderCatalog martBuilderCatalog) throws MartBuilderException {	// mysql and mssql override it (only ones allowing cross-platform operations easily)
		checkSameDatabases(martBuilderCatalog);
	}
	private final void checkSameDatabases(MartBuilderCatalog martBuilderCatalog) throws MartBuilderException {
		if (!martBuilderCatalog.getSourceDatabaseName().equals(martBuilderCatalog.getTargetDatabaseName())) {
			throw new MartBuilderException("Rdbms " + this.jdbcType.getName() + 
					" requires mart to be built within the same database as the source one");
		}
	}
	protected void checkValidTableName(String newTableName) throws MartBuilderException {}	// only oracle overrides it (max 30 characters)
	protected void checkValidColumnName(String columnName) throws MartBuilderException {}	// TODO should oracle override this like it does checkValidTableName? 
	/**
	 * Differentiates use of Mart Runner as opposed to Text Editor/File
	 */
	public void setUsingMartRunner(boolean usingMartRunner) {
		this.usingMartRunner = usingMartRunner;
	}
	protected String quote(String value) {	// overriden by mysql and mssql
		return value!=null ? ANSI_QUOTE + value + ANSI_QUOTE : null;
	}
	protected String getPrintCommand() {	// overriden by mssql and db2
		return null;
	}
	protected String getSourceTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699; all but mssql override it
		MartBuilderCatalog martBuilderCatalog = action.getMartBuilderCatalog();
		return quote(martBuilderCatalog.getSourceDatabaseName()) + "." + quote(martBuilderCatalog.getSourceSchemaName()) + "." + quote(tableName);
	}
	protected String getTargetTableFullyQualifiedName(final MartConstructorAction action, String tableName) {	// as per DCCTEST-1699; all but mssql override it
		MartBuilderCatalog martBuilderCatalog = action.getMartBuilderCatalog();
		return quote(martBuilderCatalog.getTargetDatabaseName()) + "." + quote(martBuilderCatalog.getTargetSchemaName()) + "." + quote(tableName);
	}
	protected abstract String buildGroupConcat(
			final UpdateOptimiser action,//TODO remove action if possible (use Strings instead) 
			final String fromTable) throws Exception;
	protected String addRenameStatement(final String schemaName, final String oldTableName, final String newTableName) {	// overriden by all but pg (most ansi-compliant)
		return ALTER + " " + TABLE + " " + quote(schemaName) + "." + quote(oldTableName) + " " + RENAME + " " + TO + " " + quote(newTableName) + "";
	}
	
	protected String addComputedTablePrefix(final String newTableName, final int bigTable) {	// only mssql & db2 override it (returning "" since they work differently)
		StringBuffer sb = new StringBuffer();
		sb.append(CREATE + " " + TABLE + " " + newTableName);
		sb.append(addBigTableHandling(bigTable));	// only for mysql
		sb.append(" " + AS + " ");
		return sb.toString();
	}
	protected String addBigTableHandling(final int bigTable) {	return "";	}	// only mysql overrides it
	protected String getTableAliasCreationPrefix() { return AS + " ";	}	// only oracle overrides it (does not accept "AS" in this context)
	protected String addComputedTableSuffix(final String newTableFullName) {	return "";	}	// only mssql overrides it
	protected String addIndexPrefix(final String targetSchemaName) {	return "";	}	// only db2 and oracle override it (other cannot prefix created index with schema name)
	protected String addTableAliasCreationClause(final String optTableFullName, final String tableAliasName) {	return optTableFullName + " " + tableAliasName;	}	// only mssql overrides it
	protected String addExtraUpdateFromClause(final String optTableFullName) {	return "";	}	// only mssql overrides it
	protected void postProcessComputedTableStatement(final List<String> statements, final String newTableFullName) {}	// only db2 overrides it
	protected String addDropTableSuffix() {	return "";	}	// only oracle overrides it (PURGE)
	
	public abstract Set<String> getPartitionedTables(JdbcLinkObject conObj, String partitionBase) throws MartBuilderException;

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
		String sql = "select table_name from information_schema.tables where table_schema = '"+schemaName+"' and table_name like 'meta_%'";
		try {
			Statement st = con.createStatement();
			ResultSet rs2 = st.executeQuery(sql);
			while (rs2.next()) {
				LeafCheckBoxNode cbn = new LeafCheckBoxNode(rs2.getString("TABLE_NAME"),isSelected);
				tables.add(cbn);
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

	public boolean hasOldConfigInTarget(JdbcLinkObject conObject, String schemaName) {
		if(conObject.getJdbcType() == JdbcType.DB2 
				|| conObject.getJdbcType() == JdbcType.MSSQL)
			return false;
		else if(conObject.getJdbcType() == JdbcType.MySQL || conObject.getJdbcType() == JdbcType.PostGreSQL) {
			JdbcLinkObject dbConObj = new JdbcLinkObject(conObject.getConnectionBase(),
					conObject.getJdbcType().useSchema()?conObject.getDatabaseName():schemaName,
							schemaName,conObject.getUserName(),conObject.getPassword(),
					conObject.getJdbcType(), conObject.getPartitionRegex(),conObject.getPtNameExpression(),
					conObject.isKeyGuessing());
			
			Connection con = ConnectionPool.Instance.getConnection(dbConObj);
			String sql = "select count(*) from information_schema.tables where table_schema = '"+schemaName+"' and table_name like 'meta_%'";
			boolean foundConfig = false;
			try {
				Statement st = con.createStatement();
				ResultSet rs2 = st.executeQuery(sql);
				rs2.next();
				if(rs2.getInt(1)>0) {
					foundConfig = true;
				}
	
				rs2.close();
			} catch(SQLException ex) {
				ex.printStackTrace();
			}
			ConnectionPool.Instance.releaseConnection(dbConObj);
			return foundConfig;
		} else { //oracle
			JdbcLinkObject dbConObj = new JdbcLinkObject(conObject.getConnectionBase(),
					conObject.getJdbcType().useSchema()?conObject.getDatabaseName():schemaName,
							schemaName,conObject.getUserName(),conObject.getPassword(),
					conObject.getJdbcType(), conObject.getPartitionRegex(),conObject.getPtNameExpression(),
					conObject.isKeyGuessing());
			
			Connection con = ConnectionPool.Instance.getConnection(dbConObj);
			boolean foundConfig = false;
			try {
				String catalog = con.getCatalog();
				ResultSet rs2 = con.getMetaData().getTables(catalog, schemaName, "%", new String[]{"TABLE"}); // "SELECT TABNAME AS TABLE_NAME, COLNAME AS COLUMN_NAME FROM SYSCAT.COLUMNS WHERE TABSCHEMA='" + schemaName + "'";
				while (rs2.next()) {
					String tableName = rs2.getString("TABLE_NAME");
					if((tableName.indexOf("meta_")==0 || tableName.indexOf("META_")==0) 
							&& !tableName.contains("$")	// exclude ORACLE's temporary tables (although unlikely to happen here)
							) {					
						foundConfig = true;
						break;
					}
				}	
				rs2.close();
			} catch(SQLException ex) {
				ex.printStackTrace();
			}

			ConnectionPool.Instance.releaseConnection(dbConObj);
			return foundConfig;
		}
	}
	
	public String[] getStatementsForAction(final MartConstructorAction action)
		throws ConstructorException {

		final List<String> statements = new ArrayList<String>();
		
		//TODO necessary for postgres?
		/*// Initial schema creation step.
		if (this.cleanState)
			statements.add("create schema " + action.getDataSetSchemaName());
		this.cleanState = false;*/
		
		try {
			checkValidMartBuilderCatalog(action.getMartBuilderCatalog());
			final String className = action.getClass().getName();
			final String methodName = "do"
					+ className.substring(className.lastIndexOf('$') + 1);
			final Method method = this.getClass().getMethod(methodName,
					new Class[] { action.getClass(), List.class });
			method.invoke(this, new Object[] { action, statements });
		} catch (final InvocationTargetException ite) {
			final Throwable t = ite.getCause();
			if (t instanceof ConstructorException)
				throw (ConstructorException) t;
			else
				throw new ConstructorException(t);
		} catch (final Throwable t) {
			if (t instanceof ConstructorException)
				throw (ConstructorException) t;
			else
				throw new ConstructorException(t);
		}
		
		return wrapStatements(statements);
	}

	protected String[] wrapStatements(final List<String> statements) {
		List<String> statementList = new ArrayList<String>();
		for (Object object : statements) {
			String statement = (String)object;
			String printCommand = getPrintCommand();
			if (!this.usingMartRunner && printCommand!=null && !printCommand.isEmpty()) {	// PRINT for mssql, ECHO for DB2
				this.commandCount++;
				String message = this.commandCount + " - " + statement;
				if (message.length()>DEBUG_MESSAGE_MAX_LENGTH) {
					message = message.substring(0, DEBUG_MESSAGE_MAX_LENGTH) + " ...";
				}
				statementList.add(printCommand +  " '" + message + "'");
			}
			statementList.add(statement);
			
			boolean addGo = this instanceof MsSQLDialect && !this.usingMartRunner;
			if (addGo) {	// only for mssql when not using MartRunner
				statementList.add(MsSQLDialect.GO + McUtils.NEW_LINE); // new line so a ";" is not added to the GO line (an isolated ";" is harmless)
			}
		}
		
		return (String[])statementList.toArray(new String[statementList.size()]);
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doSelect(final Select action, final List<String> statements) throws Exception {		
		final String sourceTableFullyQualifiedName = action.isFromSourceSchema() ? //TODO for doJoin and doLeftJoin as well?
				getSourceTableFullyQualifiedName(action, action.getTable()) :
				getTargetTableFullyQualifiedName(action, action.getTable());
		final String targetTableFullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getResultTable());
		
		final StringBuffer sb = new StringBuffer();
		sb.append(addComputedTablePrefix(targetTableFullyQualifiedName, action.getBigTable()));	// for mysql
		sb.append(SELECT + " ");
		for (final Iterator<Map.Entry<String,String>> i = action.getSelectColumns().entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry<String,String> entry = i.next();
			sb.append(ALIAS1 + ".");
			sb.append(quote(entry.getKey()));
			if (!entry.getKey().equals(entry.getValue())) {
				//this.checkColumnName((String) entry.getValue());
				sb.append(" " + AS + " ");
				sb.append(quote(entry.getValue()));
			}
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(addComputedTableSuffix(targetTableFullyQualifiedName));	// for mssql
		sb.append(" " + FROM + " " + sourceTableFullyQualifiedName + " " + getTableAliasCreationPrefix() + ALIAS1);
/*		if (action.getTableRestriction() != null
				|| !action.getPartitionRestrictions().isEmpty())
			sb.append(" where ");
		for (final Iterator i = action.getPartitionRestrictions().entrySet()
				.iterator(); i.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append("a.");
			sb.append((String) entry.getKey());
			sb.append("='");
			sb.append((String) entry.getValue());
			sb.append('\'');
			if (i.hasNext() || action.getTableRestriction() != null)
				sb.append(" and ");
		}
		if (action.getTableRestriction() != null)
			sb.append(action.getTableRestriction().getSubstitutedExpression(
					action.getSchemaPrefix(), "a"));*/

		statements.add(sb.toString());
		postProcessComputedTableStatement(statements, targetTableFullyQualifiedName);	// for db2
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doDrop(final Drop action, final List<String> statements) throws Exception {	// only Oracle overrides it
		final String targetTableFullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getTable());
		String dropTableStatement = DROP + " " + TABLE + " " + targetTableFullyQualifiedName + addDropTableSuffix();
		statements.add(dropTableStatement);
	};
	
	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doRename(final Rename action, final List<String> statements) throws Exception {
		final String targetSchemaName = action.getMartBuilderCatalog().getTargetSchemaName();
		final String oldTableName = action.getFrom();
		final String newTableName = action.getTo();
		//this.checkTableName(newTableName);
		checkValidTableName(newTableName);
		String renameStatement = addRenameStatement(targetSchemaName, oldTableName, newTableName);
		statements.add(renameStatement);
	}
	
	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doIndex(final Index action, final List<String> statements) throws Exception {
		final String targetSchemaName = action.getMartBuilderCatalog().getTargetSchemaName();
		final String targetTableFullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getTable());
		final StringBuffer sb = new StringBuffer();
		sb.append(CREATE + " " + INDEX + " " + addIndexPrefix(targetSchemaName) + INDEX_NAME_PREFIX + this.indexCount++ + " " + 
				ON + " " + targetTableFullyQualifiedName + "(");
		for (final Iterator<String> i = action.getColumns().iterator(); i.hasNext();) {
			String columnName = quote(i.next());
			sb.append(columnName);
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(")");

		statements.add(sb.toString());
	}
	
	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doJoin(final Join action, final List<String> statements) throws Exception {
		final String targetTableFullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getLeftTable());
		final String sourceTableFullyQualifiedName = getSourceTableFullyQualifiedName(action, action.getRightTable());
		final String resultTargetTableFullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getResultTable());
		final String joinType = action.isLeftJoin() ? LEFT : INNER;
		
		final StringBuffer sb = new StringBuffer();
		sb.append(addComputedTablePrefix(resultTargetTableFullyQualifiedName, action.getBigTable()));	// for mysql
		sb.append(SELECT + " ");
		sb.append(ALIAS1 + "." + "*");
		for (final Iterator<Map.Entry<String, String>> i = action.getSelectColumns().entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry<String,String> entry = i.next();
			sb.append("," + ALIAS2 + ".");
			sb.append(quote(entry.getKey()));
			if (!entry.getKey().equals(entry.getValue())) {
				//this.checkColumnName((String) entry.getValue());
				sb.append(" " + AS + " ");
				sb.append(quote(entry.getValue()));
			}
		}
		sb.append(addComputedTableSuffix(resultTargetTableFullyQualifiedName));	// for mssql
		sb.append(" " + FROM + " " + targetTableFullyQualifiedName + " " + getTableAliasCreationPrefix() + ALIAS1 + " " + joinType + " " + JOIN + " " + 
				sourceTableFullyQualifiedName + " " + getTableAliasCreationPrefix() + ALIAS2 + " " + ON + " ");
		for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
			if (i > 0)
				sb.append(" " + AND + " ");
			final String pkColName = quote((String)action.getLeftJoinColumns().get(i));
			final String fkColName = quote((String)action.getRightJoinColumns().get(i));
			sb.append(ALIAS1 + "." + pkColName);
			sb.append("=" + ALIAS2 + "." + fkColName);
		}
		/*	if (action.getRelationRestriction() != null) {
			sb.append(" and ");
			sb.append(action.getRelationRestriction().getSubstitutedExpression(
					action.getSchemaPrefix(),
					action.isRelationRestrictionLeftIsFirst() ? "a" : "b",
					action.isRelationRestrictionLeftIsFirst() ? "b" : "a",
					action.isRelationRestrictionLeftIsFirst(),
					!action.isRelationRestrictionLeftIsFirst(),
					action.getRelationRestrictionPreviousUnit()));
		}
		if (action.getTableRestriction() != null) {
			sb.append(" and (");
			sb.append(action.getTableRestriction().getSubstitutedExpression(
					action.getSchemaPrefix(), "b"));
			sb.append(')');
		}*/
		for (final Iterator i = action.getPartitionRestrictions().entrySet()
				.iterator(); i.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append(" " + AND + " " + ALIAS2 + ".");
			sb.append(quote((String) entry.getKey()));
			sb.append("=" + "'");
			sb.append(quote((String)entry.getValue()));
			sb.append("'");
		}
		if (action.getLoopbackDiffSource() != null) {
			sb.append(" " + AND + " " + ALIAS1 + ".");
			sb.append(quote(action.getLoopbackDiffSource()));
			sb.append("<>" + ALIAS2 + ".");
			sb.append(quote(action.getLoopbackDiffTarget()));
		}
		
		statements.add(sb.toString());
		postProcessComputedTableStatement(statements, resultTargetTableFullyQualifiedName);	// for db2
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doLeftJoin(final LeftJoin action, final List<String> statements) throws Exception {		
		final String targetTable1FullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getLeftTable());
		final String targetTable2FullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getRightTable());
		final String resultTargetTableFullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getResultTable());
		
		final StringBuffer sb = new StringBuffer();
		sb.append(addComputedTablePrefix(resultTargetTableFullyQualifiedName, action.getBigTable()));	// for mysql
		sb.append(SELECT + " ");
		for (final Iterator<String> i = action.getLeftSelectColumns().iterator(); i
				.hasNext();) {
			final String entry = (String) i.next();
			sb.append(ALIAS1 + ".");
			sb.append(quote(entry));
			sb.append(',');
		}
		for (final Iterator<String> i = action.getRightSelectColumns().iterator(); i
				.hasNext();) {
			final String entry = (String) i.next();
			sb.append(ALIAS2 + ".");
			sb.append(quote(entry));
			if (i.hasNext())
				sb.append(",");
		}
		sb.append(addComputedTableSuffix(resultTargetTableFullyQualifiedName));	// for mssql
		sb.append(" " + FROM + " " + targetTable1FullyQualifiedName + " " + this.getTableAliasCreationPrefix() + ALIAS1 + " " +
				LEFT + " " + JOIN + " " + targetTable2FullyQualifiedName + " " + this.getTableAliasCreationPrefix() + ALIAS2 + " " + ON + " ");
		for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
			if (i > 0)
				sb.append(" " + AND + " ");
			final String pkColName = quote((String)action.getLeftJoinColumns().get(i));
			final String fkColName = quote((String)action.getRightJoinColumns().get(i));
			sb.append(ALIAS1 + "." + pkColName);
			sb.append("=" + ALIAS2 + "." + fkColName + "");
		}
		statements.add(sb.toString());
		postProcessComputedTableStatement(statements, resultTargetTableFullyQualifiedName);	// for db2
	}
	
	//TODO missing doCreateOptimiser?
	
	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doUpdateOptimiser(final UpdateOptimiser action, final List<String> statements) throws Exception {
		final String target1TableFullyQualifiedName = getTargetTableFullyQualifiedName(action, 
				action.getSourceTableName() // Source? TODO poor naming or error?
			);
		final String target2TableFullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getOptTableName());

		final String optColName = action.getOptColumnName();
		final String optRestrictColName = action.getOptRestrictColumn();
		final String optRestrictValue = action.getOptRestrictValue();
		
		final String quotedOptColName = quote(optColName);
		final String quotedOptRestrictColName = quote(optRestrictColName);
		
		//this.checkColumnName(optColName);
		final String colType = action.getValueColumnName() == null ? INTEGER
				: VARCHAR + "("+ action.getValueColumnSize()+")";

		statements.add(ALTER + " " + TABLE + " " + target2TableFullyQualifiedName + " " + ADD + " " + quotedOptColName + " " + colType); // more ANSI-compliant;
		//TODO postgres?
		/*statements.add("alter table " + schemaName + "." + optTableName
				+ " add " + optColName + " integer default 0");*/
		final String countStmt =
			// !! TODO adapt for oracle and postgres; mysql, db2 and mssql should work
			//postgres
			/*final String countStmt = action.isCountNotBool() ? "count(1)"
					: "case count(1) when 0 then "
							+ (action.isNullNotZero() ? "null" : "0")
							+ " else 1 end";*/
			action.getValueColumnName() == null ?
					(action.isCountNotBool() ? 
						COUNT : 
						"CASE" + " " + COUNT + " " + "WHEN" + " " + "0" + " " + "THEN" + " " + 
							(action.isNullNotZero() ? NULL : "0") + " ELSE" + " " + "1" + " " + "END") : 
					buildGroupConcat(action, target1TableFullyQualifiedName);

		final StringBuffer sb = new StringBuffer();
		sb.append(UPDATE + " " + addTableAliasCreationClause(target2TableFullyQualifiedName, ALIAS1) + " " + SET + " " + quotedOptColName + "=" +
				"(" + SELECT + " " + countStmt + " " + FROM + " " + target1TableFullyQualifiedName + " " + ALIAS2 + " " + WHERE + " ");
		for (final Iterator i = action.getKeyColumns().iterator(); i.hasNext();) {
			final String keyCol = (String) i.next();
			sb.append(ALIAS1 + ".");
			sb.append(quote(keyCol));
			sb.append("=" + ALIAS2 + ".");
			sb.append(quote(keyCol));
			sb.append(" " + AND + " ");
		}
		if (optRestrictColName != null) {
			sb.append(ALIAS2 + ".");
			sb.append(quotedOptRestrictColName);
			if (optRestrictValue == null)
				sb.append(" " + IS_NULL + " " + AND + " ");
			else {
				sb.append("=" + "'");
				sb.append(optRestrictValue);
				sb.append("'" + " " + AND + " ");
			}
		}
		sb.append(NOT + "(");
		for (final Iterator i = action.getNonNullColumns().iterator(); i
				.hasNext();) {
			sb.append(ALIAS2 + ".");
			sb.append(quote((String) i.next()));
			sb.append(" " + IS_NULL);
			if (i.hasNext())
				sb.append(" " + AND + " ");
		}
		sb.append(")" + ")");
		sb.append(addExtraUpdateFromClause(target2TableFullyQualifiedName));	// only for mssql
		statements.add(sb.toString());
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public final void doCopyOptimiser(final CopyOptimiser action, final List<String> statements) throws Exception {
		
		//TODO check both target for other than mysql
		final String targetTable1FullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getParentOptTableName());
		final String targetTable2FullyQualifiedName = getTargetTableFullyQualifiedName(action, action.getOptTableName());
		
		final String optColName = action.getOptColumnName();
		final String quotedOptColName = quote(optColName);
		
//		this.checkColumnName(optColName);

		statements.add(ALTER + " " + TABLE + " " + targetTable2FullyQualifiedName + " " + ADD + " " + quotedOptColName + " " + INTEGER); // more ANSI-compliant;
		
		final StringBuffer sb = new StringBuffer();
		sb.append(UPDATE + " " + addTableAliasCreationClause(targetTable2FullyQualifiedName, ALIAS1) + " " + 
				SET + " " + quotedOptColName + "=" + "(" + SELECT + " " + "MAX" + "(" + quotedOptColName + ")" + " " + 
				FROM + " " + targetTable1FullyQualifiedName + " " + ALIAS2 + " " + WHERE + " ");
		for (final Iterator i = action.getKeyColumns().iterator(); i.hasNext();) {
			final String keyCol = (String) i.next();
			sb.append(ALIAS1 + ".");
			sb.append(quote(keyCol));
			sb.append("=" + ALIAS2 + ".");
			sb.append(quote(keyCol));
			if (i.hasNext())
				sb.append(" " + AND + " ");
		}
		sb.append(")");
		sb.append(addExtraUpdateFromClause(targetTable2FullyQualifiedName));	// only for mssql
		
		// if postgres doesn't work, don't use alias:
		/*
		final StringBuffer sb = new StringBuffer();
		sb.append("update " + schemaName + "." + optTableName + " set "
				+ optColName + "=(select max(" + optColName + ") from " + schemaName
				+ "." + parentOptTableName + " b where ");
		for (final Iterator i = action.getKeyColumns().iterator(); i.hasNext();) {
			final String keyCol = (String) i.next();
			sb.append(schemaName);
			sb.append('.');
			sb.append(optTableName);
			sb.append('.');
			sb.append(keyCol);
			sb.append("=b.");
			sb.append(keyCol);
			if (i.hasNext())
				sb.append(" and ");
		}
		sb.append(')');
		*/		
		statements.add(sb.toString());
	}

}