package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.objects.portal.User;
import org.biomart.objects.portal.UserGroup;
import org.biomart.objects.portal.Users;

public class UserManagementDialog extends JDialog implements ActionListener, ListSelectionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Users users;
	private JList groupList;
	private JList userList;
	private JTextField ugNameTF;
	private JTextField ugdnTF;
	private JTextField usernameTF;
	private JTextField userdnTF;
	private JTextField locationTF;
	private JCheckBox ugActiveCB;
	private JCheckBox userActiveCB;
	private JTextField openTF;

	private JButton addUgButton;
	private JButton removeUgButton;
	private JButton addUserButton;
	private JButton removeUserButton;
	private JComboBox groupCB;
	
	private JRadioButton pwRB;
	private JRadioButton openRB;
	
	private UgDocumentListener ugDocListener = new UgDocumentListener();
	private UserDocumentListener userDocListener = new UserDocumentListener();
	
	public UserManagementDialog(Users users) {
		this.users = users;
		init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(this);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		
		JPanel gridPanel = new JPanel(new GridLayout(2,2));
		JPanel ugPanel = new JPanel(new BorderLayout());
		Border etchedBdr = BorderFactory.createEtchedBorder();
		Border titledBdr = BorderFactory.createTitledBorder(etchedBdr, "group");
		ugPanel.setBorder(titledBdr);
		
		JPanel ugEditor = this.createUgEditorPanel();
		ugEditor.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel userPanel = new JPanel(new BorderLayout());
		Border userBdr = BorderFactory.createTitledBorder(etchedBdr,"user");
		userPanel.setBorder(userBdr);
		
		JPanel userEditor = this.createUserEditorPanel();
		userEditor.setBorder(BorderFactory.createEtchedBorder());
		
		gridPanel.add(ugPanel);
		gridPanel.add(ugEditor);
		gridPanel.add(userPanel);
		gridPanel.add(userEditor);
		
		DefaultListModel groupModel = new DefaultListModel();
		DefaultListModel userModel = new DefaultListModel();
		groupList = new JList(groupModel);
		groupList.addListSelectionListener(this);
		userList = new JList(userModel);
		userList.addListSelectionListener(this);
		//add group
		for(UserGroup ug: this.users.getUserList()) {
			groupModel.addElement(ug);
		}
		JScrollPane groupsp = new JScrollPane(groupList);
		JScrollPane usersp = new JScrollPane(userList);
		
		ugPanel.add(groupsp, BorderLayout.CENTER);
		userPanel.add(usersp, BorderLayout.CENTER);
		

		JPanel ugbuttonPanel = new JPanel();
		addUgButton = new JButton("+");
		addUgButton.setActionCommand("addusergroup");
		addUgButton.addActionListener(this);
		removeUgButton = new JButton("-");
		removeUgButton.setActionCommand("removeusergroup");
		removeUgButton.addActionListener(this);
		removeUgButton.setEnabled(false);
		
		ugbuttonPanel.add(addUgButton);
		ugbuttonPanel.add(removeUgButton);
		ugPanel.add(ugbuttonPanel,BorderLayout.SOUTH);
		
		JPanel userbuttonPanel = new JPanel();
		addUserButton = new JButton("+");
		addUserButton.setActionCommand("adduser");
		addUserButton.addActionListener(this);
		removeUserButton = new JButton("-");
		removeUserButton.setActionCommand("removeuser");
		removeUserButton.addActionListener(this);
		removeUserButton.setEnabled(false);
		
		userbuttonPanel.add(addUserButton);
		userbuttonPanel.add(removeUserButton);
		userPanel.add(userbuttonPanel,BorderLayout.SOUTH);

		content.add(gridPanel,BorderLayout.CENTER);
		//select the first user group
		if(((DefaultListModel)this.groupList.getModel()).getSize()>0)
			this.groupList.setSelectedIndex(0);
		this.add(content);
		this.setTitle("User Management");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("usergroupactive")) {
			UserGroup ug = (UserGroup)this.groupList.getSelectedValue();
			ug.setHideValue(!this.ugActiveCB.isSelected());
		}else if(e.getActionCommand().equals("useractive")) {
			User user = (User)this.userList.getSelectedValue();
			user.setHideValue(!this.userActiveCB.isSelected());
		}else if(e.getActionCommand().equals("addusergroup")) {
			this.addUserGroup();
		}else if(e.getActionCommand().equals("adduser")) {
			this.addUser();
		}else if(e.getActionCommand().equals("removeusergroup")) {
			this.removeUserGroup();
		}else if(e.getActionCommand().equals("removeuser")) {
			this.removeUser();
		}else if(e.getActionCommand().equals("saveuser")) {
			this.saveUser();
		}
	}
	
	
	
	private JPanel createUgEditorPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JLabel nameLabel = new JLabel("name:");
		ugNameTF = new JTextField(20);
		ugNameTF.getDocument().addDocumentListener(this.ugDocListener);
		ugNameTF.setEnabled(false);
		c.gridx = 0;
		c.gridy = 0;
		//panel.add(nameLabel,c);
		c.gridx = 1;
		//panel.add(ugNameTF,c);
		JLabel dnLabel = new JLabel("display name:");
		ugdnTF = new JTextField(20);
		ugdnTF.getDocument().addDocumentListener(this.userDocListener);
		ugdnTF.setEnabled(false);
		c.gridx = 0;
		c.gridy = 1;
		//panel.add(dnLabel,c);
		c.gridx = 1;
		//panel.add(ugdnTF,c);
		JLabel locationLabel = new JLabel("location:");
		locationTF = new JTextField(20);
		locationTF.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				setLocation();
				
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				setLocation();
				
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				setLocation();
				
			}
			
		});
		//locationTF.setEnabled(false);
		c.gridx = 0;
		c.gridy = 2;
		panel.add(locationLabel,c);
		c.gridx = 1;
		panel.add(locationTF,c);
		
		ugActiveCB = new JCheckBox("activate");
		ugActiveCB.setActionCommand("usergroupactive");
		ugActiveCB.addActionListener(this);
		c.gridx = 0;
		c.gridy = 3;
		//panel.add(ugActiveCB,c);
		return panel;
	}
	
	private JPanel createUserEditorPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JLabel nameLabel = new JLabel("name:");
		usernameTF = new JTextField(20);
		usernameTF.getDocument().addDocumentListener(this.userDocListener);
		usernameTF.setEnabled(false);
		c.gridx = 0;
		c.gridy = 0;
		//panel.add(nameLabel,c);
		c.gridx = 1;
		//panel.add(usernameTF,c);
		JLabel dnLabel = new JLabel("display name:");
		userdnTF = new JTextField(20);
		userdnTF.getDocument().addDocumentListener(this.userDocListener);
		userdnTF.setEnabled(false);
		c.gridx = 0;
		c.gridy = 1;
		//panel.add(dnLabel,c);
		c.gridx = 1;
		//panel.add(userdnTF,c);
		userActiveCB = new JCheckBox("activate");
		userActiveCB.setActionCommand("useractive");
		userActiveCB.addActionListener(this);
		c.gridx = 0;
		c.gridy = 2;
		//panel.add(userActiveCB,c);
		
		
		JPanel pwPanel = new JPanel(new BorderLayout());
		pwPanel.setBorder(BorderFactory.createEtchedBorder());
		this.pwRB = new JRadioButton("password");
		this.pwRB.setEnabled(false);
		pwPanel.add(this.pwRB,BorderLayout.NORTH);
		JPanel passwordPanels = new JPanel(new GridLayout(0,1));
		//passwordPanels.add(this.pwRB);
		JLabel pwLabel = new JLabel("password:");
		JPasswordField pwField = new JPasswordField(10);
		pwField.setEnabled(false);
		JPanel pwPanel1 = new JPanel();
		pwPanel1.add(pwLabel);
		pwPanel1.add(pwField);
		passwordPanels.add(pwPanel1);
		
		JLabel pwLabel2 = new JLabel("password:");
		JPasswordField pwField2 = new JPasswordField(10);
		pwField2.setEnabled(false);
		JPanel pwPanel2 = new JPanel();
		pwPanel2.add(pwLabel2);
		pwPanel2.add(pwField2);
		passwordPanels.add(pwPanel2);
		passwordPanels.add(pwPanel2);
		pwPanel.add(passwordPanels,BorderLayout.CENTER);
		
		c.gridwidth = 2; 
		c.gridy = 3;
		c.anchor = GridBagConstraints.LINE_START;
//		panel.add(pwPanel,c);
		
		JPanel openPanel = new JPanel(new BorderLayout());
		openPanel.setBorder(BorderFactory.createEtchedBorder());
		this.openRB = new JRadioButton("openID");
		this.openRB.setSelected(true);
		JPanel inputPanel = new JPanel();
		JLabel openLabel = new JLabel("openID:");
		openTF = new JTextField(20);
		openTF.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				userChange();				
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				userChange();
				
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				userChange();
				
			}
			
		});
		inputPanel.add(openLabel);
		inputPanel.add(openTF);
		openPanel.add(this.openRB,BorderLayout.NORTH);
		openPanel.add(inputPanel,BorderLayout.CENTER);
		
		c.gridy = 4;
		panel.add(openPanel,c);

		return panel;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource()==this.groupList) {
			boolean adjust = e.getValueIsAdjusting();
			if(!adjust) {
				UserGroup ug = (UserGroup)this.groupList.getSelectedValue();
				if(ug!=null) {
					this.ugNameTF.setText(ug.getName());
					this.ugdnTF.setText(ug.getDisplayName());
					this.locationTF.setText(ug.getPropertyValue(XMLElements.LOCATION));
					this.ugActiveCB.setSelected(!ug.isHidden());
					//update userlist
					DefaultListModel model = (DefaultListModel)this.userList.getModel();
					model.clear();
					for(User user: ug.getUserList()) {
						model.addElement(user);
					}
					if(model.getSize()>0)
						this.userList.setSelectedIndex(0);
					this.removeUgButton.setEnabled(!ug.getName().equals("anonymous"));
				}
			}
		}else if(e.getSource()==this.userList) {
			boolean adjust = e.getValueIsAdjusting();
			if(!adjust) {
				User user = (User)this.userList.getSelectedValue();
				if(user!=null) {
					this.usernameTF.setText(user.getName());
					this.userdnTF.setText(user.getDisplayName());
					this.userActiveCB.setSelected(!user.isHidden());
					this.openTF.setText(user.getPropertyValue(XMLElements.OPENID));
					this.removeUserButton.setEnabled(!user.getName().equals("anonymous"));
				}
			}
		}		
	}

	
	private class UgDocumentListener implements DocumentListener {

		@Override
		public void changedUpdate(DocumentEvent e) {
			//setUgChange(true);			
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			//setUgChange(true);			
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			//setUgChange(true);			
		}		
	}
	
	private class UserDocumentListener implements DocumentListener {

		@Override
		public void changedUpdate(DocumentEvent e) {
			//userChange();		
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			//userChange();		
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			//userChange();			
		}
		
	}
	
	private void saveUserGroup() {
		
	}
	
	private void userChange() {
		User user = (User)this.userList.getSelectedValue();
		if(user!=null)
			user.setProperty(XMLElements.OPENID, this.openTF.getText());
	}
	
	private void setLocation() {
		UserGroup ug = (UserGroup)this.groupList.getSelectedValue();
		if(ug!=null)
			ug.setProperty(XMLElements.LOCATION,this.locationTF.getText());
	}
	
	private void saveUser() {
		Object ugObj = this.groupList.getSelectedValue();
		UserGroup ug = (UserGroup)ugObj;
		User user = new User(this.usernameTF.getText(),this.userdnTF.getText(),"");
		user.setHideValue(!this.userActiveCB.isSelected());
		ug.addUser(user);
		((DefaultListModel)this.userList.getModel()).addElement(user);
	}
	
	private void removeUser() {
		int index = this.userList.getSelectedIndex();
		User user = (User)this.userList.getSelectedValue();
		UserGroup ug = (UserGroup)this.groupList.getSelectedValue();
		ug.removeUser(user);
		((DefaultListModel)this.userList.getModel()).removeElement(user);
		//select another item
		if(index>0)
			this.userList.setSelectedIndex(index-1);
		else {
			if(((DefaultListModel)this.userList.getModel()).getSize()>0)
				this.userList.setSelectedIndex(0);
			else {
				//clear
				resetUser(false,null);
			}
		}
	}
	
	private void removeUserGroup() {
		int index = this.groupList.getSelectedIndex();
		UserGroup ug = (UserGroup)this.groupList.getSelectedValue();
		((Users)ug.getParent()).removeUserGroup(ug);
		((DefaultListModel)this.groupList.getModel()).removeElement(ug);
		if(index>0) 
			this.groupList.setSelectedIndex(index-1);
		else 
			this.groupList.setSelectedIndex(0);
		McEventBus.getInstance().fire(McEventProperty.USERGROUPCHANGED.toString(),null);
	}
	
	private void resetUser(boolean active, UserGroup ug) {
		this.usernameTF.setText("");
		this.userdnTF.setText("");
		this.userActiveCB.setSelected(active);
		if(ug==null) 
			this.groupCB.setSelectedIndex(-1);
		else
			this.groupCB.setSelectedItem(ug);
	}
	
	private void addUserGroup() {
    	Users users = McGuiUtils.INSTANCE.getRegistryObject().getPortal().getUsers();
    	String groupName = JOptionPane.showInputDialog(this,"group name");
    	if(!McUtils.isStringEmpty(groupName)) {
    		//check exists
    		if(users.getUserGroupByName(groupName)!=null) {
    			JOptionPane.showMessageDialog(this, "group name conflict", "error",JOptionPane.ERROR_MESSAGE);
    			return;
    		}
    		UserGroup newUg = new UserGroup(groupName,groupName,"");
    		User user = new User(groupName,groupName,"");
    		newUg.addUser(user);
    		users.addUserGroup(newUg);
    		((DefaultListModel)this.groupList.getModel()).addElement(newUg);
    		this.groupList.setSelectedValue(newUg, true); 
    		McEventBus.getInstance().fire(McEventProperty.USERGROUPCHANGED.toString(),null);
    	}
	}
	
	private void addUser() {
    	String userName = JOptionPane.showInputDialog(this,"user name");
    	if(!McUtils.isStringEmpty(userName)) {
    		UserGroup ug = (UserGroup)this.groupList.getSelectedValue();
    		if(((Users)ug.getParent()).getUserByName(userName)!=null) {
    			JOptionPane.showMessageDialog(this, "user name conflict", "error",JOptionPane.ERROR_MESSAGE);
    			return;
    		}
    		User user = new User(userName, userName, "");
    		ug.addUser(user);
    		((DefaultListModel)this.userList.getModel()).addElement(user);
    		this.userList.setSelectedValue(user, true); 		
    	}		
	}
}