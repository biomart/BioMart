package org.biomart.configurator.view.dnd;

import java.awt.Cursor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.biomart.common.utils.ConfigComponentComparator;
import org.biomart.configurator.model.SourceTransferable;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.configurator.view.component.container.GuiContainerPanel;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.UserGroup;

/**
 * <p>Listens for drops and performs the updates.</p>
 * <p>The real magic behind the drop!</p>
 */
public class ConfigDropTargetListener implements DropTargetListener {

    private final GuiContainerPanel rootPanel;
    
    private static final Cursor droppableCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
            notDroppableCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    public ConfigDropTargetListener(GuiContainerPanel sheet) {
        this.rootPanel = sheet;
    }

    // Could easily find uses for these, like cursor changes, etc.
    public void dragEnter(DropTargetDragEvent dtde) {}
    public void dragOver(DropTargetDragEvent dtde) {
        if (!this.rootPanel.getCursor().equals(droppableCursor)) {
            this.rootPanel.setCursor(droppableCursor);
        }
    }
    public void dropActionChanged(DropTargetDragEvent dtde) {}
    public void dragExit(DropTargetEvent dte) {
        this.rootPanel.setCursor(notDroppableCursor);
    }

    /**
     * <p>The user drops the item. Performs the drag and drop calculations and layout.</p>
     * @param dtde
     */
    /*
     * two type of target will be drop here
     * 1. configcomponent
     * 2. mart name string
     */
    public void drop(DropTargetDropEvent dtde) {
        
        // Done with cursors, dropping
        this.rootPanel.setCursor(Cursor.getDefaultCursor());
        
        // Just going to grab the expected DataFlavor to make sure
        // we know what is being dropped
        DataFlavor dragAndDropPanelFlavor = null;
        
        Object transferableObj = null;
        Transferable transferable = null;
        
        try {
            // Grab expected flavor
            dragAndDropPanelFlavor = ConfigComponent.getDataFlavor();
            
            transferable = dtde.getTransferable();
           
            // What does the Transferable support
            if (transferable.isDataFlavorSupported(dragAndDropPanelFlavor)) {
                transferableObj = dtde.getTransferable().getTransferData(dragAndDropPanelFlavor);
            } else if(transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            	transferableObj = dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
            }
            
        } catch (Exception ex) { /* nope, not the place */ }
        
        // If didn't find an item, bail
        if (transferableObj == null) {
            return;
        }
        /*if(transferableObj instanceof ConfigComponent) {
	        // Cast it to the panel. By this point, we have verified it is 
	        // a RandomDragAndDropPanel.
	        ConfigComponent droppedPanel = (ConfigComponent)transferableObj;
	        this.dropConfigComponent(dtde.getLocation().x, dtde.getLocation().y, droppedPanel);
        } else*/ 
        if(transferableObj instanceof String) {
        	this.dropConfigFromSource((String)transferableObj);
        }
    }

    private void dropConfigComponent(int dropXLoc, int dropYLoc, ConfigComponent droppedPanel) {

        //how many rows in the panel
               
        int row = 0;
        int defaultX = 5;
        int defaultY = 5;
        int defaultHeight = 0;
        Map<Integer,Integer> xyMap = new HashMap<Integer,Integer>();
        Map<Integer,List<ConfigComponent>> xListMap = new HashMap<Integer,List<ConfigComponent>>();
        for(ConfigComponent component: this.rootPanel.getConfigComponents()) {
        	defaultHeight = component.getHeight();
        	if(component.getX() == defaultX) {
        		row++;
        		xyMap.put(component.getY(),row);
        		List<ConfigComponent> xlist = xListMap.get(row);
        		if(xlist == null) {
        			xlist = new ArrayList<ConfigComponent>();
        			xListMap.put(row,xlist);
        		}
        		xlist.add(component);
        	} else {
        		int currentRow = xyMap.get(component.getY());
        		xListMap.get(currentRow).add(component);
        	}
        }
        int selectedRow = 0;
        int maxY = row*(defaultY+defaultHeight);

        if(dropYLoc>maxY)
        	dropYLoc = maxY;
        
        for(int i=0; i<row; i++) {
        	int y1 = i*(defaultY+defaultHeight);
        	int y2 = y1+defaultY+defaultHeight;
        	if(dropYLoc>=y1 && dropYLoc<=y2) {
        		selectedRow = i+1;
        		break;
        	}
        }
        
        //make sure that the list is ordered by x;
        List<ConfigComponent> list = xListMap.get(selectedRow);
        Collections.sort(list, new ConfigComponentComparator());
        
        ConfigComponent selectedCC = null;
        boolean before = true;
        if(dropXLoc<5) {
        	selectedCC = list.get(0);
        } else {
        	before = false;
	        //get the right configcomponent
	        for(ConfigComponent cc: list) {
	        	int x1=  cc.getX()+cc.getWidth();
	        	int x2 = x1+defaultX;
	        	if(dropXLoc>=x1 && dropXLoc<=x2) {
	        		selectedCC = cc;
	        		break;
	        	}
	        }
	        if(selectedCC == null) //the last one
	        	selectedCC = list.get(list.size()-1);
        }
        
        //get the index of droppedcomponent and selectedcomponent in the guicontainer object
        GuiContainer gc = this.rootPanel.getGuiContainer();
        int sourceIndex = -1;
        int targetIndex = -1;
        for(int i=0; i<gc.getMartPointerList().size(); i++) {
        	if(gc.getMartPointerList().get(i).equals(droppedPanel.getMartPointer())) 
        		sourceIndex = i;
        	if(gc.getMartPointerList().get(i).equals(selectedCC.getMartPointer())) 
        		targetIndex = i;
        	if(sourceIndex!=-1 && targetIndex!=-1)
        		break;
        }
        if(!before)
        	targetIndex++;
        if(sourceIndex<targetIndex)
        	targetIndex--;
        //remove the old martpointer and insert it in the new place
        gc.getMartPointerList().remove(sourceIndex);
        gc.getMartPointerList().add(targetIndex, droppedPanel.getMartPointer());
        
        //refresh the gui
        ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refreshPanel(this.rootPanel);
    	
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
    	((McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP)).unselectOthers(null);
    }
} 
