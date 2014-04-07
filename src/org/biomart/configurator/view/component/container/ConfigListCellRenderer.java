package org.biomart.configurator.view.component.container;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.objects.Config;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;

import e.gui.EListCellRenderer;

public class ConfigListCellRenderer extends EListCellRenderer {
	

	public ConfigListCellRenderer(boolean arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean hasFocus) {
		// TODO Auto-generated method stub
		JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
		MartPointer mp = (MartPointer)value;
		final UserGroup user = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
		//JLabel label = new JLabel(mp.getConfig().getDisplayName());
		label.setText(mp.getConfig().getDisplayName());
		
		StringBuffer iconPath = new StringBuffer("images/");
		if(mp.isActivatedInUser(user)) {
			iconPath.append("config");
		}
		else {
			iconPath.append("config_h");
		}
		iconPath.append(".gif");
		label.setIcon(McUtils.createImageIcon(iconPath.toString()));
		label.setVerticalTextPosition(JLabel.BOTTOM);
		label.setHorizontalTextPosition(JLabel.CENTER);
		
		this.setToolTipText(mp.getConfig().getDisplayName());
		return label;
	}

}
