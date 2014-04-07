/*
 Copyright (C) 2006 EBI
 
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

package org.biomart.runner.model;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.ListBackedMap;
import org.biomart.configurator.utils.McUtils;
import org.biomart.runner.controller.JobHandler;
import org.biomart.runner.exceptions.JobException;
import org.jdom.Element;

/**
 * Handles planning and execution of jobs. The maximum number of threads allowed
 * is controlled by the 'maxthreads' property in the BioMart properties file.
 * See {@link Settings#getProperty(String)}.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.21 $, $Date: 2007/12/21 12:03:40 $, modified by
 *          $Author: rh4 $
 * @since 0.6
 */
public class JobPlan implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String jobId;

	private static final int MAX_THREAD_COUNT = Integer.parseInt(McUtils.isStringEmpty(Settings
			.getProperty("maxthreads")) ? "5" : Settings
			.getProperty("maxthreads"));

	private String JDBCDriverClassName;

	private String JDBCURL;

	private String JDBCUsername;

	private String JDBCPassword;

	private int threadCount;

	private String contactEmailAddress="";

	private final JobPlanSection root;

	private final Map<String,JobPlanSection> sectionIds = new HashMap<String,JobPlanSection>();

	private boolean skipDropTable;

	private String targetSchema;

	/**
	 * Create a new job plan.
	 * 
	 * @param jobId
	 *            the id of the job this plan is for.
	 */
	public JobPlan(final String jobId) {
		this.root = new JobPlanSection(jobId, this, null);
		this.jobId = jobId;
		this.threadCount = 1;
		this.skipDropTable = false;
	}

	/**
	 * Create a new job plan by duplication.
	 * 
	 * @param jobId
	 *            the id of the job this plan is for.
	 * @param plan
	 *            the plan to copy.
	 */
	public JobPlan(final String jobId, final JobPlan plan) {
		this.root = new JobPlanSection(jobId, this, null);
		this.jobId = jobId;
		this.threadCount = plan.threadCount;
		this.skipDropTable = plan.skipDropTable;
		this.JDBCDriverClassName = plan.JDBCDriverClassName;
		this.JDBCURL = plan.JDBCURL;
		this.JDBCUsername = plan.JDBCUsername;
		this.JDBCPassword = plan.JDBCPassword;
		this.contactEmailAddress = plan.contactEmailAddress;
		this.targetSchema = plan.targetSchema;
	}

	/**
	 * Override this method if you want to know when a job starts.
	 * 
	 * @throws JobException
	 *             if anything none-database-ish goes wrong.
	 */
	public void callbackStart() throws JobException {

	}

	/**
	 * Override this method if you want to know when a job ends.
	 * 
	 * @throws JobException
	 *             if anything none-database-ish goes wrong.
	 */
	public void callbackEnd() throws JobException {

	}

	/**
	 * Override this method if you want to get the results of any SQL statements
	 * that result in results (e.g. select statements).
	 * 
	 * @param action
	 *            the action that produced the results.
	 * @param rs
	 *            the resultset containing the results.
	 * @throws SQLException
	 *             if anything goes wrong because of the database.
	 * @throws JobException
	 *             if anything none-database-ish goes wrong.
	 */
	public void callbackResults(final JobPlanAction action, final ResultSet rs)
			throws SQLException, JobException {
		// Does nothing.
	}

	/**
	 * Get the starting point for the plan.
	 * 
	 * @return the starting section.
	 */
	public JobPlanSection getRoot() {
		return this.root;
	}

	/**
	 * Set the database schema into which we will be building.
	 * 
	 * @param targetSchema
	 *            the schema name.
	 */
	public void setTargetSchema(final String targetSchema) {
		this.targetSchema = targetSchema;
	}

	/**
	 * Obtain the database schema into which we will be building.
	 * 
	 * @return the schema name.
	 */
	public String getTargetSchema() {
		return this.targetSchema;
	}

	/**
	 * Obtain the section with the given ID.
	 * 
	 * @param sectionId
	 *            the ID.
	 * @return the section.
	 */
	public JobPlanSection getJobPlanSection(final String sectionId) {
		return (JobPlanSection) this.sectionIds.get(sectionId);
	}

	/**
	 * Set an action count.
	 * 
	 * @param sectionPath
	 *            the section this applies to.
	 * @param actionCount
	 *            the action count to set.
	 */
	public void setActionCount(final String[] sectionPath, final int actionCount) {
		JobPlanSection section = this.getRoot();
		for (int i = 0; i < sectionPath.length; i++)
			section = section.getSubSection(sectionPath[i]);
		section.setActionCount(actionCount);
	}

	/**
	 * Get the id of the job this plan is for.
	 * 
	 * @return the id of the job.
	 */
	public String getJobId() {
		return this.jobId;
	}

	/**
	 * @return the threadCount
	 */
	public int getThreadCount() {
		return this.threadCount;
	}

	/**
	 * @param threadCount
	 *            the threadCount to set
	 */
	public void setThreadCount(final int threadCount) {
		this.threadCount = threadCount;
	}

	/**
	 * @return the threadCount
	 */
	public int getMaxThreadCount() {
		return JobPlan.MAX_THREAD_COUNT;
	}

	/**
	 * @return the contactEmailAddress
	 */
	public String getContactEmailAddress() {
		return this.contactEmailAddress;
	}

	/**
	 * @param contactEmailAddress
	 *            the contactEmailAddress to set
	 */
	public void setContactEmailAddress(final String contactEmailAddress) {
		this.contactEmailAddress = contactEmailAddress;
	}

	/**
	 * @return the JDBCDriverClassName
	 */
	public String getJDBCDriverClassName() {
		return this.JDBCDriverClassName;
	}

	/**
	 * @param driverClassName
	 *            the JDBCDriverClassName to set
	 */
	public void setJDBCDriverClassName(final String driverClassName) {
		this.JDBCDriverClassName = driverClassName;
	}

	/**
	 * @return the JDBCPassword
	 */
	public String getJDBCPassword() {
		return this.JDBCPassword;
	}

	/**
	 * @param password
	 *            the JDBCPassword to set
	 */
	public void setJDBCPassword(final String password) {
		this.JDBCPassword = password;
	}

	/**
	 * @return the JDBCURL
	 */
	public String getJDBCURL() {
		return this.JDBCURL;
	}

	/**
	 * @param jdbcurl
	 *            the JDBCURL to set
	 */
	public void setJDBCURL(final String jdbcurl) {
		this.JDBCURL = jdbcurl;
	}

	/**
	 * @return the JDBCUsername
	 */
	public String getJDBCUsername() {
		return this.JDBCUsername;
	}

	/**
	 * @param username
	 *            the JDBCUsername to set
	 */
	public void setJDBCUsername(final String username) {
		this.JDBCUsername = username;
	}

	/**
	 * Should we skip drop-table statements?
	 * 
	 * @return <tt>true</tt> if we should.
	 */
	public boolean isSkipDropTable() {
		return this.skipDropTable;
	}

	/**
	 * Should we skip drop-table statements?
	 * 
	 * @param skipDropTable
	 *            <tt>true</tt> if we should.
	 */
	public void setSkipDropTable(final boolean skipDropTable) {
		this.skipDropTable = skipDropTable;
	}

	public int hashCode() {
		return this.jobId.hashCode();
	}

	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append(this.jobId);
		if (this.root.getStatus().equals(JobStatus.INCOMPLETE)) {
			buf.append(" [");
			buf.append(Resources.get("jobStatusIncomplete"));
			buf.append("]");
		}
		buf.append(" (");
		buf.append(this.root.getTotalActionCount());
		buf.append(")");
		return buf.toString();
	}

	public boolean equals(final Object other) {
		if (!(other instanceof JobPlan))
			return false;
		return this.jobId.equals(((JobPlan) other).getJobId());
	}

	/**
	 * Establish a connection.
	 * 
	 * @return the connection.
	 * @throws SQLException
	 *             if it couldn't connect.
	 * @throws JobException
	 *             if it couldn't connect.
	 */
	public Connection getConnection() throws SQLException, JobException {
		final Class loadedDriverClass;
		try {
			// Start out by loading the driver.
			loadedDriverClass = Class.forName(this.getJDBCDriverClassName());
		} catch (final Exception e) {
			throw new JobException(e);
		}

		// Check it really is an instance of Driver.
		if (!Driver.class.isAssignableFrom(loadedDriverClass))
			throw new ClassCastException(Resources
					.get("driverClassNotJDBCDriver"));

		// Connect!
		final Properties properties = new Properties();
		properties.setProperty("user", this.getJDBCUsername());
		final String pwd = this.getJDBCPassword();
		if (!pwd.equals(""))
			properties.setProperty("password", pwd);
		properties.setProperty("nullCatalogMeansCurrent", "false");
		return DriverManager.getConnection(this.getJDBCURL(), properties);
	}

	/**
	 * Build a job that searches for empty tables (those with non-key columns
	 * that have all nulls in those columns) and generates UNQUEUED drop
	 * statements to be executed at a later date.
	 * 
	 * @throws SQLException
	 *             if anything went wrong.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public void makeEmptyTableJob() throws SQLException, JobException {
		// Make a job to put the statements into, with a callback
		// which generates the drop statements.
		final String jobPlanId = JobHandler.nextJobId();
		final JobPlan jobPlan = new JobPlan(jobPlanId, this) {
			private static final long serialVersionUID = 1L;

			private transient List<String> actions;

			public void callbackStart() throws JobException {
				this.actions = new ArrayList<String>();
			}

			public void callbackEnd() throws JobException {
				// Where do our statements go?
				final String dropSectionName = Resources
				.get("dropTableSection");
				final JobPlanSection jobPlanSection = this.getRoot()
						.getSubSection(dropSectionName);
				// Convert the SQL into an action.
				JobHandler.setActions(jobPlanId,
						new String[] { dropSectionName }, this.actions);
				this.setActionCount(new String[] { dropSectionName },
						this.actions.size());
				// Unqueue the action.
				JobHandler.setStatus(jobPlanId, Collections
						.singleton(jobPlanSection.getIdentifier()),
						JobStatus.NOT_QUEUED, null);
			}

			public void callbackResults(final JobPlanAction action,
					final ResultSet rs) throws SQLException, JobException {
				rs.next();
				final int count = rs.getInt(1);
				// If count is 0, we have 0 non-null rows.
				if (count == 0) {
					// Drop table.
					// What table are we dropping?
					final String table = action.getAction().split("\\s+")[3];
					// Build the SQL.
					final StringBuffer sql = new StringBuffer();
					sql.append("drop table ");
					sql.append(table);
					this.actions.add(sql.toString());
				}
			}
		};
		JobHandler.getJobList().addJob(jobPlan);

		// Open connection.
		final Connection conn = this.getConnection();

		// Get database metadata, catalog, and schema details.
		final DatabaseMetaData dmd = conn.getMetaData();
		final String catalog = conn.getCatalog();
		final String schema = this.getTargetSchema();

		// Gather columns for table.
		final Map<String,Set<String>> tableMap = new HashMap<String,Set<String>>();
		ResultSet rs = null;
		try {
			rs = dmd.getColumns("".equals(dmd.getSchemaTerm()) ? schema
					: catalog, schema, "%", "%");
			// FIXME: When using Oracle, if the table is a synonym then the
			// above call returns no results.
			while (rs.next()) {
				// Skip non-nullable columns.
				if (rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls)
					continue;
				// Include all other columns.
				final String table = rs.getString("TABLE_NAME");
				final String col = rs.getString("COLUMN_NAME");
				if (!tableMap.containsKey(table))
					tableMap.put(table, new HashSet<String>());
					tableMap.get(table).add(col);
			}
		} finally {
			try {
				// Close connection.
				if (rs != null)
					rs.close();
			} finally {
				conn.close();
			}
		}

		// Iterate over the columns gathered to construct actions.
		for (final Iterator<Map.Entry<String,Set<String>>> i = tableMap.entrySet().iterator(); i.hasNext();) {
			final Map.Entry<String,Set<String>> entry = i.next();
			final String table = (String) entry.getKey();
			final Collection<String> nonKeyCols = new HashSet<String>();
			for (final Iterator<String> j =  entry.getValue().iterator(); j
					.hasNext();) {
				final String col = (String) j.next();
				// Divide into key/non-key cols.
				if (!col.endsWith(Resources.get("keySuffix")))
					nonKeyCols.add(col);
			}
			// Ignore tables which have no non-key cols.
			if (nonKeyCols.isEmpty())
				continue;
			// Build the count SQL for this table. We are counting
			// non-null rows.
			final StringBuffer sql = new StringBuffer();
			sql.append("select count(1) from ");
			sql.append(this.getTargetSchema());
			sql.append('.');
			sql.append(table);
			sql.append(" where ");
			for (final Iterator<String> k = nonKeyCols.iterator(); k.hasNext();) {
				final String nonKeyCol =  k.next();
				sql.append(nonKeyCol);
				sql.append(" is not null");
				if (k.hasNext())
					sql.append(" or ");
			}
			// Convert the SQL into an action.
			JobHandler.setActions(jobPlanId, new String[] { table },
					Collections.singleton(sql.toString()));
			this.setActionCount(new String[] { table }, 1);
		}
		// Queue the job.
		JobHandler.setStatus(jobPlanId, Collections.singleton(jobPlan.getRoot()
				.getIdentifier()), JobStatus.QUEUED, null);
	}

	public Map<String,JobPlanSection> getSectionIds() {
		return this.sectionIds;
	}
	
	public Element generateXML() {
		Element element = new Element("jobplan");
		element.setAttribute("id", this.jobId);
		element.setAttribute("JDBCDriverClassName",this.JDBCDriverClassName);
		element.setAttribute("JDBCURL",this.JDBCURL);
		element.setAttribute("JDBCUsername",this.JDBCUsername);
		element.setAttribute("JDBCPassword",this.JDBCPassword);
		element.setAttribute("contactEmailAddress",this.contactEmailAddress==null?"":this.contactEmailAddress);
		JobPlanSection root = this.getRoot();
		if(root!=null) {
			Element rootelement = root.generateXML();
			element.addContent(rootelement);
		}
		return element;
	}
	
}
