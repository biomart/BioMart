package org.biomart.configurator.controller;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import org.biomart.common.utils.McEventBus;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McEventProperty;

public class ListTransferHandler extends TransferHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		
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

        JList list = (JList)support.getComponent();
        DefaultListModel listModel = (DefaultListModel)list.getModel();
        JList.DropLocation dl = (JList.DropLocation)support.getDropLocation();
        int index = dl.getIndex();

        // Get the string that is being dropped.
        Transferable t = support.getTransferable();
        
        try {
        	String fdName = (String)t.getTransferData(DataFlavor.stringFlavor);
        	String[] _data = McUtils.getOptionsDataFromString(fdName);
        	FilterData data = new FilterData(_data[0], _data[1],Boolean.parseBoolean(_data[2]));
        	listModel.add(index, data);
        } 
        catch (Exception e) { 
        	return false; 
        }

        return true;
	}
	
	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY_OR_MOVE;
	}
	
	@Override
	public void exportDone(JComponent c, Transferable t, int action) {
	    if (action == TransferHandler.MOVE) {
            JList source = (JList)c;
            DefaultListModel model  = (DefaultListModel)source.getModel();
            int i = source.getSelectedIndex();
            model.remove(i);
            McEventBus.getInstance().fire(McEventProperty.FILTEROPTION_CHANGED.toString(), source);
	    }
	}
	
	@Override
	public Transferable createTransferable(JComponent c) {
		String selValue = "";
		if(c instanceof JList){
			//selValue = ((FilterData)((JList) c).getSelectedValue()).getName();
			selValue = ((FilterData)((JList) c).getSelectedValue()).toSavedFormat();
		}
		return new StringSelection(selValue);
	}
	

}