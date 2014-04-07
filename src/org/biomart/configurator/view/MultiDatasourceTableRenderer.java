package org.biomart.configurator.view;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.biomart.configurator.model.MultiDatasourceModel;

public class MultiDatasourceTableRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);

		if(isSelected)
			cell.setBackground(Color.GRAY);
		else if(((MultiDatasourceModel)table.getModel()).isRowVisible(row)) {
			cell.setBackground(Color.WHITE);
		}
		else {
			cell.setBackground(Color.LIGHT_GRAY);
		}
		//else
		//	cell.setBackground(Color.LIGHT_GRAY);
		if(((MultiDatasourceModel)table.getModel()).inMultipleMart(row))
			cell.setForeground(Color.RED);
		else
			cell.setForeground(Color.BLACK);
	    return cell;
	}
}
