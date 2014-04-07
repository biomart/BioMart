package org.biomart.configurator.view.component.container;

import javax.swing.TransferHandler;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;

import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;

public class ConfigListTransferHandler extends TransferHandler {

	List<MartPointer> data = new ArrayList<MartPointer>();
	GuiContainerPanel rootPanel = null;
	
	ConfigListTransferHandler(GuiContainerPanel gcp){
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
        	JList list = (JList)info.getComponent();
            DefaultListModel model = (DefaultListModel)list.getModel();
            GuiContainer gc = this.rootPanel.getGuiContainer();
            //Fetch the data -- bail if this fails
            try {
                data = (List<MartPointer>) info.getTransferable().getTransferData(TransferableConfig.MART_POINTER_FLAVOR);
            } catch (UnsupportedFlavorException ufe) {
                System.out.println("importData: unsupported data flavor");
                return false;
            } catch (IOException ioe) {
                System.out.println("importData: I/O exception");
                return false;
            }

            if (info.isDrop()) { //This is a drop
                JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
                int index = dl.getIndex();
                if (dl.isInsert()) {
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
                    return true;
                } else {
                    //model.set(index, data);
                    return true;
                }
            } else { //This is a paste
                int index = list.getSelectedIndex();
                // if there is a valid selection,
                // insert data after the selection
                if (index >= 0) {
                	for(MartPointer mp : data)
                		model.add(list.getSelectedIndex()+1, mp);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
        JList list = (JList)c;
        
        for(Object obj : list.getSelectedValues()){
        	data.add((MartPointer)obj);
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
    	/*if (action != MOVE) {
            return;
        }
        JList list = (JList)c;
        DefaultListModel model = (DefaultListModel)list.getModel();
        for(int index : list.getSelectedIndices()){
        	model.remove(index);
        }*/
    	//((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refreshPanel(rootPanel);
    	JList list = (JList)c;
    	list.repaint();
    	try {
    		List<MartPointer> mps = (List<MartPointer>)data.getTransferData(TransferableConfig.MART_POINTER_FLAVOR);
    		mps.clear();
		} catch (UnsupportedFlavorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
