package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.McEvent;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.McEventListener;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MPGroupTransferHandler;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;

public class OrderGroupDialog extends JDialog implements WindowListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private GuiContainer gc;
	private JList list;
	private JButton okButton;
	private JButton cancelButton;
	private boolean changed;
	
	public OrderGroupDialog(GuiContainer gc) {
		this.gc = gc;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		//get all groups
		List<String> groups = new ArrayList<String>();
		for(MartPointer mp: gc.getMartPointerList()) {
			String group = mp.getPropertyValue(XMLElements.GROUP);
			if(!McUtils.isStringEmpty(group)) {
				if(!groups.contains(group)) {
					groups.add(group);
				}
			}
		}
		DefaultListModel model = new DefaultListModel();
		for(String s: groups) {
			model.addElement(s);
		}
		this.list = new JList(model);
		this.list.setDragEnabled(true);
		this.list.setDropMode(DropMode.INSERT);
		this.list.setTransferHandler(new MPGroupTransferHandler());
		this.list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane sp = new JScrollPane(this.list);
		sp.setPreferredSize(new Dimension(250,250));
		
		this.okButton = new JButton(Resources.get("OK"));
		this.okButton.setEnabled(false);
		this.okButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				changed = true;
				OrderGroupDialog.this.setVisible(false);				
			}			
		});
		this.cancelButton = new JButton(Resources.get("CANCEL"));
		this.cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OrderGroupDialog.this.setVisible(false);				
			}		
		});
		buttonPanel.add(this.okButton);
		buttonPanel.add(this.cancelButton);
		
		content.add(sp,BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle("Order Group");
		McEventBus.getInstance().addListener(McEventProperty.GROUP_ORDER_CHANGED.toString(), this);
	}
	
	public boolean changed() {
		return this.changed;
	}
	
	public List<String> getNewGroupList() {
		List<String> result = new ArrayList<String>();
		DefaultListModel model = (DefaultListModel)this.list.getModel();
		for(int i=0; i<model.getSize(); i++) {
			result.add((String)model.getElementAt(i));
		}
		return result;
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		McEventBus.getInstance().removeListener(McEventProperty.GROUP_ORDER_CHANGED.toString(), this);			
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	@McEventListener
	public void update(McEvent<?> event) {
		String property = event.getProperty();
		if(property.equals(McEventProperty.GROUP_ORDER_CHANGED.toString())) {
			this.okButton.setEnabled(true);
		}
	}
}