/*
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.configurator.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.controller.dialects.MartBuilderCatalog;

/**
 * Represents one task in the grand scheme of constructing a mart.
 * Implementations of this abstract class will provide specific methods for
 * working with the various different stages of mart construction.
 * <p>
 * In all actions, if any schema parameter is null, it means to use the dataset
 * schema instead, as specified by the datasetSchemaName parameter.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.76 $, $Date: 2008/03/12 14:22:38 $, modified by
 *          $Author: rh4 $
 * @since 0.5
 */
public abstract class MartConstructorAction {

	private MartBuilderCatalog martBuilderCatalog;
	private String datasetTableName;

	/**
	 * Sets up a node.
	 * 
	 * @param datasetSchemaName
	 *            the name of the schema within which the dataset will be
	 *            constructed. Wherever other schemas in actions are specified
	 *            as null, this schema will be used in place.
	 * @param datasetTableName
	 *            the name of the table this action is associated with.
	 */
	private MartConstructorAction(final MartBuilderCatalog martBuilderCatalog, final String datasetTableName) {
		this.martBuilderCatalog = martBuilderCatalog;
		this.datasetTableName = datasetTableName;
		Log.debug("Constructor action created: " + this.getClass().getName());
	}

	public void check() throws MartBuilderException {
		
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "__" + 
			this.martBuilderCatalog + "__" + this.datasetTableName;
	}

	/**
	 * Returns the dataset table name for this action.
	 * 
	 * @return the dataset table name.
	 */
	public String getDataSetTableName() {
		return this.datasetTableName;
	}

	/**
	 * Returns the mart builder catalog (containing source and target database/schema names) for this action.
	 * 
	 * @return the dataset schema name.
	 */
	public MartBuilderCatalog getMartBuilderCatalog() {
		return this.martBuilderCatalog;
	}

	/**
	 * Override this method to produce a message describing what this node of
	 * the graph will do.
	 * 
	 * @return a description of what this node will do.
	 */
	public abstract String getStatusMessage();

	/**
	 * Update optimiser table actions.
	 */
	public static class UpdateOptimiser extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private Collection keyColumns;

		private String optTableName;

		private Collection nonNullColumns;

		private String optColumnName;

		private String sourceTableName;

		private boolean countNotBool;

		private boolean nullNotZero;
		
		private String optRestrictColumn;
		
		private String optRestrictValue;
		
		private String valueColumnName;
		
		private String valueColumnSeparator;
		
		private int valueColumnSize = 255;

		/**
		 * Creates a new UpdateOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public UpdateOptimiser(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcUpdateOpt", new String[] {
					this.getOptTableName(), this.getOptColumnName() });
		}

		/**
		 * @return the countNotBool
		 */
		public boolean isCountNotBool() {
			return this.countNotBool;
		}

		/**
		 * @param countNotBool
		 *            the countNotBool to set
		 */
		public void setCountNotBool(final boolean countNotBool) {
			this.countNotBool = countNotBool;
		}

		/**
		 * @return the nullNotZero
		 */
		public boolean isNullNotZero() {
			return this.nullNotZero;
		}

		/**
		 * @param nullNotZero
		 *            the nullNotZero to set
		 */
		public void setNullNotZero(final boolean nullNotZero) {
			this.nullNotZero = nullNotZero;
		}

		/**
		 * @return the keyColumns
		 */
		public Collection getKeyColumns() {
			return this.keyColumns;
		}

		/**
		 * @param keyColumns
		 *            the keyColumns to set
		 */
		public void setKeyColumns(final Collection keyColumns) {
			this.keyColumns = keyColumns;
		}

		/**
		 * @return the nonNullColumns
		 */
		public Collection getNonNullColumns() {
			return this.nonNullColumns;
		}

		/**
		 * @param nonNullColumns
		 *            the nonNullColumns to set
		 */
		public void setNonNullColumns(final Collection nonNullColumns) {
			this.nonNullColumns = nonNullColumns;
		}

		/**
		 * @return the optColumnName
		 */
		public String getOptColumnName() {
			return this.optColumnName;
		}

		/**
		 * @param optColumnName
		 *            the optColumnName to set
		 */
		public void setOptColumnName(final String optColumnName) {
			this.optColumnName = optColumnName;
		}

		/**
		 * @return the optTableName
		 */
		public String getOptTableName() {
			return this.optTableName;
		}

		/**
		 * @param optTableName
		 *            the optTableName to set
		 */
		public void setOptTableName(final String optTableName) {
			this.optTableName = optTableName;
		}

		/**
		 * @return the sourceTableName
		 */
		public String getSourceTableName() {
			return this.sourceTableName;
		}

		/**
		 * @param sourceTableName
		 *            the sourceTableName to set
		 */
		public void setSourceTableName(final String sourceTableName) {
			this.sourceTableName = sourceTableName;
		}

		/**
		 * @return the optRestrictColumn
		 */
		public String getOptRestrictColumn() {
			return this.optRestrictColumn;
		}

		/**
		 * @param optRestrictColumn the optRestrictColumn to set
		 */
		public void setOptRestrictColumn(String optRestrictColumn) {
			this.optRestrictColumn = optRestrictColumn;
		}

		/**
		 * @return the optRestrictValue
		 */
		public String getOptRestrictValue() {
			return this.optRestrictValue;
		}

		/**
		 * @param optRestrictValue the optRestrictValue to set
		 */
		public void setOptRestrictValue(String optRestrictValue) {
			this.optRestrictValue = optRestrictValue;
		}

		/**
		 * @return the valueColumnName
		 */
		public String getValueColumnName() {
			return this.valueColumnName;
		}

		/**
		 * @param valueColumnName the valueColumnName to set
		 */
		public void setValueColumnName(String valueColumnName) {
			this.valueColumnName = valueColumnName;
		}

		/**
		 * @return the valueColumnSeparator
		 */
		public String getValueColumnSeparator() {
			return this.valueColumnSeparator;
		}

		/**
		 * @param valueColumnSeparator the valueColumnSeparator to set
		 */
		public void setValueColumnSeparator(String valueColumnSeparator) {
			this.valueColumnSeparator = valueColumnSeparator;
		}

		/**
		 * @return the valueColumnSize
		 */
		public int getValueColumnSize() {
			return this.valueColumnSize;
		}

		/**
		 * @param valueColumnSize the valueColumnSize to set
		 */
		public void setValueColumnSize(int valueColumnSize) {
			this.valueColumnSize = valueColumnSize;
		}
	}

	/**
	 * Copy optimiser table actions.
	 */
	public static class CopyOptimiser extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private Collection keyColumns;

		private String optTableName;

		private String optColumnName;

		private String parentOptTableName;

		/**
		 * Creates a new CopyOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public CopyOptimiser(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCopyOpt", new String[] {
					this.getOptTableName(), this.getOptColumnName() });
		}

		/**
		 * @return the keyColumns
		 */
		public Collection getKeyColumns() {
			return this.keyColumns;
		}

		/**
		 * @param keyColumns
		 *            the keyColumns to set
		 */
		public void setKeyColumns(final Collection keyColumns) {
			this.keyColumns = keyColumns;
		}

		/**
		 * @return the optColumnName
		 */
		public String getOptColumnName() {
			return this.optColumnName;
		}

		/**
		 * @param optColumnName
		 *            the optColumnName to set
		 */
		public void setOptColumnName(final String optColumnName) {
			this.optColumnName = optColumnName;
		}

		/**
		 * @return the optTableName
		 */
		public String getOptTableName() {
			return this.optTableName;
		}

		/**
		 * @param optTableName
		 *            the optTableName to set
		 */
		public void setOptTableName(final String optTableName) {
			this.optTableName = optTableName;
		}

		/**
		 * @return the parentOptTableName
		 */
		public String getParentOptTableName() {
			return this.parentOptTableName;
		}

		/**
		 * @param parentOptTableName
		 *            the parentOptTableName to set
		 */
		public void setParentOptTableName(final String parentOptTableName) {
			this.parentOptTableName = parentOptTableName;
		}
	}

	/**
	 * Create optimiser table actions.
	 */
	public static class CreateOptimiser extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private Collection keyColumns;

		private String optTableName;

		private int bigTable;

		/**
		 * Creates a new CreateOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public CreateOptimiser(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCreateOpt", this.getOptTableName());
		}

		/**
		 * @return the keyColumns
		 */
		public Collection getKeyColumns() {
			return this.keyColumns;
		}

		/**
		 * @param keyColumns
		 *            the keyColumns to set
		 */
		public void setKeyColumns(final Collection keyColumns) {
			this.keyColumns = keyColumns;
		}

		/**
		 * @return the optTableName
		 */
		public String getOptTableName() {
			return this.optTableName;
		}

		/**
		 * @param optTableName
		 *            the optTableName to set
		 */
		public void setOptTableName(final String optTableName) {
			this.optTableName = optTableName;
		}

		/**
		 * @return the bigTable
		 */
		public int getBigTable() {
			return this.bigTable;
		}

		/**
		 * @param bigTable the bigTable to set
		 */
		public void setBigTable(int bigTable) {
			this.bigTable = bigTable;
		}
	}

	/**
	 * LeftJoin actions.
	 */
	public static class LeftJoin extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String leftTable;

		//private String rightSchema; by design always target schema anyway (formerly stored in datasetSchemaNamd and now in martBuilderCatalog)

		private String rightTable;

		private List<String> leftJoinColumns;

		private List<String> rightJoinColumns;

		private List<String> leftSelectColumns;

		private List<String> rightSelectColumns;

		private String resultTable;

		private int bigTable;

		/**
		 * Creates a new LeftJoin action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public LeftJoin(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcMerge", new String[] { this.getLeftTable(),
					this.getRightTable(), this.getResultTable() });
		}

		/**
		 * @return the leftJoinColumns
		 */
		public List<String> getLeftJoinColumns() {
			return this.leftJoinColumns;
		}

		/**
		 * @param leftJoinColumns
		 *            the leftJoinColumns to set
		 */
		public void setLeftJoinColumns(final List<String> leftJoinColumns) {
			this.leftJoinColumns = leftJoinColumns;
		}

		/**
		 * @return the leftSelectColumns
		 */
		public List<String> getLeftSelectColumns() {
			return this.leftSelectColumns;
		}

		/**
		 * @param leftSelectColumns
		 *            the leftSelectColumns to set
		 */
		public void setLeftSelectColumns(final List<String> leftSelectColumns) {
			this.leftSelectColumns = leftSelectColumns;
		}

		/**
		 * @return the leftTable
		 */
		public String getLeftTable() {
			return this.leftTable;
		}

		/**
		 * @param leftTable
		 *            the leftTable to set
		 */
		public void setLeftTable(final String leftTable) {
			this.leftTable = leftTable;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(final String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the rightJoinColumns
		 */
		public List getRightJoinColumns() {
			return this.rightJoinColumns;
		}

		/**
		 * @param rightJoinColumns
		 *            the rightJoinColumns to set
		 */
		public void setRightJoinColumns(final List rightJoinColumns) {
			this.rightJoinColumns = rightJoinColumns;
		}

		/**
		 * @return the rightSelectColumns
		 */
		public List<String> getRightSelectColumns() {
			return this.rightSelectColumns;
		}

		/**
		 * @param rightSelectColumns
		 *            the rightSelectColumns to set
		 */
		public void setRightSelectColumns(final List<String> rightSelectColumns) {
			this.rightSelectColumns = rightSelectColumns;
		}

		/**
		 * @return the rightTable
		 */
		public String getRightTable() {
			return this.rightTable;
		}

		/**
		 * @param rightTable
		 *            the rightTable to set
		 */
		public void setRightTable(final String rightTable) {
			this.rightTable = rightTable;
		}

		/**
		 * @return the bigTable
		 */
		public int getBigTable() {
			return this.bigTable;
		}

		/**
		 * @param bigTable the bigTable to set
		 */
		public void setBigTable(int bigTable) {
			this.bigTable = bigTable;
		}
	}

	/**
	 * Join actions.
	 */
	public static class Join extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schemaPrefix;
		
		private String leftTable;

		private Boolean rightFromSourceSchema;	// schema name itself is stored in martBuilderCatalog so we only need to know which one to pick

		private String rightTable;

		private List leftJoinColumns;

		private List rightJoinColumns;

		private Map<String,String> selectColumns;

		private String resultTable;

		private boolean relationRestrictionLeftIsFirst;

		private TransformationUnit relationRestrictionPreviousUnit;

		private final Map partitionRestrictions = new HashMap();

		private String loopbackDiffSource;

		private String loopbackDiffTarget;

		private boolean leftJoin;

		private int bigTable;
		
		/**
		 * Creates a new LeftJoin action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Join(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcMerge", new String[] { this.getLeftTable(),
					this.getRightTable(), this.getResultTable() });
		}

		/**
		 * Get the mutable map of partition restrictions to apply.
		 * 
		 * @return the map.
		 */
		public Map getPartitionRestrictions() {
			return this.partitionRestrictions;
		}

		/**
		 * @return the leftJoinColumns
		 */
		public List getLeftJoinColumns() {
			return this.leftJoinColumns;
		}

		/**
		 * @param leftJoinColumns
		 *            the leftJoinColumns to set
		 */
		public void setLeftJoinColumns(final List leftJoinColumns) {
			this.leftJoinColumns = leftJoinColumns;
		}

		/**
		 * @return the leftTable
		 */
		public String getLeftTable() {
			return this.leftTable;
		}

		/**
		 * @param leftTable
		 *            the leftTable to set
		 */
		public void setLeftTable(final String leftTable) {
			this.leftTable = leftTable;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(final String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the rightJoinColumns
		 */
		public List getRightJoinColumns() {
			return this.rightJoinColumns;
		}

		/**
		 * @param rightJoinColumns
		 *            the rightJoinColumns to set
		 */
		public void setRightJoinColumns(final List rightJoinColumns) {
			this.rightJoinColumns = rightJoinColumns;
		}

		/**
		 * @return whether the right side of from clause should could from the source or target schema
		 */		
		public boolean isRightFromSourceSchema() {
			return this.rightFromSourceSchema;
		}
		/**
		 * @param fromSourceSchema
		 *            whether the right side of from clause applies to the source or the target schema
		 */
		public void setRightFromSourceSchema(final boolean rightFromSourceSchema) {
			this.rightFromSourceSchema = rightFromSourceSchema;
		}

		/**
		 * @return the rightTable
		 */
		public String getRightTable() {
			return this.rightTable;
		}

		/**
		 * @param rightTable
		 *            the rightTable to set
		 */
		public void setRightTable(final String rightTable) {
			this.rightTable = rightTable;
		}

		/**
		 * @return the selectColumns
		 */
		public Map<String,String> getSelectColumns() {
			return this.selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(final Map<String,String> selectColumns) {
			this.selectColumns = selectColumns;
		}




		/**
		 * @return the relationRestrictionLeftIsFirst
		 */
		public boolean isRelationRestrictionLeftIsFirst() {
			return this.relationRestrictionLeftIsFirst;
		}

		/**
		 * @param relationRestrictionLeftIsFirst
		 *            the relationRestrictionLeftIsFirst to set
		 */
		public void setRelationRestrictionLeftIsFirst(
				final boolean relationRestrictionLeftIsFirst) {
			this.relationRestrictionLeftIsFirst = relationRestrictionLeftIsFirst;
		}

		/**
		 * @return the relationRestrictionPreviousUnit
		 */
		public TransformationUnit getRelationRestrictionPreviousUnit() {
			return this.relationRestrictionPreviousUnit;
		}

		/**
		 * @param relationRestrictionPreviousUnit
		 *            the relationRestrictionPreviousUnit to set
		 */
		public void setRelationRestrictionPreviousUnit(
				final TransformationUnit relationRestrictionPreviousUnit) {
			this.relationRestrictionPreviousUnit = relationRestrictionPreviousUnit;
		}

		/**
		 * @return the loopbackDiffSource
		 */
		public String getLoopbackDiffSource() {
			return this.loopbackDiffSource;
		}

		/**
		 * @param loopbackDiffSource
		 *            the loopbackDiffSource to set
		 */
		public void setLoopbackDiffSource(final String loopbackDiffSource) {
			this.loopbackDiffSource = loopbackDiffSource;
		}

		/**
		 * @return the loopbackDiffTarget
		 */
		public String getLoopbackDiffTarget() {
			return this.loopbackDiffTarget;
		}

		/**
		 * @param loopbackDiffTarget
		 *            the loopbackDiffTarget to set
		 */
		public void setLoopbackDiffTarget(final String loopbackDiffTarget) {
			this.loopbackDiffTarget = loopbackDiffTarget;
		}

		/**
		 * @return the leftJoin
		 */
		public boolean isLeftJoin() {
			return this.leftJoin;
		}

		/**
		 * @param leftJoin
		 *            the leftJoin to set
		 */
		public void setLeftJoin(final boolean leftJoin) {
			this.leftJoin = leftJoin;
		}

		/**
		 * @return the bigTable
		 */
		public int getBigTable() {
			return this.bigTable;
		}

		/**
		 * @param bigTable the bigTable to set
		 */
		public void setBigTable(int bigTable) {
			this.bigTable = bigTable;
		}

		/**
		 * @return the schemaPrefix
		 */
		public String getSchemaPrefix() {
			return this.schemaPrefix;
		}

		/**
		 * @param schemaPrefix the schemaPrefix to set
		 */
		public void setSchemaPrefix(String schemaPrefix) {
			this.schemaPrefix = schemaPrefix;
		}
	}

	/**
	 * AddExpression actions.
	 */
	public static class AddExpression extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schemaPrefix;
		
		private String table;

		private Collection selectColumns;

		private Map expressionColumns;

		private Collection groupByColumns;

		private String resultTable;

		/**
		 * Creates a new Select action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public AddExpression(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcExpressionAdd", this.getExpressionColumns()
					.keySet().toString());
		}

		/**
		 * @return the expressionColumns
		 */
		public Map getExpressionColumns() {
			return this.expressionColumns;
		}

		/**
		 * @param expressionColumns
		 *            the expressionColumns to set
		 */
		public void setExpressionColumns(final Map expressionColumns) {
			this.expressionColumns = expressionColumns;
		}

		/**
		 * @return the groupByColumns
		 */
		public Collection getGroupByColumns() {
			return this.groupByColumns;
		}

		/**
		 * @param groupByColumns
		 *            the groupByColumns to set
		 */
		public void setGroupByColumns(final Collection groupByColumns) {
			this.groupByColumns = groupByColumns;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(final String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the selectColumns
		 */
		public Collection getSelectColumns() {
			return this.selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(final Collection selectColumns) {
			this.selectColumns = selectColumns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(final String table) {
			this.table = table;
		}

		/**
		 * @return the schemaPrefix
		 */
		public String getSchemaPrefix() {
			return this.schemaPrefix;
		}

		/**
		 * @param schemaPrefix the schemaPrefix to set
		 */
		public void setSchemaPrefix(String schemaPrefix) {
			this.schemaPrefix = schemaPrefix;
		}
	}

	/**
	 * Select distinct * actions.
	 */
	public static class Distinct extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schema;

		private String table;

		private String resultTable;

		private Collection keepCols;

		private int bigTable;

		/**
		 * Creates a new Distinct action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Distinct(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDistinct", new String[] {
					this.getResultTable(), this.getTable() });
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(final String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the schema
		 */
		public String getSchema() {
			return this.schema;
		}

		/**
		 * @param schema
		 *            the schema to set
		 */
		public void setSchema(final String schema) {
			this.schema = schema;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(final String table) {
			this.table = table;
		}

		/**
		 * @return the keepCols
		 */
		public Collection getKeepCols() {
			return this.keepCols;
		}

		/**
		 * @param keepCols
		 *            the keepCols to set
		 */
		public void setKeepCols(final Collection keepCols) {
			this.keepCols = keepCols;
		}

		/**
		 * @return the bigTable
		 */
		public int getBigTable() {
			return this.bigTable;
		}

		/**
		 * @param bigTable the bigTable to set
		 */
		public void setBigTable(int bigTable) {
			this.bigTable = bigTable;
		}
	}

	/**
	 * Select actions.
	 */
	public static class Select extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schemaPrefix;
		
		private Boolean fromSourceSchema;	// schema name itself is stored in martBuilderCatalog so we only need to know which one to pick

		private String table;

		private Map<String,String> selectColumns;

		private String resultTable;


		private final Map partitionRestrictions = new HashMap();

		private int bigTable;

		/**
		 * Creates a new Select action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Select(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCreate", new String[] {
					this.getResultTable(), this.getTable() });
		}

		/**
		 * Get the mutable map of partition restrictions to apply.
		 * 
		 * @return the map.
		 */
		public Map getPartitionRestrictions() {
			return this.partitionRestrictions;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return this.resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(final String resultTable) {
			this.resultTable = resultTable;
		}

		public boolean isFromSourceSchema() {
			return this.fromSourceSchema;
		}
		
		/**
		 * @param fromSourceSchema
		 *            whether the from clause applies to the source or the target schema
		 */
		public void setFromSourceSchema(final boolean fromSourceSchema) {
			this.fromSourceSchema = fromSourceSchema;
		}

		/**
		 * @return the selectColumns
		 */
		public Map<String,String> getSelectColumns() {
			return this.selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(final Map<String,String> selectColumns) {
			this.selectColumns = selectColumns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(final String table) {
			this.table = table;
		}



		/**
		 * @return the bigTable
		 */
		public int getBigTable() {
			return this.bigTable;
		}

		/**
		 * @param bigTable the bigTable to set
		 */
		public void setBigTable(int bigTable) {
			this.bigTable = bigTable;
		}

		/**
		 * @return the schemaPrefix
		 */
		public String getSchemaPrefix() {
			return this.schemaPrefix;
		}

		/**
		 * @param schemaPrefix the schemaPrefix to set
		 */
		public void setSchemaPrefix(String schemaPrefix) {
			this.schemaPrefix = schemaPrefix;
		}
	}

	/**
	 * Initial unroll actions.
	 */
	public static class InitialUnroll extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schema;

		private String sourceTable;

		private String unrollPKCol;

		private String unrollIDCol;

		private String unrollNameCol;

		private String unrollIterationCol;

		private String namingCol;

		private String table;

		private int bigTable;

		/**
		 * Creates a new InitialUnroll action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public InitialUnroll(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcInitialUnroll");
		}

		/**
		 * @return the namingCol
		 */
		public String getNamingCol() {
			return this.namingCol;
		}

		/**
		 * @param namingCol
		 *            the namingCol to set
		 */
		public void setNamingCol(final String namingCol) {
			this.namingCol = namingCol;
		}

		/**
		 * @return the schema
		 */
		public String getSchema() {
			return this.schema;
		}

		/**
		 * @param schema
		 *            the schema to set
		 */
		public void setSchema(final String schema) {
			this.schema = schema;
		}

		/**
		 * @return the sourceTable
		 */
		public String getSourceTable() {
			return this.sourceTable;
		}

		/**
		 * @param sourceTable
		 *            the sourceTable to set
		 */
		public void setSourceTable(final String sourceTable) {
			this.sourceTable = sourceTable;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(final String table) {
			this.table = table;
		}

		/**
		 * @return the unrollIDCol
		 */
		public String getUnrollIDCol() {
			return this.unrollIDCol;
		}

		/**
		 * @param unrollIDCol
		 *            the unrollIDCol to set
		 */
		public void setUnrollIDCol(final String unrollIDCol) {
			this.unrollIDCol = unrollIDCol;
		}

		/**
		 * @return the unrollIterationCol
		 */
		public String getUnrollIterationCol() {
			return this.unrollIterationCol;
		}

		/**
		 * @param unrollIterationCol
		 *            the unrollIterationCol to set
		 */
		public void setUnrollIterationCol(final String unrollIterationCol) {
			this.unrollIterationCol = unrollIterationCol;
		}

		/**
		 * @return the unrollNameCol
		 */
		public String getUnrollNameCol() {
			return this.unrollNameCol;
		}

		/**
		 * @param unrollNameCol
		 *            the unrollNameCol to set
		 */
		public void setUnrollNameCol(final String unrollNameCol) {
			this.unrollNameCol = unrollNameCol;
		}

		/**
		 * @return the unrollPKCol
		 */
		public String getUnrollPKCol() {
			return this.unrollPKCol;
		}

		/**
		 * @param unrollPKCol
		 *            the unrollPKCol to set
		 */
		public void setUnrollPKCol(final String unrollPKCol) {
			this.unrollPKCol = unrollPKCol;
		}

		/**
		 * @return the bigTable
		 */
		public int getBigTable() {
			return this.bigTable;
		}

		/**
		 * @param bigTable the bigTable to set
		 */
		public void setBigTable(int bigTable) {
			this.bigTable = bigTable;
		}
	}

	/**
	 * Expand unroll actions.
	 */
	public static class ExpandUnroll extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String schema;

		private String sourceTable;

		private String unrollFKCol;

		private String unrollPKCol;

		private String unrollIDCol;

		private String unrollNameCol;

		private String unrollIterationCol;

		private int unrollIteration;

		private String namingCol;

		private List parentCols;

		private int bigTable;
		
		private boolean reversed;

		/**
		 * Creates a new ExpandUnroll action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public ExpandUnroll(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcExpandUnroll");
		}

		/**
		 * @return the namingCol
		 */
		public String getNamingCol() {
			return this.namingCol;
		}

		/**
		 * @param namingCol
		 *            the namingCol to set
		 */
		public void setNamingCol(final String namingCol) {
			this.namingCol = namingCol;
		}

		/**
		 * @return the schema
		 */
		public String getSchema() {
			return this.schema;
		}

		/**
		 * @param schema
		 *            the schema to set
		 */
		public void setSchema(final String schema) {
			this.schema = schema;
		}

		/**
		 * @return the sourceTable
		 */
		public String getSourceTable() {
			return this.sourceTable;
		}

		/**
		 * @param sourceTable
		 *            the sourceTable to set
		 */
		public void setSourceTable(final String sourceTable) {
			this.sourceTable = sourceTable;
		}

		/**
		 * @return the unrollFKCol
		 */
		public String getUnrollFKCol() {
			return this.unrollFKCol;
		}

		/**
		 * @param unrollFKCol
		 *            the unrollFKCol to set
		 */
		public void setUnrollFKCol(final String unrollFKCol) {
			this.unrollFKCol = unrollFKCol;
		}

		/**
		 * @return the unrollIDCol
		 */
		public String getUnrollIDCol() {
			return this.unrollIDCol;
		}

		/**
		 * @param unrollIDCol
		 *            the unrollIDCol to set
		 */
		public void setUnrollIDCol(final String unrollIDCol) {
			this.unrollIDCol = unrollIDCol;
		}

		/**
		 * @return the unrollIterationCol
		 */
		public String getUnrollIterationCol() {
			return this.unrollIterationCol;
		}

		/**
		 * @param unrollIterationCol
		 *            the unrollIterationCol to set
		 */
		public void setUnrollIterationCol(final String unrollIterationCol) {
			this.unrollIterationCol = unrollIterationCol;
		}

		/**
		 * @return the unrollNameCol
		 */
		public String getUnrollNameCol() {
			return this.unrollNameCol;
		}

		/**
		 * @param unrollNameCol
		 *            the unrollNameCol to set
		 */
		public void setUnrollNameCol(final String unrollNameCol) {
			this.unrollNameCol = unrollNameCol;
		}

		/**
		 * @return the unrollPKCol
		 */
		public String getUnrollPKCol() {
			return this.unrollPKCol;
		}

		/**
		 * @param unrollPKCol
		 *            the unrollPKCol to set
		 */
		public void setUnrollPKCol(final String unrollPKCol) {
			this.unrollPKCol = unrollPKCol;
		}

		/**
		 * @return the unrollIteration
		 */
		public int getUnrollIteration() {
			return this.unrollIteration;
		}

		/**
		 * @param unrollIteration
		 *            the unrollIteration to set
		 */
		public void setUnrollIteration(final int unrollIteration) {
			this.unrollIteration = unrollIteration;
		}

		/**
		 * @return the parentCols
		 */
		public List getParentCols() {
			return this.parentCols;
		}

		/**
		 * @param parentCols
		 *            the parentCols to set
		 */
		public void setParentCols(final List parentCols) {
			this.parentCols = parentCols;
		}

		/**
		 * @return the bigTable
		 */
		public int getBigTable() {
			return this.bigTable;
		}

		/**
		 * @param bigTable the bigTable to set
		 */
		public void setBigTable(int bigTable) {
			this.bigTable = bigTable;
		}

		/**
		 * @return the reversed
		 */
		public boolean isReversed() {
			return this.reversed;
		}

		/**
		 * @param reversed the reversed to set
		 */
		public void setReversed(boolean reversed) {
			this.reversed = reversed;
		}
	}

	/**
	 * Drop column actions.
	 */
	public static class DropColumns extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private Collection columns;

		private String table;

		/**
		 * Creates a new DropColumns action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public DropColumns(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDropCols", this.getColumns().toString());
		}

		/**
		 * @return the columns
		 */
		public Collection getColumns() {
			return this.columns;
		}

		/**
		 * @param columns
		 *            the columns to set
		 */
		public void setColumns(final Collection columns) {
			this.columns = columns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(final String table) {
			this.table = table;
		}
	}

	/**
	 * Drop actions.
	 */
	public static class Drop extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String table;

		/**
		 * Creates a new Drop action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Drop(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDrop", this.getTable());
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(final String table) {
			this.table = table;
		}
	}

	/**
	 * Index actions.
	 */
	public static class Index extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String table;

		private List<String> columns;

		/**
		 * Creates a new Index action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Index(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcIndex", new String[] { this.getTable(),
					this.getColumns().toString() });
		}

		/**
		 * @return the columns
		 */
		public List<String> getColumns() {
			return this.columns;
		}

		/**
		 * @param columns
		 *            the columns to set
		 */
		public void setColumns(final List<String> columns) {
			this.columns = columns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return this.table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(final String table) {
			this.table = table;
		}
	}

	/**
	 * Rename actions.
	 */
	public static class Rename extends MartConstructorAction {
		private static final long serialVersionUID = 1L;

		private String from;

		private String to;

		/**
		 * Creates a new Rename action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Rename(final MartBuilderCatalog martBuilderCatalog,
				final String datasetTableName) {
			super(martBuilderCatalog, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcRename", new String[] { this.getFrom(),
					this.getTo() });
		}

		/**
		 * @return the from
		 */
		public String getFrom() {
			return this.from;
		}

		/**
		 * @param from
		 *            the from to set
		 */
		public void setFrom(final String from) {
			this.from = from;
		}

		/**
		 * @return the to
		 */
		public String getTo() {
			return this.to;
		}

		/**
		 * @param to
		 *            the to to set
		 */
		public void setTo(final String to) {
			this.to = to;
		}

	}
}
