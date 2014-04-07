/**
 * 
 */
package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.common.resources.Resources;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.portal.MartPointer;

/**
 * @author lyao
 *
 */
public class DatasetSelectionDialog extends JDialog implements ActionListener{
	

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private JList dsList;
		private String selectedDataset;
		
		private List<String> datasets = new ArrayList<String>();
		
		public DatasetSelectionDialog(JDialog parent, String[] datasets) {
			super(parent);
			if(datasets != null){
				for(String ds: datasets){
					this.datasets.add(ds);
				}
			}
			if(init())
			{
				this.setModal(true);
				this.pack();
				this.setLocationRelativeTo(null);
				this.setVisible(true);
			}
		}
		
		private boolean init() {
			this.setTitle("Choose the dataset for link");
			this.setPreferredSize(new Dimension(300,400));
			JPanel content = new JPanel(new BorderLayout());
			JPanel buttonPanel = new JPanel();
			
			JButton okButton = new JButton(Resources.get("OK"));
			okButton.setActionCommand(Resources.get("OK"));
			okButton.addActionListener(this);
			
			buttonPanel.add(okButton);
			
			DefaultListModel model = new DefaultListModel();
			dsList = new JList(model);
			dsList.setSize(200,300);
			dsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane sp = new JScrollPane(dsList);
			
			//add mart
			
			
			for(String ds : this.datasets) {
				model.addElement(ds);
			}
		
			
			content.add(sp,BorderLayout.CENTER);
			content.add(buttonPanel,BorderLayout.SOUTH);
			this.add(content);
			
			return true;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals(Resources.get("OK"))) {
				if(dsList.getSelectedValue() == null){
					JOptionPane.showMessageDialog(this, "Please select a dataset from the list first.");
					return;
				}
				
				this.selectedDataset = (String)dsList.getSelectedValue();
				
				this.setVisible(false);
				this.dispose();
			}	
		}
		
		public String getSelectedDataset() {
			return this.selectedDataset;
		}
		
	
}
