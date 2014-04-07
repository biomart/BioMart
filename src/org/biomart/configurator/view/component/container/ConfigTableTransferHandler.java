package org.biomart.configurator.view.component.container;

import javax.swing.TransferHandler;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.datatransfer.*;
import javax.swing.*;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.common.resources.Log;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;

public class ConfigTableTransferHandler extends TransferHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<MartPointer> data = new ArrayList<MartPointer>();
	GuiContainerPanel rootPanel = null;
	
	ConfigTableTransferHandler(GuiContainerPanel gcp){
		this.rootPanel = gcp;
	}
    /**
     * Perform the actual data import.
     */
    public boolean importData(TransferHandler.TransferSupport info) {
        

        //If we can't handle the import, bail now.
        if (!canImport(info)) {
            return false;
        }
        
        if (info.isDataFlavorSupported(TransferableConfig.MART_POINTER_FLAVOR)) {
        	JTable table = (JTable)info.getComponent();
        	SharedDataModel model = (SharedDataModel)table.getModel();
        	GuiContainer gc = this.rootPanel.getGuiContainer();
            //Fetch the data -- bail if this fails
            try {
                data = (List<MartPointer>) info.getTransferable().getTransferData(TransferableConfig.MART_POINTER_FLAVOR);
            } catch (UnsupportedFlavorException ufe) {
                Log.error("importData: unsupported data flavor");
                return false;
            } catch (IOException ioe) {
                Log.error("importData: I/O exception");
                return false;
            }

            if (info.isDrop()) { //This is a drop
            	JTable.DropLocation dl = (JTable.DropLocation)info.getDropLocation();
                int index = dl.getRow();
                
                if (dl.isInsertRow()) {
                	//clear selection
                	table.clearSelection();
                	for(MartPointer mp : data){
                		int fromindex = model.indexOf(mp);
                		int delindex = gc.getMartPointerList().indexOf(mp);
                		int addindex = gc.getMartPointerList().size();
                		if(index < model.size())
                			addindex = gc.getMartPointerList().indexOf(model.elementAt(index));
                		model.add(index, mp);
                		gc.getMartPointerList().add(addindex, mp);
                		
                		if(fromindex > index)
                			fromindex++;
                		model.remove(fromindex);
                		if(delindex > addindex)
                			delindex ++;
                		gc.getMartPointerList().remove(delindex);
                	}
                	//restore selection
                	for(MartPointer mp : data){
                    	int selindex = model.indexOf(mp);
                    	table.addRowSelectionInterval(selindex, selindex);
                    }
                    return true;
                } else {
                    //model.set(index, data);
                    return true;
                }
                
            } else { //This is a paste
                int index = table.getSelectedRow();
                // if there is a valid selection,
                // insert data after the selection
                if (index >= 0) {
                	for(MartPointer mp : data)
                		model.add(table.getSelectedRow()+1, mp);
                // else append to the end of the list
                } else {
                	for(MartPointer mp : data)
                		model.addElement(mp);
                }
                return true;
            }
        }else if(info.isDataFlavorSupported(DataFlavor.stringFlavor)){
        	String transferable;
			try {
				transferable = (String) info.getTransferable().getTransferData(DataFlavor.stringFlavor);
				this.dropConfigFromSource(transferable);
			} catch (UnsupportedFlavorException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
        	
        }

        return true;
    }

    private void dropConfigFromSource(String transferable) {
    	String[] marts = transferable.split(",");
    	List<Mart> martList = new ArrayList<Mart>();
    	for(String martName: marts) {
    		Mart mart = McGuiUtils.INSTANCE.getRegistryObject().getMartByName(martName);
    		martList.add(mart);
    	}
    	final UserGroup user = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
    	if(this.rootPanel.getGuiContainer().getGuiType().equals(GuiType.get("martreport"))) {
    		this.rootPanel.addReportConfigs(martList);
    	}else
    		this.rootPanel.addMultipleConfigs(martList, user);
    	//clear the selection in source
    	((McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP)).unHighlightAllComponent();
    	((McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP)).unselectOthers(null);
    }
    /**
     * Bundle up the data for export.
     */
    protected Transferable createTransferable(JComponent c) {
        JTable table = (JTable)c;
        
        for(int row : table.getSelectedRows()){
        	SharedDataModel model = (SharedDataModel)table.getModel();
        	data.add((MartPointer)model.elementAt(row));
        }
        return new TransferableConfig(data);
    }

    /**
     * The list handles both copy and move actions.
     */
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    /** 
     * When the export is complete, remove the old list entry if the
     * action was a move.
     */
    protected void exportDone(JComponent c, Transferable data, int action) {
    	JTable table = (JTable)c;
    	table.repaint();
    	try {
    		List<MartPointer> mps = (List<MartPointer>)data.getTransferData(TransferableConfig.MART_POINTER_FLAVOR);
    		mps.clear();
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * We only support importing strings.
     */
    public boolean canImport(TransferHandler.TransferSupport support) {
        // we only import Mart Pointer
        if (support.isDataFlavorSupported(TransferableConfig.MART_POINTER_FLAVOR)) {
            return true;
        }else if(support.isDataFlavorSupported(DataFlavor.stringFlavor)){
        	return true;
        }
        return false;
    }
}
