package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.biomart.common.resources.Resources;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;

public class ReorderConfigDialog extends JDialog implements ActionListener, ListSelectionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private GuiContainer gc;
	private JList configList;
	private JButton upButton;
	private JButton downButton;
	private JButton exitButton;
	private boolean changed;
	
	public ReorderConfigDialog(GuiContainer gc) {
		this.gc = gc;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		DefaultListModel model = new DefaultListModel();
		configList = new JList(model);
		configList.addListSelectionListener(this);
		for(MartPointer mp: this.gc.getMartPointerList()) {
			((DefaultListModel)configList.getModel()).addElement(mp);
		}
		JScrollPane sp = new JScrollPane(configList);
		this.add(sp,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new GridLayout(0,1));
		upButton = new JButton(Resources.get("UP"));
		upButton.setActionCommand(Resources.get("UP"));
		upButton.addActionListener(this);
		
		downButton = new JButton(Resources.get("DOWN"));
		downButton.setActionCommand(Resources.get("DOWN"));
		downButton.addActionListener(this);
		
		
		exitButton = new JButton(Resources.get("CLOSE"));
		exitButton.setActionCommand(Resources.get("CLOSE"));
		exitButton.addActionListener(this);
		
		buttonPanel.add(upButton);
		buttonPanel.add(downButton);
//		buttonPanel.add(saveButton);
		buttonPanel.add(exitButton);
		this.add(buttonPanel,BorderLayout.EAST);
		this.setTitle("reorder config");
		if(model.getSize()>0)
			this.configList.setSelectedIndex(0);
		this.changed = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(Resources.get("CLOSE"))) {
			this.setVisible(false);
			this.dispose();
		}else if(e.getActionCommand().equals(Resources.get("UP"))) {
			int index = this.configList.getSelectedIndex();
			if(index <= 0)
				return;
			DefaultListModel model = (DefaultListModel)this.configList.getModel();			
			Object obj = model.remove(index);
			
			model.insertElementAt(obj, index-1);
			this.configList.setSelectedIndex(index-1);
			Collections.swap(this.gc.getMartPointerList(),index-1,index);
			this.changed = true;
		}else if(e.getActionCommand().equals(Resources.get("DOWN"))) {
			int index = this.configList.getSelectedIndex();
			DefaultListModel model = (DefaultListModel)this.configList.getModel();
			if(index == model.getSize()-1)
				return;
			Object obj = model.remove(index);
			model.insertElementAt(obj, index+1);
			this.configList.setSelectedIndex(index+1);
			Collections.swap(this.gc.getMartPointerList(),index,index+1);
			this.changed = true;
		}	
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource() == this.configList) {
			boolean adjust = e.getValueIsAdjusting();
			if(!adjust) {
				int index = this.configList.getSelectedIndex();
				if(index<=0)
					this.upButton.setEnabled(false);
				else
					this.upButton.setEnabled(true);
				
				if(index == ((DefaultListModel)this.configList.getModel()).getSize()-1)
					this.downButton.setEnabled(false);
				else
					this.downButton.setEnabled(true);
			}
		}		
	}
		
	public boolean changed() {
		return this.changed;
	}
}