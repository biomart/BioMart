package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.AddConfigInfo;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Mart;

public class AddConfigDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Mart mart;
	private JTextField nameField;
	private JCheckBox naiveCB;
	private AddConfigInfo configInfo;
	private boolean report;
	
	//private JCheckBox rdfCB;
	
	public AddConfigDialog(Mart mart, boolean report) {
		this.mart = mart;
		this.report = report;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		String defaultName = McGuiUtils.INSTANCE.getUniqueConfigName(mart, mart.getName()+ (this.report?"_report":"_config"));
		JPanel content = new JPanel(new BorderLayout());
		JPanel inputPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		
		JLabel nameLabel = new JLabel("Name");
		this.nameField = new JTextField(20);
		this.nameField.setText(defaultName);
		this.naiveCB = new JCheckBox("blank");
		this.naiveCB.setSelected(false);
		//this.rdfCB = new JCheckBox("generate RDF-triples");
		//this.rdfCB.setSelected(true);
		//this.rdfCB.setEnabled(true);
		
		inputPanel.add(nameLabel);
		inputPanel.add(this.nameField);
		if(!this.report)
			inputPanel.add(this.naiveCB);
		//inputPanel.add(this.rdfCB);
		
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				AddConfigDialog.this.configInfo = null;
				AddConfigDialog.this.setVisible(false);
				AddConfigDialog.this.dispose();	
			}			
		});
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				if(isNameValid(AddConfigDialog.this.nameField.getText())) {
					AddConfigDialog.this.configInfo = new AddConfigInfo();
					AddConfigDialog.this.configInfo.setName(AddConfigDialog.this.nameField.getText());
					AddConfigDialog.this.configInfo.setDoNaive(!AddConfigDialog.this.naiveCB.isSelected());
					AddConfigDialog.this.setVisible(false);
					AddConfigDialog.this.dispose();
				}else {
					
				}
			}			
		});
		
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		content.add(inputPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle(Resources.get("ADDACCESSPOINT"));
	}
	
	private boolean isNameValid(String name) {
		if(name.isEmpty()){
			JOptionPane.showMessageDialog(AddConfigDialog.this, "Config name can not be empty");
			return false;
		}
		else if(this.mart.getConfigByName(name)==null)
			return true;
		else{
			JOptionPane.showMessageDialog(AddConfigDialog.this, "Config already exist");
			return false;
		}
	}
	
	public AddConfigInfo getConfigInfo() {
		return this.configInfo;
	}
	/*
	public boolean isGenerateRDF() {
		// TODO Auto-generated method stub
		return this.rdfCB.isSelected();
	}
	*/
}