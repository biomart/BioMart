package org.biomart.runner.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import org.biomart.common.resources.Log;
import org.biomart.runner.controller.JobHandler;
import org.biomart.runner.exceptions.JobException;


/**
 * Represents an individual action.
 */
public class JobPlanAction implements Serializable {
	private static final long serialVersionUID = 1L;

	private String action;

	private JobStatus status;

	private Date started;

	private Date ended;

	private String message;

	private final String parentIdentifier;

	private final String jobId;

	private static int NEXT_IDENTIFIER = 0;

	private final int sequence = JobPlanAction.NEXT_IDENTIFIER++;

	/**
	 * Create a new action.
	 * 
	 * @param jobId
	 *            the job.
	 * @param action
	 *            the action to create.
	 * @param parentIdentifier
	 *            the parent node ID.
	 */
	public JobPlanAction(final String jobId, final String action,
			final String parentIdentifier) {
		this.action = action;
		this.status = JobStatus.NOT_QUEUED;
		this.parentIdentifier = parentIdentifier;
		this.jobId = jobId;
	}

	/**
	 * Get the action.
	 * 
	 * @return the action.
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * Change the action.
	 * 
	 * @param action
	 *            the new action.
	 */
	public void setAction(final String action) {
		this.action = action;
	}

	/**
	 * @return the ended
	 */
	public Date getEnded() {
		return this.ended;
	}

	/**
	 * @param ended
	 *            the ended to set
	 * @param allActions
	 *            all actions in this section, in order to do sibling tests.
	 */
	public void setEnded(final Date ended, final Collection allActions) {
		this.ended = ended;
		try {
			JobHandler.getSection(this.jobId, this.parentIdentifier)
					.updateEnded(ended, allActions);
		} catch (final JobException e) {
			// Aaargh!
			Log.error(e);
		}
	}

	/**
	 * @return the messages
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(final String message) {
		this.message = message;
	}

	/**
	 * @return the started
	 */
	public Date getStarted() {
		return this.started;
	}

	/**
	 * @param started
	 *            the started to set
	 * @param allActions
	 *            all actions in this section, in order to do sibling tests.
	 */
	public void setStarted(final Date started, final Collection allActions) {
		this.started = started;
		try {
			JobHandler.getSection(this.jobId, this.parentIdentifier)
					.updateStarted(started, allActions);
		} catch (final JobException e) {
			// Aaargh!
			Log.error(e);
		}
	}

	/**
	 * @return the status
	 */
	public JobStatus getStatus() {
		return this.status;
	}

	/**
	 * @param status
	 *            the status to set
	 * @param allActions
	 *            all actions in this section, in order to do sibling tests.
	 */
	public void setStatus(final JobStatus status,
			final Collection allActions) {
		if (status.equals(this.status))
			return;
		this.status = status;
		try {
			JobHandler.getSection(this.jobId, this.parentIdentifier)
					.updateStatus(status, allActions);
		} catch (final JobException e) {
			// Aaargh!
			Log.error(e);
		}
	}

	/**
	 * Get the parent section ID.
	 * 
	 * @return the parent section ID.
	 */
	public String getParentIdentifier() {
		return this.parentIdentifier;
	}

	/**
	 * Return a unique identifier.
	 * 
	 * @return the identifier.
	 */
	public String getIdentifier() {
		return this.parentIdentifier + "#" + this.sequence;
	}

	public int hashCode() {
		return this.sequence;
	}

	public String toString() {
		return this.getAction();
	}

	public boolean equals(final Object other) {
		if (!(other instanceof JobPlanAction))
			return false;
		return this.sequence == ((JobPlanAction) other).sequence;
	}
}
