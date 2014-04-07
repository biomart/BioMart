package org.biomart.runner.view.gui;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobStatus;

public class JobPlanTreeModel extends DefaultTreeModel implements
		TreeWillExpandListener {
	private static final long serialVersionUID = 1L;

	private static final TreeNode LOADING_TREE = new DefaultMutableTreeNode(
			Resources.get("loadingTree"));

	private static final TreeNode EMPTY_TREE = new DefaultMutableTreeNode(
			Resources.get("emptyTree"));

	private final JobPlanPanel planPanel;

	private  String host;

	private  String port;

	private final MartRunnerMonitorDialog parentDialog;

	public JobPlanTreeModel(final String host, final String port,
			final JobPlanPanel planPanel,
			final MartRunnerMonitorDialog parentDialog) {
		super(JobPlanTreeModel.EMPTY_TREE, true);
		this.planPanel = planPanel;
		this.host = host;
		this.port = port;
		this.parentDialog = parentDialog;
	}

	/**
	 * Change the job this tree shows.
	 * 
	 * @param jobPlan
	 *            the job plan.
	 * @throws ProtocolException
	 *             if it was unable to do it.
	 */
	public void setJobPlan(final JobPlan jobPlan) throws ProtocolException {
		if (jobPlan == null) {
			this.setRoot(JobPlanTreeModel.EMPTY_TREE);
			this.reload();
		} else {
			// Set loading message.
			this.setRoot(JobPlanTreeModel.LOADING_TREE);
			this.reload();
			// Get job details.
			final SectionNode rootNode = new SectionNode(null, jobPlan
					.getRoot(), this.parentDialog);
			rootNode.expanded(this.host, this.port, this.planPanel.jobId);
			this.setRoot(rootNode);
			this.reload();
			// Update GUI bits from the updated plan.
			this.planPanel.threadSpinnerModel.setValue(new Integer(jobPlan
					.getThreadCount()));
//				this.planPanel.threadSpinnerModel.setMaximum(new Integer(
//						jobPlan.getMaxThreadCount()));
			this.planPanel.threadSpinnerModel.setMaximum(new Integer(5));
			this.planPanel.jdbcUrl.setText(jobPlan.getJDBCURL());
			this.planPanel.jdbcUser.setText(jobPlan.getJDBCUsername());
			this.planPanel.startJob.setEnabled(!jobPlan.getRoot()
					.getStatus().equals(JobStatus.RUNNING));
			this.planPanel.stopJob.setEnabled(jobPlan.getRoot().getStatus()
					.equals(JobStatus.RUNNING));
			this.planPanel.skipDropTable.setSelected(jobPlan
					.isSkipDropTable());
		}
	}

	public void treeWillCollapse(final TreeExpansionEvent event)
			throws ExpandVetoException {
		// Remove children.
		final Object collapsedNode = event.getPath().getLastPathComponent();
		if (collapsedNode instanceof SectionNode)
			((SectionNode) collapsedNode).collapsed();
	}

	public void treeWillExpand(final TreeExpansionEvent event)
			throws ExpandVetoException {
		// Insert children.
		final Object expandedNode = event.getPath().getLastPathComponent();
		if (expandedNode instanceof SectionNode)
			((SectionNode) expandedNode).expanded(this.host, this.port,
					this.planPanel.jobId);
	}

	public void updateHostAndPort(String host, String port) {
		this.host = host;
		this.port = port;
	}
}
