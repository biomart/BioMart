package org.biomart.configurator.controller;

import java.awt.*;
import javax.swing.*;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.tree.*;

import java.awt.dnd.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.component.SourceConfigPanel;
import org.biomart.configurator.view.component.TargetConfigPanel;
import org.biomart.configurator.view.gui.dialogs.DatasetSelectionDialog;
import org.biomart.configurator.view.gui.dialogs.FilterDropDownDialog;
import org.biomart.configurator.view.gui.dialogs.LinkDatasetDialog;
import org.biomart.configurator.jdomUtils.McTreeModel;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.objects.objects.Attribute;

import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Config;
import org.jdom.Element;
 
public class DefaultTreeTransferHandler extends AbstractTreeTransferHandler {
	
	
	public DefaultTreeTransferHandler(MartConfigTree tree, int action) {
		super(tree, action);
	}
 
	/**
	 * rule: attribute - attribute
	 */
	public boolean canPerformAction(MartConfigTree target, List<McTreeNode> draggedNodes, int action, Point location) {
		TreePath pathTarget = target.getPathForLocation(location.x, location.y);
		if (pathTarget == null) {
			target.setSelectionPath(null);
			return false;
		}
		target.setSelectionPath(pathTarget);
		if(action == DnDConstants.ACTION_COPY) {
			return false;
		} else if(action == DnDConstants.ACTION_MOVE) {	
			return this.checkCanMove((McTreeNode)pathTarget.getLastPathComponent(), draggedNodes);
		}
		else {		
			return false;	
		}
	}
 
	/**
	 * doesn't support copy currently
	 */
	public boolean executeDrop(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode, int action) { 
		if (action == DnDConstants.ACTION_COPY) {
			return(false);
		}
		if(!this.checkBeforeExcute(newParentNode, draggedNodes))
			return false;
		if (action == DnDConstants.ACTION_MOVE) {
			//check if the move is in the same tree, if not create a pointer
			MartConfiguratorObject tconfig = (MartConfiguratorObject)((McTreeNode)newParentNode.getRoot())
					.getUserObject();
			MartConfiguratorObject sconfig = (MartConfiguratorObject)((McTreeNode)draggedNodes.get(0).getRoot())
					.getUserObject();
			boolean sameConfig = sconfig.equals(tconfig);
			//boolean sameMart = sameConfig?true:sconfig.getParent().equals(tconfig.getParent());
			
			MartConfiguratorObject targetObject = newParentNode.getObject();
			if(targetObject instanceof org.biomart.objects.objects.Container) {
				return this.moveToContainer(target, draggedNodes, newParentNode, sconfig, tconfig);
			} else if(targetObject instanceof Attribute) {
				/*if(sameConfig)
					return this.moveToAttributeList(target, draggedNodes, newParentNode);
				else*/
					return false;
			} else if(targetObject instanceof Filter) {
				/*if(sameConfig)
					return this.moveToFilterList(target, draggedNodes, newParentNode);
				else*/
					return false;
			}
		}
		//validate after drag and drop
		target.validate();
		return(false);
	}	
	//method for tree drag and drop insert
	public boolean executeInsert(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode, int action, int childIndex) { 
		if (action == DnDConstants.ACTION_COPY) {
			return(false);
		}
		if(!this.checkBeforeExcute(newParentNode, draggedNodes))
			return false;
		if (action == DnDConstants.ACTION_MOVE) {
			//check if the move is in the same tree, if not create a pointer
			MartConfiguratorObject sconfig = (MartConfiguratorObject)((McTreeNode)newParentNode.getRoot())
					.getUserObject();
			MartConfiguratorObject tconfig = (MartConfiguratorObject)((McTreeNode)draggedNodes.get(0).getRoot())
					.getUserObject();
			boolean sameConfig = sconfig.equals(tconfig);
			boolean sameMart = sameConfig?true:sconfig.getParent().equals(tconfig.getParent());
			
			McTreeNode tempNode = (McTreeNode)newParentNode.getChildAt(childIndex);
			MartConfiguratorObject targetObject = tempNode.getObject();
			if(targetObject instanceof org.biomart.objects.objects.Container) {
				return this.insertToContainer(target, draggedNodes, newParentNode, sameMart, sameConfig, childIndex);
			} else if(targetObject instanceof Attribute) {
				if(sameConfig)
					return this.insertToAttributeList(target, draggedNodes, newParentNode, childIndex);
				else
					return this.insertToContainer(target, draggedNodes, newParentNode, sameMart, sameConfig, childIndex);
			} else if(targetObject instanceof Filter) {
				if(sameConfig)
					return this.insertToFilterList(target, draggedNodes, newParentNode, childIndex);
				else
					return this.insertToContainer(target, draggedNodes, newParentNode, sameMart, sameConfig, childIndex);
			}
		}
		//validate after drag and drop
		target.validate();
		return(false);
	}	
	//check if the source container is the parent of the target container
	//including itself
	private boolean isParentContainer(Container source, Container target) {
		if(target == null || target.getParentContainer() == null)
			return false;
		if(target == source)
			return true;
		if(target.getParentContainer().equals(source))
			return true;		
		else
			return isParentContainer(source,target.getParentContainer());
	}
	private boolean insertToContainer(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode, 
			boolean sameMart, boolean sameConfig,int childIndex) {
		List<McTreeNode> nodeList = new ArrayList<McTreeNode>();
		ObjectController oc = new ObjectController();
		boolean is1Descendantof2 = false;
		for(McTreeNode tmpNode1: draggedNodes) {
			for(McTreeNode tmpNode2: draggedNodes) {
				if(tmpNode1 == tmpNode2)
					break;
				if(tmpNode2.isNodeDescendant(tmpNode1)) {
					is1Descendantof2 = true;
					break;
				}					
			}
			if(!is1Descendantof2)  {
				if(tmpNode1.getUserObject() instanceof Attribute || tmpNode1.getUserObject() instanceof Filter ||
						tmpNode1.getUserObject() instanceof Container)
					nodeList.add(tmpNode1);
				else {					
					return false;
				}
			}
		}
		if(nodeList.isEmpty())
			return false;
		
		if(sameConfig) {
			//move
			for(McTreeNode node: nodeList) {
				McTreeNode oldParent = (McTreeNode)node.getParent();
				
				//readjust child index based on source location
				int oldIndex = oldParent.getIndex(node);
				if(oldIndex < childIndex)
					childIndex --;
				
		    	if(node.getObject() instanceof Attribute) {
		    		((Container)oldParent.getObject()).removeAttribute((Attribute)node.getObject());
		    		((Container)newParentNode.getObject()).addAttribute(childIndex++, (Attribute)node.getObject());
		    	}
		    	else if(node.getObject() instanceof Filter){
		    		((Container)oldParent.getObject()).removeFilter((Filter)node.getObject());
		    		((Container)newParentNode.getObject()).addFilter(childIndex++, (Filter)node.getObject());
		    	} else if(node.getObject() instanceof Container) {
		    		Container sourceObj = (Container)node.getObject();
		    		Container targetObj = (Container)newParentNode.getObject();
		    		//check if source container is parent of target container or source == target
		    		if(this.isParentContainer(sourceObj,targetObj) || sourceObj.equals(targetObj))
		    			return false;
		    		if(sourceObj.getParent() instanceof Container) {
			    		Container sourceParentC = (Container)sourceObj.getParent();
			    		sourceParentC.removeContainer(sourceObj);
			    		targetObj.addContainer(childIndex++, sourceObj);
		    		}
		    	}
				node.removeFromParent();
				((McTreeModel)target.getModel()).nodeStructureChanged(oldParent);
			}
		} else if(sameMart) {
			
			//create a copy from master to others
			for(McTreeNode node: nodeList) {
				if(node.getUserObject() instanceof Attribute) {
					try {
						//check if it already exist
						Config targetConfig = ((Container)newParentNode.getObject()).getParentConfig();
						boolean rename = false;
						if(targetConfig.getAttributeByName(((MartConfiguratorObject)node.getUserObject()).getName()
								,new ArrayList<String>())!=null) {
							int n = JOptionPane.showConfirmDialog(
								    null,
								    "Attribute already exist, can not create more than one attribute",
								    "Warning",
								    JOptionPane.CLOSED_OPTION);
							/*if(n==0) {
								rename = true;
							}else*/
								return false;

						}
						Element attElement = ((MartConfiguratorObject)node.getUserObject()).generateXml();
						Attribute newAtt = new Attribute(attElement);
						//set new att to have the target config
						newAtt.setConfig(targetConfig);
						oc.generateAttributeRDF(newAtt, targetConfig);
						/*if(rename) {
							String newName = McUtils.getUniqueAttributeName(targetConfig, newAtt.getName());
							newAtt.setName(newName);
							newAtt.setInternalName(newName);							
						}*/
						((Container)newParentNode.getObject()).addAttribute(childIndex,newAtt);
						newAtt.synchronizedFromXML();
					} catch (FunctionalException e) {
						e.printStackTrace();
					}
				}else if(node.getUserObject() instanceof Filter) {
					Filter oldFilter = (Filter)node.getUserObject();
					Element filterElement;
					Config targetConfig = ((Container)newParentNode.getObject()).getParentConfig();
					boolean rename = false;
					if(targetConfig.getFilterByName(((MartConfiguratorObject)node.getUserObject()).getName()
							,new ArrayList<String>())!=null) {
						int n = JOptionPane.showConfirmDialog(
							    null,
							    "Filter already exist, can not create more than one filter",
							    "Warning",
							    JOptionPane.CLOSED_OPTION);
						/*if(n==0) {
							rename = true;
						}else*/
							return false;

					}

					filterElement = oldFilter.generateXml();
					Filter newFil = new Filter(filterElement);
					newFil.setConfig(targetConfig);
					oc.generateFilterRDF(newFil, targetConfig);
					/*
					if(rename) {
						String newName = McGuiUtils.INSTANCE.getUniqueFilterName(targetConfig, newFil.getName());
						newFil.setName(newName);
						newFil.setInternalName(newName);							
					}*/
					Container con = (Container)newParentNode.getObject();
					con.addFilter(childIndex, newFil);
					newFil.synchronizedFromXML();
					
					//also copy attributes filter pointed to the target if not existed
					
					if(newFil.isFilterList()){
						for(Filter fil : oldFilter.getFilterList()){
							//old filter list should not have a filter list in it
							if(fil.isFilterList())
								continue;
							if(!targetConfig.containFilterByName(fil)){
								filterElement = fil.generateXml();
								newFil = new Filter(filterElement);
								newFil.setConfig(targetConfig);
								oc.generateFilterRDF(newFil, targetConfig);
								((Container)newParentNode.getObject()).addFilter(childIndex,newFil);
								newFil.synchronizedFromXML();
							}
							
							Attribute attr = fil.getAttribute();
							if(!targetConfig.containAttributebyName(attr)){
								Element attElement = attr.generateXml();
								Attribute newAtt = new Attribute(attElement);
								//set new att to have the target config
								newAtt.setConfig(targetConfig);
								oc.generateFilterRDF(newFil, targetConfig);
								((Container)newParentNode.getObject()).addAttribute(childIndex,newAtt);
								newAtt.synchronizedFromXML();
							}
						}
					}else{
						Attribute attr = oldFilter.getAttribute();
						if(!targetConfig.containAttributebyName(attr)){
							Element attElement = attr.generateXml();
							Attribute newAtt = new Attribute(attElement);
							//set new att to have the target config
							newAtt.setConfig(targetConfig);
							oc.generateAttributeRDF(newAtt, targetConfig);
							((Container)newParentNode.getObject()).addAttribute(childIndex, newAtt);
							newAtt.synchronizedFromXML();
						}
					}
				}else if(node.getObject() instanceof Container) {
					try {
						Config targetConfig = ((Container)newParentNode.getObject()).getParentConfig();
						boolean rename = false;
						if(targetConfig.getContainerByName(((MartConfiguratorObject)node.getUserObject()).getName())!=null) {
							int n = JOptionPane.showConfirmDialog(
								    null,
								    "Container already exist, create a new container?",
								    "Question",
								    JOptionPane.YES_NO_OPTION);
							if(n==0) {
								rename = true;
							}else
								return false;
						}

						Element containerElement = ((MartConfiguratorObject)node.getUserObject()).generateXml();
						Container newCon = new Container(containerElement);
						//recursively set all child nodes to the same config
						List<Attribute> attrList = newCon.getAllAttributes(null, true, true);
						for(Attribute attr : attrList) {
							attr.setConfig(targetConfig);
							oc.generateAttributeRDF(attr, targetConfig);
						}
						List<Filter> filterList = newCon.getAllFilters(null, true, true);
						for(Filter filter : filterList) {
							filter.setConfig(targetConfig);
							oc.generateFilterRDF(filter, targetConfig);
						}
						
						if(rename) {
							//TODO
							
						}
						((Container)newParentNode.getObject()).addContainer(childIndex,newCon);
						newCon.synchronizedFromXML();
					} catch (FunctionalException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else {
			//create a pointer
			//need to know which dataset is selected in both source and target
			Dataset leftDs = this.getDataset(target, true);
			Dataset rightDs = this.getDataset(target, false);
    		//create attributepointer
    		String baseName = draggedNodes.get(0).getObject().getName()+"_"+leftDs.getName()+"_"+rightDs.getName();
    		Config config = (Config)((McTreeNode)newParentNode.getRoot()).getObject();
	    	if(draggedNodes.get(0).getObject() instanceof Attribute) {
	       		//get unique name
	    		baseName = McUtils.getUniqueAttributeName(config, baseName);
	    		Attribute attPointer = new Attribute(baseName,(Attribute)draggedNodes.get(0).getObject());
	    		//use the old displayname
	    		attPointer.setDisplayName(draggedNodes.get(0).getObject().getDisplayName());
	    		((Container)newParentNode.getObject()).addAttribute(childIndex, attPointer);
	    		//set pointed dataset
	    		attPointer.addPointedDataset(leftDs.getName());
	    		//attPointer.setPointedDatasetName(leftDs.getName());
	    	}
	    	else if(draggedNodes.get(0).getObject() instanceof Filter){
	       		//get unique name
	    		Filter filter = (Filter)draggedNodes.get(0).getObject();
	    		baseName = McGuiUtils.INSTANCE.getUniqueFilterName(config, baseName);
	    		Filter filPointer = new Filter(baseName,draggedNodes.get(0).getObject().getName(),leftDs.getName());
	    		//filPointer.setPointedInfo(draggedNodes.get(0).getObject().getName(), 
	    			//	leftDs.getName(), config.getName(), config.getMart().getName());
	    		filPointer.setDisplayName(filter.getDisplayName());
	    		filPointer.setPointedElement(filter);
	    		filPointer.setFilterType(filter.getFilterType());
	    		((Container)newParentNode.getObject()).addFilter(childIndex, filPointer);
	    	}
		}

    	//synchronize new parent node
    	newParentNode.synchronizeNode();
    	((McTreeModel)target.getModel()).nodeStructureChanged(newParentNode);	    	
		//((McTreeModel)target.getModel()).insertNodeInto(draggedNode,newParentNode,newParentNode.getChildCount());
		TreePath treePath = new TreePath(newParentNode.getPath());
		target.expandPath(treePath);
		target.scrollPathToVisible(treePath);
		target.setSelectionPath(treePath);
		return(true);
	}
	private boolean moveToContainer(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode, 
			MartConfiguratorObject sconfig, MartConfiguratorObject tconfig) {
    	//need to remove all descendant path first
		List<McTreeNode> nodeList = new ArrayList<McTreeNode>();
		boolean is1Descendantof2 = false;
		boolean sameConfig = sconfig.equals(tconfig);
		boolean sameMart = sameConfig?true:sconfig.getParent().equals(tconfig.getParent());
		ObjectController oc = new ObjectController();
		
		for(McTreeNode tmpNode1: draggedNodes) {
			for(McTreeNode tmpNode2: draggedNodes) {
				if(tmpNode1 == tmpNode2)
					break;
				if(tmpNode2.isNodeDescendant(tmpNode1)) {
					is1Descendantof2 = true;
					break;
				}					
			}
			if(!is1Descendantof2)  {
				if(tmpNode1.getUserObject() instanceof Attribute || tmpNode1.getUserObject() instanceof Filter ||
						tmpNode1.getUserObject() instanceof Container)
					nodeList.add(tmpNode1);
				else {
					//JOptionPane.showMessageDialog(null, "cannot move");
					return false;
				}
			}
		}
		if(nodeList.isEmpty())
			return false;
		
		if(sameConfig) {
			//move
			for(McTreeNode node: nodeList) {
				McTreeNode oldParent = (McTreeNode)node.getParent();		
		    	if(node.getObject() instanceof Attribute) {
		    		((Container)oldParent.getObject()).removeAttribute((Attribute)node.getObject());
		    		((Container)newParentNode.getObject()).addAttribute((Attribute)node.getObject());
		    	}
		    	else if(node.getObject() instanceof Filter){
		    		((Container)oldParent.getObject()).removeFilter((Filter)node.getObject());
		    		((Container)newParentNode.getObject()).addFilter((Filter)node.getObject());
		    	} else if(node.getObject() instanceof Container) {
		    		Container sourceObj = (Container)node.getObject();
		    		Container targetObj = (Container)newParentNode.getObject();
		    		//check if source container is parent of target container or source == target
		    		if(this.isParentContainer(sourceObj,targetObj) || sourceObj.equals(targetObj))
		    			return false;
		    		if(sourceObj.getParent() instanceof Container) {
			    		Container sourceParentC = (Container)sourceObj.getParent();
			    		sourceParentC.removeContainer(sourceObj);
			    		targetObj.addContainer(sourceObj);
		    		}
		    	}
				node.removeFromParent();
				((McTreeModel)target.getModel()).nodeStructureChanged(oldParent);
			}
		} else if(sameMart) {
			Config sourceConfig = (Config)sconfig;
			//create a copy from master to others
			for(McTreeNode node: nodeList) {
				if(node.getUserObject() instanceof Attribute) {
					try {
						//check if it already exist
						Config targetConfig = ((Container)newParentNode.getObject()).getParentConfig();
						boolean rename = false;
						if(targetConfig.getAttributeByName(((MartConfiguratorObject)node.getUserObject()).getName()
								,new ArrayList<String>())!=null) {
							int n = JOptionPane.showConfirmDialog(
								    null,
								    "Attribute already exist, can not create more than one attribute",
								    "Warning",
								    JOptionPane.CLOSED_OPTION);
							/*if(n==0) {
								rename = true;
							}else*/
								return false;

						}
						Element attElement = ((MartConfiguratorObject)node.getUserObject()).generateXml();
						Attribute newAtt = new Attribute(attElement);
						//set new att to have the target config
						newAtt.setConfig(targetConfig);
						oc.generateAttributeRDF(newAtt, targetConfig);
						/*if(rename) {
							String newName = McUtils.getUniqueAttributeName(targetConfig, newAtt.getName());
							newAtt.setName(newName);
							newAtt.setInternalName(newName);							
						}*/
						((Container)newParentNode.getObject()).addAttribute(newAtt);
						newAtt.synchronizedFromXML();
						// copy linkouturl reference attributes as well 
						if(!newAtt.getLinkOutUrl().isEmpty()){
							String linkOutUrl = newAtt.getLinkOutUrl();
							List<Attribute> attributes = McGuiUtils.INSTANCE.getAttributesFromLinkOutUrl(linkOutUrl, sourceConfig);
							for(Attribute att: attributes) {
								if(targetConfig.containAttributebyName(att))
									continue;
								attElement = att.generateXml();
								newAtt = new Attribute(attElement);
								//set new att to have the target config
								newAtt.setConfig(targetConfig);
								oc.generateAttributeRDF(newAtt, targetConfig);
								
								((Container)newParentNode.getObject()).addAttribute(newAtt);
								newAtt.synchronizedFromXML();
							}
						}
					} catch (FunctionalException e) {
						e.printStackTrace();
					}
				}else if(node.getUserObject() instanceof Filter) {
					Filter oldFilter = (Filter)node.getUserObject();
					Element filterElement;
					Config targetConfig = ((Container)newParentNode.getObject()).getParentConfig();
					boolean rename = false;
					if(targetConfig.getFilterByName(((MartConfiguratorObject)node.getUserObject()).getName()
							,new ArrayList<String>())!=null) {
						int n = JOptionPane.showConfirmDialog(
							    null,
							    "Filter already exist, can not create more than one filter",
							    "Warning",
							    JOptionPane.CLOSED_OPTION);
						/*if(n==0) {
							rename = true;
						}else*/
							return false;

					}

					filterElement = oldFilter.generateXml();
					Filter newFil = new Filter(filterElement);
					newFil.setConfig(targetConfig);
					oc.generateFilterRDF(newFil, targetConfig);
					/*
					if(rename) {
						String newName = McGuiUtils.INSTANCE.getUniqueFilterName(targetConfig, newFil.getName());
						newFil.setName(newName);
						newFil.setInternalName(newName);							
					}*/
					((Container)newParentNode.getObject()).addFilter(newFil);
					newFil.synchronizedFromXML();
					
					//also copy attributes filter pointed to the target if not existed
					
					if(newFil.isFilterList()){
						for(Filter fil : oldFilter.getFilterList()){
							//old filter list should not have a filter list in it
							if(fil.isFilterList())
								continue;
							if(!targetConfig.containFilterByName(fil)){
								filterElement = fil.generateXml();
								newFil = new Filter(filterElement);
								newFil.setConfig(targetConfig);
								oc.generateFilterRDF(newFil, targetConfig);
								((Container)newParentNode.getObject()).addFilter(newFil);
								newFil.synchronizedFromXML();
							}
							
							Attribute attr = fil.getAttribute();
							if(!targetConfig.containAttributebyName(attr)){
								Element attElement = attr.generateXml();
								Attribute newAtt = new Attribute(attElement);
								//set new att to have the target config
								newAtt.setConfig(targetConfig);
								oc.generateAttributeRDF(newAtt, targetConfig);
								((Container)newParentNode.getObject()).addAttribute(newAtt);
								newAtt.synchronizedFromXML();
							}
						}
					}else{
						Attribute attr = oldFilter.getAttribute();
						if(!targetConfig.containAttributebyName(attr)){
							Element attElement = attr.generateXml();
							Attribute newAtt = new Attribute(attElement);
							//set new att to have the target config
							newAtt.setConfig(targetConfig);
							oc.generateAttributeRDF(newAtt, targetConfig);
							((Container)newParentNode.getObject()).addAttribute(newAtt);
							newAtt.synchronizedFromXML();
						}
					}
				}else if(node.getObject() instanceof Container) {
					try {
						Config targetConfig = ((Container)newParentNode.getObject()).getParentConfig();
						boolean rename = false;
						if(targetConfig.getContainerByName(((MartConfiguratorObject)node.getUserObject()).getName())!=null) {
							int n = JOptionPane.showConfirmDialog(
								    null,
								    "Container already exist, create a new container?",
								    "Question",
								    JOptionPane.YES_NO_OPTION);
							if(n==0) {
								rename = true;
							}else
								return false;
						}

						Element containerElement = ((MartConfiguratorObject)node.getUserObject()).generateXml();
						Container newCon = new Container(containerElement);
						//recursively set all child nodes to the same config
						List<Attribute> attrList = newCon.getAllAttributes(null, true, true);
						for(Attribute attr : attrList) {
							attr.setConfig(targetConfig);
							oc.generateAttributeRDF(attr, targetConfig);
						}
						List<Filter> filterList = newCon.getAllFilters(null, true, true);
						for(Filter filter : filterList) {
							filter.setConfig(targetConfig);
							oc.generateFilterRDF(filter, targetConfig);
						}
						
						if(rename) {
							//TODO
							
						}
						((Container)newParentNode.getObject()).addContainer(newCon);
						newCon.synchronizedFromXML();
					} catch (FunctionalException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else {
			//check if the link is already exist, if not exist, then pop up the link dialog
			LinkDatasetDialog ldd = null;
			if(!McUtils.hasLink((Config)sconfig, (Config)tconfig)) {				
				ldd =new LinkDatasetDialog(this.tree.getParentDialog(), sconfig, tconfig, this.getTree(), target,nodeList,newParentNode);				
			}else
			{
				//create a copy the source to the target, replace attribute with pointers
				Config sourceConfig = (Config)sconfig;
				Config targetConfig = (Config)tconfig;
				String baseName = McUtils.getLinkName(targetConfig, sourceConfig);//draggedNodes.get(0).getObject().getName()+"_"+sconfig.getName()+"_"+tconfig.getName();
				//Link link = targetConfig.getMart().getMasterConfig().getLinkByName(baseName);
	    		Link link = McUtils.getLink(targetConfig, sourceConfig);
				
				String pointedDataset = "";
				if(link == null){
					JOptionPane.showMessageDialog(null, "Link is not found!");
					return false;
				}
				if(link.getPointedDataset().contains(",")){
					//if there is multiple dataset, then open the dataset selection dialog
					
					String[] datasets = link.getPointedDataset().split(",");
					
					DatasetSelectionDialog dsd = new DatasetSelectionDialog(this.tree.getParentDialog(), datasets);
					pointedDataset = dsd.getSelectedDataset();
				}else{
					pointedDataset = link.getPointedDataset();
				}
				
				for(McTreeNode node: nodeList) {
					if(node.getUserObject() instanceof Attribute) {
						Attribute attr = (Attribute)node.getUserObject();
						/*
						if(targetConfig.getMart().getMasterConfig().hasPointedAttribute(attr))
							continue;
							*/
						if(targetConfig.hasPointedAttribute(attr))
							continue;
						baseName = McUtils.getUniqueAttributeName((Config)tconfig, attr.getName());
						//baseName = McGuiUtils.INSTANCE.getPointedAttributeName(attr);
			    		Attribute attPointer = new Attribute(baseName,node.getObject().getName(), pointedDataset);
			    		//use the old displayname
			    		attPointer.setDisplayName(node.getObject().getDisplayName());
			    		attPointer.setPointedElement((Attribute)node.getObject());
			    		((Container)newParentNode.getObject()).addAttribute(attPointer);
			    		attPointer.setPointedConfigName(sourceConfig.getName());
			    		attPointer.setPointedMartName(sourceConfig.getMart().getName());
			    		attPointer.setLinkOutUrl(attr.getLinkOutUrl());
			    		
			    		//set pointed dataset
			    		if(link != null)
			    			attPointer.addPointedDataset(pointedDataset);
			    			//attPointer.setPointedDatasetName(link.getPointedDataset());
			    		
			    		//attPointer.synchronizedFromXML();
			    		// copy linkouturl reference attributes as well 
						if(!attr.getLinkOutUrl().isEmpty()){
							String linkOutUrl = attr.getLinkOutUrl();
							List<Attribute> attributes = McGuiUtils.INSTANCE.getAttributesFromLinkOutUrl(linkOutUrl, sourceConfig);
							for(Attribute att: attributes) {
								if(targetConfig.containAttributebyName(att))
									continue;
								baseName = McUtils.getUniqueAttributeName((Config)tconfig, att.getName());
								Attribute newAttPointer = new Attribute(baseName , att);
								//set new att to have the target config
								//newAttPointer.setConfig(targetConfig);
								newAttPointer.setDisplayName(att.getDisplayName());
					    		newAttPointer.setPointedConfigName(sourceConfig.getName());
					    		newAttPointer.setLinkOutUrl(attr.getLinkOutUrl());
								//oc.generateAttributeRDF(newAttPointer, targetConfig);
					    		if(link != null)
					    			newAttPointer.addPointedDataset(pointedDataset);
								((Container)newParentNode.getObject()).addAttribute(newAttPointer);
								newAttPointer.synchronizedFromXML();
							}
						}
			    		
					}else if(node.getUserObject() instanceof Filter) {
						Filter filter = (Filter)node.getUserObject();
						/*
						if(targetConfig.getMart().getMasterConfig().hasPointedFilter(filter))
							continue;*/
						if(targetConfig.hasPointedFilter(filter))
							continue;
						//get unique name
						baseName = McGuiUtils.INSTANCE.getUniqueFilterName((Config)tconfig, filter.getName());
			    		//baseName = McGuiUtils.INSTANCE.getPointedFilterName(filter);
						
			    		Filter filPointer = new Filter(baseName,node.getObject().getName(),pointedDataset);
			    		//filPointer.setPointedInfo(draggedNodes.get(0).getObject().getName(), 
			    			//	leftDs.getName(), config.getName(), config.getMart().getName());
			    		filPointer.setDisplayName(node.getObject().getDisplayName());
			    		filPointer.setPointedElement((Filter)node.getObject());
			    		filPointer.setFilterType(filter.getFilterType());
			    		((Container)newParentNode.getObject()).addFilter(filPointer);
			    		filPointer.setPointedConfigName(sourceConfig.getName());
			    		filPointer.setPointedMartName(sourceConfig.getMart().getName());
			    		if(link != null)
			    			filPointer.addPointedDataset(pointedDataset);
			    			//filPointer.setPointedDatasetName(link.getPointedDataset());
			    		
			    		//filPointer.synchronizedFromXML();
			    		
					}else if(node.getObject() instanceof Container) {
						try {
							boolean rename = false;
							if(targetConfig.getContainerByName(((MartConfiguratorObject)node.getUserObject()).getName())!=null) {
								int n = JOptionPane.showConfirmDialog(
									    null,
									    "Container already exist, create a new container?",
									    "Question",
									    JOptionPane.YES_NO_OPTION);
								if(n==0) {
									rename = true;
								}else
									return false;
							}
							Container oldCon = (Container)node.getObject();
							Element containerElement = ((MartConfiguratorObject)node.getUserObject()).generateXml();
							Container newCon = new Container(containerElement);
							//recursively set all child nodes to the same config
							List<Attribute> attrList = oldCon.getAllAttributes(null, true, true);
							//List<Attribute> newAttList = newCon.getAllAttributes(null, true, true);
							for(Attribute attr : attrList) {
								Attribute newAttr = newCon.getAttributeByName(attr.getName());
								/*
								if(targetConfig.getMart().getMasterConfig().hasPointedAttribute(attr)){
									newCon.removeAttribute(newAttr);
									continue;							
								}*/
								if(targetConfig.hasPointedAttribute(attr)){
									newCon.removeAttribute(newAttr);
									continue;
								}
								
								
								newAttr.setConfig(targetConfig);
								newAttr.setParent(newCon);
								newAttr.setPointer(true);
								newAttr.setPointedElement(attr);
								newAttr.setPointedConfigName(sourceConfig.getName());
								newAttr.setPointedMartName(sourceConfig.getMart().getName());
								if(link != null)
									newAttr.addPointedDataset(pointedDataset);
									//newAttr.setPointedDatasetName(link.getPointedDataset());
								
								//newAttr.synchronizedFromXML();
								
							}
							List<Filter> filterList = oldCon.getAllFilters(null, true, true);
							for(Filter filter : filterList) {
								Filter newFilter = newCon.getFilterByName(filter.getName());
								/*
								if(targetConfig.getMart().getMasterConfig().hasPointedFilter(filter)){
									newCon.removeFilter(newFilter);
									continue;
								}*/
								if(targetConfig.hasPointedFilter(filter)){
									newCon.removeFilter(newFilter);
									continue;
								}
								
								
								newFilter.setConfig(targetConfig);
								newFilter.setParent(newCon);
								newFilter.setPointer(true);
								newFilter.setPointedElement(filter);
								newFilter.setPointedConfigName(sconfig.getName());
								newFilter.setPointedMartName(sourceConfig.getMart().getName());
								if(link != null)
									newFilter.addPointedDataset(pointedDataset);
									//newFilter.setPointedDatasetName(link.getPointedDataset());
								
								//newFilter.synchronizedFromXML();							
							}
							
							if(rename) {
								//TODO
								
							}
							if(!newCon.isEmpty()){
								((Container)newParentNode.getObject()).addContainer(newCon);
								newCon.synchronizedFromXML();
							}
						} catch (FunctionalException e) {
							e.printStackTrace();
						}
					}
				}
			}			
		}

		//Synchronize derived config tree to master config tree for newly added nodes
		try{
			((Config)tconfig).syncWithMasterconfig();
		}catch(Exception e){
			e.printStackTrace();
		}
    	//synchronize root
		//McTreeNode root = (McTreeNode)target.getModel().getRoot();
		//root.synchronizeNode();
		newParentNode.synchronizeNode();
		target.getModel().reload(newParentNode);
    	//newParentNode.synchronizeNode();
		
    	
		TreePath treePath = new TreePath(newParentNode.getPath());
		target.expandPath(treePath);
		target.scrollPathToVisible(treePath);
		target.setSelectionPath(treePath);
		//McGuiUtils.refreshGui(newParentNode);
		return(true);
	}

	private boolean moveToAttributeList(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode) {
		for(McTreeNode node: draggedNodes) {
			if(node.equals(newParentNode)) {
				JOptionPane.showMessageDialog(target.getParentDialog(), "cannot move to itself","error",JOptionPane.ERROR_MESSAGE);
				return false;				
			}
			Attribute attribute = (Attribute) node.getObject();
			((Attribute)newParentNode.getObject()).addAttribute(attribute);			
		}
		TreePath treePath = new TreePath(newParentNode.getPath());
		target.expandPath(treePath);
		target.scrollPathToVisible(treePath);
		target.setSelectionPath(treePath);
		return true;
	}
	
	private boolean insertToAttributeList(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode,int childIndex) {
		
		McTreeModel model = target.getModel();
		
		for(McTreeNode node: draggedNodes) {
			McTreeNode parent = (McTreeNode)node.getParent();
			if(node.equals(newParentNode)) {
				JOptionPane.showMessageDialog(target.getParentDialog(), "cannot move to itself","error",JOptionPane.ERROR_MESSAGE);
				return false;				
			}else if(parent.equals(newParentNode)){
				int curIndex = model.getIndexOfChild(newParentNode, node);
				if(curIndex < childIndex)
					model.insertNodeInto(node, (McTreeNode)newParentNode, childIndex-1);
				else			
					model.insertNodeInto(node, (McTreeNode)newParentNode, childIndex);
				
				if(parent != null) {
					if(parent.getObject() instanceof Container) {
						//do the data insertion behind the tree model						
						Container container = (Container)parent.getObject();
						List<Attribute> listAttributes = container.getAttributeList();
												
						if(curIndex < childIndex)
							Collections.rotate(listAttributes.subList(curIndex, childIndex), -1);
						else
							Collections.rotate(listAttributes.subList(childIndex++, curIndex+1), 1);						
					}
				}
			}else{
				Container container = (Container)parent.getObject();
				container.removeAttribute((Attribute)node.getObject());
				model.insertNodeInto(node, (McTreeNode)newParentNode, childIndex);	
				model.nodeStructureChanged(parent);
			}			
		}
		model.nodeStructureChanged(newParentNode);
		
		TreePath treePath = new TreePath(newParentNode.getPath());
		target.expandPath(treePath);
		target.scrollPathToVisible(treePath);
		target.setSelectionPath(treePath);
		return true;
	}

	private boolean moveToFilterList(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode) {
		for(McTreeNode node: draggedNodes) {
			if(node.equals(newParentNode)) {
				JOptionPane.showMessageDialog(target.getParentDialog(), "cannot move to itself","error",JOptionPane.ERROR_MESSAGE);
				return false;
			}
			Filter filter = (Filter)node.getObject();
			((Filter)newParentNode.getObject()).addFilter(filter);
		}

		TreePath treePath = new TreePath(newParentNode.getPath());
		target.expandPath(treePath);
		target.scrollPathToVisible(treePath);
		target.setSelectionPath(treePath);
		return true;			
	}
	
	private boolean insertToFilterList(MartConfigTree target, List<McTreeNode> draggedNodes, McTreeNode newParentNode,int childIndex) {
		
		McTreeModel model = target.getModel();
		
		for(McTreeNode node: draggedNodes) {
			McTreeNode parent = (McTreeNode)node.getParent();
			
			if(node.equals(newParentNode)) {
				JOptionPane.showMessageDialog(target.getParentDialog(), "cannot move to itself","error",JOptionPane.ERROR_MESSAGE);
				return false;
			}else if(parent.equals(newParentNode)){
				int curIndex = model.getIndexOfChild(newParentNode, node);
				if(curIndex < childIndex)
					model.insertNodeInto(node, (McTreeNode)newParentNode, childIndex-1);
				else
					model.insertNodeInto(node, (McTreeNode)newParentNode, childIndex);
				
				if(parent != null) {
					if(parent.getObject() instanceof Container) {
						//do the data insertion behind the tree model
						Container container = (Container)parent.getObject();
						List<Filter> listFilters = container.getFilterList();
						List<Attribute> listAttribute = container.getAttributeList();
						if(listAttribute.size() > 0){
							curIndex = curIndex -listAttribute.size();
							childIndex = childIndex - listAttribute.size();
						}
						if(curIndex >= listFilters.size() || childIndex >= listFilters.size())
							return false;
						if(curIndex < childIndex)
							Collections.rotate(listFilters.subList(curIndex, childIndex), -1);
						else
							Collections.rotate(listFilters.subList(childIndex++, curIndex+1), 1);
					}
				}
			}else{
				Container container = (Container)parent.getObject();
				container.removeFilter((Filter)node.getObject());
				model.insertNodeInto(node, (McTreeNode)newParentNode, childIndex);	
				model.nodeStructureChanged(parent);
			}
		}

		model.nodeStructureChanged(newParentNode);
		
		TreePath treePath = new TreePath(newParentNode.getPath());
		target.expandPath(treePath);
		target.scrollPathToVisible(treePath);
		target.setSelectionPath(treePath);
		return true;			
	}


	private boolean checkCanMove(McTreeNode target, List<McTreeNode> sources) {
		/*
		 * sources is Configs, target is Mart
		 * all nodes in the sources has to be config, and the Datasets of the source Mart has to be the
		 * subset of the Datasets of the target Mart
		 */
		if(target.getObject() instanceof Mart) {
			for(McTreeNode source: sources) {
				if(!(source.getObject() instanceof Config)) 
					return false;
				if(source.getParent().equals(target))
					return false;
			}				
			return true;
		} 
		/*
		 * target is Container, source can be Container, Filter, or Attribute
		 */
		else if(target.getObject() instanceof Container) {
			//TODO not allow more than one for now
			if(sources == null || sources.size()>1)
				return false;
			for(McTreeNode source: sources) {
				MartConfiguratorObject obj = source.getObject();
				if(obj instanceof Container || obj instanceof Filter || obj instanceof Attribute) {					
				}else {
					return false;
				}
			}
			return true;
		}
		/*
		 * target is GuiContainer, source is MartPointer
		 */
		else if(target.getObject() instanceof GuiContainer) {
			for(McTreeNode source: sources) {
				if(!(source.getObject() instanceof MartPointer)) {
					return false;					
				}
			}
			return true;
		}
		/*
		 * target is AttributeList, source is Attribute
		 */
		else if(target.getObject() instanceof Attribute) {
			for(McTreeNode source: sources) {
				if(!(source.getObject() instanceof Attribute)) {
					return false;										
				}
			}
			return true;
		}
		/*
		 * target is FilterList, source is Filter
		 */
		else if(target.getObject() instanceof Filter) {
			for(McTreeNode source: sources) {
				if(!(source.getObject() instanceof Filter)) {
					return false;										
				}
			}
			return true;
		}

		return false;
	}

	private boolean checkBeforeExcute(McTreeNode target, List<McTreeNode> sources) {
		/*
		 * sources is Configs, target is Mart
		 * all nodes in the sources has to be config, and the Datasets of the source Mart has to be the
		 * subset of the Datasets of the target Mart
		 */
		if(target.getObject() instanceof Mart) {
			Mart targetMart = (Mart)target.getObject();
			List<Dataset> targetDsList = targetMart.getDatasetList();
			List<String> targetDsNameList = new ArrayList<String>();
			for(Dataset ds: targetDsList) {
				targetDsNameList.add(ds.getName());
			}
			
			for(McTreeNode source: sources) {
				Mart sourceMart = ((Config)source.getObject()).getMart();
				List<Dataset> sourceDsList = sourceMart.getDatasetList();
				List<String> sourceDsNameList = new ArrayList<String>();
				for(Dataset ds: sourceDsList) {
					sourceDsNameList.add(ds.getName());
				}
				if(!targetDsNameList.containsAll(sourceDsNameList)) {
					JOptionPane.showMessageDialog(target.getParentDialog(), source.getObject().getName() + " includes datasets that " +
							"cannot be merged into target");
					return false;
				}				
			}
			return true;
		} 
		/*
		 * target is Container, source can be Container, Filter, or Attribute
		 */
		else if(target.getObject() instanceof Container) {
			return true;
		}
		/*
		 * target is GuiContainer, source is MartPointer
		 */
		else if(target.getObject() instanceof GuiContainer) {
			return true;
		}
		/*
		 * target is AttributeList, source is Attribute
		 */
		else if(target.getObject() instanceof Attribute) {
			return true;
		}
		/*
		 * target is FilterList, source is Filter
		 */
		else if(target.getObject() instanceof Filter) {
			return true;
		}

		return false;
	}

	private Dataset getDataset(MartConfigTree targetTree, boolean left) {
		if(!left) {
			TargetConfigPanel panel = (TargetConfigPanel)targetTree.getParent().getParent().getParent().getParent();
			return panel.getSelectedDataset();			
		}else {
			JSplitPane splitPane = (JSplitPane)targetTree.getParent().getParent().getParent().getParent().getParent();
			//c is jscrollpane
			SourceConfigPanel spanel = (SourceConfigPanel)splitPane.getLeftComponent();
			return spanel.getSelectedDataset();
		}
	}

}
