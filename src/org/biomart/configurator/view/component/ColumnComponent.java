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

package org.biomart.configurator.view.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JTextField;

import org.biomart.configurator.view.gui.diagrams.Diagram;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;


/**
 * This simple component represents a single column within a table.
 * 
 */
public class ColumnComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

	/**
	 * Constant referring to expression column background colour.
	 */
	public static Color EXPRESSION_COLOUR = Color.MAGENTA;

	/**
	 * Constant referring to masked column background colour.
	 */
	public static Color MASKED_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to inherited column background colour.
	 */
	public static Color INHERITED_COLOUR = Color.RED;

	/**
	 * Constant referring to normal column border colour.
	 */
	public static Color NORMAL_FG_COLOUR = Color.BLACK;

	private static final Font NORMAL_FONT = Font.decode("SansSerif-PLAIN-10");

	/**
	 * Constant referring to normal column background colour.
	 */
	public static Color NORMAL_COLOUR = Color.ORANGE;

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	private final PropertyChangeListener repaintListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent e) {
			ColumnComponent.this.needsRepaint = !ColumnComponent.this
					.getDiagram().isNeedsRepaint();
		}
	};

	private final PropertyChangeListener recalcListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent e) {
			ColumnComponent.this.needsRepaint = !ColumnComponent.this
					.getDiagram().isNeedsRecalc();
		}
	};

	/**
	 * The constructor creates a new column component representing the given
	 * column. The diagram the column component is part of is also required.
	 * 
	 * @param column
	 *            the column to represent graphically.
	 * @param diagram
	 *            the diagram to display it in.
	 */
	public ColumnComponent(final Column column, final Diagram diagram) {
		super(column, diagram);
		// Column components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each label within the column.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(0, 1, 0, 2);

		// Set the background colour.
		this.setBackground(ColumnComponent.NORMAL_COLOUR);
		this.setForeground(ColumnComponent.NORMAL_FG_COLOUR);

		// Calculate the diagram.
		this.recalculateDiagramComponent();
	}

	private Column getColumn() {
		return (Column)this.getObject();
	}

	protected void doRecalculateDiagramComponent() {
		// Add the label for the column name.
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
		name.setFont(ColumnComponent.NORMAL_FONT);
		this.setRenameTextField(name);
		this.layout.setConstraints(name, this.constraints);
		this.add(name);
		// Tooltip indicating source of column.
		Column col = this.getColumn();
/*		while (col instanceof InheritedColumn)
			col = ((InheritedColumn) col).getInheritedColumn();
		if (col instanceof WrappedColumn) {
			final Column wcol = ((WrappedColumn) col).getWrappedColumn();
			final String tooltip = wcol.getTable().getSchema().getName() + "."
					+ wcol.getTable().getName() + "." + wcol.getName();
			this.setToolTipText(tooltip);
			name.setToolTipText(tooltip);
		}*/
	}



	public String getEditableName() {
		return this.getColumn() instanceof DatasetColumn ? ((DatasetColumn) this
				.getColumn()).getName()
				: this.getColumn().getName();
	}

	public String getDisplayName() {
		return this.getEditableName();
	}
}
