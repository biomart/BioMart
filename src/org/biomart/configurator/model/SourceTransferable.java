package org.biomart.configurator.model;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class SourceTransferable implements Transferable {

	private String transferedValue;
	
	public SourceTransferable(String value) {
		this.transferedValue = value;
	}
	
	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if(flavor.equals(DataFlavor.stringFlavor)) {
			return this.transferedValue;
		}
		return null;	
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] result = {DataFlavor.stringFlavor};
		return result;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(DataFlavor.stringFlavor);
	}
	
}