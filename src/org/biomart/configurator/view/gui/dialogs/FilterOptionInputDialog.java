package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.McUtils;

public class FilterOptionInputDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea ta;
	private List<String> result;
	
	public FilterOptionInputDialog(Dialog owner, boolean modal) {
		super(owner,modal);
		init();
		this.setLocationRelativeTo(owner);
		this.pack();
		this.setModal(true);
		this.setVisible(true);
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		this.ta = new JTextArea();
		
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.setActionCommand(Resources.get("OK"));
		okButton.addActionListener(this);
		
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		cancelButton.setActionCommand(Resources.get("CANCEL"));
		cancelButton.addActionListener(this);
		
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		this.add(buttonPanel,BorderLayout.SOUTH);
		this.add(ta,BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(Resources.get("OK"))) {
			setInput();
		}else if(e.getActionCommand().equals(Resources.get("CANCEL"))) {
			result = new ArrayList<String>();
		}
		this.setVisible(false);
		this.dispose();		
	}
	
	private void setInput() {
		result = new ArrayList<String>();
		String text = this.ta.getText();
		String[] _strs = text.split("\\n");
		for(String str: _strs) {
			str = str.trim();
			if(!McUtils.isStringEmpty(str)) 
				result.add(str);
		}
	}
	
	public List<String> getInputs() {
		return this.result;
	}
}