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

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.view.gui.diagrams.Diagram;
import org.biomart.configurator.view.gui.diagrams.DiagramContext;
import org.biomart.objects.objects.MartConfiguratorObject;


/**
 * Any diagram component that is box-shaped is derived from this class. It
 * handles all mouse-clicks and painting problems for them, and keeps track of
 * their sub-components in a map, so that code can reference them by database
 * object rather than exact component.
 * <p>
 * This class also handles click-and-rename capabilities by allowing subclasses
 * to specify a name label. It then calls those classes back when the name is
 * changed by the user.
 * 
 */
public abstract class BoxShapedComponent extends JPanel implements DiagramComponent, MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Subclasses use this if the component needs recalculating.
	 */
	protected boolean needsRecalc = false;

	/**
	 * Subclasses use this if the component needs repainting.
	 */
	protected boolean needsRepaint = false;

	private static final float BOX_DASHSIZE = 6.0f; // 72 = 1 inch

	private static final float BOX_DOTSIZE = 2.0f; // 72 = 1 inch

	private static final float BOX_LINEWIDTH = 1.0f; // 72 = 1 inch

	private static final float BOX_MITRE_TRIM = 10.0f; // 72 = 1 inch

	private static final Stroke DOTTED_DASHED_OUTLINE = new BasicStroke(
			BoxShapedComponent.BOX_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, BoxShapedComponent.BOX_MITRE_TRIM,
			new float[] { BoxShapedComponent.BOX_DASHSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE }, 0);

	private static final Stroke DASHED_OUTLINE = new BasicStroke(
			BoxShapedComponent.BOX_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, BoxShapedComponent.BOX_MITRE_TRIM,
			new float[] { BoxShapedComponent.BOX_DASHSIZE,
					BoxShapedComponent.BOX_DASHSIZE }, 0);

	private static final Stroke DOTTED_OUTLINE = new BasicStroke(
			BoxShapedComponent.BOX_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, BoxShapedComponent.BOX_MITRE_TRIM,
			new float[] { BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE }, 0);

	private static final Stroke INDEXED_OUTLINE = new BasicStroke(
			BoxShapedComponent.BOX_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, BoxShapedComponent.BOX_MITRE_TRIM,
			new float[] { BoxShapedComponent.BOX_DASHSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE,
					BoxShapedComponent.BOX_DOTSIZE }, 0);

	private static final Stroke OUTLINE = new BasicStroke(
			BoxShapedComponent.BOX_LINEWIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND, BoxShapedComponent.BOX_MITRE_TRIM);

	private static final Color SELECTED_COLOUR = Color.WHITE;

	private static final Color RENAMING_BORDER_COLOUR = Color.BLACK;

	private Diagram diagram;

	private boolean indexed = false;

	private boolean restricted = false;

	private boolean compounded = false;

	private boolean draggable = false;

	private boolean selectable = false;

	private boolean renameable = false;

	private boolean selected = false;

	private boolean beingRenamed = false;

	private JTextField name;

	private Object state;

	private Stroke stroke;

	private RenderingHints renderHints;
	
	private MartConfiguratorObject object;
	
	private Color color4Mouse;

	// OK to use map, as the components are recreated, not changed.
 	private final Map<MartConfiguratorObject,DiagramComponent> subComponents = 
		new HashMap<MartConfiguratorObject,DiagramComponent>();

	/**
	 * Constructs a box-shaped component around the given database object to be
	 * represented in the given diagram.
	 * 
	 * @param object
	 *            the database object to represent.
	 * @param diagram
	 *            the diagram to display ourselves in.
	 */
	public BoxShapedComponent(MartConfiguratorObject object, final Diagram diagram) {
		super();
		this.object = object;
		this.diagram = diagram;

		// Turn on the mouse.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		this.setDoubleBuffered(true); // Stop flicker.

		// Make sure we're not transparent.
		this.setOpaque(true);

		// Set-up rendering hints.
		this.renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		this.renderHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		this.addMouseListener(this);
//		this.changed = this.getObject().isVisibleModified();
	}


	/**
	 * Adds a sub-component to the map, but not to the diagram. This means that
	 * the component will take care of rendering these sub-components within its
	 * own bounds, but it is possibly to directly query the diagram to find out
	 * exactly how that sub-component has been rendered.
	 * 
	 * @param object
	 *            the model object the component represents.
	 * @param component
	 *            the component representing the model object.
	 */
	protected void addSubComponent(final MartConfiguratorObject object,
			final DiagramComponent component) {
		this.subComponents.put(object, component);
	}

	protected void paintBorder(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;
		// Override the stroke so that we get dotted outlines when appropriate.
		if (this.stroke != null)
			g2d.setStroke(this.stroke);
		super.paintBorder(g2d);
		if (this.object.isVisibleModified()) {
			this.getBorder().paintBorder(this, g,
					DiagramComponent.GLOW_WIDTH / 2,
					DiagramComponent.GLOW_WIDTH / 2,
					this.getWidth() - DiagramComponent.GLOW_WIDTH,
					this.getHeight() - DiagramComponent.GLOW_WIDTH);
			this.getBorder().paintBorder(this, g, DiagramComponent.GLOW_WIDTH,
					DiagramComponent.GLOW_WIDTH,
					this.getWidth() - DiagramComponent.GLOW_WIDTH*2,
					this.getHeight() - DiagramComponent.GLOW_WIDTH*2);
		}
	}

	protected void paintComponent(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHints(this.renderHints);
		super.paintComponent(g);
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.isPopupTrigger()) {
			// Build the basic menu.
			// If right-click on selected item, then
			// get multi menu and return that instead.
			final JPopupMenu contextMenu;
			contextMenu = this.getContextMenu();
			// Customise the context menu for this box's database object.
			if (this.getDiagram().getDiagramContext() != null)
				this.getDiagram().getDiagramContext().populateContextMenu(
						contextMenu, this, this.getObject());

/*			if (this.getDiagram().isSelected(this)) {
				contextMenu = this.getMultiContextMenu();
				// Customise the context menu for this box's database object.
				if (this.getDiagram().getDiagramContext() != null) {
					final Set selectedItems = new HashSet();
					for (final Iterator i = this.getDiagram()
							.getSelectedItems().iterator(); i.hasNext();)
						selectedItems.add(((BoxShapedComponent) i.next())
								.getObject());
					this.getDiagram().getDiagramContext()
							.populateMultiContextMenu(contextMenu,
									selectedItems, this.getObject().getClass());
				}
			} else {
				this.getDiagram().deselectAll();
				contextMenu = this.getContextMenu();
				// Customise the context menu for this box's database object.
				if (this.getDiagram().getDiagramContext() != null)
					this.getDiagram().getDiagramContext().populateContextMenu(
							contextMenu, this.getObject());
			}
			// Display.*/
			if (contextMenu.getComponentCount() > 0) {
				eventProcessed = true;
				contextMenu.show(this, evt.getX(), evt.getY());
			}
		} 
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
		else
			evt.consume();

	}

	public JPopupMenu getContextMenu() {
		final JPopupMenu contextMenu = new JPopupMenu();
		// No additional entries for us yet.
		return contextMenu;
	}

	public JPopupMenu getMultiContextMenu() {
		final JPopupMenu contextMenu = new JPopupMenu();
		// No additional entries for us yet.
		return contextMenu;
	}

	public Diagram getDiagram() {
		return this.diagram;
	}



	public Object getState() {
		return this.state;
	}
	
	@Override
	public Map<MartConfiguratorObject,DiagramComponent> getSubComponents() {
		return this.subComponents;
	}

	public void recalculateDiagramComponent() {
		// Remove everything.
		this.removeAll();
		final Object state = this.getState();
		this.doRecalculateDiagramComponent();
		this.diagram.needsSubComps = !this.getSubComponents().isEmpty();
		if (state != null)
			this.setState(state);
		// Update and paint.
		this.revalidate();
		this.repaintDiagramComponent();
	}

	/**
	 * This method actually does the work.
	 */
	protected abstract void doRecalculateDiagramComponent();

	public void repaintDiagramComponent() {
		this.updateAppearance();
	}

	/**
	 * If this is set to <tt>true</tt> then the component will appear with a
	 * dashed outline. Otherwise, it appears with a solid outline.
	 * 
	 * @param restricted
	 *            <tt>true</tt> if the component is to appear with a dashed
	 *            outline. The default is <tt>false</tt>.
	 */
	public void setRestricted(final boolean restricted) {
		this.restricted = restricted;
	}

	/**
	 * If this is set to <tt>true</tt> then the component will appear with a
	 * dotted outline. Otherwise, it appears with a solid outline.
	 * 
	 * @param compounded
	 *            <tt>true</tt> if the component is to appear with a dotted
	 *            outline. The default is <tt>false</tt>.
	 */
	public void setCompounded(final boolean compounded) {
		this.compounded = compounded;
	}

	/**
	 * If this is set to <tt>true</tt> then the component will appear with a
	 * thick outline. Otherwise, it appears with a normal outline. This
	 * overrides compounded and restricted settings.
	 * 
	 * @param indexed
	 *            <tt>true</tt> if the component is to appear with a thick
	 *            outline. The default is <tt>false</tt>.
	 */
	public void setIndexed(final boolean indexed) {
		this.indexed = indexed;
	}

	public void setState(final Object state) {
		this.state = state;
	}

	/**
	 * Can the user rename this by double-clicking on it?
	 * 
	 * @return <tt>true</tt> if they can.
	 */
	public boolean isRenameable() {
		return this.renameable && this.name != null;
	}

	/**
	 * Can the user rename this by double-clicking on it?
	 * 
	 * @param renameable
	 *            <tt>true</tt> if they can.
	 */
	public void setRenameable(final boolean renameable) {
		this.renameable = renameable;
	}

	/**
	 * Can the user select this by single-clicking on it?
	 * 
	 * @return <tt>true</tt> if they can.
	 */
	public boolean isSelectable() {
		return this.selectable && this.name != null;
	}

	/**
	 * Can the user select this by single-clicking on it?
	 * 
	 * @param selectable
	 *            <tt>true</tt> if they can.
	 */
	public void setSelectable(final boolean selectable) {
		this.selectable = selectable;
	}

	/**
	 * Can the user drag this?
	 * 
	 * @return <tt>true</tt> if they can.
	 */
	public boolean isDraggable() {
		return this.draggable;
	}

	/**
	 * Can the user drag this?
	 * 
	 * @param draggable
	 *            <tt>true</tt> if they can.
	 */
	public void setDraggable(final boolean draggable) {
		this.draggable = draggable;
	}

	/**
	 * Call this method when the component has become selected.
	 */
	public void select() {
		this.selected = true;
		this.cancelRename();
	}

	/**
	 * Call this method when the component has become deselected.
	 */
	public void deselect() {
		this.selected = false;
		this.cancelRename();
	}

	/**
	 * Sets the text field that will represent the selectable/renameable portion
	 * of this component. It will gain a mouse listener which will allow the
	 * user to single-click (select/highlight) and double-click (rename) the
	 * field. It also gains a keyboard listener to listen for Enter+Escape hits
	 * whilst renaming.
	 * 
	 * @param name
	 *            the text field to use.
	 */
	public void setRenameTextField(final JTextField name) {
		this.name = name;
		this.name.setDisabledTextColor(this.name.getForeground());
		this.name.setBackground(BoxShapedComponent.SELECTED_COLOUR);
		this.name.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterPressed");
		this.name.getActionMap().put("enterPressed", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(final ActionEvent e) {
				if (BoxShapedComponent.this.isBeingRenamed())
					BoxShapedComponent.this.doRename();
			}
		});
		this.name.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapePressed");
		this.name.getActionMap().put("escapePressed", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(final ActionEvent e) {
				if (BoxShapedComponent.this.isBeingRenamed())
					BoxShapedComponent.this.cancelRename();
			}
		});
		this.name.addMouseListener(new MouseListener() {

			public void mouseClicked(final MouseEvent e) {
				BoxShapedComponent.this.processEvent(e);
			}

			public void mouseEntered(final MouseEvent e) {
				BoxShapedComponent.this.processEvent(e);
			}

			public void mouseExited(final MouseEvent e) {
				BoxShapedComponent.this.processEvent(e);
			}

			public void mousePressed(final MouseEvent e) {
				BoxShapedComponent.this.processEvent(e);
			}

			public void mouseReleased(final MouseEvent e) {
				BoxShapedComponent.this.processEvent(e);
			}

		});
		this.deselect();
	}

	/**
	 * Call this method to make the component go into rename behaviour state.
	 */
	public void startRename() {
		this.beingRenamed = true;
		this.name.setText(this.getEditableName());
		this.name.setBorder(BorderFactory
				.createLineBorder(BoxShapedComponent.RENAMING_BORDER_COLOUR));
		this.name.setEditable(true);
		this.name.setEnabled(true);
		this.name.setOpaque(true);
		this.name.requestFocus();
	}

	/**
	 * Call this method to take the component out of rename behaviour state and
	 * revert to the state it was before, without accepting any changes the user
	 * may already have typed.
	 */
	public void cancelRename() {
		this.beingRenamed = false;
		this.name.setBorder(BorderFactory.createEmptyBorder());
		this.name.setEditable(false);
		this.name.setEnabled(false);
		this.name.setOpaque(this.isSelected());
		this.name.setText(this.getDisplayName());
	}

	/**
	 * Obtain the display name for this box-shaped object, to display above the
	 * box (as opposed to the editable name).
	 * 
	 * @return the display name.
	 */
	public abstract String getDisplayName();

	/**
	 * Returns the name the user can edit. This can be different from the name
	 * that is displayed at other times when the component is not being renamed.
	 * 
	 * @return the editable name.
	 */
	public String getEditableName() {
		return this.getDisplayName();
	}

	private void doRename() {
		this.performRename(this.name.getText());
		this.cancelRename();
	}

	/**
	 * Override this method to be informed when the user has completed a rename
	 * and wishes to enforce the new name.
	 * 
	 * @param newName
	 *            the new name entered by the user.
	 */
	public void performRename(final String newName) {
	}

	/**
	 * Is this component currently in rename behaviour state?
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isBeingRenamed() {
		return this.beingRenamed;
	}

	/**
	 * Is this component currently in select behaviour state?
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isSelected() {
		return this.selected;
	}

	public void updateAppearance() {
		final DiagramContext mod = this.getDiagram().getDiagramContext();
		if (mod != null) {
			if (this.getObject().isHidden())
				if (this instanceof ColumnComponent) {
					final TableComponent parent = (TableComponent) SwingUtilities
							.getAncestorOfClass(TableComponent.class, this);
					if (parent != null && parent.isHidingMaskedCols()) {
						this.setVisible(false);
						return;
					}
				} 
			this.setVisible(true);
			mod.customiseAppearance(this, this.getObject());
		}
		if (this.indexed)
			this.stroke = BoxShapedComponent.INDEXED_OUTLINE;
		else if (this.restricted)
			this.stroke = this.compounded ? BoxShapedComponent.DOTTED_DASHED_OUTLINE
					: BoxShapedComponent.DASHED_OUTLINE;
		else
			this.stroke = this.compounded ? BoxShapedComponent.DOTTED_OUTLINE
					: BoxShapedComponent.OUTLINE;
		this.setBorder(BorderFactory.createLineBorder(
				this.object.isVisibleModified()
					? DiagramComponent.GLOW_COLOUR : this.getForeground(), 1));
		if(this.object.isVisibleModified()) {
			System.out.println();
		}
	}
	
	public MartConfiguratorObject getObject() {
		return this.object;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
		this.color4Mouse = this.getBackground();
		this.setBackground(Color.WHITE);
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
		this.setBackground(this.color4Mouse);
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
}
