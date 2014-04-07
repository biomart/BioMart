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

package org.biomart.runner.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;
import javax.swing.JOptionPane;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.FileUtils;
import org.biomart.common.utils.SendMail;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.runner.controller.JobThreadManager.JobThreadManagerListener;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.model.JobList;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobPlanAction;
import org.biomart.runner.model.JobStatus;
import org.biomart.runner.model.JobPlanSection;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Tools for managing job statuses, and manipulating job objects.
 * 
 */
public class JobHandler {

	private static final Object planDirLock = "__PLANDIR__LOCK__";

	private static final int SAVE_INTERVAL = 5; // Seconds.

	private static long nextJob = System.currentTimeMillis();

	private static final File jobsDir = new File(
			Settings.getStorageDirectory(), "jobs");

	private static JobList jobList = null;

	private static final Map<String,JobThreadManager> jobManagers = Collections
			.synchronizedMap(new HashMap<String,JobThreadManager>());

	private static boolean jobListIsDirty = false;

	private static final Timer t = new Timer();

	static {
		if (!JobHandler.jobsDir.exists())
			JobHandler.jobsDir.mkdir();
		t.schedule(new TimerTask() {
			public void run() {
				if (JobHandler.jobListIsDirty)
					synchronized (JobHandler.planDirLock) {
						Log.debug("Saving list");
						// Save (overwrite) file with plan.
						FileOutputStream fos = null;
						try {
							final File jobListFile = JobHandler
									.getJobListFile();
							fos = new FileOutputStream(jobListFile);
							final ObjectOutputStream oos = new ObjectOutputStream(
									fos);
							oos.writeObject(JobHandler.jobList);
							oos.flush();
							fos.flush();
							//TODO change the format to xml
							final File jobListXMLFile = JobHandler.getJobListXMLFile();
					    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
					     	try {
					     		Document doc = JobHandler.jobList.generateXmlDocument();
					    		FileOutputStream fosxml = new FileOutputStream(jobListXMLFile);
					    		outputter.output(doc, fosxml);
					    		fos.close();
					    	}
					    	catch(Exception e) {
					    		e.printStackTrace();
					    	}   	

						} catch (final IOException e) {
							// What else to do with it?
							Log.error(e);
						} finally {
							if (fos != null)
								try {
									fos.close();
								} catch (final IOException e) {
									// What else to do with it?
									Log.error(e);
								}
						}
					}
				JobHandler.jobListIsDirty = false;
			}
		}, 0, JobHandler.SAVE_INTERVAL * 1000);
	}

	/**
	 * Request a new job ID. Don't define the job, just request an ID for one
	 * that could be defined in future.
	 * 
	 * @return a unique job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static String nextJobId() throws JobException {
		synchronized (JobHandler.planDirLock) {
			return "" + JobHandler.nextJob++;
		}
	}

	/**
	 * Locate any jobs that say they're running and change them to say they're
	 * STOPPED.
	 * 
	 * @return the number of jobs changed.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static int stopCrashedJobs() throws JobException {
		final Set<JobPlan> stoppedJobs = new HashSet<JobPlan>();
		// Update job summaries.
		final JobList jobList = JobHandler.getJobList();
		for (final Iterator<JobPlan> i = jobList.getAllJobs().iterator(); i.hasNext();) {
			final JobPlan plan = (JobPlan) i.next();
			final List<JobPlanSection> sections = new ArrayList<JobPlanSection>();
			sections.add(plan.getRoot());
			for (int j = 0; j < sections.size(); j++) {
				final JobPlanSection section = (JobPlanSection) sections.get(j);
				if (!section.getStatus().equals(JobStatus.RUNNING))
					continue;
				sections.addAll(section.getSubSections());
				for (final Iterator<JobPlanAction> l = JobHandler.getActions(plan.getJobId(),
						section.getIdentifier()).values().iterator(); l
						.hasNext();) {
					final JobPlanAction action = (JobPlanAction) l.next();
					if (action.getStatus().equals(JobStatus.RUNNING)) {
						JobHandler.setStatus(plan.getJobId(), action
								.getIdentifier(), JobStatus.STOPPED, null);
						stoppedJobs.add(plan);
					}
				}
			}
		}
		// Send an email when find stopped jobs.
		for (final Iterator<JobPlan> i = stoppedJobs.iterator(); i.hasNext();) {
			final JobPlan plan = (JobPlan) i.next();
			final String contactEmail = plan.getContactEmailAddress();
			if (contactEmail != null && !"".equals(contactEmail.trim()))
				try {
					SendMail.sendSMTPMail(new String[] { contactEmail },
							Resources.get("jobStoppedSubject", ""
									+ plan.getJobId()), "");
				} catch (final MessagingException e) {
					// We don't really care.
					Log.error(e);
				}
		}
		return stoppedJobs.size();
	}

	/**
	 * Flag that a job is about to start receiving commands.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param targetSchema
	 *            the schema we will be constructing.
	 * @param jdbcDriverClassName
	 *            the JDBC driver classname for the server the job will run
	 *            against.
	 * @param jdbcURL
	 *            the JDBC URL of the server the job will run against.
	 * @param jdbcUsername
	 *            the JDBC username for the server the job will run against.
	 * @param jdbcPassword
	 *            the JDBC password for the server the job will run against.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void beginJob(final String jobId, final String targetSchema,
			final String jdbcDriverClassName, final String jdbcURL,
			final String jdbcUsername, final String jdbcPassword)
			throws JobException {
		try {
			// Create a job list entry and a job plan.
			final JobPlan jobPlan = JobHandler.getJobList().getJobPlan(jobId);
			// Set the JDBC stuff.
			jobPlan.setTargetSchema(targetSchema);
			jobPlan.setJDBCDriverClassName(jdbcDriverClassName);
			jobPlan.setJDBCURL(jdbcURL);
			jobPlan.setJDBCUsername(jdbcUsername);
			jobPlan.setJDBCPassword(jdbcPassword);
			// Save stuff.
			JobHandler.saveJobList();
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job is about to end receiving commands.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void endJob(final String jobId) throws JobException {
		// We don't really care.
	}

	/**
	 * Change the status of a job plan action object.
	 * 
	 * @param jobId
	 *            the job.
	 * @param identifiers
	 *            the identifiers.
	 * @param status
	 *            the new status.
	 * @param message
	 *            the new message.
	 * @throws JobException
	 *             if it can't.
	 */
	public static void setStatus(final String jobId,
			final Collection<String> identifiers, final JobStatus status,
			final String message) throws JobException {
		JobHandler.setStatus(jobId, identifiers, status, message, true);
	}

	/**
	 * Change the status of a job plan action object.
	 * 
	 * @param jobId
	 *            the job.
	 * @param identifier
	 *            the job action.
	 * @param status
	 *            the new status.
	 * @param message
	 *            any message to assign to the action. Use <tt>null</tt> for
	 *            no message.
	 * @throws JobException
	 *             if it can't.
	 */
	public static void setStatus(final String jobId, final String identifier,
			final JobStatus status, final String message) throws JobException {
		JobHandler.setStatus(jobId, Collections.singletonList(identifier),
				status, message);
	}

	private static void setStatus(final String jobId,
			final Collection<String> identifiers, final JobStatus status,
			final String message, final boolean saveList) throws JobException {
		Map<String,JobPlanAction> actions = null;
		String previousSectionId = null;
		boolean sectionHasUpdatedActions = false;
		for (final Iterator<String> i = identifiers.iterator(); i.hasNext();) {
			final String identifier = (String) i.next();
			String sectionId = null;
			String actionId = null;
			// Convert identifier into either a section
			// or an action.
			if (identifier.indexOf('#') < 0)
				// Convert to a section and process all actions.
				sectionId = identifier;
			else {
				// Locate section and process individual action.
				final String[] parts = identifier.split("#");
				sectionId = parts[0];
				actionId = parts[1];
			}
			if (!sectionId.equals(previousSectionId)) {
				if (previousSectionId != null && sectionHasUpdatedActions)
					JobHandler.setActions(jobId, previousSectionId, actions,
							false);
				sectionHasUpdatedActions = false;
				actions = JobHandler.getActions(jobId, sectionId);
			}
			previousSectionId = sectionId;
			if (actionId != null) {
				sectionHasUpdatedActions = true;
				final JobPlanAction action = (JobPlanAction) actions
						.get(identifier);
				// Set the status.
				action.setStatus(status, actions.values());
				action.setMessage(message);
				// Update timings.
				if (status.equals(JobStatus.RUNNING)) {
					action.setStarted(new Date(), actions.values());
					action.setEnded(null, actions.values());
				} else if (status.equals(JobStatus.FAILED)
						|| status.equals(JobStatus.COMPLETED))
					action.setEnded(new Date(), actions.values());
				else {
					action.setStarted(null, actions.values());
					action.setEnded(null, actions.values());
				}
			} else {
				// Find all subsections and recurse on them.
				final Collection<String> newIdentifiers = new ArrayList<String>();
				final JobPlanSection section = JobHandler.getJobPlan(jobId)
						.getJobPlanSection(sectionId);
				if (section.getActionCount() > 0)
					// Recurse on direct actions.
					newIdentifiers.addAll(actions.keySet());
				// Recurse on subsections too.
				for (final Iterator<JobPlanSection> j = section.getSubSections().iterator(); j
						.hasNext();)
					newIdentifiers.add(((JobPlanSection) j.next())
							.getIdentifier());
				// Do the recursive call.
				JobHandler.setStatus(jobId, newIdentifiers, status, message,
						false);
			}
		}
		if (previousSectionId != null && sectionHasUpdatedActions)
			JobHandler.setActions(jobId, previousSectionId, actions, false);
		// Now save the list if required.
		if (saveList)
			try {
				JobHandler.saveJobList();
			} catch (final IOException e) {
				throw new JobException(e);
			}
	}

	/**
	 * Get the numbered section from the job def.
	 * 
	 * @param jobId
	 *            the job.
	 * @param sectionId
	 *            the section number.
	 * @return the section.
	 * @throws JobException
	 */
	public static JobPlanSection getSection(final String jobId,
			final String sectionId) throws JobException {
		return JobHandler.getJobPlan(jobId).getJobPlanSection(sectionId);
	}

	/**
	 * Do the funky empty table thang.
	 * 
	 * @param jobId
	 *            the job.
	 * @throws JobException
	 */
	public static void makeEmptyTableJob(final String jobId)
			throws JobException {
		try {
			JobHandler.getJobPlan(jobId).makeEmptyTableJob();
		} catch (final SQLException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Queue some set of sections+actions.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param identifiers
	 *            the selected node identifiers.
	 * @throws JobException
	 *             if it cannot do it.
	 */
	public static void queue(final String jobId, final Collection<String> identifiers)
			throws JobException {
		JobHandler.setStatus(jobId, identifiers, JobStatus.QUEUED, null);
	}

	/**
	 * Unqueue some set of sections+actions.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param identifiers
	 *            the selected node identifiers.
	 * @throws JobException
	 *             if it cannot do it.
	 */
	public static void unqueue(final String jobId, final Collection<String> identifiers)
			throws JobException {
		JobHandler.setStatus(jobId, identifiers, JobStatus.NOT_QUEUED, null);
	}

	/**
	 * Update an action.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param sectionId
	 *            the section ID.
	 * @param actionId
	 *            the action ID.
	 * @param action
	 *            the new action.
	 * @throws JobException
	 *             if it cannot do it.
	 */
	public static void updateAction(final String jobId, final String sectionId,
			final String actionId, final String action) throws JobException {
		final Map<String,JobPlanAction> actions = JobHandler.getActions(jobId, sectionId);
		final JobPlanAction jpAction = (JobPlanAction) actions.get(actionId);
		jpAction.setAction(action);
		jpAction.setStatus(JobStatus.NOT_QUEUED, actions.values());
		JobHandler.setActions(jobId, sectionId, actions, true);
	}

	/**
	 * Flag that an action is to be set to the job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param sectionPath
	 *            the section this applies to.
	 * @param actions
	 *            the actions to add.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void setActions(final String jobId,
			final String[] sectionPath, final Collection<String> actions)
			throws JobException {
		final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
		// Add the action to the job.
		jobPlan.setActionCount(sectionPath, actions.size());
		// Get the section ID.
		JobPlanSection section = jobPlan.getRoot();
		for (int i = 0; i < sectionPath.length; i++)
			section = section.getSubSection(sectionPath[i]);
		// Convert each action into an action object and create
		// a map.
		final Map<String,JobPlanAction> actionMap = new LinkedHashMap<String,JobPlanAction>();
		for (final Iterator<String> i = actions.iterator(); i.hasNext();) {
			final JobPlanAction action = new JobPlanAction(jobId, (String) i
					.next(), section.getIdentifier());
			actionMap.put(action.getIdentifier(), action);
		}
		section.addActions(actionMap);
		// Do the work.
		JobHandler.setActions(jobId, section.getIdentifier(), actionMap, false);
		// Update the status to QUEUED (for external requests only).
		JobHandler.setStatus(jobId, actionMap.keySet(), JobStatus.QUEUED, null);
	}

	private static void setActions(final String jobId, final String sectionId,
			final Map<String,JobPlanAction> actionMap, final boolean saveList) throws JobException {
		Log.debug("Saving actions for job " + jobId + " section " + sectionId);
		FileOutputStream fos = null;
		try {
			// Write the actions to a file.
			fos = new FileOutputStream(JobHandler.getActionsFile(jobId,
					sectionId));
			final ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(actionMap);
			oos.flush();
			fos.flush();
			//TODO save to xml
			
			FileWriter fw = new FileWriter(JobHandler.getActionsXMLFile(jobId,sectionId));
			for(Map.Entry<String, JobPlanAction> entry: actionMap.entrySet()) {
				fw.write(entry.getKey()+"\n");
				fw.write(entry.getValue().getAction()+"\n");
			}
			fw.close();
			// Save the list.
			if (saveList)
				JobHandler.saveJobList();
		} catch (final IOException e) {
			throw new JobException(e);
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (final IOException ie) {
					// Ignore.
				}
		}
	}

	/**
	 * Get the actions for the specified job section. Keys are identifiers,
	 * values are actions.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param sectionId
	 *            the section ID.
	 * @return the actions.
	 * @throws JobException
	 *             if they could not be read.
	 */

	public static Map<String,JobPlanAction> getActions(final String jobId, final String sectionId)
			throws JobException {
		Log.debug("Loading actions for job " + jobId + " section " + sectionId);
		// Load actions from file.
		synchronized (JobHandler.planDirLock) {
			// Load existing job plan.
			FileInputStream fis = null;
			Map<String,JobPlanAction> actions = null;
			try {
				final File actionsFile = JobHandler.getActionsFile(jobId,
						sectionId);
				// Doesn't exist? Return a default empty list.
				if (!actionsFile.exists())
					actions = Collections.EMPTY_MAP;
				else {
					fis = new FileInputStream(actionsFile);
					final ObjectInputStream ois = new ObjectInputStream(fis);
					actions = (Map<String,JobPlanAction>) ois.readObject();
				}
			} catch (final Throwable t) {
				// This is horrible. Make up a default one.
				Log.error(t);
				actions = Collections.EMPTY_MAP;
				throw new JobException(t);
			} finally {
				if (fis != null)
					try {
						fis.close();
					} catch (final IOException ie) {
						// We don't care.
					}
			}
			return actions;
		}
	}

	private static File getActionsFile(final String jobId,
			final String sectionId) throws IOException {
		synchronized (JobHandler.planDirLock) {
			final String firstDir = sectionId.length() <= 3 ? sectionId
					: sectionId.substring(0, 3);
			final String secondDir = sectionId.length() <= 6 ? sectionId
					: sectionId.substring(0, 6);
			final File sectionDir = new File(new File(new File(
					JobHandler.jobsDir, jobId), firstDir), secondDir);
			if (!sectionDir.exists())
				sectionDir.mkdirs();
			return new File(sectionDir, sectionId);
		}
	}
	
	private static File getActionsXMLFile(final String jobId,
			final String sectionId) throws IOException {
		synchronized (JobHandler.planDirLock) {
			final String firstDir = sectionId.length() <= 3 ? sectionId
					: sectionId.substring(0, 3);
			String xmlFirstDir = firstDir;
			final String secondDir = sectionId.length() <= 6 ? sectionId
					: sectionId.substring(0, 6);
			String xmlSecondDir = secondDir;
			final File sectionDir = new File(new File(new File(
					JobHandler.jobsDir, jobId), xmlFirstDir), xmlSecondDir);
			if (!sectionDir.exists())
				sectionDir.mkdirs();
			return new File(sectionDir, sectionId+".xml");
		}
	}

	/**
	 * Flag that an action is to be set to the job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param sectionId
	 *            the section this applies to.
	 * @param newPredecessorSectionId
	 *            the section to go after, or <tt>null</tt> if to be moved to
	 *            the top of current siblings.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void moveSection(final String jobId, final String sectionId,
			final String newPredecessorSectionId) throws JobException {
		// Get the sections.
		JobPlanSection section = JobHandler.getSection(jobId, sectionId);
		JobPlanSection newPredecessorSection = newPredecessorSectionId != null ? JobHandler
				.getSection(jobId, newPredecessorSectionId)
				: null;
		// Swap sections.
		section.getParent().moveSubSection(section, newPredecessorSection);
		// Save modified structure.
		try {
			JobHandler.saveJobList();
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Gets the plan for a job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @return the plan.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static JobPlan getJobPlan(final String jobId) throws JobException {
		return JobHandler.getJobList().getJobPlan(jobId);
	}

	/**
	 * Obtain a list of the jobs that MartRunner is currently managing.
	 * 
	 * @return a list of jobs.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static JobList getJobList() throws JobException {
		try {
			return JobHandler.loadJobList();
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Makes MartRunner forget about a job.
	 * 
	 * @param jobId
	 *            the job to forget.
	 * @throws JobException
	 *             if it couldn't lose its memory.
	 */
	public static void removeJob(final String jobId) throws JobException {
		try {
			// Stop job first if currently running.
			JobHandler.stopJob(jobId);
			// Remove the job list entry.
			final JobList jobList = JobHandler.getJobList();
			jobList.removeJob(jobId);
			JobHandler.saveJobList();
			// Recursively delete the job directory.
			FileUtils.delete(new File(JobHandler.jobsDir, jobId));
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job skip drop status has changed.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param skipDropTable
	 *            the new value - <tt>true</tt> to turn it on.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void setSkipDropTable(final String jobId,
			final boolean skipDropTable) throws JobException {
		try {
			// Create a job list entry.
			final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
			// Set the stuff.
			jobPlan.setSkipDropTable(skipDropTable);
			JobHandler.saveJobList();
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job email address has changed.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param email
	 *            the new email address to use as a contact address.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void setEmailAddress(final String jobId, final String email)
			throws JobException {
		try {
			// Create a job list entry.
			final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
			// Set the email stuff.
			final String trimmedEmail = email.trim().length() == 0 ? null
					: email.trim();
			if (jobPlan.getContactEmailAddress() == null
					&& trimmedEmail != null
					|| jobPlan.getContactEmailAddress() != null
					&& !jobPlan.getContactEmailAddress().equals(trimmedEmail)) {
				jobPlan.setContactEmailAddress(trimmedEmail);
				JobHandler.saveJobList();
			}
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Flag that a job thread count has changed.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @param threadCount
	 *            the new thread count to use.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void setThreadCount(final String jobId, final int threadCount)
			throws JobException {
		try {
			// Create a job list entry.
			final JobPlan jobPlan = JobHandler.getJobPlan(jobId);
			// Set the thread count.
			if (threadCount != jobPlan.getThreadCount()) {
				jobPlan.setThreadCount(threadCount);
				JobHandler.saveJobList();
			}
		} catch (final IOException e) {
			throw new JobException(e);
		}
	}

	/**
	 * Starts a job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void startJob(final String jobId) throws JobException {
		if (JobHandler.jobManagers.containsKey(jobId))
			return; // Ignore if already running.
		final JobThreadManager manager = new JobThreadManager(jobId,
				new JobThreadManagerListener() {
					public void jobStopped(final String jobId) {
						JobHandler.jobManagers.remove(jobId);
						Log.info("Thread manager stopped for " + jobId);
					}
				});
		JobHandler.jobManagers.put(jobId, manager);
		manager.startThreadManager();
		Log.info("Thread manager started for " + jobId);
	}

	/**
	 * Stops a job.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @throws JobException
	 *             if anything went wrong.
	 */
	public static void stopJob(final String jobId) throws JobException {
		if (!JobHandler.jobManagers.containsKey(jobId))
			return; // Ignore if already stopped.
		final JobThreadManager manager = (JobThreadManager) JobHandler.jobManagers
				.get(jobId);
		manager.stopThreadManager();
		// Don't remove it. The callback will do that.
		Log.info("Stopped thread manager " + jobId);
	}

	private static File getJobListFile() throws IOException {
		return new File(JobHandler.jobsDir, "list");
	}
	
	private static File getJobListXMLFile() throws IOException {
		return new File(JobHandler.jobsDir,"list.xml");
	}

	private static JobList loadJobList() throws IOException {
		synchronized (JobHandler.planDirLock) {
			if (JobHandler.jobList != null)
				return JobHandler.jobList;
			Log.debug("Loading list");
			final File jobListFile = JobHandler.getJobListFile();
			// Load existing job plan.
			FileInputStream fis = null;
			JobList jobList = null;
			// Doesn't exist? Return a default new list.
			if (!jobListFile.exists())
				jobList = new JobList();
			else
				try {
					fis = new FileInputStream(jobListFile);
					final ObjectInputStream ois = new ObjectInputStream(fis);
					jobList = (JobList) ois.readObject();
				} catch (final IOException e) {
					throw e;
				} catch (final Throwable t) {
					// This is horrible. Make up a default one.
					Log.error(t);
					jobList = new JobList();
				} finally {
					if (fis != null)
						fis.close();
				}
			JobHandler.jobList = jobList;
			return jobList;
		}
	}

	private static void saveJobList() throws IOException {
		synchronized (JobHandler.planDirLock) {
			JobHandler.jobListIsDirty = true;
		}
	}

	// Tools are static and cannot be instantiated.
	private JobHandler() {
	}
}
