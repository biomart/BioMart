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

package org.biomart.runner.controller;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.CompressedBlockInputStream;
import org.biomart.common.utils.CompressedBlockOutputStream;
import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobList;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobPlanAction;
import org.biomart.runner.model.JobPlanSection;

/**
 * Handles client communication.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.10 $, $Date: 2008/02/22 11:37:12 $, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class MartRunnerProtocol {

	private static final int COMPRESSED_BUFFER_SIZE = 8192; // 8k

	private static final String NEW_JOB = "NEW_JOB";

	private static final String BEGIN_JOB = "BEGIN_JOB";

	private static final String END_JOB = "END_JOB";

	private static final String LIST_JOBS = "LIST_JOBS";

	private static final String REMOVE_JOB = "REMOVE_JOB";

	private static final String SET_ACTIONS = "SET_ACTIONS";

	private static final String GET_ACTIONS = "GET_ACTIONS";

	private static final String EMAIL_ADDRESS = "EMAIL_ADDRESS";

	private static final String SKIP_DROP_TABLE = "SKIP_DROP_TABLE";

	private static final String THREAD_COUNT = "THREAD_COUNT";

	private static final String START_JOB = "START_JOB";

	private static final String STOP_JOB = "STOP_JOB";

	private static final String QUEUE = "QUEUE";

	private static final String UNQUEUE = "UNQUEUE";

	private static final String MOVE_SECTION = "MOVE_SECTION";

	private static final String UPDATE_ACTION = "UPDATE_ACTION";

	private static final String EMPTY_TABLES = "EMPTY_TABLES";

	// Short-cut for ending messages and actions.
	private static final String END_MESSAGE = "___END_MESSAGE___";

	private static final String NEXT = "___NEXT___";

	/**
	 * Handles a client communication attempt. Receives an open socket and
	 * should return it still open.
	 * 
	 * @param clientSocket
	 *            the sccket to communicate over.
	 * @throws ProtocolException
	 *             if anything went wrong.
	 */
	public static void handleClient(final Socket clientSocket)
			throws ProtocolException {
		// Translates client requests into individual methods.
		try {
			final ObjectInputStream ois = new ObjectInputStream(
					new CompressedBlockInputStream(clientSocket
							.getInputStream()));
			final ObjectOutputStream oos = new ObjectOutputStream(
					new CompressedBlockOutputStream(clientSocket
							.getOutputStream(),
							MartRunnerProtocol.COMPRESSED_BUFFER_SIZE));
			String command = null;
			try {
				while ((command = (String) ois.readObject()) != null) {
					Log.debug("Received command: " + command);
					// What do they want us to do?
					MartRunnerProtocol.class.getMethod(
							"handle_" + command,
							new Class[] { ObjectInputStream.class,
									ObjectOutputStream.class }).invoke(null,
							new Object[] { ois, oos });
				}
			} catch (final EOFException eof) {
				// So what?
			}
		} catch (final InvocationTargetException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof ProtocolException)
				throw (ProtocolException) cause;
			else if (cause instanceof IOException)
				throw new ProtocolException(Resources.get("protocolIOProbs"),
						cause);
			else
				throw new ProtocolException(cause);
		} catch (final IllegalAccessException e) {
			Log.debug("Command recognised but unavailable, ignoring");
		} catch (final NoSuchMethodException e) {
			Log.debug("Command unrecognised, ignoring");
		} catch (final Throwable t) {
			throw new ProtocolException(Resources.get("protocolIOProbs"), t);
		}
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_NEW_JOB(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		// Write out a new job ID.
		out.writeObject(JobHandler.nextJobId());
		out.flush();
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_BEGIN_JOB(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final String targetSchema = (String) in.readObject();
		final String jdbcDriverClassName = (String) in.readObject();
		final String jdbcURL = (String) in.readObject();
		final String jdbcUsername = (String) in.readObject();
		final String jdbcPassword = (String) in.readObject();
		JobHandler.beginJob(jobId, targetSchema, jdbcDriverClassName, jdbcURL,
				jdbcUsername, jdbcPassword);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_END_JOB(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		JobHandler.endJob((String) in.readObject());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_REMOVE_JOB(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		JobHandler.removeJob((String) in.readObject());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 */
	public static void handle_SET_ACTIONS(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final String[] sectionPath = ((String) in.readObject()).split(",");
		final StringBuffer actions = new StringBuffer();
		final Collection<String> finalActions = new ArrayList<String>();
		String line;
		while (!(line = (String) in.readObject())
				.equals(MartRunnerProtocol.END_MESSAGE))
			if (line.equals(MartRunnerProtocol.NEXT)) {
				final String action = actions.toString();
				finalActions.add(action);
				Log.debug("Receiving action: " + action);
				actions.setLength(0);
			} else
				actions.append(line);
		JobHandler.setActions(jobId, sectionPath, finalActions);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 */
	public static void handle_QUEUE(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final Collection<String> identifiers = new ArrayList<String>();
		String line;
		while (!(line = (String) in.readObject())
				.equals(MartRunnerProtocol.END_MESSAGE))
			identifiers.add(line);
		JobHandler.queue(jobId, identifiers);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_UNQUEUE(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final Collection<String> identifiers = new ArrayList<String>();
		String line;
		while (!(line = (String) in.readObject())
				.equals(MartRunnerProtocol.END_MESSAGE))
			identifiers.add(line);
		JobHandler.unqueue(jobId, identifiers);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_LIST_JOBS(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		out.writeObject(JobHandler.getJobList());
		out.flush();
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_UPDATE_ACTION(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final String sectionId = (String) in.readObject();
		final String actionId = (String) in.readObject();
		final String action = (String) in.readObject();
		JobHandler.updateAction(jobId, sectionId, actionId, action);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_EMPTY_TABLES(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		JobHandler.makeEmptyTableJob(jobId);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_GET_ACTIONS(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final String sectionId = (String) in.readObject();
		out.writeObject(new ArrayList<JobPlanAction>(JobHandler.getActions(jobId, sectionId)
				.values()));
		out.flush();
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_SKIP_DROP_TABLE(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final boolean value = ((Boolean) in.readObject()).booleanValue();
		JobHandler.setSkipDropTable(jobId, Boolean.valueOf(value)
				.booleanValue());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_EMAIL_ADDRESS(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final String email = (String) in.readObject();
		JobHandler.setEmailAddress(jobId, email);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_THREAD_COUNT(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final int threadCount = ((Integer) in.readObject()).intValue();
		JobHandler.setThreadCount(jobId, threadCount);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_MOVE_SECTION(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		final String jobId = (String) in.readObject();
		final String sectionId = (String) in.readObject();
		final String newPredecessorSectionId = (String) in.readObject();
		JobHandler.moveSection(jobId, sectionId, newPredecessorSectionId
				.length() == 0 ? null : newPredecessorSectionId);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_START_JOB(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		JobHandler.startJob((String) in.readObject());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws Exception
	 *             if the protocol fails.
	 */
	public static void handle_STOP_JOB(final ObjectInputStream in,
			final ObjectOutputStream out) throws Exception {
		JobHandler.stopJob((String) in.readObject());
	}

	/**
	 * Contains public methods for use by the client end of the protocol.
	 */
	public static class Client {

		/**
		 * Open a socket to the host.
		 * 
		 * @param host
		 *            the host.
		 * @param port
		 *            the port.
		 * @return the socket.
		 * @throws IOException
		 *             if it could not be opened.
		 */
		public static Socket createClientSocket(String host,
				 String port) throws IOException {
			//hack for now
			if(null==host || "".equals(host)) {
				host = MartRunnerMonitorDialog.shost;
				port = MartRunnerMonitorDialog.sport;
			}
			return new Socket(host, Integer.parseInt(port)) {
				private final OutputStream os = new ObjectOutputStream(
						new CompressedBlockOutputStream(
								super.getOutputStream(),
								MartRunnerProtocol.COMPRESSED_BUFFER_SIZE));

				private InputStream is;

				public OutputStream getOutputStream() throws IOException {
					return this.os;
				}

				public InputStream getInputStream() throws IOException {
					if (this.is == null)
						this.is = new ObjectInputStream(
								new CompressedBlockInputStream(super
										.getInputStream()));
					return this.is;
				}

				public void close() throws IOException {
					if (this.os!=null)
						this.os.flush();
					super.close();
				}
			};
		}

		/**
		 * Request a job ID that can be used for a new job. Does not actually
		 * begin the job.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @return the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static String newJob(final Socket clientSocket)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.NEW_JOB);
				oos.flush();
				return (String) ((ObjectInputStream) clientSocket
						.getInputStream()).readObject();
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job is beginning to be notified.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param targetSchema
		 *            the schema into which we will be writing.
		 * @param jdbcDriverClassName
		 *            the JDBC driver classname for the server the job will run
		 *            against.
		 * @param jdbcURL
		 *            the JDBC URL of the server the job will run against.
		 * @param jdbcUsername
		 *            the JDBC username for the server the job will run against.
		 * @param jdbcPassword
		 *            the JDBC password for the server the job will run against.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void beginJob(final Socket clientSocket,
				final String jobId, final String targetSchema,
				final String jdbcDriverClassName, final String jdbcURL,
				final String jdbcUsername, final String jdbcPassword)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.BEGIN_JOB);
				oos.writeObject(jobId);
				oos.writeObject(targetSchema);
				oos.writeObject(jdbcDriverClassName);
				oos.writeObject(jdbcURL);
				oos.writeObject(jdbcUsername);
				oos.writeObject(jdbcPassword);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job is ending being notified.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void endJob(final Socket clientSocket, final String jobId)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.END_JOB);
				oos.writeObject(jobId);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job can be removed and forgotten.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void removeJob(final Socket clientSocket,
				final String jobId) throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.REMOVE_JOB);
				oos.writeObject(jobId);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Request a list of current jobs as {@link JobPlan} objects.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @return the list of jobs.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static JobList listJobs(final Socket clientSocket)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.LIST_JOBS);
				oos.flush();
				return (JobList) ((ObjectInputStream) clientSocket
						.getInputStream()).readObject();
			} catch (final ClassNotFoundException e) {
				throw new ProtocolException(e);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Update an action in a job.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param section
		 *            the section to update.
		 * @param action
		 *            the action to update.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void updateAction(final Socket clientSocket,
				final String jobId, final JobPlanSection section,
				final JobPlanAction action) throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.UPDATE_ACTION);
				oos.writeObject(jobId);
				oos.writeObject(section.getIdentifier());
				oos.writeObject(action.getIdentifier());
				oos.writeObject(action.getAction());
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Kick off an empty table thang.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void makeEmptyTableJob(final Socket clientSocket,
				final String jobId) throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.EMPTY_TABLES);
				oos.writeObject(jobId);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Add actions to a job by defining a new section.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param partition
		 *            the partition to add to.
		 * @param dataset
		 *            the dataset to add to.
		 * @param table
		 *            the table to add to.
		 * @param actions
		 *            the SQL statement(s) to add.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void setActions(final Socket clientSocket,
				final String jobId, final String partition,
				final String dataset, final String table, final String[] actions)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.SET_ACTIONS);
				oos.writeObject(jobId);
				oos.writeObject(partition + "," + dataset + "," + table);
				for (int i = 0; i < actions.length; i++) {
					oos.writeObject(actions[i]);
					oos.writeObject(MartRunnerProtocol.NEXT);
				}
				oos.writeObject(MartRunnerProtocol.END_MESSAGE);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Retrieve job plan nodes for a given section.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param jobSection
		 *            the section to get nodes for.
		 * @return the job plan.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static Collection<JobPlanAction> getActions(final Socket clientSocket,
				final String jobId, final JobPlanSection jobSection)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.GET_ACTIONS);
				oos.writeObject(jobId);
				oos.writeObject(jobSection.getIdentifier());
				oos.flush();
				return (Collection<JobPlanAction>) ((ObjectInputStream) clientSocket
						.getInputStream()).readObject();
			} catch (final ClassNotFoundException e) {
				throw new ProtocolException(e);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job skip drop status has changed.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param skipDropTable
		 *            the new status - <tt>true</tt> to turn it on.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void setSkipDropTable(final Socket clientSocket,
				final String jobId, final boolean skipDropTable)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.SKIP_DROP_TABLE);
				oos.writeObject(jobId);
				oos.writeObject(Boolean.valueOf(skipDropTable));
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job email address has changed.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param email
		 *            the new email address.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void setEmailAddress(final Socket clientSocket,
				final String jobId, final String email)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.EMAIL_ADDRESS);
				oos.writeObject(jobId);
				oos.writeObject(email == null ? "" : email);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job thread count has changed.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param threadCount
		 *            the new thread count.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void setThreadCount(final Socket clientSocket,
				final String jobId, final int threadCount)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.THREAD_COUNT);
				oos.writeObject(jobId);
				oos.writeObject(new Integer(threadCount));
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Move a section to before the specified one.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param sectionId
		 *            the section ID to move.
		 * @param newPredecessorSectionId
		 *            the section ID to place this one after, or <tt>null</tt>
		 *            if it is to go at the top if its current sibling list.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void moveSection(final Socket clientSocket,
				final String jobId, final String sectionId,
				final String newPredecessorSectionId) throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.MOVE_SECTION);
				oos.writeObject(jobId);
				oos.writeObject(sectionId);
				oos.writeObject(newPredecessorSectionId == null ? ""
						: newPredecessorSectionId);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job is to be started.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void startJob(final Socket clientSocket,
				final String jobId) throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.START_JOB);
				oos.writeObject(jobId);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job is to be stopped.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void stopJob(final Socket clientSocket, final String jobId)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.STOP_JOB);
				oos.writeObject(jobId);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag identifiers to be queued.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param identifiers
		 *            the identifiers.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void queue(final Socket clientSocket, final String jobId,
				final Collection identifiers) throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.QUEUE);
				oos.writeObject(jobId);
				for (final Iterator i = identifiers.iterator(); i.hasNext();)
					oos.writeObject(i.next());
				oos.writeObject(MartRunnerProtocol.END_MESSAGE);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag identifiers to be unqueued.
		 * 
		 * @param clientSocket
		 *            the socket to the host.
		 * @param jobId
		 *            the job ID.
		 * @param identifiers
		 *            the identifiers.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void unqueue(final Socket clientSocket,
				final String jobId, final Collection identifiers)
				throws ProtocolException {
			try {
				final ObjectOutputStream oos = (ObjectOutputStream) clientSocket
						.getOutputStream();
				oos.writeObject(MartRunnerProtocol.UNQUEUE);
				oos.writeObject(jobId);
				for (final Iterator i = identifiers.iterator(); i.hasNext();)
					oos.writeObject(i.next());
				oos.writeObject(MartRunnerProtocol.END_MESSAGE);
			} catch (final Throwable e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}
	}
}
