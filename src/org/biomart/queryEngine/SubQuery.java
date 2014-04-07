package org.biomart.queryEngine;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.biomart.common.exceptions.BioMartException;

import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.resources.Log;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.oauth.rest.OAuthSigner;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Filter;
import org.biomart.queryEngine.queryCompiler.QueryCompiler;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;

/**
 *
 * @author Syed Haider
 *
 * An object representing an atomic indivisible dataset query.
 * A Subquery object is composed of QueryElement objects each representing
 * an Attribute, Filter, Exportable or Importable. Each subquery manages its own
 * results buffer and hides results buffer's origin (webservice or DB) from other
 * Subqueries that consumes it as an input. This is to enable communication between
 * SubQuery objects in a generic way talking through a common interface.
 * 
 * A query compilation specific logic also resides here, rightly so. The said logic
 * updates a Subquery with incoming results being exported by the previous SubQuery
 * object in the chain of SubQuery objects.
 *
 * Additionally, if there are linkinidices available for a dataset,
 * UpdateQuery() consults that too before compiling the WHERE clause (IN list)
 * just to make sure if there is any point of executing this query. If the decision
 * is 'not' to execute this SubQuery because of no overlap between LinkIndices and
 * INLIST, the downstream chain is not executed at all for the given IN LIST.
 *
 * ResultsBuffers associated to Databases connections and Webservice end-points
 * are closed here once the query is either finished or client sends a closure signal.
 *
 * OAuth specific webservice queries are also encapsulated in OAuth envelope here.
 */
public final class SubQuery {

	private final boolean isCountQuery;

    /**
     *
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	private boolean stream = true;
	private String connectionString;
	private ArrayList<Connection> connection = new ArrayList<Connection>();
	private ArrayList<PreparedStatement> preparedStatement = new ArrayList<PreparedStatement>();
	private ArrayList<Statement> statement = new ArrayList<Statement>();
    /**
     *
     */
    public ArrayList<ResultSet> resultSet = new ArrayList<ResultSet>();
    /**
     *
     */
    public int totalTablePartitions = 1;
    /**
     *
     */
    public int totalCols = 0;
    /**
     * caching flag is turned ON only for importable only subqueries
     * hence it stores the results so they can be shared between various threads.
     * it does cause memory overhead though.
     */
    public boolean caching = false;
    public ArrayList<ArrayList<String>> resultTable;
    /**
     *
     */
    protected InputStreamReader inputStreamReader = null;
    /**
     *
     */
    public BufferedReader resultSetURL;
    /**
     *
     */
    public OutputStreamWriter resultSetURL_wr;
	private String queryString = null;
	private String originalQueryString = null;
	private ArrayList<SubQuery> specialSubQueryList = new ArrayList<SubQuery>();
    // contains both result attribute (for SELECT clause, final results) and
	// joining attributes (for SELECT, temporarily needed)
	private ArrayList<QueryElement> attributeList = null; 
	// contains both result filtering filters (for WHERE clause, local filtering) 
    // and joining filters (for "IN" list, filtering from previous results)
    private ArrayList<QueryElement> filterList = null;
	private Dataset dataset;
	private DBType dbType;
	private String processor;
	private int limit;
	private boolean importableOnly = false;
	private String client = "";
	private Config config;
	private boolean emptyBatch = false;

    /**
     *
     */
    public SubQuery(boolean isCountQuery) {
		this(new ArrayList<QueryElement>(), new ArrayList<QueryElement>(), isCountQuery);
	}

    /**
     *
     * @param queryElement
     */
    public SubQuery(QueryElement queryElement, boolean isCountQuery) {
		this(isCountQuery);
		this.addElement(queryElement);
		this.dataset = queryElement.getDataset();
		this.config = queryElement.getConfig();
		if (this.dataset.getDataLinkInfo().getDataLinkType() == DataLinkType.SOURCE
				|| this.dataset.getDataLinkInfo().getDataLinkType() == DataLinkType.TARGET) {
			if (this.getConnectionString().startsWith("jdbc:mysql"))
				this.dbType = DBType.MYSQL;
			else if (this.getConnectionString().startsWith("jdbc:postgres"))
				this.dbType = DBType.POSTGRES;
			else if (this.getConnectionString().startsWith("jdbc:oracle"))
				this.dbType = DBType.ORACLE;
			else if (this.getConnectionString().startsWith("jdbc:sqlserver"))
				this.dbType = DBType.MSSQL;
			else if (this.getConnectionString().startsWith("jdbc:db2"))
				this.dbType = DBType.DB2;
		} else if (this.dataset.getDataLinkInfo().getDataLinkType() == DataLinkType.URL) {
			this.dbType = DBType.WS;
		}
	}

    /**
     *
     * @param attributeList
     * @param filterList
     */
    public SubQuery(ArrayList<QueryElement> attributeList,
			ArrayList<QueryElement> filterList, boolean isCountQuery) {

		this.isCountQuery = isCountQuery;

		this.attributeList = new ArrayList<QueryElement>();
		if (null != attributeList) {
			this.attributeList.addAll(attributeList);
		}

		this.filterList = new ArrayList<QueryElement>();
		if (null != filterList) {
			this.filterList.addAll(filterList);
		}
	}

    /**
     *
     * @param totalCols
     */
    public void setTotalCols(int totalCols) {
		this.totalCols = totalCols;
	}

    /**
     *
     * @param emptyBatch
     */
    public void setEmptyBatch(boolean emptyBatch) {
		this.emptyBatch = emptyBatch;
	}

    /**
     *
     * @return
     */
    public String getVersion() {
		return this.dataset.getVersion();
	}

    /**
     *
     * @return
     */
    public Config getConfig() {
		return config;
	}

    /**
     *
     * @return
     */
    public String getClient() {
		return client;
	}

    /**
     *
     * @return
     */
    public boolean isImportableOnly() {
		return importableOnly;
	}

    /**
     *
     * @param importableOnly
     */
    public void setImportableOnly(boolean importableOnly) {
		this.importableOnly = importableOnly;
	}

    /**
     *
     * @return
     */
    public DBType getDbType() {
		return dbType;
	}

	// Bands, ontologies, etc
    /**
     *
     * @return
     */
    public ArrayList<SubQuery> getImportableOnlySubQueries() {
		ArrayList<SubQuery> outputList = new ArrayList<SubQuery>();
		for (SubQuery subQuery : specialSubQueryList) {
			if (subQuery.isImportableOnly())
				outputList.add(subQuery);
		}
		return outputList;
	}

    /**
     *
     * @param subQuery
     */
    public void addImportableOnlySubQuery(SubQuery subQuery) {
		subQuery.setImportableOnly(true);
		this.specialSubQueryList.add(subQuery);
	}

    /**
     *
     * @return
     */
    public Dataset getDataset() {
		return dataset;
	}

    /**
     *
     * @param queryElement
     */
    public void addAttribute(QueryElement queryElement) {
		if (queryElement.getType() != QueryElementType.ATTRIBUTE) {
		} else {
			this.addElement(queryElement);
		}
	}

    /**
     *
     * @param queryElement
     */
    public void addFilter(QueryElement queryElement) {
		if (queryElement.getType() != QueryElementType.FILTER) {
		} else {
			this.addElement(queryElement);
		}
	}

    /**
     *
     * @param queryElement
     */
    public void addElement(QueryElement queryElement) {
		if (queryElement.getElement() != null
				&& queryElement.isPointer() && !(queryElement.isPointerInSource())) {
			// TODO throw error: pointers must be resolved before being added to
			// subquery
		} else {
			switch (queryElement.getType()) {
			case ATTRIBUTE:
				if (((Attribute) queryElement.getElement()).getValue() == null
						|| ((Attribute) queryElement.getElement()).getValue()
						.equals("")) {
					this.attributeList.add(queryElement);
				}
				break;
			case EXPORTABLE_ATTRIBUTE:
				this.attributeList.add(queryElement);
				break;
			case FILTER:
			case IMPORTABLE_FILTER:
				this.filterList.add(queryElement);
				break;
			default:
				break;
			}
		}
		if (this.dataset == null)
			this.dataset = queryElement.getDataset();
		if (this.config == null)
			this.config = queryElement.getConfig();
	}

    /**
     *
     * @return
     */
    public ArrayList<QueryElement> getQueryAttributeList() {
		return new ArrayList<QueryElement>(attributeList);
	}

    /**
     *
     * @return
     */
    public ArrayList<QueryElement> getQueryFilterList() {
		return new ArrayList<QueryElement>(filterList);
	}

    /**
     *
     * @return
     */
    public ArrayList<QueryElement> getExportables() {
		ArrayList<QueryElement> exportables = new ArrayList<QueryElement>();
		for (QueryElement qe : this.attributeList) {
			if (qe.getType() == QueryElementType.EXPORTABLE_ATTRIBUTE)
				exportables.add(qe);
		}
		return exportables;
	}

    /**
     *
     * @return
     */
    public int getExportablesSize() {
		int size = 0;
		for (QueryElement qe : this.attributeList) {
			if (qe.getType() == QueryElementType.EXPORTABLE_ATTRIBUTE)
				size += qe.getElementList().size();
		}
		return size;
	}

    /**
     *
     * @return
     */
    public List<QueryElement> getImportables() {
		List<QueryElement> importables = new ArrayList<QueryElement>();
		for (QueryElement qe : this.filterList) {
			if (qe.getType() == QueryElementType.IMPORTABLE_FILTER)
				importables.add(qe);
		}
		return importables;
	}

    /**
     *
     * @return
     */
    public int getImportablesSize() {
		int size = 0;
		for (QueryElement qe : this.filterList) {
			if (qe.getType() == QueryElementType.IMPORTABLE_FILTER)
				size += qe.getElementList().size();
		}
		return size;
	}

    /**
     *
     * @return
     */
    public String getProcessor() {
		return processor;
	}

    /**
     *
     * @param processor
     */
    public void setProcessor(String processor) {
		this.processor = processor;
	}

    /**
     *
     * @return
     */
    public int getLimit() {
		return limit;
	}

    /**
     *
     * @param limit
     */
    public void setLimit(int limit) {
		this.limit = limit;
	}

    /**
     *
     * @param temp_results
     * @param portablePosition
     * @param imp
     */
    public void updateQuery(List<List<String>> temp_results, int portablePosition, QueryElement imp, Boolean imp_only_sq) {
    	String quoteChar = "";
    	if (this.getDbType() == DBType.POSTGRES || getDbType() == DBType.ORACLE)
    		quoteChar = "\"";
		if (!this.emptyBatch) {
			int portableLength = imp.getElementList().size();
			//need to call this to guarantee that this.originalQueryString is set for the next line
            this.getQuery();
			Log.debug("Query string before update: " + (imp_only_sq ? this.queryString : this.originalQueryString));
			StringBuilder updatedQuery = new StringBuilder(imp_only_sq ? this.queryString : this.originalQueryString);
			HashSet<List<String>> indexValues = new HashSet<List<String>>();
			HashSet<String> values = new HashSet<String>();

            for (List<String> row : temp_results) {
				indexValues.add(row.subList(portablePosition, portablePosition + portableLength));
			}

            // link index is only working on EXACTLY ONE attribute, 
            // not attributeList. Even for attributeLists, its operates on first attribute only
            for (List<String> tuple : indexValues) {
				Log.debug("portable value: " + tuple.get(0));
				values.add(tuple.get(0));
			}

			if (this.dataset.getParentMart().getLinkIndices() != null
					&& this.dataset.getParentMart().getLinkIndices().getIndex(
							this.dataset, imp) != null) {
				// lets only keep the common values
                values.retainAll(this.dataset.getParentMart().getLinkIndices()
						.getIndex(this.dataset, imp));

			} else {
				System.err.println("Index NOT used!");
			}
			if (values.isEmpty())
				this.emptyBatch = true;
			if (this.isDatabase()) {
				if (updatedQuery.indexOf("WHERE ") < 0)
					updatedQuery.append(" WHERE ");
				else
					updatedQuery.append(" AND ");
				if (portableLength == 1) {
					Filter filter = (Filter) imp.getElementList().get(0);
					String filterColumn;
					if(!this.config.searchFromTarget()){
						String tableName = filter.getDatasetColumn().getSourceColumn().getTable().getName();
						updatedQuery.append("(")
                                .append(quoteChar)
                                .append(tableName)
                                .append(quoteChar)
                                .append(".");
						filterColumn = filter.getDatasetColumn().getSourceColumn().getName();
					}
                    else {
						filterColumn = filter.getDatasetColumn().getName();
						if (filter.getDatasetTable() == null) {
							System.out.println();
						}
						String tableName = filter.getDatasetTable().getName(this.dataset.getName());
						if (McUtils.hasPartitionBinding(tableName))
							tableName = McUtils
							.getRealName(tableName, this.dataset);
						if (tableName.endsWith("main")) {
							updatedQuery.append("(main.");
						}
                        else {
							updatedQuery.append("(").append(this.getDatabaseName())
                                    .append(".")
									.append(quoteChar).append(tableName)
                                    .append(quoteChar).append(".");
						}
					}
					switch (filter.getQualifier()) {
					case E:
						updatedQuery.append(quoteChar).append(filterColumn)
                                .append(quoteChar).append(" IN (");
						for (String value : values) {
							updatedQuery.append("'").append(value).append("',");
						}
						updatedQuery.deleteCharAt(updatedQuery.length() - 1)
                                .append("))");
						break;
					case LT:
					case LTE:
						updatedQuery.append(quoteChar).append(filterColumn).append(quoteChar)
                                .append(filter.getQualifier()).append(" (")
                                .append(Collections.max(values))
                                .append("))");
						break;
					case GT:
					case GTE:
						updatedQuery.append(quoteChar).append(filterColumn)
                                .append(quoteChar)
                                .append(filter.getQualifier())
                                .append(" (")
                                .append(Collections.min(values))
                                .append("))");
						break;
					case LIKE:
						// TODO figure out what to do in this case
						break;
					}

				} else { // It's a filterList
					updatedQuery.append("(");
					for (List<String> row : temp_results) {
						updatedQuery.append("(");
						String filterColumn;
						for (int i = 0; i < portableLength; ++i) {
							Filter filter = (Filter) imp.getElementList()
							.get(i);
							if(!this.config.searchFromTarget()){ //Virtual query
								String tableName = filter.getDatasetColumn().getSourceColumn().getTable().getName();
								updatedQuery.append("(")
                                        .append(quoteChar)
                                        .append(tableName)
                                        .append(quoteChar)
                                        .append(".");
								filterColumn = filter.getDatasetColumn().getSourceColumn().getName();
							}
                            else { // Non-virtual query
								String tableName = filter.getDatasetTable().getName(this.dataset.getName());
								filterColumn = filter.getDatasetColumn().getName();

								if (tableName.endsWith("main")) {
									updatedQuery.append("main.");
								} else {
									updatedQuery.append(this.getDatabaseName())
                                            .append(".").append(quoteChar)
                                            .append(tableName).append(quoteChar)
                                            .append(".");
								}
							}
							updatedQuery.append(quoteChar).append(filterColumn)
                                    .append(quoteChar)
									.append(filter.getQualifier())
									.append("'")
									.append(row.get(portablePosition + i))
                                    .append("' AND ");

						}
						updatedQuery.delete(updatedQuery.length() - 4,
								updatedQuery.length());
						updatedQuery.append(") OR ");
					}
					updatedQuery.delete(updatedQuery.length() - 3, updatedQuery
							.length());
					updatedQuery.append(")");
				}
			} else { // It's a WebService query
				for(int i = 0; i < imp.getElementList().size(); ++i){
				StringBuilder newFilter = new StringBuilder("<Filter name=\"");
				Filter filter = (Filter) imp.getElementList().get(i);
				newFilter.append(filter.getName()).append("\" value=\"");
				// TODO add in the values for the filter here
				for (List<String> filterValue : indexValues) {
					newFilter.append(filterValue.get(i)).append(",");
				}

				newFilter.append("\" />");

				updatedQuery.insert(updatedQuery.length() - 20, newFilter);
				}
			}
			this.queryString = updatedQuery.toString();
			Log.debug("UpdatedQuery string: " + this.queryString);
		}
	}

    /**
     *
     * @return
     */
    public String getQuery() {
		// my first visit, lets generate the SQL
		if (this.queryString == null) {
			QueryCompiler qc = new QueryCompiler(isCountQuery);
			this.queryString = qc.generateQuery(this, !this.config.searchFromTarget());
			this.originalQueryString = this.queryString;
		}
		return this.queryString;
	}

    /**
     *
     * @return
     */
    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{\"dataset\":\"").append(dataset.getName()).append("\",\"attributes\":[");

		for (int i = 0; i < attributeList.size(); ++i) {
			sb.append("\"").append(attributeList.get(i).toString()).append("\"");
			if (i != attributeList.size() - 1)
				sb.append(",");
		}

		sb.append("],\"filters\":[");

		for (int i = 0; i < filterList.size(); ++i) {
			sb.append("\"").append(filterList.get(i).toString()).append("\"");
			if (i != filterList.size() - 1)
				sb.append(",");
		}

		sb.append("]}");

		return sb.toString();
	}

    /**
     *
     * @param tablePartition
     * @throws TechnicalException
     * @throws SQLException
     */
    public final void connectToDatabase(int tablePartition) throws TechnicalException, SQLException {
		try {
			if (tablePartition == this.connection.size()) {
				this.connectionString = this.getConnectionString();
				Log.debug("Connecting...");
				Log.debug("Cx string: " + this.connectionString);
				Log.debug("User: " + this.getDatabaseUser());
				Log.debug("Password: " + StringUtils.repeat("*", this.getDatabasePassword().length()));
				Connection connectionObj = (Connection) DriverManager.getConnection(
						this.connectionString, this.getDatabaseUser(), this.getDatabasePassword());
				if (this.stream && this.isRdbmsType(DBType.POSTGRES)) {
					connectionObj.setAutoCommit(false);
				}
				this.connection.add(connectionObj);
			}
		} catch (SQLException e) {
			throw new TechnicalException(this.connectionString, e);
		}
	}

    /**
     *
     * @param query
     * @param tablePartition
     * @throws SQLException
     */
    protected final void prepareStreamingStatement(String query, int tablePartition) throws SQLException {
		if (this.stream) {
			Log.debug("preparing statement...");
			PreparedStatement prepareStatementObj = this.connection.get(tablePartition).prepareStatement(query,
					java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			if (this.isRdbmsType(DBType.MYSQL)) {
				prepareStatementObj.setFetchSize(Integer.MIN_VALUE);
			}
			if (this.isRdbmsType(DBType.POSTGRES) || this.isRdbmsType(DBType.ORACLE)) {
				prepareStatementObj.setFetchSize(QueryRunner.BATCH_SIZE);
			}
			this.preparedStatement.add(prepareStatementObj);
		} else {
			this.statement.add(this.connection.get(tablePartition).createStatement());
		}
	}

    /**
     *
     * @throws IOException
     * @throws SQLException
     * @throws TechnicalException
     */
    public void executeQuery() throws IOException, SQLException, TechnicalException {
		// dont pass zero as zero means add limit clause when its web browser
		// type client
		this.executeQuery(1);
	}

    /**
     *
     * @param qposition
     * @throws IOException
     * @throws SQLException
     * @throws TechnicalException
     */
    public void executeQuery(int qposition) throws IOException, SQLException, TechnicalException {
        // Kill count queries after 10 seconds if it hasn't returned results yet
        if (isCountQuery) {
            Runnable cleanUp = new Runnable() {
                public void run() {
                    try {
                        if (!isClosed && SubQuery.this.isDatabase()) {
                            Log.debug("Killing count query");
                            SubQuery.this.closeDBConnection();
                        } else if (!isClosed) {
                            SubQuery.this.resultSetURL.close();
                        }
                        isClosed = true;
                    } catch (Exception e) {
                        throw new BioMartException(e);
                    }
                }
            };

            QueryRunner.executor.schedule(cleanUp, 5, TimeUnit.SECONDS);
        }

		// reinitialise the resultsTable
		this.resultTable = new ArrayList<ArrayList<String>>();
		if (this.isDatabase()) {
            try {
                executeDatabaseQuery();
            // suppress SQL Exception if count query (To make timeouts work)
            } catch (SQLException e) {
                if (!isCountQuery) {
                    throw e;
                } else {
                    throw new BioMartException("Count query took too long");
                }
            }
		} else {
			executeMartServiceQuery();
		}
	}

    /**
     *
     * @throws SQLException
     * @throws TechnicalException
     */
    protected final void executeDatabaseQuery() throws SQLException, TechnicalException {
		String[] splitQuery = this.getQuery().split("\\|\\|", -1);
		String[] partitions = null;
		String partitionedQuery = null;
		if (splitQuery.length > 1) {
			partitions = splitQuery[1].split(",");
			this.totalTablePartitions = partitions.length;
		}
		for (int tablePartition = 0; tablePartition < this.totalTablePartitions; tablePartition++) {
			if (splitQuery.length > 1) {
				partitionedQuery = this.getQuery().replaceAll(
						"\\|\\|.*?\\|\\|", partitions[tablePartition]);
			} else
				partitionedQuery = this.getQuery();
			Log.debug(partitionedQuery);
			this.connectToDatabase(tablePartition);
			this.prepareStreamingStatement(partitionedQuery, tablePartition);
			Log.debug("resultSets associated with this SubQuery: "+ this.resultSet.size());

			ResultSet rs = stream ? preparedStatement.get(tablePartition).executeQuery() :
					statement.get(tablePartition).executeQuery(partitionedQuery);

			this.resultSet.add(rs);
		}

		ResultSetMetaData rsmd = this.resultSet.get(0).getMetaData();
		// reinitialise resultTable, rows, cols - cols not really need
		// reinitialisation
		this.totalCols = rsmd.getColumnCount();
	}

    /**
     *
     * @throws MalformedURLException
     * @throws IOException
     */
    protected final void executeMartServiceQuery() {
		// Prepare POST data
		Log.debug("URL: " + this.getWebServiceURL());
		// make OAuth request if needed
        if(this.config.getConsumerKey(this.dataset.getName()).length() > 0) {
            Log.debug("SENDING OAUTH request");

            OAuthRequest req = OAuthSigner.instance().buildRequest(
                Verb.POST, this.getWebServiceURL(), 
                this.config.getConsumerKey(this.dataset.getName()),
                this.config.getConsumerSecret(this.dataset.getName()),
                this.config.getAccessKey(this.dataset.getName()),
                this.config.getAccessSecret(this.dataset.getName())
                );

            req.addBodyParameter("query", this.getQuery());

            this.resultSetURL = new BufferedReader(
                new InputStreamReader(req.send().getStream()));
        }
        else {
            Log.debug("SENDING INSECURE(non-OAUTH) request");

            Client c = new Client();
            WebResource resource = c.resource(this.getWebServiceURL());

            MultivaluedMap formData = new MultivaluedMapImpl();
            formData.add("query", this.getQuery());

            try {
                InputStream is = resource
                        .type("application/x-www-form-urlencoded")
                        .post(InputStream.class, formData);

                // Construct data
                Log.debug("QUERY: " + this.getQuery());
                this.resultSetURL = new BufferedReader(
                    new InputStreamReader(is));
            } catch(UniformInterfaceException e) {
                String msg;
                if (e.getResponse().getStatus() == 405) {
                    msg = "Method Not Allowed";
                } else {
                    msg = e.getResponse().getEntity(String.class);
                }
                throw new BioMartException(String.format("HTTP Error %s - %s",
                        e.getResponse().getStatus(), msg));
            } catch(Exception e) {
                throw new BioMartException("Error while connection to HTTP source", e);
            }
        }
	}

    /**
     *
     * @throws SQLException
     * @throws IOException
     */
    public void cleanUp() throws SQLException, IOException {
		Log.debug("cleanUp called");
        this.resultTable.clear();
		this.resultTable = null;

		if (this.isDatabase()) {
			try {
				for (int i = 0; i < this.resultSet.size(); i++) {
					this.resultSet.get(i).close();
					this.resultSet.clear();
				}
				for (int i = 0; i < this.preparedStatement.size(); i++) {
					this.preparedStatement.get(i).close();
					this.preparedStatement.clear();
				}
				for (int i = 0; i < this.statement.size(); i++) {
					this.statement.get(i).close();
					this.statement.clear();
				}
			} catch (SQLException ex) {
				Logger.getLogger(SubQuery.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		} else {
			try {
				this.resultSetURL.close();
                // for non-OAUTH webservice queries
				if (this.resultSetURL_wr != null)
					this.resultSetURL_wr.close();
			} catch (IOException ex) {
				Logger.getLogger(SubQuery.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}
	}

    private boolean isClosed = false;

    /**
     *
     * @throws SQLException
     * @throws IOException
     */
    public void closeDBConnection() throws SQLException, IOException {
		if (!isClosed && this.isDatabase()) {
            try {
                    for (int i = 0; i < this.connection.size(); i++) {
                        if (this.connection.get(i) != null)
                            this.connection.get(i).close();
                    }
            } catch (SQLException ex) {
                Logger.getLogger(SubQuery.class.getName()).log(Level.SEVERE,
                        null, ex);
            } finally {
                isClosed = true;
            }
		}
	}

    /**
     *
     * @return
     */
    public String getWebServiceURL() {
        UrlLinkObject ulo = this.dataset.getDataLinkInfo().getUrlLinkObject();
        String port = ulo.getPort();
    	if(port.isEmpty() || "80".equals(port) || "443".equals(port)) {
    		return ulo.getFullHost()
    		+ ulo.getPath();
        } else {
    		return ulo.getFullHost()
    		+ ":"
    		+ ulo.getPort()
    		+ ulo.getPath();
        }
	}

    /**
     *
     * @return
     */
    public String getConnectionString() {
		return this.dataset.getDataLinkInfo().getJdbcLinkObject().getJdbcUrl();
	}

    /**
     *
     * @return
     */
    public String getDatabaseName() {
		return this.dataset.getDataLinkInfo().getJdbcLinkObject()
		.getDatabaseName();
	}
	
    /**
     *
     * @return
     */
    public String getSchemaName() {
		return this.dataset.getDataLinkInfo().getJdbcLinkObject().getSchemaName();
	}
	
    /**
     *
     * @return
     */
    public boolean useSchema() {
		return this.dataset.getDataLinkInfo().getJdbcLinkObject().useSchema();
	}
    
    /**
     *
     * @return
     */
    public boolean useDbName() {
		return this.dataset.getDataLinkInfo().getJdbcLinkObject().useDbName();
	}

    /**
     *
     * @return
     */
    public String getDatabaseUser() {
		return this.dataset.getDataLinkInfo().getJdbcLinkObject().getUserName();
	}

    /**
     *
     * @return
     */
    public String getDatabasePassword() {
		return this.dataset.getDataLinkInfo().getJdbcLinkObject().getPassword();
	}

    /**
     *
     * @param dbType
     * @return
     */
    public boolean isRdbmsType(DBType dbType) {
		if (this.dbType == dbType)
			return true;
		else
			return false;
	}

    /**
     *
     * @return
     */
    public Boolean isDatabase() {
		if (this.dataset.getDataLinkType().equals(DataLinkType.URL))
			return false;
		else
			return true;
	}

    /**
     *
     * @param batchStart
     * @param batchEnd
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public synchronized List<List<String>> getResults(int batchStart,
			int batchEnd) throws SQLException, IOException {

		List<List<String>> retRows = new ArrayList<List<String>>();
		// have not fully traversed the resultSet yet
		if (this.resultTable.size() < batchStart) {
			if (this.isDatabase()) {
				// TODO: what if the batch size exceeds total results we have
				// altogether
				// GOTCHA, this could potentially go beyond max, what happens
				// then ??
				// resolved, in the above said case, it breaks from the while
				// loop peacefully
				if (batchStart > 1) {
					// minus one because it leaves the pointer after the row
					// number
					// not needed anymore because we do FORWARD_ONLY, resultSet
					// scrolling is not possible
					// this.resultSet.absolute(batchStart-1);
				}
				GOTO: for (int tablePartition = 0; tablePartition < this.totalTablePartitions; tablePartition++) {
					while (this.resultSet.get(tablePartition).next()) {
						ArrayList<String> row = new ArrayList<String>();
						// 1 because resulSet is not a zero based index
						for (int i = 1; i <= this.totalCols; i++) {
                            final String col = this.resultSet.get(tablePartition)
									.getString(i);

                            // Commons impl. is faster than Oracle's libs:
							row.add(StringUtils.replace(StringUtils.replace(col, "\t", " "), "\n", " "));
						}
						retRows.add(row);

                        if (this.caching)
                            this.resultTable.add(row);

						batchStart++;
						if (batchStart > batchEnd)
							break GOTO;
					}
				}
			} else {
				String line = null;
				String[] cols = new String[this.totalCols];
				try {
					while ((line = this.resultSetURL.readLine()) != null) {
						ArrayList<String> row = new ArrayList<String>();
						//Log.debug("Line " + line.toString());
						cols = line.split("\t", this.totalCols);
						for (int i = 0; i < this.totalCols && i < cols.length; i++) {
							row.add(cols[i]);
						}
						retRows.add(row);

                        if (this.caching)
                            this.resultTable.add(row);

						batchStart++;
						if (batchStart > batchEnd)
							break;
					}
				} catch (ArrayIndexOutOfBoundsException ex) {
					Log.debug("GET RESULTS EXCEPTION, received this in results "
							+ line.toString());
				}
			}
		} else {
			Log.debug("UTILISING PREVIOUSLY VISITED RESULTS");
			batchStart--; // bcoz of zero based indices of resultTable
			int max = this.resultTable.size();
			while ((batchStart < batchEnd) && (batchStart < max)) {
				retRows.add(this.resultTable.get(batchStart++));
			}
		}

		return retRows;
	}

    /**
     *
     * @return
     */
    public boolean isEmptyBatch() {
		return emptyBatch;
	}

    /**
     *
     * @param client
     */
    public void setClient(String client) {
		this.client = client;

	}

    /**
     *
     * @return
     */
    public HashSet<DatasetTable> getTables() {
		HashSet<DatasetTable> allTables = new HashSet<DatasetTable>();
		for(QueryElement element : this.filterList){
			Filter filter = (Filter) element.getElement();
			if(filter!=null){
				if(filter.isFilterList()){
					for(Filter subfilter: filter.getFilterList()){
						allTables.add(subfilter.getDatasetTable());
					}
				} else {
					allTables.add(filter.getDatasetTable());
				}
			}
		}
		for(QueryElement element : this.attributeList){
			Attribute attribute = ((Attribute) element.getElement());
			if(attribute!=null)
				allTables.add(attribute.getDatasetTable());
		}
		return allTables;
	}

	public boolean isCountQuery() {
		return isCountQuery;
	}
}
