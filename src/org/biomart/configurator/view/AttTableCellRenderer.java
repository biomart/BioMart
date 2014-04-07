package org.biomart.configurator.view;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.biomart.configurator.model.XMLAttributeTableModel;

public class AttTableCellRenderer extends DefaultTableCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AttTableCellRenderer() {
		
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
		if(column==0 && ((XMLAttributeTableModel)table.getModel()).isCellEditable(row, 1)) {
			cell.setForeground(Color.RED);
		}else {
			cell.setForeground(Color.BLACK);
		}
		if(row%2 == 1)
			cell.setBackground(Color.LIGHT_GRAY);
		else
			cell.setBackground(Color.WHITE);
			
	    return cell;
	}
	
}