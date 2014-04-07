package org.biomart.configurator.controller;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.ConstructorException;
import org.biomart.common.exceptions.ListenerException;
import org.biomart.common.exceptions.PartitionException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.configurator.controller.MartConstructor.ConstructorRunnable;
import org.biomart.configurator.controller.dialects.MartBuilderCatalog;
import org.biomart.configurator.model.JoinTable;
import org.biomart.configurator.model.MartConstructorAction;
import org.biomart.configurator.model.SelectFromTable;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.configurator.model.WrappedColumn;
import org.biomart.configurator.model.MartConstructorAction.CopyOptimiser;
import org.biomart.configurator.model.MartConstructorAction.CreateOptimiser;
import org.biomart.configurator.model.MartConstructorAction.Drop;
import org.biomart.configurator.model.MartConstructorAction.Index;
import org.biomart.configurator.model.MartConstructorAction.Join;
import org.biomart.configurator.model.MartConstructorAction.LeftJoin;
import org.biomart.configurator.model.MartConstructorAction.Rename;
import org.biomart.configurator.model.MartConstructorAction.Select;
import org.biomart.configurator.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.objects.enums.DatasetOptimiserType;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.Table;

/**
 * Defines the generic way of constructing a mart. Generates a graph of
 * actions then iterates through that graph in an ordered manner, ensuring
 * that no action is reached before all actions it depends on have been
 * reached. Each action it iterates over fires an action event to all
 * listeners registered with it.
 */
public class GenericConstructorRunnable implements ConstructorRunnable {
	private boolean cancelled = false;

	private Mart datasets;

	private Collection<String> schemaPrefixes;

	private MartBuilderCatalog	// more informative than dataset name 
		datasetSchemaName;	//TODO should be renamed to martBuilderCatalog (kept as "datasetSchemaName" for now to minimize visible differences with Base Revision and better highlight changes)  
	
	private Exception failure = null;

	private Collection<MartConstructorListener> martConstructorListeners;

	private double percentComplete = 0.0;

	private final Map<DatasetTable,Set<String>> uniqueOptCols = new HashMap<DatasetTable,Set<String>>();

	private final Map<Object[], String> finalNameCache = new HashMap<Object[], String>();

	private final Map<DatasetTable,Set<String>> indexOptCols = new HashMap<DatasetTable,Set<String>>();

	private String statusMessage = Resources.get("mcCreatingGraph");

	private int tempNameCount = 0;

	private boolean alive = true;

	/**
	 * Constructs a builder object that will construct an action graph
	 * containing all actions necessary to build the given dataset, then
	 * emit events related to those actions.
	 * <p>
	 * The helper specified will interface between the builder object and
	 * the data source, providing it with bits of data it may need in order
	 * to construct the graph.
	 * 
	 * @param datasetSchemaName
	 *            the name of the database schema into which the transformed
	 *            dataset should be put.
	 * @param datasets
	 *            the dataset(s) to transform into a mart.
	 * @param schemaPrefixes
	 *            only process datasets that exist in this list of
	 *            partitions.
	 */
	public GenericConstructorRunnable(final String targetDatabaseName, final String targetSchemaName,
			final Mart datasets, final Collection<String> schemaPrefixes) {
		super();
		Log.debug("Created generic constructor runnable");
		this.datasets = datasets;
		this.schemaPrefixes = schemaPrefixes;
		this.martConstructorListeners = new ArrayList<MartConstructorListener>();
		SourceSchema sourceSchema = datasets.getCentralTable().getSchema();
		JdbcLinkObject jdbcLinkObject = sourceSchema.getJdbcLinkObject();
		this.datasetSchemaName = new MartBuilderCatalog(
				jdbcLinkObject.getDatabaseName(), jdbcLinkObject.getSchemaName(), 
				targetDatabaseName, targetSchemaName);
	}

	private void checkCancelled() throws ConstructorException {
		if (this.cancelled)
			throw new ConstructorException(Resources.get("mcCancelled"));
	}

	/**
	 * This is the starting point for the conversion of a dataset into a set
	 * of actions. Internally, it constructs a graph of actions specific to
	 * this dataset, populates the graph, then iterates over the graph at
	 * the end emitting those actions as events in the correct order, so
	 * that any action that depends on another action is guaranteed to be
	 * emitted after the action it depends on.
	 * 
	 * @param dataset
	 *            the dataset to build an action graph for and then emit
	 *            actions from that graph.
	 * @param totalDataSetCount
	 *            a counter informing this method how many datasets in total
	 *            there are to process. It is used to work out percentage
	 *            process.
	 * @throws Exception
	 *             if anything goes wrong at all during the transformation
	 *             process.
	 */
	private void makeActionsForDataset(final Mart dataset,
			final int totalDataSetCount) throws ListenerException,
			ValidationException, ConstructorException, SQLException,
			PartitionException {
		Log.debug("Making actions for dataset " + dataset);
		// Check not cancelled.
		this.checkCancelled();

		// Start with a fresh set of final names.
		this.finalNameCache.clear();

		// Find out the main table source schema.
		final SourceSchema templateSchema = dataset.getCentralTable().getSchema();

		// Is it partitioned?
		Collection schemaPartitions = new ArrayList();

		for (final Iterator i = schemaPartitions.iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (!this.schemaPrefixes.isEmpty()
					&& !this.schemaPrefixes.contains(entry.getValue()))
				i.remove();
		}

		// Work out the progress step size : 1 step = 1 table per source
		// schema partition.
		final Collection<DatasetTable> tablesToProcess = this.getTablesToProcess(dataset);
		double stepPercent = 100.0 / totalDataSetCount;
		stepPercent /= tablesToProcess.size();
		stepPercent /= schemaPartitions.size();

		// Process the tables.
		{
			
			final Set<DatasetTable> droppedTables = new HashSet<DatasetTable>();
			// Clear out optimiser col names so that they start
			// again on this partition.
			this.uniqueOptCols.clear();
			this.indexOptCols.clear();

			Log.debug("Starting schema partition " );
			this.issueListenerEvent(
					MartConstructorListener.PARTITION_STARTED,
					"");

			// Loop over dataset partitions.
			boolean fakeDSPartition =true;

			while (fakeDSPartition) {
				fakeDSPartition = false;
				// Make more specific.
				String partitionedDataSetName = dataset.getName();
				this.issueListenerEvent(
						MartConstructorListener.DATASET_STARTED,
						partitionedDataSetName);
				final Map<DatasetTable,Integer> bigParents = new HashMap<DatasetTable,Integer>();
				for (final Iterator<DatasetTable> i = tablesToProcess.iterator(); i
						.hasNext();) {
					final DatasetTable dsTable = (DatasetTable) i.next();
					if (!droppedTables.contains(dsTable.getParent())) {
						// Loop over dataset table partitions.

						boolean fakeDMPartition = true;
						final double subStepPercent =  0.5;

						while (fakeDMPartition) {
							fakeDMPartition = false;
							final double targetPercent = this.percentComplete
									+ subStepPercent;
							if (!this.makeActionsForDatasetTable(
									bigParents, subStepPercent,
									templateSchema,
									"",
									"",
									 dataset, dsTable))
								droppedTables.add(dsTable);
							// In case the construction didn't do all the
							// steps.
							this.percentComplete = targetPercent;
						}
					}

					// Check not cancelled.
					this.checkCancelled();
				}
				this.issueListenerEvent(
						MartConstructorListener.DATASET_ENDED,
						partitionedDataSetName);
			}

			this.issueListenerEvent(
					MartConstructorListener.PARTITION_ENDED,
					"");
		}
		Log.debug("Finished dataset " + dataset);
		
	}

	private List<DatasetTable> getTablesToProcess(final Mart dataset)
			throws ValidationException {
		Log.debug("Creating ordered list of tables for dataset " + dataset);
		// Create a list in the order by which we want to process tables.
		final List<DatasetTable> tablesToProcess = new ArrayList<DatasetTable>();
		// Main table first.
		tablesToProcess.add(dataset.getMainTable());
		// Now recursively expand the table list.
		for (int i = 0; i < tablesToProcess.size(); i++) {
			final DatasetTable tbl = (DatasetTable) tablesToProcess.get(i);
			// Expand the table.
			final Collection<DatasetTable> nextSCs = new ArrayList<DatasetTable>();
			final Collection<DatasetTable> nextDims = new ArrayList<DatasetTable>();
			if (tbl.getPrimaryKey() != null)
				for (final Iterator<Relation> j = tbl.getPrimaryKey().getRelations()
						.iterator(); j.hasNext();) {
					final Relation r = (Relation) j.next();
					final DatasetTable dsTab = (DatasetTable) r
							.getManyKey().getTable();
					if (
							dsTab.getFocusRelation() != null
							)
						if (dsTab.getType().equals(
								DatasetTableType.DIMENSION))
							nextDims.add(dsTab);
						else
							nextSCs.add(dsTab);
				}
			// We need to insert each dimension directly
			// after its parent table and before any subsequent
			// subclass table. This ensures that by the time the subclass
			// table is created, the parent table will have all its
			// columns in place and complete already.
			tablesToProcess.addAll(i + 1, nextSCs);
			tablesToProcess.addAll(i + 1, nextDims);
		}
		return tablesToProcess;
	}

	private boolean makeActionsForDatasetTable(final Map<DatasetTable,Integer> bigParents,
			double stepPercent, final SourceSchema templateSchema,
			final String schemaPartition, final String schemaPrefix, final Mart dataset,
			final DatasetTable dsTable) throws ListenerException,
			SQLException, PartitionException, ConstructorException {
		Log.debug("Creating actions for table " + dsTable);
		final String finalCombinedName = this.getFinalName(schemaPrefix, dsTable);
		final String tempName = "TEMP";
		String previousTempTable = null;
		boolean firstJoin = true;
		boolean requiresFinalLeftJoin = false;
		boolean requiresDistinct = false;
		final Set<DatasetColumn> droppedCols = new HashSet<DatasetColumn>();
		//int bigness = dsTable.getType().equals(DatasetTableType.MAIN) ? 0
		//		: ((Integer) bigParents.get(dsTable.getParent()))
		//				.intValue();
		int bigness = 0;

		// Skip immediately if not applicable to current schema partition.
		if (!McUtils.isStringEmpty(schemaPartition))
			return false;

		// Use the transformation units to create the basic table.
		// FIXME
		final Collection<TransformationUnit> units = dsTable.getTransformationUnits();
		stepPercent /= units.size();
		Relation firstJoinRel = null;
		for (final Iterator<TransformationUnit> j = units.iterator(); j.hasNext(); this.percentComplete += stepPercent) {
			this.checkCancelled();

			// Check if TU actually applies to us. If not, skip it.
			final TransformationUnit tu = (TransformationUnit) j.next();
			if (!tu.appliesToPartition(schemaPrefix))
				continue;
			final String tempTable = tempName + this.tempNameCount++;

			// Translate TU to Action.
			// Expression?


			if (tu instanceof JoinTable) {
				if (firstJoinRel == null)
					firstJoinRel = ((JoinTable) tu).getSchemaRelation();
				bigness = 0;
				requiresFinalLeftJoin |= this.doJoinTable(templateSchema,
						schemaPartition, schemaPrefix,
						dataset, dsTable, (JoinTable) tu, firstJoinRel,
						previousTempTable, tempTable, droppedCols, bigness,
						finalCombinedName);
			}

			// Select-from?
			else if (tu instanceof SelectFromTable) {
				bigness = 0;
				this.doSelectFromTable(templateSchema, schemaPartition,
						schemaPrefix, dataset, dsTable,
						(SelectFromTable) tu, tempTable, bigness,
						finalCombinedName);
			} else
				throw new BioMartError();

			if (previousTempTable != null) {
				final Drop action = new Drop(this.datasetSchemaName,
						finalCombinedName);
				action.setTable(previousTempTable);
				this.issueAction(action);
			}

			if (tu instanceof JoinTable && firstJoin) {
				if (droppedCols.size() == tu.getNewColumnNameMap().size()
						&& !dsTable.getType().equals(DatasetTableType.MAIN)) {
					// If first join of non-MAIN table dropped all cols
					// then the target table does not exist and the entire
					// non-MAIN table can be dropped. This also means that
					// if this is SUBCLASS then all its DMS and further
					// SUBCLASS tables can be ignored.
					final Drop action = new Drop(this.datasetSchemaName,
							finalCombinedName);
					action.setTable(tempTable);
					this.issueAction(action);
					return false;
				}
				// Don't repeat this check.
				firstJoin = false;
			}

			// Update the previous table.
			previousTempTable = tempTable;
		}

		// Do a final left-join against the parent to reinstate
		// any potentially missing rows.
		if (requiresFinalLeftJoin
				&& !dsTable.getType().equals(DatasetTableType.MAIN)) {
			final String tempTable = tempName + this.tempNameCount++;
			bigness = 0;
			this.doParentLeftJoin(schemaPrefix, dataset,
					dsTable, finalCombinedName, previousTempTable,
					tempTable, droppedCols, bigness);
			previousTempTable = tempTable;
		}

		// Drop masked dependencies and create column indices.
		final List<DatasetColumn> dropCols = new ArrayList<DatasetColumn>();
		final List<DatasetColumn> keepCols = new ArrayList<DatasetColumn>();
/*		for (final Iterator<Column> x = dsTable.getColumnList().iterator(); x
				.hasNext();) {
			final DatasetColumn col = (DatasetColumn) x.next();
			if (col.inPartition(schemaPrefix)
					&& !droppedCols.contains(col.getName()))
				if (col.isRequiredInterim() && !col.isRequiredFinal())
					dropCols.add(col.getPartitionedName());
				else if (col.isRequiredFinal())
					keepCols.add(col);
		}
*/
		// Does it need a final distinct?
/*		if (requiresDistinct) {
			final String tempTable = tempName + this.tempNameCount++;
			final Set<DatasetColumn> keepColNames = new HashSet<DatasetColumn>();
			for (final Iterator<DatasetColumn> i = keepCols.iterator(); i.hasNext();)
				keepColNames.add(((DatasetColumn) i.next())
						.getName());
			this.doDistinct(dataset, dsTable, finalCombinedName,
					previousTempTable, tempTable, keepColNames, bigness);
			previousTempTable = tempTable;
		} else if (!dropCols.isEmpty()) {
			final DropColumns dropcol = new DropColumns(
					this.datasetSchemaName, finalCombinedName);
			dropcol.setTable(previousTempTable);
			dropcol.setColumns(dropCols);
			this.issueAction(dropcol);
		}*/

		// Indexing.
		for (final Iterator<DatasetColumn> i = keepCols.iterator(); i.hasNext();) {
			final DatasetColumn col = (DatasetColumn) i.next();
			if (col.isColumnIndexed()) {
				final Index index = new Index(this.datasetSchemaName,
						finalCombinedName);
				index.setTable(previousTempTable);
				index.setColumns(Collections.singletonList(col
						.getName()));
				this.issueAction(index);
			}
		}

		// Add a rename action to produce the final table.
		final Rename action = new Rename(this.datasetSchemaName,
				finalCombinedName);
		action.setFrom(previousTempTable);
		action.setTo(finalCombinedName);
		this.issueAction(action);

		// Create indexes on all keys on the final table.
		for (final Iterator<Key> j = dsTable.getKeys().iterator(); j.hasNext();) {
			final Key key = (Key) j.next();
			final List<String> keyCols = new ArrayList<String>();
			for (int k = 0; k < key.getColumns().size(); k++)
				keyCols.add(((DatasetColumn) key.getColumns().get(k))
						.getName());
			final Index index = new Index(this.datasetSchemaName,
					finalCombinedName);
			index.setTable(finalCombinedName);
			index.setColumns(keyCols);
			this.issueAction(index);
		}

		// Create optimiser columns - either count or bool,
		// or none if not required.
		DatasetOptimiserType oType = dataset.getDatasetOptimiserType();
		if (dsTable.getType().equals(DatasetTableType.MAIN_SUBCLASS))
			oType = oType.isTable() ? DatasetOptimiserType.TABLE_INHERIT
					: DatasetOptimiserType.COLUMN_INHERIT;
		if (!oType.equals(DatasetOptimiserType.NONE)
				&& !dsTable.isSkipOptimiser())
			this.doOptimiseTable(schemaPrefix, dataset,
					dsTable, oType, !dsTable.getType().equals(
							DatasetTableType.DIMENSION)
							&& dataset.getDatasetOptimiserType().isTable(),
					bigness, finalCombinedName);

		// Remember size for children.
		bigParents.put(dsTable, new Integer(bigness));

		// Return success.*/
		return true;
	}


	private void doSelectFromTable(final SourceSchema templateSchema,
			final String schemaPartition, final String schemaPrefix, final Mart dataset,
			final DatasetTable dsTable, final SelectFromTable stu,
			final String tempTable, final int bigness,
			final String finalCombinedName) throws SQLException,
			ListenerException, PartitionException {

		final Select action = new Select(this.datasetSchemaName,
				finalCombinedName);
		action.setBigTable(bigness);
		action.setSchemaPrefix(schemaPrefix);

		// If this is a dimension, look up DM PT,
		// otherwise if this is the main table, look up DS PT,

		{
			// This is a select, so we are dealing with the first row
			// only.

			// The naming column will also always be the first row,
			// which will be on pta itself, so we don't need to
			// initialise the table as it has already been done.

			// For each of the getNewColumnNameMap cols that are in the
			// current ptable application, add a restriction for that col
			// using current ptable column value.
/*			for (final Iterator i = stu.getNewColumnNameMap().entrySet()
					.iterator(); i.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final DatasetColumn dsCol = (DatasetColumn) entry
						.getValue();
				// Only apply this to the dsCol which matches
				// the partition row's ds col.
				if (dsCol.inPartition(schemaPrefix)
						&& (dsCol.getName()
								.equals(prow.getRootDataSetCol()) || dsCol
								.getName().endsWith(
										Resources.get("columnnameSep")
												+ prow.getRootDataSetCol())))
					// Apply restriction.
					action.getPartitionRestrictions().put(
							((Column) entry.getKey()).getName(),
							pcol.getRawValueForRow(pcol.getPartitionTable()
									.currentRow()));
			}
			// PrepareRow on subdivision, if any.
			if (pta.getPartitionAppliedRows().size() > 1) {
				final PartitionAppliedRow subprow = (PartitionAppliedRow) pta
						.getPartitionAppliedRows().get(1);
				((PartitionColumn) pta.getPartitionTable().getColumns()
						.get(subprow.getPartitionCol()))
						.getPartitionTable().prepareRows(schemaPrefix,
								PartitionTable.UNLIMITED_ROWS);
			}*/
		}

		final Table sourceTable = stu.getTable();
		// Make sure that we use the same partition on the RHS
		// if it exists, otherwise use the default partition.
		String schema = null;
		Boolean fromSourceSchema = null;	// must distinguish for rdbms other than mysql (for fully qualified names)
		if (sourceTable instanceof DatasetTable) {
			schema = this.datasetSchemaName.getTargetSchemaName();
			fromSourceSchema = false; 		
		} else {
			schema = stu.getTable().getSchema().getJdbcLinkObject().getSchemaName();
			if (!this.datasetSchemaName.getSourceSchemaName().equalsIgnoreCase(schema)) {
				throw new BioMartError(this.datasetSchemaName.getSourceSchemaName() + ", " + schema);
			}
			fromSourceSchema = true;				
		}
		if (schema == null) // Can never happen.
			throw new BioMartError();

		// Source tables are always main or subclass and
		// therefore are never partitioned.
		final String table = sourceTable instanceof DatasetTable ? this
				.getFinalName(schemaPrefix,
						(DatasetTable) sourceTable) : stu.getTable()
				.getName();
		final Map<String,String> selectCols = new HashMap<String,String>();
		// Select columns from parent table.
		for (final Iterator<Map.Entry<Column, DatasetColumn>> k = stu.getNewColumnNameMap().entrySet()
				.iterator(); k.hasNext();) {
			final Map.Entry<Column,DatasetColumn> entry = k.next();
			final DatasetColumn col = (DatasetColumn) entry.getValue();

			if (col.inPartition(schemaPrefix))
				selectCols
						.put(
								sourceTable instanceof DatasetTable ? entry
										.getKey().getName()
										: entry.getKey().getName(), col
										.getName());

		}
		// Add to selectCols all the inherited has columns, if
		// this is not a dimension table and the optimiser type is not a
		// table one.
		DatasetOptimiserType oType = dataset.getDatasetOptimiserType();
		if (dsTable.getType().equals(DatasetTableType.MAIN_SUBCLASS))
			oType = oType.isTable() ? DatasetOptimiserType.TABLE_INHERIT
					: DatasetOptimiserType.COLUMN_INHERIT;
		if (!oType.isTable() && sourceTable instanceof DatasetTable
				&& !dsTable.getType().equals(DatasetTableType.DIMENSION)) {
			final Collection<String> hasCols =  this.uniqueOptCols
					.get(sourceTable);
			if (hasCols != null) {
				for (final Iterator<String> k = hasCols.iterator(); k.hasNext();) {
					final String hasCol = (String) k.next();
					selectCols.put(hasCol, hasCol);
				}
				// Make inherited copies.
				this.uniqueOptCols.put(dsTable, new HashSet<String>(hasCols));
			}
			// Inherited indexed optimiser cols.
			final Collection<String> indCols = this.indexOptCols
					.get(sourceTable);
			if (indCols != null)
				this.indexOptCols.put(dsTable, new HashSet<String>(indCols));
		}
		// Do the select.
		action.setFromSourceSchema(fromSourceSchema);
		action.setTable(table);
		action.setSelectColumns(selectCols);
		action.setResultTable(tempTable);

		// Table restriction.
//		final RestrictedTableDefinition def = stu.getTable()
//				.getRestrictTable(dataset, dsTable.getName());
//		if (def != null)
//			action.setTableRestriction(def);
		this.issueAction(action);
		
	}

	private boolean doJoinTable(final SourceSchema templateSchema,
			final String schemaPartition, final String schemaPrefix, final Mart dataset,
			final DatasetTable dsTable, final JoinTable ljtu,
			final Relation firstJoinRel, final String previousTempTable,
			final String tempTable, final Set<DatasetColumn> droppedCols,
			final int bigness, final String finalCombinedName)
			throws SQLException, ListenerException, PartitionException {

		// Left join whenever we have a double-level partition table
		// on the dataset, or whenever main/sc table + not alternative join,
		// or dimension table + alternative join.
		boolean useLeftJoin = dsTable.getType().equals(
				DatasetTableType.MAIN) || dsTable.getType().equals(DatasetTableType.MAIN_SUBCLASS);
		boolean requiresFinalLeftJoin = !useLeftJoin;
		final Join action = new Join(this.datasetSchemaName,
				finalCombinedName);
		action.setLeftJoin(useLeftJoin);
		action.setBigTable(bigness);
		action.setSchemaPrefix(schemaPrefix);


		{
			// If this is first relation after select table
			// (note first relation, not first join) then apply
			// next row to any subdiv table present.
			// Use a test to see if this is the first relation
			// after the select (regardless of how many times this
			// relation has been seen).
			final boolean nextRow = firstJoinRel.equals(ljtu
					.getSchemaRelation());
/*			if (nextRow)
				((PartitionColumn) pta.getPartitionTable().getColumns()
						.get(
								((PartitionAppliedRow) pta
										.getPartitionAppliedRows().get(1))
										.getPartitionCol()))
						.getPartitionTable().nextRow();
			// For all relations, if this is the one
			// that some subdiv partition applies to, then apply it.
			// This is a join, so we look up row by relation.
			final PartitionAppliedRow prow = pta
					.getAppliedRowForRelation(ljtu.getSchemaRelation());
			// It might not have one after all.
			if (prow != null) {
				// Look up the table that the naming column is on. It
				// will be a subtable which needs initialising on the
				// first pass, and next rowing on all passes.
				final PartitionColumn pcol = (PartitionColumn) pta
						.getPartitionTable().getColumns().get(
								prow.getPartitionCol());
				final PartitionTable ptbl = pcol.getPartitionTable();
				// For each of the getNewColumnNameMap cols that are in the
				// current ptable application, add a restriction for that
				// col using current ptable column value.
				for (final Iterator i = ljtu.getNewColumnNameMap()
						.entrySet().iterator(); i.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final DataSetColumn dsCol = (DataSetColumn) entry
							.getValue();
					// Only apply this to the dsCol which matches
					// the partition row's ds col.
					if (dsCol.existsForPartition(schemaPrefix)
							&& dsCol.getName().split("\\.")[3].equals(prow
									.getRootDataSetCol().split("\\.")[3])) {
						// Apply restriction.
						action.getPartitionRestrictions().put(
								((Column) entry.getKey()).getName(),
								pcol.getRawValueForRow(ptbl.currentRow()));
						// Make this an inner join if we are NOT dealing
						// with a two-level dataset partition.
						if (!(dsTable.getType().equals(
								DataSetTableType.MAIN)
								&& dsPta != null && dsPta
								.getPartitionAppliedRows().size() > 1)
								&& !useLeftJoin) {
							// We'll need a final left join.
							requiresFinalLeftJoin = true;
							useLeftJoin = false;
							action.setLeftJoin(false);
						}
					}
				}
			}*/ 
		}

		// Make sure that we use the same partition on the RHS
		// if it exists, otherwise use the default partition.
		String rightSchema = null;
		Boolean rightFromSourceSchema = null;	// must distinguish for rdbms other than mysql (for fully qualified names)
		if (ljtu.getTable() instanceof DatasetTable) {
			rightSchema = this.datasetSchemaName.getTargetSchemaName();
			rightFromSourceSchema = false;
		} else {
			rightSchema = ljtu.getTable().getSchema().getJdbcLinkObject().getSchemaName();
			if (null!=rightSchema && !this.datasetSchemaName.getSourceSchemaName().equals(rightSchema)) {
				throw new BioMartError();
			}
			rightFromSourceSchema = true; 	
		}
		if (rightSchema == null) {
			droppedCols.addAll(ljtu.getNewColumnNameMap().values());
			return false;
		}

		final String rightTable = ljtu.getTable() instanceof DatasetTable ? this
				.getFinalName(schemaPrefix,
						(DatasetTable) ljtu.getTable())
				: ljtu.getTable().getName();
		final List<String> leftJoinCols = new ArrayList<String>();
		final List<String> rightJoinCols = new ArrayList<String>();
		for (int i = 0; i < ljtu.getSchemaRelation().getOtherKey(
				ljtu.getSchemaSourceKey()).getColumns().size(); i++) {
			final Column rightCol = ljtu.getSchemaRelation().getOtherKey(
					ljtu.getSchemaSourceKey()).getColumns().get(i);
			if (ljtu.getTable() instanceof DatasetTable)
				rightJoinCols.add(((DatasetColumn) ljtu
						.getSchemaSourceKey().getColumns().get(i))
						.getName());
			else
				rightJoinCols.add(rightCol.getName());
		}
		final Map<String,String> selectCols = new HashMap<String,String>();
		// Populate vars.
		for (final Iterator<DatasetColumn> k = ljtu.getSourceDataSetColumns().iterator(); k
				.hasNext();) {
			final String joinCol = k.next().getName();
			if (droppedCols.contains(joinCol)) {
				droppedCols.addAll(ljtu.getNewColumnNameMap().values());
				return false;
			} else
				leftJoinCols.add(joinCol);
		}
		for (final Iterator<Map.Entry<Column, DatasetColumn>> k = ljtu.getNewColumnNameMap().entrySet()
				.iterator(); k.hasNext();) {
			final Map.Entry<Column, DatasetColumn> entry = k.next();
			final DatasetColumn col = (DatasetColumn) entry.getValue();
			if (col.inPartition(schemaPrefix)) {
				if (entry.getKey() instanceof DatasetColumn)
					selectCols.put(entry.getKey()
							.getName(), col.getName());
				else
					selectCols.put(((Column) entry.getKey()).getName(), col
							.getName());
			}
		}
		// Index the left-hand side of the join.
		final Index index = new Index(this.datasetSchemaName,
				finalCombinedName);
		index.setTable(previousTempTable);
		index.setColumns(leftJoinCols);
		this.issueAction(index);
		// Make the join.
		action.setLeftTable(previousTempTable);
		action.setRightFromSourceSchema(rightFromSourceSchema);
		action.setRightTable(rightTable);
		action.setLeftJoinColumns(leftJoinCols);
		action.setRightJoinColumns(rightJoinCols);
		action.setSelectColumns(selectCols);
		action.setResultTable(tempTable);

		// Table restriction.

		this.issueAction(action);
		
		return requiresFinalLeftJoin;
	}

	private boolean doExpression(final String schemaPrefix, final Mart dataset,
			final DatasetTable dsTable,
			final String previousTempTable, final String tempTable,
			final Set droppedCols, final String finalCombinedName)
			throws ListenerException, PartitionException {

		// Some useful stuff.
		boolean useXTable = false;
		final String xTableName = tempTable + "X";

		// Work out what columns we can select in the first group.
		final Collection selectCols = new HashSet();
		for (final Iterator z = dsTable.getColumnList().iterator(); z
				.hasNext();) {
			final DatasetColumn col = (DatasetColumn) z.next();
			final String colName = col.getName();
			if (col.inPartition(schemaPrefix)
					&& !droppedCols.contains(colName))
				selectCols.add(colName);
		}
		// Add to selectCols all the has columns for this table.
		final Collection hasCols = dataset.getDatasetOptimiserType()
				.isTable() ? null : (Collection) this.uniqueOptCols
				.get(dsTable);
		if (hasCols != null)
			selectCols.addAll(hasCols);


		return useXTable;
	}

	
	private void doParentLeftJoin(final String schemaPrefix,
			final Mart dataset,
			final DatasetTable dsTable, final String finalCombinedName,
			final String previousTempTable, final String tempTable,
			final Set droppedCols, final int bigness)
			throws ListenerException, PartitionException {
		// Work out the parent table.
		final DatasetTable parent = dsTable.getParentMainTable();
		// Work out what columns to take from each side.
		final List leftJoinCols = new ArrayList();
		final List leftSelectCols = leftJoinCols;
		final List rightJoinCols = leftJoinCols;
		final List rightSelectCols = new ArrayList();
		for (int x = 0; x < parent.getPrimaryKey().getColumns().size(); x++)
			leftJoinCols.add(((DatasetColumn) parent.getPrimaryKey()
					.getColumns().get(x)).getName());
		for (final Iterator x = dsTable.getColumnList().iterator(); x
				.hasNext();) {
			final DatasetColumn col = (DatasetColumn) x.next();
			if (col.inPartition(schemaPrefix))
				rightSelectCols.add(col.getName());
		}
		rightSelectCols.removeAll(rightJoinCols);
		rightSelectCols.removeAll(droppedCols);
		// Add to rightSelectCols all the has columns for this table.
		final Collection hasCols = dataset.getDatasetOptimiserType()
				.isTable() ? null : (Collection) this.uniqueOptCols
				.get(dsTable);
		if (hasCols != null)
			rightSelectCols.addAll(hasCols);
		// Index the left-hand side of the join.
		final Index index = new Index(this.datasetSchemaName,
				finalCombinedName);
		index.setTable(previousTempTable);
		index.setColumns(leftJoinCols);
		this.issueAction(index);
		// Make the join.
		final LeftJoin action = new LeftJoin(this.datasetSchemaName,
				finalCombinedName);
		action.setLeftTable(this.getFinalName(schemaPrefix, 
				parent));
		//action.setRightSchema(this.martBuilderCatalog.getTargetSchemaName());	always the target schema anyway
		action.setRightTable(previousTempTable);
		action.setLeftJoinColumns(leftJoinCols);
		action.setRightJoinColumns(rightJoinCols);
		action.setLeftSelectColumns(leftSelectCols);
		action.setRightSelectColumns(rightSelectCols);
		action.setResultTable(tempTable);
		action.setBigTable(bigness);
		this.issueAction(action);
		// Drop the old one.
		final Drop drop = new Drop(this.datasetSchemaName,
				finalCombinedName);
		drop.setTable(previousTempTable);
		this.issueAction(drop);
	}

	private void issueAction(final MartConstructorAction action)
			throws ListenerException {
		// Execute the action.
		this.statusMessage = action.getStatusMessage();
		this.issueListenerEvent(MartConstructorListener.ACTION_EVENT, null, action);
	}

	private String getOptimiserTableName(
			final String schemaPartitionPrefix,
			final DatasetTable dsTable, final DatasetOptimiserType oType)
			throws PartitionException {
		final StringBuffer finalName = new StringBuffer();
		if (!McUtils.isStringEmpty(schemaPartitionPrefix)) {
			finalName.append(schemaPartitionPrefix);
			finalName.append(Resources.get("tablenameSubSep"));
		}
		if("1".equals(Settings.getProperty("nameconvention")) && dsTable.getType().equals(DatasetTableType.MAIN)) {
			finalName.append(dsTable.getMart().getName());
			finalName.append(Resources.get("tablenameSep"));
		}
		finalName.append(dsTable.getName());
		if (oType.equals(DatasetOptimiserType.TABLE_INHERIT)) {
			finalName.append(Resources.get("tablenameSubSep"));
			finalName.append(Resources.get("countTblPartition"));
			finalName.append(Resources.get("tablenameSep"));
			finalName.append(Resources.get("dimensionSuffix"));
		} else if (oType.equals(DatasetOptimiserType.TABLE_BOOL_INHERIT)) {
			finalName.append(Resources.get("tablenameSubSep"));
			finalName.append(Resources.get("boolTblPartition"));
			finalName.append(Resources.get("tablenameSep"));
			finalName.append(Resources.get("dimensionSuffix"));
		} else if (dsTable.getType().equals(DatasetTableType.MAIN)) {
			if("1".equals(Settings.getProperty("nameconvention"))) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("mainSuffix"));
			}
		} else if (dsTable.getType().equals(DatasetTableType.MAIN_SUBCLASS)) {
			if("1".equals(Settings.getProperty("nameconvention"))) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("subclassSuffix"));
			}
		} else if (dsTable.getType().equals(DatasetTableType.DIMENSION)) {
			if("1".equals(Settings.getProperty("nameconvention"))) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("dimensionSuffix"));
			}
		} else
			throw new BioMartError();
		final String name = finalName.toString().replaceAll("\\W+", "");
		return name;
	}

	private String getOptimiserColumnName(
			final DatasetTable parent, final DatasetTable dsTable,
			final DatasetOptimiserType oType,
			final DatasetColumn restrictCol, final String restrictValue,
			final boolean prefix, final boolean suffix)
			throws PartitionException {
		// Set up storage for unique names if required.
		if (!this.uniqueOptCols.containsKey(parent))
			this.uniqueOptCols.put(parent, new HashSet<String>());
		if (!this.indexOptCols.containsKey(parent))
			this.indexOptCols.put(parent, new HashSet());
		// Make a unique name.
		int counter = -1;
		String name;
		do {
			final StringBuffer sb = new StringBuffer();
			if (prefix) {
				sb.append(dsTable.getName());
				if (++counter > 0) {
					sb.append(Resources.get("tablenameSubSep"));
					sb.append("" + counter);
				}
			}
			if (restrictCol != null) {
				if (prefix) {
					sb.append(Resources.get("tablenameSubSep"));
					sb.append(restrictCol.getName());
					sb.append(Resources.get("tablenameSubSep"));
				}
				sb.append(restrictValue);
				if (!prefix && ++counter > 0) {
					sb.append(Resources.get("tablenameSubSep"));
					sb.append("" + counter);
				}
			}
			if (suffix) {
				sb.append(Resources.get("tablenameSubSep"));
				sb.append(oType.isBool() ? Resources.get("boolColSuffix")
						: Resources.get("countColSuffix"));
			}
			name = sb.toString();
		} while (((Collection) this.uniqueOptCols.get(parent))
				.contains(name));
		name = name.replaceAll("\\W+", "");
		// UC/LC/Mixed?
		// Store the name above in the unique list for the parent.
		((Collection) this.uniqueOptCols.get(parent)).add(name);
		return name;
	}

	private String getFinalName(final String schemaPartitionPrefix,
			final DatasetTable dsTable) throws PartitionException {
		final Object[] finalNameCacheKey = new Object[] {
				schemaPartitionPrefix, dsTable };
		if (dsTable.getType().equals(DatasetTableType.DIMENSION)
				&& this.finalNameCache.containsKey(finalNameCacheKey))
			return (String) this.finalNameCache.get(finalNameCacheKey);
		final StringBuffer finalName = new StringBuffer();
		if (!McUtils.isStringEmpty(schemaPartitionPrefix)) {
			finalName.append(schemaPartitionPrefix);
			finalName.append(Resources.get("tablenameSubSep"));
		}
		if("1".equals(Settings.getProperty("nameconvention")) && dsTable.getType().equals(DatasetTableType.MAIN)) {
			finalName.append(dsTable.getMart().getName());	
			finalName.append(Resources.get("tablenameSep"));
		}
		finalName.append(dsTable.getName());
		String name = null;
		if("1".equals(Settings.getProperty("nameconvention"))) {
			String finalSuffix;
			if (dsTable.getType().equals(DatasetTableType.MAIN))
				finalSuffix = Resources.get("mainSuffix");
			else if (dsTable.getType().equals(DatasetTableType.MAIN_SUBCLASS))
				finalSuffix = Resources.get("subclassSuffix");
			else if (dsTable.getType().equals(DatasetTableType.DIMENSION)) {			
				finalSuffix = Resources.get("dimensionSuffix");
			} else
				throw new BioMartError();
			finalName.append(Resources.get("tablenameSep"));
			finalName.append(finalSuffix);
		
			// Remove any non-[char/number/underscore] symbols.
			name = finalName.toString().replaceAll("\\W+", "");
	
			if (dsTable.getType().equals(DatasetTableType.DIMENSION)) {
				final String firstBit = name.substring(0, name.length()
						- (Resources.get("tablenameSep") + finalSuffix)
								.length());
				final String lastBit = name.substring(name.length()
						- (Resources.get("tablenameSep") + finalSuffix)
								.length());
				int i = 1;
				final DecimalFormat formatter = new DecimalFormat("000");
				while (this.finalNameCache.containsValue(name))
					// Clash! Rename the table to avoid it.
					name = firstBit + Resources.get("tablenameSubSep")
							+ Resources.get("clashSuffix")
							+ formatter.format(i++) + lastBit;
				this.finalNameCache.put(finalNameCacheKey, name);
			}
		}else
			name = finalName.toString().replaceAll("\\W+", "");
		
		return name;
	}
	
	private void doOptimiseTable(final String schemaPrefix,
			final Mart dataset,
			final DatasetTable dsTable, final DatasetOptimiserType oType,
			final boolean createTable, final int bigness,
			final String finalCombinedName) throws ListenerException,
			PartitionException {
		if (createTable) {
			// Tables are same name, but use 'bool' or 'count'
			// instead of 'main'
			final String optTable = this.getOptimiserTableName(
					schemaPrefix, dsTable, dataset
							.getDatasetOptimiserType());
			// The key cols are those from the primary key.
			final List<String> keyCols = new ArrayList<String>();
			for (int y = 0; y < dsTable.getPrimaryKey().getColumns().size(); y++)
				keyCols.add(((DatasetColumn) dsTable.getPrimaryKey()
						.getColumns().get(y)).getName());

			// Create the table by selecting the pk.
			final CreateOptimiser create = new CreateOptimiser(
					this.datasetSchemaName, finalCombinedName);
			create.setKeyColumns(keyCols);
			create.setOptTableName(optTable);
			create.setBigTable(bigness);
			this.issueAction(create);

			// Index the pk on the new table.
			final Index index = new Index(this.datasetSchemaName,
					finalCombinedName);
			index.setTable(optTable);
			index.setColumns(keyCols);
			this.issueAction(index);
		}
		if (!dsTable.getType().equals(DatasetTableType.MAIN)) {
			// Work out the dimension/subclass parent.
			final DatasetTable parent = dsTable.getParentMainTable();
			// Set up the column on the dimension parent.
			String optTable = this
					.getOptimiserTableName(schemaPrefix, 
							parent, dataset.getDatasetOptimiserType());

			// Key columns are primary key cols from parent.
			// Do a left-join update. We're looking for rows
			// where at least one child non-key col is non-null.
			final List<String> keyCols = new ArrayList<String>();
			for (int y = 0; y < parent.getPrimaryKey().getColumns().size(); y++)
				keyCols.add(((DatasetColumn) parent.getPrimaryKey()
						.getColumns().get(y)).getName());

			// Work out what to count.
			final List<String> nonNullCols = new ArrayList<String>();
			for (final Iterator<Column> y = dsTable.getColumnList()
					.iterator(); y.hasNext();) {
				final DatasetColumn col = (DatasetColumn) y.next();
				// We won't select masked cols as they won't be in
				// the final table, and we won't select expression
				// columns as they can genuinely be null.
				if (col.inPartition(schemaPrefix) )
					nonNullCols.add(col.getName());
			}
			nonNullCols.removeAll(keyCols);

			// Loop rest of this block once per unique value
			// in column, using SQL to get those values, and
			// inserting them into each optimiser column name.
			final Map restrictCols = new HashMap();
			for (final Iterator<Column> i = dsTable.getColumnList()
					.iterator(); i.hasNext();) {
				final DatasetColumn cand = (DatasetColumn) i.next();
				if (cand.getSplitOptimiserColumn() != null)
					restrictCols.put(cand, cand.getSplitOptimiserColumn());
			}
			if (restrictCols.isEmpty())
				restrictCols.put("", null);
			for (final Iterator i = restrictCols.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				DatasetColumn restrictCol = entry.getKey().equals("") ? null
						: (DatasetColumn) entry.getKey();
				final SplitOptimiserColumnDef splitOptDef = (SplitOptimiserColumnDef) entry
						.getValue();
				final DatasetColumn splitContentCol = splitOptDef == null ? null
						: (DatasetColumn) dsTable.getColumnByName(
								splitOptDef.getContentCol());
				final Collection subNonNullCols = new ArrayList(nonNullCols);
				final List restrictValues = new ArrayList();
				if (restrictCol != null) {
					subNonNullCols.remove(restrictCol.getName());
					// Disambiguate inherited columns.
					while (restrictCol instanceof InheritedColum)
						restrictCol = ((InheritedColum) restrictCol)
								.getInheritedColumn();
					// Can only restrict on wrapped columns.
					if (restrictCol instanceof WrappedColumn) {
						// Populate restrict values.
						final Column dataCol = ((WrappedColumn) restrictCol)
								.getSourceColumn();
/*						try {
							restrictValues.addAll(dataCol.getTable()
									.getSchema().getUniqueValues(
											schemaPrefix, dataCol));
						} catch (final SQLException e) {
							throw new PartitionException(e);
						}*/
					}
				} else
					restrictValues.add(null);
				for (final Iterator j = restrictValues.iterator(); j
						.hasNext();) {
					final String restrictValue = (String) j.next();
					// Columns are dimension table names with '_bool' or
					// '_count' appended.
					final String optCol = this.getOptimiserColumnName(
							parent, dsTable, oType,
							restrictCol, restrictValue, splitOptDef == null
									|| splitOptDef.isPrefix(),
							splitOptDef == null || splitOptDef.isSuffix());

					// Do the bool/count update.
					final UpdateOptimiser update = new UpdateOptimiser(
							this.datasetSchemaName, finalCombinedName);
					update.setKeyColumns(keyCols);
					update.setNonNullColumns(subNonNullCols);
					update.setSourceTableName(finalCombinedName);
					update.setOptTableName(optTable);
					update.setOptColumnName(optCol);
					update.setCountNotBool(!oType.isBool());
					update.setNullNotZero(oType.isUseNull());
					update.setOptRestrictColumn(restrictCol == null ? null
							: restrictCol.getName());
					update.setOptRestrictValue(restrictValue);
					update
							.setValueColumnName(splitContentCol == null ? null
									: splitContentCol.getName());
					update
							.setValueColumnSeparator(splitContentCol == null ? null
									: splitOptDef.getSeparator());
					update.setValueColumnSize(splitContentCol == null ? 255
							: splitOptDef.getSize());
					this.issueAction(update);

					// Store the reference for later.
					if (!this.uniqueOptCols.containsKey(parent))
						this.uniqueOptCols.put(parent, new HashSet());
					((Collection) this.uniqueOptCols.get(parent))
							.add(optCol);
					if (!this.indexOptCols.containsKey(parent))
						this.indexOptCols.put(parent, new HashSet());
					if (!dsTable.isSkipIndexOptimiser()) {
						((Collection) this.indexOptCols.get(parent))
								.add(optCol);
						// Index the column.
						final Index index = new Index(
								this.datasetSchemaName, finalCombinedName);
						index.setTable(optTable);
						index.setColumns(Collections.singletonList(optCol));
						this.issueAction(index);
					}

					// Subclass tables need the column copied down if
					// they are column based.
					if (dsTable.getType().equals(
							DatasetTableType.MAIN_SUBCLASS)
							&& !oType.isTable()) {
						// Set up the column on the subclass itself. Because
						// we are not using tables, this will always be the
						// finished name of the subclass table itself.
						final String scOptTable = this
								.getOptimiserTableName(schemaPrefix,  dsTable, dataset
												.getDatasetOptimiserType());

						// If this is a subclass table, copy the optimiser
						// column down to us as well and add it to our own
						// set.
						final CopyOptimiser copy = new CopyOptimiser(
								this.datasetSchemaName, finalCombinedName);
						copy.setKeyColumns(keyCols);
						copy.setOptTableName(scOptTable);
						copy.setOptColumnName(optCol);
						copy.setParentOptTableName(optTable);
						this.issueAction(copy);

						// Store the reference for later.
						if (!this.uniqueOptCols.containsKey(dsTable))
							this.uniqueOptCols.put(dsTable, new HashSet());
						((Collection) this.uniqueOptCols.get(dsTable))
								.add(optCol);
						if (!this.indexOptCols.containsKey(dsTable))
							this.indexOptCols.put(dsTable, new HashSet());
						if (!dsTable.isSkipIndexOptimiser()) {
							((Collection) this.indexOptCols.get(dsTable))
									.add(optCol);
							// Index the column.
							final Index index = new Index(
									this.datasetSchemaName,
									finalCombinedName);
							index.setTable(scOptTable);
							index.setColumns(Collections
									.singletonList(optCol));
							this.issueAction(index);
						}
					}
				}
			}
		}
	}


	private void issueListenerEvent(final int event)
			throws ListenerException {
		this.issueListenerEvent(event, null);
	}

	private void issueListenerEvent(final int event, final Object data)
			throws ListenerException {
		this.issueListenerEvent(event, data, null);
	}

	private void issueListenerEvent(final int event, final Object data,
			final MartConstructorAction action) throws ListenerException {
		Log.debug("Event issued: event:" + event + " data:" + data
				+ " action:" + action);
		for (final Iterator<MartConstructorListener> i = this.martConstructorListeners.iterator(); i
				.hasNext();)
			((MartConstructorListener) i.next())
					.martConstructorEventOccurred(event, data, action);
	}

	public void addMartConstructorListener(
			final MartConstructorListener listener) {
		Log.debug("Listener added to constructor runnable");
		this.martConstructorListeners.add(listener);
	}

	public void cancel() {
		Log.debug("Constructor runnable cancelled");
		this.cancelled = true;
	}

	public Exception getFailureException() {
		return this.failure;
	}

	public int getPercentComplete() {
		return (int) this.percentComplete;
	}

	public String getStatusMessage() {
		return this.statusMessage;
	}

	public void run() {
		try {
			// Begin.
			Log.debug("Construction started");
			this.issueListenerEvent(MartConstructorListener.CONSTRUCTION_STARTED);

			// Work out how many datasets we have.
			final int totalDataSetCount = 1;
				
			try {
				this.makeActionsForDataset(datasets, totalDataSetCount);
			} catch (final Throwable t) {
				throw t;
			}
			
			this.issueListenerEvent(MartConstructorListener.CONSTRUCTION_ENDED);
			Log.debug("Construction ended");
		} catch (final ConstructorException e) {
			// This is so the users see a nice message straight away.
			this.failure = e;
		} catch (final Throwable t) {
			this.failure = new ConstructorException(t);
		} finally {
			this.alive = false;
		}
		if (null!=failure) {	//TODO handle elsewhere? (from calling method?)
			JOptionPane.showMessageDialog(null, "An error occured while building the mart (see logs for more information)");
			Log.error("mart builder error");
			failure.printStackTrace();
		}
	}

	public boolean isAlive() {
		return this.alive;
	}

	public void finalize() {
		this.alive = false;
	}
}
