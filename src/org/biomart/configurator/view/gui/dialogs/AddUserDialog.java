package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.portal.UserGroup;
import org.biomart.objects.portal.Users;

public class AddUserDialog extends JDialog implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField nameField;
	private JTextField displayNameField;
	private JPasswordField pwField;
	private JPasswordField repwField;
	private UserGroup user;
	private Users users;
	
	public AddUserDialog (Users users) {
		this.users= users;
		init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.setActionCommand(Resources.get("OK"));
		okButton.addActionListener(this);
		buttonPanel.add(okButton);
		
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		JLabel nameLabel = new JLabel(XMLElements.NAME.toString());
		inputPanel.add(nameLabel, c);
		
		c.gridx = 1;
		nameField = new JTextField(20);
		inputPanel.add(nameField,c);
		
		c.gridx = 0;
		c.gridy = 1;
		JLabel displayNameLabel = new JLabel(XMLElements.DISPLAYNAME.toString());
		inputPanel.add(displayNameLabel,c);
		
		c.gridx = 1;
		displayNameField = new JTextField(20);
		inputPanel.add(displayNameField,c);
		
		c.gridx = 0;
		c.gridy = 2;
		JLabel pwLabel = new JLabel(XMLElements.PASSWORD.toString());
		inputPanel.add(pwLabel,c);
		
		c.gridx = 1;
		pwField = new JPasswordField(20);
		inputPanel.add(pwField,c);
		
		c.gridx = 0;
		c.gridy = 3;
		JLabel repwLabel = new JLabel("Password Confirm");
		inputPanel.add(repwLabel,c);
		
		c.gridx = 1;
		repwField = new JPasswordField(20);
		inputPanel.add(repwField,c);
		
		content.add(inputPanel,BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle("Add User Group");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(Resources.get("OK"))) {
			if(!checkPassword()) {
				JOptionPane.showMessageDialog(this, "password error", "error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(McUtils.isStringEmpty(this.nameField.getText()) || McUtils.isStringEmpty(this.displayNameField.getText())) {
				JOptionPane.showMessageDialog(this, "empty field","error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			UserGroup user = new UserGroup(this.nameField.getText(),this.displayNameField.getText(),
					new String(this.pwField.getPassword()));
			if(isUserExist(user))
				return;
			this.user = user;
			this.setVisible(false);
			this.dispose();
		}
	}
	
	private boolean checkPassword() {
		String p1 = new String(this.pwField.getPassword());
		String p2 = new String(this.repwField.getPassword());
		return p1.equals(p2);
	}
	
	private boolean isUserExist(UserGroup user) {
		if(this.users.getUserList().contains(user)) {
			JOptionPane.showMessageDialog(this, "user already exist","error",JOptionPane.ERROR_MESSAGE);
			return true;
		}
		return false;
	}
	
	public UserGroup getUser() {
		return this.user;
	}
	
}