package org.biomart.configurator.wizard.addsource;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.McEventBus;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.wizard.WizardPanel;
import org.biomart.objects.objects.MartRegistry;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class ASStartPanel extends WizardPanel implements ItemListener {
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String IDENTIFIER = "AS_START";
	
	private JComboBox type;
	private JComboBox profile;
	private Map<DataLinkType,List<String>> profileMap;
	private Document doc;
    
    public ASStartPanel() {
    	init();
    }
    
    private void init() {
    	this.setLayout(new BorderLayout());
    	JPanel topPanel = new JPanel(new GridBagLayout());
    	GridBagConstraints c = new GridBagConstraints();
    	c.fill = GridBagConstraints.HORIZONTAL;
    	c.gridx = 0;
    	c.gridy = 0;
    	
    	JLabel typelabel = new JLabel(Resources.get("CHOOSETYPE"));
    	topPanel.add(typelabel,c);
    	
		this.type = new JComboBox(new DataLinkType[] { DataLinkType.URL, DataLinkType.RDBMS, DataLinkType.FILE });
    	this.type.addItemListener(this);
		c.gridx = 1;
    	topPanel.add(this.type,c);
    	
    	
    	c.gridx = 0;
    	c.gridy = 1;
    	JLabel profileLabel = new JLabel(Resources.get("CHOOSEPROFILE"));
    	topPanel.add(profileLabel,c);
    	
    	this.profile = new JComboBox();
    	this.profile.setEditable(true);
    	c.gridx = 1;
    	topPanel.add(this.profile,c);
    	
    	this.add(topPanel,BorderLayout.NORTH);
    }

    private void loadDefaultValue() {
    	this.profileMap = new HashMap<DataLinkType,List<String>>();
    	this.type.setSelectedItem(DataLinkType.URL);
    	this.updateNameReference(DataLinkType.URL);
    }
    
	@Override
	public void saveWizardState() {
		Object result = this.getWizardStateObject();
		if(result == null) {
			result = new AddSourceWizardObject();
			this.getWizard().getModel().setWizardResultObject(result);
		}
		DataLinkInfo dlinkInfo = new DataLinkInfo((DataLinkType)this.type.getSelectedItem());
		if(this.profile.getSelectedItem()!=null) {
			((AddSourceWizardObject) result).setProfileName((String)this.profile.getSelectedItem());
			((AddSourceWizardObject) result).setProfile(this.doc);
		}
		else
			((AddSourceWizardObject) result).setProfileName(null);
		((AddSourceWizardObject) result).setDlinkInfo(dlinkInfo);
	}

	@Override
	public boolean validateWizard() {
		return true;
	}

	@Override
	public Object getNextPanelId() {
		DataLinkType dlinkType = (DataLinkType)this.type.getSelectedItem();
		switch (dlinkType) {
		case URL:
			return ASURLPanel.IDENTIFIER;
		case FILE:
			return ASFilePanel.IDENTIFIER;
		default:
			return ASDBPanel.IDENTIFIER;			
		}
	}

	@Override
	public Object getBackPanelId() {
		return null;
	}


	@Override
	public void aboutToDisplayPanel(boolean next) {
		//load profile
		if(next) {
			loadProfiles();
	    	loadDefaultValue();
	    	McEventBus.getInstance().fire(McEventProperty.STATUSBAR_UPDATE.toString(), Resources.get("ADDSOURCETITLE"));
		} else {
			//load from current state
			loadFromState();
		}
	}
	
	private void loadFromState() {
		AddSourceWizardObject state = (AddSourceWizardObject)this.getWizardStateObject();
		this.profile.setSelectedItem(state.getProfileName());
		this.type.setSelectedItem(state.getDlinkInfo().getDataLinkType());
	}
    
	/**
	 * reset the cached history items for the name combobox
	 * @param type
	 */
	private void updateNameReference(DataLinkType type) {
		this.profile.removeAllItems();
		if(this.doc == null)
			return;
		Element root = doc.getRootElement();
		List<String> names = new ArrayList<String>();
		Element profileE = root.getChild(type.name());
		if(profileE==null)
			return;
		@SuppressWarnings("unchecked")
		List<Element> listE = profileE.getChildren();
		if(listE==null)
			return;
		for(Element e: listE) {
			names.add(e.getName());
		}

		for (String item: names)
			this.profile.addItem(item);

		this.profile.setSelectedIndex(-1);	
	}
	
	private void loadProfiles() {
		File cacheDir = Settings.getClassCacheDir();
		File cacheXml = new File(cacheDir,"cacheXml");
		if(cacheXml.exists()) {
			try {
				SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
				this.doc = saxBuilder.build(cacheXml);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}		
		}
		//make sure doc is not null
		if(this.doc == null) {
			Element root = new Element("profile");
			this.doc = new Document(root);
		}
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			DataLinkType type = (DataLinkType)this.type.getSelectedItem();
			this.updateNameReference(type);
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

 
}
