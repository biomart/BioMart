package org.biomart.configurator.view.gui.dialogs;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelListener;

import org.biomart.configurator.model.XMLAttributeTableModel;
import org.biomart.configurator.view.AttributeTable;
import org.biomart.objects.objects.MartConfiguratorObject;

public class PropertyDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MartConfiguratorObject mcObj;
	private TableModelListener listener;
	
	public PropertyDialog(JDialog parent, MartConfiguratorObject mcObj, TableModelListener l) {
		super(parent);
		this.mcObj = mcObj;
		this.listener = l;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
		this.setTitle("properties");
	}
	
	private void init() {
		AttributeTable at = new AttributeTable(null,this);
		XMLAttributeTableModel tModel = new XMLAttributeTableModel(mcObj,null,true);
		tModel.addTableModelListener(listener);
		at.setModel(tModel);
		at.setContextMenuEnv(mcObj);
		
		JScrollPane sp = new JScrollPane(at);
		this.add(sp);
	}
	
}