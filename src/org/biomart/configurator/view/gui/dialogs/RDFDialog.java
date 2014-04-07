package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.biomart.common.resources.Resources;

public class RDFDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean result = false;
	private boolean exposed;
	private JCheckBox exposedCB;
	
	public RDFDialog() {
		init();
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JLabel label = new JLabel(Resources.get("RDFWARNING"),UIManager.getIcon("OptionPane.warningIcon"),JLabel.LEFT);
		JPanel buttonPanel = new JPanel();

		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				RDFDialog.this.result = false;
				RDFDialog.this.setVisible(false);
				RDFDialog.this.dispose();	
			}			
		});
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exposed = RDFDialog.this.exposedCB.isSelected();
				RDFDialog.this.result = true;
				RDFDialog.this.setVisible(false);
				RDFDialog.this.dispose();
			}			
		});
		
		this.exposedCB = new JCheckBox(Resources.get("PSEUDOEXPOSED"));
		
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		buttonPanel.add(this.exposedCB);
		
		content.add(label, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle("Warning");
		this.pack();
		this.setLocationRelativeTo(null);
		this.setModal(true);
		this.setVisible(true);
	}
	
	public boolean getResult() {
		return this.result;
	}
	
	public boolean isPseudoExposed() {
		return this.exposed;
	}
}