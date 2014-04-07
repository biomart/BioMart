package org.biomart.configurator.view.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.gui.dialogs.ConfigDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.configurator.view.menu.ContextMenuConstructor;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;
import org.biomart.configurator.view.dnd.ConfigDnDTransferHandler;
import org.biomart.configurator.view.idwViews.*;

public class ConfigComponent extends JPanel implements MouseListener, ClipboardOwner, 
	PropertyChangeListener, Transferable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MartPointer mp;
	private JTextField displayNameField;
	private JLabel iconLabel;
	private int cutcopyOperation = 0; //1 = cut, 2 = copy, 0 = n/a
	private static DataFlavor CONFIG_FLAVOR = null;
	
	public ConfigComponent(MartPointer mp, UserGroup user) {
		this.mp = mp;
		init(user);
		getDataFlavor();
	}
	
	private void init(UserGroup user) {
		this.setLayout(new BorderLayout());
		this.setBorder(new EtchedBorder());

		iconLabel = new JLabel();
		displayNameField = new JTextField(this.mp.getConfig().getDisplayName());
		//displayNameField.setEnabled(false);
		displayNameField.setBackground(this.getBackground());
		displayNameField.setBorder(null);
		displayNameField.setDropTarget(null);
		displayNameField.addKeyListener(new KeyAdapter() {
	        public void keyPressed(KeyEvent e) {
	            int key = e.getKeyCode();
	            if (key == KeyEvent.VK_ENTER) {
	            	changeDisplayName();
	            }
	        }
		});
		this.add(iconLabel,BorderLayout.CENTER);
		this.add(displayNameField,BorderLayout.SOUTH);
		this.refreshIcon(user);
		this.addMouseListener(this);
		this.setTransferHandler(new ConfigDnDTransferHandler());
	}

	public MartPointer getMartPointer() {
		return this.mp;
	}

	public static DataFlavor getDataFlavor()  {
		if(CONFIG_FLAVOR==null)
			try {
				CONFIG_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=org.biomart.configurator.view.component.ConfigComponent");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return CONFIG_FLAVOR;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		this.setBackground(Color.YELLOW);
		this.setCursor(new Cursor(Cursor.HAND_CURSOR));
		//set treeview highlight
		McViewSourceGroup groupView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		groupView.setHighlight(this.mp.getMart() , Color.YELLOW);
		//set others to 
		Set<Mart> martSet = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getMPMartMap().get(this.mp);
		for(Mart mart: martSet) {
			//set tree highlights
			if(mart.equals(this.mp.getMart()))
				continue;
			groupView.setHighlight(mart , Color.CYAN);
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		this.setBackground(this.getParent().getBackground());
		this.setCursor(Cursor.getDefaultCursor());
		
		McViewSourceGroup groupView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		
		groupView.setHighlight(this.mp.getMart(), this.getParent().getBackground());

		//set others to 
		Set<Mart> martSet = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getMPMartMap().get(this.mp);
		for(Mart mart: martSet) {
			//set tree highlights			
			groupView.setHighlight(mart , this.getParent().getBackground());			
		}

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.getClickCount() == 2) {			
			new ConfigDialog(mp.getConfig());
		}
		else if(e.isPopupTrigger())
			this.handleMouseClick(e);	
		else {
			if(McGuiUtils.INSTANCE.isWindows()) {
				if(e.getModifiers() == InputEvent.BUTTON1_MASK)
					exportDrag(e);
			} else 
				exportDrag(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isPopupTrigger())
			this.handleMouseClick(e);			
	}
	
	private void exportDrag(MouseEvent e) {
        JComponent c = (JComponent) e.getSource();
        TransferHandler handler = c.getTransferHandler();
        handler.exportAsDrag(c, e, TransferHandler.COPY);		
	}
	

	private void handleMouseClick(MouseEvent e) {
		this.mouseExited(null);
		JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this,"config",false);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// TODO Auto-generated method stub
		
	}

	private void changeDisplayName() {
		String newName = this.displayNameField.getText();
		String oldName = this.mp.getConfig().getDisplayName();
		if(!oldName.equals(newName)) {
			this.mp.getConfig().setDisplayName(newName);
			
			this.firePropertyChange("test", oldName, newName);
			//refresh
			((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refresh();
		}
	}

	public void setActivated(boolean activated, UserGroup user) {
		if(activated) {
			this.mp.addUser(user);
		}else {
			this.mp.removeUser(user);
		}
		this.refreshIcon(user);	
	}
	
	public void refreshIcon(UserGroup user) {
		StringBuffer iconPath = new StringBuffer("images/");
		if(this.isActivatedInUser(user)) {
			iconPath.append("config");/*
			if(this.mp.getConfig().getObjectStatus() == ValidationStatus.INVALID)
				iconPath.append("_e");*/
		}
		else {
			iconPath.append("config_h");/*
			if(this.mp.getConfig().getObjectStatus() == ValidationStatus.INVALID)
				iconPath.append("_e");*/
		}
		iconPath.append(".gif");
		this.iconLabel.setIcon(McUtils.createImageIcon(iconPath.toString()));
		this.revalidate();
	}


	public boolean isActivatedInUser(UserGroup user) {
		return this.mp.isActivatedInUser(user);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		UserGroup user = (UserGroup)e.getNewValue();
		this.refreshIcon(user);
	}

	public void setCutCopyOperation(int value) {
		this.cutcopyOperation = value;
		if(value==0)
			this.setBorder(new EtchedBorder());
		else if(value==1)
			this.setBorder(new LineBorder(Color.black, 1));
		else
			this.setBorder(new LineBorder(Color.GREEN,1));
	}
	
	public int getCutCopyOperation() {
		return this.cutcopyOperation;
	}

	@Override
	public Object getTransferData(DataFlavor flavor)
			/*throws UnsupportedFlavorException, IOException*/ {
		if (flavor == CONFIG_FLAVOR) {
			return ConfigComponent.this;
		}
		else {
			//throw new UnsupportedFlavorException(flavor);	
		}
		return flavor;			

	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
	       DataFlavor flavor = ConfigComponent.getDataFlavor();
	       DataFlavor[] result = {flavor};
	       return result;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return (getDataFlavor().equals(flavor));
	}

	public void clean() {
		this.mp = null;
	}
			 
	@Override
	public boolean equals(Object object) {
		if(this == object)
			return true;
		if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		ConfigComponent cc = (ConfigComponent) object;
		return cc.mp.equals(this.mp);
	}
	
	@Override
	public int hashCode() {
		return this.mp.hashCode();
	}
}