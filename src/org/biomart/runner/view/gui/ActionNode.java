package org.biomart.runner.view.gui;

import java.net.Socket;
import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.runner.controller.MartRunnerProtocol.Client;
import org.biomart.runner.model.JobPlanAction;
import org.biomart.runner.model.JobPlanSection;

public class ActionNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1L;

	private final JobPlanAction action;

	private final SectionNode parent;

	private final MartRunnerMonitorDialog parentDialog;

	public ActionNode(final SectionNode parent,
			final JobPlanAction action,
			final MartRunnerMonitorDialog parentDialog) {
		this.action = action;
		this.parent = parent;
		this.parentDialog = parentDialog;
	}

	public JobPlanAction getAction() {
		return this.action;
	}

	public Enumeration children() {
		return null;
	}

	public boolean getAllowsChildren() {
		return false;
	}

	public TreeNode getChildAt(final int childIndex) {
		return null;
	}

	public int getChildCount() {
		return 0;
	}

	public int getIndex(final TreeNode node) {
		return 0;
	}

	public TreeNode getParent() {
		return this.parent;
	}

	public boolean isLeaf() {
		return true;
	}

	public void setUserObject(final Object userObject) {
		// Set the actions.
		final String oldAction = this.action.getAction();
		this.action.setAction((String) userObject);
		final JobPlanSection section = this.parent.getSection();
		// Send the update to the server.
		try {
			final Socket clientSocket = Client.createClientSocket(
					this.parentDialog.host, this.parentDialog.port);
			Client.updateAction(clientSocket, section.getJobPlan()
					.getJobId(), section, this.action);
			clientSocket.close();
		} catch (final Throwable pe) {
			this.action.setAction(oldAction);
			StackTrace.showStackTrace(pe);
		}
	}

	public String toString() {
		return this.action.toString();
	}
}
