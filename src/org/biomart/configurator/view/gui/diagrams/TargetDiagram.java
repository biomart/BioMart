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

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;

import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.configurator.view.gui.diagrams.TargetLayoutManager.TargetLayoutConstraint;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Relation;


public class TargetDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private Mart mart;


	private final PropertyChangeListener listener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent evt) {
			TargetDiagram.this.needsRecalc = true;
		}
	};


	/**
	 * Creates a new diagram that displays the tables and relations inside a
	 * specific dataset.
	 * 
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param mart
	 *            the dataset to draw in this diagram.
	 */
	public TargetDiagram(JDialog owner, final Mart mart) {
		// Call the general diagram constructor first.
		super(new TargetLayoutManager());
		// Set up our background colour.
		this.setBackground(Diagram.BACKGROUND_COLOUR);

		// Remember the schema, then lay it out.
		this.mart = mart;
		this.setDiagramContext(new TargetDiagramContext(owner, mart));
		this.recalculateDiagram();
	}


	public void doRecalculateDiagram() {
		// Skip if can't get main table.
		if (this.mart.getMainTable() == null)
			return;
		// Add stuff.
		final List<DatasetTable> mainTables = new ArrayList<DatasetTable>();
		mainTables.add(this.mart.getMainTable());
		for (int i = 0; i < mainTables.size(); i++) {
			final DatasetTable table =  mainTables.get(i);
			// Create constraint.
			final TargetLayoutConstraint constraint = new TargetLayoutConstraint(
					TargetLayoutConstraint.MAIN, i);
			// Add main table.
			this.add(new TableComponent(table, this), constraint,
					Diagram.TABLE_LAYER);
			// Add dimension tables.
			if (table.getPrimaryKey() != null)
				for (final Iterator<Relation> r = table.getPrimaryKey().getRelations()
						.iterator(); r.hasNext();) {
					final Relation relation =  r.next();
					Key manyKey = relation.getManyKey();
					if(null==manyKey)
						continue;
					final DatasetTable target = (DatasetTable) manyKey.getTable();
					if (target.getType().equals(DatasetTableType.DIMENSION)) {
						if(target.isHidden() && this.isHideMasked()) {
							continue;
						}
						// Create constraint.
						final TargetLayoutConstraint dimConstraint = new TargetLayoutConstraint(
								TargetLayoutConstraint.DIMENSION, i);
						// Add dimension table.
						this.add(new TableComponent(target, this),
								dimConstraint, Diagram.TABLE_LAYER);
					} else
						mainTables.add(target);
					// Add relation.
					this.add(new RelationComponent(relation, this),
							Diagram.RELATION_LAYER);
				}
		}
	}

	/**
	 * Returns the dataset that this diagram represents.
	 * 
	 * @return the dataset this diagram represents.
	 */
	public Mart getDataSet() {
		return this.mart;
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() >= 2) {
/*			final int index = DataSetComponent.this.getDiagram().getMartTab()
					.getDataSetTabSet().indexOfTab(
							DataSetComponent.this.getDataSet().getName());
			DataSetComponent.this.getDiagram().getMartTab().getDataSetTabSet()
					.setSelectedIndex(index);
			// Mark as handled.
			eventProcessed = true;*/
		}
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}
		

}
