package org.biomart.configurator.wizard.addsource;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.FileLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.wizard.WizardPanel;
import org.jdom.Element;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.Validators;
import org.netbeans.validation.api.ui.ValidationPanel;


public class ASFilePanel extends WizardPanel implements ActionListener, DocumentListener {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String IDENTIFIER = "AS_FILE";
	
	private JButton chooseButton;
	private JTextField fileTF;
	private final ValidationPanel pnl = new ValidationPanel();
	
	public ASFilePanel() {
		this.init();
    }  
    
    private void init() {
    	this.chooseButton = new JButton(Resources.get("CHOOSEFILEBUTTON"));
    	this.chooseButton.setActionCommand(Resources.get("CHOOSEFILEBUTTON"));
    	this.chooseButton.addActionListener(this);
    	
    	JPanel topPanel = new JPanel();
    	pnl.setInnerComponent(topPanel);
    	this.fileTF = new JTextField(50);
    	this.fileTF.getDocument().addDocumentListener(this);
    	topPanel.add(chooseButton);
    	topPanel.add(fileTF);
    	Validator<Document> d = Validators.forDocument(true,
                Validators.REQUIRE_NON_EMPTY_STRING, Validators.FILE_MUST_EXIST);
    	pnl.getValidationGroup().add(this.fileTF, d);

    	this.setLayout(new BorderLayout());
    	this.add(pnl);
    }

	@Override
	public void saveWizardState() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
		DataLinkInfo dlinkInfo = result.getDlinkInfo();
		FileLinkObject fileLink = new FileLinkObject();
		fileLink.setFileName(this.fileTF.getText());
		dlinkInfo.setFileLinkObject(fileLink);		
	}

	@Override
	public boolean validateWizard() {
		Problem p = pnl.getProblem();
		boolean validate = p == null ? true : p.severity() != Severity.FATAL;
		if(!validate)
			return false;

		if(McUtils.isStringEmpty(this.fileTF.getText()))
			return false;
		else
			return true;
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
			if(result.getProfileName()!=null) {
				Element root = result.getProfile().getRootElement();
				Element urlE = root.getChild(DataLinkType.FILE.name());
				if(urlE!=null && urlE.getChild(result.getProfileName())!=null) {
					//load valud from element
					this.loadValueFromHistory(urlE.getChild(result.getProfileName()));
				}
			} 
		} else {
			this.loadFromState();
		}
	}

	private void loadFromState() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
		this.fileTF.setText(result.getDlinkInfo().getFileLinkObject().getFileName());
	}
	
	private void loadValueFromHistory(Element e) {
		this.fileTF.setText(e.getAttributeValue("file"));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(Resources.get("CHOOSEFILEBUTTON"))) {
			final String currentDir = Settings.getProperty("currentOpenDir");
			File file = null;
			final JFileChooser xmlFileChooser = new JFileChooser();
			xmlFileChooser.setCurrentDirectory(currentDir == null ? new File(".")
					: new File(currentDir));
			if (xmlFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				// Update the load dialog.
				Settings.setProperty("currentOpenDir", xmlFileChooser
						.getCurrentDirectory().getPath());

				file = xmlFileChooser.getSelectedFile();
			}
			if(file != null)
				try {
					this.fileTF.setText(file.getCanonicalPath());
				} catch (IOException e1) {
					e1.printStackTrace();
				}

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
    

}
