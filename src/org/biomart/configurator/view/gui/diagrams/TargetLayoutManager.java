
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


/**
 * This layout manager lays out components in rows, grouped by the main dataset
 * table they are associated with. The main table itself is always first on each
 * row.
 * 
 */
public class TargetLayoutManager implements LayoutManager2 {
	private static final int RELATION_SPACING = 5; // 72 = 1 inch

	private static final int TABLE_PADDING = 10; // 72 = 1 inch

	private Dimension size;

	private boolean sizeKnown;

	private final Map<Component,Dimension> prefSizes = new HashMap<Component,Dimension>();

	private final Map<Component,Object> constraints = new HashMap<Component,Object>();

	private final List<DiagramComponent> mainTables;

	private final List<List<DiagramComponent>> dimensionTables;

	private final List<RelationComponent> relations;

	private final List<Integer> rowHeights;

	private final List<Integer> rowWidths;

	private final Collection<Component> fixedComps;

	/**
	 * Sets up some defaults for the layout, ready for use.
	 */
	public TargetLayoutManager() {
		this.sizeKnown = true;
		this.size = new Dimension(0, 0);
		this.mainTables = new ArrayList<DiagramComponent>();
		this.rowHeights = new ArrayList<Integer>();
		this.rowWidths = new ArrayList<Integer>();
		this.dimensionTables = new ArrayList<List<DiagramComponent>>();
		this.relations = new ArrayList<RelationComponent>();
		this.fixedComps = new HashSet<Component>();
	}
	
	  /**
	   * Returns the alignment along the x axis. This specifies how the component
	   * would like to be aligned relative to other components. The value should be
	   * a number between 0 and 1 where 0 represents alignment along the origin, 1
	   * is aligned the furthest away from the origin, 0.5 is centered, etc.
	   */
	public float getLayoutAlignmentX(final Container target) {
		return 0.5f;
	}

	  /** Returns the alignment along the y axis. This specifies how the component
	   * would like to be aligned relative to other components. The value should be
	   * a number between 0 and 1 where 0 represents alignment along the origin, 1
	   * is aligned the furthest away from the origin, 0.5 is centered, etc.
	   */
	public float getLayoutAlignmentY(final Container target) {
		return 0.5f;
	}
	  /**
	   * Invalidates the layout, indicating that if the layout manager has cached
	   * information it should be discarded.
	   */
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

			// We have the same number of rows as main/subclass tables.
			for (int rowNum = 0; rowNum < this.mainTables.size(); rowNum++) {
				int rowHeight = 0;
				int rowWidth = 0;
				Component comp = (Component) this.mainTables.get(rowNum);

				// Start each row by working out space required for
				// the main/subclass table that begins it.
				if (comp != null) {
					final Dimension prefSize = comp.getPreferredSize();
					this.prefSizes.put(comp, prefSize);
					rowHeight = prefSize.height;
					rowWidth = prefSize.width;
				}

				// Update the row width to accommodate the dimensions too.
				for (final Iterator<DiagramComponent> i = (this.dimensionTables
						.get(rowNum)).iterator(); i.hasNext();) {
					comp = (Component) i.next();
					if (!comp.isVisible())
						continue;
					final Dimension prefSize = comp.getPreferredSize();
					this.prefSizes.put(comp, prefSize);
					rowHeight = Math.max(rowHeight, prefSize.height);
					rowWidth += prefSize.width;
				}

				// Pad the row with space for relations.
				rowHeight += TargetLayoutManager.TABLE_PADDING * 2;
				rowHeight += this.dimensionTables.get(rowNum).size()
						* TargetLayoutManager.RELATION_SPACING;
				this.rowHeights.set(rowNum, new Integer(rowHeight));
				this.size.height += rowHeight;

				// Allow horizontal space for relations between each dimension.
				rowWidth += TargetLayoutManager.TABLE_PADDING * 2;
				rowWidth += (this.dimensionTables.get(rowNum).size() + 1)
						* TargetLayoutManager.TABLE_PADDING * 2;
				rowWidth += this.dimensionTables.get(rowNum).size()
						* TargetLayoutManager.RELATION_SPACING * 2;
				this.rowWidths.set(rowNum, new Integer(rowWidth));
				this.size.width = Math.max(rowWidth, this.size.width);
			}

			this.sizeKnown = true;
		}
	}

	  /**
	   * Adds the specified component to the layout, using the specified constraint
	   * object.
	   * 
	   * @param comp
	   *          the component to be added
	   * @param constraints
	   *          where/how the component is added to the layout.
	   */
	public void addLayoutComponent(final Component comp,
			final Object constraints) {
		synchronized (comp.getTreeLock()) {
			if (comp instanceof RelationComponent)
				this.relations.add((RelationComponent)comp);
			else if (comp instanceof DiagramComponent
					&& constraints instanceof TargetLayoutConstraint) {
				this.constraints.put(comp, constraints);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.put(comp, prefSize);

				final int rowNum = ((TargetLayoutConstraint) constraints)
						.getRow();

				// Ensure arrays are large enough.
				while (this.mainTables.size() - 1 < rowNum) {
					this.mainTables.add(null);
					this.rowHeights.add(new Integer(0));
					this.rowWidths.add(new Integer(
							TargetLayoutManager.TABLE_PADDING * 2));
					this.dimensionTables.add(new ArrayList<DiagramComponent>());
				}

				// Work out where to put it.
				if (((TargetLayoutConstraint) constraints).getType() == TargetLayoutConstraint.MAIN)
					this.mainTables.set(rowNum, (DiagramComponent)comp);
				else
					this.dimensionTables.get(rowNum).add((DiagramComponent)comp);

				// Update the row width to accommodate it.
				final int oldRowWidth = ((Integer) this.rowWidths.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth += prefSize.width
						+ TargetLayoutManager.TABLE_PADDING * 2
						+ TargetLayoutManager.RELATION_SPACING * 2;
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				// Increase the row height if necessary.
				final int oldRowHeight = ((Integer) this.rowHeights.get(rowNum))
						.intValue();
				final int newRowHeight = Math.max(oldRowHeight, prefSize.height
						+ TargetLayoutManager.TABLE_PADDING * 2)
						+ TargetLayoutManager.RELATION_SPACING;
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

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
			else if (comp instanceof RelationComponent)
				this.relations.remove(comp);
			else {
				final TargetLayoutConstraint constraints = (TargetLayoutConstraint) this.constraints
						.remove(comp);
				final Dimension prefSize = comp.getPreferredSize();
				this.prefSizes.remove(comp);

				final int rowNum = constraints.getRow();

				// Work out where to remove it from.
				if (constraints.getType() == TargetLayoutConstraint.MAIN)
					this.mainTables.set(rowNum, null);
				else
					this.dimensionTables.get(rowNum).remove(comp);

				// Reduce the row width accordingly.
				final int oldRowWidth = ((Integer) this.rowWidths.get(rowNum))
						.intValue();
				final int oldRowHeight = ((Integer) this.rowHeights.get(rowNum))
						.intValue();
				int newRowWidth = oldRowWidth;
				newRowWidth -= prefSize.width
						+ TargetLayoutManager.TABLE_PADDING * 2
						+ TargetLayoutManager.RELATION_SPACING * 2;
				this.rowWidths.set(rowNum, new Integer(newRowWidth));

				// If the row maximum height is now too big, reduce it.
				int newRowHeight = this.mainTables.get(rowNum) != null ? ((Component) this.mainTables
						.get(rowNum)).getPreferredSize().height
						: 0;
				for (final Iterator<DiagramComponent> i = this.dimensionTables
						.get(rowNum).iterator(); i.hasNext();)
					newRowHeight = Math.max(newRowHeight,
							((Component) i.next()).getPreferredSize().height);
				newRowHeight += TargetLayoutManager.TABLE_PADDING * 2
						+ this.dimensionTables.get(rowNum).size()
						* TargetLayoutManager.RELATION_SPACING;
				this.rowHeights.set(rowNum, new Integer(newRowHeight));

				this.size.height -= oldRowHeight - newRowHeight;

				// While last row is empty, remove last row.
				int lastRow = this.mainTables.size() - 1;
				while (lastRow >= 0 && this.mainTables.get(lastRow) == null
						&&  this.dimensionTables.get(lastRow).isEmpty()) {
					// Remove all references to empty row.
					this.mainTables.remove(lastRow);
					this.rowHeights.remove(lastRow);
					this.rowWidths.remove(lastRow);
					this.dimensionTables.remove(lastRow);
					// Update last row pointer.
					lastRow--;
				}

				// New width needs re-calculating from all rows.
				this.size.width = 0;
				for (final Iterator<Integer> i = this.rowWidths.iterator(); i.hasNext();)
					this.size.width = Math.max((i.next()).intValue(),
							this.size.width);
			}
		}
	}

	public void layoutContainer(final Container parent) {
		// Work out how big we are.
		this.calculateSize(parent);
		synchronized (parent.getTreeLock()) {
			// Fixed components are ignored. The parent should
			// lay them out.

			// Assumption that we are laying out a diagram.
			final Dimension maskedButton = ((Diagram) parent)
					.getHideMaskedArea();

			// Lay out each row at a time.
			int nextY = TargetLayoutManager.TABLE_PADDING + maskedButton.height;

			for (int rowNum = 0; rowNum < this.mainTables.size(); rowNum++) {
				int x = TargetLayoutManager.TABLE_PADDING * 3;
				final int y = nextY
						+ ((Integer) this.rowHeights.get(rowNum)).intValue()
						- this.dimensionTables.get(rowNum).size()
						* TargetLayoutManager.RELATION_SPACING
						- TargetLayoutManager.TABLE_PADDING;

				// First of all print the main/subclass table.
				if (this.mainTables.get(rowNum) != null) {
					final Component comp = (Component) this.mainTables
							.get(rowNum);
					final Dimension prefSize = (Dimension) this.prefSizes
							.get(comp);
					comp.setBounds(x, y - prefSize.height, prefSize.width,
							prefSize.height);
					comp.validate();
					x += prefSize.width + TargetLayoutManager.TABLE_PADDING
							* 2
							+ this.dimensionTables.get(rowNum).size()
							* TargetLayoutManager.RELATION_SPACING;
				}

				// Then all the dimensions for that table.
				for (final Iterator<DiagramComponent> i = this.dimensionTables
						.get(rowNum).iterator(); i.hasNext();) {
					final Component comp = (Component) i.next();
					if (!comp.isVisible())
						continue;
					final Dimension prefSize = (Dimension) this.prefSizes
							.get(comp);
					comp.setBounds(x, y - prefSize.height, prefSize.width,
							prefSize.height);
					comp.validate();
					x += prefSize.width + TargetLayoutManager.TABLE_PADDING
							* 2 + TargetLayoutManager.RELATION_SPACING;
				}
				nextY += ((Integer) this.rowHeights.get(rowNum)).intValue();
			}

			// Finally print all relations.
			for (final Iterator<RelationComponent> i = this.relations.iterator(); i.hasNext();) {
				final RelationComponent comp = i.next();

				// Obtain keys and work out position relative to
				// diagram.
				int rowNum = 0;
				int rowBottom = maskedButton.height + ((Integer) this.rowHeights.get(rowNum)).intValue();
				final KeyComponent firstKey = comp.getFirstKeyComponent();
				final KeyComponent secondKey = comp.getSecondKeyComponent();
				if (firstKey == null || firstKey.getParent() == null
						|| !firstKey.isVisible() || !firstKey.getParent().isValid()) 
					continue;
				if (secondKey == null || secondKey.getParent() == null
						|| !secondKey.isVisible()
						|| !secondKey.getParent().isValid()) 	
					continue;
				// Update key locations.
				Rectangle firstKeyRectangle = firstKey.getBounds();
				final int firstKeyInsetX = firstKeyRectangle.x;
				Rectangle secondKeyRectangle = secondKey.getBounds();
				final int secondKeyInsetX = secondKeyRectangle.x;
				firstKeyRectangle = SwingUtilities.convertRectangle(firstKey
						.getParent(), firstKeyRectangle, parent);
				secondKeyRectangle = SwingUtilities.convertRectangle(secondKey
						.getParent(), secondKeyRectangle, parent);
				// Work out true row bottom.
				while ((firstKeyRectangle.y >= rowBottom || secondKeyRectangle.y >= rowBottom)
						&& rowNum < this.rowHeights.size() - 1)
					rowBottom += ((Integer) this.rowHeights.get(++rowNum))
							.intValue();

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
				if (firstKeyRectangle.x == secondKeyRectangle.x) {
					// Main/Subclass -> Subclass
					relBottomY = (int) Math.max(leftKeyRectangle.getCenterY(),
							rightKeyRectangle.getCenterY());
					relLeftX = leftKeyRectangle.x
							- TargetLayoutManager.TABLE_PADDING;
					relRightX = rightKeyRectangle.x;

					leftX = leftKeyRectangle.x - leftKeyInsetX;
					leftTagX = leftX - TargetLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = rightKeyRectangle.x - rightKeyInsetX;
					rightTagX = rightX - TargetLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = leftX - TargetLayoutManager.TABLE_PADDING * 2;
					viaY = (leftY + rightY) / 2;
				} else {
					// Main/Subclass -> Dimension
					relRightX = rightKeyRectangle.x;
					relLeftX = (int) leftKeyRectangle.getMaxX();
					relBottomY = rowBottom;

					leftX = (int) leftKeyRectangle.getMaxX() + leftKeyInsetX;
					leftTagX = leftX + TargetLayoutManager.RELATION_SPACING;
					leftY = (int) leftKeyRectangle.getCenterY();
					rightX = rightKeyRectangle.x - rightKeyInsetX;
					rightTagX = rightX - TargetLayoutManager.RELATION_SPACING;
					rightY = (int) rightKeyRectangle.getCenterY();
					viaX = leftX
							+ this.dimensionTables.get(rowNum).size()
							* TargetLayoutManager.RELATION_SPACING / 2;
					viaY = relTopY + (int) ((relBottomY - relTopY) * 1.8);
				}

				// Set overall bounds.
				final Rectangle bounds = new Rectangle(
						(Math.min(relLeftX, viaX) - TargetLayoutManager.RELATION_SPACING * 4),
						(Math.min(relTopY, viaY) - TargetLayoutManager.RELATION_SPACING * 4),
						(Math.abs(Math.max(relRightX, viaX)
								- Math.min(relLeftX, viaX)) + TargetLayoutManager.RELATION_SPACING * 8),
						(Math.abs(Math.max(relBottomY, viaY)
								- Math.min(relTopY, viaY)) + TargetLayoutManager.RELATION_SPACING * 8));
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
	 * Use this class to specify which row and what type each table should be.
	 */
	public static class TargetLayoutConstraint {
		/**
		 * This component is a main/subclass table.
		 */
		public static final int MAIN = 1;

		/**
		 * This component is a dimension table.
		 */
		public static final int DIMENSION = 2;

		private final int type;

		private final int row;

		/**
		 * Construct a constraint indicating which row the component should go
		 * on, and what type it is.
		 * 
		 * @param type
		 *            the type of component (see {@link #MAIN} and
		 *            {@link #DIMENSION}).
		 * @param row
		 *            the row to put it on (zero-indexed).
		 */
		public TargetLayoutConstraint(final int type, final int row) {
			this.type = type;
			this.row = row;
		}

		private int getType() {
			return this.type;
		}

		private int getRow() {
			return this.row;
		}
	}
}
