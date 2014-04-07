/**
 * 
 */
package org.biomart.configurator.update;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.WordUtils;
import org.biomart.backwardCompatibility.BackwardCompatibility;
import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.dialects.McSQL;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.ForeignKey;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.objects.PrimaryKey;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.RelationTarget;
import org.biomart.objects.portal.MartPointer;
import org.biomart.queryEngine.OperatorType;
import org.jdom.Document;
import org.jdom.Element;

/**
 * @author lyao
 * 
 */
public class UpdateMartModel {

    public Map<String, List<String>> getDataBaseInfo(Dataset ds) throws MartBuilderException {
        DataLinkInfo dlink = ds.getDataLinkInfoNonFlip();
        if (dlink.getDataLinkType() == DataLinkType.SOURCE || dlink.getDataLinkType() == DataLinkType.TARGET) {
            if (ds.isMaterialized()) {
                dlink = ds.getDataLinkInfoForTarget();
            }
            DatasetTable mainDst = ds.getParentMart().getMainTable();
            String tmpDatasetName = mainDst.getName();
            // get the first __
            int index = tmpDatasetName.indexOf(Resources.get("tablenameSep"));
            String dsName = index > 0
                    ? tmpDatasetName.substring(0, index) : tmpDatasetName;
            if (McUtils.hasPartitionBinding(dsName)) {
                dsName = McUtils.getRealName(dsName, ds);
            }

            McSQL mcsql = new McSQL();
            return mcsql.getMartTablesInfo(dlink.getJdbcLinkObject(), dsName);
        } else { // url
            if ("0.7".equals(ds.getVersion())) {
                return this.getDbInfoFromUrl(ds);
            } else {
                return this.getDbInfoFromUrl8(ds);
            }
        }
    }

    private Map<String, List<String>> getDbInfoFromUrl(Dataset ds) {
        Mart configuratorMart = ds.getParentMart();
        BackwardCompatibility bc = new BackwardCompatibility();
        /*
         * JdbcLinkObject linkObject = new JdbcLinkObject(dlinkInfo.getJdbcLinkObject().getJdbcUrl()+schemaName,
         * schemaName, schemaName,dlinkInfo.getJdbcLinkObject().getUserName(),
         * dlinkInfo.getJdbcLinkObject().getPassword(), dlinkInfo.getJdbcLinkObject().getJdbcType(),
         * dlinkInfo.getJdbcLinkObject().getPartitionRegex(),
         * dlinkInfo.getJdbcLinkObject().getPtNameExpression(),false);
         */
        // assume only one database selected for now
        // update jdbcLinkObject
        // dlinkInfo.setJdbcLinkObject(linkObject);
        PartitionTable mainPT = configuratorMart.getSchemaPartitionTable();
        boolean grouped = (mainPT.getTotalRows() > 1);
        String fullhost = mainPT.getValue(0, 0);
        // remove the http:// for host
        int index = fullhost.indexOf("://");
        String host = fullhost.substring(index + 3);
        String port = mainPT.getValue(0, 1);
        String path = mainPT.getValue(0, 2);
        String keys = mainPT.getValue(0, 9);
        String version = mainPT.getValue(0, 7);
        DataLinkInfo dlinkInfo = new DataLinkInfo(DataLinkType.URL);

        UrlLinkObject url = new UrlLinkObject();
        url.setFullHost(fullhost);
        url.setHost(host);
        url.setGrouped(grouped);
        url.setPath(path);
        url.setPort(port);
        url.setKeys(keys);
        url.setVersion8("0.8".equals(version));
        dlinkInfo.setUrlLinkObject(url);

        String modifiedName = configuratorMart.getName();
        if (modifiedName.matches("(.)*_(\\d)+\\Z")) {
            modifiedName = modifiedName.substring(0, modifiedName.lastIndexOf("_"));
        }
        if (mainPT.getTotalRows() == 1) {
            String dsName = mainPT.getValue(0, PartitionUtils.DATASETNAME);
            if (dsName.indexOf("_") >= 0) {
                int i = dsName.indexOf("_");
                dsName = dsName.substring(i + 1);
            }
            if (modifiedName.endsWith("_" + dsName)) {
                modifiedName = modifiedName.substring(0, modifiedName.lastIndexOf("_"));
            }
        }
        List<DatasetFromUrl> tmpDsList = McGuiUtils.INSTANCE.getDatasetsFromURL(fullhost + ":" + port + path,
                modifiedName);
        List<DatasetFromUrl> dsList = new ArrayList<DatasetFromUrl>();
        // only use the dataset has the same name
        for (DatasetFromUrl dsu : tmpDsList) {
            if (dsu.getName().equals(ds.getName())) {
                dsList.add(dsu);
            }
        }

        // dlinkInfo.setUrlLinkObject(value);
        // MartRegistry martRegistry = new MartRegistry("test");
        bc.setMartRegistry(configuratorMart.getMartRegistry());
        bc.setDataLinkInfoObject(dlinkInfo);
        bc.setDatasetsForUrl(dsList);
        List<Mart> martList = bc.parseOldTemplates();
        // is should have one mart only
        if (martList.isEmpty()) {
            return new HashMap<String, List<String>>();
        }
        Collection<DatasetTable> dstList = martList.get(0).getDatasetTables();
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (DatasetTable dst : dstList) {
            List<String> colList = new ArrayList<String>();
            for (Column col : dst.getColumnList()) {
                colList.add(col.getName());
            }
            result.put(dst.getName(), colList);
        }
        return result;
    }

    private Map<String, List<String>> getDbInfoFromUrl8(Dataset ds) {
        Mart configuratorMart = ds.getParentMart();
        PartitionTable mainPT = configuratorMart.getSchemaPartitionTable();

        int row = mainPT.getRowNumberByDatasetName(ds.getName());

        String fullhost = mainPT.getValue(row, PartitionUtils.CONNECTION);
        String port = mainPT.getValue(row, PartitionUtils.DATABASE);
        String userName = mainPT.getValue(row, PartitionUtils.USERNAME);
        String password = mainPT.getValue(row, PartitionUtils.PASSWORD);

        Map<String, List<String>> result = new HashMap<String, List<String>>();
        return result;
    }

    public void updateDatasetTables(Mart mart, Dataset ds, Map<String, List<String>> newTables,
            Set<DatasetTable> newInMartTable, Set<DatasetTable> newInDsTable, Set<DatasetColumn> newInMartColumn,
            Set<DatasetColumn> newInDsColumn, Set<DatasetTable> dropInMartTable, Set<DatasetTable> dropInDsTable,
            Set<DatasetColumn> dropInMartColumn, Set<DatasetColumn> dropInDsColumn, boolean isNewForDataset) {

        String datasetName = ds.getName();
        Collection<DatasetTable> oldDstList = mart.getDatasetTables();

        List<DatasetTable> droppedList = new ArrayList<DatasetTable>(oldDstList);
        for (Map.Entry<String, List<String>> entry : newTables.entrySet()) {
            boolean found = false;
            DatasetTable oldDst = null;
            for (DatasetTable oldDs : oldDstList) {
                String oldTableName = oldDs.getName();
                if (McUtils.hasPartitionBinding(oldTableName)) {
                    oldTableName = McUtils.getRealName(oldTableName, ds);
                }
                String newName = entry.getKey();
                if (McUtils.hasPartitionBinding(newName)) {
                    newName = McUtils.getRealName(newName, ds);
                }
                if (oldTableName.equals(newName)) {
                    found = true;
                    oldDst = oldDs;
                    droppedList.remove(oldDs);
                    break;
                }
            }

            if (!found) {
                // new for the mart, create a new DatasetTable
                DatasetTable newDst = new DatasetTable(mart, entry.getKey(), DatasetTableType.DIMENSION);
                newInMartTable.add(newDst);
            } else {
                // check if the table is new in dataset
                if (isNewForDataset) {
                    oldDst.addInPartitions(datasetName);
                    oldDst.setVisibleModified(true);
                } else {
                    boolean inDs = oldDst.getRange().contains(datasetName);
                    if (!inDs) {// add partition info in the oldDst
                        oldDst.addInPartitions(datasetName);
                        oldDst.setVisibleModified(true);
                        newInDsTable.add(oldDst);
                    }
                }
                // TODO: need to check the columns here
                // check the dropped columns
                Set<DatasetColumn> droppedColumns = new HashSet<DatasetColumn>();
                for (Column dsc : oldDst.getColumnList()) {
                    droppedColumns.add((DatasetColumn) dsc);
                }

                for (String newDscStr : entry.getValue()) {
                    DatasetColumn existingCol = oldDst.getColumnByName(newDscStr);
                    if (existingCol != null) {
                        droppedColumns.remove(existingCol);
                        // check if the column is in current partition
                        if (!existingCol.inPartition(datasetName)) {
                            existingCol.addInPartitions(datasetName);
                            existingCol.setVisibleModified(true);
                            newInDsColumn.add(existingCol);
                        }
                    } else { // the column is new to mart
                        // is it a bool column?
                        DatasetColumn newColumn = new DatasetColumn(oldDst, newDscStr);
                        newInMartColumn.add(newColumn);
                        oldDst.addColumn(newColumn);
                        newColumn.setVisibleModified(true);
                        newColumn.addInPartitions(datasetName);
                    }
                } // end of for
                  // drop columns
                for (DatasetColumn dsc : droppedColumns) {
                    if (dsc.inPartition(datasetName)) {
                        this.dropColumnFromDataset(mart, dsc, datasetName, dropInMartColumn, dropInDsColumn);
                    }
                }
            }// end of else
        }
        // add new datasetTable
        for (DatasetTable newAddedDst : newInMartTable) {
            mart.addTable(newAddedDst);

            newAddedDst.addInPartitions(datasetName);
            newAddedDst.setVisibleModified(true);

            for (String colStr : newTables.get(newAddedDst.getName())) {
                DatasetColumn dsc = new DatasetColumn(newAddedDst, colStr);
                newAddedDst.addColumn(dsc);
                dsc.addInPartitions(datasetName);
            }
        }
        // remove the old one from partitions
        for (DatasetTable droppedDst : droppedList) {
            // remove the table in it does not exist in any partitions
            droppedDst.removeFromPartition(datasetName);
            if (droppedDst.getRange().isEmpty()) {
                droppedDst.getMart().removeDatasetTable(droppedDst);
                dropInMartTable.add(droppedDst);
            }
            // remove all related attributes and filters
            for (Column droppedDsc : droppedDst.getColumnList(datasetName)) {
                this.dropColumnFromDataset(mart, (DatasetColumn) droppedDsc, datasetName, dropInMartColumn,
                        dropInDsColumn);
            }
        }

    }

    private void dropColumnFromDataset(Mart mart, DatasetColumn column, String datasetName,
            Set<DatasetColumn> dropInMartColumn, Set<DatasetColumn> dropInDsColumn) {
        column.removeFromPartitions(datasetName);
        if (column.getRange().isEmpty()) {
            // remove the column
            column.getDatasetTable().removeColumn(column);
            dropInMartColumn.add(column);
        } else {
            dropInDsColumn.add(column);
        }
    }

    public boolean updateOptions(Mart mart, List<Dataset> dslist) throws MartBuilderException {
        // update options for all configs, datasets

        for (Config config : mart.getConfigList()) {

            List<Filter> allFilter = config.getFilters(new ArrayList<String>(), true, true);
            for (Filter filter : allFilter) {
                if (filter.hasDropDown() && filter.getObjectStatus() == ValidationStatus.VALID) {
                    if (filter.getQualifier() != OperatorType.RANGE) {
                        filter.updateDropDown(dslist);
                    }
                }
            }
        }
        return true;
    }

    public boolean updateMart(Mart mart) throws MartBuilderException {
        boolean mixed = false;
        PartitionTable mainPT = mart.getSchemaPartitionTable();
        int conCol = PartitionUtils.CONNECTION;
        if (mainPT.getTotalRows() > 1) {
            // check if they have the same connection
            String base = mainPT.getValue(0, conCol);
            for (int i = 1; i < mainPT.getTotalRows(); i++) {
                if (!base.equals(mainPT.getValue(i, conCol))) {
                    mixed = true;
                    break;
                }
            }
        }

        for (Dataset ds : mart.getDatasetList()) {
            // check if it is hidden for the master
            DataLinkInfo dlink = ds.getDataLinkInfoNonFlip();
            if (dlink.getDataLinkType() == DataLinkType.SOURCE || dlink.getDataLinkType() == DataLinkType.TARGET) {
                if (ds.hideOnConfig(mart.getMasterConfig())) {
                    continue;
                }
                if (dlink.getDataLinkType() == DataLinkType.TARGET) {
                    this.updateDatasetForDB(ds);
                } else {
                    try {
                        MartController.getInstance().updateDatasetFromSource(ds);
                    } catch (DataModelException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // this.updateDatasetForSourceDB(ds);
                }
            } else {
                if ("0.7".equals(ds.getVersion())) {
                    this.updateDatasetForDB(ds);
                } else {
                    if (mixed) {

                    } else {
                        this.updateDsFromUrl8(ds);
                    }
                }
            }
        } // end of for dataset

        return true;
    }

    private void updateDsFromUrl8(Dataset ds) {
        Mart mart = ds.getParentMart();
        // get one martpointer, any one is ok
        List<MartPointer> mpList = mart.getMartRegistry().getPortal().getRootGuiContainer()
                .getMartPointerListforMart(mart);
        if (mpList.isEmpty()) {
            return;
        }
        PartitionTable mainPT = mart.getSchemaPartitionTable();

        int conCol = PartitionUtils.CONNECTION;
        int row = 0;
        String fullhost = mainPT.getValue(row, conCol);
        String port = mainPT.getValue(row, PartitionUtils.DATABASE);
        String userName = mainPT.getValue(row, PartitionUtils.USERNAME);
        String password = mainPT.getValue(row, PartitionUtils.PASSWORD);
        String urlStr = fullhost + (McUtils.isStringEmpty(port)
                ? "" : ":" + port) + "/martservice/xml/configs/" + mpList.get(0).getName();
        Document doc = McUtils.getDocumentFromUrl(urlStr, userName, password);
        if (doc == null) {
            return;
        }

        Element rootElement = doc.getRootElement();
        Element martElement = rootElement.getChild(XMLElements.MART.toString());
        MartRegistry registry = mart.getMartRegistry();
        registry.removeMart(mart);
        Mart newMart = new Mart(martElement);
        registry.addMart(newMart);
        registry.synchronizedFromXML();
        MartController.getInstance().fixObjects(registry);

    }

    private void updateDatasetForDB(Dataset ds) throws MartBuilderException {

        // progressMonitor.setStatus("updating dataset "+ds.getName() +
        // " in mart "+ds.getParentMart().getName());
        Mart mart = ds.getParentMart();
        // check if it has 2nd level schema partitions
        if (mart.getMainTable() != null && mart.getMainTable().hasSubPartition()) {
            this.updateDsWith2SchemaPartitions(ds);
            return;
        }
        Set<DatasetTable> newInMartTable = new HashSet<DatasetTable>();
        Set<DatasetTable> newInDsTable = new HashSet<DatasetTable>();
        Set<DatasetColumn> newInMartColumn = new HashSet<DatasetColumn>();
        Set<DatasetColumn> newInDsColumn = new HashSet<DatasetColumn>();

        Set<DatasetTable> dropInMartTable = new HashSet<DatasetTable>();
        Set<DatasetTable> dropInDsTable = new HashSet<DatasetTable>();
        Set<DatasetColumn> dropInMartColumn = new HashSet<DatasetColumn>();
        Set<DatasetColumn> dropInDsColumn = new HashSet<DatasetColumn>();

        // get all database info
        Map<String, List<String>> tables = this.getDataBaseInfo(ds);
        if (tables.isEmpty()) {// skip the url for now {
            DataLinkInfo dlink = ds.getDataLinkInfoNonFlip();
            if (!(dlink.getDataLinkType() == DataLinkType.SOURCE || dlink.getDataLinkType() == DataLinkType.TARGET)) {
                throw new MartBuilderException("database naming convention error");
            }
        }

        this.updateDatasetTables(mart, ds, tables, newInMartTable, newInDsTable, newInMartColumn, newInDsColumn,
                dropInMartTable, dropInDsTable, dropInMartColumn, dropInDsColumn, false);
        // update configs
        {
            Config config = mart.getMasterConfig();
            Container newAC = config.getContainerByName(Resources.get("NEWATTRIBUTE"));
            if (newAC == null) {
                newAC = new Container(Resources.get("NEWATTRIBUTE"));
            }
            newAC.setVisibleModified(true);

            Container newFC = config.getContainerByName(Resources.get("NEWFILTER"));
            if (newFC == null) {
                newFC = new Container(Resources.get("NEWFILTER"));
            }
            newFC.setVisibleModified(true);

            for (DatasetTable newDst : newInMartTable) {
                for (Column column : newDst.getColumnList()) {
                    // create foreign key for it
                    if (column.getName().endsWith(Resources.get("keySuffix"))) {
                        ForeignKey fk = new ForeignKey(column);
                        newDst.addForeignKey(fk);
                        // update relation for the new table, link it with the
                        // highest level of main table
                        List<DatasetTable> mainDstList = newDst.getMart().getOrderedMainTableList();
                        for (DatasetTable mainDst : mainDstList) {
                            if (mainDst.getPrimaryKey().getColumns().iterator().next().getName()
                                    .equals(column.getName())) {
                                // create relation
                                PrimaryKey pk = mainDst.getPrimaryKey();
                                if (!Relation.isRelationExist(pk, fk)) {
                                    try {
                                        new RelationTarget(pk, fk, Cardinality.MANY_A);
                                    } catch (AssociationException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    this.updateConfigForNewColumn(config, (DatasetColumn) column, newAC, newFC);
                }
            }

            for (DatasetColumn newDsc : newInMartColumn) {
                this.updateConfigForNewColumn(config, newDsc, newAC, newFC);
            }

            // for(DatasetColumn newDsc: newInDsColumn) {
            // this.updateConfigForNewColumn(config, newDsc, newAC, newFC);
            // }

            if (!newAC.isEmpty()) {
                config.getRootContainer().addContainer(newAC);
            }
            if (!newFC.isEmpty()) {
                config.getRootContainer().addContainer(newFC);
            }
        }
        // drop table and column
        for (DatasetTable dropTable : dropInMartTable) {
            for (Column dsc : dropTable.getColumnList()) {
                this.handleConfig4DroppedColumn((DatasetColumn) dsc, mart);
            }
        }

        for (DatasetColumn dropColumn : dropInMartColumn) {
            this.handleConfig4DroppedColumn(dropColumn, mart);
        }
    }

    private void updateDsWith2SchemaPartitions(Dataset ds) throws MartBuilderException {
        Mart mart = ds.getParentMart();

        Set<DatasetTable> newInMartTable = new HashSet<DatasetTable>();
        Set<DatasetTable> newInDsTable = new HashSet<DatasetTable>();
        Set<DatasetColumn> newInMartColumn = new HashSet<DatasetColumn>();
        Set<DatasetColumn> newInDsColumn = new HashSet<DatasetColumn>();

        Set<DatasetTable> dropInMartTable = new HashSet<DatasetTable>();
        Set<DatasetTable> dropInDsTable = new HashSet<DatasetTable>();
        Set<DatasetColumn> dropInMartColumn = new HashSet<DatasetColumn>();
        Set<DatasetColumn> dropInDsColumn = new HashSet<DatasetColumn>();

        DatasetTable mainTable = mart.getMainTable();
        String tmpDatasetName = mainTable.getName();
        // get the first __
        int index = tmpDatasetName.indexOf(Resources.get("tablenameSep"));
        String dsName = index > 0
                ? tmpDatasetName.substring(0, index) : tmpDatasetName;
        index = dsName.indexOf(Resources.get("tablenameSubSep"));
        String dsNameSql = dsName.substring(0, index);
        // get the first table;

        McSQL mcsql = new McSQL();
        // get all database info
        Map<String, List<String>> tables = mcsql.getMartTablesInfo(ds.getDataLinkInfoNonFlip().getJdbcLinkObject(),
                dsName);
        if (tables.isEmpty()) {// skip the url for now {
            throw new MartBuilderException("database naming convention error");
        }

        this.updateDatasetTables(mart, ds, tables, newInMartTable, newInDsTable, newInMartColumn, newInDsColumn,
                dropInMartTable, dropInDsTable, dropInMartColumn, dropInDsColumn, false);
        // update configs
        {
            Config config = mart.getMasterConfig();
            Container newAC = config.getContainerByName(Resources.get("NEWATTRIBUTE"));
            if (newAC == null) {
                newAC = new Container(Resources.get("NEWATTRIBUTE"));
            }
            newAC.setVisibleModified(true);

            Container newFC = config.getContainerByName(Resources.get("NEWFILTER"));
            if (newFC == null) {
                newFC = new Container(Resources.get("NEWFILTER"));
            }
            newFC.setVisibleModified(true);

            for (DatasetTable newDst : newInMartTable) {
                for (Column column : newDst.getColumnList()) {
                    // create foreign key for it
                    if (column.getName().endsWith(Resources.get("keySuffix"))) {
                        ForeignKey fk = new ForeignKey(column);
                        newDst.addForeignKey(fk);
                        // update relation for the new table, link it with the
                        // highest level of main table
                        List<DatasetTable> mainDstList = newDst.getMart().getOrderedMainTableList();
                        for (DatasetTable mainDst : mainDstList) {
                            if (mainDst.getPrimaryKey().getColumns().iterator().next().getName()
                                    .equals(column.getName())) {
                                // create relation
                                PrimaryKey pk = mainDst.getPrimaryKey();
                                if (!Relation.isRelationExist(pk, fk)) {
                                    try {
                                        new RelationTarget(pk, fk, Cardinality.MANY_A);
                                    } catch (AssociationException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    this.updateConfigForNewColumn(config, (DatasetColumn) column, newAC, newFC);
                }
            }

            for (DatasetColumn newDsc : newInMartColumn) {
                this.updateConfigForNewColumn(config, newDsc, newAC, newFC);
            }

            /*
             * for(DatasetColumn newDsc: newInDsColumn) { this.updateConfigForNewColumn(config, newDsc, newAC, newFC); }
             */

            if (!newAC.isEmpty()) {
                config.getRootContainer().addContainer(newAC);
            }
            if (!newFC.isEmpty()) {
                config.getRootContainer().addContainer(newFC);
            }
        }
        // drop table and column
        for (DatasetTable dropTable : dropInMartTable) {
            for (Column dsc : dropTable.getColumnList()) {
                this.handleConfig4DroppedColumn((DatasetColumn) dsc, mart);
            }
        }

        for (DatasetColumn dropColumn : dropInMartColumn) {
            this.handleConfig4DroppedColumn(dropColumn, mart);
        }
    }

    private void handleConfig4DroppedColumn(DatasetColumn droppedCol, Mart mart) {
        List<Attribute> attributeList = droppedCol.getReferences();
        for (Attribute att : attributeList) {
            att.setObjectStatus(ValidationStatus.INVALID);
            List<MartConfiguratorObject> mcObjList = att.getAllReferences();
            for (MartConfiguratorObject mcObj : mcObjList) {
                mcObj.setObjectStatus(ValidationStatus.INVALID);
            }
        }
    }

    private void updateConfigForNewColumn(Config config, DatasetColumn newDsc, Container newAC, Container newFC) {

        DatasetTable table = (DatasetTable) newDsc.getTable();
        List<DatasetTable> maintables = config.getMart().getOrderedMainTableList();

        if (table.getType() != DatasetTableType.DIMENSION) {
            // if table is not the last submain, skip update
            if (!table.getName().equals(maintables.get(maintables.size() - 1).getName()))
                return;
        }

        String tableName = newDsc.getTable().getName();
        Container foundAttContainer = this.findContainer(config, newAC, newFC,
                tableName + Resources.get("ATTRIBUTESUFFIX"));
        Attribute newA;
        if (foundAttContainer != null) {
            newA = this.createNaiveAttribute(config, foundAttContainer, newDsc);
        } else {
            foundAttContainer = new Container(tableName + Resources.get("ATTRIBUTESUFFIX"));
            newAC.addContainer(foundAttContainer);
            newA = this.createNaiveAttribute(config, foundAttContainer, newDsc);
        }

        Container foundFilterContainer = this.findContainer(config, newAC, newFC,
                tableName + Resources.get("FILTERSUFFIX"));
        if (foundFilterContainer != null) {
            this.createNaiveFilter(config, foundFilterContainer, newA);
        } else {
            foundFilterContainer = new Container(tableName + Resources.get("FILTERSUFFIX"));
            newFC.addContainer(foundFilterContainer);
            this.createNaiveFilter(config, foundFilterContainer, newA);
        }

    }

    private Attribute createNaiveAttribute(Config mart, Container container, DatasetColumn dsc) {
        // create a naive attribute
        // check name
        String baseName = dsc.getTable().getName() + Resources.get("tablenameSep") + dsc.getName();
        String name = McUtils.getUniqueAttributeName(mart, baseName);
        String displayName = WordUtils.capitalize(dsc.getName());
        displayName = displayName.replaceAll("_", " ");
        Attribute newA = new Attribute(dsc, name, displayName);
        newA.setVisibleModified(true);
        container.addAttribute(newA);
        container.setVisibleModified(true); // TODO: if a container has both ADD
                                            // and REMOVE?
        return newA;
    }

    private Filter createNaiveFilter(Config mart, Container container, Attribute attribute) {
        // check name
        String name = McGuiUtils.INSTANCE.getUniqueFilterName(mart, attribute.getName());
        Filter newF = new Filter(attribute, name);
        if (attribute.getName().endsWith(Resources.get("BOOLSUFFIX"))) {
            newF.setFilterType(FilterType.BOOLEAN);
        }
        newF.setVisibleModified(true);
        container.addFilter(newF);
        container.setVisibleModified(true);
        return newF;
    }

    /*
     * check by the order of mart, attributeContainer, filterContainer
     */
    private Container findContainer(Config config, Container attributeContainer, Container filterContainer, String name) {
        Container c = config.getContainerByName(name);
        if (c == null) {
            c = attributeContainer.getContainerByNameResursively(name);
        }
        if (c == null) {
            c = filterContainer.getContainerByNameResursively(name);
        }
        return c;
    }

    public boolean updateDataset(Dataset ds, boolean updateOption) throws MartBuilderException {
        boolean checkOrphanColumn = false;

        DataLinkInfo dlink = ds.getDataLinkInfoNonFlip();
        if (dlink.getDataLinkType() == DataLinkType.TARGET) {
            this.updateDatasetForDB(ds);
            checkOrphanColumn = true;
        } else if (dlink.getDataLinkType() == DataLinkType.SOURCE) {
            try {
                MartController.getInstance().updateDatasetFromSource(ds);
                checkOrphanColumn = true;
            } catch (DataModelException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            if ("0.7".equals(ds.getVersion())) {
                this.updateDatasetForDB(ds);
            } else {
                this.updateDsFromUrl8(ds);
            }
        }

        /*
         * if (checkOrphanColumn) { Mart mart = ds.getParentMart();
         * MartController.getInstance().createNaiveForOrphanColumn(mart); }
         */
        return true;
    }

    public boolean updateDatasets(List<Dataset> dslist, boolean updateOption) throws MartBuilderException {
        boolean checkOrphanColumn = false;
        for (Dataset ds : dslist) {
            DataLinkInfo dlink = ds.getDataLinkInfoNonFlip();
            if (dlink.getDataLinkType() == DataLinkType.TARGET) {
                this.updateDatasetForDB(ds);
                checkOrphanColumn = true;
            } else if (dlink.getDataLinkType() == DataLinkType.SOURCE) {
                try {
                    MartController.getInstance().updateDatasetFromSource(ds);
                    checkOrphanColumn = true;
                } catch (DataModelException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                if ("0.7".equals(ds.getVersion())) {
                    this.updateDatasetForDB(ds);
                } else {
                    this.updateDsFromUrl8(ds);
                }
            }
        }
        if (updateOption && !dslist.isEmpty()) {
            this.updateOptions(dslist.get(0).getParentMart(), dslist);
        }
        /*
         * if (checkOrphanColumn) { Mart mart = dslist.get(0).getParentMart();
         * MartController.getInstance().createNaiveForOrphanColumn(mart); }
         */
        return true;
    }

    private void updateDatasetForSourceDB(Dataset ds) throws MartBuilderException {

        // progressMonitor.setStatus("updating dataset "+ds.getName() +
        // " in mart "+ds.getParentMart().getName());
        Mart mart = ds.getParentMart();
        // check if it has 2nd level schema partitions
        if (mart.getMainTable().hasSubPartition()) {
            this.updateDsWith2SchemaPartitions(ds);
            return;
        }
        Set<DatasetTable> newInMartTable = new HashSet<DatasetTable>();
        Set<DatasetTable> newInDsTable = new HashSet<DatasetTable>();
        Set<DatasetColumn> newInMartColumn = new HashSet<DatasetColumn>();
        Set<DatasetColumn> newInDsColumn = new HashSet<DatasetColumn>();

        Set<DatasetTable> dropInMartTable = new HashSet<DatasetTable>();
        Set<DatasetTable> dropInDsTable = new HashSet<DatasetTable>();
        Set<DatasetColumn> dropInMartColumn = new HashSet<DatasetColumn>();
        Set<DatasetColumn> dropInDsColumn = new HashSet<DatasetColumn>();

        // get all database info
        Map<String, List<String>> tables = this.getDataBaseInfo(ds);
        if (tables.isEmpty()) {// skip the url for now {
            throw new MartBuilderException("database naming convention error");
        }

        this.updateDatasetTables(mart, ds, tables, newInMartTable, newInDsTable, newInMartColumn, newInDsColumn,
                dropInMartTable, dropInDsTable, dropInMartColumn, dropInDsColumn, false);
        // update configs
        {
            Config config = mart.getMasterConfig();
            Container newAC = config.getContainerByName(Resources.get("NEWATTRIBUTE"));
            if (newAC == null) {
                newAC = new Container(Resources.get("NEWATTRIBUTE"));
            }
            newAC.setVisibleModified(true);

            Container newFC = config.getContainerByName(Resources.get("NEWFILTER"));
            if (newFC == null) {
                newFC = new Container(Resources.get("NEWFILTER"));
            }
            newFC.setVisibleModified(true);

            for (DatasetTable newDst : newInMartTable) {
                for (Column column : newDst.getColumnList()) {
                    // create foreign key for it
                    if (column.getName().endsWith(Resources.get("keySuffix"))) {
                        ForeignKey fk = new ForeignKey(column);
                        newDst.addForeignKey(fk);
                        // update relation for the new table, link it with the
                        // highest level of main table
                        List<DatasetTable> mainDstList = newDst.getMart().getOrderedMainTableList();
                        for (DatasetTable mainDst : mainDstList) {
                            if (mainDst.getPrimaryKey().getColumns().iterator().next().getName()
                                    .equals(column.getName())) {
                                // create relation
                                PrimaryKey pk = mainDst.getPrimaryKey();
                                if (!Relation.isRelationExist(pk, fk)) {
                                    try {
                                        new RelationTarget(pk, fk, Cardinality.MANY_A);
                                    } catch (AssociationException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    this.updateConfigForNewColumn(config, (DatasetColumn) column, newAC, newFC);
                }
            }

            for (DatasetColumn newDsc : newInMartColumn) {
                this.updateConfigForNewColumn(config, newDsc, newAC, newFC);
            }

            // for(DatasetColumn newDsc: newInDsColumn) {
            // this.updateConfigForNewColumn(config, newDsc, newAC, newFC);
            // }

            if (!newAC.isEmpty()) {
                config.getRootContainer().addContainer(newAC);
            }
            if (!newFC.isEmpty()) {
                config.getRootContainer().addContainer(newFC);
            }
        }
        // drop table and column
        for (DatasetTable dropTable : dropInMartTable) {
            for (Column dsc : dropTable.getColumnList()) {
                this.handleConfig4DroppedColumn((DatasetColumn) dsc, mart);
            }
        }

        for (DatasetColumn dropColumn : dropInMartColumn) {
            this.handleConfig4DroppedColumn(dropColumn, mart);
        }
    }
}
