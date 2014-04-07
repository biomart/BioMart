package org.biomart.configurator.view.dnd;

import java.awt.Component;
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
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.configurator.view.component.container.ActionPanel;
import org.biomart.configurator.view.component.container.ExpandingPanel;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.portal.GuiContainer;


public class MartDropTargetListener implements DropTargetListener {


    
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		DropTargetContext context = dtde.getDropTargetContext();
		Component component = context.getComponent();
		component.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		DropTargetContext context = dte.getDropTargetContext();
		Component component = context.getComponent();
		component.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		DropTargetContext context = dtde.getDropTargetContext();
		Component component = context.getComponent();
		if(component instanceof ActionPanel ) {
			component.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		}
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		DropTargetContext context = dtde.getDropTargetContext();
		Component component = context.getComponent(); 
        // Done with cursors, dropping
        component.setCursor(Cursor.getDefaultCursor());    
		if(!(component instanceof ActionPanel)) {
			return;
		}
		ActionPanel ac = (ActionPanel)component;
        // Just going to grab the expected DataFlavor to make sure
        // we know what is being dropped
      
        Object transferableObj = null;
        Transferable transferable = null;
        
        try {            
            transferable = dtde.getTransferable();
            
            // What does the Transferable support
            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                transferableObj = dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
            } 
            
        } catch (Exception ex) { /* nope, not the place */ }
        
        // If didn't find an item, bail
        if (transferableObj == null) {
            return;
        }
        

        String martName = (String)transferableObj;
        //reset the mart group and then refresh the gui
        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        String[] _marts = martName.split(",");
        for(String _mart: _marts) {
	        Mart mart = registry.getMartByName(_mart);
	        mart.setGroupName(ac.getTitle());
        }

        McViewSourceGroup group =  (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
        group.showSource(registry);
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		
	}
	
}