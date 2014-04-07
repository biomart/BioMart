package org.biomart.configurator.view.component.container;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicButtonUI;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.XMLAttributeTableModel;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.configurator.view.menu.ContextMenuConstructor;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.UserGroup;

import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

/**
 * Component to be used as tabComponent; Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 */
public class ButtonTabComponent extends JPanel implements MouseListener,
		PropertyChangeListener, TableModelListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final DnDTabbedPane pane;
	private boolean titleEnabled;
	private JLabel label;
	private final GuiContainer gc;
	private JButton button;

	public ButtonTabComponent(final DnDTabbedPane pane, final GuiContainer gc) {
		// unset default FlowLayout' gaps
		super(new BorderLayout());
		this.gc = gc;
		if (pane == null) {
			throw new NullPointerException("TabbedPane is null");
		}
		this.pane = pane;
		setOpaque(false);

		// make JLabel read titles from JTabbedPane
		label = new JLabel() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public String getText() {
				int i = pane.indexOfTabComponent(ButtonTabComponent.this);
				if (i != -1) {
					return pane.getTitleAt(i);
				}
				return "";
			}
		};

		add(label, BorderLayout.CENTER);
		// add more space between the label and the button
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		// tab button
		button = new TabButton();
		add(button, BorderLayout.EAST);
		// add more space to the top of the component
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		this.addMouseListener(this);
		// this.setComponentPopupMenu(ContextMenuConstructor.getInstance().getContextMenu(this,
		// "guicontainer"));
		DragGestureListener dgl = new DragGestureListener() {
            public void dragGestureRecognized(DragGestureEvent e) {
            	int i = pane.indexOfTabComponent(ButtonTabComponent.this);
                getParentTabPane().setDragTabIndex(i);
                //"disabled tab problem".
                if(i<0 || !getParentTabPane().isEnabledAt(i)) return;
                getParentTabPane().initGlassPane(e.getComponent(), e.getDragOrigin());
                try{
                    e.startDrag(DragSource.DefaultMoveDrop, getParentTabPane().getTransferable(),
                    		getParentTabPane().getDragSourceListener());
                }catch(InvalidDnDOperationException idoe) {
                    idoe.printStackTrace();
                }
            }
        };
		new DragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dgl);
	}

	public DnDTabbedPane getParentTabPane() {
		return this.pane;
	}

	public void setEnableTitle(boolean b) {
		this.titleEnabled = b;
		if (b)
			label.setForeground(Color.BLACK);
		else
			label.setForeground(Color.GRAY);
	}

	private class TabButton extends JButton implements ActionListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public TabButton() {
			int size = 17;
			setPreferredSize(new Dimension(size, size));
			setToolTipText("close this tab");
			// Make the button looks the same for all Laf's
			setUI(new BasicButtonUI());
			// Make it transparent
			setContentAreaFilled(false);
			// No need to be focusable
			setFocusable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(false);
			// Making nice rollover effect
			// we use the same listener for all buttons
			addMouseListener(buttonMouseListener);
			setRolloverEnabled(true);
			// Close the proper tab by clicking the button
			addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			/*
			 * int i = pane.indexOfTabComponent(ButtonTabComponent.this); if (i
			 * != -1) { int n = JOptionPane.showConfirmDialog( null,
			 * "Delete the tab and its sub components?", "Question",
			 * JOptionPane.YES_NO_OPTION); if(n == 0) { //don't select the last
			 * tab + if(i==pane.getTabCount()-2 && i>0) { ChangeListener
			 * listener = pane.getChangeListener();
			 * pane.removeChangeListener(listener); pane.remove(i);
			 * pane.setSelectedIndex(i-1); pane.addChangeListener(listener);
			 * }else if(i==pane.getTabCount()-2 && i==0) { pane.remove(1);
			 * pane.remove(0); }else pane.remove(i); } }
			 */
		}

		// we don't want to update UI for this button
		public void updateUI() {
		}

		// paint the cross
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			// shift the image for pressed buttons
			if (getModel().isPressed()) {
				g2.translate(1, 1);
			}
			g2.setStroke(new BasicStroke(2));
			UserGroup tmpUser = ((McViewPortal) McViews.getInstance().getView(
					IdwViewType.PORTAL)).getUser();
			if (gc.isActivatedInUser(tmpUser))
				g2.setColor(Color.GREEN);
			else
				g2.setColor(Color.GRAY);
			Shape s = new Rectangle(getWidth() - 4, getHeight() - 4);
			g2.draw(s);
			g2.fill(s);
			g2.dispose();
		}
	}

	private final static MouseListener buttonMouseListener = new MouseAdapter() {
		public void mouseEntered(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(true);
			}
		}

		public void mouseExited(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(false);
			}
		}
	};

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// super.processMouseEvent(e);

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		// super.processMouseEvent(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {

		int i = pane.indexOfTabComponent(ButtonTabComponent.this);
		if (i != -1) {
			pane.setSelectedIndex(i);
		}
		if (e.isPopupTrigger()) {
			JPopupMenu menu = ContextMenuConstructor.getInstance()
					.getContextMenu(this, "guicontainer",false);
			menu.show(e.getComponent(), e.getX(), e.getY());
		} // else
			// super.processMouseEvent(e);

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			JPopupMenu menu = ContextMenuConstructor.getInstance()
					.getContextMenu(this, "guicontainer",false);
			menu.show(e.getComponent(), e.getX(), e.getY());
		}// else
			// super.processMouseEvent(e);

	}

	public JButton getButton() {
		return this.button;
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if(e.getNewValue() instanceof Boolean) {
			Boolean b = (Boolean)e.getNewValue();
			
			int i = pane.indexOfTabComponent(ButtonTabComponent.this);
			if (i != -1) {
				String name = pane.getTitleAt(i);
				boolean hasConfig = (McGuiUtils.INSTANCE.getRegistryObject().getPortal().getRootGuiContainer().getAllMartPointerListResursively().size()>0);
				if(name.equals("report") && !hasConfig){
					//special case for report enabling
					if(b.booleanValue()){
						this.setEnableTitle(false);
					}else{
						this.setEnableTitle(b.booleanValue());
					}
				}else{
					this.setEnableTitle(b.booleanValue());
				}
			}
			
		}
		
		this.revalidate();
	}

	public GuiContainer getGuiContainer() {
		return this.gc;
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		XMLAttributeTableModel model = (XMLAttributeTableModel) e.getSource();
		if (model.getValueAt(e.getFirstRow(), 0).equals(
				XMLElements.DISPLAYNAME.toString())) {
			int i = pane.indexOfTabComponent(ButtonTabComponent.this);
			if (i != -1) {
				pane.setTitleAt(
						i,
						(String) model.getValueAt(e.getFirstRow(),
								e.getColumn()));
				this.revalidate();
			}
		}

	}
}
