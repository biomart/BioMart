package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.biomart.api.enums.Operation;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.MartPointer;

public class AddMartPointerDialog extends JDialog implements ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField nameTF;
	private JComboBox martCB;
	private JComboBox configCB;
	private JComboBox operationCB;
	private JTextField iconTF;
	private MartPointer martPointer;
	private JList datasetsList;
	
	public AddMartPointerDialog() {
		this.init();
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
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(createMartPointer())
				AddMartPointerDialog.this.setVisible(false);				
			}			
		});
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		JLabel nameLabel = new JLabel(XMLElements.NAME.toString());
		inputPanel.add(nameLabel, c);
		
		c.gridx = 1;
		this.nameTF = new JTextField(10);
		inputPanel.add(this.nameTF, c);
		
		JLabel martLabel = new JLabel(XMLElements.MART.toString());
		c.gridx = 0;
		c.gridy = 1;
		inputPanel.add(martLabel,c);
		
		martCB = new JComboBox();

		c.gridx = 1;
		inputPanel.add(this.martCB,c);
		
		JLabel configLabel = new JLabel(XMLElements.CONFIG.toString());
		c.gridx = 0;
		c.gridy = 2;
		inputPanel.add(configLabel,c);
		
		this.configCB = new JComboBox();
		c.gridx = 1;
		inputPanel.add(this.configCB,c);
		
		JLabel operationLabel = new JLabel(XMLElements.OPERATION.toString());
		c.gridx = 0;
		c.gridy = 3;
		inputPanel.add(operationLabel, c);
		
		this.operationCB = new JComboBox();
		for(Operation o: Operation.values()) 
			this.operationCB.addItem(o);
		c.gridx = 1;
		inputPanel.add(this.operationCB,c);
		
		JLabel iconLabel = new JLabel(XMLElements.ICONS.toString());
		c.gridx = 0;
		c.gridy = 4;
		inputPanel.add(iconLabel,c);
		
		this.iconTF = new JTextField(10);
		c.gridx = 1;
		inputPanel.add(this.iconTF,c);
		
		JLabel datasetsLabel = new JLabel(XMLElements.DATASETS.toString());
		c.gridx = 0;
		c.gridy = 5;
		inputPanel.add(datasetsLabel,c);
		
		DefaultListModel model = new DefaultListModel();
		this.datasetsList = new JList(model);
		JScrollPane sp = new JScrollPane(this.datasetsList);
		sp.setPreferredSize(new Dimension(200,200));
		c.gridx = 1;
		inputPanel.add(sp,c);
		
		martCB.addItemListener(this);
		List<Mart> martList = this.getMarts();
		for(Mart mart:martList) {
			martCB.addItem(mart);
		}
		if(martCB.getItemCount()>0)
			martCB.setSelectedIndex(0);
		content.add(inputPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle(Resources.get("CREATEMARTPOINTERTITLE"));
	}
	
	private List<Mart> getMarts() {
		return McGuiUtils.INSTANCE.getRegistryObject().getMartList();
	}

	public void itemStateChanged(ItemEvent event) {
		if (event.getStateChange() == ItemEvent.SELECTED) {
			Object object = event.getSource();
			if(object instanceof JComboBox) {
				//update config combobox and dataset list
				Object item = ((JComboBox)object).getSelectedItem();
				this.configCB.removeAllItems();
				((DefaultListModel)this.datasetsList.getModel()).removeAllElements();
				if(item!=null) {
					Mart mart = (Mart)item;
					for(Config config: mart.getConfigList()) {
						this.configCB.addItem(config);
					}	
					for(Dataset ds: mart.getDatasetList()) {
						((DefaultListModel)this.datasetsList.getModel()).addElement(ds);
					}
				}
			}
		}	
	}
	
	private boolean createMartPointer() {
		String mpName = this.nameTF.getText();
		if(mpName == null || mpName.trim().equals("")) {
			JOptionPane.showMessageDialog(this, "no martpointer name");
			return false;
		}
		Object martO = this.martCB.getSelectedItem();
		if(martO == null) {
			JOptionPane.showMessageDialog(this, "no mart");
			return false;
		}
		Mart mart = (Mart)martO;
		Object configO = this.configCB.getSelectedItem();
		if(configO == null) {
			JOptionPane.showMessageDialog(this, "no data");
			return false;
		}
		Config config = (Config)configO;
		Operation o = (Operation)this.operationCB.getSelectedItem();
		Object[] datasets = this.datasetsList.getSelectedValues();
		if(datasets==null || datasets.length ==0) {
			JOptionPane.showMessageDialog(this, "no datasets");
			return false;			
		}
			
		this.martPointer = new MartPointer(config,mpName);
		this.martPointer.setIconName(this.iconTF.getText());
		this.martPointer.setOperation(o);

		
		return true;
	}
	
	public MartPointer getMartPointer() {
		return this.martPointer;
	}
}