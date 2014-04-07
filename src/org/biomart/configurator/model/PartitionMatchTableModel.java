package org.biomart.configurator.model;

import java.util.List;

import javax.swing.table.AbstractTableModel;
import org.biomart.configurator.model.PartitionMatchBean;

public class PartitionMatchTableModel extends AbstractTableModel {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String[] columnNames = {"Source",
                                        "Target",
                                        "Select"
									};
        private Object[][] data;

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
        	if(c==2)
        		return Boolean.class;
        	else
        		return PTCellObject.class;
//            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col < 2) {
                return false;
            } else {
                return true;
            }
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
 
            data[row][col] = value;
            fireTableCellUpdated(row, col);
         }
        
        public void setObjectDataArray(List<PartitionMatchBean> aList) {
        	this.data = new Object[aList.size()][3];
        	for(int i=0; i<aList.size(); i++) {
        		this.data[i][0] = aList.get(i).getSource();
        		this.data[i][1] = aList.get(i).getTarget();
        		this.data[i][2] = aList.get(i).isCheck();
        	}
        }
        
        public PartitionMatchTableModel(List<PartitionMatchBean> aList) {
        	super();
        	setObjectDataArray(aList);
        }

}
