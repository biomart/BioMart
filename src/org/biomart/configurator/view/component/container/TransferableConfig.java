package org.biomart.configurator.view.component.container;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.biomart.objects.portal.MartPointer;

public class TransferableConfig implements Transferable{
	public static DataFlavor MART_POINTER_FLAVOR;
	private List<MartPointer> mplist;
	private DataFlavor[] df = {MART_POINTER_FLAVOR};

	public TransferableConfig(List<MartPointer> mps){
		try{
		MART_POINTER_FLAVOR =
			new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=org.biomart.configurator.view.component.container.TransferableConfig");
		this.mplist = mps;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		// TODO Auto-generated method stub
		if (flavor == MART_POINTER_FLAVOR) {
			return mplist;
		}
		else {
			throw new UnsupportedFlavorException(flavor);	
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		// TODO Auto-generated method stub		 
		return df;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		// TODO Auto-generated method stub
		return Arrays.asList(df).contains(flavor);
	}

}
