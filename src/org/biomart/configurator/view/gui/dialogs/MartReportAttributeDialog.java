package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;

import org.biomart.common.resources.Resources;
import org.biomart.objects.objects.Attribute;

public class MartReportAttributeDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JList list;
	private Set<Attribute> selectedAttributes;
	
	public MartReportAttributeDialog(Collection<Attribute> attributes) {
		this.init(attributes);
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);			
	}
	
	private void init(Collection<Attribute> attributes) {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		JButton okButton = new JButton(Resources.get("OK"));
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		this.list = new JList();

		content.add(buttonPanel,BorderLayout.SOUTH);
		content.add(list,BorderLayout.CENTER);
	}
	
	public Set<Attribute> getSelectedAttributes() {
		return this.selectedAttributes;
	}
	
}