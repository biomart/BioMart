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

package org.biomart.configurator.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.biomart.common.exceptions.ConstructorException;
import org.biomart.common.exceptions.ListenerException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.controller.dialects.DatabaseDialect;
import org.biomart.configurator.controller.dialects.DialectFactory;
import org.biomart.configurator.model.JDBCDataLink;
import org.biomart.configurator.model.MartConstructorAction;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.type.JdbcType;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.runner.controller.MartRunnerProtocol;

/**
 * This implementation of the {@link MartConstructor} interface generates DDL
 * statements corresponding to each {@link MartConstructorAction}.
 * <p>
 * The implementation depends on both the source and target databases being
 * {@link JDBCSchema} instances, and that they are compatible as defined by
 * {@link JDBCSchema#canCohabit(DataLink)}.
 * <p>
 * DDL statements are generated and output either to a text buffer, or to one or
 * more files.
 * <p>
 * The databases must be available and online for the class to do anything, as
 * it queries the database on a number of occasions to find out things such as
 * partition values.
 * 
 */
public class SaveDDLMartConstructor implements MartConstructor {

	private File outputFile;

	private StringBuffer outputStringBuffer;

	private String outputHost;

	private String outputPort;

	private String overrideHost;

	private String overridePort;

	/**
	 * Creates a constructor that, when requested, will begin constructing a
	 * mart and outputting DDL to a file.
	 * 
	 * @param outputFile
	 *            the file to write the DDL to. Multi-file granularity will
	 *            write this file as a gzipped tar archive containing many plain
	 *            text files.
	 */
	public SaveDDLMartConstructor(final File outputFile) {
		Log.info("Saving DDL to " + outputFile.getPath());
		// Remember the settings.
		this.outputFile = outputFile;
		// This last call is redundant but is included for clarity.
		this.outputStringBuffer = null;
		this.outputHost = null;
		this.outputPort = null;
		this.overrideHost = null;
		this.overridePort = null;
	}

	/**
	 * Creates a constructor that, when requested, will begin constructing a
	 * mart and outputting DDL to a string buffer.
	 * 
	 * @param outputStringBuffer
	 *            the string buffer to write the DDL to. This parameter can only
	 *            be used if writing to a single file for all DDL. Any other
	 *            granularity will cause an exception.
	 */
	public SaveDDLMartConstructor(final StringBuffer outputStringBuffer) {
		Log.info("Saving DDL to buffer");
		// Remember the settings.
		this.outputStringBuffer = outputStringBuffer;
		// This last call is redundant but is included for clarity.
		this.outputFile = null;
		this.outputHost = null;
		this.outputPort = null;
		this.overrideHost = null;
		this.overridePort = null;
	}

	/**
	 * Creates a constructor that, when requested, will begin constructing a
	 * mart and outputting actions to the given host.
	 * 
	 * @param outputHost
	 *            the host to receive actions.
	 * @param outputPort
	 *            the port the host is listening on.
	 * @param overrideHost
	 *            the JDBC host to receive SQL.
	 * @param overridePort
	 *            the port the JDBC host is listening on.
	 */
	public SaveDDLMartConstructor(final String outputHost,
			final String outputPort, final String overrideHost,
			final String overridePort) {
		Log.info("Saving DDL to MartRunner");
		// Remember the settings.
		this.outputHost = outputHost;
		this.outputPort = outputPort;
		this.overrideHost = overrideHost;
		this.overridePort = overridePort;
		// Redundant but included for clarity.
		this.outputFile = null;
		this.outputStringBuffer = null;
	}

	public ConstructorRunnable getConstructorRunnable(
			final String targetDatabaseName, final String targetSchemaName,
			final Mart datasets, final Collection<String> prefixes)
			throws Exception {
		// First, make a set of all input schemas. We use a set to prevent
		// duplicates.
		final Set<SourceSchema> inputSchemas = new HashSet<SourceSchema>();
		inputSchemas.addAll(datasets.getIncludedSchemas());

		// Convert the set to a list.
		final List<SourceSchema> inputSchemaList = new ArrayList<SourceSchema>(inputSchemas);

		// Set the output dialect to match the first one in the list.
		Log.debug("Getting dialect");
		JdbcLinkObject jdbcLinkObject = ((SourceSchema) inputSchemaList.get(0)).getJdbcLinkObject();
		JdbcType jdbcType = jdbcLinkObject.getJdbcType();	// FIXME: NullPointerException sometimes occur here
		final DatabaseDialect dd = DialectFactory
				.getDialect(jdbcType);
		if (dd == null)
			throw new ConstructorException("unknownDialect");


		Log.debug("Working out what DDL helper to use");
		// Work out what kind of helper to use. The helper will
		// perform the actual conversion of action to DDL and divide
		// the results into appropriate files or buffers.
		final DDLHelper helper = this.outputStringBuffer == null ? this.outputHost != null ? (DDLHelper) new RemoteHostHelper(
				this.outputHost, this.outputPort, dd,
				(JDBCDataLink) inputSchemaList.get(0), this.overrideHost,
				this.overridePort, targetDatabaseName, targetSchemaName)
				: (DDLHelper) new TableAsFileHelper(this.outputFile, dd)
				: new SingleStringBufferHelper(this.outputStringBuffer, dd);
		Log.debug("Chose helper " + helper.getClass().getName());

		boolean usingMartRunner = this.outputFile == null && this.outputStringBuffer == null;	// at this stage if both are null it means we are using MartRunner
		dd.setUsingMartRunner(usingMartRunner);	// needed especially for mssql (if not using MartRunner, then "GO" statements should be issued for each command)
		
		// Construct and return the runnable that uses the helper
		// to do the actual work. Note how the helper is it's own
		// listener - it provides both database query facilities,
		// and converts action events back into DDL appropriate for
		// the database it is connected to.
		Log.debug("Building constructor runnable");
		final ConstructorRunnable cr = new GenericConstructorRunnable(
				targetDatabaseName, targetSchemaName, datasets, prefixes);
		cr.addMartConstructorListener(helper);
		return cr;
	}

	/**
	 * This abstract class is the base for all DDL helpers.
	 */
	private abstract static class DDLHelper implements MartConstructorListener {
		private DatabaseDialect dialect;

		private int tempTableSeq = 0;

		/**
		 * Constructs a DDL helper.
		 * 
		 * @param dialect
		 *            the language that this DDL helper will speak.
		 */
		protected DDLHelper(final DatabaseDialect dialect) {
			this.dialect = dialect;
		}

		/**
		 * Translates an action into commands, using
		 * {@link DatabaseDialect#getStatementsForAction(MartConstructorAction)}
		 * 
		 * @param action
		 *            the action to translate.
		 * @return the translated action. Usually the array will contain only
		 *         one entry, but when including comments or in certain other
		 *         circumstances, the DDL for the action may consist of a number
		 *         of individual statements, in which case each statement will
		 *         occupy one entry in the array. The array will be ordered in
		 *         the order the statements should be executed.
		 * @throws ConstructorException
		 *             if anything went wrong.
		 */
		protected String[] getStatementsForAction(
				final MartConstructorAction action) throws ConstructorException {
			return this.dialect.getStatementsForAction(action);
		}

		/**
		 * Obtain a unique temp table name to use.
		 * 
		 * @return a unique temp table name. Different on every call!
		 */
		public String getNewTempTableName() {
			return "TEMP__" + this.tempTableSeq++;
		}
	}

	/**
	 * Statements are saved altogether inside a string buffer.
	 */
	public static class SingleStringBufferHelper extends DDLHelper {

		private StringBuffer outputStringBuffer;

		/**
		 * Constructs a helper which will output all DDL into a single string
		 * buffer.
		 * 
		 * @param outputStringBuffer
		 *            the string buffer to write the DDL into.
		 * @param dialect
		 *            the type of SQL the buffer should present.
		 */
		public SingleStringBufferHelper(final StringBuffer outputStringBuffer,
				final DatabaseDialect dialect) {
			super(dialect);
			this.outputStringBuffer = outputStringBuffer;
		}

		public void martConstructorEventOccurred(final int event,
				final Object data, final MartConstructorAction action)
				throws ListenerException {
			if (event == MartConstructorListener.ACTION_EVENT) {
				final String[] cmd;
				try {
					// Convert the action to some DDL.
					cmd = this.getStatementsForAction(action);
				} catch (final ConstructorException ce) {
					throw new ListenerException(ce);
				}
				// Write the data.
				for (int i = 0; i < cmd.length; i++) {
					this.outputStringBuffer.append(cmd[i]);
					this.outputStringBuffer.append(";\n");
				}
			}
		}
	}

	/**
	 * Statements are saved as a single SQL file per table inside a Zip file.
	 */
	public static class TableAsFileHelper extends DDLHelper {

		private Map actions;

		private File file;

		private String dataset;

		private String partition;

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		/**
		 * Constructs a helper which will output all DDL into a single file per
		 * table inside the given zip file.
		 * 
		 * @param outputFile
		 *            the zip file to write the DDL into.
		 * @param dialect
		 *            the type of SQL the file should contain.
		 */
		public TableAsFileHelper(final File outputFile,
				final DatabaseDialect dialect) {
			super(dialect);
			this.actions = new LinkedHashMap();
			this.file = outputFile;
		}

		/**
		 * Retrieves the file we are writing to.
		 * 
		 * @return the file we are writing to.
		 */
		public File getFile() {
			return this.file;
		}

		public void martConstructorEventOccurred(final int event,
				final Object data, final MartConstructorAction action)
				throws ListenerException {
			try {
				if (event == MartConstructorListener.CONSTRUCTION_STARTED) {
					// Create and open the zip file.
					Log.debug("Starting zip file " + this.getFile().getPath());
					this.outputFileStream = new FileOutputStream(this.getFile());
					this.outputZipStream = new ZipOutputStream(
							this.outputFileStream);
					this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
				} else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
					// Close the zip stream. Will also close the
					// file output stream by default.
					Log.debug("Closing zip file");
					this.outputZipStream.finish();
					this.outputFileStream.flush();
					this.outputFileStream.close();
				} else if (event == MartConstructorListener.DATASET_STARTED) {
					// Clear out action map ready for next dataset.
					this.dataset = (String) data;
					Log.debug("Dataset " + this.dataset + " starting");
					this.actions.clear();
				} else if (event == MartConstructorListener.PARTITION_STARTED) {
					this.partition = (String) data;
					Log.debug("Partition " + this.partition + " starting");
				} else if (event == MartConstructorListener.DATASET_ENDED) {
					// Write out one file per table in files.
					Log.debug("Dataset ending");
					for (final Iterator i = this.actions.entrySet().iterator(); i
							.hasNext();) {
						final Map.Entry actionEntry = (Map.Entry) i.next();
						final String tableName = (String) actionEntry.getKey();
						final String entryFilename = this.partition + "/"
								+ this.dataset + "/" + tableName
								+ Resources.get("ddlExtension");
						Log.debug("Starting entry " + entryFilename);
						final ZipEntry entry = new ZipEntry(entryFilename);
						entry.setTime(System.currentTimeMillis());
						this.outputZipStream.putNextEntry(entry);
						// What actions are for this table?
						final List tableActions = (List) actionEntry.getValue();
						// Write the actions for the table itself.
						for (final Iterator j = tableActions.iterator(); j
								.hasNext();) {
							final MartConstructorAction nextAction = (MartConstructorAction) j
									.next();
							// Convert the action to some DDL.
							final String[] cmd;
							try {
								cmd = this.getStatementsForAction(nextAction);
							} catch (final ConstructorException ce) {
								throw new ListenerException(ce);
							}
							// Write the data.
							for (int k = 0; k < cmd.length; k++) {
								this.outputZipStream.write(cmd[k].getBytes());
								if (!(cmd[k].endsWith(";") || cmd[k]
										.endsWith("/")))
									this.outputZipStream.write(';');
								this.outputZipStream.write(System.getProperty(
										"line.separator").getBytes());
							}
						}
						// Done with this entry.
						Log.debug("Closing entry");
						this.outputZipStream.closeEntry();
					}
					// Write the dataset manifest.
					final ZipEntry entry = new ZipEntry(this.partition + "/"
							+ this.dataset + "/"
							+ Resources.get("datasetManifest"));
					entry.setTime(System.currentTimeMillis());
					this.outputZipStream.putNextEntry(entry);
					for (final Iterator i = this.actions.keySet().iterator(); i
							.hasNext();) {
						this.outputZipStream.write(((String) i.next())
								.getBytes());
						this.outputZipStream.write(Resources
								.get("ddlExtension").getBytes());
						this.outputZipStream.write(System.getProperty(
								"line.separator").getBytes());
					}
					this.outputZipStream.closeEntry();
				} else if (event == MartConstructorListener.ACTION_EVENT) {
					// Add the action to the current map.
					final String dsTableName = action.getDataSetTableName();
					if (!this.actions.containsKey(dsTableName))
						this.actions.put(dsTableName, new ArrayList());
					((List) this.actions.get(dsTableName)).add(action);
				}
			} catch (final IOException ie) {
				throw new ListenerException(ie);
			}
		}
	}

	/**
	 * Statements are transmitted to a remote host for execution.
	 */
	public static class RemoteHostHelper extends DDLHelper {

		private Map<String,List<MartConstructorAction>> actions;

		private String outputHost;

		private String outputPort;

		private String overrideHost;

		private String overridePort;

		private String job;

		private String dataset;

		private String targetDatabase;

		private String targetSchema;

		private String partition;

		private JDBCDataLink targetJDBCDataLink;

		private Socket clientSocket;

		/**
		 * Constructs a helper which will output all actions directly to the
		 * given host for interpretation.
		 * 
		 * @param outputHost
		 *            the host to send actions to.
		 * @param outputPort
		 *            the port the host is listening on.
		 * @param overrideHost
		 *            the JDBC host to send SQL to.
		 * @param overridePort
		 *            the port the JDBC host is listening on.
		 * @param dialect
		 *            the type of SQL the actions should contain.
		 * @param targetJDBCDataLink
		 *            the target JDBC connection to receive the SQL.
		 * @param targetDatabase
		 *            the database into which we will be building.
		 * @param targetSchema
		 *            the schema into which we will be building.
		 */
		public RemoteHostHelper(final String outputHost,
				final String outputPort, final DatabaseDialect dialect,
				final JDBCDataLink targetJDBCDataLink,
				final String overrideHost, final String overridePort,
				final String targetDatabase, final String targetSchema) {
			super(dialect);
			this.outputHost = outputHost;
			this.outputPort = outputPort;
			this.overrideHost = overrideHost;
			this.overridePort = overridePort;
			this.actions = new LinkedHashMap<String,List<MartConstructorAction>>();
			this.targetJDBCDataLink = targetJDBCDataLink;
			this.targetDatabase = targetDatabase;
			this.targetSchema = targetSchema;
		}

		/**
		 * Obtain the job Id for this job.
		 * 
		 * @return the job.
		 */
		public String getJobId() {
			return this.job;
		}

		public void martConstructorEventOccurred(final int event,
				final Object data, final MartConstructorAction action)
				throws ListenerException {
			try {
				if (event == MartConstructorListener.CONSTRUCTION_STARTED) {
					Log.debug("Starting MartRunner job definition");
					// Write the opening message to the socket.
					this.clientSocket = MartRunnerProtocol.Client
							.createClientSocket(this.outputHost,
									this.outputPort);
					this.job = MartRunnerProtocol.Client
							.newJob(this.clientSocket);
					// Substitute JDBC url with alternative
					// JDBC host and port.
					String url = this.targetJDBCDataLink.getUrl();
					if (this.overrideHost != null
							&& this.overridePort != null
							&& this.overrideHost.trim().length()
									+ this.overridePort.trim().length() > 0)
						url = url.replaceAll("(//|@)[^:]+:\\d+", "$1"
								+ this.overrideHost + ":" + this.overridePort);
					url = url.replaceAll(this.targetJDBCDataLink
							.getDataLinkDatabase(), targetDatabase);
					MartRunnerProtocol.Client.beginJob(this.clientSocket,
							this.job, this.targetSchema,
							this.targetJDBCDataLink.getDriverClassName(), url,
							this.targetJDBCDataLink.getUsername(),
							this.targetJDBCDataLink.getPassword());
				} else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
					Log.debug("Finished MartRunner job definition");
					// Write the closing message to the socket.
					MartRunnerProtocol.Client.endJob(this.clientSocket,
							this.job);
					this.clientSocket.close();
				} else if (event == MartConstructorListener.DATASET_STARTED) {
					// Clear out action map ready for next dataset.
					this.dataset = (String) data;
					Log.debug("Dataset " + this.dataset + " starting");
					this.actions.clear();
				} else if (event == MartConstructorListener.PARTITION_STARTED) {
					// Clear out action map ready for next dataset.
					this.partition = (String) data;
					Log.debug("Partition " + this.partition + " starting");
				} else if (event == MartConstructorListener.DATASET_ENDED) {
					// Write out one file per table in files.
					Log.debug("Dataset ending, writing actions now");
					for (final Iterator<Map.Entry<String, List<MartConstructorAction>>> i = this.actions.entrySet().iterator(); i
							.hasNext();) {
						final Map.Entry<String, List<MartConstructorAction>> actionEntry = i.next();
						// What table is this?
						final String tableName = (String) actionEntry.getKey();
						// What actions are for this table?
						final List<MartConstructorAction> tableActions = (List<MartConstructorAction>) actionEntry.getValue();
						// Write the actions for the table itself.
						final List<String> actions = new ArrayList<String>();
						for (final Iterator<MartConstructorAction> j = tableActions.iterator(); j
								.hasNext();)
							try {
								// Convert the action to some DDL.
								actions.addAll(Arrays.asList(this.getStatementsForAction(j.next())));
							} catch (final ConstructorException ce) {
								throw new ListenerException(ce);
							}
						// Write the data.
						MartRunnerProtocol.Client.setActions(this.clientSocket,
								this.job, this.partition, this.dataset,
								tableName, (String[]) actions
										.toArray(new String[0]));
					}
				} else if (event == MartConstructorListener.ACTION_EVENT) {
					// Add the action to the current map.
					final String dsTableName = action.getDataSetTableName();
					if (!this.actions.containsKey(dsTableName))
						this.actions.put(dsTableName, new ArrayList<MartConstructorAction>());
					(this.actions.get(dsTableName)).add(action);
				}
			} catch (final Throwable pe) {
				throw new ListenerException(pe);
			}
		}
	}
}
