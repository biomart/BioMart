package org.biomart.queryEngine.queryCompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Settings;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.Table;
import org.biomart.queryEngine.DBType;
import org.biomart.queryEngine.OperatorType;
import org.biomart.queryEngine.QueryElement;
import org.biomart.queryEngine.SubQuery;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author Jonathan Guberman, Arek Kasprzyk
 *
 *         This class prepares SQL query for both Source schema (Virtual Mart) based querying as well as Materialised
 *         schemas (Marts). A vital component of query compilation which is used for ON THE FLY extension of WHERE
 *         clause for dataset joining, sits in the SubQuery object.
 */
public class QueryCompiler {

    // added by Yong for QC testing
    /**
     * qcPath is a list of org.biomart.builder.model.Table org.biomart.builder.model.Relation
     *
     * methods may be used by QC Table.getName() Table.getColumns() Relation.getFirstKey.getName()
     * Relation.getSecondKey.getName()
     */
    // private List<Object> qcPath;
    private Map<DatasetTable, List<Object>> qcPathMap;
    private List<Attribute> selectedAttributes;
    private Map<Filter, String> selectedFilters;
    @SuppressWarnings("unused")
    private Boolean leftFlag;
    private final String only = "NOT NULL";
    private final String excluded = "NULL";
    private String quoteChar = "";

    /**
     *
     * @param dbName
     * @param ds
     * @return
     */
    public String searchMaterilizedSchema(String dbName, Dataset ds) {
        boolean leftFlag = ("true".equalsIgnoreCase(Settings.getProperty("leftjoin"))
                ? true : false);
        List<String> dss = new ArrayList<String>();
        dss.add(ds.getName());

        // Create list of all main tables
        List<DatasetTable> allTmpMainTables = selectedAttributes.iterator().next().getDatasetTable().getMart()
                .getOrderedMainTableList();
        List<DatasetTable> allMainTables = new ArrayList<DatasetTable>();
        // if datasettable is partitioned, replace it to a real datasettable
        for (DatasetTable tmpDst : allTmpMainTables) {
            allMainTables.add(tmpDst);
        }
        /*
         * for(Table table : mart.getTableList()){ if(table.getMain()){ allMainTables.add(table); } }
         */

        HashSet<Column> columnsToCheck = new HashSet<Column>();
        HashSet<DatasetTable> mainTables = new HashSet<DatasetTable>();
        HashSet<DatasetTable> dmTables = new HashSet<DatasetTable>();

        StringBuilder querySQL = new StringBuilder("SELECT "); // TODO: use newly created (DatabaseDialect.SELECT + " ")
                                                               // instead of hardcoded (likewise for other SQL keywords
                                                               // in this class)

        /*
         * Loop through all the attributes, adding them to either the list of main tables or the list of dm tables
         */
        for (Attribute attribute : selectedAttributes) {
            DatasetTable table = attribute.getDatasetTable();
            if (table.getType().equals(DatasetTableType.DIMENSION)) {
                dmTables.add(table);

                for (Column dsc : table.getRelations().iterator().next()
                        .getOtherKey(table.getForeignKeys().iterator().next()).getColumns()) {
                    columnsToCheck.add(dsc);
                }

            } else {
                mainTables.add(table);
                columnsToCheck.add(attribute.getDataSetColumn());
            }
        }
        /*
         * Loop through all the filters, finding their corresponding attributes, and then adding those to either the
         * list of main tables or the list of dm tables
         */
        for (Filter filter : selectedFilters.keySet()) {
            if (filter.isFilterList()) {
                for (Filter subFilter : filter.getFilterList(dss)) {
                    findTableType(columnsToCheck, mainTables, dmTables, subFilter, ds);
                }
            } else {
                findTableType(columnsToCheck, mainTables, dmTables, filter, ds);
            }
        }

        /*
         * Figure out what table should be the main table by checking if it contains any of the necessary keys. If it
         * doesn't contain any, move to the next smaller main table. Otherwise, it is our main.
         */
        // TODO fix this: ordering of main tables must be guaranteed
        String mainTableName = null;
        outer: for (DatasetTable table : allMainTables) {
            // only compare the name, not the column object
            Set<String> allcolumns = new HashSet<String>();
            for (Column col : table.getColumnList(ds.getName())) {
                allcolumns.add(col.getName());
            }
            Set<String> columnNamesToCheck = new HashSet<String>();
            for (Column col : columnsToCheck) {
                columnNamesToCheck.add(col.getName());
            }
            if (allcolumns.containsAll(columnNamesToCheck)) {
                mainTableName = table.getSubPartitionCommaList(ds.getName());
                // get real table name
                if (McUtils.hasPartitionBinding(mainTableName))
                    mainTableName = McUtils.getRealName(mainTableName, ds);
                break outer;
            }
            /*
             * for (Column column : columnsToCheck) { if (table.getColumnList().contains(column)) {
             * System.err.println(column.getName()); mainTableName = table.getName();//table.getMaterializedName();
             * break outer; } }
             */
        }

        // For getting totals
        String totalMainTableName = null;

        for (DatasetTable table : allMainTables) {
            if (table.getType().equals(DatasetTableType.MAIN)) {
                totalMainTableName = table.getName(ds.getName());
            }
        }

        Log.info("Using main table: " + mainTableName);

        if (isCountQuery) {
            querySQL.append(" COUNT(*) AS count ");
        } else {
            /*
             * Loop through the attributes, adding them to the SELECT clause of the query
             */
            for (Attribute attribute : selectedAttributes) {
                DatasetColumn column = attribute.getDataSetColumn();
                // String tableName = column.getTable().getName();
                DatasetTable table = attribute.getDatasetTable();
                String tableName = table.getSubPartitionCommaList(ds.getName());
                if (McUtils.hasPartitionBinding(tableName)) {
                    tableName = McUtils.getRealName(tableName, ds);
                }
                if (table.isMain()) {
                    querySQL.append("main.");
                } else {
                    querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                }
                querySQL.append(quoteChar + column.getName() + quoteChar);
                querySQL.append(", ");
            } // for
              // Clean up the final comma if necessary
            if (selectedAttributes.size() > 0) {
                querySQL.deleteCharAt(querySQL.length() - 2);
            }
        }

        // Add all of the dm tables and the main table to the FROM clause
        querySQL.append(" FROM ");
        if (leftFlag) {
            querySQL.append(dbName + "." + quoteChar + mainTableName + quoteChar + " main ");
            for (DatasetTable table : dmTables) {
                Key key = table.getForeignKeys().iterator().next();
                for (Column column : key.getColumns()) {
                    String keyName = column.getName(); // mart.getTable(table).getKey().getName();
                    String tableName = table.getName();
                    if (McUtils.hasPartitionBinding(tableName))
                        tableName = McUtils.getRealName(tableName, ds);
                    String fullTable = dbName + "." + quoteChar + tableName + quoteChar;
                    querySQL.append(" LEFT JOIN " + fullTable + " on main." + quoteChar + keyName + quoteChar + "="
                            + fullTable + "." + quoteChar + keyName + quoteChar);
                }
            }
        } else {
            HashSet<String> tableNames = new HashSet<String>();
            for (DatasetTable table : dmTables) {
                String tableName = table.getSubPartitionCommaList(ds.getName());
                if (McUtils.hasPartitionBinding(tableName))
                    tableName = McUtils.getRealName(tableName, ds);
                if (!tableNames.contains(tableName)) {
                    querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ", ");
                    tableNames.add(tableName);
                }
            }
            querySQL.append(dbName + "." + quoteChar + mainTableName + quoteChar + " main ");
        } // if

        /*
         * Loop through the filters, adding them to the WHERE clause
         */
        if (selectedFilters.size() > 0 || (!leftFlag && dmTables.size() > 0)) {
            querySQL.append(" WHERE ");
        }

        for (Filter filter : selectedFilters.keySet()) {
            // If we're doing a count query then only add filters from main table

            if (filter.isFilterList()) {
                int expectedFilterListValuesSize = filter.getFilterList(dss).size();
                querySQL.append("(");
                String[] splittedSelectedFilters = selectedFilters.get(filter).split("[,\\n\\r]");
                for (String value : splittedSelectedFilters) {
                    String[] splitValue = value.split(filter.getSplitOnValue(), -1);
                    querySQL.append("(");
                    String currentValue = null;
                    if (splitValue.length < expectedFilterListValuesSize) {
                        Log.warn("The number of value tokens of the filter list "+ filter.getName()
                                + " does not match the number of filters inside the filter list");
                    }
                    for (int i = 0; i < splitValue.length; ++i) {
                        currentValue = splitValue[i];
                        if (filter.getSplitOnValue().equals("") || splitValue.length == 1
                                || filter.getQualifier().equals(OperatorType.RANGE)) {
                            currentValue = value;
                        }
                        
                        Filter subFilter = filter.getFilterList(dss).get(i);
                        DatasetTable table = subFilter.getDatasetTable();
                        DatasetTableType tableType = table.getType();
                        String tableName = subFilter.getDatasetTable().getSubPartitionCommaList(ds.getName());
                        if (McUtils.hasPartitionBinding(tableName))
                            tableName = McUtils.getRealName(tableName, ds);

                        if (subFilter.getQualifier() == OperatorType.RANGE) {
                            querySQL.append("(");
                            String operation1 = " >= ";
                            String operation2 = " <= ";

                            String originalValue = selectedFilters.get(subFilter);
                            if (originalValue == null)
                                originalValue = currentValue;
                            String[] values = originalValue.split(subFilter.getSplitOnValue());
                            if (values.length == 1) {
                                values = new String[2];
                                if (originalValue.startsWith(">=")) {
                                    values[0] = originalValue.substring(2);
                                    values[1] = null;
                                    operation1 = " >= ";
                                } else if (originalValue.startsWith("<=")) {
                                    values[0] = null;
                                    values[1] = originalValue.substring(2);
                                    operation2 = " <= ";
                                } else if (originalValue.startsWith(">")) {
                                    values[0] = originalValue.substring(1);
                                    values[1] = null;
                                    operation1 = " > ";
                                } else if (originalValue.startsWith("<")) {
                                    values[0] = null;
                                    values[1] = originalValue.substring(1);
                                    operation2 = " < ";
                                } else {
                                    values[0] = originalValue;
                                    values[1] = null;
                                    operation1 = " = ";
                                }
                            }
                            if (values[0] != null) {
                                if (tableType.equals(DatasetTableType.MAIN)
                                        || tableType.equals(DatasetTableType.MAIN_SUBCLASS)
                                        || tableName.endsWith("main") || tableName.endsWith("main||")) {
                                    querySQL.append("main.");
                                } else {
                                    querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                                }
                                querySQL.append(quoteChar + subFilter.getDatasetColumn().getName() + quoteChar);
                                querySQL.append(operation1 + values[0]);
                            }

                            if (values[0] != null && values[1] != null)
                                querySQL.append(" AND ");

                            if (values[1] != null) {
                                if (tableType.equals(DatasetTableType.MAIN)
                                        || tableType.equals(DatasetTableType.MAIN_SUBCLASS)
                                        || tableName.endsWith("main") || tableName.endsWith("main||")) {
                                    querySQL.append("main.");
                                } else {
                                    querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                                }
                                querySQL.append(quoteChar + subFilter.getDatasetColumn().getName() + quoteChar);
                                querySQL.append(operation2 + values[1] + " ");
                            }
                            querySQL.append(") ");
                        } else if (subFilter.getQualifier() != OperatorType.IS) {
                            if (table.isMain()) {
                                querySQL.append("main.");
                            } else {
                                querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                            }
                            querySQL.append(quoteChar + subFilter.getDatasetColumn().getName() + quoteChar);
                            querySQL.append(" " + subFilter.getQualifier() + " ");
                            querySQL.append("'" + currentValue + "' ");
                        } else {
                            if (table.isMain()) {
                                querySQL.append("main.");
                            } else {
                                querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                            }
                            querySQL.append(quoteChar + subFilter.getDatasetColumn().getName() + quoteChar);
                            querySQL.append(" " + subFilter.getQualifier() + " ");
                            querySQL.append("'" + currentValue + "' ");
                        }
                        if (filter.getFilterOperation() != null)
                            querySQL.append(filter.getFilterOperation() + " ");
                        else
                            querySQL.append("AND ");
                    }
                    if (filter.getFilterOperation() != null)
                        querySQL.delete(querySQL.length() - (filter.getFilterOperation().toString().length() + 1),
                                querySQL.length());
                    else
                        querySQL.delete(querySQL.length() - 4, querySQL.length());
                    querySQL.append(") OR ");

                }
                querySQL.delete(querySQL.length() - 3, querySQL.length());
                querySQL.append(") AND ");
            } else {
                String tableName = filter.getDatasetTable().getSubPartitionCommaList(ds.getName());
                if (McUtils.hasPartitionBinding(tableName))
                    tableName = McUtils.getRealName(tableName, ds);
                querySQL.append("(");
                String[] splitFilter = selectedFilters.get(filter).split("[,\\n\\r]");
                DatasetTable table = filter.getDatasetTable();
                DatasetTableType tableType = table.getType();
                if (filter.getQualifier() == OperatorType.E && splitFilter.length > 1) {
                    if (table.isMain()) {
                        querySQL.append("main.");
                    } else {
                        querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                    }
                    querySQL.append(quoteChar + filter.getDatasetColumn().getName() + quoteChar);
                    querySQL.append(" IN (");
                    for (String value : splitFilter) {
                        querySQL.append("'" + value.trim() + "',");
                    }
                    querySQL.deleteCharAt(querySQL.length() - 1);
                    querySQL.append(")");
                } else if (filter.getQualifier() == OperatorType.RANGE) {
                    String combineOperator = filter.getFilterOperation() == null
                            ? " AND " : " " + filter.getFilterOperation().toString() + " ";
                    for (String originalValue : splitFilter) {
                        String operation1 = " >= ";
                        String operation2 = " <= ";

                        String[] values = originalValue.split(filter.getSplitOnValue());
                        if (values.length == 1) {
                            values = new String[2];
                            if (originalValue.startsWith(">=")) {
                                values[0] = originalValue.substring(2);
                                values[1] = null;
                                operation1 = " >= ";
                            } else if (originalValue.startsWith("<=")) {
                                values[0] = null;
                                values[1] = originalValue.substring(2);
                                operation2 = " <= ";
                            } else if (originalValue.startsWith(">")) {
                                values[0] = originalValue.substring(1);
                                values[1] = null;
                                operation1 = " > ";
                            } else if (originalValue.startsWith("<")) {
                                values[0] = null;
                                values[1] = originalValue.substring(1);
                                operation2 = " < ";
                            } else {
                                values[0] = originalValue;
                                values[1] = null;
                                operation1 = " = ";
                            }
                        }
                        if (values[0] != null) {
                            if (tableType.equals(DatasetTableType.MAIN)
                                    || tableType.equals(DatasetTableType.MAIN_SUBCLASS) || tableName.endsWith("main")
                                    || tableName.endsWith("main||")) {
                                querySQL.append("main.");
                            } else {
                                querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                            }
                            querySQL.append(quoteChar + filter.getDatasetColumn().getName() + quoteChar);
                            querySQL.append(operation1 + values[0]);
                        }

                        if (values[0] != null && values[1] != null)
                            querySQL.append(" AND ");

                        if (values[1] != null) {
                            if (tableType.equals(DatasetTableType.MAIN)
                                    || tableType.equals(DatasetTableType.MAIN_SUBCLASS) || tableName.endsWith("main")
                                    || tableName.endsWith("main||")) {
                                querySQL.append("main.");
                            } else {
                                querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                            }
                            querySQL.append(quoteChar + filter.getDatasetColumn().getName() + quoteChar);
                            querySQL.append(operation2 + values[1] + " ");
                        }
                        querySQL.append(combineOperator);
                    }
                    querySQL.delete(querySQL.length() - combineOperator.length(), querySQL.length());
                } else {
                    for (String value : splitFilter) {
                        if (table.isMain()) {
                            querySQL.append("main.");
                        } else {
                            querySQL.append(dbName + "." + quoteChar + tableName + quoteChar + ".");
                        }
                        querySQL.append(quoteChar + filter.getDatasetColumn().getName() + quoteChar);
                        querySQL.append(" " + filter.getQualifier() + " ");
                        if (filter.getQualifier() != OperatorType.IS)
                            querySQL.append("'" + value.trim() + "' OR ");
                        else
                            querySQL.append(value.trim() + " OR ");

                    }
                    querySQL.delete(querySQL.length() - 3, querySQL.length());
                }
                querySQL.append(") AND ");
            }
        }

        // Clean up the last bracket and AND if necessary
        if (selectedFilters.size() > 0 && (dmTables.size() == 0 || leftFlag)) {
            querySQL.delete(querySQL.length() - 4, querySQL.length());
        }

        // Add in the join relations
        if (!leftFlag) {
            for (DatasetTable table : dmTables) {
                Key key = table.getForeignKeys().iterator().next();
                for (Column column : key.getColumns()) {
                    String keyName = column.getName();
                    String tableName = table.getSubPartitionCommaList(ds.getName());
                    if (McUtils.hasPartitionBinding(tableName))
                        tableName = McUtils.getRealName(tableName, ds);
                    querySQL.append(" main." + quoteChar + keyName + quoteChar + "=" + dbName + "." + quoteChar
                            + tableName + quoteChar + "." + quoteChar + keyName + quoteChar + " AND");
                }
            }
            if (dmTables.size() > 0) {
                querySQL.delete(querySQL.length() - 4, querySQL.length());
            }
        }

        return querySQL.toString();
    }

    private void findTableType(HashSet<Column> columnsToCheck, HashSet<DatasetTable> mainTables,
            HashSet<DatasetTable> dmTables, Filter filter, Dataset ds) {
        DatasetTable tableName = filter.getDatasetTable();
        if (tableName.getType().equals(DatasetTableType.DIMENSION)) {
            dmTables.add(tableName);
            for (Column cc : tableName.getRelations().iterator().next()
                    .getOtherKey(tableName.getForeignKeys().iterator().next()).getColumns()) {
                columnsToCheck.add(cc);
            }
        } else {
            mainTables.add(tableName);
            columnsToCheck.add(filter.getDatasetColumn());
        }
    }

    /**
     *
     * @param dbName
     * @return
     */
    public String searchSourceSchema(String dbName) {
        // searchMaterilizedSchema();
        // System.out.println("----");
        dbName = dbName + ".";
        // Create list of all main tables
        List<DatasetTable> allMainTables = selectedAttributes.iterator().next().getDatasetTable().getMart()
                .getOrderedMainTableList();
        Set<Column> columnsToCheck = new HashSet<Column>();

        /*
         * Loop through all the attributes, adding them to the list of columns to check the main table
         */
        for (Attribute attribute : selectedAttributes) {
            DatasetTable table = attribute.getDatasetTable();
            if (table.getType().equals(DatasetTableType.DIMENSION)) {
                for (Column column : table.getRelations().iterator().next()
                        .getOtherKey(table.getForeignKeys().iterator().next()).getColumns())
                    columnsToCheck.add(column);
            } else {

                columnsToCheck.add(attribute.getDataSetColumn());
            }
        }
        /*
         * Loop through all the filters, finding their corresponding attributes, and then adding those to the list of
         * columns to check for the main table
         */
        for (Filter filter : selectedFilters.keySet()) {
            DatasetTable tableName = filter.getDatasetTable();
            if (tableName.getType().equals(DatasetTableType.DIMENSION)) {
                for (Column column : tableName.getRelations().iterator().next()
                        .getOtherKey(tableName.getForeignKeys().iterator().next()).getColumns())
                    columnsToCheck.add(column);
            } else {
                columnsToCheck.add(filter.getDatasetColumn());
            }
        }

        /*
         * Figure out what table should be the main table by checking if it contains any of the necessary keys. If it
         * doesn't contain any, move to the next smaller main table. Otherwise, it is our main.
         */
        DatasetTable mainTable = allMainTables.get(allMainTables.size() - 1);// null;
        /*
         * outer: for(DatasetTable table:allMainTables){ //for (Column column : columnsToCheck) { //if
         * (table.getColumnList().contains(column)) { if (table.getColumnList().containsAll(columnsToCheck)) { mainTable
         * = table; break outer; } //} }
         */

        // This map will keep track of all the sourcecolumns for each datasettable
        Map<DatasetTable, Set<Table>> sourceTablesByDataSetTable = new HashMap<DatasetTable, Set<Table>>();
        // First we keep track of all the source tables for the selected attributes
        Set<Table> tempSet = null;
        for (Attribute attribute : selectedAttributes) {
            DatasetColumn curColumn = attribute.getDataSetColumn();
            DatasetTable curDataSetTable = curColumn.getDatasetTable();
            // if(curDataSetTable.getType().equals(DatasetTableType.MAIN) || curDataSetTable.getType().equals(
            // DatasetTableType.MAIN_SUBCLASS))
            if (curDataSetTable.isMain())
                curDataSetTable = mainTable;
            Table curSourceTable = curColumn.getSourceColumn().getTable();

            tempSet = sourceTablesByDataSetTable.get(curDataSetTable);
            if (null == tempSet) {
                tempSet = new HashSet<Table>();
            }
            tempSet.add(curSourceTable);
            sourceTablesByDataSetTable.put(curDataSetTable, tempSet);
        }

        // And the same for filters
        if (!(selectedFilters == null)) {
            for (Filter filter : selectedFilters.keySet()) {
                DatasetColumn curColumn = filter.getDatasetColumn();
                DatasetTable curDataSetTable = curColumn.getDatasetTable();
                // if(curDataSetTable.getType().equals(DatasetTableType.MAIN) || curDataSetTable.getType().equals(
                // DatasetTableType.MAIN_SUBCLASS))
                if (curDataSetTable.isMain())
                    curDataSetTable = mainTable;
                Table curSourceTable = curColumn.getSourceColumn().getTable();

                tempSet = sourceTablesByDataSetTable.get(curDataSetTable);
                if (null == tempSet) {
                    tempSet = new HashSet<Table>();
                }
                tempSet.add(curSourceTable);
                sourceTablesByDataSetTable.put(curDataSetTable, tempSet);
            }
        } else {
            System.err.println("Empty filterset!");
        }
        // Next we make sure that we have the Source keys and Source tables for joining the main to the DMs, but only if
        // we have more than one table
        if (sourceTablesByDataSetTable.keySet().size() > 1) {
            Map<DatasetTable, Set<Table>> tempMap = new HashMap<DatasetTable, Set<Table>>();
            for (DatasetTable curDSTable : sourceTablesByDataSetTable.keySet()) {
                if (curDSTable.getType().equals(DatasetTableType.DIMENSION)) {
                    Relation relation = curDSTable.getRelations().iterator().next();
                    Key curDSKey = relation.getKeyForTable(curDSTable);
                    Key curMainKey = relation.getOtherKey(curDSKey);
                    DatasetTable curMainTable = mainTable;// (DatasetTable) curMainKey.getTable();

                    // Make sure the DM table's key's source table is included
                    tempSet = sourceTablesByDataSetTable.get(curDSTable);
                    if (null == tempSet) {
                        tempSet = new HashSet<Table>();
                    }
                    tempSet.add(((DatasetColumn) curDSKey.getColumns().get(0)).getTable());
                    tempMap.put(curDSTable, tempSet);

                    // And the inverse: Make sure the Main table's key's source table is included;
                    // This will need to resolve to a single main eventually, but for now this is OK
                    tempSet = sourceTablesByDataSetTable.get(curMainTable);
                    if (null == tempSet) {
                        tempSet = new HashSet<Table>();
                    }
                    tempSet.add(((DatasetColumn) curMainKey.getColumns().get(0)).getTable());
                    tempMap.put(mainTable/* curMainTable */, tempSet);
                }
            }
            sourceTablesByDataSetTable.putAll(tempMap);
        }

        /*
         * Loop through the attributes, adding them to the SELECT clause of the query
         */
        StringBuilder querySQL = new StringBuilder();

        if (isCountQuery) {
            querySQL.append("SELECT COUNT(*) FROM ").append(mainTable.getName());
        } else {
            querySQL.append("SELECT ");
            for (Attribute attribute : selectedAttributes) {
                querySQL.append(dbName);
                String tableName = attribute.getDataSetColumn().getSourceColumn().getTable().getName();
                querySQL.append(quoteChar + tableName + quoteChar);
                querySQL.append(".");
                querySQL.append(quoteChar + attribute.getDataSetColumn().getSourceColumn().getName() + quoteChar);
                querySQL.append(", ");
            }
            // Clean up the final comma if necessary
            if (selectedAttributes.size() > 0) {
                querySQL.deleteCharAt(querySQL.length() - 2);
            }
        }

        querySQL.append("FROM ");
        int insertParenthesisPoint = querySQL.length(); // Keep track of where to insert opening parentheses
        int openParenthesisCount = 0;

        LinkedHashMap<Table, Relation> orderedSourceTables = new LinkedHashMap<Table, Relation>();
        List<Object> mainSourceList = qcPathMap.get(mainTable);

        for (int i = 0; i <= getLastIndex(sourceTablesByDataSetTable.get(mainTable), mainSourceList); i += 2) {
            if (orderedSourceTables.get((Table) mainSourceList.get(i)) == null)
                if (i == 0) {
                    orderedSourceTables.put((Table) mainSourceList.get(i), null);
                } else {
                    orderedSourceTables.put((Table) mainSourceList.get(i), (Relation) mainSourceList.get(i - 1));
                }
        }
        for (DatasetTable table : sourceTablesByDataSetTable.keySet()) {
            List<Object> sourceList = qcPathMap.get(table);
            for (int i = 0; i <= getLastIndex(sourceTablesByDataSetTable.get(table), sourceList); i += 2) {
                if (orderedSourceTables.get((Table) sourceList.get(i)) == null)
                    if (i == 0) {
                        /* Don't do anything, because this is a dm table. May need revision */
                        // orderedSourceTables.put((Table) sourceList.get(i), null);
                    } else {
                        orderedSourceTables.put((Table) sourceList.get(i), (Relation) sourceList.get(i - 1));
                    }
            }
        }

        List<Table> reorderedTableList = getReorderedJoinTableList(orderedSourceTables);
        for (Table sourceTable : reorderedTableList) {
            Relation relation = orderedSourceTables.get(sourceTable);
            if (null == relation) {
                querySQL.append(dbName + quoteChar + sourceTable.getName() + quoteChar);
            } else {
                querySQL.append(" LEFT JOIN ");
                querySQL.append(dbName + quoteChar + sourceTable.getName() + quoteChar + " ON ");
                querySQL.append(dbName + quoteChar + sourceTable.getName() + quoteChar + "." + quoteChar
                        + relation.getFirstKeyColumnForTable(sourceTable).getName() + quoteChar + "=" + dbName
                        + quoteChar + relation.getFirstKeyColumnForOtherTable(sourceTable).getTable().getName()
                        + quoteChar + "." + quoteChar + relation.getFirstKeyColumnForOtherTable(sourceTable).getName()
                        + quoteChar + ")");
                openParenthesisCount++;
            }
        }
        for (int i = 0; i < openParenthesisCount; ++i) {
            querySQL.insert(insertParenthesisPoint, '(');
        }

        if (selectedFilters.size() > 0) {
            querySQL.append(" WHERE ");
            for (Filter filter : selectedFilters.keySet()) {
                String values[] = selectedFilters.get(filter).split("[,\\n\\r]");
                String tableName = filter.getDatasetColumn().getSourceColumn().getTable().getName();
                querySQL.append(dbName + quoteChar + tableName + quoteChar);
                querySQL.append(".");
                querySQL.append(quoteChar + filter.getDatasetColumn().getSourceColumn().getName() + quoteChar);
                if (values.length == 1) {
                    querySQL.append(" ");
                    querySQL.append(filter.getQualifier());
                    querySQL.append("'" + selectedFilters.get(filter) + "'");
                } else {
                    if (filter.getQualifier().equals(OperatorType.E)) {
                        querySQL.append(" IN (");
                        for (String value : values) {
                            querySQL.append("'" + value.trim() + "',");
                        }
                        querySQL.deleteCharAt(querySQL.length() - 1);
                        querySQL.append(")");
                    }
                }
                querySQL.append(" AND ");
            }
            querySQL.delete(querySQL.length() - 4, querySQL.length());
        }
        // System.out.print("Source: ");
        // System.out.println(querySQL);
        return querySQL.toString();
    }

    /**
     * Returns an RDBMS/case-independent deterministic ordering of tables (for join)
     */
    private List<Table> getReorderedJoinTableList(LinkedHashMap<Table, Relation> orderedSourceTables) {

        Map<Table, List<Table>> dependenceMap = new TreeMap<Table, List<Table>>();
        boolean first = true;
        for (Table sourceTable : orderedSourceTables.keySet()) {
            if (first) { // skip first table (must be first anyway)
                first = false;
            } else {
                Relation relation = orderedSourceTables.get(sourceTable);
                Table referencedTable = relation.getFirstKeyColumnForOtherTable(sourceTable).getTable();
                assert null != referencedTable;

                List<Table> referencingTableList = dependenceMap.get(referencedTable);
                if (null == referencingTableList) {
                    referencingTableList = new ArrayList<Table>();
                    dependenceMap.put(referencedTable, referencingTableList);
                }
                assert !referencingTableList.contains(sourceTable);
                referencingTableList.add(sourceTable);
            }
        }
        assert orderedSourceTables.isEmpty() || !dependenceMap.isEmpty() : dependenceMap;

        List<Table> reorderedJoinTableList = new ArrayList<Table>();
        if (!orderedSourceTables.isEmpty()) {
            Table firstSourceTable = orderedSourceTables.keySet().iterator().next();
            assert firstSourceTable.getRelations() == null; // first table has no relation by design
            reorderedJoinTableList.add(firstSourceTable);
            recursiveTableAddition(dependenceMap, reorderedJoinTableList, firstSourceTable);
        }

        String debugString = McUtils.NEW_LINE + "dependenceMap = " + dependenceMap + McUtils.NEW_LINE
                + McUtils.NEW_LINE + "joinTableList = " + reorderedJoinTableList + McUtils.NEW_LINE;
        assert reorderedJoinTableList.size() == orderedSourceTables.size()
                && new TreeSet<Table>(reorderedJoinTableList).size() == orderedSourceTables.size() : debugString;
        Log.debug(debugString);
        return reorderedJoinTableList;
    }

    private void recursiveTableAddition(Map<Table, List<Table>> dependenceMap, List<Table> reorderedJoinTableList,
            Table referencedTable) {
        List<Table> referencingTableList = dependenceMap.get(referencedTable);
        if (null != referencingTableList) {
            assert !referencingTableList.isEmpty();
            Collections.sort(referencingTableList);
            for (Table referencingTable : referencingTableList) {
                reorderedJoinTableList.add(referencingTable);
                recursiveTableAddition(dependenceMap, reorderedJoinTableList, referencingTable);
            }
        }
    }

    /**
     *
     * @param value
     */
    public void setQcPathMap(Map<DatasetTable, List<Object>> value) {
        this.qcPathMap = value;
    }

    /**
     *
     * @param value
     */
    public void setSelectedAttributes(List<Attribute> value) {
        this.selectedAttributes = value;
    }

    /**
     *
     * @param value
     */
    public void setSelectedFilters(Map<Filter, String> value) {
        this.selectedFilters = value;
    }

    /**
	 *
	 */
    private final boolean isCountQuery;

    public QueryCompiler(boolean isCountQuery) {
        this.isCountQuery = isCountQuery;
        this.selectedAttributes = new ArrayList<Attribute>();
        this.selectedFilters = new HashMap<Filter, String>();
    }

    /**
     * Generates the SQL query corresponding to the SubQuery object specified
     *
     * @param subQuery
     * @param forceSource
     * @return
     */

    public String generateQuery(SubQuery subQuery, boolean forceSource) {
        // If forcing it to use the source schema or that the schema is not materialized
        // if (forceSource /*||
        // !subQuery.getConfig().getMart().getMaterialized()*/) {
        // return searchSourceSchema();
        // }
        // Use the materialized schema
        // else {
        ArrayList<Attribute> portables = new ArrayList<Attribute>();
        List<String> datasets = new ArrayList<String>();
        datasets.add(subQuery.getDataset().getInternalName());
        for (QueryElement queryElement : subQuery.getQueryAttributeList()) {
            Attribute attribute;
            switch (queryElement.getType()) {
            case ATTRIBUTE:
                attribute = (Attribute) queryElement.getElement();
                List<Attribute> attributeList = attribute.getAttributeList(datasets, true);
                if (attributeList.isEmpty()) {
                    attributeList = new ArrayList<Attribute>();
                    attributeList.add(attribute);
                }
                for (Attribute subAttribute : attributeList) {
                    if (subAttribute == null)
                        Log.error("ATTRIBUTE is NULL");
                    if (subAttribute.getValue() == null || subAttribute.getValue().equals(""))
                        this.selectedAttributes.add(subAttribute);
                }
                break;
            case EXPORTABLE_ATTRIBUTE:
                for (Element element : queryElement.getElementList()) {
                    attribute = (Attribute) element;
                    if (attribute == null)
                        Log.error("EXPORTABLE ATTRIBUTE is NULL");
                    portables.add(attribute);
                    if (queryElement.getPortablePosition() == null) {
                        queryElement.setPortablePosition(portables.size() - 1);
                    }
                }
                break;
            }
        }

        for (QueryElement queryElement : subQuery.getQueryFilterList()) {
            Filter filter;
            switch (queryElement.getType()) {
            case FILTER:
                filter = (Filter) queryElement.getElement();

                if (subQuery.isDatabase() || subQuery.getVersion().equals("0.7")) {
                    if (filter.isFilterList()) {
                        for (Filter subFilter : filter.getFilterList(datasets)) {
                            if (subFilter.getFilterType() == FilterType.BOOLEAN) {
                                if (queryElement.getFilterValues().equals("only")) {
                                    subFilter.setQualifier(OperatorType.IS);
                                    queryElement.setFilterValues(this.only);
                                } else if (queryElement.getFilterValues().equals("excluded")) {
                                    subFilter.setQualifier(OperatorType.IS);
                                    queryElement.setFilterValues(this.excluded);
                                }
                            }
                        }
                    }
                }
                this.selectedFilters.put(filter, queryElement.getFilterValues());
                if (subQuery.isDatabase() || subQuery.getVersion().equals("0.7")) {
                    if (filter.getFilterType() == FilterType.BOOLEAN) {
                        if (queryElement.getFilterValues().equals("only")) {
                            filter.setQualifier(OperatorType.IS);
                            this.selectedFilters.put(filter, this.only);
                        } else if (queryElement.getFilterValues().equals("excluded")) {
                            filter.setQualifier(OperatorType.IS);
                            this.selectedFilters.put(filter, this.excluded);
                        }
                    }
                }
                break;
            case IMPORTABLE_FILTER:
                for (Element element : queryElement.getElementList()) {
                    filter = (Filter) element;
                    if (filter.getAttribute() == null)
                        Log.error("IMPORTABLE FILTER's attribute is NULL");
                    portables.add(filter.getAttribute());
                    if (queryElement.getPortablePosition() == null) {
                        queryElement.setPortablePosition(portables.size() - 1);
                    }
                }
                break;
            }
        }
        this.selectedAttributes.addAll(0, portables);
        subQuery.setTotalCols(this.selectedAttributes.size());
        if (subQuery.isDatabase()) {
            String prefix = "";
            if (subQuery.getDbType() == DBType.ORACLE || subQuery.getDbType() == DBType.POSTGRES
                    || subQuery.getDbType() == DBType.DB2)
                quoteChar = "\"";
            if (subQuery.useDbName() && subQuery.useSchema())
                prefix = quoteChar + subQuery.getDatabaseName() + quoteChar + "." + quoteChar
                        + subQuery.getSchemaName() + quoteChar;
            else if (subQuery.useDbName())
                prefix = quoteChar + subQuery.getDatabaseName() + quoteChar;
            else if (subQuery.useSchema())
                prefix = quoteChar + subQuery.getSchemaName() + quoteChar;

            return searchSchema(prefix, subQuery.getDataset(), forceSource);
        } else {
            return searchWebServices(subQuery);
        }
        // }
    }

    private String searchSchema(String dbName, Dataset ds, boolean forceSource) {
        if (forceSource) {
            this.qcPathMap = McUtils.getQcPathMap(ds);
            return searchSourceSchema(dbName);
        } else {
            return searchMaterilizedSchema(dbName, ds);
        }
    }

    // Allow filters to be excluded in count queries so we can get a total #
    private String searchWebServices(SubQuery subQuery) {
        org.jdom.Element queryElement = new org.jdom.Element("Query");
        Document queryDocument = new Document(queryElement);
        // queryElement.setAttribute("processor", subQuery.getProcessor());
        // queryElement.setAttribute("limit", Integer.toString(subQuery.getLimit()));
        queryElement.setAttribute("client", subQuery.getClient());
        String virtualSchema = subQuery.getDataset().getValueForColumn(11);
        if (virtualSchema != null && !virtualSchema.equals(""))
            queryElement.setAttribute("virtualSchemaName", virtualSchema);

        org.jdom.Element datasetElement = new org.jdom.Element("Dataset");
        datasetElement.setAttribute("name", subQuery.getDataset().getName());
        if (subQuery.getConfig().getName() == null)
            datasetElement.setAttribute("config", subQuery.getDataset().getParentMart().getDefaultConfig().getName());
        else
            datasetElement.setAttribute("config", subQuery.getConfig().getName());

        if (isCountQuery) {
            queryElement.setAttribute("count", "1");
        }

        queryElement.addContent(datasetElement);

        for (Attribute attribute : this.selectedAttributes) {
            org.jdom.Element attributeElement = new org.jdom.Element("Attribute");
            attributeElement.setAttribute("name", attribute.getInternalName());
            datasetElement.addContent(attributeElement);
        }

        for (Filter filter : this.selectedFilters.keySet()) {
            org.jdom.Element filterElement = new org.jdom.Element("Filter");
            filterElement.setAttribute("name", filter.getInternalName());
            System.err.println(filter.getFilterType());
            if (subQuery.getVersion().equals("0.7") && filter.getFilterType() == FilterType.BOOLEAN) {
                if (this.selectedFilters.get(filter).equals(this.only)) {
                    this.selectedFilters.put(filter, "only");
                    filterElement.setAttribute("excluded", "0");
                } else if (this.selectedFilters.get(filter).equals(this.excluded)) {
                    this.selectedFilters.put(filter, "excluded");
                    filterElement.setAttribute("excluded", "1");
                }
            } else {
                filterElement.setAttribute("value", this.selectedFilters.get(filter));
            }
            datasetElement.addContent(filterElement);
        }

        XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString(queryDocument);
    }

    private int getLastIndex(Set<Table> set, List<Object> list) {
        int i = 0;
        if (set != null && list != null) {
            for (Table item : set) {
                if (list.lastIndexOf(item) > i)
                    i = list.lastIndexOf(item);
            }
        }

        return i;
    }
}
