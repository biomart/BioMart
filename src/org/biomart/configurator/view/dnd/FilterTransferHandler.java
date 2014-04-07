package org.biomart.configurator.view.dnd;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;
import org.biomart.objects.objects.Filter;

public class FilterTransferHandler extends TransferHandler {

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
        	String attName = (String)t.getTransferData(DataFlavor.stringFlavor);
            //use the first attribute to get the parentconfig
            Filter a =  ((Filter)listModel.get(0)).getParentConfig().getMart()
            	.getMasterConfig().getFilterByName(attName, new ArrayList<String>());

        	listModel.add(index, a);
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
	    }
	}
	
	@Override
	public Transferable createTransferable(JComponent c) {
		String selValue = "";
		if(c instanceof JList){
			selValue = ((Filter)((JList) c).getSelectedValue()).getName();
			
		}
		return new StringSelection(selValue);
	}

}