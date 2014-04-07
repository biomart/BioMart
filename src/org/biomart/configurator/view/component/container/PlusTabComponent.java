package org.biomart.configurator.view.component.container;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.portal.GuiContainer;

public class PlusTabComponent extends JPanel implements MouseListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final DnDTabbedPane pane;
	private boolean titleEnabled;
	private JLabel label = new JLabel("+");
	
	public PlusTabComponent(final DnDTabbedPane pane) {
		super(new BorderLayout());
		this.pane = pane;
		setOpaque(false);
		this.add(label,BorderLayout.CENTER);
		this.setToolTipText("Add a new tab");
		this.addMouseListener(this);
	}

	public void setEnableTitle(boolean b) {
		this.titleEnabled = b;
		if(b)
			label.setForeground(Color.BLACK);
		else
			label.setForeground(Color.GRAY);
	}
	
 	private void addGuiContainer(DnDTabbedPane tabPane) {
		final int count = tabPane.getTabCount();
			String tabName = JOptionPane.showInputDialog("tab name");	
			if(!McUtils.isStringEmpty(tabName)) {
				//replace space with "_"
				String gcName = tabName.replaceAll(" ", "_");
				//check if name conflict
				if(null!=tabPane.getGuiContainer().getRootGuiContainer().getGCByNameRecursively(gcName)) {
					JOptionPane.showMessageDialog(this, "name conflict");
					return;
				}
				GuiContainer newGc = new GuiContainer(gcName);
				newGc.setDisplayName(tabName);
				newGc.addUser(((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getUser());
				tabPane.getGuiContainer().addGuiContainer(newGc);
				tabPane.add(new GuiContainerPanel(newGc), tabName, count-1);
				ButtonTabComponent btc = new ButtonTabComponent(tabPane,newGc);
				((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).addPropertyChangeListener(McEventProperty.USER.toString(), btc);
				tabPane.setTabComponentAt(count-1, btc);
				tabPane.setSelectedIndex(count-1);
			}
		
	}


	@Override
	public void mouseClicked(MouseEvent arg0) {
		//if(this.titleEnabled)
			this.addGuiContainer(this.pane);
		
	}


	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		Boolean b = (Boolean)e.getNewValue();
		this.setEnableTitle(b);
		
	}

}