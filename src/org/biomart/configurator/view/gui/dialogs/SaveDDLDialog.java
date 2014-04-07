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

package org.biomart.configurator.view.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import org.biomart.common.exceptions.ListenerException;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.common.view.gui.dialogs.ViewTextDialog;
import org.biomart.configurator.controller.MartConstructor;
import org.biomart.configurator.controller.MartConstructorListener;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.SaveDDLMartConstructor;
import org.biomart.configurator.controller.MartConstructor.ConstructorRunnable;
import org.biomart.objects.objects.Mart;

/**
 * A dialog which allows the user to choose some options about creating DDL over
 * a given set of datasets, then lets them actually do it. The options include
 * whether to output to file or to screen.
 * 
 */
public class SaveDDLDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private JList prefixesList;
	private JTextField targetDatabaseName;
	private JTextField targetSchemaName;
	private JComboBox outputFormat;
	private JFileChooser outputFileChooser;
	private JTextField outputFileLocation;
	private JButton outputFileLocationButton;
	private JTextField runDDLHost;
	private JTextField runDDLPort;
	private JTextField overrideHost;
	private JTextField overridePort;
	
	private Mart mart;

	/**
	 * Constant referring to running DDL.
	 */
	public static final String RUN_DDL = Resources.get("runDDL");

	/**
	 * Constant referring to viewing DDL.
	 */
	public static final String VIEW_DDL = Resources.get("viewDDL");

	/**
	 * Constant referring to saving DDL.
	 */
	public static final String SAVE_DDL = Resources.get("filePerTableDDL");

	/**
	 * Creates (but does not display) a dialog centred on the given tab, which
	 * allows DDL generation for the given datasets. When the OK button is
	 * chosen, the DDL is generated in the background.
	 * 
	 * @param mart
	 *           the tab in which this will be displayed.
	 * @param prefixes
	 *            the schema partition prefixes to list.
	 * @param generateOption
	 *            the option to select in the dropdown.
	 */
	public SaveDDLDialog(final Mart mart,
			final Collection<String> prefixes, final String generateOption) {
		// Create the base dialog.
		super();
		this.setTitle(Resources.get("saveDDLDialogTitle"));
		this.setModal(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Remember the tabset that the schema we are working with is part of
		// (or will be part of if it's not been created yet).
		this.mart = mart;

		// Create the content pane for the dialog, ie. the bit that will hold
		// all the various questions and answers.
		final JPanel content = new JPanel(new GridBagLayout());
		this.setContentPane(content);

		// Create some constraints for labels, except those on the last row
		// of the dialog.
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create some constraints for fields, except those on the last row
		// of the dialog.
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.NONE;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create some constraints for labels on the last row of the dialog.
		final GridBagConstraints labelLastRowConstraints = (GridBagConstraints) labelConstraints
				.clone();
		labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create some constraints for fields on the last row of the dialog.
		final GridBagConstraints fieldLastRowConstraints = (GridBagConstraints) fieldConstraints
				.clone();
		fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Create input fields for target schema name and granularity,
		// and for run ddl host/port.
		this.targetDatabaseName = new JTextField(20);
		this.targetSchemaName = new JTextField(20);

		this.outputFormat = new JComboBox();
		this.outputFormat.addItem(SaveDDLDialog.SAVE_DDL);
		this.outputFormat.addItem(SaveDDLDialog.VIEW_DDL);
		this.outputFormat.addItem(SaveDDLDialog.RUN_DDL);

		// Create the list for choosing partitions.
		this.prefixesList = new JList(prefixes.toArray(new String[0]));
		this.prefixesList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.prefixesList.setSelectionInterval(0, prefixes.size() - 1);
		this.prefixesList.setVisibleRowCount(4); // Arbitrary.
		// Set the list to 30-characters wide. Longer than this and it will
		// show a horizontal scrollbar.
		this.prefixesList
				.setPrototypeCellValue("012345678901234567890123456789");

		// Create a file chooser for finding the DDL file we will save.
		this.outputFileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			public File getSelectedFile() {
				File file = super.getSelectedFile();
				if (file != null && !file.exists()) {
					final String filename = file.getName();
					final String extension = Resources.get("zipExtension");
					if (!filename.endsWith(extension)
							&& filename.indexOf('.') < 0)
						file = new File(file.getParentFile(), filename
								+ extension);
				}
				return file;
			}
		};
		final String currentDir = Settings.getProperty("currentSaveDir");
		this.outputFileChooser.setCurrentDirectory(currentDir == null ? null
				: new File(currentDir));
		this.outputFileChooser.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".zip".
			public boolean accept(final File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(
								Resources.get("zipExtension"));
			}

			public String getDescription() {
				return Resources.get("ZIPDDLFileFilterDescription");
			}
		});
		final JLabel outputFileLabel = new JLabel(Resources
				.get("saveDDLFileLocationLabel"));
		this.outputFileLocation = new JTextField(20);
		this.outputFileLocationButton = new JButton(Resources
				.get("browseButton"));

		// Attach the file chooser to the output file location button.
		this.outputFileLocationButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.outputFileChooser
						.showSaveDialog(content) == JFileChooser.APPROVE_OPTION) {
					Settings.setProperty("currentSaveDir",
							SaveDDLDialog.this.outputFileChooser
									.getCurrentDirectory().getPath());
					final File file = SaveDDLDialog.this.outputFileChooser
							.getSelectedFile();
					// When a file is chosen, put its name in the driver
					// class location field.
					if (file != null)
						SaveDDLDialog.this.outputFileLocation.setText(file
								.toString());
				}
			}
		});

		// Create the host/port label/fields.
		final JLabel outputHostLabel = new JLabel(Resources
				.get("runDDLHostLabel"));
		this.runDDLHost = new JTextField(20);
		final JLabel outputPortLabel = new JLabel(Resources
				.get("runDDLPortLabel"));
		this.runDDLPort = new JFormattedTextField(new DecimalFormat("0"));
		this.runDDLPort.setColumns(5);
		final JLabel overrideHostLabel = new JLabel(Resources
				.get("overrideHostLabel"));
		this.overrideHost = new JTextField(20);
		final JLabel overridePortLabel = new JLabel(Resources
				.get("overridePortLabel"));
		this.overridePort = new JFormattedTextField(new DecimalFormat("0"));
		this.overridePort.setColumns(5);

		// Add listeners to view DDL options which show/hide additional stuff.
		this.outputFormat.addItemListener(new ItemListener() {
			public void itemStateChanged(final ItemEvent e) {
				if (SaveDDLDialog.this.outputFormat.getSelectedItem().equals(
						Resources.get("filePerTableDDL"))) {
					outputFileLabel.setVisible(true);
					SaveDDLDialog.this.outputFileLocation.setVisible(true);
					SaveDDLDialog.this.outputFileLocationButton
							.setVisible(true);
				} else {
					outputFileLabel.setVisible(false);
					SaveDDLDialog.this.outputFileLocation.setVisible(false);
					SaveDDLDialog.this.outputFileLocationButton
							.setVisible(false);
				}
				if (SaveDDLDialog.this.outputFormat.getSelectedItem().equals(
						Resources.get("runDDL"))) {
					outputHostLabel.setVisible(true);
					outputPortLabel.setVisible(true);
					overrideHostLabel.setVisible(true);
					overridePortLabel.setVisible(true);
					SaveDDLDialog.this.runDDLHost.setVisible(true);
					SaveDDLDialog.this.runDDLPort.setVisible(true);
					SaveDDLDialog.this.overrideHost.setVisible(true);
					SaveDDLDialog.this.overridePort.setVisible(true);
					//SaveDDLDialog.this.overrideHost.setEnabled(false);
					//SaveDDLDialog.this.overridePort.setEnabled(false);	
					String hostStr = mart.getDatasetList().get(0).
						getDataLinkInfoForSource().getJdbcLinkObject().getHost();
					String portStr = mart.getDatasetList().get(0).
						getDataLinkInfoForSource().getJdbcLinkObject().getPort();
					SaveDDLDialog.this.overrideHost.setText(hostStr);
					SaveDDLDialog.this.overridePort.setText(portStr);					
				} else {
					outputHostLabel.setVisible(false);
					outputPortLabel.setVisible(false);
					overrideHostLabel.setVisible(false);
					overridePortLabel.setVisible(false);
					SaveDDLDialog.this.runDDLHost.setVisible(false);
					SaveDDLDialog.this.runDDLPort.setVisible(false);
					SaveDDLDialog.this.overrideHost.setVisible(false);
					SaveDDLDialog.this.overridePort.setVisible(false);
				}
				SaveDDLDialog.this.pack();
			}
		});

		// Lay out the window.

		JPanel field = new JPanel();
		content.add(field, fieldConstraints);

		// Add the prefix lists.
		JLabel label = new JLabel(Resources.get("selectedPrefixesLabel"));
		content.add(label, labelConstraints);
		field = new JPanel();
		field.add(new JScrollPane(this.prefixesList));
		content.add(field, fieldConstraints);

		// Add the target database settings label and field.
		label = new JLabel(Resources.get("targetDatabaseLabel"));
		content.add(label, labelConstraints);
		field = new JPanel();
		field.add(this.targetDatabaseName);
		content.add(field, fieldConstraints);

		// Add the target schema settings label and field.
		label = new JLabel(Resources.get("targetSchemaLabel"));
		content.add(label, labelConstraints);
		field = new JPanel();
		field.add(this.targetSchemaName);
		content.add(field, fieldConstraints);

		// Add the format field.
		label = new JLabel(Resources.get("outputFormatLabel"));
		content.add(label, labelConstraints);
		field = new JPanel();
		field.add(this.outputFormat);
		content.add(field, fieldConstraints);

		// Add the output location label, field and file chooser button.
		content.add(outputFileLabel, labelConstraints);
		field = new JPanel();
		field.add(this.outputFileLocation);
		field.add(this.outputFileLocationButton);
		content.add(field, fieldConstraints);

		// Add the output host/port etc..
		content.add(outputHostLabel, labelConstraints);
		field = new JPanel();
		field.add(this.runDDLHost);
		content.add(field, fieldConstraints);
		content.add(outputPortLabel, labelConstraints);
		field = new JPanel();
		field.add(this.runDDLPort);
		content.add(field, fieldConstraints);
		content.add(overrideHostLabel, labelConstraints);
		field = new JPanel();
		field.add(this.overrideHost);
		content.add(field, fieldConstraints);
		content.add(overridePortLabel, labelConstraints);
		field = new JPanel();
		field.add(this.overridePort);
		content.add(field, fieldConstraints);

		// The close and execute buttons.
		final JButton cancel = new JButton(Resources.get("cancelButton"));
		final JButton execute = new JButton(Resources.get("saveDDLButton"));

		// Intercept the close button, which closes the dialog
		// without taking any action.
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				SaveDDLDialog.this.setVisible(false);
			}
		});

		// Intercept the execute button, which validates the fields
		// then creates the DDL and closes the dialog.
		execute.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (SaveDDLDialog.this.validateFields()) {
					SaveDDLDialog.this.createDDL();
					SaveDDLDialog.this.setVisible(false);
					SaveDDLDialog.this.dispose();
				}
			}
		});

		// Add the buttons.
		label = new JLabel();
		content.add(label, labelLastRowConstraints);
		field = new JPanel();
		field.add(cancel);
		field.add(execute);
		content.add(field, fieldLastRowConstraints);

		// Make execute the default button.
		this.getRootPane().setDefaultButton(execute);

		// Set a default value of View SQL.
		SaveDDLDialog.this.outputFormat.setSelectedItem(generateOption);

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	/**
	 * This method takes the settings from the dialog, having already been
	 * validated, and uses them to set up and start the DDL generation process.
	 */
	private void createDDL() {
		// What datasets are we making DDL for?
		Collection<String> selectedPrefixes = new ArrayList<String>();
		for(Object obj: this.prefixesList.getSelectedValues()) {
			selectedPrefixes.add((String)obj);
		}
		// Make a stringbuffer in case we want screen output.
		final StringBuffer sb = new StringBuffer();
		// Make the constructor object which will create the DDL.
		MartConstructor constructor;
		if (this.outputFormat.getSelectedItem().equals(
				Resources.get("filePerTableDDL")))
			constructor = new SaveDDLMartConstructor(new File(
					this.outputFileLocation.getText()));
		else if (this.outputFormat.getSelectedItem().equals(
				Resources.get("runDDL")))
			constructor = new SaveDDLMartConstructor(this.runDDLHost.getText(),
					this.runDDLPort.getText(), this.overrideHost.getText(),
					this.overridePort.getText());
		else
			constructor = new SaveDDLMartConstructor(sb);

		try {
			// Obtain the DDL generator from the constructor object.

			final ConstructorRunnable cr = constructor.getConstructorRunnable(
					targetDatabaseName.getText(), targetSchemaName.getText(), mart,
					selectedPrefixes);
			// If we want screen output, add a listener that listens for
			// completion of construction. When completed, use the
			// stringbuffer, which will contain the DDL, to pop up a simple
			// text dialog for the user to view it with. Also if we want
			// remote host output, a remote host monitor dialog pops up instead.
			cr.addMartConstructorListener(new MartConstructorListener() {
				public void martConstructorEventOccurred(final int event,
						final Object data, final org.biomart.configurator.model.MartConstructorAction action)
						throws ListenerException {
					if (event == MartConstructorListener.CONSTRUCTION_ENDED
							&& cr.getFailureException() == null)
						if (SaveDDLDialog.this.outputFormat.getSelectedItem()
								.equals(SaveDDLDialog.VIEW_DDL))
							ViewTextDialog
									.displayText(Resources
											.get("mcViewDDLWindowTitle"), sb
											.toString());
						else if (SaveDDLDialog.this.outputFormat
								.getSelectedItem()
								.equals(SaveDDLDialog.RUN_DDL)) {
							//update schema partitiontable
							String databaseName = SaveDDLDialog.this.targetDatabaseName.getText();
							String schemaName = SaveDDLDialog.this.targetSchemaName.getText();
							int col1 = PartitionUtils.DATABASE;
							int col2 = PartitionUtils.SCHEMA;
							mart.getSchemaPartitionTable().setValue(0, col1, databaseName);
							mart.getSchemaPartitionTable().setValue(0, col2, schemaName);
							MartController.getInstance().requestMonitorRemoteHost(
												SaveDDLDialog.this.runDDLHost
														.getText(),
												SaveDDLDialog.this.runDDLPort
														.getText());
						}
				}

			});
			MartController.getInstance().requestMonitorConstructorRunnable(this,cr);
		} catch (final Throwable t) {
			StackTrace.showStackTrace(t);
			JOptionPane.showMessageDialog(this, Resources
					.get("martConstructionFailed"), Resources
					.get("messageTitle"), JOptionPane.WARNING_MESSAGE);
		}
		
	}

	private boolean isEmpty(final String string) {
		// Strings are empty if they are null or all whitespace.
		return string == null || string.trim().length() == 0;
	}

	private boolean validateFields() {
		// List of messages to display, if any are necessary.
/*		final List messages = new ArrayList();

		// Must have a target schema.
		if (this.isEmpty(this.targetDatabaseName.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("targetDatabase")));

		// Must have a target schema.
		if (this.isEmpty(this.targetSchemaName.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("targetSchema")));

		// Must have an output file.
		if (this.outputFormat.getSelectedItem().equals(SaveDDLDialog.SAVE_DDL)
				&& this.isEmpty(this.outputFileLocation.getText()))
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("saveDDLFileLocation")));

		// Must have an output host/port.
		if (this.outputFormat.getSelectedItem().equals(SaveDDLDialog.RUN_DDL)) {
			if (this.isEmpty(this.runDDLHost.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("runDDLHost")));
			if (this.isEmpty(this.runDDLPort.getText()))
				messages.add(Resources.get("fieldIsEmpty", Resources
						.get("runDDLPort")));
			// Both or neither override settings = EOR = XOR.
			if (this.isEmpty(this.overrideHost.getText())
					^ this.isEmpty(this.overridePort.getText()))
				if (this.isEmpty(this.overrideHost.getText()))
					messages.add(Resources.get("fieldIsEmpty", Resources
							.get("overrideHost")));
				else
					messages.add(Resources.get("fieldIsEmpty", Resources
							.get("overridePort")));
		}

		// Must have at least one dataset selected.
		if (this.datasetsList.getSelectedValues().length == 0)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("selectedDataSets")));

		// Must have at least one prefix selected.
		if (this.prefixesList.getModel().getSize() > 0
				&& this.prefixesList.getSelectedValues().length == 0)
			messages.add(Resources.get("fieldIsEmpty", Resources
					.get("selectedPrefixes")));

		// Check it has a _key column on main table in every dataset.
		for (int i = 0; i < this.datasetsList.getSelectedValues().length; i++) {
			final DataSet dataset = (DataSet) this.datasetsList
					.getSelectedValues()[i];
			boolean hasKeyCol = false;
			for (final Iterator j = dataset.getMainTable().getColumns()
					.values().iterator(); !hasKeyCol && j.hasNext();)
				hasKeyCol |= ((DataSetColumn) j.next()).getModifiedName()
						.endsWith(Resources.get("keySuffix"));
			if (!hasKeyCol) {
				// Prompt.
				final int option = JOptionPane.showConfirmDialog(this,
						Resources.get("datasetNoKeyColConfirm", dataset
								.getName()), Resources.get("questionTitle"),
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				// If prompt says NO, add to the failure messages.
				if (option != JOptionPane.YES_OPTION)
					messages.add(Resources.get("datasetNoKeyCol", dataset
							.getName()));
			}
		}

		// Any messages to display? Show them.
		if (!messages.isEmpty())
			JOptionPane.showMessageDialog(null,
					messages.toArray(new String[0]), Resources
							.get("validationTitle"),
					JOptionPane.INFORMATION_MESSAGE);

		// Validation succeeds if there are no messages.
		return messages.isEmpty();
		*/
		return true;
	}
}
