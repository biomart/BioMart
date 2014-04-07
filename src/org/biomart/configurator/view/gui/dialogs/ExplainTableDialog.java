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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.Transaction;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.configurator.model.JoinTable;
import org.biomart.configurator.model.SelectFromTable;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.configurator.view.gui.diagrams.ExplainContext;
import org.biomart.configurator.view.gui.diagrams.ExplainTransformationDiagram;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Table;

/**
 * This simple dialog explains a table by drawing a series of diagrams of the
 * underlying tables and relations involved in it.
 * <p>
 * It has two tabs. In the first tab goes an overview diagram. In the second tab
 * goes a series of smaller diagrams, each one an instance of
 * {@link ExplainTransformationDiagram} which represents a single step in the
 * transformation process required to produce the table being explained.
 */
public class ExplainTableDialog extends JDialog {
	private static final long serialVersionUID = 1;

	private static final int MAX_UNITS = McUtils.isStringEmpty(Settings.getProperty("maxunits"))? 50
			: Integer.parseInt(Settings.getProperty("maxunits"));

	private JCheckBox maskedHidden;

	private boolean needsRebuild;

	/**
	 * Opens an explanation showing the underlying relations and tables behind a
	 * specific dataset table.
	 * 
	 * @param martTab
	 *            the mart tab which will handle menu events.
	 * @param table
	 *            the table to explain.
	 */
/*	public static void showTableExplanation(JDialog owner,
			final DatasetTable table) {
		new ExplainTableDialog(owner,table).setVisible(true);
	}*/


	private Mart ds = null;

	private DatasetTable dsTable = null;

	private GridBagConstraints fieldConstraints;

	private GridBagConstraints fieldLastRowConstraints;

	private GridBagConstraints labelConstraints;

	private GridBagConstraints labelLastRowConstraints;

//	private MartTab martTab;

	private JPanel transformationPanel;

	private final List<ExplainTransformationDiagram> transformationTableDiagrams = new ArrayList<ExplainTransformationDiagram>();

//	private TransformationContext transformationContext;

	private ExplainContext explainContext = null;

	private final PropertyChangeListener listener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent evt) {
			ExplainTableDialog.this.needsRebuild = true;
		}
	};

	private final PropertyChangeListener recalcListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent evt) {
//			ExplainTableDialog.this.maskedHidden
//					.setSelected(ExplainTableDialog.this.dsTable
//							.isExplainHideMasked());
			ExplainTableDialog.this.recalculateTransformation();
		}
	};

	/**
	 * The background for the masked checkbox.
	 */
	public static final Color MASK_BG_COLOR = Color.WHITE;

	public ExplainTableDialog(JDialog owner, final DatasetTable dsTable) {
		// Create the blank dialog, and give it an appropriate title.
		super(owner);
		this.setTitle(Resources.get("explainTableDialogTitle", dsTable
				.getName()));
		this.setModal(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.ds = dsTable.getMart();
		this.dsTable = dsTable;

		// Create a context.
		this.explainContext = new ExplainContext(this, this.ds, dsTable);

		// Create constraints for labels that are not in the last row.
		this.labelConstraints = new GridBagConstraints();
		this.labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		this.labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		this.labelConstraints.anchor = GridBagConstraints.LINE_END;
		this.labelConstraints.insets = new Insets(0, 2, 0, 0);
		// Create constraints for fields that are not in the last row.
		this.fieldConstraints = new GridBagConstraints();
		this.fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		this.fieldConstraints.fill = GridBagConstraints.NONE;
		this.fieldConstraints.anchor = GridBagConstraints.LINE_START;
		this.fieldConstraints.insets = new Insets(0, 1, 0, 2);
		// Create constraints for labels that are in the last row.
		this.labelLastRowConstraints = (GridBagConstraints) this.labelConstraints
				.clone();
		this.labelLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;
		// Create constraints for fields that are in the last row.
		this.fieldLastRowConstraints = (GridBagConstraints) this.fieldConstraints
				.clone();
		this.fieldLastRowConstraints.gridheight = GridBagConstraints.REMAINDER;

		// Compute the transformation diagram.
		this.transformationPanel = new JPanel(new GridBagLayout());
		this.recalculateTransformation();
		JScrollPane sp = new JScrollPane(this.transformationPanel);



		// Set up our content pane.
		final JPanel content = new JPanel(new BorderLayout());
		this.setContentPane(content);


		content.add(sp, BorderLayout.CENTER);
	
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    this.setSize(screenSize.width-200,screenSize.height - 200);
		this.setLocationRelativeTo(null);
		this.setModal(true);

		// Add a listener to the dataset such that if any part of the dataset
		// changes, we recalculate ourselves entirely.
		this.needsRebuild = false;

//		this.ds.addPropertyChangeListener("directModified", this.listener);

		// Select the default button (which shows the transformation card).
		// We must physically click on it to make the card show.
		this.setVisible(true);

		
	}


	private void recalculateTransformation() {
		this.needsRebuild = false;
		new LongProcess() {
			public void run() throws Exception {
				// Keep a note of shown tables.
				final Map<String,Map<String,Object>> shownTables = new HashMap<String,Map<String,Object>>();
				for (int i = 1; i <= ExplainTableDialog.this.transformationTableDiagrams
						.size(); i++) {
					final TableComponent[] comps = ((ExplainTransformationDiagram) ExplainTableDialog.this.transformationTableDiagrams
							.get(i - 1)).getTableComponents();
					final Map<String,Object> map = new HashMap<String,Object>();
					shownTables.put("" + i, map);
					for (int j = 0; j < comps.length; j++)
						map.put(((Table) comps[j].getObject()).getName(),
								comps[j].getState());
				}

				// Clear the transformation box.
				ExplainTableDialog.this.transformationPanel.removeAll();
				ExplainTableDialog.this.transformationTableDiagrams.clear();

				// Keep track of columns counted so far.
				final List columnsSoFar = new ArrayList();

				// Count our steps.
				int stepNumber = 1;

				// If more than a set limit of units, we hit memory
				// and performance issues. Refuse to do the display and
				// instead put up a helpful message. Limit should be
				// configurable from a properties file.
				final Collection<TransformationUnit> units = new ArrayList<TransformationUnit>(
						ExplainTableDialog.this.dsTable
								.getTransformationUnits()); // To prevent
															// concmod.
				if (units.size() > ExplainTableDialog.MAX_UNITS)
					ExplainTableDialog.this.transformationPanel.add(new JLabel(
							Resources.get("tooManyUnits", ""
									+ ExplainTableDialog.MAX_UNITS)),
							ExplainTableDialog.this.fieldLastRowConstraints);
				else {
					// Insert show/hide hidden steps button.
					ExplainTableDialog.this.transformationPanel.add(new JLabel(),
							ExplainTableDialog.this.labelConstraints);
					JPanel field = new JPanel();
					ExplainTableDialog.this.transformationPanel.add(field,
							ExplainTableDialog.this.fieldConstraints);

					// Iterate over transformation units.
					for (final Iterator<TransformationUnit> i = units.iterator(); i.hasNext();) {
						final TransformationUnit tu =  i.next();
						// Holders for our stuff.
						final JLabel label;
						final ExplainTransformationDiagram diagram;
						Map<String,Object> map = shownTables.get("" + stepNumber);
						if (map==null)
							map = new HashMap<String,Object>();
						// Draw the unit.
			/*			if (tu instanceof Expression) {
							// Do an expression column list.
							label = new JLabel(
									Resources
											.get(
													"stepTableLabel",
													new String[] {
															"" + stepNumber,
															Resources
																	.get("explainExpressionLabel") }));
							diagram = new ExplainTransformationDiagram.AdditionalColumns(
									ExplainTableDialog.this.martTab, tu,
									stepNumber,
									ExplainTableDialog.this.explainContext,
									map);
						} else 
						if (tu instanceof SkipTable) {
							// Don't show these if we're hiding masked things.
							if (ExplainTableDialog.this.dsTable
									.isExplainHideMasked())
								continue;
							// Temp table to schema table join.
							label = new JLabel(
									Resources
											.get(
													"stepTableLabel",
													new String[] {
															"" + stepNumber,
															Resources
																	.get("explainSkipLabel") }));
							diagram = new ExplainTransformationDiagram.SkipTempReal(
									ExplainTableDialog.this.martTab,
									(SkipTable) tu, columnsSoFar, stepNumber,
									ExplainTableDialog.this.explainContext,
									map);
						} else */
							if (tu instanceof JoinTable) {
							// Temp table to schema table join.
							label = new JLabel(
									Resources
											.get(
													"stepTableLabel",
													new String[] {
															"" + stepNumber,
															Resources
																	.get("explainMergeLabel") }));
							diagram = new ExplainTransformationDiagram.TempReal(
									(JoinTable) tu, columnsSoFar, stepNumber,
									ExplainTableDialog.this.explainContext,
									map);
						} else if (tu instanceof SelectFromTable) {
							// Do a single-step select.
							label = new JLabel(
									Resources
											.get(
													"stepTableLabel",
													new String[] {
															"" + stepNumber,
															Resources
																	.get("explainSelectLabel") }));
							diagram = new ExplainTransformationDiagram.SingleTable(
									(SelectFromTable) tu, stepNumber,
									ExplainTableDialog.this.explainContext,
									map);
						} else
							throw new BioMartError();
						// Display the diagram.
						ExplainTableDialog.this.transformationPanel
								.add(
										label,
										i.hasNext() ? ExplainTableDialog.this.labelConstraints
												: ExplainTableDialog.this.labelLastRowConstraints);
					//	diagram.setDiagramContext(ExplainTableDialog.this.transformationContext);
						field = new JPanel();
						field.add(diagram);
						ExplainTableDialog.this.transformationPanel
								.add(
										field,
										i.hasNext() ? ExplainTableDialog.this.fieldConstraints
												: ExplainTableDialog.this.fieldLastRowConstraints);
						// Add columns from this unit to the transformed table.
						columnsSoFar.addAll(tu.getNewColumnNameMap().values());
						stepNumber++;
						// Remember what tables we just added.
						ExplainTableDialog.this.transformationTableDiagrams
								.add(diagram);
								
					}
				}

				// Resize the diagram to fit the components.
				ExplainTableDialog.this.transformationPanel.revalidate();
				ExplainTableDialog.this.transformationPanel.repaint();
			}
		}.start();
	}
}
