package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;

import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.portal.MartPointer;

public class PublicConfigDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JList list;

	public PublicConfigDialog() {
		init();
		this.setModal(true);
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    this.setSize(screenSize.width-100,screenSize.height - 100);
		this.setLocationRelativeTo(null);
		this.setVisible(true);	

	}
	
	private void init() {
		JPanel configPanel = new JPanel(new BorderLayout());
		DefaultListModel model = new DefaultListModel();
		list = new JList(model);
		list.setSize(400,400);
		//add martpointer
		List<MartPointer> mpList = McGuiUtils.INSTANCE.getRegistryObject().getPortal().
			getRootGuiContainer().getAllMartPointerListResursively();
		for(MartPointer mp: mpList) {
			
		}
	}
}