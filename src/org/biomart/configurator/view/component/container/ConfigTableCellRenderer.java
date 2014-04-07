package org.biomart.configurator.view.component.container;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;

public class ConfigTableCellRenderer extends DefaultTableCellRenderer {

	
	

	@Override
	protected void setValue(Object obj) {
		// TODO Auto-generated method stub
		super.setValue(obj);
	}
	
	
	
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object obj,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		Component c = super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);

		//this.setToolTipText(obj.toString());
		final UserGroup user = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
		
		StringBuffer iconPath = new StringBuffer("images/");
		MartPointer mp = (MartPointer)((SharedDataModel) table.getModel()).elementAt(row);
		if(mp.isActivatedInUser(user)) {
			iconPath.append("config");
		}
		else {
			iconPath.append("config_h");
		}
		iconPath.append(".gif");
		this.setIcon(McUtils.createImageIcon(iconPath.toString()));
			
		McViewSourceGroup groupView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		//groupView.refreshGui();
		
		ConfigTable ct = (ConfigTable)table;
		
		if(ct.getRow() >= 0 && ct.getRow() == row){
			//c.setBackground(Color.YELLOW);

			groupView.setHighlight(mp.getMart() , Color.YELLOW);
			//set others to 
			Set<Mart> martSet = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getMPMartMap().get(mp);
			for(Mart mart: martSet) {
				//set tree highlights
				if(mart.equals(mp.getMart()))
					continue;
				groupView.setHighlight(mart , Color.CYAN);
			}
		}
		
		return c;
	}

}
