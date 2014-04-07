package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JScrollPane;

import org.biomart.configurator.component.PartitionTablePanel;
import org.biomart.objects.objects.PartitionTable;

public class PartitionTableDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PartitionTable ptable;
	
	public PartitionTableDialog(JDialog parent, PartitionTable ptable, int start, int end) {
		super(parent);
		this.ptable = ptable;
		init(start,end);
		this.setModal(true);
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    this.setSize(screenSize.width-100,screenSize.height - 100);
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init(int start, int end) {
		this.setLayout(new BorderLayout());
		PartitionTablePanel pp = new PartitionTablePanel(ptable,start,end);
		JScrollPane sp = new JScrollPane(pp);
		this.add(sp,BorderLayout.CENTER);
	}
	
}