package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.biomart.common.resources.Resources;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.MartPointer;

public class AddDsToMartPointerDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Mart mart;
	private String dsName;
	private List<MartPointer> mpList;
	private List<JCheckBox> cbList;

	public AddDsToMartPointerDialog(Mart mart, String datasetName) {
		this.mart = mart;
		this.dsName = datasetName;
		this.init();
		this.setModal(true);
//		this.pack();
		this.setSize(400,300);
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		//find all martpointer related to the mart
		List<MartConfiguratorObject> refObjs = mart.getReferences();
		this.mpList = new ArrayList<MartPointer>();
		for(MartConfiguratorObject obj: refObjs) {
			this.mpList.add((MartPointer)obj); 
		}
		this.cbList = new ArrayList<JCheckBox>();
		JPanel content = new JPanel(new BorderLayout());
		JPanel inputPanel = new JPanel(new GridBagLayout());
		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateMartPointer();
				AddDsToMartPointerDialog.this.setVisible(false);
			}			
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AddDsToMartPointerDialog.this.setVisible(false);
			}
		});
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;

		for(MartPointer mp: mpList) {
			JCheckBox cb = new JCheckBox(mp.getMart().getName(),true);
			inputPanel.add(cb,c);
			this.cbList.add(cb);
			c.gridy ++;
		}
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		content.add(buttonPanel,BorderLayout.SOUTH);
		content.add(inputPanel,BorderLayout.NORTH);
		this.add(content);
		this.setTitle("Add new dataset in portal and update links?");
	}
	
	private void updateMartPointer() {
	}

}