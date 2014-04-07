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

package org.biomart.configurator.view.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.view.gui.diagrams.Diagram;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.SourceSchema;

/**
 * A diagram component that represents a schema. It usually only has a label in
 * it, but if the schema has any external relations, then the tables with those
 * relations will appear in full using {@link TableComponent}s.
 * 
 */
public class SchemaComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

	/**
	 * Normal background colour.
	 */
	public static final Color BACKGROUND_COLOUR = Color.YELLOW;

	/**
	 * Background for masked schemas.
	 */
	public static Color MASKED_BACKGROUND = Color.LIGHT_GRAY;

	private static final Font BOLD_FONT = Font.decode("SansSerif-BOLD-10");

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	private final PropertyChangeListener repaintListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent e) {
			SchemaComponent.this.needsRepaint = true;
		}
	};

	private final PropertyChangeListener recalcListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent e) {
			SchemaComponent.this.needsRecalc = true;
		}
	};

	/**
	 * Constructs a schema diagram component in the given diagram that displays
	 * details of a particular schema.
	 * 
	 * @param schema
	 *            the schema to display details of.
	 * @param diagram
	 *            the diagram to display the details in.
	 */
	public SchemaComponent(final SourceSchema schema, final Diagram diagram) {
		super(schema, diagram);

		// Schema components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each part of the schema component.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(5, 5, 5, 5);

		// Set the background colour.
		this.setBackground(SchemaComponent.BACKGROUND_COLOUR);

		// Calculate the components and add them to the list.
		this.recalculateDiagramComponent();

	}

	private SourceSchema getSchema() {
		return (SourceSchema) this.getObject();
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	public JPopupMenu getContextMenu() {
		// First of all, work out what would have been shown by default.
		final JPopupMenu contextMenu = super.getContextMenu();

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
	}

	protected void doRecalculateDiagramComponent() {
		// Clear subcomponents.
		this.getSubComponents().clear();

		// Add the label for the schema name,
		final JTextField name = new JTextField() {
			private static final long serialVersionUID = 1L;

			private Color opaqueBackground;

			// work around transparency issue in OS X 10.5
			public void setOpaque(boolean opaque) {
				if (opaque != isOpaque()) {
					if (opaque) {
						super.setBackground(opaqueBackground);
					} else if (opaqueBackground != null) {
						opaqueBackground = getBackground();
						super.setBackground(TRANSPARENT_COLOR);
					}
				}
				super.setOpaque(opaque);
			}

			// work around transparency issue in OS X 10.5
			public void setBackground(Color color) {
				if (isOpaque()) {
					super.setBackground(color);
				} else {
					opaqueBackground = color;
				}
			}
		};
		name.setFont(SchemaComponent.BOLD_FONT);
		this.setRenameTextField(name);
		this.layout.setConstraints(name, this.constraints);
		this.add(name);

	}



	public String getEditableName() {
		return this.getSchema().getName();
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}


}
