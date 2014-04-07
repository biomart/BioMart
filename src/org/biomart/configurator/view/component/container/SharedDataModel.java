package org.biomart.configurator.view.component.container;

import javax.swing.DefaultListModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.biomart.common.utils.XMLElements;
import org.biomart.objects.portal.MartPointer;

public class SharedDataModel extends DefaultListModel implements TableModel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String[] columnNames;

    public SharedDataModel(String[] columnNames) {
        super();
        this.columnNames = columnNames;
    }

    public void rowChanged(int row) {
        fireContentsChanged(this, row, row); 
    }

    private TableModel tableModel = new AbstractTableModel() {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public String getColumnName(int column) {
            return columnNames[column];
        }
        public int getRowCount() { 
            return size();
        }
        public int getColumnCount() {
            return columnNames.length;
        }
        public Object getValueAt(int row, int column) {
        	MartPointer mp = (MartPointer)elementAt(row);
            if(columnNames[column].equals("Group")){
            	return mp.getProperty(XMLElements.GROUP);
            }else if(columnNames[column].equals("Name")){
            	return mp.getConfig().getProperty(XMLElements.DISPLAYNAME);
            }else if(columnNames[column].equals("Dataset")){
            	return mp.getMart().getDatasetList().get(0).getDisplayName();
            }else if(columnNames[column].equals("Source")){
            	return mp.getMart().getProperty(XMLElements.DISPLAYNAME);
            }
            return "";
        }
        public boolean isCellEditable(int row, int column) {
            return false;
        }
        public void setValueAt(Object value, int row, int column) {
            String newValue = (String)value;
            MartPointer mp = (MartPointer)elementAt(row);
            if(columnNames[column].equals("Group")){
            	mp.setProperty(XMLElements.GROUP, newValue);
            }else if(columnNames[column].equals("Name")){
            	mp.getConfig().setProperty(XMLElements.DISPLAYNAME, newValue);
            }else if(columnNames[column].equals("Description")){
            	mp.getConfig().setProperty(XMLElements.DESCRIPTION, newValue);
            }else if(columnNames[column].equals("Source")){
            	mp.getMart().setProperty(XMLElements.DISPLAYNAME, newValue);
            }
            
            fireTableCellUpdated(row, column); //table event
            rowChanged(row);                   //list event
        }
    };

    //Implement the TableModel interface.
    public int getRowCount() {
        return tableModel.getRowCount();
    }
    public int getColumnCount() {
        return tableModel.getColumnCount();
    }
    public String getColumnName(int columnIndex) {
        return tableModel.getColumnName(columnIndex);
    }
    public Class getColumnClass(int columnIndex) {
        return tableModel.getColumnClass(columnIndex);
    }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return tableModel.isCellEditable(rowIndex, columnIndex);
    }
    public Object getValueAt(int rowIndex, int columnIndex) {
        return tableModel.getValueAt(rowIndex, columnIndex);
    }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        tableModel.setValueAt(aValue, rowIndex, columnIndex);
    }
    public void addTableModelListener(TableModelListener l) {
        tableModel.addTableModelListener(l);
    }
    public void removeTableModelListener(TableModelListener l) {
        tableModel.removeTableModelListener(l);
    }
}
