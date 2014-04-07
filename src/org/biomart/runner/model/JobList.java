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

package org.biomart.runner.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biomart.common.exceptions.FunctionalException;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Handles list of jobs currently known.
 * 
 */
public class JobList implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<String,JobPlan> jobList = Collections
			.synchronizedMap(new LinkedHashMap<String,JobPlan>());

	/**
	 * Add a job.
	 * 
	 * @param job
	 *            the job to add.
	 */
	public void addJob(final JobPlan job) {
		this.jobList.put(job.getJobId(), job);
	}

	/**
	 * Remove a job.
	 * 
	 * @param jobId
	 *            the job ID to remove.
	 */
	public void removeJob(final String jobId) {
		this.jobList.remove(jobId);
	}

	/**
	 * Get the Job plan for the given id.
	 * 
	 * @param jobId
	 *            the job ID.
	 * @return the job plan.
	 */
	public JobPlan getJobPlan(final String jobId) {
		if (!this.jobList.containsKey(jobId))
			this.addJob(new JobPlan(jobId));
		return (JobPlan) this.jobList.get(jobId);
	}

	/**
	 * Retrieve all jobs we currently know about.
	 * 
	 * @return the set of all jobs.
	 */
	public Collection<JobPlan> getAllJobs() {
		return this.jobList.values();
	}

	public Element generateXML() {
		Element jobListElement = new Element("joblist");
		for(JobPlan jobplan : this.getAllJobs()) {
			Element planElement = jobplan.generateXML();
			jobListElement.addContent(planElement);
		}
		return jobListElement;
	}
	
	public Document generateXmlDocument() throws FunctionalException {
		return new Document(this.generateXML());
	}

}
