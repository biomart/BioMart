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

package org.biomart.common.view.gui;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.Autoscroll;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * A {@link JTree} that allows drag-and-drop and autoscroll when doing so.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.1 $, $Date: 2007/11/09 11:36:28 $, modified by
 *          $Author: rh4 $
 * @since 0.7
 */
public abstract class DraggableJTree extends JTree implements Autoscroll,
		DragSourceListener, DragGestureListener, DropTargetListener {
	private static final long serialVersionUID = 1L;

	private int margin = 12;

	private TreePath transferStartPath;

	private TreePath transferStopPath;

	private boolean dragValid = false;

	private final DragSource source = new DragSource();

	private final DropTarget target = new DropTarget(this, this);

	private final DragGestureRecognizer recognizer = this.source
			.createDefaultDragGestureRecognizer(this,
					DnDConstants.ACTION_COPY_OR_MOVE, this);

	private BufferedImage image = null;

	private final Rectangle rect2D = new Rectangle();

	/**
	 * See {@link JTree#JTree()}.
	 */
	public DraggableJTree() {
		super();
		// To prevent unused-variable warnings.
		if (this.target == null || this.recognizer == null) {
		}
	}

	/**
	 * See {@link JTree#JTree(Hashtable)}.
	 * 
	 * @param value
	 */
	public DraggableJTree(final Hashtable value) {
		super(value);
	}

	/**
	 * See {@link JTree#JTree(Object[])}.
	 * 
	 * @param value
	 */
	public DraggableJTree(final Object[] value) {
		super(value);
	}

	/**
	 * See {@link JTree#JTree(TreeModel)}.
	 * 
	 * @param newModel
	 */
	public DraggableJTree(final TreeModel newModel) {
		super(newModel);
	}

	/**
	 * See {@link JTree#JTree(TreeNode, boolean)}.
	 * 
	 * @param root
	 * @param asksAllowsChildren
	 */
	public DraggableJTree(final TreeNode root, final boolean asksAllowsChildren) {
		super(root, asksAllowsChildren);
	}

	/**
	 * See {@link JTree#JTree(TreeNode)}.
	 * 
	 * @param root
	 */
	public DraggableJTree(final TreeNode root) {
		super(root);
	}

	/**
	 * See {@link JTree#JTree(Vector)}.
	 * 
	 * @param value
	 */
	public DraggableJTree(final Vector value) {
		super(value);
	}

	/**
	 * Set the auto-scroll detection margin size.
	 * 
	 * @param margin
	 *            the size.
	 */
	public void setMargin(final int margin) {
		this.margin = margin;
	}

	/**
	 * Get the auto-scroll detection margin size.
	 * 
	 * @return the size.
	 */
	public int getMargin() {
		return this.margin;
	}

	public void autoscroll(final Point p) {
		int realrow = this.getRowForLocation(p.x, p.y);
		final Rectangle outer = this.getBounds();
		realrow = p.y + outer.y <= this.margin ? realrow < 1 ? 0 : realrow - 1
				: realrow < this.getRowCount() - 1 ? realrow + 1 : realrow;
		this.scrollRowToVisible(realrow);
	}

	public Insets getAutoscrollInsets() {
		final Rectangle outer = this.getBounds();
		final Rectangle inner = this.getParent().getBounds();
		return new Insets(inner.y - outer.y + this.margin, inner.x - outer.x
				+ this.margin, outer.height - inner.height - inner.y + outer.y
				+ this.margin, outer.width - inner.width - inner.x + outer.x
				+ this.margin);
	}

	public void dragDropEnd(final DragSourceDropEvent dsde) {
		if (dsde.getDropSuccess())
			this.dragCompleted(dsde.getDropAction(), this.transferStartPath,
					this.transferStopPath);
	}

	public void dragEnter(final DragSourceDragEvent dsde) {
		this.updateCursor(dsde);
	}

	public void dragExit(final DragSourceEvent dse) {
		dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
	}

	public void dragOver(final DragSourceDragEvent dsde) {
		this.updateCursor(dsde);
	}

	private void updateCursor(final DragSourceDragEvent dsde) {
		final int action = dsde.getDropAction();
		if (action == DnDConstants.ACTION_COPY)
			dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
		else if (action == DnDConstants.ACTION_MOVE)
			dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
		else
			dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
	}

	public void dropActionChanged(final DragSourceDragEvent dsde) {
		this.updateCursor(dsde);
	}

	public void dragGestureRecognized(final DragGestureEvent dge) {
		final TreePath path = this.getSelectionPath();
		// Is path draggable? Abstract method will check for us.
		// We can't move an empty selection.
		if (path == null || !this.isValidDragPath(path))
			return;
		final Rectangle pathBounds = this.getPathBounds(path);
		final JComponent lbl = (JComponent) this.getCellRenderer()
				.getTreeCellRendererComponent(
						this,
						path.getLastPathComponent(),
						false,
						this.isExpanded(path),
						((DefaultTreeModel) this.getModel()).isLeaf(path
								.getLastPathComponent()), 0, false);
		lbl.setBounds(pathBounds);
		this.image = new BufferedImage(lbl.getWidth(), lbl.getHeight(),
				BufferedImage.TYPE_INT_ARGB_PRE);
		final Graphics2D graphics = this.image.createGraphics();
		graphics.setComposite(AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, 0.5f));
		lbl.paint(graphics);
		graphics.dispose();
		this.transferStartPath = path;
		this.source.startDrag(dge, DragSource.DefaultMoveNoDrop, this.image,
				new Point(0, 0), new StringSelection(
						"MartRunner JobPlan Tree Drag"), this);
	}

	private TreePath getPathForEvent(final DropTargetDragEvent dtde) {
		final Point p = dtde.getLocation();
		final DropTargetContext dtc = dtde.getDropTargetContext();
		final JTree tree = (JTree) dtc.getComponent();
		return tree.getClosestPathForLocation(p.x, p.y);
	}

	private TreePath getPathForEvent(final DropTargetDropEvent dtde) {
		final Point p = dtde.getLocation();
		final DropTargetContext dtc = dtde.getDropTargetContext();
		final JTree tree = (JTree) dtc.getComponent();
		return tree.getClosestPathForLocation(p.x, p.y);
	}

	public void dragEnter(final DropTargetDragEvent dtde) {
		this.updateDrag(dtde);
	}

	public void dragExit(final DropTargetEvent dte) {
		this.clearImage();
	}

	public void dragOver(final DropTargetDragEvent dtde) {
		this.updateDrag(dtde);
	}

	private void updateDrag(final DropTargetDragEvent dtde) {
		this.paintImage(dtde.getLocation());
		// Is path droppable? Abstract method will check for us.
		this.dragValid = this.isValidDropPath(this.getPathForEvent(dtde));
		if (!this.dragValid)
			dtde.rejectDrag();
		else
			dtde.acceptDrag(dtde.getDropAction());
	}

	public void drop(final DropTargetDropEvent dtde) {
		try {
			this.clearImage();
			this.transferStopPath = this.getPathForEvent(dtde);
			// Is path droppable? Abstract method will check for us.
			if (!this.isValidDropPath(this.transferStopPath)) {
				dtde.rejectDrop();
				dtde.dropComplete(false);
				return;
			}
			// Complete the drag.
			final Transferable tr = dtde.getTransferable();
			final DataFlavor[] flavors = tr.getTransferDataFlavors();
			for (int i = 0; i < flavors.length; i++)
				if (tr.isDataFlavorSupported(flavors[i])) {
					dtde.acceptDrop(dtde.getDropAction());
					dtde.dropComplete(true);
					return;
				}
			dtde.rejectDrop();
		} catch (final Throwable t) {
			t.printStackTrace();
			dtde.rejectDrop();
			dtde.dropComplete(false);
		}
	}

	private final void paintImage(final Point pt) {
		this.paintImmediately(this.rect2D.getBounds());
		this.rect2D.setRect((int) pt.getX(), (int) pt.getY(), this.image
				.getWidth(), this.image.getHeight());
		this.getGraphics().drawImage(this.image, (int) pt.getX(),
				(int) pt.getY(), this);
	}

	private final void clearImage() {
		this.paintImmediately(this.rect2D.getBounds());
	}

	public void dropActionChanged(final DropTargetDragEvent dtde) {
		this.updateDrag(dtde);
	}

	/**
	 * Check to see if the given path is a valid starting point.
	 * 
	 * @param path
	 *            the path to the node being dragged.
	 * @return <tt>true</tt> if it can be dragged.
	 */
	public abstract boolean isValidDragPath(final TreePath path);

	/**
	 * Check to see if the given path is a valid stopping point.
	 * 
	 * @param path
	 *            the path to the node being dropped onto.
	 * @return <tt>true</tt> if it can be dropped onto.
	 */
	public abstract boolean isValidDropPath(final TreePath path);

	/**
	 * Drag has completed.
	 * 
	 * @param action
	 *            the type of drag.
	 * @param from
	 *            what was dragged.
	 * @param to
	 *            where it was dragged to.
	 */
	public abstract void dragCompleted(final int action, final TreePath from,
			final TreePath to);
}
