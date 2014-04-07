package org.biomart.configurator.wizard.addsource;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.wizard.WizardPanel;
import org.jdom.Element;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.Validators;
import org.netbeans.validation.api.ui.ValidationPanel;

public class ASURLPanel extends WizardPanel implements DocumentListener {
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String IDENTIFIER = "AS_URL";
	
	private JComboBox protocolCB;
	private JTextField hostTF;
	private JTextField portTF;
	private JTextField pathTF;
	private JTextField userTF;
	private JPasswordField passwordTF;
	private JTextField oauthTF;
	private JCheckBox versionCB;
	private final ValidationPanel pnl = new ValidationPanel();
        
    public ASURLPanel() {
       init(); 
    }  
    
    private void init() {
        //Here we create our Validation Panel.  It has a built-in
        //ValidationGroup we can use - we will just call
        //pnl.getValidationGroup() and add validators to it tied to
        //components
        
    	this.setLayout(new BorderLayout());
    	
    	JPanel topPanel = new JPanel(new GridBagLayout());
    	
    	GridBagConstraints c = new GridBagConstraints();
    	c.fill = GridBagConstraints.HORIZONTAL;
    	c.gridx = 0;
    	c.gridy = 0;
    	
    	JLabel protocolLabel = new JLabel("protocol:");
    	topPanel.add(protocolLabel,c);
    	
    	c.gridx = 1;
    	this.protocolCB = new JComboBox(new String[]{"http","https"});
    	topPanel.add(protocolCB,c);
    	
    	c.gridx = 2;
    	this.versionCB = new JCheckBox("version 0.8");
    	topPanel.add(versionCB,c);
    	
    	c.gridx = 0;
    	c.gridy = 1;
    	JLabel hostLabel = new JLabel("host:");
    	topPanel.add(hostLabel,c);
    	    	
    	c.gridx = 1;
    	this.hostTF = new JTextField(30);
    	this.hostTF.setName("Host");
    	this.hostTF.getDocument().addDocumentListener(this);
    	topPanel.add(hostTF,c);
    	Validator<Document> d = Validators.forDocument(true,
                Validators.REQUIRE_NON_EMPTY_STRING);
    	pnl.getValidationGroup().add(this.hostTF, d);
    	
    	c.gridx = 0;
    	c.gridy = 2;
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
    	c.gridy = 3;
    	JLabel pathLabel = new JLabel("path:");
    	topPanel.add(pathLabel,c);
    	
    	c.gridx = 1;
    	this.pathTF = new JTextField(20);
    	this.pathTF.setName("Path");
    	this.pathTF.getDocument().addDocumentListener(this);
    	topPanel.add(pathTF,c);
    	pnl.getValidationGroup().add(this.pathTF, d);
    	
    	c.gridx = 0;
    	c.gridy = 4;
    	JLabel userLabel = new JLabel("user:");
    	topPanel.add(userLabel,c);
    	
    	c.gridx = 1;
    	this.userTF = new JTextField(20);
    	topPanel.add(userTF,c);
    	
    	c.gridx = 0;
    	c.gridy = 5;
    	JLabel pwLabel = new JLabel("password:");
    	topPanel.add(pwLabel,c);
    	
    	c.gridx = 1;
    	this.passwordTF = new JPasswordField(20);
    	topPanel.add(passwordTF,c);
    	
    	c.gridx = 0;
    	c.gridy = 6;
    	JLabel oauthLabel = new JLabel("Key (oauth):");
    	topPanel.add(oauthLabel,c);
    	
    	c.gridx = 1;
    	this.oauthTF = new JTextField(20);
    	topPanel.add(oauthTF,c);
    	
    	pnl.setInnerComponent(topPanel);
        pnl.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
              Problem p = pnl.getProblem();
              boolean enable = p == null ? true : p.severity() != Severity.FATAL;

            }
          });
    	this.add(pnl,BorderLayout.NORTH);
    }
    
    private void loadDefaultValue() {
    	this.hostTF.setText(Resources.get("biomartUrl2"));
    	this.portTF.setText("80");
    	this.pathTF.setText(Resources.get("biomartPath"));
    }

	@Override
	public void saveWizardState() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
		UrlLinkObject url = new UrlLinkObject();
		String prefix = (String)this.protocolCB.getSelectedItem();
		url.setProtocol(prefix);
		url.setHost(this.hostTF.getText());
		url.setPort(this.portTF.getText());
		url.setPath(this.pathTF.getText());
		url.setUserName(this.userTF.getText());
		url.setPassword(new String(this.passwordTF.getPassword()));
		url.setKeys(this.oauthTF.getText());
		url.setVersion8(this.versionCB.isSelected());
		
		result.getDlinkInfo().setUrlLinkObject(url);	
	}

	@Override
	public boolean validateWizard() {
		boolean validate = true;
		Problem p = pnl.getProblem();
		validate = p == null ? true : p.severity() != Severity.FATAL;

		if(McUtils.isStringEmpty(this.hostTF.getText())) {
			validate = false;
		}
		else if(McUtils.isStringEmpty(this.portTF.getText())){
			validate = false;
		}
		else if(McUtils.isStringEmpty(this.pathTF.getText())){
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
				Element urlE = root.getChild(DataLinkType.URL.name());
				if(urlE!=null && urlE.getChild(result.getProfileName())!=null) {
					useDefaultProfile = false;
					//load valud from element
					this.loadValueFromHistory(urlE.getChild(result.getProfileName()));
				}
			} 
			if(useDefaultProfile)
				this.loadDefaultValue();
		} else {
			this.loadFromState();
		}
	}
	
	private void loadFromState() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
		UrlLinkObject linkObj = result.getDlinkInfo().getUrlLinkObject();
		this.protocolCB.setSelectedItem(linkObj.getProtocol());
		this.hostTF.setText(linkObj.getHost());
		this.portTF.setText(linkObj.getPort());
		this.pathTF.setText(linkObj.getPath());
		this.userTF.setText(linkObj.getUserName());
		this.passwordTF.setText(linkObj.getPassword());
		this.oauthTF.setText(linkObj.getKeys());
		this.versionCB.setSelected(linkObj.isVersion8());
	}
	
	private void loadValueFromHistory(Element e) {
		this.protocolCB.setSelectedItem(e.getAttributeValue("protocol"));
		this.hostTF.setText(e.getAttributeValue("host"));
		this.portTF.setText(e.getAttributeValue("port"));
		this.pathTF.setText(e.getAttributeValue("path"));
		this.userTF.setText(e.getAttributeValue("user"));
		this.passwordTF.setText(e.getAttributeValue("password"));
		this.oauthTF.setText(e.getAttributeValue("key"));
		this.versionCB.setSelected("0.8".equals(e.getAttributeValue("version"))?true:false);
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
    
}
