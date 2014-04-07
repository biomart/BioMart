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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.configurator.view.gui.dialogs.PropertyDialog;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.view.component.ColumnComponent;
import org.biomart.configurator.view.component.KeyComponent;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.configurator.view.gui.dialogs.ExplainTableDialog;
import org.biomart.configurator.view.menu.DiagramMenuConstructor;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.Relation;
import org.biomart.configurator.controller.InheritedColum;

/**
 * This context adapts dataset diagrams to display different colours, and
 * provides the context menu for interacting with dataset diagrams.
 * 
 */
public class TargetDiagramContext extends SchemaContext {
	private final Mart mart;
	/**
	 * Creates a new context that will adapt database objects according to the
	 * settings in the specified dataset.
	 * 
	 * @param martTab
	 *            the mart tab this context appears in.
	 * @param mart
	 *            the dataset this context will use for customising menus and
	 *            colours.
	 */

	public TargetDiagramContext(JDialog owner, final Mart mart) {
		super(owner);
		this.mart = mart;
	}

	/**
	 * Obtain the dataset that this context is linked with.
	 * 
	 * @return our dataset.
	 */
	protected Mart getDataSet() {
		return this.mart;
	}

	public void customiseAppearance(final JComponent component,
			final Object object) {

		// Is it a relation?
		if (object instanceof Relation) {

			// Which relation is it?
			final Relation relation = (Relation) object;
			final RelationComponent relcomp = (RelationComponent) component;

			// What tables does it link?
			final DatasetTable target = (DatasetTable) relation.getManyKey()
					.getTable();

			// Is it compounded?
			// Fade MASKED DIMENSION relations.
			if (target.getType().equals(DatasetTableType.MAIN_SUBCLASS))
				relcomp.setForeground(RelationComponent.SUBCLASS_COLOUR);

			// All the rest are normal.
			else
				relcomp.setForeground(RelationComponent.NORMAL_COLOUR);
		}

		// Is it a table?
		else if (object instanceof DatasetTable) {

			// Which table is it?
			final TableComponent tblcomp = (TableComponent) component;
			final DatasetTable tbl = (DatasetTable) object;
			final DatasetTableType tableType = tbl.getType();

			if(((DatasetTable) object).isHidden()) {
				tblcomp.setBackground(TableComponent.MASKED_COLOUR);
			}
			// Highlight DIMENSION tables.
			else if (tableType.equals(DatasetTableType.DIMENSION)) {
				tblcomp.setBackground(TableComponent.BACKGROUND_COLOUR);
			}

			else
				tblcomp.setBackground(TableComponent.BACKGROUND_COLOUR);

			if(tblcomp.getTable().hasSubPartition()) {
				tblcomp.setCompounded(true);
			}
			tblcomp.setRenameable(true);
			tblcomp.setSelectable(true);
		}

		// Columns.
		else if (object instanceof DatasetColumn) {

			// Which column is it?
			final DatasetColumn column = (DatasetColumn) object;
			final ColumnComponent colcomp = (ColumnComponent) component;

			// Fade out all MASKED columns.
			if (this.isMasked(column))
				colcomp.setBackground(ColumnComponent.MASKED_COLOUR);
			// Red INHERITED columns.
			else if (column instanceof InheritedColum)
				colcomp.setBackground(ColumnComponent.INHERITED_COLOUR);
			else
				colcomp.setBackground(ColumnComponent.NORMAL_COLOUR);

			colcomp.setIndexed(false);

			colcomp.setRenameable(true);
			colcomp.setSelectable(true);
		}

		// Keys
		else if (object instanceof Key) {
			final KeyComponent keycomp = (KeyComponent) component;

			keycomp.setIndexed(true);

			// Remove drag-and-drop from the key as it does not apply in
			// the window context.
			keycomp.setDraggable(false);
		}
	}

	public void populateMultiContextMenu(final JPopupMenu contextMenu,
			final Collection selectedItems, final Class clazz) {

		
	}

	public boolean isMasked(final Object object) {
		if(object instanceof MartConfiguratorObject)
			return ((MartConfiguratorObject)object).isHidden();
		return false;
	}

	public void populateContextMenu(final JPopupMenu contextMenu, JComponent component, 
			final Object object) {
		JPopupMenu popupMenu = DiagramMenuConstructor.getInstance().getContextMenu(this.parent, component, object);
		if(popupMenu!=null)
			for(Component c: popupMenu.getComponents())
				contextMenu.add(c);
	}
}
