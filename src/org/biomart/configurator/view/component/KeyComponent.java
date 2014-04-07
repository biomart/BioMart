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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.view.gui.diagrams.Diagram;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.PrimaryKey;

/**
 * Represents a key by listing out in a label each column in the key.
 * <p>
 * Drag-and-drop code courtesy of <a
 * href="http://www.javaworld.com/javaworld/jw-03-1999/jw-03-dragndrop.html?page=1">JavaWorld</a>.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.28 $, $Date: 2007/11/09 11:36:28 $, modified by
 *          $Author: rh4 $
 * @since 0.5
 */
public class KeyComponent extends BoxShapedComponent {
	private static final long serialVersionUID = 1;

	private static final Color PK_BACKGROUND_COLOUR = Color.CYAN;

	private static final Color FK_BACKGROUND_COLOUR = Color.YELLOW;

	/**
	 * Constant referring to handmade key colour.
	 */
	public static Color HANDMADE_COLOUR = Color.GREEN;

	/**
	 * Constant referring to incorrect key colour.
	 */
	public static Color INCORRECT_COLOUR = Color.LIGHT_GRAY;

	/**
	 * Constant referring to normal key colour.
	 */
	public static Color NORMAL_COLOUR = Color.DARK_GRAY;

	private static final Font PLAIN_FONT = Font.decode("SansSerif-PLAIN-10");

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	private final PropertyChangeListener repaintListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent e) {
			KeyComponent.this.needsRepaint = true;
		}
	};

	private final PropertyChangeListener recalcListener = new PropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent e) {
			KeyComponent.this.needsRecalc = true;
		}
	};

	private BufferedImage image = null;

	private final Rectangle rect2D = new Rectangle();

	/**
	 * The constructor constructs a key component around a given key object, and
	 * associates it with the given display.
	 * 
	 * @param key
	 *            the key to represent.
	 * @param diagram
	 *            the diagram to draw the key on.
	 */
	public KeyComponent(final Key key, final Diagram diagram) {
		super(key, diagram);

		// Key components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each column in the key.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(0, 1, 0, 2);

		// Create the background colour.
		if (key instanceof PrimaryKey)
			this.setBackground(KeyComponent.PK_BACKGROUND_COLOUR);
		else
			this.setBackground(KeyComponent.FK_BACKGROUND_COLOUR);

		// Calculate the component layout.
		this.recalculateDiagramComponent();

		// Set up drag-and-drop capabilities.
		final DragSource dragSource = DragSource.getDefaultDragSource();
		final DragSourceListener dsListener = new DragSourceListener() {
			public void dragEnter(DragSourceDragEvent e) {
				KeyComponent.this.paintImage(e.getLocation());
				DragSourceContext context = e.getDragSourceContext();
				int myaction = e.getDropAction();
				if ((myaction & DnDConstants.ACTION_LINK) != 0)
					context.setCursor(DragSource.DefaultLinkDrop);
				else
					context.setCursor(DragSource.DefaultLinkNoDrop);
			}

			public void dragDropEnd(DragSourceDropEvent e) {
				KeyComponent.this.clearImage();
			}

			public void dragExit(DragSourceEvent dse) {
				KeyComponent.this.paintImage(dse.getLocation());
				DragSourceContext context = dse.getDragSourceContext();
				context.setCursor(DragSource.DefaultLinkNoDrop);
			}

			public void dragOver(DragSourceDragEvent dsde) {
				this.dragEnter(dsde);
			}

			public void dropActionChanged(DragSourceDragEvent dsde) {
				this.dragEnter(dsde);
			}
		};
		final DragGestureListener dgListener = new DragGestureListener() {
			public void dragGestureRecognized(DragGestureEvent e) {
				if (KeyComponent.this.isDraggable())
					try {
						Transferable transferable = new KeyTransferable(
								((KeyComponent) e.getComponent()).getKey());
						JComponent lbl = (JComponent) e.getComponent();
						KeyComponent.this.image = new BufferedImage(lbl
								.getWidth(), lbl.getHeight(),
								BufferedImage.TYPE_INT_ARGB_PRE);
						Graphics2D graphics = KeyComponent.this.image
								.createGraphics();
						graphics.setComposite(AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, 0.5f));
						lbl.paint(graphics);
						graphics.dispose();
						e.startDrag(DragSource.DefaultLinkNoDrop,
								KeyComponent.this.image, new Point(0, 0),
								transferable, dsListener);
					} catch (final Throwable t) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								StackTrace.showStackTrace(t);
							}
						});
					}
			}
		};
		final DropTargetListener dtListener = new DropTargetListener() {
			public void dragEnter(DropTargetDragEvent e) {
				if (this.isDragOk(e) == false) {
					e.rejectDrag();
					return;
				}
				e.acceptDrag(DnDConstants.ACTION_LINK);
			}

			public void dragOver(DropTargetDragEvent e) {
				if (this.isDragOk(e) == false) {
					e.rejectDrag();
					return;
				}
				e.acceptDrag(DnDConstants.ACTION_LINK);
			}

			public void dropActionChanged(DropTargetDragEvent e) {
				if (this.isDragOk(e) == false) {
					e.rejectDrag();
					return;
				}
				e.acceptDrag(DnDConstants.ACTION_LINK);
			}

			public void dragExit(DropTargetEvent e) {
			}

			private boolean isDragOk(DropTargetDragEvent e) {
				DataFlavor[] flavors = KeyTransferable.flavors;
				DataFlavor chosen = null;
				for (int i = 0; i < flavors.length; i++)
					if (e.isDataFlavorSupported(flavors[i])) {
						chosen = flavors[i];
						break;
					}
				if (chosen == null)
					return false;
				int sa = e.getSourceActions();
				if ((sa & DnDConstants.ACTION_LINK) == 0)
					return false;
				return true;
			}

			public void drop(DropTargetDropEvent e) {
				DataFlavor[] flavors = KeyTransferable.flavors;
				DataFlavor chosen = null;
				for (int i = 0; i < flavors.length; i++)
					if (e.isDataFlavorSupported(flavors[i])) {
						chosen = flavors[i];
						break;
					}
				if (chosen == null) {
					e.rejectDrop();
					return;
				}
				int sa = e.getSourceActions();
				if ((sa & DnDConstants.ACTION_LINK) == 0) {
					e.rejectDrop();
					return;
				}
				Object data = null;
				try {
					data = e.getTransferable().getTransferData(chosen);
					if (data instanceof Key) {
						final Key sourceKey = (Key) data;
						final Key targetKey = KeyComponent.this.getKey();
/*						if (!sourceKey.equals(targetKey)) {
							KeyComponent.this.getDiagram().getMartTab()
									.getSchemaTabSet().requestCreateRelation(
											sourceKey, targetKey);
							e.acceptDrop(DnDConstants.ACTION_LINK);
							e.dropComplete(true);
						}*/
					}
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
					e.rejectDrop();
					e.dropComplete(false);
				}
			}
		};
		new DropTarget(this, DnDConstants.ACTION_LINK, dtListener, true);
		dragSource.createDefaultDragGestureRecognizer(this,
				DnDConstants.ACTION_LINK, dgListener);
	}

	private final void paintImage(final Point pt) {
		SwingUtilities.convertPointFromScreen(pt, this.getDiagram());
		this.getDiagram().paintImmediately(this.rect2D.getBounds());
		this.rect2D.setRect((int) pt.getX(), (int) pt.getY(), this.image
				.getWidth(), this.image.getHeight());
		this.getDiagram().getGraphics().drawImage(this.image, (int) pt.getX(),
				(int) pt.getY(), this);
	}

	private final void clearImage() {
		this.getDiagram().paintImmediately(this.rect2D.getBounds());
	}

	private Key getKey() {
		return (Key) this.getObject();
	}

	public String getDisplayName() {
		return "";
	}

	protected void doRecalculateDiagramComponent() {
		// Calculate new label.
		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < this.getKey().getColumns().size(); i++) {
			if (i > 0)
				sb.append(", ");
			final Column column = this.getKey().getColumns().get(i);
			sb.append(column instanceof DatasetColumn ? ((DatasetColumn) column)
							.getName()
							: column.getName());
		}

		// Add the label.
		final JLabel label = new JLabel(sb.toString());
		label.setFont(KeyComponent.PLAIN_FONT);
		this.layout.setConstraints(label, this.constraints);
		this.add(label);

	}

	private static class KeyTransferable implements Transferable {

		private static final DataFlavor keyFlavor = new DataFlavor(Key.class,
				"MartBuilder Schema Key") {
			private static final long serialVersionUID = 1L;
		};

		private static final DataFlavor[] flavors = { KeyTransferable.keyFlavor };

		private static final List flavorList = Arrays
				.asList(KeyTransferable.flavors);

		public synchronized DataFlavor[] getTransferDataFlavors() {
			return KeyTransferable.flavors;
		}

		public boolean isDataFlavorSupported(final DataFlavor flavor) {
			return KeyTransferable.flavorList.contains(flavor);
		}

		private Key key;

		private KeyTransferable(final Key key) {
			this.key = key;
		}

		public Object getTransferData(final DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (KeyTransferable.keyFlavor.equals(flavor))
				return this.key;
			else
				throw new UnsupportedFlavorException(flavor);
		}
	}
}
