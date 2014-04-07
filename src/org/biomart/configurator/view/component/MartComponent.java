package org.biomart.configurator.view.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.EtchedBorder;

import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.model.SourceTransferable;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.component.container.GuiContainerPanel;
import org.biomart.configurator.view.gui.dialogs.DatasourceDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.configurator.view.menu.ContextMenuConstructor;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.MartPointer;



public class MartComponent extends JPanel implements MouseListener, Transferable, DragGestureListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Mart mart;
	private JLabel displayNameField;
	private JLabel iconLabel;
	private boolean selected;
	

	public MartComponent(Mart mart) {
		this.mart = mart;
		this.init();
	}
	
	private void init() {
		FlowLayout fo = new FlowLayout();
		this.setLayout(fo);
		fo.setAlignment(FlowLayout.LEFT);
		iconLabel = new JLabel();
		displayNameField = new JLabel(this.mart.getDisplayName());
		this.add(iconLabel);
		this.add(displayNameField);
		this.refreshIcon();
		this.addMouseListener(this);
		DragSource ds = new DragSource();
	    ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);
		this.setBorder(BorderFactory.createLineBorder(this.getBackground()));
		this.setOpaque(true);
	}

	public Mart getMart() {
		return this.mart;
	}

	public void refreshIcon() {
		String iconPath = "images/source.gif";
		this.iconLabel.setIcon(McUtils.createImageIcon(iconPath));
		this.revalidate();
	}


	public void clean() {
		this.mart = null;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		McViewPortal portalView = ((McViewPortal) McViews
				.getInstance().getView(IdwViewType.PORTAL));
		this.setBackground(Color.YELLOW);

		GuiContainerPanel gcp = portalView.getSelectedGcPanel();
		gcp.setHighlight(this.getMart());
		/*
		this.setCursor(new Cursor(Cursor.HAND_CURSOR));
		Set<ConfigComponent> cclist = portalView
				.getComponentListByMart(this.getMart());
		for (ConfigComponent cc : cclist) {
			cc.setBackground(Color.YELLOW);
		}
		Map<MartPointer,Set<Mart>> configMartMap = portalView.getMPMartMap();
		for(Map.Entry<MartPointer, Set<Mart>> entry: configMartMap.entrySet()) {
			if(entry.getValue().contains(this.getMart())) {
				ConfigComponent cc = portalView.getComponentByMartPointer(entry.getKey());
				cc.setBackground(Color.CYAN);
			}
		}*/
	}

	@Override
	public void mouseExited(MouseEvent e) {
		McViewPortal portalView = ((McViewPortal) McViews
				.getInstance().getView(IdwViewType.PORTAL));
		this.setBackground(this.getParent().getBackground());
		/*this.setCursor(new Cursor(Cursor.HAND_CURSOR));
		Set<ConfigComponent> cclist = portalView
				.getComponentListByMart(this.getMart());
		for (ConfigComponent cc : cclist) {
			cc.setBackground(this.getParent().getBackground());
		}
		Map<MartPointer,Set<Mart>> configMartMap = portalView.getMPMartMap();
		for(Map.Entry<MartPointer, Set<Mart>> entry: configMartMap.entrySet()) {
			if(entry.getValue().contains(this.getMart())) {
				ConfigComponent cc = portalView.getComponentByMartPointer(entry.getKey());
				cc.setBackground(this.getParent().getBackground());
			}
		}*/
		GuiContainerPanel gcp = portalView.getSelectedGcPanel();
		gcp.setHighlight(null);
	}

	@Override
	public void mousePressed(MouseEvent e) {	
		/* clear all when 1. not meta down
		 * 2. popup
		 * 3. already selected
		 * 
		 */
		if(!(e.isMetaDown() || (e.isPopupTrigger() && this.isSelected()) || e.isShiftDown() || ((MartComponent)e.getSource()).isSelected())) {
			this.clearOthers((Component)e.getSource());
		}
		if(!this.isSelected() && e.getButton() == MouseEvent.BUTTON3){
			this.clearOthers((Component)e.getSource());
		}

		McViewSourceGroup groupview = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		if(e.isShiftDown() && groupview.getLastpoint()!=null) {
			this.clearOthers(this);
			List<MartComponent> mcs = groupview.getComponentsBetween(groupview.getLastpoint(),this);
			for(MartComponent mc: mcs) {
				mc.setSelected(true);
				mc.setBorder(BorderFactory.createLineBorder(Color.RED));
			}
		} else
			groupview.setLastpoint(this);
		this.setSelected(true);
		this.setBorder(BorderFactory.createLineBorder(Color.RED));
		

		if(e.isPopupTrigger()) {
			this.handleRightClick(e);
		}
		else if(e.getClickCount() == 2) {	
			List<Mart> martList = new ArrayList<Mart>();
			martList.add(((MartComponent)e.getSource()).getMart());
			// added cols orders to a new PtModel
			ArrayList<Integer> cols = new ArrayList<Integer>();
			cols.add(PartitionUtils.DATASETNAME);
			cols.add(PartitionUtils.DISPLAYNAME);
			cols.add(PartitionUtils.CONNECTION);
			cols.add(PartitionUtils.DATABASE);
			cols.add(PartitionUtils.SCHEMA);
			cols.add(PartitionUtils.HIDE);
			cols.add(PartitionUtils.KEY);
			new DatasourceDialog(martList, cols, !this.getMart().getSourceContainer().isGrouped());

		} 
		this.revalidate();
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		/* clear all when 1. not meta down
		 * 2. popup
		 * 3. already selected
		 * 
		 */
		if(!(e.isMetaDown() || (e.isPopupTrigger() && this.isSelected()) || e.isShiftDown())) {
			this.clearOthers((Component)e.getSource());
		}
		if(e.isPopupTrigger())
			this.handleRightClick(e);		
		
	}

	private void handleRightClick(MouseEvent e) {
		this.mouseExited(null);
		JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this,"mart",this.getSelectedComponents().size()>1);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	@Override
	public Object getTransferData(DataFlavor e)
			throws UnsupportedFlavorException, IOException {
		if(e == DataFlavor.stringFlavor) {
        	Container container = SwingUtilities.getAncestorOfClass(org.biomart.configurator.view.idwViews.McViewSourceGroup.class, this);
    		if(container != null) {
    			McViewSourceGroup sgview = (McViewSourceGroup)container;
    			List<Mart> martList = sgview.getSelectedMarts();
    			List<String> marts = new ArrayList<String>();
    			for(Mart mart: martList) {
    				marts.add(mart.getName());
    			}
    			if(!marts.isEmpty()) {
            		return McUtils.StrListToStr(marts, ",");        		
            	}
    		}
			return null;
		}
		else
			return null;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] result =  {DataFlavor.stringFlavor};
		return result;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor df) {
		return DataFlavor.stringFlavor.equals(df);
	}
	

	
	private void clearOthers(Component comp) {
		Container c = SwingUtilities.getAncestorOfClass(org.biomart.configurator.view.idwViews.McViewSourceGroup.class, comp);
		if(c != null) {
			McViewSourceGroup sgview = (McViewSourceGroup)c;
			sgview.unselectOthers((MartComponent)comp);
		}
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}
	
	@Override
	public String toString() {
		return this.mart.toString();
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		dge.startDrag(DragSource.DefaultCopyDrop, this);
	}
	
	public List<Mart> getSelectedComponents() {
		List<Mart> martList = new ArrayList<Mart>();
    	Container container = SwingUtilities.getAncestorOfClass(org.biomart.configurator.view.idwViews.McViewSourceGroup.class, this);
		if(container != null) {
			McViewSourceGroup sgview = (McViewSourceGroup)container;
			
			for(Mart mart: sgview.getSelectedMarts()) {
				martList.add(mart);
			}
		}
		return martList;
	}
}