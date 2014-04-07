package org.biomart.runner.view.gui;

import java.net.Socket;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.runner.controller.MartRunnerProtocol.Client;
import org.biomart.runner.model.JobPlanAction;
import org.biomart.runner.model.JobPlanSection;

public class SectionNode implements TreeNode {

	private final JobPlanSection section;

	private final SectionNode parent;

	private final MartRunnerMonitorDialog parentDialog;

	// Must use Vector to be able to provide enumeration.
	private final Vector<TreeNode> children = new Vector<TreeNode>();

	public SectionNode(final SectionNode parent,
			final JobPlanSection section,
			final MartRunnerMonitorDialog parentDialog) {
		this.section = section;
		this.parent = parent;
		this.parentDialog = parentDialog;
	}

	public JobPlanSection getSection() {
		return this.section;
	}

	public void collapsed() {
		// Forget children.
		this.children.clear();
	}

	public void expanded(final String host, final String port,
			final String jobId) {
		// Create children.
		// Actions first.
		try {
			final Socket clientSocket = Client.createClientSocket(host,
					port);
			final Collection<JobPlanAction> actions = Client.getActions(clientSocket,
					jobId, this.section);
			for (final Iterator<JobPlanAction> i = actions.iterator(); i.hasNext();)
				this.children.add(new ActionNode(this, (JobPlanAction) i
						.next(), this.parentDialog));
			clientSocket.close();
		} catch (final Throwable e) {
			// Log it.
			Log.error(e);
			// Add dummy actions instead.
			for (int i = 0; i < this.section.getActionCount(); i++)
				this.children.add(new DefaultMutableTreeNode(Resources
						.get("emptyTree")));
		}
		// Then subsections.
		for (final Iterator i = this.section.getSubSections().iterator(); i
				.hasNext();)
			this.children.add(new SectionNode(this, (JobPlanSection) i
					.next(), this.parentDialog));
	}

	public Enumeration children() {
		return this.children.elements();
	}

	public boolean getAllowsChildren() {
		return this.getChildCount() > 0;
	}

	public TreeNode getChildAt(final int childIndex) {
		return (TreeNode) this.children.get(childIndex);
	}

	public int getChildCount() {
		return this.section.getActionCount()
				+ this.section.getSubSections().size();
	}

	public int getIndex(final TreeNode node) {
		return this.children.indexOf(node);
	}

	public TreeNode getParent() {
		return this.parent;
	}

	public boolean isLeaf() {
		return this.getChildCount() == 0;
	}

	public String toString() {
		return this.section.toString();
	}
}
