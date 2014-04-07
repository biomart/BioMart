package org.biomart.runner.view.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.DraggableJTree;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.view.gui.dialogs.MartRunnerMonitorDialog;
import org.biomart.runner.controller.MartRunnerProtocol.Client;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobPlanAction;
import org.biomart.runner.model.JobStatus;
import org.biomart.runner.model.JobPlanSection;

// A panel for showing the job plans in.
public class JobPlanPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private  String host;

	private  String port;

	private JTree tree;

	private JobPlanTreeModel treeModel;

	public String jobId;

	private final JTextField jobIdField;

	private final JSpinner threadSpinner;

	public final SpinnerNumberModel threadSpinnerModel;

	public final JTextField jdbcUrl;

	public final JTextField jdbcUser;

	private final JFormattedTextField started;

	private final JFormattedTextField finished;

	private final JTextField elapsed;

	private final JTextField status;

	private final JTextArea messages;

	public final JButton startJob;

	public final JButton stopJob;

	public final JCheckBox skipDropTable;

	public void updateHostAndPort(String host, String port) {
		this.host = host;
		this.port = port;
		this.treeModel.updateHostAndPort(host, port);
	}
	/**
	 * Create a new job description panel. In the top half goes two panes -
	 * an email settings pane, and the job tree view. In the bottom half
	 * goes an explanation panel.
	 * 
	 * @param parentDialog
	 *            the dialog we are displaying in.
	 * @param host
	 *            the host to talk to MartRunner at.
	 * @param port
	 *            the port to talk to MartRunner at.
	 */
	public JobPlanPanel(final MartRunnerMonitorDialog parentDialog,
			final String host, final String port) {
		super(new BorderLayout(2, 2));
		this.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
		this.host = host;
		this.port = port;

		// Create constraints for labels that are not in the last row.
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Create a panel to hold the header details.
		final JPanel headerPanel = new JPanel(new GridBagLayout());

		// Create the user-interactive bits of the panel.
		this.threadSpinnerModel = new SpinnerNumberModel(1, 1, 1, 1);
		this.threadSpinner = new JSpinner(this.threadSpinnerModel);
		this.threadSpinner.setEnabled(false);
		// Spinner listener updates summary thread count instantly.
		this.threadSpinnerModel.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				if (JobPlanPanel.this.jobId != null)
					try {
						final Socket clientSocket = Client
								.createClientSocket(host, port);
						Client
								.setThreadCount(
										clientSocket,
										JobPlanPanel.this.jobId,
										((Integer) JobPlanPanel.this.threadSpinnerModel
												.getValue()).intValue());
						clientSocket.close();
					} catch (final Throwable pe) {
						StackTrace.showStackTrace(pe);
					}
			}
		});

		// Populate the header panel.
		JLabel label = new JLabel(Resources.get("jobIdLabel"));
		headerPanel.add(label, labelConstraints);
		JPanel field = new JPanel();
		this.jobIdField = new JTextField(12);
		this.jobIdField.setEnabled(false);
		field.add(this.jobIdField);
		field.add(new JLabel(Resources.get("threadCountLabel")));
		field.add(this.threadSpinner);
		headerPanel.add(field, fieldConstraints);

		label = new JLabel(Resources.get("jdbcURLLabel"));
		headerPanel.add(label, labelConstraints);
		field = new JPanel();
		this.jdbcUrl = new JTextField(30);
		this.jdbcUrl.setEnabled(false);
		field.add(this.jdbcUrl);
		field.add(new JLabel(Resources.get("usernameLabel")));
		this.jdbcUser = new JTextField(12);
		this.jdbcUser.setEnabled(false);
		field.add(this.jdbcUser);
		headerPanel.add(field, fieldConstraints);

		headerPanel.add(new JLabel(), labelLastRowConstraints);
		field = new JPanel();
		this.startJob = new JButton(Resources.get("startJobButton"));
		this.stopJob = new JButton(Resources.get("stopJobButton"));
		// Button listeners to start+stop jobs.
		this.startJob.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (JobPlanPanel.this.jobId != null)
					try {
						final Socket clientSocket = Client
								.createClientSocket(host, port);
						Client.startJob(clientSocket,
								JobPlanPanel.this.jobId);
						clientSocket.close();
						JobPlanPanel.this.startJob.setEnabled(false);
					} catch (final Throwable pe) {
						StackTrace.showStackTrace(pe);
					}
			}
		});
		this.stopJob.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (JobPlanPanel.this.jobId != null)
					try {
						final Socket clientSocket = Client
								.createClientSocket(host, port);
						Client.stopJob(clientSocket,
								JobPlanPanel.this.jobId);
						clientSocket.close();
						JobPlanPanel.this.stopJob.setEnabled(false);
					} catch (final Throwable pe) {
						StackTrace.showStackTrace(pe);
					}
			}
		});
		this.skipDropTable = new JCheckBox(Resources
				.get("skipDropTableLabel"));
		this.skipDropTable.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (JobPlanPanel.this.jobId != null)
					try {
						final Socket clientSocket = Client
								.createClientSocket(host, port);
						Client.setSkipDropTable(clientSocket,
								JobPlanPanel.this.jobId,
								JobPlanPanel.this.skipDropTable
										.isSelected());
						clientSocket.close();
					} catch (final Throwable pe) {
						StackTrace.showStackTrace(pe);
					}
			}
		});
		field.add(this.startJob);
		field.add(this.stopJob);
		field.add(this.skipDropTable);
		headerPanel.add(field, fieldLastRowConstraints);

		// Create a panel to hold the footer details.
		final JPanel footerPanel = new JPanel(new GridBagLayout());

		// Populate the footer panel.
		label = new JLabel(Resources.get("statusLabel"));
		footerPanel.add(label, labelConstraints);
		field = new JPanel();
		this.status = new JTextField(12);
		this.status.setEnabled(false);
		field.add(this.status);
		field.add(new JLabel(Resources.get("elapsedLabel")));
		this.elapsed = new JTextField(12);
		this.elapsed.setEnabled(false);
		field.add(this.elapsed);
		field.add(new JLabel(Resources.get("startedLabel")));
		this.started = new JFormattedTextField(new SimpleDateFormat());
		this.started.setColumns(12);
		this.started.setEnabled(false);
		field.add(this.started);
		field.add(new JLabel(Resources.get("finishedLabel")));
		this.finished = new JFormattedTextField(new SimpleDateFormat());
		this.finished.setColumns(12);
		this.finished.setEnabled(false);
		field.add(this.finished);
		footerPanel.add(field, fieldConstraints);

		label = new JLabel(Resources.get("messagesLabel"));
		footerPanel.add(label, labelConstraints);
		field = new JPanel();
		this.messages = new JTextArea(6, 60);
		this.messages.setEnabled(false);
		field.add(new JScrollPane(this.messages));
		footerPanel.add(field, fieldConstraints);

		// Create the tree and default model.
		// Create a JTree to hold job details.
		this.treeModel = new JobPlanTreeModel(this.host, this.port, this,
				parentDialog);
		this.tree = new DraggableJTree(this.treeModel) {
			private static final long serialVersionUID = 1L;

			public boolean isPathEditable(final TreePath path) {
				return path.getPathCount() > 0
						&& path.getLastPathComponent() instanceof ActionNode;
			}

			public boolean isValidDragPath(final TreePath path) {
				// Drag-and-drop of level-1 nodes (ie. first level
				// below root only)
				return path.getPathCount() == 2;
			}

			public boolean isValidDropPath(final TreePath path) {
				// Drag-and-drop of level-1 nodes (ie. first level
				// below root only) PLUS level-0 node (root).
				return path.getPathCount() <= 2;
			}

			public void dragCompleted(final int action,
					final TreePath from, final TreePath to) {
				// On successful drag-and-drop, call move-section
				// with the identifiers of the source and target sections.
				final JobPlanSection fromSection = ((SectionNode) from
						.getLastPathComponent()).getSection();
				final JobPlanSection toSection = ((SectionNode) to
						.getLastPathComponent()).getSection();
				// Confirm.
				new LongProcess() {
					public void run() throws Exception {
						// Queue the job.
						final Socket clientSocket = Client
								.createClientSocket(host, port);
						Client.moveSection(clientSocket,
								JobPlanPanel.this.jobId, fromSection
										.getIdentifier(),
								toSection == treeModel.getRoot() ? null
										: toSection.getIdentifier());
						clientSocket.close();
						// Update the list.
						parentDialog.refreshJobList.doClick();
					}
				}.start();

			}
		};
		this.tree.setOpaque(true);
		this.tree.setBackground(Color.WHITE);
		this.tree.setEditable(true);
		this.tree.setRootVisible(true); // Always show the root node.
		this.tree.setShowsRootHandles(true); // Allow root expansion.
		this.tree.setCellRenderer(new JobPlanTreeCellRenderer());

		// Add context menu to the job plan tree.
		this.tree.addMouseListener(new MouseListener() {
			public void mouseClicked(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mouseEntered(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mouseExited(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mousePressed(final MouseEvent e) {
				this.doMouse(e);
			}

			public void mouseReleased(final MouseEvent e) {
				this.doMouse(e);
			}

			private void doMouse(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					final TreePath treePath = JobPlanPanel.this.tree
							.getPathForLocation(e.getX(), e.getY());
					if (treePath != null) {
						// Work out what was clicked on or
						// multiply selected.
						final TreePath[] selectedPaths;
						if (JobPlanPanel.this.tree.getSelectionCount() == 0)
							selectedPaths = new TreePath[] { treePath };
						else
							selectedPaths = JobPlanPanel.this.tree
									.getSelectionPaths();

						// Show menu.
						final JPopupMenu contextMenu = this
								.getContextMenu(Arrays
										.asList(selectedPaths));
						if (contextMenu != null
								&& contextMenu.getComponentCount() > 0) {
							contextMenu.show(JobPlanPanel.this.tree, e
									.getX(), e.getY());
							e.consume();
						}
					}
				}
			}

			private JPopupMenu getContextMenu(final Collection selectedPaths) {
				// Convert paths to identifiers.
				final Set identifiers = new HashSet();
				final List selectedNodes = new ArrayList();
				for (final Iterator i = selectedPaths.iterator(); i
						.hasNext();)
					selectedNodes.add(((TreePath) i.next())
							.getLastPathComponent());
				for (int i = 0; i < selectedNodes.size(); i++) {
					final Object node = selectedNodes.get(i);
					if (node instanceof ActionNode)
						identifiers.add(((ActionNode) node).getAction()
								.getIdentifier());
					else if (node instanceof SectionNode)
						identifiers.add(((SectionNode) node).getSection()
								.getIdentifier());
				}

				// Did we produce anything?
				if (identifiers.size() < 1)
					return null;

				// Build menu.
				final JPopupMenu contextMenu = new JPopupMenu();

				// Queue row.
				final JMenuItem queue = new JMenuItem(Resources
						.get("queueSelectionTitle"));
				queue.setMnemonic(Resources.get("queueSelectionMnemonic")
						.charAt(0));
				queue.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						// Confirm.
						new LongProcess() {
							public void run() throws Exception {
								// Queue the job.
								final Socket clientSocket = Client
										.createClientSocket(host, port);
								Client.queue(clientSocket,
										JobPlanPanel.this.jobId,
										identifiers);
								clientSocket.close();
								// Update the list.
								parentDialog.refreshJobList.doClick();
							}
						}.start();
					}
				});
				contextMenu.add(queue);

				// Unqueue row.
				final JMenuItem unqueue = new JMenuItem(Resources
						.get("unqueueSelectionTitle"));
				unqueue.setMnemonic(Resources.get(
						"unqueueSelectionMnemonic").charAt(0));
				unqueue.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						// Confirm.
						new LongProcess() {
							public void run() throws Exception {
								// Unqueue the job.
								final Socket clientSocket = Client
										.createClientSocket(host, port);
								Client.unqueue(clientSocket,
										JobPlanPanel.this.jobId,
										identifiers);
								clientSocket.close();
								// Update the list.
								parentDialog.refreshJobList.doClick();
							}
						}.start();
					}
				});
				contextMenu.add(unqueue);

				return contextMenu;
			}
		});

		// Listener on tree to update footer panel fields.
		this.tree.addTreeSelectionListener(new TreeSelectionListener() {

			public void valueChanged(final TreeSelectionEvent e) {
				// Default values.
				Date started = null;
				Date ended = null;
				JobStatus status = JobStatus.UNKNOWN;
				String messages = null;
				long elapsed = 0;

				// Check a path was actually selected.
				final TreePath path = e.getPath();
				if (path != null) {
					final Object selectedNode = e.getPath()
							.getLastPathComponent();

					// Get info.
					if (selectedNode instanceof SectionNode) {
						final JobPlanSection section = ((SectionNode) selectedNode)
								.getSection();
						status = section.getStatus();
						started = section.getStarted();
						ended = section.getEnded();
						messages = null;
					} else if (selectedNode instanceof ActionNode) {
						final JobPlanAction action = ((ActionNode) selectedNode)
								.getAction();
						status = action.getStatus();
						started = action.getStarted();
						ended = action.getEnded();
						messages = action.getMessage();
					}

					// Elapsed time calculation.
					if (started != null)
						if (ended != null)
							elapsed = ended.getTime() - started.getTime();
						else
							elapsed = new Date().getTime()
									- started.getTime();
				}

				// Elapsed time to string.
				elapsed /= 1000; // Un-millify.
				final long seconds = elapsed % 60;
				elapsed /= 60;
				final long minutes = elapsed % 60;
				elapsed /= 60;
				final long hours = elapsed % 24;
				elapsed /= 24;
				final long days = elapsed;

				// Update dialog.
				try {
					if (started != null) {
						JobPlanPanel.this.started.setValue(started);
						JobPlanPanel.this.started.commitEdit();
					} else
						JobPlanPanel.this.started.setText(null);
					if (ended != null) {
						JobPlanPanel.this.finished.setValue(ended);
						JobPlanPanel.this.finished.commitEdit();
					} else
						JobPlanPanel.this.finished.setText(null);
				} catch (final ParseException pe) {
					// Don't be so silly.
					Log.error(pe);
				}
				JobPlanPanel.this.elapsed.setText(Resources.get(
						"timeElapsedPattern", new String[] { "" + days,
								"" + hours, "" + minutes, "" + seconds }));
				JobPlanPanel.this.status.setText(status.toString());
				JobPlanPanel.this.messages.setText(messages);

				// Redraw.
				JobPlanPanel.this.revalidate();
			}
		});

		// Install an ExpansionListener on the tree which causes the model
		// to add actions dynamically from server.
		this.tree.addTreeWillExpandListener(this.treeModel);

		// Update the layout.
		this.add(headerPanel, BorderLayout.PAGE_START);
		this.add(new JScrollPane(this.tree), BorderLayout.CENTER);
		this.add(footerPanel, BorderLayout.PAGE_END);

		// Set the default values.
		this.setNoJob();
	}

	private void setNoJob() {
		this.jobId = null;
		this.jobIdField.setText(Resources.get("noJobSelected"));
		this.threadSpinnerModel.setValue(new Integer(1));
		this.threadSpinner.setEnabled(false);
		this.jdbcUrl.setText(null);
		this.jdbcUser.setText(null);
		//this.contactEmail.setText(null);
		//this.contactEmail.setEnabled(false);
		//this.updateEmailButton.setEnabled(false);
		this.startJob.setEnabled(false);
		this.stopJob.setEnabled(false);
		this.skipDropTable.setSelected(false);
		this.skipDropTable.setEnabled(false);
		try {
			this.treeModel.setJobPlan(null);
		} catch (final ProtocolException e) {
			StackTrace.showStackTrace(e);
		}
	}

	public void setJobPlan(final JobPlan jobPlan) {
		if (jobPlan == null)
			this.setNoJob();
		else
			new LongProcess() {
				public void run() throws Exception {
					// Get new job ID.
					final String jobId = jobPlan.getJobId();

					// Update viewable fields.
					JobPlanPanel.this.jobIdField.setText(jobId);
					//JobPlanPanel.this.threadSpinner.setEnabled(true);
					//JobPlanPanel.this.contactEmail.setEnabled(true);
					//JobPlanPanel.this.updateEmailButton.setEnabled(true);
					JobPlanPanel.this.skipDropTable.setEnabled(true);

					// Same job ID as before? Remember expansion set.
					final boolean jobIdChanged = !jobId
							.equals(JobPlanPanel.this.jobId);

					final List openRows = new ArrayList();
					if (!jobIdChanged) {
						// Remember tree state.
						final Enumeration openNodePaths = JobPlanPanel.this.tree
								.getExpandedDescendants(JobPlanPanel.this.tree
										.getPathForRow(0));
						while (openNodePaths != null
								&& openNodePaths.hasMoreElements()) {
							final TreePath openNodePath = (TreePath) openNodePaths
									.nextElement();
							openRows.add(new Integer(JobPlanPanel.this.tree
									.getRowForPath(openNodePath)));
						}
						// Sort the row numbers to prevent weirdness with
						// opening parents of already opened paths.
						Collections.sort(openRows);
					} else
						// Update our job ID.
						JobPlanPanel.this.jobId = jobId;

					// Update tree.
					JobPlanPanel.this.treeModel.setJobPlan(jobPlan);

					if (!jobIdChanged)
						// Re-expand tree.
						for (final Iterator i = openRows.iterator(); i
								.hasNext();)
							JobPlanPanel.this.tree.expandRow(((Integer) i
									.next()).intValue());
				}
			}.start();
	}
}
