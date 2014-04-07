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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.Transaction;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.common.view.gui.dialogs.ComponentImageSaver;
import org.biomart.common.view.gui.dialogs.ComponentPrinter;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.view.component.BoxShapedComponent;
import org.biomart.configurator.view.component.DiagramComponent;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.Table;

/**
 * A diagram represents a collection of database components. It usually contains
 * components that are tables, which themselves contain other objects which are
 * keys and columns. The diagram also contains components which are relations,
 * which link key objects. The diagram remembers all this, and provides a
 * context-menu handling for the components it displays. {@link DiagramContext}
 * listeners can be attached to the diagram to customise context menu rendering,
 * and also to customise rendering of the individual components, for instance in
 * order to apply alternative colour schemes.
 * <p>
 * Specific extensions of this basic diagram class handle the decisions as to
 * what to add and what to remove from the diagram. This base class simply deals
 * with the context menus and display of components in the diagram.
 */
public abstract class Diagram extends JLayeredPane implements Scrollable,
		Autoscroll, AdjustmentListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * This is inherited by subclasses to indicate they need redrawing when the
	 * next transaction ends.
	 */
	protected boolean needsRecalc = false;

	/**
	 * This is inherited by subclasses to indicate they need repainting when the
	 * next transaction ends.
	 */
	protected boolean needsRepaint = false;

	/**
	 * This is public only so that the diagram picks up component changes.
	 */
	public boolean needsSubComps = false;

	private static final int AUTOSCROLL_INSET = 12;

	/**
	 * The background colour to use for this diagram.
	 */
	public static final Color BACKGROUND_COLOUR = Color.WHITE;

	/**
	 * The background for the masked checkbox.
	 */
	public static final Color MASK_BG_COLOR = Color.WHITE;

	/**
	 * The layer for always-on-top components.
	 */
	public static final int TOP_LAYER = 0;

	/**
	 * The layer for middle components.
	 */
	public static final int TABLE_LAYER = 0;

	/**
	 * The layer for always-bottom components.
	 */
	public static final int RELATION_LAYER = -1;
	
	private final List<BoxShapedComponent> selectedItems = new ArrayList<BoxShapedComponent>();

	// OK to use maps as it gets cleared out each time, the keys never change.
	private final Map<MartConfiguratorObject,DiagramComponent> componentMap = 
		new HashMap<MartConfiguratorObject,DiagramComponent>();

	private DiagramContext diagramContext;
	
	private JCheckBox hideMasked;

	/**
	 * Creates a new diagram which belongs inside the given mart tab and uses
	 * the given layout manager. The {@link MartTab#getMart()} method will be
	 * used to work out which mart is being interacted with when the user
	 * selects items in the context menus attached to components in this
	 * diagram.
	 * 
	 * @param layout
	 *            the layout manager to use to layout the diagram. If
	 *            <tt>null</tt>, a default manager will be used that does not
	 *            recognise the distinction between relations and tables.
	 * @param martTab
	 *            the mart tab this diagram will use to discover which mart is
	 *            currently visible when working out where to send events.
	 */

	public Diagram(final LayoutManager layout) {
		// Set us up with the layout.
		super();
		if (layout != null)
			this.setLayout(layout);
		else
			this.setLayout(new FlowLayout());
		Log.debug("Creating new diagram of type " + this.getClass().getName());

		// Enable mouse events to be picked up all over the diagram.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		this.setDoubleBuffered(true); // Stop flicker.

		this.hideMasked = new JCheckBox(Resources.get("HIDEMASKED"));
		// It has a semi-transparent background with no border.
		this.hideMasked.setOpaque(true);
		this.hideMasked.setBackground(Diagram.MASK_BG_COLOR);
		this.hideMasked.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				Diagram.this.recalculateDiagram();
			}
		});
		
		// Deal with drops.
		final DropTargetListener dtListener = new DropTargetListener() {
			public void dragEnter(DropTargetDragEvent e) {
				e.rejectDrag();
			}

			public void dragOver(DropTargetDragEvent e) {
				e.rejectDrag();
			}

			public void dropActionChanged(DropTargetDragEvent e) {
				e.rejectDrag();
			}

			public void dragExit(DropTargetEvent e) {
			}

			public void drop(DropTargetDropEvent e) {
				e.rejectDrop();
			}
		};
		new DropTarget(this, DnDConstants.ACTION_COPY, dtListener, true);

		// Set our background.
		this.setBackground(Diagram.BACKGROUND_COLOUR);
		this.setOpaque(true);
	}

	private void recalculateSubComps() {
		final Collection<Component> comps = Arrays.asList(this.getComponents());
		for (final Iterator<Map.Entry<MartConfiguratorObject,DiagramComponent>> i = this.componentMap.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry<MartConfiguratorObject,DiagramComponent> entry =  i.next();
			if (!comps.contains(entry.getValue()))
				i.remove();
		}
		final Map<MartConfiguratorObject,DiagramComponent> subCompMap =
			new HashMap<MartConfiguratorObject,DiagramComponent>();
		for (final Iterator<DiagramComponent> i = this.componentMap.values().iterator(); i
				.hasNext();) {
			final Object o = i.next();
			if (o instanceof DiagramComponent)
				subCompMap.putAll(((DiagramComponent) o).getSubComponents());
		}
		this.componentMap.putAll(subCompMap);
	}



	/**
	 * Return all components that intersect a region. The region is defined by
	 * the smallest box possible that contains the entire bounding boxes of the
	 * two components specified.
	 * 
	 * @param oneCorner
	 *            one of the two components that specify the region.
	 * @param otherCorner
	 *            the other component that specifies the region.
	 * @param componentClass
	 *            the type of components to search for within the region.
	 * @return the components found in that region. May be empty but never
	 *         <tt>null</tt>. Will include the two components used to specify
	 *         the region if they are of the correct class.
	 */
	public Collection<Component> getComponentsInRegion(final Component oneCorner,
			final Component otherCorner, final Class<?> componentClass) {
		final Rectangle firstCorner = SwingUtilities.convertRectangle(oneCorner
				.getParent(), oneCorner.getBounds(), this);
		final Rectangle secondCorner = SwingUtilities.convertRectangle(
				otherCorner.getParent(), otherCorner.getBounds(), this);
		final Rectangle clipRegion = firstCorner.union(secondCorner);
		final Collection<Component> results = new HashSet<Component>();
		for (final Iterator<Map.Entry<MartConfiguratorObject, DiagramComponent>> i = this.componentMap.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry<MartConfiguratorObject, DiagramComponent>  entry = i.next();
			if (!entry.getKey().getClass().equals(componentClass))
				continue;
			final Component candidate = (Component)entry.getValue();
			final Rectangle candRect = SwingUtilities.convertRectangle(
					candidate.getParent(), candidate.getBounds(), this);
			if (clipRegion.contains(candRect))
				results.add(candidate);
		}
		return results;
	}


	private JPopupMenu populateContextMenu(final JPopupMenu contextMenu) {
		if (this.diagramContext != null)
			this.diagramContext.populateContextMenu(contextMenu, null, this);
		return contextMenu;
	}

	protected void addImpl(final Component comp, final Object constraints,
			final int index) {
		if (comp instanceof DiagramComponent) {
			final DiagramComponent dcomp = (DiagramComponent) comp;
			this.needsSubComps = true;
			this.componentMap.put(dcomp.getObject(), dcomp);
//			dcomp.getSubComponents().addPropertyChangeListener(this.listener);
		}
		super.addImpl(comp, constraints, index);
	}

	public void remove(final Component comp) {
	//	if (comp instanceof DiagramComponent)
	//		this.componentMap.remove(((DiagramComponent) comp).getObject());
		super.remove(comp);
	}

	public void remove(final int index) {
		final Object comp = this.getComponent(index);
	//	if (comp instanceof DiagramComponent)
	//		this.componentMap.remove(((DiagramComponent) comp).getObject());
		super.remove(index);
	}

	public void removeAll() {
		// Clear our internal lookup map.
		this.componentMap.clear();
		// Do what the parent JComponent would do.
		super.removeAll();
	}

	/**
	 * Override this method to actually do the work of recalculating which
	 * components should appear in the diagram. The method should first clear
	 * out all the old components from the diagram, as this will not have been
	 * done already. On return, the diagram should contain a new set of
	 * components, or an updated set of components that correctly reflects its
	 * current state.
	 */
	public abstract void doRecalculateDiagram();


	/**
	 * Looks up the diagram component in this diagram that is related to the
	 * specified database object. If there is no component related to that
	 * object, then null is returned, otherwise the component is returned.
	 * 
	 * @param object
	 *            the database object to look up the component for.
	 * @return the diagram component that represents that database object in
	 *         this diagram, or null if that model object is not in this diagram
	 *         at all.
	 */
	public DiagramComponent getDiagramComponent(final Object object) {
		return (DiagramComponent) this.componentMap.get(object);
	}


	/**
	 * Override this to find out when the hide masked checkbox changes.
	 * 
	 * @param newHideMasked
	 *            true if it is now selected.
	 */
	protected void hideMaskedChanged(final boolean newHideMasked) {
		// By default we don't care.
	}


	/**
	 * This method is called when the diagram needs to be cleared and
	 * repopulated. It remembers the states of all the components in the
	 * diagram, then delegates to {@link #doRecalculateDiagram()} to do the
	 * actual work of clearing out and repopulating the diagram. Finally, it
	 * reapplies the states remembered to any components in the new diagram that
	 * match the components in the old diagram using the
	 * {@link Object#equals(Object)} method.
	 */
	public void recalculateDiagram() {
		Log.debug("Recalculating diagram");

		// Remember states.
		final Map<MartConfiguratorObject,Object> stateMap = new HashMap<MartConfiguratorObject,Object>();
		for (final Iterator<Map.Entry<MartConfiguratorObject,DiagramComponent>> i = this.componentMap.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry<MartConfiguratorObject,DiagramComponent> entry = i.next();
			final DiagramComponent o = entry.getValue();
			if (o instanceof BoxShapedComponent)
				stateMap.put(entry.getKey(), ((BoxShapedComponent) o)
						.getState());

		}

		// First of all, remove all our existing components.
		this.removeAll();
		this.componentMap.clear();

		// Delegate to do the actual diagram
		// clear-and-repopulate.
		this.doRecalculateDiagram();

		// Do the subcomp thing.
		this.recalculateSubComps();

		// Reinstate states.
		for (final Iterator<Map.Entry<MartConfiguratorObject,Object>> i = stateMap.entrySet().iterator(); i.hasNext();) {
			final Map.Entry<MartConfiguratorObject,Object> entry =  i.next();
			final BoxShapedComponent o = (BoxShapedComponent) this.componentMap
					.get(entry.getKey());
			if (o != null && entry.getValue() != null)
				o.setState(entry.getValue());
		}


		// Set up a floating panel with the hide masked box.
		if (this.isUseHideMasked())
			this.add(Diagram.this.hideMasked, null, Diagram.TOP_LAYER);

		// Resize the diagram to fit our new components.
		this.resizeDiagram();

		// Initial placement of the hide masked button.
		this.adjustmentValueChanged(null);

		// Repaint the whole diagram to update the state of any
		// new bits and remove any ghosts that may be left on
		// screen.
		this.repaintDiagram();
	}

	/**
	 * Set the hide masked checkbox.
	 * 
	 * @param newHideMasked
	 *            true to select it.
	 */
	public void setHideMasked(final boolean newHideMasked) {
		if (this.hideMasked.isSelected() != newHideMasked)
			this.hideMasked.doClick();
	}
	
	/**
	 * This method walks through the components in the diagram, and calls
	 * {@link DiagramComponent#repaintDiagramComponent()} on each in turn. This
	 * has the effect of updating the appearance of every component and causing
	 * it to redraw. It does not recalculate the location of any of these
	 * components, neither does it recalculate which components are displayed in
	 * the diagram at present.
	 * <p>
	 * This method does not resize the diagram to fit components, so do not use
	 * it if the component size is likely to have changed (eg. show/hide columns
	 * on a table). Use {@link #recalculateDiagram()} instead.
	 */
	public void repaintDiagram() {
		for (final Iterator<DiagramComponent> i = this.componentMap.values().iterator(); i
				.hasNext();) {
			DiagramComponent dc = i.next();
			dc.repaintDiagramComponent();
		}
		this.repaint();
	}

	/**
	 * Are we waiting to recalculate?
	 * 
	 * @return <tt>true</tt> if we are.
	 */
	public boolean isNeedsRecalc() {
		return this.needsRecalc;
	}

	/**
	 * Are we waiting to redraw?
	 * 
	 * @return <tt>true</tt> if we are.
	 */
	public boolean isNeedsRepaint() {
		return this.needsRepaint;
	}

	/**
	 * Work out the minimum size for this diagram, resize ourselves to that
	 * size, then validate ourselves so our contents get laid out correctly.
	 */
	public void resizeDiagram() {
		// Reset our size to the minimum.
		this.setSize(this.getPreferredSize());
		// Update ourselves.
		this.revalidate();
	}

	public Dimension getPreferredSize() {
		// Stretch ourselves to fill the viewport if we are smaller than it.
		Dimension preferredSize = this.getLayout().preferredLayoutSize(this);
		final JViewport viewport = this.getParent() instanceof JViewport ? (JViewport) this
				.getParent()
				: null;
		if (viewport != null)
			preferredSize = new Dimension((int) Math.max(preferredSize
					.getWidth(), viewport.getWidth()), (int) Math.max(
					preferredSize.getHeight(), viewport.getHeight()));
		return preferredSize;
	}


	public void autoscroll(final Point cursorLoc) {
		final JViewport viewport = (JViewport) SwingUtilities
				.getAncestorOfClass(JViewport.class, this);
		if (viewport == null)
			return;
		final Point viewPos = viewport.getViewPosition();
		final int viewHeight = viewport.getExtentSize().height;
		final int viewWidth = viewport.getExtentSize().width;

		// perform scrolling
		if (cursorLoc.y - viewPos.y < Diagram.AUTOSCROLL_INSET)
			viewport.setViewPosition(new Point(viewPos.x, Math.max(viewPos.y
					- Diagram.AUTOSCROLL_INSET, 0)));
		else if (viewPos.y + viewHeight - cursorLoc.y < Diagram.AUTOSCROLL_INSET)
			// down
			viewport
					.setViewPosition(new Point(viewPos.x, Math.min(viewPos.y
							+ Diagram.AUTOSCROLL_INSET, this.getHeight()
							- viewHeight)));
		else if (cursorLoc.x - viewPos.x < Diagram.AUTOSCROLL_INSET)
			// left
			viewport.setViewPosition(new Point(Math.max(viewPos.x
					- Diagram.AUTOSCROLL_INSET, 0), viewPos.y));
		else if (viewPos.x + viewWidth - cursorLoc.x < Diagram.AUTOSCROLL_INSET)
			// right
			viewport.setViewPosition(new Point(Math.min(viewPos.x
					+ Diagram.AUTOSCROLL_INSET, this.getWidth() - viewWidth),
					viewPos.y));
	}

	public Insets getAutoscrollInsets() {
		final int height = this.getHeight();
		final int width = this.getWidth();
		return new Insets(height, width, height, width);
	}

	public Dimension getPreferredScrollableViewportSize() {
		return this.getPreferredSize();
	}

	public int getScrollableBlockIncrement(final Rectangle visibleRect,
			final int orientation, final int direction) {
		return this.getScrollableUnitIncrement(visibleRect, orientation,
				direction) * 4;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	public int getScrollableUnitIncrement(final Rectangle visibleRect,
			final int orientation, final int direction) {
		return Diagram.AUTOSCROLL_INSET;
	}

	/**
	 * Should we hide masked things?
	 * 
	 * @return <tt>true</tt> if we should.
	 */
	protected boolean isUseHideMasked() {
		return true;
	}
	
	/**
	 * Work out how much space the masked/hidden button needs.
	 * 
	 * @return the space it needs.
	 */
	protected Dimension getHideMaskedArea() {
		if (this.isUseHideMasked())
			return this.hideMasked.getPreferredSize();
		else
			return new Dimension(0, 0);
	}

	public void adjustmentValueChanged(final AdjustmentEvent evt) {
		if (!this.isUseHideMasked())
			return;
		// This panel hangs out top-left regardless of viewport
		// scrolling.
		final Dimension buttonSize = this.getHideMaskedArea();
		JViewport viewport = null;
		if (this.getParent() != null && this.getParent() instanceof JViewport)
			viewport = (JViewport) this.getParent();
		if (viewport != null) {
			final Rectangle viewportOffset = viewport.getViewRect();
			//this.hideMasked.setBounds(viewportOffset.x + viewportOffset.width
			//		- buttonSize.width, viewportOffset.y, buttonSize.width,
			//		buttonSize.height);
			this.hideMasked.setBounds(10, viewportOffset.y, buttonSize.width,
					buttonSize.height);

		} else
			this.hideMasked.setBounds(10, 0, buttonSize.width, buttonSize.height);

			//this.hideMasked.setBounds(this.getPreferredSize().width
			//		- buttonSize.width, 0, buttonSize.width, buttonSize.height);
		// To wipe out the opaque background a repaint is necessary.
		Diagram.this.hideMasked.repaint();
	}

	/**
	 * Sets the diagram context that will be used to customise colours and
	 * context menus for this diagram.
	 * 
	 * @param diagramContext
	 *            the diagram context to use.
	 */
	public void setDiagramContext(final DiagramContext diagramContext) {
		Log.debug("Switching diagram context");
		// Apply it to ourselves.
		if (diagramContext != this.diagramContext) {
			this.diagramContext = diagramContext;
			this.repaintDiagram();
		}
	}
	
	/**
	 * Returns the diagram context that is being used to customise colours and
	 * context menus for this diagram.
	 * 
	 * @return the diagram context that is being used.
	 */
	public DiagramContext getDiagramContext() {
		return this.diagramContext;
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;

		//if (evt.getButton() > 0)
			//this.deselectAll();

		// Is it a right-click?
		if (evt.isPopupTrigger()) {

			// Obtain the basic context menu for this diagram.
			final JPopupMenu contextMenu = new JPopupMenu();

			// Add the common diagram stuff.
			this.populateContextMenu(contextMenu);

			// If our context menu actually has anything in it now, display it.
			if (contextMenu.getComponentCount() > 0) {
				contextMenu.show(this, evt.getX(), evt.getY());
				eventProcessed = true;
			}
		}

		// Pass the event on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	public Table askUserForTable() {
		// Pop up a dialog box with a list of tables in it, and ask the
		// user to select one. Only tables which appear in this diagram will
		// be in the list.

		// First, work out what tables are in this diagram.
		final Map<String,Table> sortedTables = new TreeMap<String,Table>();
		for (final Iterator<MartConfiguratorObject> i = this.componentMap.keySet().iterator(); i
				.hasNext();) {
			final Object o = i.next();
			if (o instanceof DatasetTable)
				sortedTables.put(((DatasetTable) o).getName(), (DatasetTable)o);
			else if (o instanceof Table)
				sortedTables.put(((Table) o).getName(), (Table)o);
		}

		// Create a combo box of tables.
		final JComboBox tableChoice = new JComboBox();
		tableChoice.setRenderer(new ListCellRenderer() {
			public Component getListCellRendererComponent(final JList list,
					final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final Table tbl = (Table) value;
				final JLabel label = new JLabel((String) McGuiUtils.INSTANCE.getKeysByValue(sortedTables, tbl));
				label.setOpaque(true);
				label.setFont(list.getFont());
				if (isSelected) {
					label.setBackground(list.getSelectionBackground());
					label.setForeground(list.getSelectionForeground());
				} else {
					label.setBackground(list.getBackground());
					label.setForeground(list.getForeground());
				}
				return label;
			}
		});
		for (final Iterator<Table> i = sortedTables.values().iterator(); i.hasNext();)
			tableChoice.addItem(i.next());

		// Now, create the choices box, display it, and return the one
		// that the user selected. If the user didn't select anything, or
		// cancelled the choice, this will return null.
		JOptionPane.showMessageDialog(this, tableChoice, Resources
				.get("findTableName"), JOptionPane.QUESTION_MESSAGE, null);

		// Return the choice.
		return (Table) tableChoice.getSelectedItem();
	}

	/**
	 * Given a particular model object, lookup the diagram component that it
	 * represents, then scroll the diagram so that it is centred on that diagram
	 * component. This depends on the diagram being held within a
	 * {@link JScrollPane} - if it isn't, this method will do nothing.
	 * 
	 * @param object
	 *            the database object to locate and scroll to.
	 */
	public void findObject(final Object object) {
		// Don't do it if the object is null or if we are not in a viewport.
		if (object == null)
			return;

		// Look up the diagram component for the model object.
		final JComponent comp = (JComponent) this.getDiagramComponent(object);
		// If the model object is not in this diagram, don't scroll to it!
		if (comp == null)
			return;

		// Obtain the scrollpane view of this diagram.
		final JViewport viewport = (JViewport) SwingUtilities
				.getAncestorOfClass(JViewport.class, this);

		if (viewport != null) {
			// Work out the location of the diagram component.
			final Rectangle compLocation = SwingUtilities.convertRectangle(comp
					.getParent(), comp.getBounds(), this);

			// How big is the scrollpane view we are being seen through?
			final Dimension viewSize = viewport.getExtentSize();
			final Dimension ourSize = this.getSize();

			// Work out the top-left coordinate of the area of diagram that
			// should appear in the scrollpane if this diagram component is to
			// appear in the absolute centre.
			int newViewPointX = (int) compLocation.getCenterX()
					- viewSize.width / 2;
			int newViewPointY = (int) compLocation.getCenterY()
					- viewSize.height / 2;

			// Move the scrollpoint if it goes off the bottom-right.
			if (newViewPointX + viewSize.width > ourSize.width)
				newViewPointX = ourSize.width - viewSize.width;
			if (newViewPointY + viewSize.height > ourSize.height)
				newViewPointY = ourSize.height - viewSize.height;

			// Move the scrollpoint if it goes off the top-left of the diagram
			// or if the whole diagram can fit without scrolling.
			if (newViewPointX < 0)
				newViewPointX = 0;
			if (newViewPointY < 0)
				newViewPointY = 0;

			// Scroll to that position.
			viewport.setViewPosition(new Point(newViewPointX, newViewPointY));
		}

		// Select the object.
		if (comp instanceof BoxShapedComponent)
			this.toggleItem((BoxShapedComponent) comp);

		// Repaint newly visible area.
		this.repaint(this.getVisibleRect());
	}

	/**
	 * Toggle the given item highlight on/off and dehighlight all others.
	 * 
	 * @param item
	 *            the item to toggle.
	 */
	private void toggleItem(final BoxShapedComponent item) {
		// (De)select this item only and clear rest of group.
		boolean selected = this.isSelected(item);
		this.deselectAll();
		if (!selected) {
			this.selectedItems.add(item);
			item.select();
		}
	}
	
	/**
	 * Check to see if the given item is highlighted in this diagram.
	 * 
	 * @param item
	 *            the item to check.
	 * @return <tt>true</tt> if it is highlighted.
	 */
	private boolean isSelected(final BoxShapedComponent item) {
		return this.selectedItems.contains(item);
	}

	/**
	 * Dehighlights all items.
	 */
	private void deselectAll() {
		for (final Iterator<BoxShapedComponent> i = this.selectedItems.iterator(); i.hasNext();)
			((BoxShapedComponent) i.next()).deselect();
		this.selectedItems.clear();
	}

	protected boolean isHideMasked() {
		return this.hideMasked.isSelected();
	}
}
