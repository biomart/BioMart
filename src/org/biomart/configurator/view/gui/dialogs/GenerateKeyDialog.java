package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Filter;


public class GenerateKeyDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField nameTF;
	private JTextField displayNameTF;
	private Filter filterList;
	private String key;
	
	public GenerateKeyDialog() {
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
		JButton okButton = new JButton(Resources.get("GENERATEKEY"));
		JButton saveButton = new JButton(Resources.get("SAVE"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(generateKey())
					GenerateKeyDialog.this.setVisible(false);				
			}			
		});
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		buttonPanel.add(saveButton);
		
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		JLabel nameLabel = new JLabel(XMLElements.NAME.toString());
		inputPanel.add(nameLabel, c);
		
		this.nameTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.nameTF, c);
		
		JLabel displayName = new JLabel(XMLElements.DISPLAYNAME.toString());
		c.gridx = 0;
		c.gridy = 1;
		inputPanel.add(displayName,c);
		
		this.displayNameTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.displayNameTF, c);

		content.add(inputPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle(Resources.get("CREATEFILTERLISTTITLE"));
	}
	
	private boolean generateKey() {
		String initStr = this.nameTF.getText();
		if(McUtils.isStringEmpty(initStr)) {
			return false;
		}
		return true;
	}
	
	public Filter getCreatedFilterList() {
		return this.filterList;
	}
}