package org.biomart.configurator.wizard.addsource;


import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.biomart.configurator.utils.treelist.FileCheckBoxNode;
import org.biomart.configurator.utils.treelist.LeafCheckBoxList;

public class MartMetaList extends LeafCheckBoxList implements MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private MartMetaModel model;

	public MartMetaList() {
		this.addMouseListener(this);
	}
	
	private void handleNodeSelection(FileCheckBoxNode obj)  {
		if(null == obj)
			return;
		if(obj.hasSubNodes()) {
			this.restoreTables(obj);
		} else {
			this.setItem(obj);
		}		
	}
	
	private void restoreTables(FileCheckBoxNode node) {
		this.getMetaModel().firePropertyChange(MartMetaModel.RESTORE_ITEMS, null, node.getSubNodes());
	}

	private void setItem(FileCheckBoxNode node) {
		this.getMetaModel().firePropertyChange(MartMetaModel.SET_ITEMS, null, node);
	}

	public void setMetaModel(MartMetaModel model) {
		this.model = model;
	}

	public MartMetaModel getMetaModel() {
		return model;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		this.handleNodeSelection((FileCheckBoxNode)this.getSelectedValue());
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}