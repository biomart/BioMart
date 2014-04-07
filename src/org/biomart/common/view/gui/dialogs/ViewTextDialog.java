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

package org.biomart.common.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultEditorKit;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.configurator.utils.McUtils;

/**
 * A dialog which allows the user to view some text, and optionally print,
 * search and save it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.12 $, $Date: 2007/07/11 13:12:29 $, modified by
 *          $Author: rh4 $
 * @since 0.6
 */
public class ViewTextDialog extends JFrame {
	private static final long serialVersionUID = 1;

	private ViewTextDialog(final String title, final String text) {
		// Create the base dialog.
		super(title);

		// Create the content pane for the dialog.
		final JPanel content = new JPanel(new BorderLayout());
		this.setContentPane(content);

		// Build the text editor pane.
		final JTextArea editorPane = new JTextArea(text);

		// Make it read-only and word-wrapped.
		editorPane.setEditable(false);
		editorPane.setWrapStyleWord(true);
		editorPane.setLineWrap(true);

		// Create a simple copy/select-all/wrap menu.
		final JPopupMenu menu = new JPopupMenu();

		// Copy.
		final JMenuItem copy = new JMenuItem(editorPane.getActionMap().get(
				DefaultEditorKit.copyAction));
		copy.setText(Resources.get("copy"));
		copy.setMnemonic(Resources.get("copyMnemonic").charAt(0));
		menu.add(copy);

		// Select-all.
		final JMenuItem selectAll = new JMenuItem(editorPane.getActionMap()
				.get(DefaultEditorKit.selectAllAction));
		selectAll.setText(Resources.get("selectAll"));
		selectAll.setMnemonic(Resources.get("selectAllMnemonic").charAt(0));
		menu.add(selectAll);

		menu.addSeparator();

		// Wrap.
		final JCheckBoxMenuItem wrap = new JCheckBoxMenuItem(Resources
				.get("wordWrap"));
		wrap.setMnemonic(Resources.get("wordWrapMnemonic").charAt(0));
		wrap.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				editorPane.setLineWrap(wrap.isSelected());
			}
		});
		wrap.setSelected(editorPane.getWrapStyleWord());
		menu.add(wrap);

		// Attach a mouse listener to the editor pane that
		// will open the menu on demand.
		editorPane.addMouseListener(new MouseListener() {
			public void mouseReleased(final MouseEvent e) {
				this.handleMouse(e);
			}

			public void mouseClicked(final MouseEvent e) {
				this.handleMouse(e);
			}

			public void mousePressed(final MouseEvent e) {
				this.handleMouse(e);
			}

			public void mouseEntered(final MouseEvent e) {
				this.handleMouse(e);
			}

			public void mouseExited(final MouseEvent e) {
				this.handleMouse(e);
			}

			private void handleMouse(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					copy.setEnabled(editorPane.getSelectedText() != null);
					menu.show(e.getComponent(), e.getX(), e.getY());
					e.consume();
				}
			}
		});

		// Put the editor pane in a scroll pane.
		final JScrollPane editorScrollPane = new JScrollPane(editorPane);
		editorScrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		editorScrollPane.setPreferredSize(new Dimension(600, 400));

		// Build the toolbar.
		final JToolBar toolBarPane = new JToolBar();
		toolBarPane.setFloatable(false);
		toolBarPane.setRollover(true);

		// Create a file chooser for finding the TXT file we will save.
		final JFileChooser saver = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			public File getSelectedFile() {
				File file = super.getSelectedFile();
				if (file != null && !file.exists()) {
					final String filename = file.getName();
					final String extension = Resources.get("txtExtension");
					if (!filename.endsWith(extension)
							&& filename.indexOf('.') < 0)
						file = new File(file.getParentFile(), filename
								+ extension);
				}
				return file;
			}
		};
		final String currentDir = Settings.getProperty("currentSaveDir");
		saver.setCurrentDirectory(currentDir == null ? null : new File(
				currentDir));
		saver.setFileFilter(new FileFilter() {
			// Accepts only files ending in ".txt".
			public boolean accept(final File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(
								Resources.get("txtExtension"));
			}

			public String getDescription() {
				return Resources.get("TXTFileFilterDescription");
			}
		});

		// Make the save button as an image.
		final JButton saverButton = new JButton(McUtils.createImageIcon(Resources
				.get("SAVEIMAGE")));
		saverButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (saver.showSaveDialog(ViewTextDialog.this) == JFileChooser.APPROVE_OPTION) {
					Settings.setProperty("currentSaveDir", saver
							.getCurrentDirectory().getPath());
					final File file = saver.getSelectedFile();
					// When a file is chosen, save the file.
					if (file != null)
						new LongProcess() {
							public void run() throws Exception {
								FileWriter fw = null;
								try {
									fw = new FileWriter(file);
									fw.write(editorPane.getText());
									fw.flush();
								} finally {
									if (fw != null)
										try {
											fw.close();
										} catch (final IOException e) {
											// Ignore this one.
										}
								}
							}
						}.start();
				}
			}
		});

		// Add the save option to the toolbar.
		toolBarPane.add(saverButton);

		// Make a print button.
		final JButton printButton = new JButton(McUtils.createImageIcon(Resources
				.get("PRINTIMAGE")));
		printButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				new LongProcess() {
					public void run() throws Exception {
						(new ComponentPrinter(editorPane)).print();
					}
				}.start();
			}
		});
		toolBarPane.add(printButton);

		// Make a find button.
		final JTextField searchText = new JTextField(20);
		final JButton searchButton = new JButton(Resources.get("searchButton"));
		searchText.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				searchButton.doClick();
				searchText.requestFocus();
			}
		});
		searchButton.addActionListener(new ActionListener() {
			private Matcher matcher;

			private String currSearch = "";

			public void actionPerformed(final ActionEvent e) {
				final String search = searchText.getText().trim();
				if (search.length() == 0)
					return;
				if (!this.currSearch.equals(search)) {
					this.currSearch = search;
					this.matcher = Pattern.compile(this.currSearch).matcher(
							editorPane.getText());
					editorPane.setCaretPosition(0);
				}
				if (this.matcher.find(editorPane.getCaretPosition())) {
					editorPane.getCaret().setDot(this.matcher.start());
					editorPane.getCaret().moveDot(this.matcher.end());
					editorPane.getCaret().setSelectionVisible(true);
				} else
					Toolkit.getDefaultToolkit().beep();
			}
		});
		searchText.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(final DocumentEvent e) {
				this.documentEvent(e);
			}

			public void insertUpdate(final DocumentEvent e) {
				this.documentEvent(e);
			}

			public void removeUpdate(final DocumentEvent e) {
				this.documentEvent(e);
			}

			private void documentEvent(final DocumentEvent e) {
				searchButton.doClick();
			}
		});
		toolBarPane.add(searchText);
		toolBarPane.add(searchButton);

		// Construct the content panel.
		content.add(toolBarPane, BorderLayout.PAGE_START);
		content.add(editorScrollPane, BorderLayout.CENTER);

		// Set size of window.
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	/**
	 * Displays the given text in a dialog with the given title, allowing the
	 * user to search/print/save the text but not edit it.
	 * 
	 * @param title
	 *            the title to give the dialog.
	 * @param textBuffer
	 *            the text to show.
	 */
	public static void displayText(final String title, final String textBuffer) {
		// Create and show a window frame.
		new ViewTextDialog(title, textBuffer).setVisible(true);
	}
}
