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

package org.biomart.configurator.view.gui.diagrams;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.biomart.configurator.model.FakeTable;
import org.biomart.configurator.view.component.KeyComponent;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.SchemaComponent;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.configurator.view.gui.diagrams.ExplainTransformationDiagram.RealisedRelation;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.Table;

/**
 * This context applies to the general schema view, as seen via a dataset tab.
 * It allows dataset-specific things such as masked relations to be set up,
 * where those things have to be defined against the source schema rather than
 * the dataset's generated schema.
 */
public class ExplainContext extends SchemaContext {
	private Mart dataset;

	private DatasetTable datasetTable;

	/**
	 * Creates a new context within a given set of tabs, which applies to a
	 * specific dataset table. All menu options will apply to this dataset, and
	 * operations working with these datasets will be delegated to the methods
	 * specified in the tabset.
	 * 
	 * @param martTab
	 *            the mart tab that the dataset tab appears within.
	 * @param dataset
	 *            the dataset the table is in.
	 * @param datasetTable
	 *            the dataset table we are attached to.
	 */
	public ExplainContext(final JDialog parent, final Mart dataset,
			final DatasetTable datasetTable) {
		super(parent);
		this.dataset = dataset;
		this.datasetTable = datasetTable;
	}


	public void customiseAppearance(final JComponent component,
			final Object object) {

		if (object instanceof Relation)
			this.customiseRelationAppearance(component, (Relation) object,
					RealisedRelation.NO_ITERATION);

		// Schema objects.
		else if (object instanceof SourceSchema) {
			final SourceSchema schema = (SourceSchema) object;
			final SchemaComponent schcomp = (SchemaComponent) component;
			if (this.isMasked(schema))
				schcomp.setBackground(SchemaComponent.MASKED_BACKGROUND);
			else
				schcomp.setBackground(SchemaComponent.BACKGROUND_COLOUR);

			schcomp.setRenameable(false);
			schcomp.setSelectable(false);
		}

		// This section customises table objects.
		else if (object instanceof Table) {
			final Table table = (Table) object;
			final TableComponent tblcomp = (TableComponent) component;


			tblcomp.setBackground(TableComponent.BACKGROUND_COLOUR);

	/*		if (this.getDataSetTable() != null)
				tblcomp.setRestricted(table.getRestrictTable(this.getDataSet(),
						this.getDataSetTable().getName()) != null);*/
		}

		// This section customises the appearance of key objects within
		// table objects in the diagram.
		else if (object instanceof Key) {
			final KeyComponent keycomp = (KeyComponent) component;

			// All are normal.
			keycomp.setForeground(KeyComponent.NORMAL_COLOUR);

			// Remove drag-and-drop from the key as it does not apply in
			// the window context.
			keycomp.setDraggable(false);
		}
		
	}

	public boolean isMasked(final Object object) {
		return false;
	}

	/**
	 * See {@link #customiseAppearance(JComponent, Object)} but this applies to
	 * a particular relation iteration.
	 * 
	 * @param component
	 *            See {@link #customiseAppearance(JComponent, Object)}.
	 * @param relation
	 *            the relation.
	 * @param iteration
	 *            the iteration of the relation, or
	 *            {@link RealisedRelation#NO_ITERATION} for all iterations.
	 */
	public void customiseRelationAppearance(final JComponent component,
			final Relation relation, final int iteration) {

		// Is it restricted?
/*		if (this.getDataSetTable() != null)
			((RelationComponent) component)
					.setRestricted(relation.isRestrictRelation(this
							.getDataSet(), this.getDataSetTable().getName())
							&& (iteration == RealisedRelation.NO_ITERATION || relation
									.getRestrictRelation(this.getDataSet(),
											this.getDataSetTable().getName(),
											iteration) != null));

		// Is it compounded?
		((RelationComponent) component)
				.setCompounded((this.getDataSetTable() == null ? relation
						.getCompoundRelation(this.getDataSet()) : relation
						.getCompoundRelation(this.getDataSet(), this
								.getDataSetTable().getName())) != null);

		// Is it loopback?
		((RelationComponent) component)
				.setLoopback((this.getDataSetTable() == null ? relation
						.getLoopbackRelation(this.getDataSet()) : relation
						.getLoopbackRelation(this.getDataSet(), this
								.getDataSetTable().getName())) != null);

		// Fade out all UNINCLUDED and MASKED relations.
		boolean included = this.getDataSetTable() == null ? this.getDataSet()
				.getIncludedRelations().contains(relation) : this
				.getDataSetTable().getIncludedRelations().contains(relation);
		if (!included
				|| (this.getDataSetTable() == null ? relation
						.isMaskRelation(this.getDataSet()) : relation
						.isMaskRelation(this.getDataSet(), this
								.getDataSetTable().getName())))
			component.setForeground(RelationComponent.MASKED_COLOUR);
*/
		// Highlight SUBCLASS relations.
		if (relation.isSubclassRelation(null))
			component.setForeground(RelationComponent.SUBCLASS_COLOUR);

/*		// Highlight UNROLLED relations.
		else if (this.getDataSetTable() != null
				&& relation.getUnrolledRelation(this.getDataSet()) != null
				&& !this.getDataSetTable().getType().equals(
						DatasetTableType.DIMENSION))
			component.setForeground(RelationComponent.UNROLLED_COLOUR);
*/
		// All others are normal.
		else
			component.setForeground(RelationComponent.NORMAL_COLOUR);

	}

	/**
	 * Obtain the dataset that this context is linked with.
	 * 
	 * @return our dataset.
	 */
	protected Mart getDataSet() {
		return this.dataset;
	}

	/**
	 * Obtain the dataset that this context is linked with.
	 * 
	 * @return our dataset.
	 */
	protected DatasetTable getDataSetTable() {
		return this.datasetTable;
	}

	public void populateContextMenu(final JPopupMenu contextMenu,
			final Object object) {

/*		if (object instanceof Relation)
			this.populateRelationContextMenu(contextMenu, (Relation) object,
					RealisedRelation.NO_ITERATION);
		// This menu is attached to all table objects.
		else if (object instanceof Table) {
			// Add a separator if there's other stuff before us.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Obtain the table object we should refer to.
			final Table table = (Table) object;

			// Accept/Reject changes - only appear in explain dataset
			// table, and only enabled if dataset table includes this table
			// and dataset table has visible modified columns from this
			// table.
			if (this.getDataSetTable() != null) {
				final JMenuItem accept = new JMenuItem(Resources
						.get("acceptChangesTitle"));
				accept.setMnemonic(Resources.get("acceptChangesMnemonic")
						.charAt(0));
				accept.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainContext.this.getMartTab().getDataSetTabSet()
								.requestAcceptAll(
										ExplainContext.this.getDataSetTable(),
										table);
					}
				});
				accept.setEnabled(this.getDataSetTable()
						.hasVisibleModifiedFrom(table));
				contextMenu.add(accept);

				final JMenuItem reject = new JMenuItem(Resources
						.get("rejectChangesTitle"));
				reject.setMnemonic(Resources.get("rejectChangesMnemonic")
						.charAt(0));
				reject.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainContext.this.getMartTab().getDataSetTabSet()
								.requestRejectAll(
										ExplainContext.this.getDataSetTable(),
										table);
					}
				});
				reject.setEnabled(this.getDataSetTable()
						.hasVisibleModifiedFrom(table));
				contextMenu.add(reject);

				contextMenu.addSeparator();
			}

			// The mask option allows the user to mask all
			// relations on a table.
			final JMenuItem mask = new JMenuItem(Resources
					.get("maskTableTitle"));
			mask.setMnemonic(Resources.get("maskTableMnemonic").charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestMaskAllRelations(
									ExplainContext.this.getDataSet(),
									ExplainContext.this.getDataSetTable(),
									table);
				}
			});
			contextMenu.add(mask);

			// The unmask option allows the user to unmask all
			// relations on a table.
			final JMenuItem unmask = new JMenuItem(Resources
					.get("unmaskTableTitle"));
			unmask.setMnemonic(Resources.get("unmaskTableMnemonic").charAt(0));
			unmask.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnmaskAllRelations(
									ExplainContext.this.getDataSet(),
									ExplainContext.this.getDataSetTable(),
									table);
				}
			});
			contextMenu.add(unmask);

			contextMenu.addSeparator();

			// If it's a restricted table...
			if (this.getDataSetTable() != null
					&& ((Table) object).getRestrictTable(this.getDataSet(),
							this.getDataSetTable().getName()) != null) {

				// Option to modify restriction.
				final JMenuItem modify = new JMenuItem(Resources
						.get("modifyTableRestrictionTitle"), new ImageIcon(
						Resources.getResourceAsURL("filter.gif")));
				modify.setMnemonic(Resources.get(
						"modifyTableRestrictionMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainContext.this.getMartTab().getDataSetTabSet()
								.requestRestrictTable(
										ExplainContext.this.getDataSet(),
										ExplainContext.this.getDataSetTable(),
										table);
					}
				});
				contextMenu.add(modify);
				if (this.getDataSetTable() == null)
					modify.setEnabled(false);

			} else {

				// Add a table restriction.
				final JMenuItem restriction = new JMenuItem(Resources
						.get("addTableRestrictionTitle"), new ImageIcon(
						Resources.getResourceAsURL("filter.gif")));
				restriction.setMnemonic(Resources.get(
						"addTableRestrictionMnemonic").charAt(0));
				restriction.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						ExplainContext.this.getMartTab().getDataSetTabSet()
								.requestRestrictTable(
										ExplainContext.this.getDataSet(),
										ExplainContext.this.getDataSetTable(),
										table);
					}
				});
				contextMenu.add(restriction);
				if (this.getDataSetTable() == null)
					restriction.setEnabled(false);
			}

			// Option to remove restriction.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeTableRestrictionTitle"));
			remove.setMnemonic(Resources.get("removeTableRestrictionMnemonic")
					.charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestUnrestrictTable(
									ExplainContext.this.getDataSet(),
									ExplainContext.this.getDataSetTable(),
									table);
				}
			});
			contextMenu.add(remove);
			if (this.getDataSetTable() == null
					|| table.getRestrictTable(this.getDataSet(), this
							.getDataSetTable().getName()) == null)
				remove.setEnabled(false);

			// Option to set 'big table' value.
			final JCheckBoxMenuItem bigTable = new JCheckBoxMenuItem(Resources
					.get("bigTableTitle"));
			bigTable.setMnemonic(Resources.get("bigTableMnemonic").charAt(0));
			bigTable.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestBigTable(ExplainContext.this.getDataSet(),
									ExplainContext.this.getDataSetTable(),
									table);
				}
			});
			bigTable.setSelected((this.getDataSetTable() == null ? table
					.getBigTable(this.getDataSet()) : table.getBigTable(this
					.getDataSet(), this.getDataSetTable().getName())) > 0);
			contextMenu.add(bigTable);

			// The transform start option.
			final JCheckBoxMenuItem transformStart = new JCheckBoxMenuItem(
					Resources.get("transformStartTitle"));
			transformStart.setMnemonic(Resources.get("transformStartMnemonic")
					.charAt(0));
			transformStart.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestTransformStart(
									ExplainContext.this.getDataSet(),
									ExplainContext.this.getDataSetTable(),
									table, transformStart.isSelected());
				}
			});
			transformStart.setEnabled(this.getDataSetTable() != null
					&& this.getDataSetTable().getType().equals(
							DataSetTableType.DIMENSION));
			transformStart.setSelected(this.getDataSetTable() != null
					&& table.isTransformStart(this.getDataSet(), this
							.getDataSetTable().getName()));
			contextMenu.add(transformStart);
		}*/
	}

	/**
	 * See {@link #populateContextMenu(JPopupMenu, Object)} but this applies to
	 * a particular relation iteration.
	 * 
	 * @param contextMenu
	 *            See {@link #populateContextMenu(JPopupMenu, Object)}.
	 * @param relation
	 *            the relation.
	 * @param iteration
	 *            the iteration of the relation, or
	 *            {@link RealisedRelation#NO_ITERATION} for all iterations.
	 */
	public void populateRelationContextMenu(final JPopupMenu contextMenu,
			final Relation relation, final int iteration) {

		// Add a separator if there's other stuff before us.
		if (contextMenu.getComponentCount() > 0)
			contextMenu.addSeparator();

		// Work out what state the relation is already in.
/*		final boolean alternativeJoined = this.getDataSetTable() != null
				&& relation.isAlternativeJoin(this.getDataSet(), this
						.getDataSetTable().getName());
		final boolean incorrect = relation.getStatus().equals(
				ComponentStatus.INFERRED_INCORRECT);
		final boolean relationMasked = this.getDataSetTable() == null ? relation
				.isMaskRelation(this.getDataSet())
				: relation.isMaskRelation(this.getDataSet(), this
						.getDataSetTable().getName());
		final boolean relationRestricted = this.getDataSetTable() != null
				&& relation.isRestrictRelation(this.getDataSet(), this
						.getDataSetTable().getName())
				&& (iteration == RealisedRelation.NO_ITERATION || relation
						.getRestrictRelation(this.getDataSet(), this
								.getDataSetTable().getName(), iteration) != null);
		final boolean relationSubclassed = relation.isSubclassRelation(this
				.getDataSet());
		final boolean relationCompounded = (this.getDataSetTable() == null ? relation
				.getCompoundRelation(this.getDataSet())
				: relation.getCompoundRelation(this.getDataSet(), this
						.getDataSetTable().getName())) != null;
		final boolean relationUnrolled = this.getDataSetTable() == null ? false
				: relation.getUnrolledRelation(this.getDataSet()) != null
						&& !this.getDataSetTable().getType().equals(
								DataSetTableType.DIMENSION);
		final boolean relationForced = this.getDataSetTable() == null ? relation
				.isForceRelation(this.getDataSet())
				: relation.isForceRelation(this.getDataSet(), this
						.getDataSetTable().getName());
		final boolean relationIncluded = this.getDataSetTable() == null ? this
				.getDataSet().getIncludedRelations().contains(relation) : this
				.getDataSetTable().getIncludedRelations().contains(relation);
		final boolean relationLoopbacked = (this.getDataSetTable() == null ? relation
				.getLoopbackRelation(this.getDataSet())
				: relation.getLoopbackRelation(this.getDataSet(), this
						.getDataSetTable().getName())) != null;

		// The mask/unmask option allows the user to mask/unmask a relation.
		final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(Resources
				.get("maskRelationTitle"));
		mask.setMnemonic(Resources.get("maskRelationMnemonic").charAt(0));
		mask.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this.getMartTab().getDataSetTabSet()
						.requestMaskRelation(ExplainContext.this.getDataSet(),
								ExplainContext.this.getDataSetTable(),
								relation, mask.isSelected());
			}
		});
		contextMenu.add(mask);
		if (incorrect || relationUnrolled || !relationMasked
				&& !relationIncluded)
			mask.setEnabled(false);
		if (relationMasked)
			mask.setSelected(true);

		// The mask/unmask option allows the user to alternative-join a
		// relation.
		final JCheckBoxMenuItem join = new JCheckBoxMenuItem(Resources
				.get("alternativeJoinTitle"));
		join.setMnemonic(Resources.get("alternativeJoinMnemonic").charAt(0));
		join.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this
						.getMartTab()
						.getDataSetTabSet()
						.requestAlternativeJoin(
								ExplainContext.this.getDataSet(),
								ExplainContext.this.getDataSetTable(),
								relation,
								ExplainContext.this.getDataSetTable().getType()
										.equals(DatasetTableType.DIMENSION) ? join
										.isSelected()
										: !join.isSelected());
			}
		});
		contextMenu.add(join);
		if (this.getDataSetTable() == null || incorrect || relationUnrolled
				|| relationMasked || !relationIncluded)
			join.setEnabled(false);
		join.setSelected(this.getDataSetTable() != null
				&& (this.getDataSetTable().getType().equals(
						DatasetTableType.DIMENSION) ? alternativeJoined
						: !alternativeJoined));

		// The loopback option allows the user to loopback include a relation
		// that would otherwise only be included once.
		final JCheckBoxMenuItem loopback = new JCheckBoxMenuItem(Resources
				.get("loopbackRelationTitle"));
		loopback.setMnemonic(Resources.get("loopbackRelationMnemonic")
				.charAt(0));
		loopback.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this
						.getMartTab()
						.getDataSetTabSet()
						.requestLoopbackRelation(
								ExplainContext.this.getDataSet(),
								ExplainContext.this.getDataSetTable(), relation);
			}
		});
		contextMenu.add(loopback);
		if (incorrect || relationUnrolled || relationMasked
				|| !relation.isOneToMany() && !relationLoopbacked)
			loopback.setEnabled(false);
		loopback.setSelected(relationLoopbacked);

		// The force option allows the user to forcibly include a relation
		// that would otherwise remain unincluded.
		final JCheckBoxMenuItem force = new JCheckBoxMenuItem(Resources
				.get("forceIncludeRelationTitle"));
		force.setMnemonic(Resources.get("forceIncludeRelationMnemonic").charAt(
				0));
		force.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this.getMartTab().getDataSetTabSet()
						.requestForceRelation(ExplainContext.this.getDataSet(),
								ExplainContext.this.getDataSetTable(),
								relation, force.isSelected());
			}
		});
		contextMenu.add(force);
		if (incorrect || relationUnrolled || relationMasked || relationIncluded
				&& !relationForced)
			force.setEnabled(false);
		if (relationForced)
			force.setSelected(true);

		// The compound option allows the user to compound a relation.
		final JCheckBoxMenuItem compound = new JCheckBoxMenuItem(Resources
				.get("compoundRelationTitle"));
		compound.setMnemonic(Resources.get("compoundRelationMnemonic")
				.charAt(0));
		compound.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this
						.getMartTab()
						.getDataSetTabSet()
						.requestCompoundRelation(
								ExplainContext.this.getDataSet(),
								ExplainContext.this.getDataSetTable(), relation);
				compound
						.setSelected((ExplainContext.this.getDataSetTable() == null ? relation
								.getCompoundRelation(ExplainContext.this
										.getDataSet())
								: relation.getCompoundRelation(
										ExplainContext.this.getDataSet(),
										ExplainContext.this.getDataSetTable()
												.getName())) != null);
			}
		});
		contextMenu.add(compound);
		if (incorrect || relationUnrolled || relationMasked
				|| this.getDataSet().isPartitionTable())
			compound.setEnabled(false);
		if (relationCompounded)
			compound.setSelected(true);

		// The subclass/unsubclass option allows subclassing, but is
		// only selectable when the relation is unmasked and not
		// incorrect or already flagged as being in any conflicting state.
		final JCheckBoxMenuItem subclass = new JCheckBoxMenuItem(Resources
				.get("subclassRelationTitle"));
		subclass.setMnemonic(Resources.get("subclassRelationMnemonic")
				.charAt(0));
		subclass.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this.getMartTab().getDataSetTabSet()
						.requestSubclassRelation(
								ExplainContext.this.getDataSet(), relation,
								subclass.isSelected());
			}
		});
		contextMenu.add(subclass);
		if (incorrect || relationUnrolled || relationMasked
				|| relation.isOneToOne() || this.getDataSetTable() != null)
			subclass.setEnabled(false);
		if (relationSubclassed)
			subclass.setSelected(true);

		contextMenu.addSeparator();

		// If it's a restricted relation...
		if (relationRestricted) {

			// Option to modify restriction.
			final JMenuItem modify = new JMenuItem(Resources
					.get("modifyRelationRestrictionTitle"), new ImageIcon(
					Resources.getResourceAsURL("filter.gif")));
			modify.setMnemonic(Resources.get(
					"modifyRelationRestrictionMnemonic").charAt(0));
			modify.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestRestrictRelation(
									ExplainContext.this.getDataSet(),
									ExplainContext.this.getDataSetTable(),
									relation, iteration);
				}
			});
			contextMenu.add(modify);
			if (incorrect || relationUnrolled || relationMasked
					|| this.getDataSetTable() == null)
				modify.setEnabled(false);

		} else {

			// Add a relation restriction.
			final JMenuItem restriction = new JMenuItem(Resources
					.get("addRelationRestrictionTitle"), new ImageIcon(
					Resources.getResourceAsURL("filter.gif")));
			restriction.setMnemonic(Resources.get(
					"addRelationRestrictionMnemonic").charAt(0));
			restriction.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					ExplainContext.this.getMartTab().getDataSetTabSet()
							.requestRestrictRelation(
									ExplainContext.this.getDataSet(),
									ExplainContext.this.getDataSetTable(),
									relation, iteration);
				}
			});
			if (incorrect || relationUnrolled || relationMasked
					|| this.getDataSetTable() == null)
				restriction.setEnabled(false);
			contextMenu.add(restriction);
		}

		// Option to remove restriction.
		final JMenuItem remove = new JMenuItem(Resources
				.get("removeRelationRestrictionTitle"));
		remove.setMnemonic(Resources.get("removeRelationRestrictionMnemonic")
				.charAt(0));
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				ExplainContext.this.getMartTab().getDataSetTabSet()
						.requestUnrestrictRelation(
								ExplainContext.this.getDataSet(),
								ExplainContext.this.getDataSetTable(),
								relation, iteration);
			}
		});
		contextMenu.add(remove);
		if (!relationRestricted)
			remove.setEnabled(false);
			*/
	}
}
