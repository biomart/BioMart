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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JDialog;

import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.Table;
import org.biomart.configurator.view.gui.diagrams.SchemaLayoutManager.SchemaLayoutConstraint;

/**
 * Displays the contents of a schema within a diagram object. It adds a series
 * of {@link TableComponent} and {@link RelationComponent} objects when the
 * diagram is recalculated, and treats the schema object it represents as the
 * basic background object of the diagram.
 * 
 */
public class SchemaDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private SourceSchema schema;
	private Mart mart;
	private JDialog parent;

	/**
	 * Creates a new diagram that displays the tables and relations inside a
	 * specific schema.
	 * 
	 * @param layout
	 *            the layout manager to use to display the diagram.
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param schema
	 *            the schema to draw in this diagram.
	 */
	public SchemaDiagram(final JDialog parent, final Mart mart) {
		// Call the general diagram constructor first.
		super(new SchemaLayoutManager());
		this.parent = parent;
		// Remember the schema, then lay it out.
		this.schema = mart.getIncludedSchemas().get(0);
		this.mart = mart;
		this.setDiagramContext(new SchemaContext(parent));
		this.recalculateDiagram();
	}




	public void doRecalculateDiagram() {
		// Add a TableComponent for each table in the schema.
		final Set<Relation> usedRels = new HashSet<Relation>();
		for (final Iterator<Table> i = this.schema.getTables()
				.iterator(); i.hasNext();) {
			final Table t = (Table) i.next();
			final Collection<Relation> tRels = new HashSet<Relation>();
			int indent = 0;
			for (final Iterator<Relation> j = t.getRelations().iterator(); j.hasNext();) {
				final Relation rel = (Relation) j.next();
				if (!usedRels.contains(rel)) {
					tRels.add(rel);
					this.add(new RelationComponent(rel, this),
							new SchemaLayoutConstraint(indent++),
							Diagram.RELATION_LAYER);
					usedRels.add(rel);
				}
			}
			this.add(new TableComponent(t, this), new SchemaLayoutConstraint(
					tRels.size()), Diagram.TABLE_LAYER);
		}
	}

	/**
	 * Returns the schema that this diagram represents.
	 * 
	 * @return the schema this diagram represents.
	 */
	public SourceSchema getSchema() {
		return this.schema;
	}
}
