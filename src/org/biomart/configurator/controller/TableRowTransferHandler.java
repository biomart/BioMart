package org.biomart.configurator.controller;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.model.MultiDatasourceModel;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;

public class TableRowTransferHandler extends TransferHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<Mart> martList;
	
	public TableRowTransferHandler(List<Mart> martList) {
		this.martList = martList;
	}
	
	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		 if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
	            return false;
	        }
	        return true;
	}
	
	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        JTable table = (JTable)support.getComponent();
        MultiDatasourceModel tableModel = (MultiDatasourceModel)table.getModel();
        JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
        int index = dl.getRow();
        int max = table.getModel().getRowCount();
        if (index < 0 || index > max)
           index = max;
        // Get the string that is being dropped.
        Transferable t = support.getTransferable();
        String dsName = null;
        try {
        	dsName = (String)t.getTransferData(DataFlavor.stringFlavor);        
        	int rowFrom = tableModel.getRowByName(dsName);
        	if(index-1==rowFrom)
        		return false; //not moved
        	tableModel.reorder(rowFrom, index);
        } 
        catch (Exception e) { 
        	return false; 
        }
        //change in each mart
        for(Mart mart: this.martList) {
        	//check the dataset exist in mart
        	PartitionTable pt = mart.getSchemaPartitionTable();
        	int rowFrom = pt.getRowNumberByDatasetSuffix(dsName);
        	if(rowFrom>=0 && index <= pt.getTotalRows()) {
        		pt.reorder(rowFrom, index);
        	}
        }
        return true;
	}
	
	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY_OR_MOVE;
	}
	
/*	@Override
	public void exportDone(JComponent c, Transferable t, int action) {

	}*/
	
	@Override
	public Transferable createTransferable(JComponent c) {
		String selValue = "";
		if(c instanceof JTable){
			int row = ((JTable) c).getSelectedRow();
			//common name
			MultiDatasourceModel model = (MultiDatasourceModel)((JTable)c).getModel();
			selValue = (String)model.getValueAt(row, PartitionUtils.DSM_DATASETNAME);			
		}
		return new StringSelection(selValue);
	}

}