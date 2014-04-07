/**
 * 
 */
package org.biomart.configurator.controller;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import org.biomart.configurator.model.object.FilterData;
import org.biomart.objects.objects.Attribute;

/**
 * @author lyao
 * 
 */
public class LinkListTransferHandler extends TransferHandler {

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

		JList list = (JList) support.getComponent();
		DefaultListModel listModel = (DefaultListModel) list.getModel();
		JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
		int index = dl.getIndex();

		// Get the string that is being dropped.
		Transferable t = support.getTransferable();

		try {
			String fdName = (String) t.getTransferData(DataFlavor.stringFlavor);

			listModel.add(index, fdName);
		} catch (Exception e) {
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
			JList source = (JList) c;
			DefaultListModel model = (DefaultListModel) source.getModel();
			int i = source.getSelectedIndex();
			model.remove(i);
		}
	}

	private String exportString(JComponent c) {
		JList list = (JList) c;
		String fd = (String) list.getSelectedValue();
		return fd;
	}

	@Override
	public Transferable createTransferable(JComponent c) {
		// TODO Auto-generated method stub
		String selValue = "";
		if (c instanceof JList) {
			Object obj = ((JList) c).getSelectedValue();
			if (obj instanceof Attribute)
				selValue = ((Attribute) obj).getName();
			else if (obj instanceof String)
				selValue = (String) obj;
		}

		return new StringSelection(selValue);

	}

}
