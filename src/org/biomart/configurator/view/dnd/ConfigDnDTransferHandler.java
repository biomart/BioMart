package org.biomart.configurator.view.dnd;



import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.biomart.configurator.model.SourceTransferable;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.objects.objects.Mart;

/**
 * <p>Used by both the draggable class and the target for negotiating data.</p>
 * <p>Note that this should be set for both the draggable object and the drop target.</p>
 */
public class ConfigDnDTransferHandler extends TransferHandler implements DragSourceMotionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
/*	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		if(support.isDataFlavorSupported(DataFlavor.stringFlavor) || 
				support.isDataFlavorSupported(ConfigComponent.getDataFlavor())) {
			return true;
		}else
			return false;
	}*/

    /**
     * <p>This creates the Transferable object. In our case, ConfigComponent implements Transferable, so this requires only a type cast.</p>
     * @param c
     * @return
     */
    @Override()
    public Transferable createTransferable(JComponent c) {
        // TaskInstancePanel implements Transferable
        if (c instanceof ConfigComponent) {
            Transferable tip = (ConfigComponent) c;
            return tip;
        } 

        // Not found
        return null;
    }

    public void dragMouseMoved(DragSourceDragEvent dsde) {}

    /**
     * <p>This is queried to see whether the component can be copied, moved, both or neither. We are only concerned with copying.</p>
     * @param c
     * @return
     */
    @Override()
    public int getSourceActions(JComponent c) {
        if (c instanceof ConfigComponent) {
            return TransferHandler.MOVE;
        } 
        
        return TransferHandler.NONE;
    }
}
