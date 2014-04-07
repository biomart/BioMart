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

package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.runner.controller.MartRunnerProtocol.Client;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobStatus;
import org.biomart.runner.view.gui.JobPlanListCellRenderer;
import org.biomart.runner.view.gui.JobPlanListModel;
import org.biomart.runner.view.gui.JobPlanPanel;

public class MartRunnerMonitorDialog extends JDialog {
	private static final long serialVersionUID = 1;

	public static final Font PLAIN_FONT = Font.decode("SansSerif-PLAIN-12");

	private static final Font ITALIC_FONT = Font.decode("SansSerif-ITALIC-12");

	private static final Font BOLD_FONT = Font.decode("SansSerif-BOLD-12");

	private static final Font BOLD_ITALIC_FONT = Font
			.decode("SansSerif-BOLDITALIC-12");

	public static final Color PALE_BLUE = Color.decode("0xEEEEFF");

	public static final Color PALE_GREEN = Color.decode("0xEEFFEE");

	public static final Map<JobStatus,Color> STATUS_COLOR_MAP = new HashMap<JobStatus,Color>();

	public static final Map<JobStatus,Font> STATUS_FONT_MAP = new HashMap<JobStatus,Font>();

	public final JButton refreshJobList;

	public static String shost;
	public static String sport;
	public  String host;
	public  String port;
	private JobPlanListModel jobPlanListModel; 
	private JobPlanPanel jobPlanPanel;
	
	private static MartRunnerMonitorDialog dialog = null;
	static {
		// Colours.
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.NOT_QUEUED,
				Color.BLACK);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.INCOMPLETE,
				Color.CYAN);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.QUEUED,
				Color.MAGENTA);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.FAILED,
				Color.RED);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.RUNNING,
				Color.BLUE);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.STOPPED,
				Color.ORANGE);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.COMPLETED,
				Color.GREEN);
		MartRunnerMonitorDialog.STATUS_COLOR_MAP.put(JobStatus.UNKNOWN,
				Color.LIGHT_GRAY);
		// Fonts
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.NOT_QUEUED,
				MartRunnerMonitorDialog.PLAIN_FONT);
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.INCOMPLETE,
				MartRunnerMonitorDialog.ITALIC_FONT);
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.QUEUED,
				MartRunnerMonitorDialog.PLAIN_FONT);
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.FAILED,
				MartRunnerMonitorDialog.BOLD_FONT);
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.RUNNING,
				MartRunnerMonitorDialog.BOLD_ITALIC_FONT);
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.STOPPED,
				MartRunnerMonitorDialog.BOLD_FONT);
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.COMPLETED,
				MartRunnerMonitorDialog.PLAIN_FONT);
		MartRunnerMonitorDialog.STATUS_FONT_MAP.put(JobStatus.UNKNOWN,
				MartRunnerMonitorDialog.ITALIC_FONT);
	}

	/**
	 * Opens an explanation showing what a remote MartRunner host is up to.
	 * 
	 * @param host
	 *            the host to monitor.
	 * @param port
	 *            the port to connect to the host with.
	 */
	public static void monitor(final String host, final String port) {
		if(dialog==null)
			dialog =  new MartRunnerMonitorDialog(host, port);
		if(!"".equals(host)) {
			dialog.updateHostPort(host, port);
			dialog.doRefresh();
		}		
		dialog.setModal(true);
		dialog.setVisible(true);
	}
	
	/**
	 * update all related host & port
	 * @param host
	 * @param port
	 */
	public void updateHostPort(String host, String port) {
		this.host = host;
		this.port = port;
		MartRunnerMonitorDialog.shost = host;
		MartRunnerMonitorDialog.sport = port;
		jobPlanListModel.updateHostAndPort(host, port);
		jobPlanPanel.updateHostAndPort(host, port);
	}

	private MartRunnerMonitorDialog(final String host, final String port) {
		// Make the RHS scrollpane containing job descriptions.
		jobPlanPanel = new JobPlanPanel(this, host, port);
		this.host = host;
		this.port = port;

		// Make the LHS list of jobs.
		jobPlanListModel = new JobPlanListModel(host,port);
		final JList jobList = new JList(jobPlanListModel);
		jobList.setCellRenderer(new JobPlanListCellRenderer());
		jobList.setBackground(Color.WHITE);
		jobList.setOpaque(true);
		jobList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.refreshJobList = new JButton(Resources.get("refreshButton"));
		final JPanel jobListPanel = new JPanel(new BorderLayout());
		jobListPanel.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
		jobListPanel.add(new JLabel(Resources.get("jobListTitle")),
				BorderLayout.PAGE_START);
		jobListPanel.add(new JScrollPane(jobList), BorderLayout.CENTER);
		final JPanel refreshPanel = new JPanel();
		refreshPanel.add(this.refreshJobList);
//		refreshPanel.add(refreshRate);
		jobListPanel.add(refreshPanel, BorderLayout.PAGE_END);
		// Updates when refresh button is hit.
		this.refreshJobList.addActionListener(new ActionListener() {
			private boolean firstRun = true;

			public void actionPerformed(final ActionEvent e) {
				new LongProcess() {
					public void run() {
						Object selection = jobList.getSelectedValue();
						try {
							jobPlanListModel.updateList();
						} catch (final ProtocolException e) {
							StackTrace.showStackTrace(e);
						} finally {
							// Attempt to select the first item on first run.
							if (firstRun && jobPlanListModel.size() > 0)
								selection = jobPlanListModel.lastElement();
							jobList.setSelectedValue(selection, true);
							firstRun = false;
						}
					}
				}.start();
			}
		});

		// Add a listener to the list to update the pane on the right.
		jobList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent e) {
				final Object selection = jobList.getSelectedValue();
				if (!e.getValueIsAdjusting())
					// Update the panel on the right with the new job.
					if(!MartRunnerMonitorDialog.this.host.equals(""))
					jobPlanPanel
							.setJobPlan(selection instanceof JobPlan ? (JobPlan) selection
									: null);
			}
		});

		// Add context menu to the job list.
		jobList.addMouseListener(new MouseListener() {
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
					final int index = jobList.locationToIndex(e.getPoint());
					if (index >= 0) {
						final JobPlan plan = (JobPlan) jobPlanListModel
								.getElementAt(index);
						final JPopupMenu menu = new JPopupMenu();

						// Remove job.
						final JMenuItem empty = new JMenuItem(Resources
								.get("emptyTableJobTitle"));
						empty.setMnemonic(Resources
								.get("emptyTableJobMnemonic").charAt(0));
						empty.addActionListener(new ActionListener() {
							public void actionPerformed(final ActionEvent evt) {
								new LongProcess() {
									public void run() throws Exception {
										// Do the job.
										final Socket clientSocket = Client
												.createClientSocket(host, port);
										Client.makeEmptyTableJob(clientSocket,
												plan.getJobId());
										clientSocket.close();
									}
								}.start();
							}
						});
						menu.add(empty);

						menu.addSeparator();

						// Remove job.
						final JMenuItem remove = new JMenuItem(Resources
								.get("removeJobTitle"));
						remove.setMnemonic(Resources.get("removeJobMnemonic")
								.charAt(0));
						remove.addActionListener(new ActionListener() {
							public void actionPerformed(final ActionEvent evt) {
								// Confirm.
								if (JOptionPane.showConfirmDialog(jobList,
										Resources.get("removeJobConfirm"),
										Resources.get("questionTitle"),
										JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
									new LongProcess() {
										public void run() throws Exception {
											// Remove the job.
											final Socket clientSocket = Client
													.createClientSocket(host,
															port);
											Client.removeJob(clientSocket, plan
													.getJobId());
											clientSocket.close();
											// Update the list.
											MartRunnerMonitorDialog.this.refreshJobList
													.doClick();
										}
									}.start();
							}
						});
						menu.add(remove);

						// Remove all job.
						final JMenuItem removeAll = new JMenuItem(Resources
								.get("removeAllJobsTitle"));
						removeAll.setMnemonic(Resources.get(
								"removeAllJobsMnemonic").charAt(0));
						removeAll.addActionListener(new ActionListener() {
							public void actionPerformed(final ActionEvent evt) {
								// Confirm.
								if (JOptionPane.showConfirmDialog(jobList,
										Resources.get("removeAllJobsConfirm"),
										Resources.get("questionTitle"),
										JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
									new LongProcess() {
										public void run() throws Exception {
											// Remove all jobs.
											final Enumeration e = jobPlanListModel
													.elements();
											final Socket clientSocket = Client
													.createClientSocket(host,
															port);
											while (e.hasMoreElements())
												Client.removeJob(clientSocket,
														((JobPlan) e
																.nextElement())
																.getJobId());
											clientSocket.close();
											// Update the list.
											MartRunnerMonitorDialog.this.refreshJobList
													.doClick();
										}
									}.start();
							}
						});
						menu.add(removeAll);

						// Show the menu.
						menu.show(jobList, e.getX(), e.getY());
						e.consume();
					}
				}
			}

		});

		// Make the content pane.
		final JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, false, jobListPanel, jobPlanPanel);
		splitPane.setOneTouchExpandable(true);
//		this.setLayout(new GridLayout(1,1));
//		this.add(splitPane);

		// Set up our content pane.
		this.setContentPane(splitPane);

		// Pack the window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	public void doRefresh() {
		if(this.host!=null && !this.host.equals(""))
			this.refreshJobList.doClick();
	}

}
