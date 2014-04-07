package org.biomart.configurator.view;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

public class PartitionTableHeadRenderer extends DefaultTableCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
		
		if(column<15)
			setText("*"+value.toString());
		else
			setText(value.toString()); 
		setBorder(UIManager.getBorder("TableHeader.cellBorder")); 
		return cell;
	}
	
}