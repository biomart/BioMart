package org.biomart.configurator.view;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.biomart.configurator.component.PtModel;


public class PartitionTableCellRenderer extends DefaultTableCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);

		if(isSelected)
			cell.setBackground(Color.GRAY);
		else if(((PtModel)table.getModel()).isRowVisible(row)) {
			cell.setBackground(Color.WHITE);
		}
		else {
			cell.setBackground(Color.LIGHT_GRAY);
		}
					
	    return cell;
	}

}