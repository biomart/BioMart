package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.model.*;

/**
 * 
 * @author yliang
 *
 */
public class PartitionsSelectDialog extends JDialog implements ActionListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton cancelButton;
	private JButton okButton;
	private PartitionMatchModel pmBean;
	private JTable table;
	private static PartitionsSelectDialog instance;
	private boolean abort =true;

	public static PartitionsSelectDialog getInstance() {
		if(instance == null)
			instance = new PartitionsSelectDialog();
		return instance;
	}
	
	private PartitionsSelectDialog() {
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		this.setContentPane(content);
		this.setModal(true);
		
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		this.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent we) {
		    		 Window parent = SwingUtilities.getWindowAncestor((Component) we.getSource());
                     // Close the popup window
		    		 abort = true;
                     parent.dispose(); 
		    }
		});


        table = new JTable();
        table.setPreferredScrollableViewportSize(new Dimension(100, 70));
//        table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(500,100));

        //Add the scroll pane to this panel.
        content.add(scrollPane,BorderLayout.NORTH);
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());
        content.add(buttonsPanel,BorderLayout.SOUTH);
        
        
        cancelButton = new JButton(Resources.get("CANCEL"));
        cancelButton.setActionCommand("CANCEL");
        cancelButton.addActionListener(this);
        okButton = new JButton(Resources.get("OK"));
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);

        buttonsPanel.add(cancelButton);
        buttonsPanel.add(okButton);
        
        this.pack();
        this.setLocationRelativeTo(null);
        
    }
	
	public void setTableModel(AbstractTableModel model) {
		this.table.setModel(model);
	}
	
	public Object getValueAt(int row, int col) {
		return this.table.getModel().getValueAt(row, col);
	}
	
	public int getRowCount() {
		return this.table.getRowCount();
	}

	public void actionPerformed(ActionEvent ae) {
		if(ae.getActionCommand().equals("OK")) {
			this.abort = false;
		}
		else if(ae.getActionCommand().equals("CANCEL")) {
			this.abort = true;
		}
		Window parent = SwingUtilities.getWindowAncestor((Component) ae.getSource());
        parent.dispose(); 
	}
	
	public boolean aborted() {
		return this.abort;
	}
	
	public String getRangeString(boolean source) {
		int col = 1;
		if(source)
			col = 0;
		StringBuffer sb = new StringBuffer();
		PartitionMatchTableModel model = (PartitionMatchTableModel)this.table.getModel();
		for(int i=0; i<this.table.getRowCount();i++) {
			if((Boolean)model.getValueAt(i, 2)==true) {
				sb.append("[");
				PTCellObject cell = (PTCellObject)model.getValueAt(i, col);
				sb.append(cell.getTableName()+"R");
				sb.append(cell.getRow()+":1]"); //1 means visible
			}
		}
		return sb.toString();
	}
	
	
}