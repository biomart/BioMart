package org.biomart.configurator.controller;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import org.biomart.configurator.view.gui.dialogs.MatchDatasetDialog;
import org.biomart.objects.objects.Attribute;

public class DatasetListTransferHandler extends TransferHandler {
	private int[] sourceIndex ;
	private int targetIndex = 0;
	private MatchDatasetDialog matchDatasetDialog;
	private JList sourceList;
	private JList targetList;
	
	public DatasetListTransferHandler(MatchDatasetDialog matchDatasetDialog) {
		// TODO Auto-generated constructor stub
		this.matchDatasetDialog = matchDatasetDialog;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			return false;
		}
		this.targetList = (JList)support.getComponent();
		if(this.sourceList.equals(this.targetList))
			return false;
		return true;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!support.isDrop()) {
			return false;
		}

		JList list = (JList) support.getComponent();
		DefaultListModel listModel = (DefaultListModel) list.getModel();
		JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
		this.targetIndex = dl.getIndex();

		// Get the string that is being dropped.
		/*
		Transferable t = support.getTransferable();

		try {
			String fdName = (String) t.getTransferData(DataFlavor.stringFlavor);

			listModel.add(index, fdName);
		} catch (Exception e) {
			return false;
		}
		 */
		
		return true;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY_OR_MOVE;
	}

	@Override
	public void exportDone(JComponent c, Transferable t, int action) {
		if (action == TransferHandler.MOVE) {
			//create a dataset link between dnd source and target
			for(Integer i : this.sourceIndex){
				JList source = (JList)c;
				if(source.getName().equals("source")){
					this.matchDatasetDialog.createLink(i.intValue(), this.targetIndex);
				}else if(source.getName().equals("target")){
					this.matchDatasetDialog.createLink(this.targetIndex, i.intValue());						
				}
				
			}
		}
	}

	@Override
	public Transferable createTransferable(JComponent c) {
		// TODO Auto-generated method stub
		String selValue = "";
		if (c instanceof JList) {
			JList source = (JList) c;
			Object obj = source.getSelectedValue();
			DefaultListModel model = (DefaultListModel) source.getModel();			
			this.sourceIndex = source.getSelectedIndices();
			this.sourceList = source;
		}
		
		return new StringSelection(selValue);

	}

}
