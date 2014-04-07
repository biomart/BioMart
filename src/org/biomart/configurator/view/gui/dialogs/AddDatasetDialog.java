package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.type.JdbcType;

public class AddDatasetDialog extends JDialog implements ItemListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean changed;
	private JComboBox typeCB;
	public static String URLPANEL = "URL";
	public static String DBPANEL = "DB";
	public static String HTTP = "http";
	public static String HTTPS = "https";
	private JPanel cardPanel;
	private JTextField dbhostTF;
	private JTextField urlhostTF;
	private JComboBox protocolCB;
	private List<String> result;
	private JTextField dbportTF;
	private JTextField urlportTF;
	private JTextField pathTF;
	private JComboBox dbTypeCB;
	private JTextField pwTF;
	private JTextField userTF;
	
	private JTextField dbTF;
	private JTextField schemaTF;
	private JTextField dsSuffixTF;
	private JTextField dsDisplayNameTF;
	
	private String col0;
	private String col1;
	private String col2;
	private String col3;
	private String col4;
	private String col5;
	private String col7;
	
	private boolean mulRows; 
	private boolean isFromEdit;
	

	public AddDatasetDialog(JDialog parent, String col0, String col1, String col2, String col3, String col4, String col5,
			String col7, boolean multipleRows, boolean isFromEdit) {
		super(parent);
		this.col0 = col0;
		this.col1 = col1;
		this.col2 = col2;
		this.col3 = col3;
		this.col4 = col4;
		this.col5 = col5;
		this.col7 = col7;
		this.mulRows = multipleRows;
		this.isFromEdit = isFromEdit;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		JPanel typePanel = new JPanel();
		cardPanel = new JPanel(new CardLayout());
		JPanel buttonPanel = new JPanel();
		
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		JButton okButton = new JButton(Resources.get("OK"));
		
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				executeCancel();
			}			
		});
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				executeOK();				
			}			
		});
		
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		String comboBoxItems[] = { DBPANEL,URLPANEL };

		JLabel conType = new JLabel("Connection Type:");
		this.typeCB = new JComboBox(comboBoxItems);
		this.typeCB.addItemListener(this);
		
		JLabel dsNameLabel = new JLabel("dataset (suffix):");
		this.dsSuffixTF = new JTextField(20);
		
		JLabel dsdsLabel = new JLabel("displayname:");
		this.dsDisplayNameTF = new JTextField(20);
		
		typePanel.add(conType);
		typePanel.add(this.typeCB);
		typePanel.add(dsNameLabel);
		typePanel.add(this.dsSuffixTF);
		typePanel.add(dsdsLabel);
		typePanel.add(dsDisplayNameTF);
		
		cardPanel.add(this.createDBPanel(),DBPANEL);
		cardPanel.add(this.createURLPanel(),URLPANEL);
		
		this.add(typePanel,BorderLayout.NORTH);
		this.add(cardPanel,BorderLayout.CENTER);
		this.add(buttonPanel,BorderLayout.SOUTH);
		
		if(this.mulRows) {
			this.dsSuffixTF.setText("");
			this.dsSuffixTF.setEnabled(false);
			this.dsDisplayNameTF.setText("");
			this.dsDisplayNameTF.setEnabled(false);
		}else {
			this.dsSuffixTF.setText(this.col5);
			this.dsDisplayNameTF.setText(this.col7);
		}
		if(isFromEdit) {
			this.dsSuffixTF.setText(this.col5);
			this.dsSuffixTF.setEnabled(false);
		}
			
		if(this.col0.indexOf("jdbc")==0) {
			this.typeCB.setSelectedItem(DBPANEL);
			String[] _db = this.col0.split(":");
			JdbcType type = JdbcType.valueLike(_db[0]+":"+_db[1]);
			this.dbTypeCB.setSelectedItem(type);
			String hostStr = null;
			String portStr = null;
			if(type == JdbcType.Oracle) {
				hostStr = _db[3].substring(1);
				portStr = _db[4];
			} else if(type == JdbcType.MSSQL) {
				hostStr = _db[2].substring(2);
				portStr = _db[3];				
			}
			else {
				hostStr = _db[2].substring(2);
				portStr = _db[3].substring(0,_db[3].length()-1);
			}
			this.dbhostTF.setText(hostStr);
			this.dbportTF.setText(portStr);
			this.userTF.setText(col3);
			this.pwTF.setText(col4);
			if(this.mulRows) {
				this.dbTF.setText("");
				this.schemaTF.setText("");
				this.dbTF.setEnabled(false);
				this.schemaTF.setEnabled(false);
			} else {
				this.dbTF.setText(col1);
				this.schemaTF.setText(col2);				
			}
		}else {
			this.typeCB.setSelectedItem(URLPANEL);
			int index = this.col0.indexOf(HTTP);
			//need to handle http or https
			if(index>=0) {
				index = this.col0.indexOf("://");
				String hostStr = this.col0.substring(index+3);
				this.protocolCB.setSelectedItem(HTTP);
				this.urlhostTF.setText(hostStr);
				this.urlportTF.setText(col1);
				this.pathTF.setText(col2);
			}
			index = this.col0.indexOf(HTTPS);
			if(index >=0){
				index = this.col0.indexOf("://");
				String hostStr = this.col0.substring(index+3);
				this.protocolCB.setSelectedItem(HTTPS);
				this.urlhostTF.setText(hostStr);
				this.urlportTF.setText(col1);
				this.pathTF.setText(col2);
			}
		}
	}
	
	private boolean validateDSname(){
		if(this.dsSuffixTF.getText().isEmpty()){
			JOptionPane.showMessageDialog(this, "Dataset suffix can not be empty!");
			return false;
		}
		else if(this.dsDisplayNameTF.getText().isEmpty()){
			if(this.mulRows)
				return true;
			JOptionPane.showMessageDialog(this, "Dataset display name can not be empty!");
			return false;
		}
		return true;
	}
	private boolean validateHost(String value) {
		// TODO Auto-generated method stub
		if(value.indexOf("https://") >=0 || value.indexOf("http://") >=0)
			return false;
		else return true;
	}

	private void executeCancel() {
		this.changed = false;
		this.setVisible(false);
		this.dispose();
	}
	
	private void executeOK() {
		this.result = new ArrayList<String>();
		String typeStr = (String)this.typeCB.getSelectedItem();
		
		if(!this.validateDSname()){
			this.setVisible(false);
			this.dispose();
			return;
		}
		
		if(typeStr.equals(AddDatasetDialog.DBPANEL)) {
			JdbcType jdbcType = (JdbcType)this.dbTypeCB.getSelectedItem();
			String tmplate = jdbcType.getUrlTemplate();
			tmplate = tmplate.replaceAll("<HOST>", this.dbhostTF.getText());
			tmplate = tmplate.replaceAll("<PORT>", this.dbportTF.getText());
			result.add(AddDatasetDialog.DBPANEL);
			result.add(tmplate);
			result.add(this.dbTF.getText());
			result.add(this.schemaTF.getText());
			result.add(this.userTF.getText());
			result.add(this.pwTF.getText());
			//add dataset suffix
			result.add(this.dsSuffixTF.getText());
			result.add(this.dsDisplayNameTF.getText());
		}else {
			
			String hostStr = this.urlhostTF.getText();
			if(!this.validateHost(hostStr)){
				JOptionPane.showMessageDialog(this, "host name is not valid!");
				this.setVisible(false);
				this.dispose();
				return;
			}
			/*if(hostStr.indexOf("http")<0)*/
			hostStr = this.protocolCB.getSelectedItem()+"://"+hostStr;
			String pathStr = this.pathTF.getText();
			if(!pathStr.startsWith("/")) {
				pathStr = "/"+pathStr;
			}
			result.add(AddDatasetDialog.URLPANEL);
			result.add(hostStr);
			result.add(this.urlportTF.getText());
			result.add(pathStr);
			result.add("");
			result.add("");
			//add dataset suffix
			result.add(this.dsSuffixTF.getText());
			result.add(this.dsDisplayNameTF.getText());
		}
		this.changed = true;
		
		this.setVisible(false);
		this.dispose();
	}
	
	private JPanel createDBPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		int row = 0;
		JLabel dbType = new JLabel("Database type:");
		dbTypeCB = new JComboBox(JdbcType.values());
		c.anchor = GridBagConstraints.LINE_END;
		c.gridx = 0;
		c.gridy = row++;
		panel.add(dbType,c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(dbTypeCB,c);
		
		JLabel hostLabel = new JLabel("host:");
		c.gridx = 0;
		c.gridy = row++;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(hostLabel,c);
		
		dbhostTF = new JTextField(40);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(dbhostTF,c);
		
		JLabel portLabel = new JLabel("port:");
		c.gridx = 0;
		c.gridy = row++;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(portLabel,c);
		
		dbportTF = new JTextField(5);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(dbportTF,c);
		
		JLabel userLabel = new JLabel("user:");
		c.gridx = 0;
		c.gridy = row++;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(userLabel,c);
		
		userTF = new JTextField(20);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(userTF,c);
		
		JLabel pwLabel = new JLabel("password:");
		c.gridx = 0;
		c.gridy = row++;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(pwLabel,c);
		
		pwTF = new JTextField(20);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(pwTF,c);
		
		JLabel dbLabel = new JLabel("database:");
		c.gridx = 0;
		c.gridy = row++;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(dbLabel,c);
		
		this.dbTF = new JTextField(20);
		this.dbTF.setEnabled(!this.mulRows);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(this.dbTF,c);
		
		JLabel schemaLabel = new JLabel("schema:");
		c.gridx = 0;
		c.gridy = row++;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(schemaLabel,c);
		
		this.schemaTF = new JTextField(20);
		this.schemaTF.setEnabled(!this.mulRows);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(this.schemaTF,c);
		
		return panel;
	}
	
	private JPanel createURLPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JLabel protocolLabel = new JLabel("protocol:");
		c.anchor = GridBagConstraints.LINE_END;
		c.gridx = 0;
		c.gridy = 0;
		panel.add(protocolLabel,c);
		String[] protocols = {HTTP, HTTPS};
		this.protocolCB = new JComboBox(protocols);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(protocolCB,c);
		
		JLabel hostLabel = new JLabel("host:");
		c.anchor = GridBagConstraints.LINE_END;
		c.gridx = 0;
		c.gridy = 1;
		panel.add(hostLabel,c);
		urlhostTF = new JTextField(40);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(urlhostTF,c);
		JLabel portLabel = new JLabel("port:");
		c.gridx = 0;
		c.gridy = 2;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(portLabel,c);
		urlportTF = new JTextField(5);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(urlportTF,c);
		
		JLabel pathLabel = new JLabel("path:");
		c.gridx = 0;
		c.gridy = 3;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add(pathLabel,c);
		
		pathTF = new JTextField(20);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add(pathTF,c);
		
		return panel;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		CardLayout cl = (CardLayout)(cardPanel.getLayout());
	    cl.show(cardPanel, (String)e.getItem());		
	}
	
	public boolean changed() {
		return this.changed;
	}
	
	public List<String> getResult() {
		return this.result;
	}
}