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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.biomart.configurator.view.component.DiagramComponent;
import org.biomart.configurator.view.component.KeyComponent;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.SchemaComponent;


/**
 * This layout manager lays out components in rows of a square block.
 * 
 */
public class SchemaLayoutManager implements LayoutManager2 {
	private static final int RELATION_SPACING = 5; // 72 = 1 inch

	private static final int TABLE_PADDING = 10; // 72 = 1 inch

	private Dimension size;

	private boolean sizeKnown;

	private final Map<Component,Dimension> prefSizes = new HashMap<Component,Dimension>();

	private final Map<Component,Object> constraints = new HashMap<Component,Object>();

	private final List<List<Component>> rows = new ArrayList<List<Component>>();

	private final List<RelationComponent> relations = new ArrayList<RelationComponent>();

	private final List<Integer> rowHeights = new ArrayList<Integer>();

	private final List<Integer> rowWidths = new ArrayList<Integer>();

	private final List<Integer> rowSpacings = new ArrayList<Integer>();

	private int tableCount;

	private final Collection<Component> fixedComps = new HashSet<Component>();

	/**
	 * Sets up some defaults for the layout, ready for use.
	 */
	public SchemaLayoutManager() {
		this.sizeKnown = true;
		this.size = new Dimension(0, 0);
		this.tableCount = 0;
	}

	public float getLayoutAlignmentX(final Container target) {
		return 0.5f;
	}

	public float getLayoutAlignmentY(final Container target) {
		return 0.5f;
	}

	public void invalidateLayout(final Container target) {
		this.sizeKnown = false;
	}

	public void addLayoutComponent(final String name, final Component comp) {
		this.addLayoutComponent(comp, null);
	}

	public Dimension maximumLayoutSize(final Container target) {
		return this.minimumLayoutSize(target);
	}

	public Dimension preferredLayoutSize(final Container parent) {
		return this.minimumLayoutSize(parent);
	}

	public Dimension minimumLayoutSize(final Container parent) {
		// Work out how big we are.
		this.calculateSize(parent);
		synchronized (parent.getTreeLock()) {

			// Work out our parent's insets.
			final Insets insets = parent.getInsets();

			// The minimum size is our size plus our
			// parent's insets size.
			final Dimension dim = new Dimension(0, 0);
			dim.width = this.size.width + insets.left + insets.right;
			dim.height = this.size.height + insets.top + insets.bottom;

			// That's it!
			return dim;
		}
	}

	private void calculateSize(final Container parent) {
		synchronized (parent.getTreeLock()) {
			if (this.sizeKnown)
				return;

			// Assumption that we are laying out a diagram.
			final Dimension maskedButton = ((Diagram) parent)
					.getHideMaskedArea();

			this.size.height = maskedButton.height;
			this.size.width = maskedButton.width;
			this.prefSizes.clear();

			for (int rowNum = 0; rowNum < this.rows.size(); rowNum++) {
				final List<Component> row = this.rows.get(rowNum);

				int rowHeight = 0;
				int rowWidth = 0;
				int rowSpacing = 0;

				// For each component, allow space plus padding either
				// side equivalent to the number of relations from that
				// component.
				for (final Iterator<Component> i = row.iterator(); i.hasNext();) {
					final Component comp = i.next();
					final Dimension prefSize = comp.getPreferredSize();
					this.prefSizes.put(comp, prefSize);
					final int compSpacing = ((SchemaLayoutConstraint) this.constraints
							.get(comp)).getRelCount()
							* SchemaLayoutManager.RELATION_SPACING;
					rowHeight = Math.max(rowHeight, prefSize.height
							+ compSpacing * 2);
					rowWidth += prefSize.width + compSpacing * 2;
					rowSpacing = Math.max(rowSpacing, compSpacing);
				}

				this.rowSpacings.set(rowNum, new Integer(rowSpacing));

				// Add a bit of padding above and below each row.
				rowHeight += SchemaLayoutManager.TABLE_PADDING * 2;
				this.rowHeights.set(rowNum, new Integer(rowHeight));
				this.size.height += rowHeight;

				// Add a bit of padding at each end of each row.
				rowWidth += (row.size() + 1)
						* SchemaLayoutManager.TABLE_PADDING * 2;
				this.rowWidths.set(rowNum, new Integer(rowWidth));
				this.size.width = Math.max(rowWidth, this.size.width);
			}

			this.sizeKnown = true;
		}
	}

	public void addLayoutComponent(final Component comp,
			final Object constraints) {
		synchronized (comp.getTreeLock()) {
			if (comp instanceof RelationComponent) {
				this.relations.add((RelationComponent)comp);
				this.constraints.put(comp, constraints);
			} else if (comp instanceof DiagramComponent
					&& constraints instanceof SchemaLayoutConstraint) {

				this.constraints.put(comp, constraints);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.put(comp, prefSize);

				// Work out how many components per row we need to make
				// a square, and therefore which row has space on to
				// put this component.
				final int rowLength = (int) Math.ceil(Math
						.sqrt(++this.tableCount));
				int rowNum = 0;
				while (rowNum < this.rows.size()
						&&  this.rows.get(rowNum).size() >= rowLength)
					rowNum++;
				((SchemaLayoutConstraint) constraints).setRow(rowNum);

				// Ensure arrays are large enough.
				while (rowNum >= this.rows.size()) {
					this.rowSpacings.add(new Integer(0));
					this.rowHeights.add(new Integer(0));
					this.rowWidths.add(new Integer(0));
					this.rows.add(new ArrayList<Component>());
				}

				this.rows.get(rowNum).add(comp);

				// The component needs space for its relations.
				final int compSpacing = SchemaLayoutManager.RELATION_SPACING
						* ((SchemaLayoutConstraint) constraints).getRelCount();

				// Update the row to accommodate the new component.
				final int oldRowWidth = ((Integer) this.rowWidths.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth += prefSize.width
						+ SchemaLayoutManager.TABLE_PADDING * 2 + compSpacing
						* 2;
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				// Update the row height if the new component plus spacing
				// is higher than the old row.
				final int oldRowHeight = ((Integer) this.rowHeights.get(rowNum))
						.intValue();
				final int newRowHeight = Math.max(oldRowHeight, prefSize.height
						+ SchemaLayoutManager.TABLE_PADDING * 2)
						+ compSpacing * 2;
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

				// Increase the space to the next row to accommodate the
				// relations from this new component, if necessary.
				this.rowSpacings.set(rowNum, new Integer(Math.max(
						((Integer) this.rowSpacings.get(rowNum)).intValue(),
						compSpacing)));

				this.size.height += newRowHeight - oldRowHeight;
				this.size.width = Math.max(this.size.width, newRowWidth);
			} else
				this.fixedComps.add(comp);
		}
	}

	public void removeLayoutComponent(final Component comp) {
		synchronized (comp.getTreeLock()) {
			if (this.fixedComps.contains(comp))
				this.fixedComps.remove(comp);
			else if (comp instanceof RelationComponent) {
				this.relations.remove(comp);
				this.constraints.remove(comp);
			} else {
				final SchemaLayoutConstraint constraints = (SchemaLayoutConstraint) this.constraints
						.remove(comp);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.remove(comp);

				// How much padding was this component given?
				final int compSpacing = SchemaLayoutManager.RELATION_SPACING
						* constraints.getRelCount();

				this.tableCount--;
				final int rowNum = constraints.getRow();

				this.rows.get(rowNum).remove(comp);

				// Reduce the row width and height accordingly.
				final int oldRowWidth = ((Integer) this.rowWidths.get(rowNum))
						.intValue();
				final int oldRowHeight = ((Integer) this.rowHeights.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth -= prefSize.width
						+ SchemaLayoutManager.TABLE_PADDING * 2 + compSpacing
						* 2;
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				int newRowHeight = 0;
				for (final Iterator<Component> i =  this.rows.get(rowNum)
						.iterator(); i.hasNext();)
					newRowHeight = Math.max(newRowHeight,
							i.next().getPreferredSize().height
									+ compSpacing * 2);
				newRowHeight += SchemaLayoutManager.TABLE_PADDING * 2;
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

				this.size.height -= oldRowHeight - newRowHeight;

				// While last row is empty, remove last row.
				int lastRow = this.rows.size() - 1;
				while (lastRow >= 0 && this.rows.get(lastRow) == null
						&& this.rows.get(lastRow).isEmpty()) {
					// Remove all references to empty row.
					this.rows.remove(lastRow);
					this.rowHeights.remove(lastRow);
					this.rowSpacings.remove(lastRow);
					this.rowWidths.remove(lastRow);
					// Update last row pointer.
					lastRow--;
				}

				// New width needs re-calculating from all rows.
				this.size.width = 0;
				for (final Iterator<Integer> i = this.rowWidths.iterator(); i.hasNext();)
					this.size.width = Math.max(i.next().intValue(),
							this.size.width);
			}
		}
	}

	public void layoutContainer(final Container parent) {
		// Work out how big we are.
		this.calculateSize(parent);
		synchronized (parent.getTreeLock()) {

			// Fixed components are ignored. The parent should lay
			// them out.
			// Assumption that we are laying out a diagram.
			final Dimension maskedButton = ((Diagram) parent)
					.getHideMaskedArea();

			int nextY = SchemaLayoutManager.TABLE_PADDING + maskedButton.height;

			// Layout each row in turn.
			for (int rowNum = 0; rowNum < this.rows.size(); rowNum++) {
				int x = SchemaLayoutManager.TABLE_PADDING;
				final int y = nextY
						+ ((Integer) this.rowHeights.get(rowNum)).intValue()
						- SchemaLayoutManager.TABLE_PADDING * 2
						- ((Integer) this.rowSpacings.get(rowNum)).intValue();

				// Layout each component in the row.
				for (final Iterator<Component> i =  this.rows.get(rowNum)
						.iterator(); i.hasNext();) {
					final Component comp = (Component) i.next();
					final Dimension prefSize = (Dimension) this.prefSizes
							.get(comp);
					final int compSpacing = ((SchemaLayoutConstraint) this.constraints
							.get(comp)).getRelCount()
							* SchemaLayoutManager.RELATION_SPACING;
					x += compSpacing;
					comp.setBounds(x, y - prefSize.height, prefSize.width,
							prefSize.height);
					comp.validate();
					x += prefSize.width + SchemaLayoutManager.TABLE_PADDING * 2
							+ compSpacing;
				}
				nextY += ((Integer) this.rowHeights.get(rowNum)).intValue();
			}

			// Work out how the relations are going to join things up.
			for (final Iterator<RelationComponent> i = this.relations.iterator(); i.hasNext();) {
				final RelationComponent comp =  i.next();
				// Obtain first key and work out position relative to
				// diagram.
				int firstRowNum = 0;
				if (this.rowHeights.size()==0)
					continue;
				int firstRowBottom = ((Integer) this.rowHeights
						.get(firstRowNum)).intValue();
				final KeyComponent firstKey = comp.getFirstKeyComponent();
				if (firstKey == null)
					continue;
				Rectangle firstKeyRectangle = firstKey.getBounds();
				int firstKeyInsetX = firstKeyRectangle.x;
				firstKeyRectangle = SwingUtilities.convertRectangle(firstKey
						.getParent(), firstKeyRectangle, parent);
				if (firstKey.getParent() == null || !firstKey.getParent().isValid()) 
					continue;
				if (firstKey.getParent().getParent() instanceof SchemaComponent)
					firstKeyInsetX += firstKey.getParent().getBounds().x;
				while (firstKeyRectangle.y >= firstRowBottom
						&& firstRowNum < this.rows.size() - 1)
					firstRowBottom += ((Integer) this.rowHeights
							.get(++firstRowNum)).intValue();

				// Do the same for the second key.
				int secondRowNum = 0;
				if (this.rowHeights.size()==0)
					continue;
				int secondRowBottom = ((Integer) this.rowHeights
						.get(secondRowNum)).intValue();
				final KeyComponent secondKey = comp.getSecondKeyComponent();
				if (secondKey == null)
					continue;
				Rectangle secondKeyRectangle = secondKey.getBounds();
				int secondKeyInsetX = secondKeyRectangle.x;
				secondKeyRectangle = SwingUtilities.convertRectangle(secondKey
						.getParent(), secondKeyRectangle, parent);
				if (secondKey.getParent() == null || !secondKey.getParent().isValid()) 
					continue;
				if (secondKey.getParent().getParent() instanceof SchemaComponent)
					secondKeyInsetX += secondKey.getParent().getBounds().x;
				while (secondKeyRectangle.y >= secondRowBottom
						&& secondRowNum < this.rows.size() - 1)
					secondRowBottom += ((Integer) this.rowHeights
							.get(++secondRowNum)).intValue();

				// Work out left/right most.
				final Rectangle leftKeyRectangle = firstKeyRectangle.x <= secondKeyRectangle.x ? firstKeyRectangle
						: secondKeyRectangle;
				final Rectangle rightKeyRectangle = firstKeyRectangle.x > secondKeyRectangle.x ? firstKeyRectangle
						: secondKeyRectangle;
				final int leftKeyInsetX = leftKeyRectangle == firstKeyRectangle ? firstKeyInsetX
						: secondKeyInsetX;
				final int rightKeyInsetX = rightKeyRectangle == firstKeyRectangle ? firstKeyInsetX
						: secondKeyInsetX;

				// Work out Y coord for top of relation.
				final int relTopY = (int) Math.min(leftKeyRectangle
						.getCenterY(), rightKeyRectangle.getCenterY());
				int relBottomY, relLeftX, relRightX;
				int leftX, rightX, leftY, rightY, viaX, viaY;
				int leftTagX, rightTagX;

				// Both at same X location?
				if (Math.abs(firstKeyRectangle.x - secondKeyRectangle.x) < 100) {
					relBottomY = (int) Math.max(leftKeyRectangle.getCenterY(),
							rightKeyRectangle.getCenterY());
					relLeftX = leftKeyRectangle.x
							- SchemaLayoutManager.TABLE_PADDING;
					relRightX = rightKeyRectangle.x;

					leftX = leftKeyRectangle.x - leftKeyInsetX;
					leftTagX = leftX - SchemaLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = rightKeyRectangle.x - rightKeyInsetX;
					rightTagX = rightX - SchemaLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = leftX - SchemaLayoutManager.TABLE_PADDING * 2;
					viaY = (leftY + rightY) / 2;
				} else {
					relRightX = (int) Math.max(leftKeyRectangle.getMaxX(),
							rightKeyRectangle.x);
					relLeftX = (int) Math.min(leftKeyRectangle.getMaxX(),
							rightKeyRectangle.x);
					relBottomY = Math.max(firstRowBottom, secondRowBottom);

					leftX = (int) leftKeyRectangle.getMaxX() + leftKeyInsetX;
					leftTagX = leftX + SchemaLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = rightKeyRectangle.x - rightKeyInsetX;
					rightTagX = rightX - SchemaLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = (leftX + rightX) / 2;
					if (Math.abs(rightX - leftX) < 100)
						viaY = (leftY + rightY) / 2;
					else if (Math.abs(rightY - leftY) > 100)
						viaY = (relBottomY + relTopY) / 2;
					else
						viaY = relTopY + (int) ((relBottomY - relTopY) * 1.8);
				}

				// Set overall bounds.
				final Rectangle bounds = new Rectangle(
						(Math.min(relLeftX, viaX) - SchemaLayoutManager.RELATION_SPACING * 4),
						(Math.min(relTopY, viaY) - SchemaLayoutManager.RELATION_SPACING * 4),
						(Math.abs(Math.max(relRightX, viaX)
								- Math.min(relLeftX, viaX)) + SchemaLayoutManager.RELATION_SPACING * 8),
						(Math.abs(Math.max(relBottomY, viaY)
								- Math.min(relTopY, viaY)) + SchemaLayoutManager.RELATION_SPACING * 8));
				comp.setBounds(bounds);

				// Create a path to describe the relation shape. It
				// will have 2 components to it - move, curve.
				final GeneralPath path = new GeneralPath(
						GeneralPath.WIND_EVEN_ODD, 4);

				// Move to starting point at primary key.
				path.moveTo(leftX - bounds.x, leftY - bounds.y);

				// Left tag.
				path.lineTo(leftTagX - bounds.x, leftY - bounds.y);

				// Draw from the first key midpoint across to the vertical
				// track.
				path.quadTo(viaX - bounds.x, viaY - bounds.y, rightTagX
						- bounds.x, rightY - bounds.y);

				// Right tag.
				path.lineTo(rightX - bounds.x, rightY - bounds.y);

				// Set the shape.
				comp.setLineShape(path);
			}
		}
	}

	/**
	 * Use this constraint to indicate to the layout manager how much spacing to
	 * give each component.
	 */
	public static class SchemaLayoutConstraint {

		private final int relCount;

		private int row;

		/**
		 * Construct a new constraint indicating that the given number of
		 * relations lead off this component, so that space is left for them.
		 * Or, if the component is a relation, this indicates the index off the
		 * table that this relation is so that it bends out of the way of other
		 * relations accordingly.
		 * 
		 * @param relCount
		 *            the number of relations from this component.
		 */
		public SchemaLayoutConstraint(final int relCount) {
			this.relCount = relCount;
			this.row = 0;
		}

		private int getRelCount() {
			return this.relCount;
		}

		private void setRow(final int row) {
			this.row = row;
		}

		private int getRow() {
			return this.row;
		}
	}
}
