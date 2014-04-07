package org.biomart.configurator.view;

import java.awt.Component;
import javax.swing.Icon;
import org.biomart.configurator.model.McModel;
import org.biomart.configurator.utils.type.IdwViewType;
import net.infonode.docking.View;

public abstract class McView extends View {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private IdwViewType type;
	
	public McView(String title, Icon icon, Component component, McModel model, IdwViewType type) {
		super(title, icon, component);
		this.type = type;
	}

	public IdwViewType getType() {
		return this.type;
	}
		

	
	
}