package org.biomart.configurator.controller;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.controller.dialects.McSQL;
import org.biomart.configurator.model.JDBCDataLink;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.ForeignKey;
import org.biomart.objects.objects.PrimaryKey;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.SourceColumn;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.SourceTable;
import org.biomart.objects.objects.Table;

/**
 * This implementation of the {@link SchemaController} interface connects to a JDBC data source and loads tables, keys
 * and relations using database metadata.
 * <p>
 * If key-guessing is enabled, foreign keys are guessed instead of being read from the database. Guessing works by
 * iterating through known primary keys, where the first column of the key matches the name of the table (optionally
 * with '_id' appended), then iterating through all other tables looking for sets of columns with identical names, or
 * names that have had '_key' appended. If it finds a matching set, then it assumes that it has found a foreign key, and
 * establishes a relation between the two.
 * <p>
 * When using keyguessing, primary keys are read from database metadata, but if this method returns no results, then
 * each table is searched for a column with the same name as the table, optionally with '_id' appended. If one is found,
 * then it is assumed that that column is the primary key for the table.
 * <p>
 * This implementation is very careful not to override any hand-made relations or keys, or to reinstate any that have
 * previously been marked as incorrect.
 */
public class TargetSchema implements JDBCDataLink {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private String realSchemaName;
    // TODO for now, assumne 1:1 relation between target and source
    // will change to 1:many later
    private SourceSchema sourceSchema;

    private String dataLinkDatabase;
    private String dataLinkSchema;
    private JdbcLinkObject conObj;

    private boolean keyGuessing;

    public TargetSchema(final SourceSchema schema, JdbcLinkObject conObject) {
        this.sourceSchema = schema;
        Log.debug("Creating JDBC schema");
        // Remember the settings.
        this.conObj = conObject;

        if (conObject != null) {
            Log.debug("Creating schema " + conObject.getDatabaseName());
            this.setKeyGuessing(conObject.isKeyGuessing());
            this.setDataLinkSchema(conObject.getSchemaName());
            this.setDataLinkDatabase(conObject.getDatabaseName());
        }

    }

    public Connection getConnection() throws SQLException {
        // If we are already connected, test to see if we are
        // still connected. If not, reset our connection.
        if (this.connection != null && this.connection.isClosed())
            try {
                Log.debug("Closing dead JDBC connection");
                this.connection.close();
            } catch (final SQLException e) {
                // We don't care. Ignore it.
            } finally {
                this.connection = null;
            }

        // If we are not connected, we should attempt to (re)connect now.
        if (this.connection == null) {
            Log.debug("Establishing JDBC connection");
            // Start out with no driver at all.
            Class loadedDriverClass = null;

            // Try the system class loader instead.
            try {
                loadedDriverClass = Class.forName(this.conObj.getJdbcType().getDriverClassName());
            } catch (final ClassNotFoundException e) {
                final SQLException e2 = new SQLException();
                e2.initCause(e);
                throw e2;
            }

            // Check it really is an instance of Driver.
            if (!Driver.class.isAssignableFrom(loadedDriverClass))
                throw new ClassCastException(Resources.get("driverClassNotJDBCDriver"));

            // Connect!
            final Properties properties = new Properties();
            properties.setProperty("user", this.conObj.getUserName());
            if (!this.conObj.getPassword().equals(""))
                properties.setProperty("password", this.conObj.getPassword());
            properties.setProperty("nullCatalogMeansCurrent", "false");
            /*
             * this.connection = DriverManager.getConnection( overrideDataLinkSchema == null ? this.conObj.getJdbcUrl():
             * (this.conObj.getJdbcUrl()) .replaceAll(this.getDataLinkSchema(), overrideDataLinkSchema), properties);
             */

            this.connection = DriverManager.getConnection(this.conObj.getJdbcUrl(), conObj.getUserName(),
                    conObj.getPassword());
            // Check the schema name.
            final DatabaseMetaData dmd = this.connection.getMetaData();
            final String catalog = this.connection.getCatalog();
            this.realSchemaName = this.getDataLinkSchema();
            ResultSet rs = dmd.getTables(catalog, this.realSchemaName, "%", null);
            if (!rs.next()) {
                rs = dmd.getTables(catalog, this.realSchemaName.toUpperCase(), "%", null);
                if (rs.next())
                    this.realSchemaName = this.realSchemaName.toUpperCase();
            }
            if (!rs.next()) {
                rs = dmd.getTables(catalog, this.realSchemaName.toLowerCase(), "%", null);
                if (rs.next())
                    this.realSchemaName = this.realSchemaName.toLowerCase();
            }
            rs.close();
        }

        // Return the connection.
        return this.connection;
    }

    public void setDataLinkDatabase(final String dataLinkDatabase) {
        Log.debug("Setting data link database on " + this + " to " + dataLinkDatabase);
        if (this.dataLinkDatabase == dataLinkDatabase || this.dataLinkDatabase != null
                && this.dataLinkDatabase.equals(dataLinkDatabase))
            return;
        this.dataLinkDatabase = dataLinkDatabase;
        // Reset the cached database connection.
        try {
            this.closeConnection();
        } catch (final SQLException e) {
            // We don't care.
        }
    }

    public void setDataLinkSchema(final String dataLinkSchema) {
        Log.debug("Setting data link schema on " + this + " to " + dataLinkSchema);
        if (this.dataLinkSchema == dataLinkSchema || this.dataLinkSchema != null
                && this.dataLinkSchema.equals(dataLinkSchema))
            return;
        this.dataLinkSchema = dataLinkSchema;
        // Reset the cached database connection.
        try {
            this.closeConnection();
        } catch (final SQLException e) {
            // We don't care.
        }
    }

    private void closeConnection() throws SQLException {
        Log.debug("Closing JDBC connection");
        if (this.connection != null)
            try {
                this.connection.close();
            } finally {
                this.connection = null;
            }
    }

    public String getDriverClassName() {
        return this.conObj.getJdbcType().getDriverClassName();
    }

    public String getPassword() {
        return this.conObj.getPassword();
    }

    public String getUrl() {
        return this.conObj.getJdbcUrl();
    }

    public String getUsername() {
        return this.conObj.getUserName();
    }

    private void initSourceSchemaFromXML(List<SourceSchema> sourceSchemas) {
        Log.info("Initialize source schema");
        // assume only one sourceschema for now
        if (sourceSchemas.isEmpty())
            return;
        SourceSchema tmpSS = sourceSchemas.get(0);
        List<Table> tables = tmpSS.getTables();
        for (Table table : tables) {
            this.sourceSchema.addTable(table);
        }
        for (Relation r : tmpSS.getRelations()) {
            this.sourceSchema.addRelation(r);
        }
    }

    private void initSourceSchemaFromDB() throws MartBuilderException {
        Log.info("Initialize source schema");
        // final ProgressDialog progressMonitor = ProgressDialog.getInstance();
        // progressMonitor.setStatus("creating mart from source");

        Map<String, List<String>> tblColMap = new TreeMap<String, List<String>>(McUtils.BETTER_STRING_COMPARATOR); // Tree
                                                                                                                   // for
                                                                                                                   // order
        Map<String, List<String>> tblPkMap = new TreeMap<String, List<String>>(McUtils.BETTER_STRING_COMPARATOR); // Tree
                                                                                                                  // for
                                                                                                                  // order

        this.setMetaInfo(this.conObj.getDatabaseName(), this.conObj.getSchemaName(), tblColMap, tblPkMap);

        for (String tbName : tblColMap.keySet()) {
            String tableName = tbName.substring(tbName.indexOf(".") + 1);
            SourceTable sourceTable = new SourceTable(this.sourceSchema, tableName);
            this.sourceSchema.addTable(sourceTable);
            // create column
            for (String colName : tblColMap.get(tbName)) {
                SourceColumn sourceCol = new SourceColumn(sourceTable, colName);
                sourceTable.addColumn(sourceCol);
            }
            // create PK
            List<Column> pkColList = new ArrayList<Column>();
            if (tblPkMap.get(tableName) != null)
                for (String pkColName : tblPkMap.get(tableName)) {
                    pkColList.add(sourceTable.getColumnByName(pkColName));
                }
            this.createPKforTable(sourceTable, pkColList);
        }

        if (this.isKeyGuessing())
            try {
                MartController.getInstance().synchroniseKeysUsingKeyGuessing(this.sourceSchema,
                        new TreeSet<ForeignKey>()); // Tree for order
            } catch (DataModelException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        else
            try {
                MartController.getInstance().synchroniseKeysUsingDMD(this.sourceSchema, new TreeSet<ForeignKey>(),
                        this.getConnection().getMetaData(), // Tree for order
                        this.conObj.getSchemaName(), this.connection.getCatalog());
            } catch (DataModelException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        Log.info("Done synchronising");
    }

    public void initSourceSchema(List<SourceSchema> sourceSchemas) throws MartBuilderException {
        if (McUtils.isCollectionEmpty(sourceSchemas))
            this.initSourceSchemaFromDB();
        else
            this.initSourceSchemaFromXML(sourceSchemas);
    }

    /**
     * create a PK for table, if the table has candidate columns, use them, otherwise use keyGuessing
     * 
     * @param table
     * @param hasPK
     * @param pkCols
     */
    private void createPKforTable(Table table, List<Column> pkCols) {
        if (table == null)
            return;
        if (pkCols.isEmpty() && this.isKeyGuessing()) {
            // create PK by keyguessing
            // Did DMD find a PK? If not, which is really unusual but
            // potentially may happen, attempt to find one by looking for a
            // single column with the same name as the table or with '_id'
            // appended if it is source. For the mart, pk is the columns with '_key'.
            // Only do this if we are using key-guessing.
            // Plain version first.
            Column candidateCol = table.getColumnByName(table.getName());
            // Try with '_id' appended if plain version turned up
            // nothing.
            if (candidateCol == null)
                candidateCol = table.getColumnByName(table.getName() + Resources.get("primaryKeySuffix"));
            // Found something? Add it to the primary key columns map,
            // with a dummy key of 1. (Use Short for the key because
            // that
            // is what DMD would have used had it found anything
            // itself).
            if (candidateCol != null)
                pkCols.add(candidateCol);

        }
        // create PK
        if (!pkCols.isEmpty()) {
            PrimaryKey pkObject;
            try {
                List<Column> columns = new ArrayList<Column>();

                for (Column cc : pkCols) {
                    columns.add(cc);
                }
                pkObject = new PrimaryKey(columns);
            } catch (final Throwable th) {
                throw new BioMartError(th);
            }

            try {
                table.setPrimaryKey(pkObject);
            } catch (final Throwable th) {
                throw new BioMartError(th);
            }
        }

    }

    /**
     * get the metadata info from source database, and set to tblColMap,tblPkMap, and tblFkMap
     * 
     * @throws MartBuilderException
     * @throws SQLException
     */
    private void setMetaInfo(String dbName, String schemaName, Map<String, List<String>> tblColMap,
            Map<String, List<String>> tblPkMap) throws MartBuilderException {
        McSQL mcsql = new McSQL();
        mcsql.setMetaInfo(this.conObj, dbName, schemaName, tblColMap, tblPkMap);
    }

    public SourceSchema getSourceSchema() {
        return this.sourceSchema;
    }

    public String getDataLinkDatabase() {
        return this.dataLinkDatabase;
    }

    public String getDataLinkSchema() {
        return this.dataLinkSchema;
    }

    /**
     * Checks whether this schema uses key-guessing or not.
     * 
     * @return <tt>true</tt> if it does, <tt>false</tt> if it doesn't.
     */
    public boolean isKeyGuessing() {
        return this.keyGuessing;
    }

    /**
     * Sets whether this schema uses key-guessing or not.
     * 
     * @param keyGuessing
     *            <tt>true</tt> if it does, <tt>false</tt> if it doesn't.
     */
    public void setKeyGuessing(final boolean keyGuessing) {
        Log.debug("Setting key guessing on " + this + " to " + keyGuessing);
        if (this.keyGuessing == keyGuessing)
            return;
        this.keyGuessing = keyGuessing;
    }

}
