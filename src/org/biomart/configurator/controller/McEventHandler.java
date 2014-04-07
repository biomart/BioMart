package org.biomart.configurator.controller;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.TreePath;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Filter;

public class McEventHandler {
	public void syncAttributeListNew(Attribute attribute, Config sourceConfig) {
		for(Config config: sourceConfig.getMart().getConfigList()) {
			if(config == sourceConfig) 
				continue;
			if(!config.isMasterConfig())
				continue;
			//add it to the same container, if cannot find the container, create a newattribute container
			String containername = attribute.getParentContainer().getName();
			Container container = config.getContainerByName(containername);
			if(container == null) {
				container = config.getContainerByName(Resources.get("NEWATTRIBUTE"));
				if(container == null) {
					container = new Container(Resources.get("NEWATTRIBUTE"));
					config.getRootContainer().addContainer(container);
				}
			}
			List<Attribute> attributeList = attribute.getAttributeList();
			for(Attribute a: attributeList) {
				if(config.getAttributeByName(a.getName(), new ArrayList<String>()) == null) {
					Attribute newA = a.cloneMyself(false);
					container.addAttribute(newA);
					if(a.isPointer() && a.getPointedAttribute()!=null) {
						//check if the pointed attribute exist
						Attribute pa = a.getPointedAttribute();
						if(config.getAttributeByName(pa.getName(), new ArrayList<String>()) == null) {
							Attribute newPa = pa.cloneMyself(false);
							container.addAttribute(newPa);
						}
					}
				}
			}
			if(config.getAttributeByName(attribute.getName(), new ArrayList<String>()) == null) {
				Attribute newA = attribute.cloneMyself(false);
				container.addAttribute(newA);
			}
		}
	}
	
	public void syncFilterListNew(Filter filter, Config sourceConfig) {
		for(Config config: sourceConfig.getMart().getConfigList()) {
			if(config == sourceConfig) 
				continue;
			if(!config.isMasterConfig())
				continue;
			//add it to the same container, if cannot find the container, create a newattribute container
			String containername = filter.getParentContainer().getName();
			Container container = config.getContainerByName(containername);
			if(container == null) {
				container = config.getContainerByName(Resources.get("NEWATTRIBUTE"));
				if(container == null) {
					container = new Container(Resources.get("NEWATTRIBUTE"));
					config.getRootContainer().addContainer(container);
				}
			}
			List<Filter> filterList = filter.getFilterList();
			for(Filter f: filterList) {
				if(config.getFilterByName(f.getName(), new ArrayList<String>()) == null) {
					Filter newA = f.cloneMyself();
					container.addFilter(newA);
				}
				//pointer?
				if(f.isPointer() && f.getPointedFilter()!=null) {
					Filter pf = f.getPointedFilter();
					if(config.getFilterByName(pf.getName(), new ArrayList<String>()) == null) {
						Filter newPf = pf.cloneMyself();
						container.addFilter(newPf);
					}
				}
				//check if attribute is there
				Attribute a = f.getAttribute();
				if(a!=null && config.getAttributeByName(a.getName(), new ArrayList<String>()) == null) {
					Attribute newA = a.cloneMyself(false);
					container.addAttribute(newA);
				}
			}
			if(config.getFilterByName(filter.getName(), new ArrayList<String>()) == null) {
				Filter newA = filter.cloneMyself();
				container.addFilter(newA);				
			}
		}				
	}

	public void syncPseudoAttribute(Attribute attribute, Config sourceConfig) {
		for(Config config: sourceConfig.getMart().getConfigList()) {
			if(config == sourceConfig) 
				continue;
			if(!config.isMasterConfig())
				continue;
			//add it to the same container, if cannot find the container, create a newattribute container
			String containername = attribute.getParentContainer().getName();
			Container container = config.getContainerByName(containername);
			if(container == null) {
				container = config.getContainerByName(Resources.get("NEWATTRIBUTE"));
				if(container == null) {
					container = new Container(Resources.get("NEWATTRIBUTE"));
					config.getRootContainer().addContainer(container);
				}
			}
			if(config.getAttributeByName(attribute.getName(),new ArrayList<String>()) == null) {
				Attribute newA = attribute.cloneMyself(false);
				container.addAttribute(newA);
			}
		}		
	}

	public void syncAttributeListUpdate(Attribute attribute, Config sourceConfig) {
		for(Config config: sourceConfig.getMart().getConfigList()) {
			if(config == sourceConfig) 
				continue;
			//add it to the same container, if cannot find the container, create a newattribute container
			Attribute att = config.getAttributeByName(attribute.getName(), new ArrayList<String>());
			if(att == null)
				continue;
			att.updateAttributeList(attribute.getAttributeListString());
		}		
	}
	
	public void syncFilterListUpdate(Filter filter, Config sourceConfig) {
		for(Config config: sourceConfig.getMart().getConfigList()) {
			if(config == sourceConfig) 
				continue;
			//add it to the same container, if cannot find the container, create a newattribute container
			Filter fil = config.getFilterByName(filter.getName(), new ArrayList<String>());
			if(fil == null)
				continue;
			fil.updateFilterList(filter.getFilterListString());
		}		
	}
	


	
	
	public void refreshTree(MartConfigTree tree) {
		TreePath oldPath = tree.getSelectionPath();
		McTreeNode rootNode = (McTreeNode)tree.getModel().getRoot();
		rootNode.synchronizeNode();
		tree.getModel().reload();
		if(oldPath!=null) 
			tree.expandPath(oldPath);		
	}
	
	public void refreshAttributeTable(MartConfigTree tree) {
		TreePath path = tree.getSelectionPath();
		if(path == null)
			return;
		McTreeNode selectedNode = (McTreeNode)path.getLastPathComponent();
		tree.updateAttributeTable(selectedNode);		
	}
	

}