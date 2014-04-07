package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.PartitionTable;

public class AddPseudoAttributeDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField nameTF;
	private JTextField displayNameTF;
	private JTextField descriptionTF;
	private Attribute attribute;
	private JTable table;
	private final Config config; 
	private final boolean edit;
	
	public AddPseudoAttributeDialog(Config config, JDialog parent) {
		super(parent);
		this.config = config;
		this.edit = false;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	public AddPseudoAttributeDialog(Config config, Attribute attribute, JDialog parent) {
		super(parent);
		this.config = config;
		this.edit = true;
		this.attribute = attribute;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(createAttribute())
					AddPseudoAttributeDialog.this.setVisible(false);				
			}			
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					AddPseudoAttributeDialog.this.setVisible(false);				
			}						
		});
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		JLabel nameLabel = new JLabel(XMLElements.NAME.toString());
		inputPanel.add(nameLabel, c);
		
		this.nameTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.nameTF, c);
		
		JLabel displayName = new JLabel(XMLElements.DISPLAYNAME.toString());
		c.gridx = 0;
		c.gridy = 1;
		inputPanel.add(displayName,c);
		
		this.displayNameTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.displayNameTF, c);

		JLabel descriptionName = new JLabel(XMLElements.DESCRIPTION.toString());
		c.gridx = 0;
		c.gridy = 2;
		inputPanel.add(descriptionName,c);
		
		this.descriptionTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.descriptionTF,c);
		

		content.add(inputPanel, BorderLayout.NORTH);
		content.add(this.createCentralPanel(), BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle(Resources.get("CREATEATTRIBUTETITLE"));
		this.setFieldEnable(!edit);
		this.reloadPartitionTable();
	}
	
	private JScrollPane createCentralPanel() {
		PartitionTable pt = this.config.getMart().getSchemaPartitionTable();
		MyTableModel model = new MyTableModel(pt);
		table = new JTable(model);
		table.putClientProperty("terminateEditOnFocusLost", true);
		table.setShowGrid(true);
		table.setGridColor(Color.BLACK);
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(400,300));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);	
		return sp;
	}
	
	private boolean createAttribute() {
		if(McUtils.isStringEmpty(this.nameTF.getText())) {
			JOptionPane.showMessageDialog(this, Resources.get("EMPTYNAME"));
			return false;
		}
		//check unique name
		if(!edit && !McGuiUtils.INSTANCE.checkUniqueAttributeInConfig(config, this.nameTF.getText())) {
			JOptionPane.showMessageDialog(this,
				    this.nameTF.getText()+" already exist.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);

			return false;
		}
		if(edit) {
			//add values to partitiontable
			PartitionTable pt = this.config.getMart().getSchemaPartitionTable();
			String value = this.attribute.getValue();
			int col = McUtils.getPartitionColumnValue(value);
			for(int i=0; i<pt.getTotalRows(); i++) {
				pt.setValue(i, col, (String)table.getValueAt(i, 2));
			}
		}else {
			this.attribute = new Attribute(this.nameTF.getText(), this.displayNameTF.getText());
			this.attribute.setDescription(this.descriptionTF.getText());
			//add values to partitiontable
			PartitionTable pt = this.config.getMart().getSchemaPartitionTable();
			int col = pt.addColumn("");
			for(int i=0; i<pt.getTotalRows(); i++) {
				pt.setValue(i, col, (String)table.getValueAt(i, 2));
			}
			this.attribute.setProperty(XMLElements.VALUE, "(p0c"+col+")");
		}
		return true;
	}
	
	public Attribute getCreatedAttribute() {
		return this.attribute;
	}

	private void setFieldEnable(boolean b) {
		this.nameTF.setEditable(b);
		this.displayNameTF.setEnabled(b);
		this.descriptionTF.setEditable(b);
	}
	
	private void reloadPartitionTable() {
		if(this.edit) {
			this.nameTF.setText(this.attribute.getName());
			this.displayNameTF.setText(this.attribute.getDisplayName());
			this.descriptionTF.setText(this.attribute.getDescription());
			MyTableModel model = (MyTableModel)table.getModel();
			PartitionTable pt = this.config.getMart().getSchemaPartitionTable();
			String value = this.attribute.getValue();
			int col = McUtils.getPartitionColumnValue(value);
			for(int i=0; i<pt.getTotalRows(); i++) {
				model.setValueAt(pt.getValue(i, col), i, 2);
			}
		}
	}

	
	class MyTableModel extends AbstractTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private PartitionTable pt;
		private List<List<String>> data;
		
		public MyTableModel(PartitionTable pt) {
			this.pt = pt;		
			//init table
			data = new ArrayList<List<String>>();
			for(int i=0; i<pt.getTotalRows(); i++) {
				List<String> row = new ArrayList<String>();
				row.add(pt.getValue(i,PartitionUtils.DATASETNAME));
				row.add(pt.getValue(i,PartitionUtils.DISPLAYNAME));
				row.add("");
				data.add(row);
			}
		}
		
		@Override
		public int getColumnCount() {
			return 3;
		}
		
		@Override
		public String getColumnName(int col) {
			if(col==0)
				return XMLElements.DATASET.toString();
			else if(col==1)
				return XMLElements.DISPLAYNAME.toString();
			else
				return XMLElements.VALUE.toString();
		}


		@Override
		public int getRowCount() {
			return this.pt.getTotalRows();
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data.get(row).get(col);
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if(columnIndex<2)
				return false;
			else
				return true;
		}
		
		@Override
		public void setValueAt(Object value, int row, int col) {
			if(col == 2) {
				this.data.get(row).set(2, (String)value);
			}
		}
		
	}
}