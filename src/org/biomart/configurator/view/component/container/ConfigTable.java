package org.biomart.configurator.view.component.container;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableCellRenderer;

import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.MartPointer;

import e.gui.ETable;

public class ConfigTable extends ETable {

	private Mart mart = null;
	private boolean selfonly = false;
	private int row = -1;
	
	public void setRow(int row)
	{
		this.row = row;
	}
	public int getRow(){
		return this.row;
	}
	@Override
	public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
		// TODO Auto-generated method stub
		McViewPortal portalView = ((McViewPortal) McViews
				.getInstance().getView(IdwViewType.PORTAL));
		Component c = super.prepareRenderer(tcr, row, col);
		SharedDataModel model = (SharedDataModel)this.getModel();
		if(mart != null) {
			MartPointer mp = (MartPointer) model.elementAt(row);	
				
			boolean highlight = mp.getConfig().getMart().equals(this.mart);
			boolean curRow = (this.row < 0 || this.row == row); 
			if(highlight && curRow) {
				c.setBackground(Color.YELLOW);
				for(int i : this.getSelectedRows()){
					if(i == row)
						c.setForeground(Color.black);
				}
			}
		
			if(!selfonly){
				Map<MartPointer,Set<Mart>> configMartMap = portalView.getMPMartMap();
				for(Map.Entry<MartPointer, Set<Mart>> entry: configMartMap.entrySet()) {
					if(entry.getValue().contains(this.mart)) {
						ConfigComponent cc = portalView.getComponentByMartPointer(entry.getKey());
						if(cc.getMartPointer().equals(mp))
							c.setBackground(Color.CYAN);
					}
				}
			}
		}
		return c;
	}

	public ConfigTable(SharedDataModel model) {
		// TODO Auto-generated constructor stub
		super(model);
	}

	public void setMart(Mart mart) {
		this.mart = mart;
	}
	
	public void setSelfOnly(boolean selfonly) {
		this.selfonly = selfonly;
	}
}
