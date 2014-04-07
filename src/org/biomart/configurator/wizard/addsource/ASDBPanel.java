package org.biomart.configurator.wizard.addsource;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.apache.batik.ext.awt.image.spi.DefaultBrokenLinkProvider;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.JdbcType;
import org.biomart.configurator.wizard.WizardPanel;
import org.jdom.Element;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.Validators;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationPanel;

public class ASDBPanel extends WizardPanel implements ItemListener, DocumentListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String IDENTIFIER = "AS_DB";
	
	private JComboBox protocolCB;
	private JTextField hostTF;
	private JTextField portTF;
	private JTextField userTF;
	private JPasswordField passTF;
	private JTextField dbTF;
	private JRadioButton sourceRB;
	private JRadioButton targetRB;
	private JCheckBox myIsamCB;
	private final ValidationPanel pnl = new ValidationPanel();

	public ASDBPanel() {
		init();
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		JPanel topPanel = new JPanel(new GridBagLayout());
		
    	GridBagConstraints c = new GridBagConstraints();
    	c.fill = GridBagConstraints.HORIZONTAL;
    	c.gridx = 0;
    	c.gridy = 0;
    	
    	this.sourceRB = new JRadioButton("source schema");
    	topPanel.add(sourceRB,c);
    	
    	c.gridx = 1;
    	this.targetRB = new JRadioButton("relational mart");
    	topPanel.add(targetRB,c);
    	
    	ButtonGroup bg = new ButtonGroup();
    	bg.add(sourceRB);
    	bg.add(targetRB);
    	
    	c.gridx = 0;
    	c.gridy = 1;
    	JLabel protocolLabel = new JLabel("protocol:");
    	topPanel.add(protocolLabel,c);
    	
    	c.gridx = 1;
    	this.protocolCB = new JComboBox(JdbcType.values());
    	this.protocolCB.addItemListener(this);
    	topPanel.add(protocolCB,c);
    	
    	c.gridx = 2;
    	this.myIsamCB = new JCheckBox("MyISAM");
    	topPanel.add(myIsamCB,c);
    	
    	c.gridx = 0;
    	c.gridy = 2;
    	JLabel hostLabel = new JLabel("host:");
    	topPanel.add(hostLabel,c);
    	
    	c.gridx = 1;
    	this.hostTF = new JTextField(30);
    	this.hostTF.getDocument().addDocumentListener(this);
    	this.hostTF.setName("Host");
    	this.hostTF.getDocument().addDocumentListener(this);
    	topPanel.add(hostTF,c);
    	Validator<Document> d = Validators.forDocument(true,
                Validators.REQUIRE_NON_EMPTY_STRING);
    	pnl.getValidationGroup().add(this.hostTF, d);
    	
    	c.gridx = 0;
    	c.gridy = 3;
    	JLabel portLabel = new JLabel("port:");
    	topPanel.add(portLabel,c);
    	
    	c.gridx = 1;
    	this.portTF = new JTextField(5);
    	this.portTF.setName("Port");
    	this.portTF.getDocument().addDocumentListener(this);
    	Validator<Document> d2 = Validators.forDocument(true,
                Validators.REQUIRE_NON_EMPTY_STRING, Validators.REQUIRE_VALID_INTEGER);
    	pnl.getValidationGroup().add(this.portTF, d2);
    	topPanel.add(portTF,c);
    	
    	c.gridx = 0;
    	c.gridy = 4;
    	JLabel userLabel = new JLabel("user:");
    	topPanel.add(userLabel,c);
    	
    	c.gridx = 1;
    	this.userTF = new JTextField(20);
    	this.userTF.getDocument().addDocumentListener(this);
    	this.userTF.setName("User");
    	pnl.getValidationGroup().add(this.userTF,d);
    	topPanel.add(userTF,c);
    	
    	c.gridx = 0;
    	c.gridy = 5;
    	JLabel pwLabel = new JLabel("password:");
    	topPanel.add(pwLabel,c);
    	
    	c.gridx = 1;
    	this.passTF = new JPasswordField(20);
    	topPanel.add(passTF,c);
    	
    	c.gridx = 0;
    	c.gridy = 6;
    	JLabel dbLabel = new JLabel("database:");
    	topPanel.add(dbLabel,c);
    	
    	c.gridx = 1;
    	this.dbTF = new JTextField(20);
    	this.dbTF.setName("Database");
    	this.dbTF.getDocument().addDocumentListener(this);
    	topPanel.add(dbTF,c);
    	
    	final DBNameValidator dbvalidator = new DBNameValidator();
    	
    	class DBTextListener extends ValidationListener implements ItemListener, DocumentListener {

    		@Override
    		protected boolean validate(Problems problems) {			
    			return dbvalidator.validate(problems, dbTF.getName(), dbTF.getText());
    		}

			@Override
			public void itemStateChanged(ItemEvent e) {
				validate();				
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				validate();				
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				validate();				
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				validate();				
			}   		
    	}
    	
    	
    	DBTextListener dblistener = new DBTextListener();
    	this.protocolCB.addItemListener(dblistener);
    	this.dbTF.getDocument().addDocumentListener(dblistener);
    	pnl.getValidationGroup().add(dblistener);
    	pnl.setInnerComponent(topPanel);    	
    	this.add(pnl,BorderLayout.NORTH);    	
	}
	
	private void loadDefaultValue() {
		this.sourceRB.setSelected(true);
		this.hostTF.setText(Resources.get("ensemblSourceHost"));
		this.portTF.setText(Resources.get("ensemblSourcePort"));
		this.userTF.setText("anonymous");
		this.myIsamCB.setSelected(true);
	}
	
	@Override
	public void saveWizardState() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
		JdbcType jdbcType = (JdbcType)this.protocolCB.getSelectedItem();
		boolean keyguessing = (jdbcType == JdbcType.MySQL && myIsamCB.isSelected());
		String tmplate = jdbcType.getUrlTemplate();
		tmplate = tmplate.replaceAll("<HOST>", this.hostTF.getText());
		tmplate = tmplate.replaceAll("<PORT>", this.portTF.getText());

		JdbcLinkObject jdbcLink = new JdbcLinkObject(tmplate,this.dbTF.getText(),this.dbTF.getText(),
				this.userTF.getText(),new String(this.passTF.getPassword()),jdbcType,
				"","",keyguessing);
		result.getDlinkInfo().setJdbcLinkObject(jdbcLink);
		result.getDlinkInfo().setType(this.sourceRB.isSelected()? DataLinkType.SOURCE: DataLinkType.TARGET);
	}

	@Override
	public boolean validateWizard() {
		boolean validate = true;
		Problem p = pnl.getProblem();
		validate = p == null ? true : p.severity() != Severity.FATAL;

		if(McUtils.isStringEmpty(this.hostTF.getText())){
			validate = false;
		}
		else if(McUtils.isStringEmpty(this.portTF.getText())){
			validate = false;
		}
		else if(McUtils.isStringEmpty(this.userTF.getText())){
			validate = false;
		}
		JdbcType type = (JdbcType)this.protocolCB.getSelectedItem();
		if(type!=JdbcType.MySQL && McUtils.isStringEmpty(this.dbTF.getText())) {
			validate = false;
		}

		return validate;
	}

	@Override
	public Object getNextPanelId() {
		return ASMartsPanel.IDENTIFIER;
	}

	@Override
	public Object getBackPanelId() {
		return ASStartPanel.IDENTIFIER;
	}


	@Override
	public void aboutToDisplayPanel(boolean next) {
		if(next) {
			AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
			boolean useDefaultProfile = true;
			if(result.getProfileName()!=null) {
				Element root = result.getProfile().getRootElement();
				Element urlE = root.getChild(DataLinkType.RDBMS.name());
				if(urlE!=null && urlE.getChild(result.getProfileName())!=null) {
					useDefaultProfile = false;
					//load valud from element
					this.loadValueFromHistory(urlE.getChild(result.getProfileName()));
				}
			} 
			if(useDefaultProfile)
				this.loadDefaultValue();
		} else {
			loadFromState();
		}
	}
	
	private void loadFromState() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
		JdbcLinkObject linkObj = result.getDlinkInfo().getJdbcLinkObject();
		this.protocolCB.setSelectedItem(linkObj.getJdbcType());
		boolean source = result.getDlinkInfo().getDataLinkType() == DataLinkType.SOURCE;
		if(source)
			this.sourceRB.setSelected(true);
		else
			this.targetRB.setSelected(true);
		this.myIsamCB.setSelected(linkObj.isKeyGuessing());
		this.hostTF.setText(linkObj.getHost());
		this.portTF.setText(linkObj.getPort());
		this.userTF.setText(linkObj.getUserName());
		this.passTF.setText(linkObj.getPassword());
		this.dbTF.setText(linkObj.getDatabaseName());
	}
	
	private void loadValueFromHistory(Element e) {
		this.protocolCB.setSelectedItem(JdbcType.valueOf(e.getAttributeValue("protocol")));
		this.hostTF.setText(e.getAttributeValue("host"));
		this.portTF.setText(e.getAttributeValue("port"));
		this.userTF.setText(e.getAttributeValue("user"));
		this.passTF.setText(e.getAttributeValue("password"));
		this.dbTF.setText(e.getAttributeValue("database"));
		this.myIsamCB.setSelected(Boolean.parseBoolean(e.getAttributeValue("myisam")));
		boolean source = Boolean.parseBoolean(e.getAttributeValue("source"));
		if(source)
			this.sourceRB.setSelected(true);
		else
			this.targetRB.setSelected(true);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {		
		if (e.getStateChange() == ItemEvent.SELECTED) {
			JdbcType type = (JdbcType)this.protocolCB.getSelectedItem();
			if(type == JdbcType.MySQL) {
				this.myIsamCB.setEnabled(true);
				this.myIsamCB.setSelected(true);
			} else {
				this.myIsamCB.setEnabled(false);
				this.myIsamCB.setSelected(false);
			}
			this.getWizard().updateButtonEnabled();
		}
	}

	@Override
	public void aboutToHidePanel(boolean next) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void backClear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		this.getWizard().updateButtonEnabled();
		
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		this.getWizard().updateButtonEnabled();
		
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		this.getWizard().updateButtonEnabled();
		
	}
	
	private final class DBNameValidator implements Validator<String> {
		public boolean validate(Problems problems, String compName, String model) {
			boolean result = true;
			JdbcType type = (JdbcType)protocolCB.getSelectedItem();
			if(type!=JdbcType.MySQL && model.length()==0) {
				result = false;
				problems.add ("database name cannot be null in "+type);
			}
			
			return result;
		}
	}

	
}