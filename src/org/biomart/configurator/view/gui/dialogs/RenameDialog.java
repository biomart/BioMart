package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.MartPointer;

public class RenameDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MartConfiguratorObject object;
	private JTextField nameField;
	private JCheckBox updateRefCB;
	
	public RenameDialog(JDialog parent,MartConfiguratorObject mcObj) {
		super(parent);
		this.object = mcObj;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel inputPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		
		JLabel nameLabel = new JLabel("new name: ");
		this.nameField = new JTextField(20);
		this.updateRefCB = new JCheckBox("Update references");
		this.updateRefCB.setSelected(true);
		this.updateRefCB.setEnabled(false);
		
		inputPanel.add(nameLabel);
		inputPanel.add(this.nameField);
		inputPanel.add(this.updateRefCB);
		
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				RenameDialog.this.setVisible(false);
				RenameDialog.this.dispose();	
			}			
		});
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rename();
			}			
		});
		
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		content.add(inputPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle("rename object");
	}
	
	private boolean rename() {
		//find all references
		String newName = this.nameField.getText();
		if(McUtils.isStringEmpty(newName)) {
			JOptionPane.showMessageDialog(this, "name is empty", "error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(this.object instanceof Attribute) {
			Attribute a = (Attribute)this.object;
			MartController.getInstance().renameAttribute((Attribute)this.object, newName, null);
		}else if(this.object instanceof Filter) {
			MartController.getInstance().renameFilter((Filter)this.object, newName,null);
		}else if(this.object instanceof Container) {
//			List<MartConfiguratorObject> references = ((Container)object).getReferences();
		}else if(this.object instanceof Mart) {
			List<MartConfiguratorObject> references = ((Mart)object).getReferences();
			//FIXME hardcode for now
			for(MartConfiguratorObject refObj: references) {
				((MartPointer)refObj).setProperty(XMLElements.MART,newName);
			}
		}

		this.object.setName(newName);
		return true;
	}
}