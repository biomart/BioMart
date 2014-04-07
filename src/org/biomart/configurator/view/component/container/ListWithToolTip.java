package org.biomart.configurator.view.component.container;

import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.ListModel;

import org.biomart.objects.objects.MartConfiguratorObject;


public class ListWithToolTip extends JList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public String getToolTipText(MouseEvent evt) {
        // Get item index
        int index = locationToIndex(evt.getPoint());
        // Get item
        Object item = getModel().getElementAt(index);
        if(item instanceof MartConfiguratorObject)
        	return ((MartConfiguratorObject)item).getName()+" -- "+((MartConfiguratorObject)item).getDisplayName();

        return super.getToolTipText(evt);
    }
	
	public ListWithToolTip(ListModel model) {
		super(model);
	}

}