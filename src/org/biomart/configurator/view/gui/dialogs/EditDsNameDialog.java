package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.McUtils;

public class EditDsNameDialog extends JDialog implements DocumentListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LinkedHashMap<Integer,String> nameRowMap;
	private LinkedHashMap<Integer,JTextField> fieldMap;
	private JButton okButton;
	private boolean changed = false;
	private boolean allowEmpty = false;
	private boolean allowDuplicate = false;
	
	public EditDsNameDialog(JDialog parent, LinkedHashMap<Integer,String> map,boolean allowEmpty, boolean allowDuplicate) {
		super(parent);
		this.nameRowMap = map;
		this.init();
		this.setModal(true);
		this.setAllowEmpty(allowEmpty);
		this.setAllowDuplicate(allowDuplicate);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);

	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		this.fieldMap = new LinkedHashMap<Integer,JTextField>();
		JPanel buttonPanel = new JPanel();
		okButton = new JButton(Resources.get("OK"));
		okButton.setEnabled(false);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				executeOK();
			}			
		});
		
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				executeCancel();
			}			
		});
		
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
				
		JPanel listPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		int row = 0;
		c.gridy = row;

		for(Map.Entry<Integer, String> entry: this.nameRowMap.entrySet()) {
			JTextField field = new JTextField(40);
			field.getDocument().addDocumentListener(this);
			field.setText(entry.getValue());
			this.fieldMap.put(entry.getKey(), field);
			c.gridy = row++;
			listPanel.add(field,c);
		}
		JScrollPane sp2 = new JScrollPane(listPanel);
		
		this.add(buttonPanel,BorderLayout.SOUTH);
		this.add(sp2,BorderLayout.CENTER);
	}
	
	private void executeCancel() {
		this.changed = false;
		this.setVisible(false);
		this.dispose();
	}
	
	private void executeOK() {
		//validate
		Set<String> nameSet = new HashSet<String>();
		for(Map.Entry<Integer, JTextField> entry: this.fieldMap.entrySet()) {
			String value = entry.getValue().getText();
			if(!this.allowEmpty && McUtils.isStringEmpty(value)) {
				JOptionPane.showMessageDialog(this, "empty field");
				return;
			}
			if(!this.allowDuplicate && nameSet.contains(value)) {
				JOptionPane.showMessageDialog(this, "duplicate name");
				return;
			}
			nameSet.add(value);
		}
		for(Map.Entry<Integer, JTextField> entry: this.fieldMap.entrySet()) {
			String value = entry.getValue().getText();
			this.nameRowMap.put(entry.getKey(), value);
		}
		this.changed = true;
		this.setVisible(false);
		this.dispose();
	}
	
	public boolean changed() {
		return this.changed;
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		this.okButton.setEnabled(true);
		
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		this.okButton.setEnabled(true);
		
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		this.okButton.setEnabled(true);
		
	}

	/**
	 * @return the allowEmpty
	 */
	public boolean isAllowEmpty() {
		return allowEmpty;
	}

	/**
	 * @param allowEmpty the allowEmpty to set
	 */
	public void setAllowEmpty(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}

	/**
	 * @return the allowDuplicate
	 */
	public boolean isAllowDuplicate() {
		return allowDuplicate;
	}

	/**
	 * @param allowDuplicate the allowDuplicate to set
	 */
	public void setAllowDuplicate(boolean allowDuplicate) {
		this.allowDuplicate = allowDuplicate;
	}
}