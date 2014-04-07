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

package org.biomart.configurator.controller;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.apache.commons.lang.WordUtils;
import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.MartConstructor.ConstructorRunnable;
import org.biomart.configurator.controller.dialects.McSQL;
import org.biomart.configurator.model.JoinTable;
import org.biomart.configurator.model.SelectFromTable;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.configurator.model.WrappedColumn;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.model.object.DimensionQueueObject;
import org.biomart.configurator.model.object.NormalQueueObject;
import org.biomart.configurator.model.object.SubclassQueueObject;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.utils.type.ComponentStatus;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.ForeignKey;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.objects.PrimaryKey;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.RelationSource;
import org.biomart.objects.objects.RelationTarget;
import org.biomart.objects.objects.SourceColumn;
import org.biomart.objects.objects.SourceContainers;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.SourceTable;
import org.biomart.objects.objects.Table;
import org.biomart.objects.portal.Portal;
import org.jdom.Document;
import org.jdom.Element;

public class MartController {

    private static MartController instance;
    private boolean changed = false;

    public static MartController getInstance() {
        if (instance == null)
            instance = new MartController();
        return instance;
    }

    private MartController() {

    }

    private void processParentDatasetTable(TransformationUnit parentTU, DatasetTable parentDsTable,
            DatasetTable dsTable, DatasetTableType type, List<DatasetColumn> sourceDSCols,
            Set<DatasetColumn> unusedCols, Collection<ForeignKey> unusedFKs, Map<String, Integer> uniqueBases) {
        parentTU = new SelectFromTable(parentDsTable);
        dsTable.addTransformationUnit(parentTU);

        // Make a list to hold the child table's FK cols.
        final List<Column> dsTableFKCols = new ArrayList<Column>();

        // Get the primary key of the parent DS table.
        final PrimaryKey parentDSTablePK = parentDsTable.getPrimaryKey();

        // Loop over each column in the parent table. If this is
        // a subclass table, add it. If it is a dimension table,
        // only add it if it is in the PK or is in the first underlying
        // key. In either case, if it is in the PK, add it both to the
        // child PK and the child FK. Also inherit if it is involved
        // in a restriction on the very first join.
        for (final Iterator<Column> i = parentDsTable.getColumnList().iterator(); i.hasNext();) {
            final DatasetColumn parentDSCol = (DatasetColumn) i.next();
            boolean inRelationRestriction = false;
            // If this is not a subclass table, we need to filter columns.
            if (!type.equals(DatasetTableType.MAIN_SUBCLASS)) {
                // Skip columns that are not in the primary key.
                final boolean inPK = parentDSTablePK.getColumns().contains(parentDSCol);
                final boolean inSourceKey = sourceDSCols.contains(parentDSCol);
                // If the column is in a restricted relation
                // on the source relation, we need to inherit it.
                // Inherit it?
                if (!inPK && !inSourceKey && !inRelationRestriction)
                    continue;
            }
            // Only unfiltered columns reach this point. Create a copy of
            // the column.
            final InheritedColum dsCol;
            if (!dsTable.getColumnNames().contains(parentDSCol.getName())) {
                WrappedColumn tmpDsc = (WrappedColumn) parentDsTable.getColumnByName(parentDSCol.getName());
                // WrappedColumn tmpDsc = new
                // WrappedColumn(parentDSCol.getSourceColumn(),parentDSCol.getName(),dsTable);
                dsCol = new InheritedColum(dsTable, tmpDsc);
                dsTable.addColumn(dsCol);
                // If any other col has modified name same as
                // inherited col's modified name, then rename the
                // other column to avoid clash.
                for (final Iterator<Column> j = dsTable.getColumnList().iterator(); j.hasNext();) {
                    final DatasetColumn cand = (DatasetColumn) j.next();
                    if (!(cand instanceof InheritedColum) && cand.getName().equals(dsCol.getName())) {
                        final DatasetColumn renameCol = inRelationRestriction
                                ? cand : (DatasetColumn) dsCol;
                        if (renameCol.getName().endsWith(Resources.get("keySuffix"))) {
                            renameCol.setName(renameCol.getName().substring(0,
                                    renameCol.getName().indexOf(Resources.get("keySuffix")))
                                    + "_clash" + Resources.get("keySuffix"));
                            renameCol.setInternalName(renameCol.getInternalName().substring(0,
                                    renameCol.getInternalName().indexOf(Resources.get("keySuffix")))
                                    + "_clash" + Resources.get("keySuffix"));
                        } else {
                            renameCol.setName(renameCol.getName() + "_clash");
                            renameCol.setInternalName(renameCol.getInternalName() + "_clash");
                        }
                    }
                }
            } else
                dsCol = (InheritedColum) dsTable.getColumnByName(parentDSCol.getName());
            unusedCols.remove(dsCol);
            parentTU.getNewColumnNameMap().put(parentDSCol, (DatasetColumn) dsCol);
            dsCol.setTransformationUnit(parentTU);
            uniqueBases.put(parentDSCol.getName(), new Integer(0));
            // Add the column to the child's FK, but only if it was in
            // the parent PK.
            if (parentDSTablePK.getColumns().contains(parentDSCol))
                dsTableFKCols.add(dsCol);
        }

        try {
            // Create the child FK.
            List<Column> columns = new ArrayList<Column>();
            for (Column cc : dsTableFKCols) {
                columns.add(cc);
            }
            ForeignKey fkObject = new ForeignKey(columns);

            // KeyController dsTableFK = new KeyController(fkObject);

            // Create only if not already exists.
            for (final Iterator<ForeignKey> i = dsTable.getForeignKeys().iterator(); i.hasNext();) {
                final ForeignKey cand = i.next();
                if (cand.equals(fkObject))
                    fkObject = cand;
            }
            if (!dsTable.getForeignKeys().contains(fkObject)) {
                dsTable.getForeignKeys().add(fkObject);
                // dsTable.getForeignKeys().add(dsTableFK);
                // Link the child FK to the parent PK.
                new RelationTarget(parentDSTablePK, fkObject, Cardinality.MANY_A);
                // parentDSTablePK.getObject().addRelation(relation);
                // dsTableFK.getObject().addRelation(relation);
            }
            unusedFKs.remove(fkObject);
        } catch (final Throwable t) {
            throw new BioMartError(t);
        }

        // Copy all parent FKs and add to child, but WITHOUT
        // relations. Subclasses only!
        if (type.equals(DatasetTableType.MAIN_SUBCLASS))
            for (final Iterator<ForeignKey> i = parentDsTable.getForeignKeys().iterator(); i.hasNext();) {
                final ForeignKey parentFK = i.next();
                final List<Column> childFKCols = new ArrayList<Column>();
                for (int j = 0; j < parentFK.getColumns().size(); j++)
                    childFKCols.add(parentTU.getNewColumnNameMap().get(parentFK.getColumns().get(j)));
                try {
                    // Create the child FK.
                    List<Column> columns = new ArrayList<Column>();
                    for (Column cc : childFKCols) {
                        columns.add(cc);
                    }
                    ForeignKey fkObject = new ForeignKey(columns);

                    // KeyController dsTableFK = (KeyController)(fkObject.getWrapper());

                    // Create only if not already exists.
                    for (final Iterator<ForeignKey> j = dsTable.getForeignKeys().iterator(); j.hasNext();) {
                        final ForeignKey cand = j.next();
                        if (cand.equals(fkObject))
                            fkObject = cand;
                    }
                    if (dsTable.getForeignKeys().contains(fkObject))
                        dsTable.getForeignKeys().add(fkObject);
                    // dsTable.getForeignKeys().add(dsTableFK);
                    unusedFKs.remove(fkObject);
                } catch (final Throwable t) {
                    throw new BioMartError(t);
                }
            }
    }

    /**
     * This internal method builds a dataset table based around a real table. It works out what dimensions and
     * subclasses are required then recurses to create those too.
     * 
     * @param type
     *            the type of table to build.
     * @param parentDSTable
     *            the table which this dataset table creates a foreign key to. If this is to be a subclass table, it
     *            will inherit all columns from this parent table.
     * @param realSourceTable
     *            the real table in a schema from where the transformation to create this dataset table will begin.
     * @param skippedMainSourceTables
     *            the main tables to skip when building subclasses and dimensions.
     * @param sourceRelation
     *            the real relation in a schema which was followed in order to discover that this dataset table should
     *            be created. For instance, it could be the 1:M relation between the realTable parameter of this call,
     *            and the realTable parameter of the main table call to this method.
     */
    private void generateDatasetTable(final Mart mart, final DatasetTableType type, final DatasetTable parentDsTable,
            final SourceTable realSourceTable, final List<SourceTable> skippedMainSourceTables,
            final List<DatasetColumn> sourceDSCols, Relation sourceRelation, final Set<DatasetTable> unusedDsTables) {
        Log.debug("Creating dataset table for " + realSourceTable + " with parent relation " + sourceRelation
                + " as a " + type);
        // Create the empty dataset table. Use a unique prefix
        // to prevent naming clashes.
        String prefix = "";
        if (parentDsTable != null) {
            final String parts[] = parentDsTable.getName().split(Resources.get("tablenameSep")); // __
            prefix = parts[parts.length - 1] + Resources.get("tablenameSep");
        }
        String fullName = prefix + realSourceTable.getName();

        // Loop over all tables with similar names to check for reuse.
        DatasetTable dsTable = null;
        for (DatasetTable entry : mart.getDatasetTables()) {
            final String testName = entry.getName();
            // If find table starting with same letters, check to see
            // if can reuse, and update fullName to match it.
            if (testName.equals(fullName) || testName.startsWith(fullName))
                if (entry.getFocusRelation() == null || (entry.getFocusRelation().equals(sourceRelation))) {
                    fullName = testName;
                    dsTable = entry;
                    dsTable.setType(type); // Just to make sure.
                    unusedDsTables.remove(dsTable);
                    dsTable.getTransformationUnits().clear();
                    break;
                }
        }

        // If still not found anything after all tables checked,
        // create new table.
        if (dsTable == null) {
            dsTable = new DatasetTable(mart, fullName, type);
            dsTable.setFocusRelation((RelationSource) sourceRelation);
            mart.addTable(dsTable);
        }

        // Create the three relation-table pair queues we will work with. The
        // normal queue holds pairs of relations and tables. The other two hold
        // a list of relations only, the tables being the FK ends of each
        // relation. The normal queue has a third object associated with each
        // entry, which specifies whether to treat the 1:M relations from
        // the merged table as dimensions or not.
        final List<NormalQueueObject> normalQ = new ArrayList<NormalQueueObject>();
        final List<SubclassQueueObject> subclassQ = new ArrayList<SubclassQueueObject>();
        final List<DimensionQueueObject> dimensionQ = new ArrayList<DimensionQueueObject>();

        // Set up a list to hold columns for this table's primary key.
        final List<Column> dsTablePKCols = new ArrayList<Column>();

        // Make a list of existing columns and foreign keys.
        final Set<DatasetColumn> unusedDsCols = new HashSet<DatasetColumn>();
        for (Column column : dsTable.getColumnList()) {
            unusedDsCols.add((DatasetColumn) column);
        }
        final Collection<ForeignKey> unusedDsFKs = new HashSet<ForeignKey>(dsTable.getForeignKeys());

        // Make a map for unique column base names.
        final Map<String, Integer> uniqueBases = new HashMap<String, Integer>();

        // If the parent dataset table is not null, add columns from it
        // as appropriate. Dimension tables get just the PK, and an
        // FK linking them back. Subclass tables get all columns, plus
        // the PK with FK link, plus all the relations we followed to
        // get these columns.
        TransformationUnit parentTU = null;
        if (parentDsTable != null) {
            this.processParentDatasetTable(parentTU, parentDsTable, dsTable, type, sourceDSCols, unusedDsCols,
                    unusedDsFKs, uniqueBases);
        }

        final Map<SourceTable, Integer> tableTracker = new HashMap<SourceTable, Integer>();
        // Process the table. This operation will populate the initial
        // values in the normal, subclass and dimension queues. We only
        // want dimensions constructed if we are not already constructing
        // a dimension ourselves.
        this.processTable(mart, parentTU, dsTable, dsTablePKCols, realSourceTable, normalQ, subclassQ, dimensionQ,
                sourceDSCols, sourceRelation, !type.equals(DatasetTableType.DIMENSION), new ArrayList<String>(),
                new ArrayList<String>(), 0, unusedDsCols, uniqueBases, skippedMainSourceTables, tableTracker);

        // Process the normal queue. This merges tables into the dataset
        // table using the relation specified in each pair in the queue.
        // The third value is the dataset parent table columns to link from.
        // The fourth value of each entry in the queue determines whether or
        // not to continue making dimensions off each table in the queue.
        // The fifth value is the counter of how many times this relation has
        // been seen before.
        // The sixth value is a map of relation counts used to reach this point.
        for (int i = 0; i < normalQ.size(); i++) {
            NormalQueueObject nqObject = normalQ.get(i);
            final Relation mergeSourceRelation = nqObject.getRelation();
            final List<DatasetColumn> newSourceDSCols = nqObject.getColumnList();
            final Table mergeTable = nqObject.getTable();
            final TransformationUnit previousUnit = nqObject.getTransformationUnit();
            final boolean makeDimensions = nqObject.isMakeDimension();
            final List<String> nameCols = nqObject.getNextNameColumns();
            final List<String> nameColSuffixes = nqObject.getNextNameColSuffixes();
            this.processTable(mart, previousUnit, dsTable, dsTablePKCols, (SourceTable) mergeTable, normalQ, subclassQ,
                    dimensionQ, newSourceDSCols, mergeSourceRelation == null
                            ? null : mergeSourceRelation, makeDimensions, nameCols, nameColSuffixes, i + 1,
                    unusedDsCols, uniqueBases, skippedMainSourceTables, tableTracker);
        }

        // Create the primary key on this table, but only if it has one.
        // Don't bother for dimensions.
        if (!dsTablePKCols.isEmpty() && !dsTable.getType().equals(DatasetTableType.DIMENSION)) {
            // Create the key.
            List<Column> columns = new ArrayList<Column>();
            for (Column cc : dsTablePKCols) {
                columns.add(cc);
            }
            PrimaryKey pkObject = new PrimaryKey(columns);
            // KeyController pkc = new KeyController(pkObject);
            dsTable.setPrimaryKey(pkObject);
        } else
            dsTable.setPrimaryKey(null);

        // Drop unused columns and foreign keys.
        for (final Iterator<ForeignKey> i = unusedDsFKs.iterator(); i.hasNext();) {
            final ForeignKey fk = i.next();
            for (final Iterator<Relation> j = fk.getRelations().iterator(); j.hasNext();) {
                final Relation rel = j.next();
                rel.getFirstKey().removeRelation(rel);
                rel.getSecondKey().removeRelation(rel);
            }
            dsTable.getForeignKeys().remove(fk);
        }
        for (final Iterator<DatasetColumn> i = unusedDsCols.iterator(); i.hasNext();) {
            final DatasetColumn deadCol = i.next();
            dsTable.removeColumn(deadCol);
        }

        // Rename columns in keys to have _key suffixes, and
        // remove that suffix from all others.
        // TODO is _key done in processTable?
        /*
         * for (final Iterator<Column> i = dsTable.getColumnList().iterator(); i .hasNext();) { final DatasetColumn
         * dsCol = (DatasetColumn)i.next(); if (((DatasetColumn)dsCol).isKeyColumn() && !dsCol.getName().endsWith(
         * Resources.get("keySuffix"))) { dsCol.setName(dsCol.getName() + Resources.get("keySuffix"));
         * dsCol.setInternalName(dsCol.getInternalName() + Resources.get("keySuffix")); } else if
         * (!((DatasetColumn)dsCol).isKeyColumn() && dsCol.getName().endsWith( Resources.get("keySuffix"))) {
         * dsCol.setName(dsCol.getName().substring(0,dsCol.getName().indexOf(Resources.get("keySuffix"))));
         * dsCol.setInternalName
         * (dsCol.getInternalName().substring(0,dsCol.getInternalName().indexOf(Resources.get("keySuffix")))); } }
         */

        // Only dataset tables with primary keys can have subclasses
        // or dimensions.
        if (dsTable.getPrimaryKey() != null) {
            // Process the subclass relations of this table.
            for (SubclassQueueObject scqObject : subclassQ) {
                final List<DatasetColumn> newSourceDSCols = scqObject.getColumnList();
                final Relation subclassRelation = scqObject.getRelation();
                this.generateDatasetTable(mart, DatasetTableType.MAIN_SUBCLASS, dsTable, (SourceTable) subclassRelation
                        .getManyKey().getTable(), skippedMainSourceTables, newSourceDSCols, subclassRelation,
                        unusedDsTables);
            }

            // Process the dimension relations of this table. For 1:M it's easy.
            // For M:M, we have to work out which end is connected to the real
            // table, then process the table at the other end of the relation.
            // TODO do we have M:M
            for (DimensionQueueObject dqObject : dimensionQ) {
                final List<DatasetColumn> newSourceDSCols = dqObject.getDatasetColumnlist();
                final Relation dimensionRelation = dqObject.getRelation();
                if (dimensionRelation.isOneToMany())
                    this.generateDatasetTable(mart, DatasetTableType.DIMENSION, dsTable,
                            (SourceTable) dimensionRelation.getManyKey().getTable(), skippedMainSourceTables,
                            newSourceDSCols, dimensionRelation, unusedDsTables);
                else
                    System.err.println("check relation !!!");
            }
        }
    }

    /**
     * This method takes a real table and merges it into a dataset table. It does this by creating {@link WrappedColumn}
     * instances for each new column it finds in the table.
     * <p>
     * If a source relation was specified, columns in the key in the table that is part of that source relation are
     * ignored, else they'll get duplicated.
     * 
     * @param dsTable
     *            the dataset table we are constructing and should merge the columns into.
     * @param dsTablePKCols
     *            the primary key columns of that table. If we find we need to add to these, we should add to this list
     *            directly.
     * @param mergeTable
     *            the real table we are about to merge columns from.
     * @param normalQ
     *            the queue to add further real tables into that we find need merging into this same dataset table.
     * @param subclassQ
     *            the queue to add starting points for subclass tables that we find.
     * @param dimensionQ
     *            the queue to add starting points for dimension tables we find.
     * @param sourceRelation
     *            the real relation we followed to reach this table.
     * @param relationCount
     *            how many times we have left to follow each relation, so that we don't follow them too often.
     * @param makeDimensions
     *            <tt>true</tt> if we should add potential dimension tables to the dimension queue, <tt>false</tt> if we
     *            should just ignore them. This is useful for preventing dimensions from gaining dimensions of their
     *            own.
     * @param nameCols
     *            the list of partition columns to prefix the new dataset columns with.
     * @param queuePos
     *            this position in the queue to insert the next steps at.
     * @param skippedMainTables
     *            the main tables to skip when processing subclasses and dimensions.
     * @param mergeTheseRelationsInstead
     *            ignore all other rules and merge these ones, and don't fire dimensions on any of these either.
     */
    private void processTable(final Mart mart, final TransformationUnit previousUnit, final DatasetTable dsTable,
            final List<Column> dsTablePKCols, final SourceTable mergeTable, final List<NormalQueueObject> normalQ,
            final List<SubclassQueueObject> subclassQ, final List<DimensionQueueObject> dimensionQ,
            final List<DatasetColumn> sourceDsCols, final Relation sourceRelation, final boolean makeDimensions,
            final List<String> nameCols, final List<String> nameColSuffixes, int queuePos,
            final Set<DatasetColumn> unusedCols, final Map<String, Integer> uniqueBases,
            final List<SourceTable> skippedMainTables, Map<SourceTable, Integer> tableTracker) {
        Log.debug("Processing table " + mergeTable);

        // Remember the schema.
        mart.addSourceSchema(mergeTable.getSchema());
        // Don't ignore any keys by default.
        final Set<Column> ignoreCols = new HashSet<Column>();

        final TransformationUnit tu;
        int tableTrackerCount = 0;

        // Count the table.
        if (tableTracker.containsKey(mergeTable))
            tableTrackerCount = ((Integer) tableTracker.get(mergeTable)).intValue() + 1;
        tableTracker.put(mergeTable, new Integer(tableTrackerCount));

        // Did we get here via somewhere else?
        if (sourceRelation != null) {
            // Work out what key to ignore by working out at which end
            // of the relation we are.
            final Key ignoreKey = sourceRelation.getKeyForTable(mergeTable);
            ignoreCols.addAll(ignoreKey.getColumns());
            final Key mergeKey = sourceRelation.getOtherKey(ignoreKey);
            // Add the relation and key to the list that the table depends on.
            // This list is what defines the path required to construct
            // the DDL for this table.
            tu = new JoinTable(previousUnit, mergeTable, sourceRelation, sourceDsCols, mergeKey);

        } else
            tu = new SelectFromTable(mergeTable);
        // this.includedTables.add(mergeTable);
        // this.mart.getTableList().add((DatasetTable)mergeTable.getObject());
        // dsTable.includedTables.add(mergeTable);
        // dsTable.includedSchemas.add(((SourceTableController)mergeTable).getSchema());

        dsTable.addTransformationUnit(tu);

        // Work out the merge table's PK.
        final PrimaryKey mergeTablePK = mergeTable.getPrimaryKey();

        // We must merge only the first PK we come across, if this is
        // a main table, or the first PK we come across after the
        // inherited PK, if this is a subclass. Dimensions dont get
        // merged at all.
        boolean includeMergeTablePK = mergeTablePK != null
                && !mergeTablePK.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
                && !dsTable.getType().equals(DatasetTableType.DIMENSION);
        if (includeMergeTablePK && sourceRelation != null)
            // Only add further PK columns if the relation did NOT
            // involve our PK and was NOT 1:1.
            includeMergeTablePK = dsTablePKCols.isEmpty() && !sourceRelation.isOneToOne()
                    && !sourceRelation.getFirstKey().equals(mergeTablePK)
                    && !sourceRelation.getSecondKey().equals(mergeTablePK);

        // Make a list of all columns involved in keys on the merge table.
        // final Set<ColumnController> colsUsedInKeys = new HashSet<ColumnController>();
        // for (final Iterator<KeyController> i = mergeTable.getKeys().iterator(); i.hasNext();)
        // colsUsedInKeys.addAll(Arrays.asList(i.next().getColumns()));

        // Add all columns from merge table to dataset table, except those in
        // the ignore key.
        for (final Iterator<Column> i = mergeTable.getColumnList().iterator(); i.hasNext();) {
            final SourceColumn c = (SourceColumn) i.next();

            // Ignore those in the key used to get here.
            if (ignoreCols.contains(c))
                continue;
            if (c.isHidden())
                continue;
            // Create a name for this column.
            String internalColName = c.getName();
            internalColName = internalColName + "_"// + ((Mart)mergeTable.getSchema().getParent()).getUniqueId()
                    + mergeTable.getSchema().getUniqueId() + tableTrackerCount + mergeTable.getUniqueId();
            // Add the unique suffix to the visible col name.
            // Expand to full-length by prefixing relation
            // info, and relation tracker info. Note we use
            // the tracker not the iteration as this gives us
            // a unique repetition number.
            // Add partitioning prefixes.
            for (int k = 0; k < nameCols.size(); k++) {
                final String pcolName = (String) nameCols.get(k);
                final String suffix = "#" + (String) nameColSuffixes.get(k);
                internalColName = pcolName + suffix + Resources.get("columnnameSep") + internalColName;
            }
            // Rename all PK columns to have the '_key' suffix.
            // otherwise, column will be added but not as _key
            // Reuse or create new wrapped column?
            WrappedColumn wc = null;
            boolean reusedColumn = false;
            // Don't reuse cols that will be part of the PK.
            if (dsTable.getColumnNames().contains(internalColName)) {
                final DatasetColumn candDSCol = dsTable.getColumnByName(internalColName);
                if (candDSCol instanceof WrappedColumn && !(candDSCol instanceof InheritedColum)) {
                    wc = (WrappedColumn) dsTable.getColumnByName(internalColName);
                    reusedColumn = true;
                }
            }

            if (!reusedColumn) {
                // Create new column using unique name.
                wc = new WrappedColumn((SourceColumn) c, internalColName, dsTable);

                // Insert column rename using origColName, but
                // only if one not already specified (e.g. from
                // XML file).
                if (wc.isColumnRenamed() == false) {
                    try {
                        wc.setColumnRename(internalColName, false);
                    } catch (ValidationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                dsTable.addColumn(wc);

                // Listen to this column to modify ourselves.
                // if (!dsTable.getType().equals(DatasetTableType.DIMENSION))
                // wc.getDatasetColumn().addPropertyChangeListener("columnRename",
                // this.rebuildListener);
            }

            unusedCols.remove(wc);
            tu.getNewColumnNameMap().put(c, (DatasetColumn) wc);
            wc.setTransformationUnit(tu);

            // If the column is in any key on this table then it is a
            // dependency for possible future linking, which must be
            // flagged.
            // wc.setKeyDependency(colsUsedInKeys.contains(c));

            // If the column was in the merge table's PK, and we are
            // expecting to add the PK to the generated table's PK, then
            // add it to the generated table's PK.
            if (includeMergeTablePK && mergeTablePK.getColumns().contains(c))
                dsTablePKCols.add(wc);
        }

        // Update the three queues with relations that lead away from this
        // table.
        final List<Relation> mergeRelations = new ArrayList<Relation>(mergeTable.getRelations());
        Collections.sort(mergeRelations);
        for (int i = 0; i < mergeRelations.size(); i++) {
            final Relation r = mergeRelations.get(i);

            // Allow to go back up sourceRelation if it is a loopback
            // 1:M relation and we have just merged the 1 end.

            // Don't go back up same relation unless we are doing
            // loopback. If we are doing loopback, do source relation last
            // by adding it to the end of the queue and skipping it this
            // time round.
            if (r.equals(sourceRelation))
                continue;

            if (r.isHidden())
                continue;

            // If just come down a 1:1, don't go back up another 1:1
            // to same table.
            if (sourceRelation != null && sourceRelation.isOneToOne() && r.isOneToOne()) {
                final Set<Table> tables = new HashSet<Table>();
                tables.add(r.getFirstKey().getTable());
                tables.add(r.getSecondKey().getTable());
                tables.remove(sourceRelation.getFirstKey().getTable());
                tables.remove(sourceRelation.getSecondKey().getTable());
                if (tables.isEmpty())
                    continue;
            }

            // Don't follow incorrect relations, or relations
            // between incorrect keys.
            if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
                    || r.getFirstKey().getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
                    || r.getSecondKey().getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                continue;

            // Don't follow relations to ignored tables.
            if (r.getOtherKey(r.getKeyForTable(mergeTable)).getTable().isHidden())
                continue;

            // Don't follow relations back to skipped mains unless
            // they have been forced.
            if (skippedMainTables.contains(r.getOtherKey(r.getKeyForTable(mergeTable)).getTable())
                    && !(r.isSubclassRelation(mart.getName())))
                continue;

            // Set up a holder to indicate whether or not to follow
            // the relation.
            boolean followRelation = false;
            boolean forceFollowRelation = false;

            // Are we at the 1 end of a 1:M?
            // If so, we may need to make a dimension, or a subclass.
            if (r.isOneToMany() && r.getFirstKey().getTable().equals(mergeTable)) {

                // Subclass subclassed relations, if we are currently
                // not building a dimension table.
                if (r.isSubclassRelation(mart.getName()) && !dsTable.getType().equals(DatasetTableType.DIMENSION)) {
                    final List<Column> newSourceDSCols = new ArrayList<Column>();
                    for (int j = 0; j < r.getFirstKey().getColumns().size(); j++)
                        newSourceDSCols.add(tu.getDatasetColumnFor(r.getFirstKey().getColumns().get(j)));

                    List<DatasetColumn> dsclist = new ArrayList<DatasetColumn>();
                    for (Column cc : newSourceDSCols) {
                        dsclist.add((DatasetColumn) cc);
                    }
                    SubclassQueueObject scqObject = new SubclassQueueObject();
                    scqObject.setColumnList(dsclist);
                    scqObject.setRelation(r);
                    subclassQ.add(scqObject);
                }

                // Dimensionize dimension relations, which are all other 1:M
                // relations, if we are not constructing a dimension
                // table, and are currently intending to construct
                // dimensions.
                else if (makeDimensions && !dsTable.getType().equals(DatasetTableType.DIMENSION)) {
                    final List<DatasetColumn> newSourceDSCols = new ArrayList<DatasetColumn>();
                    for (int j = 0; j < r.getFirstKey().getColumns().size(); j++) {
                        final DatasetColumn newCol = tu.getDatasetColumnFor(r.getFirstKey().getColumns().get(j));
                        newSourceDSCols.add((DatasetColumn) newCol);
                    }

                    DimensionQueueObject dqObject = new DimensionQueueObject();
                    dqObject.setDatasetColumnlist(newSourceDSCols);
                    dqObject.setRelation(r);
                    dimensionQ.add(dqObject);

                }

            }

            // Follow all others. Don't follow relations that are
            // already in the subclass or dimension queues.
            else
                followRelation = true;

            // If we follow a 1:1, and we are currently
            // including dimensions, include them from the 1:1 as well.
            // Otherwise, stop including dimensions on subsequent tables.
            if (followRelation || forceFollowRelation) {
                final List<String> nextNameCols = new ArrayList<String>(nameCols);
                final Map<String, List<String>> nextNameColSuffixes = new HashMap<String, List<String>>();
                nextNameColSuffixes.put("0", new ArrayList<String>(nameColSuffixes));

                final Key sourceKey = r.getKeyForTable(mergeTable);
                final Key targetKey = r.getOtherKey(sourceKey);
                final List<Column> newSourceDSCols = new ArrayList<Column>();
                for (int j = 0; j < sourceKey.getColumns().size(); j++)
                    newSourceDSCols.add(tu.getDatasetColumnFor(sourceKey.getColumns().get(j)));

                final List<String> nextList = new ArrayList<String>(nameColSuffixes);
                nextList.add("1");
                nextNameColSuffixes.put("1", nextList);

                // Insert first one at next position in queue
                // after current position. This creates multiple
                // top-down paths, rather than sideways-spanning trees of
                // actions. (If this queue were a graph, doing it this way
                // makes it depth-first as opposed to breadth-first).
                // The queue position is incremented so that they remain
                // in order - else they'd end up reversed.

                // if should be a sourceTable
                NormalQueueObject nqObject = new NormalQueueObject();
                nqObject.setRelation(r);
                List<DatasetColumn> dsclist = new ArrayList<DatasetColumn>();
                for (Column cc : newSourceDSCols) {
                    dsclist.add((DatasetColumn) cc);
                }
                nqObject.setColumnList(dsclist);
                nqObject.setTable(targetKey.getTable());
                nqObject.setTransformationUnit(tu);
                nqObject.setMakeDimension(makeDimensions && r.isOneToOne() || forceFollowRelation);
                nqObject.setNextNameColumns(nextNameCols);
                nqObject.setNextNameColSuffixes(nextNameColSuffixes.get("0"));
                normalQ.add(queuePos++, nqObject);
            }
        }
    }

    /**
     * Synchronise this mart with the schema that is providing its tables. Synchronisation means checking the columns
     * and relations and removing any that have disappeared. The mart is then regenerated.
     * 
     * @throws SQLException
     *             never thrown - this is inherited from {@link SchemaController} but does not apply here because we are
     *             not doing any database communications.
     * @throws DataModelException
     *             never thrown - this is inherited from {@link SchemaController} but does not apply here because we are
     *             not attempting any new logic with the schema.
     */
    public void synchronise(Mart mart) throws DataModelException {
        Log.debug("Regenerating mart " + mart.getName());
        // don't sync for now if the type is not from source
        // if(!this.mart.getDataLinkType().equals(DataLinkType.SOURCE))
        // return;
        // this.setFullSyncValue(false);

        // Empty out used rels and schs.
        // this.includedRelations.clear();
        // this.includedTables.clear();

        // Get the real main table.
        final SourceTable realCentralSourceTable = (SourceTable) mart.getRealCentralTable();

        // Work out the main tables to skip while transforming. main - submain ...
        final List<SourceTable> skippedTables = new ArrayList<SourceTable>();
        skippedTables.add(realCentralSourceTable);
        // the size may increase
        for (int i = 0; i < skippedTables.size(); i++) {
            final SourceTable cand = skippedTables.get(i);
            if (cand.getPrimaryKey() != null)
                for (final Relation rel : cand.getPrimaryKey().getRelations()) {
                    if (!rel.isHidden() && rel.isSubclassRelation(mart.getName()))
                        skippedTables.add((SourceTable) rel.getManyKey().getTable());
                }
        }

        // Make a list of all tables.
        final Set<DatasetTable> unusedDsTables = new HashSet<DatasetTable>(mart.getDatasetTables());
        try {
            // Generate the main table. It will recursively generate all the
            // others.
            this.generateDatasetTable(mart, DatasetTableType.MAIN, null, realCentralSourceTable, skippedTables,
                    new ArrayList<DatasetColumn>(), null, unusedDsTables);
        } catch (final Exception pe) {
            throw new DataModelException(pe);
        }

        // Drop any rels from tables still in list, then drop tables too.
        for (final Iterator<DatasetTable> i = unusedDsTables.iterator(); i.hasNext();) {
            final DatasetTable deadTbl = (DatasetTable) i.next();
            for (final Iterator<Key> j = deadTbl.getKeys().iterator(); j.hasNext();) {
                final Key key = j.next();
                for (final Iterator<Relation> r = key.getRelations().iterator(); r.hasNext();) {
                    final Relation rel = r.next();
                    Key otherKey = rel.getOtherKey(key);
                    otherKey.removeRelation(rel);
                    r.remove();
                }
            }

            deadTbl.setPrimaryKey(null);
            deadTbl.getForeignKeys().clear();
            mart.removeDatasetTable(deadTbl);
        }
        Log.info("done synchronize");
    }

    public Collection<Mart> requestCreateMartsFromTarget(MartRegistry registry, DataLinkInfo dlinkInfo)
            throws MartBuilderException {
        List<Mart> result = new ArrayList<Mart>();
        for (Map.Entry<MartInVirtualSchema, List<DatasetFromUrl>> entry : dlinkInfo.getJdbcLinkObject().getDsInfoMap()
                .entrySet()) {
            String tbName = entry.getValue().get(0).getName();
            int index = tbName.indexOf(Resources.get("tablenameSep"));
            String dsName = tbName.substring(0, index);
            Mart mc = null;
            if (!dlinkInfo.isBCPartitioned())
                mc = this.requestCreateDataSetFromTarget(registry, dsName, dlinkInfo);
            else
                mc = this.requestCreateDataSetFromTargetPartitioned(registry, entry.getKey(), dlinkInfo);
            if (mc != null)
                result.add(mc);
            // only one for now
            break;
        }
        return result;
    }

    private Mart requestCreateDataSetFromTarget(MartRegistry registry, String dsName, DataLinkInfo dlinkInfo)
            throws MartBuilderException {
        // create dataset
        // MartController dataset = null;
        Mart mart = new Mart(registry, dsName, null);
        // dataset = (MartController)mart.getWrapper();

        // add datasettable
        McSQL mcSql = new McSQL();
        JdbcLinkObject dbconObj = dlinkInfo.getJdbcLinkObject().getDsInfoMap().keySet().iterator().next()
                .getJdbcLinkObject();
        // dataset.setConnectionObject(dbconObj);
        // dataset name is the martName + "_" + last part of the database name
        String databaseName = dbconObj.getSchemaName();
        String[] _names = databaseName.split(Resources.get("tablenameSubSep"), 2);
        String datasetName;
        if (_names.length == 1)
            datasetName = dsName + "_" + _names[0];
        else
            datasetName = dsName + "_" + _names[1];

        String sqlDsName = dsName;
        if (dlinkInfo.isTargetTableNamePartitioned()) {
            int index = dsName.lastIndexOf(Resources.get("tablenameSubSep"));
            if (index >= 0) {
                sqlDsName = dsName.substring(0, index);
            }
        }
        Set<String> subPartitionTables = mcSql.getPartitionedTables(dbconObj, sqlDsName);
        Map<String, List<String>> tblColMap = mcSql.getMartTablesInfo(dbconObj, dsName);
        if (tblColMap.isEmpty())
            return null;
        // get all main tables and keys TODO improve
        Map<String, List<String>> mainKeyMap = new HashMap<String, List<String>>();
        List<String> orderedMainTableList = new ArrayList<String>();
        List<String> mainTableList = new ArrayList<String>();
        // get all mainTable
        for (Map.Entry<String, List<String>> entry : tblColMap.entrySet()) {
            if (!isMainTable(entry.getKey()))
                continue;
            mainTableList.add(entry.getKey());
        }
        // build the mainKeyMap and order the mainTable
        String[] mainArray = new String[mainTableList.size()];
        for (String tblName : mainTableList) {
            if (!isMainTable(tblName))
                continue;
            List<String> keyList = new ArrayList<String>();
            for (String colStr : tblColMap.get(tblName)) {
                if (colStr.endsWith(Resources.get("keySuffix")))
                    // if(colStr.indexOf(Resources.get("keySuffix"))>=0)
                    keyList.add(colStr);
            }
            mainKeyMap.put(tblName, keyList);
            if (keyList.size() < 1) {
                JOptionPane.showMessageDialog(null, "no key column in " + tblName);
                return null;
            }
            mainArray[keyList.size() - 1] = tblName;
        }
        orderedMainTableList = Arrays.asList(mainArray);
        if (orderedMainTableList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "check " + dbconObj.getDatabaseName());
            return null;
        }

        for (Map.Entry<String, List<String>> entry : tblColMap.entrySet()) {
            String tblName = entry.getKey();
            DatasetTable dstable = (DatasetTable) mart.getTableByName(tblName);
            // create datasettable when it is null
            if (dstable == null) {
                // get table type
                DatasetTableType dstType;
                if (isMainTable(tblName)) {
                    if (orderedMainTableList.indexOf(tblName) > 0)
                        dstType = DatasetTableType.MAIN_SUBCLASS;
                    else
                        dstType = DatasetTableType.MAIN;
                } else
                    dstType = DatasetTableType.DIMENSION;

                dstable = new DatasetTable(mart, tblName, dstType);
                dstable.addInPartitions(datasetName);
                mart.addTable(dstable);
                // columns
                for (String colStr : entry.getValue()) {
                    // all are fake wrapped column
                    DatasetColumn column = dstable.getColumnByName(colStr);
                    if (column == null) {
                        column = new DatasetColumn(dstable, colStr);
                        column.addInPartitions(datasetName);
                        dstable.addColumn(column);
                    }
                    // set pk or fk if colStr is a key
                    if (colStr.indexOf(Resources.get("keySuffix")) >= 0) {
                        if (dstType.equals(DatasetTableType.DIMENSION)) {
                            ForeignKey fkObject = new ForeignKey(column);
                            // KeyController fk = new KeyController(fkObject);
                            fkObject.setStatus(ComponentStatus.INFERRED);
                            if (!dstable.getForeignKeys().contains(fkObject)) {
                                dstable.getForeignKeys().add(fkObject);
                                // dstable.getForeignKeys().add(fk);
                            }
                        } else if (dstType.equals(DatasetTableType.MAIN)) {
                            PrimaryKey pkObject = new PrimaryKey(column);
                            // KeyController pk = new KeyController(pkObject);
                            pkObject.setStatus(ComponentStatus.INFERRED);
                            dstable.setPrimaryKey(pkObject);
                        } else {
                            if (isColPkinTable(colStr, tblName, mainKeyMap, orderedMainTableList)) {
                                PrimaryKey pkObject = new PrimaryKey(column);
                                // KeyController pk = new KeyController(pkObject);
                                pkObject.setStatus(ComponentStatus.INFERRED);
                                dstable.setPrimaryKey(pkObject);
                            } else {
                                ForeignKey fkObject = new ForeignKey(column);
                                // KeyController fk = new KeyController(fkObject);
                                fkObject.setStatus(ComponentStatus.INFERRED);
                                if (!dstable.getForeignKeys().contains(fkObject)) {
                                    dstable.getForeignKeys().add(fkObject);
                                    // dstable.getForeignKeys().add(fk);
                                }
                            }
                        }
                    }
                } // end of columns
            }// end of if
            if (dlinkInfo.isTargetTableNamePartitioned()) {
                for (String value : subPartitionTables)
                    dstable.addSubPartition(value);
            }
        }

        // relations main -- dimension first
        for (String mainTStr : orderedMainTableList) {
            DatasetTable mainTable = mart.getTableByName(mainTStr);
            for (DatasetTable table : mart.getDatasetTables()) {
                if (!table.getType().equals(DatasetTableType.DIMENSION))
                    continue;
                PrimaryKey pk = mainTable.getPrimaryKey();
                // if pk and fk have the same columns (same name for now), matched
                List<String> pkColList = new ArrayList<String>();
                for (Column tmpCol1 : pk.getColumns())
                    pkColList.add(tmpCol1.getName());

                for (ForeignKey fk : table.getForeignKeys()) {
                    List<String> fkColList = new ArrayList<String>();
                    for (Column tmpCol2 : fk.getColumns())
                        fkColList.add(tmpCol2.getName());
                    if (pkColList.size() == fkColList.size() && pkColList.containsAll(fkColList)) {

                        if (!Relation.isRelationExist(pk, fk))
                            try {
                                Relation rel = new RelationTarget(pk, fk, Cardinality.MANY_A);
                                // pk.getObject().addRelation(rel);
                                // fk.getObject().addRelation(rel);
                                rel.setOriginalCardinality(Cardinality.MANY_A);
                                rel.setStatus(ComponentStatus.INFERRED);
                            } catch (AssociationException e) {
                                e.printStackTrace();
                            }
                    }
                }
            }
        }// end of main -- dimension relation
         // sub main relation
        for (int i = 0; i < orderedMainTableList.size() - 1; i++) {
            DatasetTable firstDst = mart.getTableByName(orderedMainTableList.get(i));
            DatasetTable secondDst = mart.getTableByName(orderedMainTableList.get(i + 1));
            PrimaryKey pk = firstDst.getPrimaryKey();
            // if pk and fk have the same columns (same name for now), matched
            List<String> pkColList = new ArrayList<String>();
            for (Column tmpCol1 : pk.getColumns())
                pkColList.add(tmpCol1.getName());

            for (ForeignKey fk : secondDst.getForeignKeys()) {
                List<String> fkColList = new ArrayList<String>();
                for (Column tmpCol2 : fk.getColumns())
                    fkColList.add(tmpCol2.getName());
                if (pkColList.size() == fkColList.size() && pkColList.containsAll(fkColList)) {

                    if (!Relation.isRelationExist(pk, fk))
                        try {
                            Relation rel = new RelationTarget(pk, fk, Cardinality.MANY_A);
                            // pk.getObject().addRelation(rel);
                            // fk.getObject().addRelation(rel);
                            rel.setOriginalCardinality(Cardinality.MANY_A);
                            rel.setStatus(ComponentStatus.INFERRED);
                            rel.setSubclassRelation(true, Relation.DATASET_WIDE);
                        } catch (AssociationException e) {
                            e.printStackTrace();
                        } catch (ValidationException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                }
            }
        }

        mart.setCentralTable(mart.getTableByName(orderedMainTableList.get(0)));
        // create a default partitiontable
        PartitionTable pt = new PartitionTable(mart, PartitionType.SCHEMA);
        // new PartitionTableController(pt);

        List<String> rowItem = new ArrayList<String>();
        rowItem.add(dbconObj.getConnectionBase());
        rowItem.add(dbconObj.getDatabaseName());
        rowItem.add(dbconObj.getSchemaName());
        rowItem.add(dbconObj.getUserName());
        rowItem.add(dbconObj.getPassword());
        rowItem.add(datasetName);
        rowItem.add(XMLElements.FALSE_VALUE.toString());
        rowItem.add(datasetName);
        rowItem.add(Resources.BIOMART_VERSION);
        // add empty string till col 14
        rowItem.add("");
        rowItem.add("");
        rowItem.add("");
        rowItem.add("");
        rowItem.add("");
        rowItem.add("");
        pt.addNewRow(rowItem);

        mart.addPartitionTable(pt);
        return mart;
    }

    private Mart requestCreateDataSetFromTargetPartitioned(MartRegistry registry, MartInVirtualSchema martInVS,
            DataLinkInfo dlinkInfo) throws MartBuilderException {
        // mart name is the database name;
        JdbcLinkObject dbconObj = dlinkInfo.getJdbcLinkObject().getDsInfoMap().keySet().iterator().next()
                .getJdbcLinkObject();
        String databaseName = dbconObj.getSchemaName();
        Mart mart = new Mart(registry, martInVS.getName(), null);
        // create a default partitiontable
        PartitionTable pt = new PartitionTable(mart, PartitionType.SCHEMA);
        mart.addPartitionTable(pt);

        McSQL mcSql = new McSQL();

        // load tables for all datasets
        for (DatasetFromUrl dsurl : dlinkInfo.getJdbcLinkObject().getDsInfoMap().get(martInVS)) {
            // assume that they all follow the name convention
            String tablename = dsurl.getName();
            int index = tablename.indexOf(Resources.get("tablenameSep"));
            String dsName = tablename.substring(0, index);
            Map<String, List<String>> tblColMap = mcSql.getMartTablesInfo(dbconObj, dsName);
            if (tblColMap.isEmpty())
                return null;

            // get all main tables and keys TODO improve
            Map<String, List<String>> mainKeyMap = new HashMap<String, List<String>>();
            List<String> orderedMainTableList = new ArrayList<String>();
            List<String> mainTableList = new ArrayList<String>();
            // get all mainTable
            for (Map.Entry<String, List<String>> entry : tblColMap.entrySet()) {
                if (!isMainTable(entry.getKey()))
                    continue;
                mainTableList.add(entry.getKey());
            }
            // build the mainKeyMap and order the mainTable
            String[] mainArray = new String[mainTableList.size()];
            for (String tblName : mainTableList) {
                if (!isMainTable(tblName))
                    continue;
                List<String> keyList = new ArrayList<String>();
                for (String colStr : tblColMap.get(tblName)) {
                    if (colStr.endsWith(Resources.get("keySuffix")))
                        // if(colStr.indexOf(Resources.get("keySuffix"))>=0)
                        keyList.add(colStr);
                }
                mainKeyMap.put(tblName, keyList);
                if (keyList.size() < 1) {
                    JOptionPane.showMessageDialog(null, "no key column in " + tblName);
                    return null;
                }
                mainArray[keyList.size() - 1] = tblName;
            }
            orderedMainTableList = Arrays.asList(mainArray);
            if (orderedMainTableList.isEmpty()) {
                JOptionPane.showMessageDialog(null, "check " + dbconObj.getDatabaseName());
                return null;
            }

            for (Map.Entry<String, List<String>> entry : tblColMap.entrySet()) {
                String tblName = entry.getKey();
                // put it in partition format
                index = tblName.indexOf(Resources.get("tablenameSep"));
                String _tblName = tblName.substring(index + 2);
                String tblNamePartitioned = "(p0c" + PartitionUtils.DATASETNAME + ")" + "__" + _tblName;
                DatasetTable dstable = (DatasetTable) mart.getTableByName(tblNamePartitioned);
                // create datasettable when it is null
                if (dstable == null) {
                    // get table type
                    DatasetTableType dstType;
                    if (isMainTable(tblNamePartitioned)) {
                        if (orderedMainTableList.indexOf(tblNamePartitioned) > 0)
                            dstType = DatasetTableType.MAIN_SUBCLASS;
                        else
                            dstType = DatasetTableType.MAIN;
                    } else
                        dstType = DatasetTableType.DIMENSION;

                    dstable = new DatasetTable(mart, tblNamePartitioned, dstType);
                    dstable.addInPartitions(dsName);
                    mart.addTable(dstable);
                }// end of if
                else {
                    dstable.addInPartitions(dsName);
                }
                DatasetTableType dstType = dstable.getType();
                // columns
                for (String colStr : entry.getValue()) {
                    // all are fake wrapped column
                    DatasetColumn column = dstable.getColumnByName(colStr);
                    if (column == null) {
                        column = new DatasetColumn(dstable, colStr);
                        column.addInPartitions(dsName);
                        dstable.addColumn(column);
                    } else {
                        column.addInPartitions(dsName);
                    }
                    // set pk or fk if colStr is a key
                    if (colStr.indexOf(Resources.get("keySuffix")) >= 0) {
                        if (dstType.equals(DatasetTableType.DIMENSION)) {
                            ForeignKey fkObject = new ForeignKey(column);
                            // KeyController fk = new KeyController(fkObject);
                            fkObject.setStatus(ComponentStatus.INFERRED);
                            if (!dstable.getForeignKeys().contains(fkObject)) {
                                dstable.getForeignKeys().add(fkObject);
                                // dstable.getForeignKeys().add(fk);
                            }
                        } else if (dstType.equals(DatasetTableType.MAIN)) {
                            PrimaryKey pkObject = new PrimaryKey(column);
                            // KeyController pk = new KeyController(pkObject);
                            pkObject.setStatus(ComponentStatus.INFERRED);
                            dstable.setPrimaryKey(pkObject);
                        } else {
                            if (isColPkinTable(colStr, tblName, mainKeyMap, orderedMainTableList)) {
                                PrimaryKey pkObject = new PrimaryKey(column);
                                // KeyController pk = new KeyController(pkObject);
                                pkObject.setStatus(ComponentStatus.INFERRED);
                                dstable.setPrimaryKey(pkObject);
                            } else {
                                ForeignKey fkObject = new ForeignKey(column);
                                // KeyController fk = new KeyController(fkObject);
                                fkObject.setStatus(ComponentStatus.INFERRED);
                                if (!dstable.getForeignKeys().contains(fkObject)) {
                                    dstable.getForeignKeys().add(fkObject);
                                    // dstable.getForeignKeys().add(fk);
                                }
                            }
                        }
                    }
                } // end of columns
            }

            // relations main -- dimension first
            for (String mainTStr : orderedMainTableList) {
                DatasetTable mainTable = mart.getTableByName(mainTStr);
                for (DatasetTable table : mart.getDatasetTables()) {
                    if (!table.getType().equals(DatasetTableType.DIMENSION))
                        continue;
                    PrimaryKey pk = mainTable.getPrimaryKey();
                    // if pk and fk have the same columns (same name for now), matched
                    List<String> pkColList = new ArrayList<String>();
                    for (Column tmpCol1 : pk.getColumns())
                        pkColList.add(tmpCol1.getName());

                    for (ForeignKey fk : table.getForeignKeys()) {
                        List<String> fkColList = new ArrayList<String>();
                        for (Column tmpCol2 : fk.getColumns())
                            fkColList.add(tmpCol2.getName());
                        if (pkColList.size() == fkColList.size() && pkColList.containsAll(fkColList)) {

                            if (!Relation.isRelationExist(pk, fk))
                                try {
                                    Relation rel = new RelationTarget(pk, fk, Cardinality.MANY_A);
                                    // pk.getObject().addRelation(rel);
                                    // fk.getObject().addRelation(rel);
                                    rel.setOriginalCardinality(Cardinality.MANY_A);
                                    rel.setStatus(ComponentStatus.INFERRED);
                                } catch (AssociationException e) {
                                    e.printStackTrace();
                                }
                        }
                    }
                }
            }// end of main -- dimension relation
             // sub main relation
            for (int i = 0; i < orderedMainTableList.size() - 1; i++) {
                DatasetTable firstDst = mart.getTableByName(orderedMainTableList.get(i));
                DatasetTable secondDst = mart.getTableByName(orderedMainTableList.get(i + 1));
                PrimaryKey pk = firstDst.getPrimaryKey();
                // if pk and fk have the same columns (same name for now), matched
                List<String> pkColList = new ArrayList<String>();
                for (Column tmpCol1 : pk.getColumns())
                    pkColList.add(tmpCol1.getName());

                for (ForeignKey fk : secondDst.getForeignKeys()) {
                    List<String> fkColList = new ArrayList<String>();
                    for (Column tmpCol2 : fk.getColumns())
                        fkColList.add(tmpCol2.getName());
                    if (pkColList.size() == fkColList.size() && pkColList.containsAll(fkColList)) {

                        if (!Relation.isRelationExist(pk, fk))
                            try {
                                Relation rel = new RelationTarget(pk, fk, Cardinality.MANY_A);
                                // pk.getObject().addRelation(rel);
                                // fk.getObject().addRelation(rel);
                                rel.setOriginalCardinality(Cardinality.MANY_A);
                                rel.setStatus(ComponentStatus.INFERRED);
                                rel.setSubclassRelation(true, Relation.DATASET_WIDE);
                            } catch (AssociationException e) {
                                e.printStackTrace();
                            } catch (ValidationException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                    }
                }
            }
            List<String> rowItem = new ArrayList<String>();
            rowItem.add(dbconObj.getConnectionBase());
            rowItem.add(dbconObj.getDatabaseName());
            rowItem.add(dbconObj.getSchemaName());
            rowItem.add(dbconObj.getUserName());
            rowItem.add(dbconObj.getPassword());
            rowItem.add(dsName);
            rowItem.add(XMLElements.FALSE_VALUE.toString());
            rowItem.add(dsName);
            rowItem.add(Resources.BIOMART_VERSION);
            // add empty string till col 14
            rowItem.add("");
            rowItem.add("");
            rowItem.add("");
            rowItem.add("");
            rowItem.add("");
            rowItem.add("");
            pt.addNewRow(rowItem);

        }

        mart.setCentralTable(mart.getOrderedMainTableList().get(0));
        return mart;
    }

    private boolean isMainTable(String tableName) {
        return tableName.indexOf(Resources.get("tablenameSep") + Resources.get("mainSuffix")) >= 0;
    }

    /**
     * the table is submain table
     * 
     * @param colName
     * @param tableName
     * @return
     */
    private boolean isColPkinTable(String colName, String tableName, Map<String, List<String>> tableKeyMap,
            List<String> tableList) {
        int index = tableList.indexOf(tableName);
        String preTableName = tableList.get(index - 1);
        return !(tableKeyMap.get(preTableName).contains(colName));
    }

    /**
     * scan the whole registry to fix attribute/filter pointer fix link pointer create links from exportable/importable
     * TODO will remove exportable/importable later
     */
    public void fixObjects(MartRegistry registry) {
        // fix attribute/filter pointer
        for (Mart mart : registry.getMartList()) {
            for (Config config : mart.getConfigList()) {
                // for all datasets
                List<Attribute> attList = config.getAttributes(new ArrayList<String>(), true, true);
                for (Attribute attribute : attList) {
                    if (attribute.isPointer() && attribute.getObjectStatus() != ValidationStatus.VALID) {
                        // fix the pointer attribute
                        String pointedAttName = attribute.getPointedAttributeName();
                        String pointedMartName = attribute.getPointedMartName();
                        String pointedDatasetName = attribute.getPointedDatasetName();
                        String pointedConfigName = attribute.getPointedConfigName();
                        Dataset dataset = null;
                        Mart pointedMart = null;
                        // find mart, if martname is empty, try dataset
                        if (pointedMartName != null && !(pointedMartName.trim().equals(""))) {
                            pointedMart = registry.getMartByName(pointedMartName);
                            if (pointedMart == null)
                                continue;
                            dataset = pointedMart.getDatasetByName(pointedDatasetName);
                            if (dataset == null)
                                continue;
                        } else {
                            dataset = registry.getDatasetByName(pointedDatasetName, config.getName());
                            if (dataset == null)
                                continue;
                            pointedMart = dataset.getParentMart();
                        }
                        Config pConfig = pointedMart.getConfigByName(pointedConfigName);
                        if (pConfig == null) {
                            pConfig = pointedMart.getDefaultConfig();
                        }
                        Attribute pointedAttribute = pConfig.getAttributeByName(null, pointedAttName, true);
                        if (pointedAttribute == null)
                            continue;
                        attribute.addPointedDataset(dataset);
                        pointedAttribute.setConfig(pConfig);
                        attribute.setPointedElement(pointedAttribute);
                        attribute.setObjectStatus(ValidationStatus.VALID);
                    }
                }
                // filters
                List<Filter> filterList = config.getFilters(new ArrayList<String>(), true, true);
                for (Filter filter : filterList) {
                    if (filter.isPointer() && filter.getObjectStatus() != ValidationStatus.VALID) {
                        // fix the pointer attribute
                        String pointedFilterName = filter.getPointedFilterName();
                        String pointedMartName = filter.getPointedMartName();
                        String pointedDatasetName = filter.getPointedDatasetName();
                        String pointedConfigName = filter.getPointedConfigName();
                        Dataset dataset = null;
                        Mart pointedMart = null;
                        // find mart
                        if (pointedMartName != null && !(pointedMartName.trim().equals(""))) {
                            pointedMart = registry.getMartByName(pointedMartName);
                            if (pointedMart == null)
                                continue;
                            dataset = pointedMart.getDatasetByName(pointedDatasetName);
                            if (dataset == null)
                                continue;
                        } else {
                            dataset = registry.getDatasetByName(pointedDatasetName, config.getName());
                            if (dataset == null)
                                continue;
                            pointedMart = dataset.getParentMart();
                        }
                        Config pConfig = pointedMart.getConfigByName(pointedConfigName);
                        if (pConfig == null) {
                            pConfig = pointedMart.getDefaultConfig();
                            if (pConfig == null) {
                                Log.debug("config is null " + pointedConfigName + " in pointed mart "
                                        + pointedMart.getName() + " in filter pointer " + filter.getName());
                                continue;
                            }
                        }
                        Filter pointedFilter = pConfig.getFilterByName(null, pointedFilterName, true);
                        if (pointedFilter == null)
                            continue;
                        filter.addPointedDataset(dataset);
                        pointedFilter.setConfig(pConfig);
                        filter.setPointedElement(pointedFilter);
                        filter.setObjectStatus(ValidationStatus.VALID);
                    }
                }
            }
        }

        // fix link pointer
        for (Mart mart : registry.getMartList()) {
            for (Config config : mart.getConfigList()) {
                List<Link> linkList = config.getLinkList();
                for (Link link : linkList) {
                    link.synchronizedFromXML();
                }
            }
        }
    }

    /**
     * Given a set of tables, produce the minimal set of datasets which include all the specified tables. Tables can be
     * included in the same dataset if they are linked by 1:M relations (1:M, 1:M in a chain), or if the table is the
     * last in the chain and is linked to the previous table by a pair of 1:M and M:1 relations via a third table,
     * simulating a M:M relation.
     * <p>
     * If the chains of tables fork, then one dataset is generated for each branch of the fork.
     * <p>
     * Every suggested dataset is synchronised before being returned.
     * <p>
     * Datasets will be named after their central tables. If a dataset with that name already exists, a '_' and sequence
     * number will be appended to make the new dataset name unique.
     * <p>
     * 
     * @param includeTables
     *            the tables that must appear in the final set of datasets.
     * @return the collection of datasets generated.
     * @throws SQLException
     *             if there is any problem talking to the source database whilst generating the dataset.
     * @throws DataModelException
     *             if synchronisation fails.
     */
    private Collection<Mart> suggestMarts(final MartRegistry registry, final TargetSchema schema,
            final Collection<SourceTable> includeTables) throws SQLException, DataModelException {
        Log.debug("Suggesting datasets for " + includeTables);

        // The root tables are all those which do not have a M:1 relation
        // to another one of the initial set of tables. This means that
        // extra datasets will be created for each table at the end of
        // 1:M:1 relation, so that any further tables past it will still
        // be included.
        Log.debug("Finding root tables");
        final Collection<SourceTable> rootTables = new HashSet<SourceTable>(includeTables);
        for (final Iterator<SourceTable> i = includeTables.iterator(); i.hasNext();) {
            final SourceTable candidate = i.next();
            for (final Iterator<Relation> j = candidate.getRelations().iterator(); j.hasNext();) {
                final Relation rel = j.next();
                if (rel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                    continue;
                if (!rel.isOneToMany())
                    continue;
                if (!rel.getManyKey().getTable().equals(candidate))
                    continue;
                if (includeTables.contains(rel.getFirstKey().getTable()))
                    rootTables.remove(candidate);
            }
        }
        // We construct one dataset per root table.
        final List<Mart> suggestedMarts = new ArrayList<Mart>();
        for (final Iterator<SourceTable> i = rootTables.iterator(); i.hasNext();) {
            final SourceTable rootTable = i.next();
            Log.debug("Constructing dataset for root table " + rootTable);
            Mart tmpMart = null;
            /*
             * if(reuseMart) { tmpMart = registry.getMartByName(rootTable.getName()); } else
             */
            tmpMart = new Mart(registry, rootTable.getName(), rootTable);
            tmpMart.setHasSource(true);
            tmpMart.setTargetSchema(schema);
            // Process it.
            final Collection<SourceTable> tablesIncluded = new HashSet<SourceTable>();
            tablesIncluded.add(rootTable);
            Log.debug("Attempting to find subclass marts");
            suggestedMarts
                    .addAll(this.continueSubclassing(registry, includeTables, tablesIncluded, tmpMart, rootTable));
        }

        // Synchronise them all.
        Log.debug("Synchronising constructed marts");
        for (Mart ds : suggestedMarts) {
            ds.setTargetSchema(schema);
            this.synchronise(ds);
        }

        // Do any of the resulting datasets contain all the tables
        // exactly with subclass relations between each?
        // If so, just use that one dataset and forget the rest.
        Log.debug("Finding perfect candidate");
        Mart perfectDS = null;
        for (final Iterator<Mart> i = suggestedMarts.iterator(); i.hasNext() && perfectDS == null;) {
            final Mart candidate = i.next();

            // A candidate is a perfect match if the set of tables
            // covered by the subclass relations is the same as the
            // original set of tables requested.
            final Collection<Table> scTables = new HashSet<Table>();
            for (final Iterator<Relation> j = candidate.getRelations().iterator(); j.hasNext();) {
                final Relation r = j.next();
                if (!r.isSubclassRelation(candidate.getName()))
                    continue;
                scTables.add(r.getFirstKey().getTable());
                scTables.add(r.getSecondKey().getTable());
            }
            // Finally perform the check to see if we have them all.
            if (scTables.containsAll(includeTables))
                perfectDS = candidate;
        }
        if (perfectDS != null) {
            Log.debug("Perfect candidate found - dropping others");
            // Drop the others.
            for (final Iterator<Mart> i = suggestedMarts.iterator(); i.hasNext();) {
                final Mart candidate = i.next();
                if (!candidate.equals(perfectDS)) {
                    registry.removeMart(candidate);
                    i.remove();
                }
            }
            // Rename it to lose any extension it may have gained.
            String newName = perfectDS.getCentralTable().getName();
            String uniqueName = registry.getNextMartName(newName);
            perfectDS.setName(uniqueName);
        } else
            Log.debug("No perfect candidate found - retaining all");

        // Return the final set of suggested datasets.
        return suggestedMarts;
    }

    /**
     * This internal method takes a bunch of tables that the user would like to see as subclass or main tables in a
     * single dataset, and attempts to find a subclass path between them. For each subclass path it can build, it
     * produces one dataset based on that path. Each path contains as many tables as possible. The paths do not overlap.
     * If there is a choice, the one chosen is arbitrary.
     * 
     * @param includeTables
     *            the tables we want to include as main or subclass tables.
     * @param tablesIncluded
     *            the tables we have managed to include in a path so far.
     * @param mart
     *            the dataset we started out from which contains just the main table on its own with no subclassing.
     * @param table
     *            the real table we are looking at to see if there is a subclass path between any of the include tables
     *            and any of the existing subclassed or main tables via this real table.
     * @return the datasets we have created - one per subclass path, or if there were none, then a singleton collection
     *         containing the dataset originally passed in.
     */
    // TODO check algorithm
    private Collection<Mart> continueSubclassing(final MartRegistry registry,
            final Collection<SourceTable> includeTables, final Collection<SourceTable> tablesIncluded, final Mart mart,
            final SourceTable table) {
        // Check table has a primary key.
        final PrimaryKey pk = table.getPrimaryKey();

        // Make a unique set to hold all the resulting datasets. It
        // is initially empty.
        final Set<Mart> suggestedDataSets = new HashSet<Mart>();
        // Make a set to contain relations to subclass.
        final Set<Relation> subclassedRelations = new HashSet<Relation>();
        // Make a map to hold tables included for each relation.
        final Map<Relation, Set<SourceTable>> relationTablesIncluded = new HashMap<Relation, Set<SourceTable>>();
        // Make a list to hold all tables included at this level.
        final Set<Table> localTablesIncluded = new HashSet<Table>(tablesIncluded);

        // Find all 1:M relations starting from the given table that point
        // to another interesting table (includeTables).
        if (pk != null)
            for (final Iterator<Relation> i = pk.getRelations().iterator(); i.hasNext();) {
                final Relation r = i.next();
                if (!r.isOneToMany())
                    continue;
                else if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                    continue;

                // For each relation, if it points to another included
                // table via 1:M we should subclass the relation.
                // subclass table should have primary key
                final SourceTable target = (SourceTable) r.getManyKey().getTable();
                if (includeTables.contains(target) && !localTablesIncluded.contains(target)
                        && null != target.getPrimaryKey()) {
                    subclassedRelations.add(r);
                    final Set<SourceTable> newRelationTablesIncluded = new HashSet<SourceTable>(tablesIncluded);
                    relationTablesIncluded.put(r, newRelationTablesIncluded);
                    newRelationTablesIncluded.add(target);
                    localTablesIncluded.add(target);
                }
            }

        // Find all 1:M:1 relations starting from the given table that point
        // to another interesting table.
        if (pk != null)
            for (final Iterator<Relation> i = pk.getRelations().iterator(); i.hasNext();) {
                final Relation firstRel = i.next();
                if (!firstRel.isOneToMany())
                    continue;
                else if (firstRel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                    continue;

                final Table intermediate = firstRel.getManyKey().getTable();
                for (final Iterator<ForeignKey> j = intermediate.getForeignKeys().iterator(); j.hasNext();) {
                    final ForeignKey fk = j.next();
                    if (fk.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                        continue;
                    for (final Iterator<Relation> k = fk.getRelations().iterator(); k.hasNext();) {
                        final Relation secondRel = k.next();
                        if (secondRel.equals(firstRel))
                            continue;
                        else if (!secondRel.isOneToMany())
                            continue;
                        else if (secondRel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                            continue;
                        // For each relation, if it points to another included
                        // table via M:1 we should subclass the relation, and this relation
                        // will replace the old one if created above.
                        // But localTablesIncluded has the old table and new one
                        final SourceTable target = (SourceTable) secondRel.getFirstKey().getTable();
                        if (includeTables.contains(target) && !localTablesIncluded.contains(target)) {
                            subclassedRelations.add(firstRel);
                            final Set<SourceTable> newRelationTablesIncluded = new HashSet<SourceTable>(tablesIncluded);
                            // if may replace the existing one
                            relationTablesIncluded.put(firstRel, newRelationTablesIncluded);
                            newRelationTablesIncluded.add(target);
                            localTablesIncluded.add(target);
                        }
                    }
                }
            }

        // No subclassing? Return a singleton.
        if (subclassedRelations.isEmpty())
            return Collections.singleton(mart);

        // Iterate through the relations we found and recurse.
        // If not the last one, we copy the original dataset and
        // work on the copy, otherwise we work on the original.
        // TODO check, should not use reference here, should have another copy of target schema and source schema
        for (final Iterator<Relation> i = subclassedRelations.iterator(); i.hasNext();) {
            final Relation r = i.next();
            Mart suggestedDataSet = mart;
            try {
                if (i.hasNext()) {
                    String martName = mart.getCentralTable().getName();
                    // make another copy of the schema

                    Mart tmpDataSet = new Mart(registry, martName, mart.getCentralTable());
                    tmpDataSet.setHasSource(true);
                    suggestedDataSet = tmpDataSet;

                    // Copy subclassed relations from existing dataset.
                    for (final Relation j : mart.getRelations())
                        j.setSubclassRelation(true, tmpDataSet.getName());
                }
                r.setSubclassRelation(true, suggestedDataSet.getName());
            } catch (final ValidationException e) {
                // Not valid? OK, ignore this one.
                e.printStackTrace();
                continue;
            }
            suggestedDataSets.addAll(this.continueSubclassing(registry, includeTables, relationTablesIncluded.get(r),
                    suggestedDataSet, (SourceTable) r.getManyKey().getTable()));
        }

        // Return the resulting datasets.
        return suggestedDataSets;
    }

    public Collection<Mart> requestCreateMartsFromSource(MartRegistry registry, DataLinkInfo dlinkInfo,
            List<SourceSchema> sourceSchemas) throws MartBuilderException {
        Collection<Mart> result = new ArrayList<Mart>();
        for (Map.Entry<MartInVirtualSchema, List<DatasetFromUrl>> entry : dlinkInfo.getJdbcLinkObject().getDsInfoMap()
                .entrySet()) {
            JdbcLinkObject jdbcLinkObj = entry.getKey().getJdbcLinkObject();
            SourceSchema tmpSchema = new SourceSchema(jdbcLinkObj);
            // add source schemas
            TargetSchema schema = new TargetSchema(tmpSchema, jdbcLinkObj);
            schema.initSourceSchema(sourceSchemas);

            List<DatasetFromUrl> stStrings = entry.getValue();

            List<SourceTable> suggestTables = new ArrayList<SourceTable>();

            for (DatasetFromUrl st : stStrings) {
                Table table = (Table) schema.getSourceSchema().getTableByName(st.getName());
                if (table != null)
                    suggestTables.add((SourceTable) table);
            }

            try {
                Collection<Mart> dss = this.suggestMarts(registry, schema, suggestTables);
                // create a default partition;
                for (Mart ds : dss) {
                    PartitionTable pt = new PartitionTable(ds, PartitionType.SCHEMA);
                    String datasetName = jdbcLinkObj.getSchemaName();
                    List<String> rowItem = new ArrayList<String>();
                    rowItem.add(jdbcLinkObj.getConnectionBase());
                    rowItem.add(jdbcLinkObj.getDatabaseName());
                    rowItem.add(jdbcLinkObj.getSchemaName());
                    rowItem.add(jdbcLinkObj.getUserName());
                    rowItem.add(jdbcLinkObj.getPassword());
                    rowItem.add(jdbcLinkObj.getSchemaName());
                    rowItem.add(XMLElements.FALSE_VALUE.toString());
                    rowItem.add(datasetName);
                    rowItem.add(Resources.BIOMART_VERSION);
                    rowItem.add("");
                    // for mysql, innodb or myisam
                    String kg = jdbcLinkObj.isKeyGuessing()
                            ? "1" : "0";
                    rowItem.add(kg);
                    rowItem.add(jdbcLinkObj.getDatabaseName());
                    rowItem.add(jdbcLinkObj.getSchemaName());
                    rowItem.add(jdbcLinkObj.getUserName());
                    rowItem.add(jdbcLinkObj.getPassword());
                    pt.addNewRow(rowItem);
                    ds.addPartitionTable(pt);
                    // add inpartition in tables/columns
                    for (DatasetTable dst : ds.getDatasetTables()) {
                        dst.addInPartitions(datasetName);
                        for (Column col : dst.getColumnList()) {
                            col.addInPartitions(datasetName);
                        }
                    }
                }
                result.addAll(dss);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    /*
     * entry point for both GUI and web api
     */
    public void requestCreateRegistryFromXML(MartRegistry registry, Document document) {
        Element root = document.getRootElement();
        McGuiUtils.INSTANCE.setRegistry(null);
        this.constructMartRegsitry(registry, root);
        this.fixObjects(registry);
        Validation.validate(registry, false);
        McGuiUtils.INSTANCE.setRegistry(registry);
        // load option file
        Element optionsElement = document.getRootElement().getChild(XMLElements.OPTIONS.toString());
        if (optionsElement != null) {
            optionsElement.detach();
            Options.getInstance().setOptions(optionsElement);
        }
    }

    private void constructMartRegsitry(MartRegistry registry, Element registryElement) {
        // marts
        @SuppressWarnings("unchecked")
        List<Element> martElementList = registryElement.getChildren(XMLElements.MART.toString());
        for (Element martElement : martElementList) {
            Mart mart = new Mart(martElement);
            registry.addMart(mart);
        }
        // sourcecontainers
        Element scsElement = registryElement.getChild(XMLElements.SOURCECONTAINERS.toString());
        SourceContainers scontainers = null;
        if (scsElement == null) {
            scsElement = new Element(XMLElements.SOURCECONTAINERS.toString());
            scsElement.setAttribute(XMLElements.NAME.toString(), "root");
            scsElement.setAttribute(XMLElements.DISPLAYNAME.toString(), "root");
            scsElement.setAttribute(XMLElements.INTERNALNAME.toString(), "root");
            scsElement.setAttribute(XMLElements.DESCRIPTION.toString(), "root");
            scsElement.setAttribute(XMLElements.HIDE.toString(), Boolean.toString(false));
            scontainers = new SourceContainers(scsElement);
            scontainers.addSourceContainerForOldXML(registry);
        } else
            scontainers = new SourceContainers(scsElement);
        registry.setSourcecontainers(scontainers);

        Element portalElement = registryElement.getChild(XMLElements.PORTAL.toString());
        Portal portal = new Portal(portalElement);
        registry.setPortal(portal);
        registry.synchronizedFromXML();
    }

    /**
     * Opens the dialog box to monitor the specified remote host and port.
     * 
     * @param host
     *            the host to connect to.
     * @param port
     *            the port the host is listening on.
     */
    public void requestMonitorRemoteHost(final String host, final String port) {
        // Open remote host monitor dialog.
        MartRunnerMonitorDialog.monitor(host, port);
    }

    /**
     * Runs the given {@link ConstructorRunnable} and monitors it's progress.
     * 
     * @param constructor
     *            the constructor that will build a mart.
     */
    public void requestMonitorConstructorRunnable(JDialog parent, final ConstructorRunnable constructor) {
        final ProgressDialog progressMonitor = ProgressDialog.getInstance(parent);

        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    constructor.run();
                } catch (final Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            StackTrace.showStackTrace(t);
                        }
                    });
                } finally {
                    progressMonitor.setVisible(false);
                }
                return null;
            }

            @Override
            protected void done() {
                progressMonitor.setVisible(false);
            }
        };

        worker.execute();
        progressMonitor.start("processing ...");

    }

    public List<MartConfiguratorObject> getReferencesForPartition(Mart mart, String ptcol) {
        List<MartConfiguratorObject> result = new ArrayList<MartConfiguratorObject>();

        return result;
    }

    private void setProperty(MartConfiguratorObject mcObj, String pro, String value) {
        XMLElements xe = XMLElements.valueFrom(pro);
        if (xe != null)
            mcObj.setProperty(xe, value);
    }

    public void setProperty(MartConfiguratorObject mcObj, String pro, String value, Dataset dataset) {
        if (dataset == null) {
            this.setProperty(mcObj, pro, value);
        } else {
            Config config = mcObj.getParentConfig();
            if (config == null) {
                this.setProperty(mcObj, pro, value);
                return;
            }
            PartitionTable spt = config.getMart().getSchemaPartitionTable();
            int row = spt.getRowNumberByDatasetName(dataset.getName());
            String oldValue = mcObj.getPropertyValue(XMLElements.valueFrom(pro));
            if (row >= 0) {
                // check if the original value is from partitiontable
                if (McUtils.hasPartitionBinding(oldValue)) {
                    // get the column
                    int col = McUtils.getPartitionColumnValue(oldValue);
                    spt.setValue(row, col, value);
                } else {
                    // create a new column
                    int col = spt.addColumn(oldValue);
                    spt.setValue(row, col, value);
                    this.setProperty(mcObj, pro, "(p0c" + col + ")");
                }
            } else
                this.setProperty(mcObj, pro, value);
        }
    }

    private boolean renameAttribute(Attribute attribute, String newName) {

        // get all references
        if (attribute.getName().equals(newName)) {
            return false;
        }
        Config config = attribute.getParentConfig();
        // check name conflict
        if (null != attribute.getParentConfig().getAttributeByName(null, newName, true)) {
            JOptionPane.showMessageDialog(null, "name conflic");
            return false;
        }
        // remember the oldname
        String oldName = attribute.getName();
        List<MartConfiguratorObject> references = attribute.getAllReferences();
        Container parentContainer = attribute.getParentContainer();
        attribute.getParentConfig().removeFromAttributeMap(attribute);
        attribute.setProperty(XMLElements.NAME, newName);
        attribute.getParentConfig().addToAttributeMap(attribute);
        for (MartConfiguratorObject mcObj : references) {
            if (mcObj instanceof Attribute) {
                // attribute pointer or list
                Attribute att = (Attribute) mcObj;
                if (att.isPointer()) {
                    att.setProperty(XMLElements.POINTEDATTRIBUTE, newName);
                } else if (att.isAttributeList()) {
                    String alStr = att.getAttributeListString();
                    String[] alArrays = alStr.split(",");
                    List<String> alList = Arrays.asList(alArrays);
                    int index = alList.indexOf(oldName);
                    alList.set(index, newName);
                    att.setAttributeListString(McUtils.StrListToStr(alList, ","));
                }
            } else if (mcObj instanceof Filter) {
                Filter fil = (Filter) mcObj;
                fil.setProperty(XMLElements.ATTRIBUTE, newName);
            }
        }
        ObjectController oc = new ObjectController();
        oc.generateRDF(MartController.getInstance().getMainAttribute(config.getMart()), config, false);
        return true;
    }

    public boolean renameAttribute(Attribute attribute, String newName, Dataset ds) {
        if (ds == null)
            return this.renameAttribute(attribute, newName);
        String oldName = attribute.getName();
        Config config = attribute.getParentConfig();
        if (config == null)
            return false;
        PartitionTable schemaPT = config.getMart().getSchemaPartitionTable();
        int row = schemaPT.getRowNumberByDatasetName(ds.getName());
        if (McUtils.hasPartitionBinding(oldName)) {
            // just reset the value in partitiontable
            int col = McUtils.getPartitionColumnValue(oldName);
            schemaPT.setValue(row, col, newName);
        } else {
            // create a new partitioncolumn
            int col = schemaPT.addColumn(oldName);
            schemaPT.setValue(row, col, newName);
            String name = "(p0c" + col + ")";
            this.renameAttribute(attribute, name);
        }
        ObjectController oc = new ObjectController();
        oc.generateRDF(MartController.getInstance().getMainAttribute(config.getMart()), config, false);
        return true;
    }

    private boolean renameFilter(Filter filter, String newName) {
        // get all references
        if (filter.getName().equals(newName)) {
            return false;
        }
        Config config = filter.getParentConfig();
        // check name conflict
        if (null != config.getFilterByName(null, newName, true)) {
            JOptionPane.showMessageDialog(null, "name conflic");
            return false;
        }
        List<Filter> references = filter.getReferences();
        String oldName = filter.getName();

        config.removeFromFilterMap(filter);
        filter.setProperty(XMLElements.NAME, newName);
        config.addToFilterMap(filter);
        for (Filter fil : references) {
            if (fil.isPointer()) {
                fil.setProperty(XMLElements.POINTEDFILTER, newName);
            } else if (fil.isFilterList()) {
                String alStr = fil.getFilterListString();
                String[] alArrays = alStr.split(",");
                List<String> alList = Arrays.asList(alArrays);
                int index = alList.indexOf(oldName);
                alList.set(index, newName);
                fil.setFilterListString(McUtils.StrListToStr(alList, ","));
            }
        }
        ObjectController oc = new ObjectController();
        oc.generateRDF(MartController.getInstance().getMainAttribute(config.getMart()), config, false);
        return true;
    }

    public boolean renameFilter(Filter filter, String newName, Dataset ds) {
        if (ds == null)
            this.renameFilter(filter, newName);
        else {
            String oldName = filter.getName();
            Config config = filter.getParentConfig();
            if (config == null)
                return false;
            PartitionTable schemaPT = config.getMart().getSchemaPartitionTable();
            int row = schemaPT.getRowNumberByDatasetName(ds.getName());
            if (McUtils.hasPartitionBinding(oldName)) {
                // just reset the value in partitiontable
                int col = McUtils.getPartitionColumnValue(oldName);
                schemaPT.setValue(row, col, newName);
            } else {
                // create a new partitioncolumn
                int col = schemaPT.addColumn(oldName);
                schemaPT.setValue(row, col, newName);
                String name = "(p0c" + col + ")";
                this.renameFilter(filter, name);
            }
            ObjectController oc = new ObjectController();
            oc.generateRDF(MartController.getInstance().getMainAttribute(config.getMart()), config, false);
        }

        return true;
    }

    public boolean renameContainer(Container container, String newName) {
        if (container.getName().equals(newName))
            return false;
        // check name conflict
        if (null != container.getParentConfig().getContainerByName(newName)) {
            JOptionPane.showMessageDialog(null, "name conflic");
            return false;
        }
        container.setName(newName);
        return true;
    }

    public List<Attribute> getReferencedAttributeForColumn(Config config, DatasetColumn dsc) {
        List<Attribute> result = new ArrayList<Attribute>();
        List<Attribute> allAtts = config.getAttributes(new ArrayList<String>(), true, true);
        for (Attribute att : allAtts) {
            if (att.getDataSetColumn() != null && att.getDataSetColumn().equals(dsc))
                result.add(att);
        }
        return result;
    }

    public List<Filter> getReferencedFilterForAttribute(Config config, Attribute att) {
        List<Filter> result = new ArrayList<Filter>();
        List<Filter> allFilters = config.getFilters(new ArrayList<String>(), true, true);
        for (Filter filter : allFilters) {
            if (filter.getAttribute() != null && filter.getAttribute().equals(att))
                result.add(filter);
        }
        return result;
    }

    /**
     * return the attribute associate with the biggest main table's (submain) primary key. assuming that there is only
     * one attribute, if more than one, return the first one.
     * 
     * @param mart
     * @return
     */
    public Attribute getMainAttribute(Mart mart) {
        List<DatasetTable> dts = mart.getOrderedMainTableList();
        // for(int i=dts.size()-1;i>=0;i--) {
        for (DatasetTable dt : dts) {
            // DatasetTable dt = dts.get(i);
            PrimaryKey pk = dt.getPrimaryKey();
            if (pk == null)
                continue;
            DatasetColumn dsc = (DatasetColumn) pk.getColumns().get(0);
            List<Attribute> attributes = dsc.getReferences();
            if (McUtils.isCollectionEmpty(attributes))
                continue;
            return attributes.get(0);
        }
        return null;
    }

    public boolean hasSubMain(DatasetTable dst) {
        if (dst.getType() == DatasetTableType.DIMENSION)
            return false;
        for (Relation r : dst.getRelations()) {
            if (r.isSubclassRelation(null) && r.getFirstKey().getTable().equals(dst)) {
                return true;
            }
        }
        return false;
    }

    // Pass in a list object to hold table and column with orphan foreign key
    private boolean findOrphanFKFromDB(List<ForeignKey> orphanKeyList, StringBuffer orphanSearch, Mart mart)
            throws Exception {
        List<SourceSchema> sss = mart.getIncludedSchemas();
        // assuming that only one for now
        if (McUtils.isCollectionEmpty(sss))
            return false;
        SourceSchema ss = sss.get(0);

        String catalog = ss.getConnection().getCatalog();
        Map<String, Collection<Relation>> orphanFK = new HashMap<String, Collection<Relation>>();

        List<String> dbcols;
        boolean foundOrphanFK = false;
        StringBuffer result = orphanSearch;

        // List missTableList = new ArrayList();

        Map<String, Map<String, List<String>>> dbTableColMap = this.getDBTableColsMap(ss.getConnection());

        // ResultSet dbTableSet = getTablesFromDB();
        // Map<String,Set<String>> tableColMap = getDBTableColumnCollection(dbTableSet);
        // dbTableSet.close();

        // String missingTable = "Missing Table";

        // Loop through each foreign key in the GUI model tables
        for (final Table t : ss.getTables()) {
            // Find the hashset of columns in corresponding DB table
            // check only the first schema for now, assume that they are all the same
            dbcols = dbTableColMap.get(catalog).get(t.getName());
            // Tables dropped or renamed is handled inside sync process
            if (dbcols == null) {
                // missTableList.add(t.getName());

                boolean foundRel = addTableKeysToOrphanList(t, orphanFK);
                if (foundRel) {
                    foundOrphanFK = true;
                }
                continue;

            }

            for (final ForeignKey k : t.getForeignKeys()) {
                for (int kcl = 0; kcl < k.getColumns().size(); kcl++)

                    // If there is no matching column in the DB table, the
                    // key is orphan
                    if (!dbcols.contains(k.getColumns().get(kcl).getName())) {

                        foundOrphanFK = true;
                        orphanKeyList.add(k);

                        orphanFK.put(k.getColumns().get(kcl).getName(), k.getRelations());
                        Log.debug("found orphan foreign key" + k.toString() + " in table " + t.toString());
                    }
            }

        }
        result.append("Orphan Relation: ");
        if (foundOrphanFK) {

            // Output missingt table
            // result.append("Missing Table: " + missTableList.toString());
            result.append(orphanFK.toString());
        }

        return foundOrphanFK;

    }

    private Map<String, Map<String, List<String>>> getDBTableColsMap(Connection con) throws SQLException {
        Map<String, Map<String, List<String>>> dbTableColMap = new HashMap<String, Map<String, List<String>>>();
        Map<String, List<String>> tblColMap = new HashMap<String, List<String>>();
        List<String> colList = new ArrayList<String>();
        final String catalog = con.getCatalog();
        StringBuffer sqlSB = new StringBuffer(
                "select table_schema,table_name,column_name,column_key from information_schema.columns where ");
        sqlSB.append("table_schema='" + catalog + "' order by table_schema,table_name, ordinal_position");

        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sqlSB.toString());
            String schemaName;
            String tableName;
            String lastTableName = null;
            String lastSchemaName = null;
            while (rs.next()) {
                schemaName = rs.getString("table_schema");
                tableName = rs.getString("table_name");
                // finish all columns in one table and move to the next, if previous table doesn't have a PK,
                // create using keyguessing
                if (!tableName.equals(lastTableName)) {
                    if (lastTableName != null) {
                        tblColMap.put(lastTableName, colList);
                        colList = new ArrayList<String>();
                    }
                    // change schema
                    if (lastSchemaName != null) {
                        if (!lastSchemaName.equals(schemaName)) {
                            dbTableColMap.put(lastSchemaName, tblColMap);
                            tblColMap = new HashMap<String, List<String>>();
                        }
                    }
                    // move to next table
                    // clean flags
                    lastTableName = tableName;
                    lastSchemaName = schemaName;
                }
                colList.add(rs.getString("column_name"));
            }
            if (null == dbTableColMap.get(lastSchemaName)) {
                dbTableColMap.put(lastSchemaName, tblColMap);
            }
            rs.close();

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return dbTableColMap;

    }

    private boolean addTableKeysToOrphanList(Table t, Map<String, Collection<Relation>> orphanFK) {

        boolean foundRel = false;
        // Add primary key to orphan key hash set
        final PrimaryKey pmk = t.getPrimaryKey();
        // orphanKeyList.add(k);
        if (pmk != null) {
            for (int kcl = 0; kcl < pmk.getColumns().size(); kcl++) {
                String colName = pmk.getColumns().get(kcl).getName();
                Collection<Relation> relCollection = pmk.getRelations();
                if (!McUtils.isCollectionEmpty(relCollection)) {
                    foundRel = true;
                    orphanFK.put(colName, relCollection);
                }

            }
        }
        // If the table has foreign key, then it must contain relations
        Set<ForeignKey> fkCollection = t.getForeignKeys();
        if (fkCollection != null && fkCollection.size() > 0) {
            foundRel = true;

            // Add foreign keys to orphan key hash set
            for (final ForeignKey k : t.getForeignKeys()) {
                // orphanKeyList.add(k);
                for (int kcl = 0; kcl < k.getColumns().size(); kcl++)
                    orphanFK.put(k.getColumns().get(kcl).getName(), k.getRelations());
            }
        }

        return foundRel;
    }

    public void updateDatasetFromSource(Dataset ds) throws SQLException, DataModelException {
        Mart mart = ds.getParentMart();
        List<SourceSchema> sss = mart.getIncludedSchemas();
        // assuming that only one for now
        if (McUtils.isCollectionEmpty(sss))
            return;
        SourceSchema ss = sss.get(0);

        final DatabaseMetaData dmd = ss.getConnection().getMetaData();
        final String catalog = ss.getConnection().getCatalog();

        // List of objects storing orphan key column and its table name
        List<ForeignKey> orphanFKList = new ArrayList<ForeignKey>();
        StringBuffer orphanSearch = new StringBuffer();
        boolean orphanBool = false;

        /*
         * try { orphanBool = findOrphanFKFromDB(orphanFKList, orphanSearch, mart); if (orphanBool) { Frame frame = new
         * Frame(); Object[] options = { "Proceed", "Abort Synchronization" }; int n = JOptionPane .showOptionDialog(
         * frame,
         * "Some columns in relations no longer exist in source DB. This may be caused by renaming/dropping tables/columns in source DB.\n"
         * +
         * "When choose 'Proceed', you will be prompted to save this information for later use. Do you want to proceed?"
         * +"\n", "Schema Update Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, // do // not //
         * use a // custom // Icon options, // the titles of buttons options[1]); // default button title if (n ==
         * JOptionPane.NO_OPTION) { return; } else{ new SaveOrphanKeyDialog("Orphan Relation", orphanSearch.toString());
         * } } } catch (Exception e) { e.printStackTrace(); } // Now that user decides to sync GUI model to DB schema,
         * remove orphan foreign key clearOrphanForeignKey(orphanFKList);
         */
        // Create a list of existing tables. During this method, we remove
        // from this list all tables that still exist in the database. At
        // the end of the method, the list contains only those tables
        // which no longer exist, so they will be dropped.
        final Collection<Table> tablesToBeDropped = new HashSet<Table>(ss.getTables());

        // Load tables and views from database, then loop over them.
        ResultSet dbTables;
        String sName = ss.getJdbcLinkObject().getJdbcType().useSchema()
                ? ss.getJdbcLinkObject().getSchemaName() : catalog;

        dbTables = dmd.getTables(catalog, sName, "%", new String[] { "TABLE", "VIEW", "ALIAS", "SYNONYM" });

        // Do the loop.
        final Collection<Table> tablesToBeKept = new HashSet<Table>();
        while (dbTables.next()) {
            // Check schema and catalog.
            final String catalogName = dbTables.getString("TABLE_CAT");
            final String schemaName = dbTables.getString("TABLE_SCHEM");
            String schemaPrefix = schemaName;

            // What is the table called?
            final String dbTableName = dbTables.getString("TABLE_NAME");
            Log.debug("Processing table " + catalogName + "." + dbTableName);

            // Look to see if we already have a table by this name defined.
            // If we do, reuse it. If not, create a new table.
            Table dbTable = ss.getTableByName(dbTableName);
            if (dbTable == null)
                try {
                    dbTable = new SourceTable(ss, dbTableName);
                    dbTable.setVisibleModified(true);
                    ss.addTable(dbTable);
                } catch (final Throwable t) {
                    throw new BioMartError(t);
                }
            // Add schema prefix to list.
            if (schemaPrefix != null)
                dbTable.addInPartitions(schemaPrefix);

            // Table exists, so remove it from our list of tables to be
            // dropped at the end of the method.
            tablesToBeDropped.remove(dbTable);
            tablesToBeKept.add(dbTable);
        }
        dbTables.close();

        // Loop over all columns.
        for (final Iterator<Table> i = tablesToBeKept.iterator(); i.hasNext();) {
            final Table dbTable = (Table) i.next();
            final String dbTableName = dbTable.getName();
            // Make a list of all the columns in the table. Any columns
            // remaining in this list by the end of the loop will be
            // dropped.
            final Collection<Column> colsToBeDropped = new HashSet<Column>(dbTable.getColumnList());

            // Clear out the existing schema partition info on all cols.
            for (final Iterator<Column> j = dbTable.getColumnList().iterator(); j.hasNext();)
                ((Column) j.next()).cleanInPartitions();

            // Load the table columns from the database, then loop over
            // them.
            Log.debug("Loading table column list for " + dbTableName);
            ResultSet dbTblCols;

            dbTblCols = dmd.getColumns(catalog, sName, dbTableName, "%");

            // FIXME: When using Oracle, if the table is a synonym then the
            // above call returns no results.
            while (dbTblCols.next()) {
                // Check schema and catalog.
                final String catalogName = dbTblCols.getString("TABLE_CAT");
                final String schemaName = dbTblCols.getString("TABLE_SCHEM");
                String schemaPrefix = null;
                // No prefix if partitions are empty;
                /*
                 * if (!this.getPartitions().isEmpty()) { if ("".equals(dmd.getSchemaTerm())) // Use catalog name to get
                 * prefix. schemaPrefix = (String) this.getPartitions().get( catalogName); else // Use schema name to
                 * get prefix. schemaPrefix = (String) this.getPartitions().get( schemaName); // Don't want to include
                 * if prefix is still null. if (schemaPrefix == null) continue; }
                 */

                // What is the column called, and is it nullable?
                final String dbTblColName = dbTblCols.getString("COLUMN_NAME");
                Log.debug("Processing column " + dbTblColName);

                // Look to see if the column already exists on this table.
                // If it does, reuse it. Else, create it.
                Column dbTblCol = (Column) dbTable.getColumnByName(dbTblColName);
                if (dbTblCol == null)
                    try {
                        dbTblCol = new SourceColumn((SourceTable) dbTable, dbTblColName);
                        dbTblCol.setVisibleModified(true);
                        dbTable.addColumn(dbTblCol);
                    } catch (final Throwable t) {
                        throw new BioMartError(t);
                    }

                // Column exists, so remove it from our list of columns to
                // be dropped at the end of the loop.
                colsToBeDropped.remove(dbTblCol);
                if (schemaPrefix != null)
                    dbTblCol.addInPartitions(schemaPrefix);
            }
            dbTblCols.close();

            // Drop all columns that are left in the list, as they no longer
            // exist in the database.
            for (final Iterator<Column> j = colsToBeDropped.iterator(); j.hasNext();) {
                final Column column = (Column) j.next();
                Log.debug("Dropping redundant column " + column.getName());
                dbTable.getColumnList().remove(column);
            }

        }

        // Remove from schema all tables not found in the database, using
        // the list we constructed above.
        for (final Iterator<Table> i = tablesToBeDropped.iterator(); i.hasNext();) {
            final Table existingTable = (Table) i.next();
            Log.debug("Dropping redundant table " + existingTable);
            final String tableName = existingTable.getName();
            // By clearing its keys we will also clear its relations.
            for (final Iterator<Key> j = existingTable.getKeys().iterator(); j.hasNext();) {
                j.next().removeAllRelations();
            }
            existingTable.setPrimaryKey(null);
            existingTable.getForeignKeys().clear();
            ss.removeTableByName(tableName);
        }

        // Get and create primary keys.
        // Work out a list of all foreign keys currently existing.
        // Any remaining in this list later will be dropped.
        final Collection<ForeignKey> fksToBeDropped = new HashSet<ForeignKey>();
        for (final Iterator<Table> i = ss.getTables().iterator(); i.hasNext();) {
            final Table t = (Table) i.next();
            fksToBeDropped.addAll(t.getForeignKeys());

            // Obtain the primary key from the database. Even in databases
            // without referential integrity, the primary key is still
            // defined and can be obtained from the metadata.
            Log.debug("Loading table primary keys");
            String searchCatalog = catalog;
            String searchSchema = sName;
            /*
             * if (!t.getSchemaPartitions().isEmpty()) { // Locate partition with first prefix. final String prefix =
             * (String) t.getSchemaPartitions() .iterator().next(); String schemaName = (String) new InverseMap(this
             * .getPartitions()).get(prefix); if (schemaName == null) // Should never happen. throw new BioMartError();
             * if ("".equals(dmd.getSchemaTerm())) searchCatalog = schemaName; searchSchema = schemaName; }
             */
            final ResultSet dbTblPKCols = dmd.getPrimaryKeys(searchCatalog, searchSchema, t.getName());

            // Load the primary key columns into a map keyed by column
            // position.
            // In other words, the first column in the key has a map key of
            // 1, and so on. We do this because we can't guarantee we'll
            // read the key columns from the database in the correct order.
            // We keep the map sorted, so that when we iterate over it later
            // we get back the columns in the correct order.
            final Map<Short, Column> pkCols = new TreeMap<Short, Column>();
            while (dbTblPKCols.next()) {
                final String pkColName = dbTblPKCols.getString("COLUMN_NAME");
                final Short pkColPosition = new Short(dbTblPKCols.getShort("KEY_SEQ"));
                pkCols.put(pkColPosition, t.getColumnByName(pkColName));
            }
            dbTblPKCols.close();

            // Did DMD find a PK? If not, which is really unusual but
            // potentially may happen, attempt to find one by looking for a
            // single column with the same name as the table or with '_id'
            // appended.
            // Only do this if we are using key-guessing.
            if (pkCols.isEmpty() && ss.getJdbcLinkObject().isKeyGuessing()) {
                Log.debug("Found no primary key, so attempting to guess one");
                // Plain version first.
                Column candidateCol = (Column) t.getColumnByName(t.getName());
                // Try with '_id' appended if plain version turned up
                // nothing.
                if (candidateCol == null)
                    candidateCol = (Column) t.getColumnByName(t.getName() + Resources.get("primaryKeySuffix"));
                // Found something? Add it to the primary key columns map,
                // with a dummy key of 1. (Use Short for the key because
                // that
                // is what DMD would have used had it found anything
                // itself).
                if (candidateCol != null)
                    pkCols.put(Short.valueOf("1"), candidateCol);
            }

            // Obtain the existing primary key on the table, if the table
            // previously existed and even had one in the first place.
            final PrimaryKey existingPK = t.getPrimaryKey();

            // Did we find a PK on the database copy of the table?
            if (!pkCols.isEmpty()) {

                // Yes, we found a PK on the database copy of the table. So,
                // create a new key based around the columns we identified.
                PrimaryKey candidatePK;
                try {
                    candidatePK = new PrimaryKey(new ArrayList<Column>(pkCols.values()));
                } catch (final Throwable th) {
                    throw new BioMartError(th);
                }

                // If the existing table has no PK, or has a PK which
                // matches and is not incorrect, or has a PK which does not
                // match
                // and is not handmade, replace that PK with the one we
                // found.
                // This way we preserve any existing handmade PKs, and don't
                // override any marked as incorrect.
                try {
                    if (existingPK == null)
                        t.setPrimaryKey(candidatePK);
                    else if (existingPK.equals(candidatePK) && existingPK.getStatus().equals(ComponentStatus.HANDMADE))
                        existingPK.setStatus(ComponentStatus.INFERRED);
                    else if (!existingPK.equals(candidatePK)
                            && !existingPK.getStatus().equals(ComponentStatus.HANDMADE))
                        t.setPrimaryKey(candidatePK);
                } catch (final Throwable th) {
                    throw new BioMartError(th);
                }
            } else // No, we did not find a PK on the database copy of the
            // table, so that table should not have a PK at all. So if the
            // existing table has a PK which is not handmade, remove it.
            if (existingPK != null && !existingPK.getStatus().equals(ComponentStatus.HANDMADE))
                try {
                    t.setPrimaryKey(null);
                } catch (final Throwable th) {
                    throw new BioMartError(th);
                }
        }

        // Are we key-guessing? Key guess the foreign keys, passing in a
        // reference to the list of existing foreign keys. After this call
        // has completed, the list will contain all those foreign keys which
        // no longer exist, and can safely be dropped.
        if (ss.getJdbcLinkObject().isKeyGuessing())
            this.synchroniseKeysUsingKeyGuessing(ss, fksToBeDropped);
        // Otherwise, use DMD to do the same, also passing in the list of
        // existing foreign keys to be updated as the call progresses. Also
        // pass in the DMD details so it doesn't have to work them out for
        // itself.
        else
            this.synchroniseKeysUsingDMD(ss, fksToBeDropped, dmd, sName, catalog);

        // Drop any foreign keys that are left over (but not handmade ones).
        for (final Iterator<ForeignKey> i = fksToBeDropped.iterator(); i.hasNext();) {
            final Key k = (Key) i.next();
            if (k.getStatus().equals(ComponentStatus.HANDMADE))
                continue;
            Log.debug("Dropping redundant foreign key " + k);
            for (final Iterator<Relation> r = k.getRelations().iterator(); r.hasNext();) {
                final Relation rel = (Relation) r.next();
                rel.getFirstKey().getRelations().remove(rel);
                rel.getSecondKey().getRelations().remove(rel);
            }
            k.getTable().getForeignKeys().remove(k);
        }

        // rebuild mart
        this.rebuildMartFromSource(mart);
    }

    private void clearOrphanForeignKey(List<ForeignKey> orphanFKList) {
        for (ForeignKey fk : orphanFKList) {
            // Remove the relations for this foreign key
            for (final Relation rel : fk.getRelations()) {
                rel.getFirstKey().getRelations().remove(rel);
                rel.getSecondKey().getRelations().remove(rel);
            }
            // Remove the key from the table
            fk.getTable().getForeignKeys().remove(fk);
        }
    }

    /**
     * Establish foreign keys based purely on database metadata.
     * 
     * @param fksToBeDropped
     *            the list of foreign keys to update as we go along. By the end of the method, the only keys left in
     *            this list should be ones that no longer exist in the database and may be dropped.
     * @param dmd
     *            the database metadata to obtain the foreign keys from.
     * @param schema
     *            the database schema to read metadata from.
     * @param catalog
     *            the database catalog to read metadata from.
     * @param stepSize
     *            the progress step size to increment by.
     * @throws SQLException
     *             if there was a problem talking to the database.
     * @throws DataModelException
     *             if there was a logical problem during construction of the set of foreign keys.
     */
    /**
     * @param fksToBeDropped
     * @param dmd
     * @param schema
     * @param catalog
     * @param stepSize
     * @throws SQLException
     * @throws DataModelException
     */
    public void synchroniseKeysUsingDMD(final SourceSchema ss, final Collection<ForeignKey> fksToBeDropped,
            final DatabaseMetaData dmd, final String schema, final String catalog) throws SQLException,
            DataModelException {
        Log.debug("Running DMD key synchronisation");
        // Loop through all the tables in the database, which is the same
        // as looping through all the primary keys.
        Log.debug("Finding tables");
        for (final Iterator<Table> i = ss.getTables().iterator(); i.hasNext();) {

            // Obtain the table and its primary key.
            final SourceTable pkTable = (SourceTable) i.next();
            final PrimaryKey pk = pkTable.getPrimaryKey();
            // Skip all tables which have no primary key.
            if (pk == null)
                continue;

            Log.debug("Processing primary key " + pk);

            // Make a list of relations that already exist in this schema,
            // from some previous run. Any relations that are left in this
            // list by the end of the loop for this table no longer exist in
            // the database, and will be dropped.
            final Collection<Relation> relationsToBeDropped = new TreeSet<Relation>(pk.getRelations()); // Tree for
                                                                                                        // order

            // Identify all foreign keys in the database metadata that refer
            // to the current primary key.
            Log.debug("Finding referring foreign keys");
            String searchCatalog = catalog;
            String searchSchema = schema;
            final ResultSet dbTblFKCols = dmd.getExportedKeys(searchCatalog, searchSchema, pkTable.getName());

            // Loop through the results. There will be one result row per
            // column per key, so we need to build up a set of key columns
            // in a map.
            // The map keys represent the column position within a key. Each
            // map value is a list of columns. In essence the map is a 2-D
            // representation of the foreign keys which refer to this PK,
            // with the keys of the map (Y-axis) representing the column
            // position in the FK, and the values of the map (X-axis)
            // representing each individual FK. In all cases, FK columns are
            // assumed to be in the same order as the PK columns. The map is
            // sorted by key column position.
            // An assumption is made that the query will return columns from
            // the FK in the same order as all other FKs, ie. all column 1s
            // will be returned before any 2s, and then all 2s will be
            // returned
            // in the same order as the 1s they are associated with, etc.
            final TreeMap<Short, List<Column>> dbFKs = new TreeMap<Short, List<Column>>();
            while (dbTblFKCols.next()) {
                final String fkTblName = dbTblFKCols.getString("FKTABLE_NAME");
                final String fkColName = dbTblFKCols.getString("FKCOLUMN_NAME");
                final Short fkColSeq = new Short(dbTblFKCols.getShort("KEY_SEQ"));
                if (fkTblName != null && fkTblName.contains("$")) { // exclude ORACLE's temporary tables (unlikely to be
                                                                    // found here though)
                    continue;
                }

                // Note the column.
                if (!dbFKs.containsKey(fkColSeq))
                    dbFKs.put(fkColSeq, new ArrayList<Column>());
                // In some dbs, FKs can be invalid, so we need to check
                // them.
                final Table fkTbl = ss.getTableByName(fkTblName);
                if (fkTbl != null) {
                    final Column fkCol = (Column) fkTbl.getColumnByName(fkColName);
                    if (fkCol != null)
                        (dbFKs.get(fkColSeq)).add(fkCol);
                }
            }
            dbTblFKCols.close();

            // Sort foreign keys by name (case insensitive)
            for (List<Column> columnList : dbFKs.values()) {
                Collections.sort(columnList);
            }

            // Only construct FKs if we actually found any.
            if (!dbFKs.isEmpty()) {
                // Identify the sequence of the first column, which may be 0
                // or 1, depending on database implementation.
                final int firstColSeq = ((Short) dbFKs.firstKey()).intValue();

                // How many columns are in the PK?
                final int pkColCount = pkTable.getPrimaryKey().getColumns().size();

                // How many FKs do we have?
                final int fkCount = dbFKs.get(dbFKs.firstKey()).size();

                // Loop through the FKs, and construct each one at a time.
                for (int j = 0; j < fkCount; j++) {
                    // Set up an array to hold the FK columns.
                    final List<Column> candidateFKColumns = new ArrayList<Column>();

                    // For each FK column name, look up the actual column in
                    // the table.
                    for (final Iterator<Map.Entry<Short, List<Column>>> k = dbFKs.entrySet().iterator(); k.hasNext();) {
                        final Map.Entry<Short, List<Column>> entry = k.next();
                        final Short keySeq = (Short) entry.getKey();
                        // Convert the db-specific column index to a
                        // 0-indexed figure for the array of fk columns.
                        final int fkColSeq = keySeq.intValue() - firstColSeq;
                        candidateFKColumns.add((Column) (entry.getValue()).get(j));
                    }

                    // Create a template foreign key based around the set
                    // of candidate columns we found.
                    ForeignKey fkObject;
                    try {
                        List<Column> columns = new ArrayList<Column>();
                        for (int k = 0; k < candidateFKColumns.size(); k++) {
                            columns.add(candidateFKColumns.get(k));
                        }
                        fkObject = new ForeignKey(columns);
                        // new KeyController(fkObject);
                    } catch (final Throwable t) {
                        throw new BioMartError(t);
                    }
                    final Table fkTable = fkObject.getTable();

                    // If any FK already exists on the target table with the
                    // same columns in the same order, then reuse it.
                    boolean fkAlreadyExists = false;
                    for (final Iterator<ForeignKey> f = fkTable.getForeignKeys().iterator(); f.hasNext()
                            && !fkAlreadyExists;) {
                        final ForeignKey candidateFK = f.next();
                        if (candidateFK.equals(fkObject)) {
                            // Found one. Reuse it!
                            fkObject = candidateFK;
                            // Update the status to indicate that the FK is
                            // backed by the database, if previously it was
                            // handmade.
                            if (fkObject.getStatus().equals(ComponentStatus.HANDMADE))
                                fkObject.setStatus(ComponentStatus.INFERRED);
                            // Remove the FK from the list to be dropped
                            // later, as it definitely exists now.
                            fksToBeDropped.remove(candidateFK);
                            // Flag the key as existing.
                            fkAlreadyExists = true;
                        }
                    }

                    // Has the key been reused, or is it a new one?
                    if (!fkAlreadyExists)
                        try {
                            fkTable.getForeignKeys().add(fkObject);
                            // fkTable.getForeignKeys().add(fk);
                        } catch (final Throwable t) {
                            throw new BioMartError(t);
                        }

                    // Work out whether the relation from the FK to
                    // the PK should be 1:M or 1:1. The rule is that
                    // it will be 1:M in all cases except where the
                    // FK table has a PK with identical columns to
                    // the FK, in which case it is 1:1, as the FK
                    // is unique.
                    Cardinality card = Cardinality.MANY_A;
                    final PrimaryKey fkPK = fkTable.getPrimaryKey();
                    if (fkPK != null && fkObject.getColumns().equals(fkPK.getColumns()))
                        card = Cardinality.ONE;

                    // Check to see if it already has a relation.
                    boolean relationExists = false;
                    for (final Iterator<Relation> f = fkObject.getRelations().iterator(); f.hasNext();) {
                        // Obtain the next relation.
                        final Relation candidateRel = f.next();

                        // a) a relation already exists between the FK
                        // and the PK.
                        if (candidateRel.getOtherKey(fkObject).equals(pk)) {
                            // If cardinality matches, make it
                            // inferred. If doesn't match, make it
                            // modified and update original cardinality.
                            try {
                                if (card.equals(candidateRel.getCardinality())) {
                                    if (!candidateRel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                                        candidateRel.setStatus(ComponentStatus.INFERRED);
                                } else {
                                    if (!candidateRel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                                        candidateRel.setStatus(ComponentStatus.MODIFIED);
                                    candidateRel.setOriginalCardinality(card);
                                }
                            } catch (final AssociationException ae) {
                                throw new BioMartError(ae);
                            }
                            // Don't drop it at the end of the loop.
                            relationsToBeDropped.remove(candidateRel);
                            // Say we've found it.
                            relationExists = true;
                        }

                        // b) a handmade relation exists elsewhere which
                        // should not be dropped. All other relations
                        // elsewhere will be dropped.
                        else if (candidateRel.getStatus().equals(ComponentStatus.HANDMADE))
                            // Don't drop it at the end of the loop.
                            relationsToBeDropped.remove(candidateRel);
                    }

                    // If relation did not already exist, create it.
                    if (!relationExists && !pk.equals(fkObject)) {
                        // Establish the relation.
                        try {
                            new RelationSource(pk, fkObject, card);
                            // pk.getObject().addRelation(relation);
                            // fk.getObject().addRelation(relation);
                        } catch (final Throwable t) {
                            throw new BioMartError(t);
                        }
                    }
                }
            }

            // Remove any relations that we didn't find in the database (but
            // leave the handmade ones behind).
            for (final Iterator<Relation> j = relationsToBeDropped.iterator(); j.hasNext();) {
                final Relation r = j.next();
                if (r.getStatus().equals(ComponentStatus.HANDMADE))
                    continue;
                r.getFirstKey().removeRelation(r);
                r.getSecondKey().removeRelation(r);
            }
        }
    }

    /**
     * This method implements the key-guessing algorithm for foreign keys. Basically, it iterates through all known
     * primary keys, and looks for sets of matching columns in other tables, either with the same names or with '_key'
     * appended. Any matching sets found are assumed to be foreign keys with relations to the current primary key.
     * <p>
     * Relations are 1:M, except when the table at the FK end has a PK with identical column to the FK. In this case,
     * the FK is forced to be unique, which implies that it can only partake in a 1:1 relation, so the relation is
     * marked as such.
     * 
     * @param fksToBeDropped
     *            the list of foreign keys to update as we go along. By the end of the method, the only keys left in
     *            this list should be ones that no longer exist in the database and may be dropped.
     * @param stepSize
     *            the progress step size to increment by.
     * @throws SQLException
     *             if there was a problem talking to the database.
     * @throws DataModelException
     *             if there was a logical problem during construction of the set of foreign keys.
     */
    public void synchroniseKeysUsingKeyGuessing(final SourceSchema ss, final Collection<ForeignKey> fksToBeDropped)
            throws SQLException, DataModelException {
        Log.debug("Running non-DMD key synchronisation");
        // Loop through all the tables in the database, which is the same
        // as looping through all the primary keys.
        Log.debug("Finding tables");
        for (final Iterator<Table> i = ss.getTables().iterator(); i.hasNext();) {
            // Obtain the table and its primary key.
            final SourceTable pkTable = (SourceTable) i.next();
            final PrimaryKey pk = pkTable.getPrimaryKey();
            // Skip all tables which have no primary key.
            if (pk == null)
                continue;

            Log.debug("Processing primary key " + pk);

            // If an FK exists on the PK table with the same columns as the
            // PK, then we cannot use this PK to make relations to other
            // tables.
            // This is because the FK shows that this table is not the
            // original source of the data in those columns. Some other
            // table is the original source, so we assume that relations
            // will have been established from that other table instead. So,
            // we skip this table.
            boolean pkIsAlsoAnFK = false;
            for (final Iterator<ForeignKey> j = pkTable.getForeignKeys().iterator(); j.hasNext() && !pkIsAlsoAnFK;) {
                final ForeignKey fk = j.next();
                if (fk.getColumns().equals(pk.getColumns()))
                    pkIsAlsoAnFK = true;
            }
            if (pkIsAlsoAnFK)
                continue;

            // To maintain some degree of sanity here, we assume that a PK
            // is the original source of data (and not a copy of data
            // sourced from some other table) if the first column in the PK
            // has the same name as the table it is in, or with '_id'
            // appended, or is just 'id' on its own. Any PK which does not
            // have this property is skipped.
            final Column firstPKCol = pk.getColumns().get(0);
            String firstPKColName = firstPKCol.getName();
            int idPrefixIndex = firstPKColName.indexOf(Resources.get("primaryKeySuffix"));
            // then try uppercase, in Oracle, names are uppercase
            if (idPrefixIndex < 0)
                idPrefixIndex = firstPKColName.toUpperCase().indexOf(Resources.get("primaryKeySuffix").toUpperCase());
            if (idPrefixIndex >= 0)
                firstPKColName = firstPKColName.substring(0, idPrefixIndex);
            if (!firstPKColName.equals(pkTable.getName()) && !firstPKColName.equals(Resources.get("idCol")))
                continue;

            // Make a list of relations that already exist in this schema,
            // from some previous run. Any relations that are left in this
            // list by the end of the loop for this table no longer exist in
            // the database, and will be dropped.
            final Collection<Relation> relationsToBeDropped = new TreeSet<Relation>(pk.getRelations()); // Tree for
                                                                                                        // order

            // Now we know that we can use this PK for certain, look for all
            // other tables (other than the one the PK itself belongs to),
            // for sets of columns with identical names, or with '_key'
            // appended. Any set that we find is going to be an FK with a
            // relation back to this PK.
            Log.debug("Searching for possible referring foreign keys");
            for (final Iterator<Table> l = ss.getTables().iterator(); l.hasNext();) {
                // Obtain the next table to look at.
                final SourceTable fkTable = (SourceTable) l.next();

                // Make sure the table is not the same as the PK table.
                if (fkTable.equals(pkTable))
                    continue;

                // Set up an empty list for the matching columns.
                final List<Column> candidateFKColumns = new ArrayList<Column>();
                int matchingColumnCount = 0;

                // Iterate through the PK columns and find a column in the
                // target FK table with the same name, or with '_key'
                // appended,
                // or with the PK table name and an underscore prepended.
                // If found, add that target column to the candidate FK
                // column
                // set.
                for (int columnIndex = 0; columnIndex < pk.getColumns().size(); columnIndex++) {
                    final String pkColumnName = pk.getColumns().get(columnIndex).getName();
                    // Start out by assuming no match.
                    Column candidateFKColumn = null;
                    // Don't try to find 'id' or 'id_key' columns as that
                    // would be silly and would probably match far too much.
                    if (!pkColumnName.equals(Resources.get("idCol"))) {
                        // Try equivalent name first.
                        candidateFKColumn = fkTable.getColumnByName(pkColumnName);
                        // Then try with '_key' appended, if not found.
                        if (candidateFKColumn == null)
                            candidateFKColumn = fkTable.getColumnByName(pkColumnName
                                    + Resources.get("foreignKeySuffix"));
                    }
                    // Then try with PK tablename+'_' prepended, if not
                    // found.
                    if (candidateFKColumn == null)
                        candidateFKColumn = fkTable.getColumnByName(pkTable.getName() + "_" + pkColumnName);
                    // Found it? Add it to the candidate list.
                    if (candidateFKColumn != null) {
                        candidateFKColumns.add(candidateFKColumn);
                        matchingColumnCount++;
                    }
                }

                // We found a matching set, so create a FK on it!
                if (matchingColumnCount == pk.getColumns().size()) {
                    // Create a template foreign key based around the set
                    // of candidate columns we found.
                    ForeignKey fkObject;
                    try {
                        List<Column> columns = new ArrayList<Column>();
                        for (int k = 0; k < candidateFKColumns.size(); k++) {
                            columns.add(candidateFKColumns.get(k));
                        }
                        fkObject = new ForeignKey(columns);
                        // new KeyController(fkObject);
                    } catch (final Throwable t) {
                        throw new BioMartError(t);
                    }

                    // If any FK already exists on the target table with the
                    // same columns in the same order, then reuse it.
                    boolean fkAlreadyExists = false;
                    for (final Iterator<ForeignKey> f = fkTable.getForeignKeys().iterator(); f.hasNext()
                            && !fkAlreadyExists;) {
                        final ForeignKey candidateFK = f.next();
                        if (candidateFK.equals(fkObject)) {
                            // Found one. Reuse it!
                            fkObject = candidateFK;
                            // Update the status to indicate that the FK is
                            // backed by the database, if previously it was
                            // handmade.
                            if (fkObject.getStatus().equals(ComponentStatus.HANDMADE))
                                fkObject.setStatus(ComponentStatus.INFERRED);
                            // Remove the FK from the list to be dropped
                            // later, as it definitely exists now.
                            fksToBeDropped.remove(candidateFK);
                            // Flag the key as existing.
                            fkAlreadyExists = true;
                        }
                    }

                    // Has the key been reused, or is it a new one?
                    if (!fkAlreadyExists)
                        try {
                            // fkTable.getForeignKeys().add(fk);
                            fkTable.getForeignKeys().add(fkObject);
                        } catch (final Throwable t) {
                            throw new BioMartError(t);
                        }

                    // Work out whether the relation from the FK to
                    // the PK should be 1:M or 1:1. The rule is that
                    // it will be 1:M in all cases except where the
                    // FK table has a PK with identical columns to
                    // the FK, in which case it is 1:1, as the FK
                    // is unique.
                    Cardinality card = Cardinality.MANY_A;
                    final PrimaryKey fkPK = fkTable.getPrimaryKey();
                    if (fkPK != null && fkObject.getColumns().equals(fkPK.getColumns()))
                        card = Cardinality.ONE;

                    // Check to see if it already has a relation.
                    boolean relationExists = false;
                    for (final Iterator<Relation> f = fkObject.getRelations().iterator(); f.hasNext();) {
                        // Obtain the next relation.
                        final Relation candidateRel = f.next();

                        // a) a relation already exists between the FK
                        // and the PK.
                        if (candidateRel.getOtherKey(fkObject).equals(pk)) {
                            // If cardinality matches, make it
                            // inferred. If doesn't match, make it
                            // modified and update original cardinality.
                            try {
                                if (card.equals(candidateRel.getCardinality())) {
                                    if (!candidateRel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                                        candidateRel.setStatus(ComponentStatus.INFERRED);
                                } else {
                                    if (!candidateRel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
                                        candidateRel.setStatus(ComponentStatus.MODIFIED);
                                    candidateRel.setOriginalCardinality(card);
                                }
                            } catch (final AssociationException ae) {
                                throw new BioMartError(ae);
                            }
                            // Don't drop it at the end of the loop.
                            relationsToBeDropped.remove(candidateRel);
                            // Say we've found it.
                            relationExists = true;
                        }

                        // b) a handmade relation exists elsewhere which
                        // should not be dropped. All other relations
                        // elsewhere will be dropped.
                        else if (candidateRel.getStatus().equals(ComponentStatus.HANDMADE))
                            // Don't drop it at the end of the loop.
                            relationsToBeDropped.remove(candidateRel);
                    }

                    // If relation did not already exist, create it.
                    if (!relationExists) {
                        // Establish the relation.
                        try {
                            RelationSource rel = new RelationSource(pk, fkObject, card);
                            // pk.getObject().addRelation(relation);
                            // fk.getObject().addRelation(relation);
                        } catch (final Throwable t) {
                            throw new BioMartError(t);
                        }
                    }
                }
            }

            // Remove any relations that we didn't find in the database (but
            // leave the handmade ones behind).
            for (final Iterator<Relation> j = relationsToBeDropped.iterator(); j.hasNext();) {
                final Relation r = j.next();
                if (r.getStatus().equals(ComponentStatus.HANDMADE))
                    continue;
                r.getFirstKey().removeRelation(r);
                r.getSecondKey().removeRelation(r);
            }
        }
    }

    public void setChanged(boolean b) {
        this.changed = b;
    }

    public boolean isRegistryChanged() {
        return this.changed;
    }

    public void rebuildMartFromSource(Mart mart) {
        try {
            this.synchronise(mart);
        } catch (DataModelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * when a new dataset is added in the partitiontable, some columns are set to empty, they need to be fixed by
     * setting a default value that is similar to other rows. For example, see pointeddataset
     */
    public void fixPartitionTable() {
        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        // get all pointers
        for (Mart mart : registry.getMartList()) {
            for (Config config : mart.getConfigList()) {
                if (config.isMasterConfig())
                    continue;
                for (Attribute att : config.getAllAttributes()) {
                    if (att.isPointer()
                            && McUtils.hasPartitionBinding(att.getPropertyValue(XMLElements.POINTEDDATASET))) {
                        fixPartitionTable(mart, att.getPropertyValue(XMLElements.POINTEDDATASET));
                    }
                }
                for (Filter fil : config.getAllFilters()) {
                    if (fil.isPointer()
                            && McUtils.hasPartitionBinding(fil.getPropertyValue(XMLElements.POINTEDDATASET))) {
                        fixPartitionTable(mart, fil.getPropertyValue(XMLElements.POINTEDDATASET));
                    }
                }
            }
        }
        // fix pointed dataset for all marts
        MartController.getInstance().fixPointedDatasetForLink();
    }

    private void fixPartitionTable(Mart mart, String value) {
        // check if there is missing value in the column
        String pointeddataset = value;
        // assuming that the value is like (p?c?)
        int col = McUtils.getPartitionColumnValue(pointeddataset);
        // assuming it is in the schema partition
        PartitionTable pt = mart.getSchemaPartitionTable();
        Set<Integer> emptyRow = new HashSet<Integer>();
        Map<Integer, String> existValues = new HashMap<Integer, String>();
        for (int i = 0; i < pt.getTotalRows(); i++) {
            String item = pt.getValue(i, col);
            if (McUtils.isStringEmpty(item))
                emptyRow.add(i);
            else if (existValues.isEmpty()) // one is good for now
                existValues.put(i, item);
        }
        if (!emptyRow.isEmpty() && !existValues.isEmpty()) {
            Map.Entry<Integer, String> entry = existValues.entrySet().iterator().next();
            String referenceName = entry.getValue();
            // separate by the last "_"
            int index0 = referenceName.lastIndexOf("_");
            if (index0 >= 0) {
                for (int row : emptyRow) {
                    String mydatasetName = pt.getValue(row, PartitionUtils.DATASETNAME);
                    int index1 = mydatasetName.lastIndexOf("_");
                    if (index1 >= 0) {
                        String datasetName = referenceName.substring(0, index0) + "_"
                                + mydatasetName.substring(index1 + 1);
                        // if datasetname exist in other mart
                        if (McGuiUtils.INSTANCE.getMartFromDataset(datasetName) != null)
                            pt.setValue(row, col, datasetName);
                    }
                }

            }
        }

    }

    public void createNaiveForOrphanColumn(Mart mart) {
        Config master = mart.getMasterConfig();
        Container newAttributeContainer = master.getContainerByName(Resources.get("NEWATTRIBUTE"));
        Container newFilterContainer = master.getContainerByName(Resources.get("NEWFILTER"));
        List<DatasetTable> mainList = mart.getOrderedMainTableList();
        if (mainList.isEmpty())
            return;
        DatasetTable keptMain = mainList.get(mainList.size() - 1);
        for (DatasetTable dst : mart.getDatasetTables()) {
            // if it is main table, only create the submain
            if (dst.getType() != DatasetTableType.DIMENSION) {
                if (!dst.equals(keptMain))
                    continue;
            }

            for (Column col : dst.getColumnList()) {
                if (!((DatasetColumn) col).hasReferences()) {
                    // create a naive attribute and filter for it
                    String baseName = dst.getName() + Resources.get("tablenameSep") + col.getName();
                    String displayName = WordUtils.capitalize(col.getName());
                    displayName = displayName.replaceAll("_", " ");
                    Attribute att = new Attribute((DatasetColumn) col, baseName, displayName);
                    if (newAttributeContainer == null) {
                        newAttributeContainer = new Container(Resources.get("NEWATTRIBUTE"));
                        master.getRootContainer().addContainer(newAttributeContainer);
                    }
                    newAttributeContainer.addAttribute(att);
                    att.setVisibleModified(true);
                    newAttributeContainer.setVisibleModified(true);

                    Filter filter = new Filter(att, att.getName());
                    if (newFilterContainer == null) {
                        newFilterContainer = new Container(Resources.get("NEWFILTER"));
                        master.getRootContainer().addContainer(newFilterContainer);
                    }
                    newFilterContainer.addFilter(filter);
                    newFilterContainer.setVisibleModified(true);
                    filter.setVisibleModified(true);
                }
            }
        }
    }

    /**
     * return the fist source schema's database name. if the source schema is null, (in case of relational mart), return
     * the mart name.
     * 
     * @param mart
     * @return
     */
    public String getFirstDBNameFromMart(Mart mart) {
        List<SourceSchema> sss = mart.getIncludedSchemas();
        if (McUtils.isCollectionEmpty(sss)) {
            return mart.getDatasetList().get(0).getDataLinkInfo().getJdbcLinkObject().getDatabaseName();
        } else {
            return sss.get(0).getDataLinkDatabase();
        }

    }

    /**
     * 1. change partition table 2. all table/column inpartition 3. all links 4. all pointeddataset 5. options
     * 
     * @param mart
     * @param oldvalue
     * @param newvalue
     */
    public void renameDataset(Mart mart, String oldvalue, String newvalue) {
        PartitionTable pt = mart.getSchemaPartitionTable();
        int row = pt.getRowNumberByDatasetName(oldvalue);
        // 2. all table/column inpartition
        for (DatasetTable dst : mart.getDatasetTables()) {
            if (dst.inPartition(oldvalue)) {
                dst.renameInPartition(oldvalue, newvalue);
                for (Column column : dst.getColumnList()) {
                    if (column.inPartition(oldvalue)) {
                        column.renameInPartition(oldvalue, newvalue);
                    }
                }
            }
        }
        // 3. all links
        for (Mart m : mart.getMartRegistry().getMartList()) {
            for (Config config : m.getConfigList()) {
                for (Link link : config.getLinkList()) {
                    String pointeddsStr = link.getPointedDataset();
                    if (McUtils.isStringEmpty(pointeddsStr))
                        continue;
                    if (McUtils.hasPartitionBinding(pointeddsStr))
                        continue;
                    if (McGuiUtils.INSTANCE.inProperty(pointeddsStr, oldvalue)) {
                        link.setPointedDataset(McGuiUtils.INSTANCE.replaceValueInListStr(pointeddsStr, oldvalue,
                                newvalue));
                    }
                }
            }
        }
        // 4. all pointeddataset
        for (Mart m : mart.getMartRegistry().getMartList()) {
            for (Config config : m.getConfigList()) {
                for (Attribute att : config.getAllAttributes()) {
                    if (att.isPointer() && att.getPointedAttribute() != null) {
                        String pointeddsStr = att.getPointedDatasetName();
                        if (McUtils.isStringEmpty(pointeddsStr))
                            continue;
                        if (McUtils.hasPartitionBinding(pointeddsStr))
                            continue;
                        if (McGuiUtils.INSTANCE.inProperty(pointeddsStr, oldvalue)) {
                            att.setProperty(XMLElements.POINTEDDATASET,
                                    McGuiUtils.INSTANCE.replaceValueInListStr(pointeddsStr, oldvalue, newvalue));
                        }
                    }
                }
            }
        }

        for (Mart m : mart.getMartRegistry().getMartList()) {
            for (Config config : m.getConfigList()) {
                for (Filter filter : config.getAllFilters()) {
                    if (filter.isPointer() && filter.getPointedFilter() != null) {
                        String pointeddsStr = filter.getPointedDatasetName();
                        if (McUtils.isStringEmpty(pointeddsStr))
                            continue;
                        if (McUtils.hasPartitionBinding(pointeddsStr))
                            continue;
                        if (McGuiUtils.INSTANCE.inProperty(pointeddsStr, oldvalue)) {
                            filter.setProperty(XMLElements.POINTEDDATASET,
                                    McGuiUtils.INSTANCE.replaceValueInListStr(pointeddsStr, oldvalue, newvalue));
                        }
                    }
                }
            }
        }
        // 5. options
        Options.getInstance().renameDataset(oldvalue, newvalue);
        // 1. change partition table
        pt.setValue(row, PartitionUtils.DATASETNAME, newvalue);
    }

    public void refreshMartForGUI(Mart mart) {

        for (DatasetTable dst : mart.getDatasetTables()) {
            for (Column column : dst.getColumnList()) {
                if (column.isHidden()) {
                    DatasetColumn dsc = (DatasetColumn) column;
                }
            }
        }
    }

    public void requestImportRegistryFromXML(MartRegistry registry, Document document) {
        // TODO Auto-generated method stub
        this.fixObjects(registry);
        Validation.validate(registry, false);
        // load option file
        Element optionsElement = document.getRootElement().getChild(XMLElements.OPTIONS.toString());
        if (optionsElement != null) {
            optionsElement.detach();
            Options.getInstance().setOptions(optionsElement);
        }
    }

    /*
     * when a new dataset added, need to fix the column (in partitiontable) in the pointeddataset in the link
     */
    public void fixPointedDatasetForLink() {
        for (Mart mart : McGuiUtils.INSTANCE.getRegistryObject().getMartList()) {
            List<Link> linkList = mart.getLinkList();
            for (Link link : linkList) {
                String pointedds = link.getPointedDataset();
                if (McUtils.hasPartitionBinding(pointedds)) {
                    int col = McUtils.getPartitionColumnValue(pointedds);
                    PartitionTable pt = mart.getSchemaPartitionTable();
                    String martPrefix = this.findMartInPartitionTable(pt, col);
                    if (McUtils.isStringEmpty(martPrefix))
                        continue;
                    for (int i = 0; i < pt.getTotalRows(); i++) {
                        String item = pt.getValue(i, col);
                        if (McUtils.isStringEmpty(item)) {
                            String datasetName = pt.getValue(i, PartitionUtils.DATASETNAME);
                            int index = datasetName.lastIndexOf(Resources.get("MARTDSSEPARATOR"));
                            if (index > 0) {
                                String suffix = datasetName.substring(index);
                                pt.setValue(i, col, martPrefix + suffix);
                            }
                        }
                    }
                }
            }
        }
    }

    private String findMartInPartitionTable(PartitionTable pt, int col) {
        for (int i = 0; i < pt.getTotalRows(); i++) {
            String item = pt.getValue(i, col);
            if (!McUtils.isStringEmpty(item)) {
                int index = item.lastIndexOf(Resources.get("MARTDSSEPARATOR"));
                if (index > 0) {
                    String dsStr = item.substring(index + 1);
                    String myItem = pt.getValue(i, PartitionUtils.DATASETNAME);
                    int index2 = myItem.lastIndexOf(Resources.get("MARTDSSEPARATOR"));
                    if (index2 > 0) {
                        String mydsStr = myItem.substring(index2 + 1);
                        if (dsStr.equals(mydsStr)) {
                            return item.substring(0, index);
                        }
                    }
                }
            }
        }
        return null;
    }

}
