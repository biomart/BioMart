package org.biomart.configurator.controller;

import java.util.ArrayList;
import java.util.Map;

import javax.swing.JDialog;

import org.biomart.common.utils.McEventBus;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.view.gui.dialogs.AddAttributeListDialog;
import org.biomart.configurator.view.gui.dialogs.AddFilterListDialog;
import org.biomart.configurator.view.gui.dialogs.AddPseudoAttributeDialog;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.processors.ProcessorRegistry;

public class AttributeTableHandler {
	private JDialog parent;
	
	public AttributeTableHandler(JDialog parent){
		this.parent = parent;
	}
	
	public boolean requestEditDatasetsInMartPointer(MartConfiguratorObject node) {
		
		return true;
	}
	
	public boolean requestEditFilterList(MartConfiguratorObject node) {
		org.biomart.objects.objects.Filter filter = (org.biomart.objects.objects.Filter)node;
		String oldvalue = filter.getFilterListString();
		Config config = node.getParentConfig();
		new AddFilterListDialog(config,filter,parent);
		//nothing changed
		if(oldvalue.equals(filter.getFilterListString())) 
			return true;
		
		if(config.isMasterConfig()) {
			McEventBus.getInstance().fire(McEventProperty.SYNC_UPDATE_LIST.toString(), node);
			McEventBus.getInstance().fire(McEventProperty.REFRESH_SOURCETABLE.toString(), null);
			McEventBus.getInstance().fire(McEventProperty.REFRESH_TARGETTABLE.toString(), null);
		} else {
			Config masterConfig = config.getMart().getMasterConfig();
			String[] filterStrs = filter.getFilterListString().split(",");
			for(String filterStr: filterStrs) {
				if(config.getFilterByName(filterStr, new ArrayList<String>()) ==null) {
					org.biomart.objects.objects.Filter filInSource = masterConfig.getFilterByName(filterStr, new ArrayList<String>());
					if(filInSource!=null) {
						org.biomart.objects.objects.Filter copy = filInSource.cloneMyself();
						filter.getParentContainer().addFilter(copy);
					}
				}
			}
			McEventBus.getInstance().fire(McEventProperty.REFRESH_TARGETTABLE.toString(), null);
		}
		return true;
	}
	
	public boolean requestEditAttributeList(MartConfiguratorObject node) {
		org.biomart.objects.objects.Attribute attribute = (org.biomart.objects.objects.Attribute)node;
		String oldvalue = attribute.getAttributeListString();
		Config config = node.getParentConfig();
		new AddAttributeListDialog(config,attribute, parent);
		//nothing changed
		if(oldvalue.equals(attribute.getAttributeListString()))
			return true;
						
		if(config.isMasterConfig()) {
			McEventBus.getInstance().fire(McEventProperty.SYNC_UPDATE_LIST.toString(), node);
			McEventBus.getInstance().fire(McEventProperty.REFRESH_SOURCETABLE.toString(), null);
			McEventBus.getInstance().fire(McEventProperty.REFRESH_TARGETTABLE.toString(), null);
		} else {
			Config masterConfig = config.getMart().getMasterConfig();
			String[] attStrs = attribute.getAttributeListString().split(",");
			for(String att: attStrs) {
				if(config.getAttributeByName(att, new ArrayList<String>())==null) {
					org.biomart.objects.objects.Attribute attInSource = masterConfig.getAttributeByName(att, new ArrayList<String>());
					if(attInSource!=null) {
						org.biomart.objects.objects.Attribute copy = attInSource.cloneMyself(false);
						attribute.getParentContainer().addAttribute(copy);
					}
				}
			}
			McEventBus.getInstance().fire(McEventProperty.REFRESH_TARGETTABLE.toString(), null);
		}

		return true;
	}
	
	public boolean requestEditPseudoAttribute(MartConfiguratorObject node) {
		Config config = node.getParentConfig();
		new AddPseudoAttributeDialog(config,(org.biomart.objects.objects.Attribute)node,parent);
		return true;
	}

	public void requestProcessor(Config config) {
		
	}
}