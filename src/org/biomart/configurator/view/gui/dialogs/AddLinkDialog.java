package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;

public class AddLinkDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JComboBox martCB;
	private JComboBox configCB;
	private JList allAttList;
	private JList selectedAttList;
	private final List<Filter> filterList;
	private JList sourceDsList;
	private JList targetDsList;
	
	public AddLinkDialog(List<Filter> filterList) {
		this.filterList = filterList;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);			
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		JPanel martPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		
		JLabel martLabel = new JLabel("mart:");
		JLabel configLabel = new JLabel("config:");
		
		martCB = new JComboBox();
		martCB.addActionListener(this);
		martCB.setActionCommand("mart");
		
		configCB = new JComboBox();
		configCB.addActionListener(this);
		configCB.setActionCommand("config");
		
		martPanel.add(martLabel);
		martPanel.add(martCB);
		martPanel.add(configLabel);
		martPanel.add(configCB);
		
		JButton okButton = new JButton(Resources.get("OK"));
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		this.add(martPanel,BorderLayout.NORTH);
		this.add(this.createCentralPanel(),BorderLayout.CENTER);
		this.add(buttonPanel,BorderLayout.SOUTH);
		
		this.initMartCB();
		this.martCB.setSelectedIndex(0);
	}
	
	private void initMartCB() {
		List<Mart> martList = McGuiUtils.INSTANCE.getRegistryObject().getMartList();
		for(Mart mart: martList) {
			this.martCB.addItem(mart);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("mart")) {
			JComboBox cb = (JComboBox)e.getSource();
	        Mart mart = (Mart)cb.getSelectedItem();
	        List<Config> configList = mart.getConfigList();
	        this.configCB.removeAllItems();
	        for(Config config: configList) {
	        	this.configCB.addItem(config);
	        }
	        this.configCB.setSelectedIndex(0);
		}else if(e.getActionCommand().equals("config")) {
			JComboBox cb = (JComboBox)e.getSource();
			Config config = (Config)cb.getSelectedItem();
			if(config == null)
				return;
			List<Attribute> attList = config.getAttributes(new ArrayList<String>(), true, true);
			DefaultListModel model = (DefaultListModel)this.allAttList.getModel();
			model.removeAllElements();
			for(Attribute att: attList)
				model.addElement(att);
		}else if(e.getActionCommand().equals(">")) {
			addAttribute();
		}else if(e.getActionCommand().equals("<")) {
			removeAttribute();
		}else if(e.getActionCommand().equals("addlinkeddataset")) {
			
		}else if(e.getActionCommand().equals("removelinkeddataset")) {
			
		}
		
	}
	
	private JPanel createCentralPanel() {
		JPanel panel = new JPanel(new GridLayout(0,1));
		panel.add(this.createAttributePanel());
		panel.add(this.createDatasetPanel());
		return panel;
	}
	
	private JPanel createAttributePanel() {
		JPanel listPanel = new JPanel(new GridLayout());
		JPanel panel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();

		JButton addButton = new JButton(">");
		addButton.setActionCommand(">");
		addButton.addActionListener(this);
		
		JButton removeButton = new JButton("<");
		removeButton.setActionCommand("<");
		removeButton.addActionListener(this);
		
		buttonPanel.add(addButton);
		buttonPanel.add(removeButton);
		
		DefaultListModel model1 = new DefaultListModel();
		DefaultListModel model2 = new DefaultListModel();
		this.allAttList = new JList(model1);
		JScrollPane sp1 = new JScrollPane(this.allAttList);
		
		this.selectedAttList = new JList(model2);
		JScrollPane sp2 = new JScrollPane(this.selectedAttList);
		
		
		listPanel.add(sp1);
		listPanel.add(sp2);
				
		panel.add(listPanel,BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel createDatasetPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JPanel listPanel = new JPanel(new GridLayout());
		JPanel buttonPanel = new JPanel();
		
		JButton addButton = new JButton("add linked datasets");
		addButton.setActionCommand("addlinkeddataset");
		addButton.addActionListener(this);
		
		JButton removeButton = new JButton("remove linked datasets");
		removeButton.setActionCommand("removelinkeddataset");
		removeButton.addActionListener(this);
		
		buttonPanel.add(addButton);
		buttonPanel.add(removeButton);
		
		DefaultListModel model1 = new DefaultListModel();
		DefaultListModel model2 = new DefaultListModel();
		sourceDsList = new JList(model1);
		JList targetDsList = new JList(model2);
		JScrollPane sp1 = new JScrollPane(sourceDsList);
		JScrollPane sp2 = new JScrollPane(targetDsList);

		listPanel.add(sp1);
		listPanel.add(sp2);

		panel.add(listPanel,BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);		
		//set sourcedatasetlis
		this.setSourceDatasetsList();
		return panel;
	}
	
	private List<Dataset> getSourceDatasets() {
		List<Dataset> sourceDsList = new ArrayList<Dataset>();
		//get current mart
		List<Dataset> allDatasets = this.filterList.get(0).getParentConfig().getMart().getDatasetList();
		for(Dataset ds: allDatasets) {
			boolean selected = true;
			for(Filter filter: this.filterList) {
				if(!filter.inPartition(ds.getName())) {
					selected = false;
					break;
				}
			}
			if(selected)
				sourceDsList.add(ds);
		}
		return sourceDsList;
	}
	
	private void setSourceDatasetsList() {
		List<Dataset> dsList = this.getSourceDatasets();
		DefaultListModel model = (DefaultListModel)this.sourceDsList.getModel();
		for(Dataset ds: dsList) {
			model.addElement(ds);
		}
	}
	
	private void addAttribute() {
		Object[] _obj = this.allAttList.getSelectedValues();
		DefaultListModel model = (DefaultListModel) this.selectedAttList.getModel();
		for(Object obj: _obj) {
			if(!model.contains(obj)) {
				model.addElement(obj);
			}
		}
	}
	
	private void removeAttribute() {
		DefaultListModel model = (DefaultListModel) this.selectedAttList.getModel();
		model.removeElementAt(this.selectedAttList.getSelectedIndex());
	}
	
	private void addTargetDataset() {
		DefaultListModel model = (DefaultListModel) this.selectedAttList.getModel();
		
		List<Dataset> dsList = new ArrayList<Dataset>();
		Mart mart = (Mart)this.martCB.getSelectedItem();
		List<Dataset> allDsList = mart.getDatasetList();
		for(Dataset ds: allDsList) {
			boolean selected = true;
			for(int i=0; i<model.size(); i++) {
				Attribute att = (Attribute)model.get(i);
				if(!att.inPartition(ds.getName())) {
					selected = false;
					break;
				}
			}
			if(selected)
				dsList.add(ds);
		}
		
	}
}