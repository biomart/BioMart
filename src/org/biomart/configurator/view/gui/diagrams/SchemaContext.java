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

package org.biomart.configurator.view.gui.diagrams;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.type.ComponentStatus;
import org.biomart.configurator.view.component.ColumnComponent;
import org.biomart.configurator.view.component.KeyComponent;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.SchemaComponent;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.configurator.view.menu.DiagramMenuConstructor;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.Table;

/**
 * Provides the context menus and colour schemes to use when viewing a schema in
 * its plain vanilla form, ie. not a dataset schema, and not a window from a
 * dataset onto a set of masked relations.

 */
public class SchemaContext implements DiagramContext {

	protected JDialog parent;
	/**
	 * Creates a new context which will pass any menu actions onto the given
	 * mart tab.
	 * 
	 * @param martTab
	 *            the mart tab which will receive any menu actions the user
	 *            selects.
	 */
	public SchemaContext(JDialog parent) {
		this.parent = parent;
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {
		// This bit updates schema boxes.
		if (object instanceof SourceSchema) {
			final SourceSchema schema = (SourceSchema) object;
			final SchemaComponent schcomp = (SchemaComponent) component;
			if (this.isMasked(schema))
				schcomp.setBackground(SchemaComponent.MASKED_BACKGROUND);
			else
				schcomp.setBackground(SchemaComponent.BACKGROUND_COLOUR);
		}

		// This bit removes a restricted outline from any restricted tables.
		else if (object instanceof Table) {
			final TableComponent tblcomp = (TableComponent) component;
			final Table table = (Table) object;
			tblcomp.setRestricted(false);

			// Fade out all ignored tables.
			if (this.isMasked(table))
				tblcomp.setBackground(TableComponent.IGNORE_COLOUR);

			// All others are normal.
			else
				tblcomp.setBackground(TableComponent.BACKGROUND_COLOUR);
		}

		// This bit removes a restricted outline from any restricted tables.
		else if (object instanceof Column) {
			final ColumnComponent colcomp = (ColumnComponent) component;
			final Column col = (Column) object;

			// Fade out all ignored tables.
			if (this.isMasked(col))
				colcomp.setBackground(ColumnComponent.MASKED_COLOUR);

			// All others are normal.
			else
				colcomp.setBackground(ColumnComponent.NORMAL_COLOUR);
		}

		// Relations get pretty colours if they are incorrect or handmade.
		else if (object instanceof Relation) {

			// What relation is this?
			final Relation relation = (Relation) object;
			final RelationComponent relcomp = (RelationComponent) component;

			// Is it restricted?
			relcomp.setRestricted(false);

			// Is it compounded?
			relcomp.setCompounded(false);

			// Is it loopback?
			relcomp.setLoopback(false);

			// Fade out all INFERRED_INCORRECT relations and those which
			// head to ignored tables.
			if (this.isMasked(relation))
				relcomp.setForeground(RelationComponent.MASKED_COLOUR);
			
			// Highlight all HANDMADE relations.
			else if (relation.getStatus().equals(ComponentStatus.HANDMADE))
				relcomp.setForeground(RelationComponent.HANDMADE_COLOUR);

			// Highlight MODIFIED relations.
			else if (relation.getStatus().equals(ComponentStatus.MODIFIED))
				relcomp.setForeground(RelationComponent.MODIFIED_COLOUR);

			// All others are normal.
			else
				relcomp.setForeground(RelationComponent.NORMAL_COLOUR);
		}

		// Keys also get pretty colours for being incorrect or handmade.
		else if (object instanceof Key) {

			// What key is this?
			final Key key = (Key) object;
			final KeyComponent keycomp = (KeyComponent) component;

			// Fade out all INFERRED_INCORRECT relations.
			if (ComponentStatus.INFERRED_INCORRECT.equals(key.getStatus()))
				keycomp.setForeground(KeyComponent.INCORRECT_COLOUR);

			// Highlight all HANDMADE relations.
			else if (ComponentStatus.HANDMADE.equals(key.getStatus()))
				keycomp.setForeground(KeyComponent.HANDMADE_COLOUR);

			// All others are normal.
			else
				keycomp.setForeground(KeyComponent.NORMAL_COLOUR);

			// Add drag-and-drop to all keys here.
			keycomp.setDraggable(true);
		}
	}

	public boolean isMasked(final Object object) {
		if(object instanceof MartConfiguratorObject)
			return ((MartConfiguratorObject)object).isHidden();
		return false;
	}

	public void populateMultiContextMenu(final JPopupMenu contextMenu,
			final Collection selectedItems, final Class clazz) {
		// Nothing to do here.
	}

	public void populateContextMenu(final JPopupMenu contextMenu, JComponent component, 
			final Object object) {
		JPopupMenu popupMenu = DiagramMenuConstructor.getInstance().getContextMenu(parent, component, object);
		if(popupMenu!=null)
			for(Component c: popupMenu.getComponents())
				contextMenu.add(c);
		
	}
}
