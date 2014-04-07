package org.biomart.runner.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.ListBackedMap;
import org.jdom.Element;

/**
 * Describes a section of a job, ie. a group of associated actions.
 */
public class JobPlanSection implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String label;

	private final ListBackedMap<String,JobPlanSection> subSections = new ListBackedMap<String,JobPlanSection>();

	private int actionCount = 0;

	private final JobPlanSection parent;

	private final JobPlan plan;

	private JobStatus status;

	private Date started;

	private Date ended;
	
	private Map<String,JobPlanAction> actionMap;

	private static int NEXT_IDENTIFIER = 0;

	private final int sequence = JobPlanSection.NEXT_IDENTIFIER++;

	/**
	 * Define a new section with the given label.
	 * 
	 * @param label
	 *            the label.
	 * @param parent
	 *            the parent node.
	 * @param plan
	 *            the plan this section is part of.
	 */
	public JobPlanSection(final String label, final JobPlan plan,
			final JobPlanSection parent) {
		this.label = label;
		this.parent = parent;
		this.plan = plan;
		this.status = JobStatus.INCOMPLETE;
		this.actionMap = new LinkedHashMap<String,JobPlanAction>();
		plan.getSectionIds().put(this.getIdentifier(), this);
	}

	/**
	 * Obtain the job plan.
	 * 
	 * @return the job plan.
	 */
	public JobPlan getJobPlan() {
		return this.plan;
	}

	/**
	 * Obtain the parent node.
	 * 
	 * @return the parent node.
	 */
	public JobPlanSection getParent() {
		return this.parent;
	}

	/**
	 * Get a subsection. Creates it if it does not exist.
	 * 
	 * @param label
	 *            the label of the subsection.
	 * @return the subsection.
	 */
	public JobPlanSection getSubSection(final String label) {
		if (!this.subSections.containsKey(label))
			this.subSections.put(label, new JobPlanSection(label,
					this.plan, this));
		return (JobPlanSection) this.subSections.get(label);
	}

	/**
	 * Get all subsections as {@link JobPlanSection} objects.
	 * 
	 * @return all subsections.
	 */
	public Collection<JobPlanSection> getSubSections() {
		return this.subSections.values();
	}

	/**
	 * Move the section to just after the specified section, or if
	 * <tt>null</tt>, to the top of its sibling list.
	 * 
	 * @param section
	 *            the section (must be a child of this section).
	 * @param newPredecessorSection
	 *            the new predecessor section (must either be <tt>null</tt>
	 *            or a child of this section).
	 */
	public void moveSubSection(final JobPlanSection section,
			final JobPlanSection newPredecessorSection) {
		if (newPredecessorSection == null)
			// Insert at top.
			this.subSections.put(null, section.label, section);
		else
			// Insert before given label.
			this.subSections.put(newPredecessorSection.label,
					section.label, section);
	}

	/**
	 * Sets the action count.
	 * 
	 * @param actionCount
	 *            the action count to add.
	 */
	public void setActionCount(final int actionCount) {
		this.actionCount = actionCount;
	}

	/**
	 * How many actions are in this section alone?
	 * 
	 * @return the count.
	 */
	public int getActionCount() {
		return this.actionCount;
	}

	/**
	 * How many actions in total are in this section and all subsections?
	 * 
	 * @return the count.
	 */
	public int getTotalActionCount() {
		int count = this.getActionCount();
		for (final Iterator<JobPlanSection> i = this.getSubSections().iterator(); i
				.hasNext();)
			count += ((JobPlanSection) i.next()).getTotalActionCount();
		return count;
	}

	/**
	 * @return the ended
	 */
	public Date getEnded() {
		return this.ended;
	}

	public void updateEnded(Date newEnded, final Collection<JobPlanAction> allActions) {
		// If our date is not null and new date is not null
		// and new date is before our date, do nothing.
		if (this.ended != null) {
			if (newEnded != null) {
				if (newEnded.before(this.ended))
					return;
			}
			// If our date is not null and new date is null,
			// take latest date from children.
			else {
				for (final Iterator<JobPlanSection> i = this.getSubSections().iterator(); i
						.hasNext();) {
					final Date childEnded = ((JobPlanSection) i.next())
							.getEnded();
					if (newEnded == null || childEnded != null
							&& newEnded.before(childEnded))
						newEnded = childEnded;
				}
				if (allActions != null)
					for (final Iterator<JobPlanAction> i = allActions.iterator(); i
							.hasNext();) {
						final Date childEnded = ((JobPlanAction) i.next())
								.getEnded();
						if (newEnded == null || childEnded != null
								&& newEnded.before(childEnded))
							newEnded = childEnded;
					}
			}
		}
		// Otherwise if new date is also null, do nothing.
		else if (newEnded == null)
			return;
		// Update date as it has changed.
		this.ended = newEnded;
		if (this.parent != null)
			this.parent.updateEnded(newEnded, null);
	}

	/**
	 * @return the started
	 */
	public Date getStarted() {
		return this.started;
	}

	void updateStarted(Date newStarted, final Collection<JobPlanAction> allActions) {
		// If our date is not null and new date is not null
		// and new date is after our date, do nothing.
		if (this.started != null) {
			if (newStarted != null) {
				if (newStarted.after(this.started))
					return;
			}
			// If our date is not null and new date is null,
			// take earliest date from children.
			else {
				for (final Iterator<JobPlanSection> i = this.getSubSections().iterator(); i
						.hasNext();) {
					final Date childStarted = ((JobPlanSection) i.next())
							.getStarted();
					if (newStarted == null || childStarted != null
							&& newStarted.after(childStarted))
						newStarted = childStarted;
				}
				if (allActions != null)
					for (final Iterator<JobPlanAction> i = allActions.iterator(); i
							.hasNext();) {
						final Date childStarted = ((JobPlanAction) i.next())
								.getStarted();
						if (newStarted == null || childStarted != null
								&& newStarted.after(childStarted))
							newStarted = childStarted;
					}
			}
		}
		// Otherwise if new date is also null, do nothing.
		else if (newStarted == null)
			return;
		// Update date as it has changed.
		this.started = newStarted;
		if (this.parent != null)
			this.parent.updateStarted(newStarted, null);
	}

	/**
	 * @return the status
	 */
	public JobStatus getStatus() {
		return this.status;
	}

	void updateStatus(JobStatus newStatus,
			final Collection<JobPlanAction> allActions) {
		// New one less important? Check all and take most important.
		if (!newStatus.isMoreImportantThan(this.status)) {
			for (final Iterator<JobPlanSection> i = this.getSubSections().iterator(); i
					.hasNext();) {
				final JobStatus childStatus = ((JobPlanSection) i.next())
						.getStatus();
				if (childStatus.isMoreImportantThan(newStatus))
					newStatus = childStatus;
			}
			if (allActions != null)
				for (final Iterator<JobPlanAction> i = allActions.iterator(); i.hasNext();) {
					final JobStatus childStatus = ((JobPlanAction) i.next())
							.getStatus();
					if (childStatus.isMoreImportantThan(newStatus))
						newStatus = childStatus;
				}
		}
		// Same status? Keep it.
		if (newStatus.equals(this.status))
			return;
		// Change it now.
		this.status = newStatus;
		if (this.parent != null)
			this.parent.updateStatus(newStatus, null);
	}

	/**
	 * Return a unique identifier.
	 * 
	 * @return the identifier.
	 */
	public String getIdentifier() {
		return "" + this.sequence;
	}

	public int hashCode() {
		return this.sequence;
	}

	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append(this.label);
		if (this.getStatus().equals(JobStatus.INCOMPLETE)) {
			buf.append(" [");
			buf.append(Resources.get("jobStatusIncomplete"));
			buf.append("]");
		}
		buf.append(" (");
		buf.append(this.getTotalActionCount());
		buf.append(")");
		return buf.toString();
	}

	public boolean equals(final Object other) {
		if (!(other instanceof JobPlanSection))
			return false;
		return this.sequence == ((JobPlanSection) other).sequence;
	}

	public Element generateXML() {
		Element element = new Element("jobplansection");
		element.setAttribute("id", this.getIdentifier());
		element.setAttribute("label",this.label);
		for(JobPlanSection subSection: this.getSubSections()) {
			Element sube = subSection.generateXML();
			element.addContent(sube);
		}
		for(Map.Entry<String, JobPlanAction> entry: actionMap.entrySet()) {
			Element actionE = new Element("jobaction");
			actionE.setAttribute("id", entry.getKey());
			actionE.setAttribute("action", entry.getValue().getAction());
			element.addContent(actionE);
		}
		return element;
	}
	
	public void addActions(Map<String,JobPlanAction> actionMap) {
		this.actionMap.putAll(actionMap);
	}
}
