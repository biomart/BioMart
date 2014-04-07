package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;

public class MissingDatasetsDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Set<String> missingDatasets;
	
	public MissingDatasetsDialog(Set<String> missingDatasets) {
		this.missingDatasets = missingDatasets;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		Vector<String> missingDss = new Vector<String>(missingDatasets);		
		JLabel label = new JLabel("<html>This dataset contains pointer attributes and/or filters that require " +
				"links to external marts. <br> For these pointers to work, you must also add the data source " +
				"that contains these datasets.</html>");
		JList list = new JList(missingDss);
		JScrollPane sp = new JScrollPane(list);
		this.add(sp,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				MissingDatasetsDialog.this.setVisible(false);
				MissingDatasetsDialog.this.dispose();
			}
		});
		buttonPanel.add(closeButton);
		this.add(label,BorderLayout.NORTH);
		this.add(buttonPanel,BorderLayout.SOUTH);
		this.setTitle("missing datasets");
	}
	
}