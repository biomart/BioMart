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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.SendMail;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobPlanAction;
import org.biomart.runner.model.JobStatus;
import org.biomart.runner.model.JobPlanSection;

/**
 * Takes a job and runs it and manages the associated threads.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.26 $, $Date: 2008/03/03 15:09:29 $, modified by
 *          $Author: rh4 $
 * @since 0.6
 */
public class JobThreadManager extends Thread {

	private static final String SYNC_KEY = "__SYNC__KEY__";

	private final String jobId;

	private final JobThreadManagerListener listener;

	private final List<JobThread> jobThreadPool = Collections
			.synchronizedList(new ArrayList<JobThread>());

	private boolean jobStopped = false;

	/**
	 * Create a new manager for the given job ID.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param listener
	 *            a callback listener.
	 */
	public JobThreadManager(final String jobId,
			final JobThreadManagerListener listener) {
		super();
		this.jobId = jobId;
		this.listener = listener;
	}

	/**
	 * Starts us.
	 */
	public void startThreadManager() {
		// Un-stop if necessary.
		this.jobStopped = false;
		// Start.
		this.start();
	}

	/**
	 * Stops us.
	 */
	public void stopThreadManager() {
		// Stop all threads once they have finished their current action.
		this.jobStopped = true;
	}

	public void run() {
		// Get the summary and the plan.
		try {
			final JobPlan plan = JobHandler.getJobPlan(this.jobId);
			final String contactEmail = plan.getContactEmailAddress();
			plan.callbackStart();

			// Send emails.
			if (contactEmail != null && !"".equals(contactEmail.trim()))
				try {
					SendMail
							.sendSMTPMail(new String[] { contactEmail },
									Resources.get("jobStartingSubject", ""
											+ this.jobId), "");
				} catch (final MessagingException e) {
					// We don't really care.
					Log.error(e);
				}

			// Timer updates thread pool with correct number of threads,
			// if that number has changed. If reduced, it stops the ones
			// that are in excess. If increased, it starts new ones.
			// Run timer immediately to create initial population.
			final Timer timer = new Timer();
			final TimerTask task = new TimerTask() {
				public void run() {
					JobThreadManager.this.resizeJobThreadPool(plan,
							JobThreadManager.this.jobStopped ? 0 : plan
									.getThreadCount());
				}
			};
			timer.schedule(task, 0, 5 * 1000); // Updates every 5 seconds.

			// Monitor pool and sleep until it is empty.
			do
				try {
					Thread.sleep(5 * 1000); // Checks every 5 seconds.
				} catch (final InterruptedException e) {
					// Don't care.
				}
			while (!this.jobThreadPool.isEmpty());

			// Stop monitoring the pool.
			timer.cancel();
			plan.callbackEnd();

			// Send emails.
			if (contactEmail != null && !"".equals(contactEmail.trim())) {
				final String subject;
				if (plan.getRoot().getStatus().equals(JobStatus.COMPLETED))
					subject = Resources.get("jobEndedOKSubject", ""
							+ this.jobId);
				else
					subject = Resources.get("jobEndedNOKSubject", ""
							+ this.jobId);
				try {
					SendMail.sendSMTPMail(new String[] { contactEmail },
							subject, "");
				} catch (final MessagingException e) {
					// We don't really care.
					Log.error(e);
				}
			}
		} catch (final Throwable t) {
			// It hates us.
			Log.fatal(t);
		} finally {
			// Do a callback.
			this.listener.jobStopped(this.jobId);
		}
	}

	private synchronized void resizeJobThreadPool(final JobPlan plan,
			final int requiredSize) {
		int actualSize = this.jobThreadPool.size();
		if (requiredSize < actualSize)
			// Reduce pool by stopping oldest thread.
			while (actualSize-- > requiredSize)
				((JobThread) this.jobThreadPool.get(0)).cancel();
		else if (requiredSize > actualSize)
			// Increase pool.
			while (actualSize++ < requiredSize) {
				// Add thread to pool and start it running.
				final JobThread thread = new JobThread(this, plan);
				thread.start();
				this.jobThreadPool.add(thread);
			}
	}

	private static class JobThread extends Thread {

		private final JobThreadManager manager;

		private final JobPlan plan;

		private static int SEQUENCE_NUMBER = 0;

		private final int sequence = JobThread.SEQUENCE_NUMBER++;

		private Connection connection;

		private JobPlanSection currentSection = null;

		private Set<String> tableNames = new HashSet<String>();

		private boolean cancelled = false;

		private JobThread(final JobThreadManager manager, final JobPlan plan) {
			super();
			this.manager = manager;
			this.plan = plan;
		}

		private void cancel() {
			this.cancelled = true;
		}

		public void run() {
			try {
				Log.info("Thread " + this.sequence + " starting");
				// Build list of tables in target schema.
				final Connection conn = this.getConnection();
				// Load tables and views from database, then loop over them.
				final ResultSet dbTables = conn.getMetaData().getTables(
						conn.getCatalog(), this.plan.getTargetSchema(), "%",
						new String[] { "TABLE", "VIEW", "ALIAS", "SYNONYM" });
				while (dbTables.next())
					this.tableNames.add(dbTables.getString("TABLE_NAME"));
				dbTables.close();
				// Each thread grabs sections from the queue until none are
				// left.
				while (this.continueRunning()
						&& (this.currentSection = this.getNextSection()) != null) {
					// Process section.
					final Map<String,JobPlanAction> actions = JobHandler.getActions(this.plan
							.getJobId(), this.currentSection.getIdentifier());
					for (final Iterator<JobPlanAction> i = actions.values().iterator(); i
							.hasNext()
							&& this.continueRunning();) {
						final JobPlanAction action = (JobPlanAction) i.next();
						// Only process queued/stopped actions.
						if (!(action.getStatus().equals(JobStatus.QUEUED) || action
								.getStatus().equals(JobStatus.STOPPED)))
							continue;
						// Process the action.
						else if (!this.processAction(action))
							break;
					}
					this.currentSection = null;
				}
			} catch (final Throwable t) {
				// Break out early and complain.
				Log.error(t);
			} finally {
				Log.info("Thread " + this.sequence + " ending");
				this.closeConnection();
				this.manager.jobThreadPool.remove(this);
			}
		}

		private String getCurrentSectionIdentifier() {
			return this.currentSection == null ? null : this.currentSection
					.getIdentifier();
		}

		private boolean continueRunning() {
			return !this.manager.jobStopped && !this.cancelled;
		}

		public boolean equals(final Object o) {
			if (!(o instanceof JobThread))
				return false;
			else
				return this.sequence == ((JobThread) o).sequence;
		}

		private boolean processAction(final JobPlanAction action) {
			boolean actionFailed = false;
			try {
				// Update action status to running.
				JobHandler.setStatus(this.plan.getJobId(), action
						.getIdentifier(), JobStatus.RUNNING, null);
				// Execute action.
				String failureMessage = null;
				try {
					final Connection conn = this.getConnection();
					final String sql = action.toString();
					// If action is create table (), check in stored
					// list to see if it needs dropping first.
					String dropTableSchema = null;
					String dropTableName = null;
					if (sql.startsWith("create table")) {
						dropTableName = sql.split(" ")[2];
						if (dropTableName.indexOf('.') >= 0) {
							final String[] parts = dropTableName.split("\\.");
							dropTableSchema = parts[0];
							dropTableName = parts[1];
						}
					} else if (sql.indexOf("rename") >= 0) {
						if (sql.startsWith("rename table")) {
							// MySQL table rename.
							dropTableName = sql.split(" ")[4];
							if (dropTableName.indexOf('.') >= 0) {
								final String[] parts = dropTableName
										.split("\\.");
								dropTableSchema = parts[0];
								dropTableName = parts[1];
							}
						} else if (sql.startsWith("alter table")
								&& sql.indexOf("rename to") > 0) {
							// Oracle+Postgres table rename.
							dropTableName = sql.split(" ")[5];
							if (dropTableName.indexOf('.') >= 0) {
								final String[] parts = dropTableName
										.split("\\.");
								dropTableSchema = parts[0];
								dropTableName = parts[1];
							}
						}
					}
					if (dropTableName != null
							&& this.tableNames.contains(dropTableName)) {
						final Statement stmt = conn.createStatement();
						final StringBuffer dropSql = new StringBuffer();
						dropSql.append("drop table ");
						if (dropTableSchema != null) {
							dropSql.append(dropTableSchema);
							dropSql.append('.');
						}
						dropSql.append(dropTableName);
						Log.debug("About to execute: "+dropSql);
						stmt.execute(dropSql.toString());
						Log.debug("Completed: "+dropSql);
						try {
							final SQLWarning warning = conn.getWarnings();
							if (warning != null)
								throw warning;
						} finally {
							stmt.close();
						}
					}
					// If action is drop table (), check to see if
					// we should skip over it instead.
					if (!(this.plan.isSkipDropTable() && sql
							.startsWith("drop table"))) {
						final Statement stmt = conn.createStatement();
						Log.debug("About to execute: "+sql);
						if (stmt.execute(sql)) {
							ResultSet rs = null;
							try {
								rs = stmt.getResultSet();
								this.plan.callbackResults(action, rs);
								final SQLWarning warning = conn.getWarnings();
								if (warning != null)
									throw warning;
							} finally {
								try {
									if (rs != null)
										rs.close();
								} finally {
									stmt.close();
								}
							}
						}
						Log.debug("Completed: "+sql);
					}
				} catch (final Throwable t) {
					final StringWriter messageWriter = new StringWriter();
					final PrintWriter pw = new PrintWriter(messageWriter);
					t.printStackTrace(pw);
					pw.flush();
					failureMessage = messageWriter.getBuffer().toString();
				}
				// Update status to failed or completed, and store
				// exception messages if failed.
				if (failureMessage != null) {
					JobHandler.setStatus(this.plan.getJobId(), action
							.getIdentifier(), JobStatus.FAILED, failureMessage);
					actionFailed = true;
				} else
					JobHandler.setStatus(this.plan.getJobId(), action
							.getIdentifier(), JobStatus.COMPLETED, null);
			} catch (final JobException e) {
				// We don't really care but print it just in case.
				Log.warn(e);
			}
			return !actionFailed;
		}

		private Connection getConnection() throws Exception {
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
			if (this.connection == null)
				this.connection = this.plan.getConnection();
			return this.connection;
		}

		private void closeConnection() {
			if (this.connection != null)
				try {
					Log.debug("Closing JDBC connection");
					this.connection.close();
				} catch (final SQLException e) {
					// We really don't care.
				}
		}

		private synchronized JobPlanSection getNextSection() {
			synchronized (JobThreadManager.SYNC_KEY) {
				final List sections = new ArrayList();
				sections.add(this.plan.getRoot());
				for (int i = 0; i < sections.size(); i++) {
					final JobPlanSection section = (JobPlanSection) sections
							.get(i);
					// Check actions. If none failed and none running,
					// and at least one queued or stopped, then select it.
					boolean hasUsableActions = false;
					boolean hasUnusableSiblings = false;
					// Only do check if has actions at all and is not
					// running or failed.
					if (section.getActionCount() > 0
							&& (section.getStatus().equals(JobStatus.STOPPED) || section
									.getStatus().equals(JobStatus.QUEUED))) {
						hasUsableActions = true;
						// Check that no sibling sections have actions that are
						// running.
						final JobPlanSection parent = section.getParent();
						final List siblings = new ArrayList();
						if (parent != null)
							if (parent.getStatus().equals(JobStatus.RUNNING))
								hasUnusableSiblings = true;
							else
								siblings.addAll(parent.getSubSections());
						// If any sibling claimed by another section, then this
						// section is not usable.
						for (final Iterator k = siblings.iterator(); !hasUnusableSiblings
								&& k.hasNext();) {
							final JobPlanSection sibling = (JobPlanSection) k
									.next();
							if (sibling.getStatus().equals(JobStatus.RUNNING))
								hasUnusableSiblings = true;
							else
								for (final Iterator<JobThread> j = this.manager.jobThreadPool
										.iterator(); !hasUnusableSiblings
										&& j.hasNext();) {
									final String threadId = j.next()
											.getCurrentSectionIdentifier();
									hasUnusableSiblings = threadId != null
											&& threadId.equals(sibling
													.getIdentifier());
								}
						}
					}
					// If all three checks satisfied, we can use this section.
					if (hasUsableActions && !hasUnusableSiblings)
						return section;
					// Otherwise, add subsections to list and keep looking.
					else
						sections.addAll(section.getSubSections());
				}
				// Return null if there are no more sections to process.
				return null;
			}
		}
	}

	/**
	 * A set of callback methods that the manager thread uses to notify
	 * interested parties of interesting things.
	 */
	public interface JobThreadManagerListener {
		/**
		 * This method is called when all threads have finished.
		 * 
		 * @param jobId
		 *            the jobId that has stopped.
		 */
		public void jobStopped(final String jobId);
	}
}
