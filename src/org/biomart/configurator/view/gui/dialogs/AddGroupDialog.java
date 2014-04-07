package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AddGroupDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField tf;
	private JCheckBox cb;
	private String name;
	private boolean grouped;
	
	public AddGroupDialog(Component owner) {
		init(owner);
	}
	
	private void init(Component owner) {
		this.setLayout(new BorderLayout());
		JLabel gLabel = new JLabel("name:");
		tf = new JTextField(20);
		cb = new JCheckBox("grouped");

		JPanel northPanel = new JPanel();
		northPanel.add(gLabel);
		northPanel.add(tf);
		northPanel.add(cb);
		
		JPanel southPanel = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.setActionCommand("ok");
		okButton.addActionListener(this);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		southPanel.add(cancelButton);
		southPanel.add(okButton);
		
		this.add(northPanel,BorderLayout.CENTER);
		this.add(southPanel,BorderLayout.SOUTH);
		this.setLocationRelativeTo(owner);
		this.pack();
		this.setModal(true);
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("ok")) {
			this.name = this.tf.getText();
			this.grouped = this.cb.isSelected();
			this.setVisible(false);
			this.dispose();
		}else if(e.getActionCommand().equals("cancel")) {
			this.name = null;
			this.setVisible(false);
			this.dispose();
		}
		
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean isGrouped() {
		return this.grouped;
	}
	
	
}