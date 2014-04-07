package org.biomart.configurator.controller;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.TreePath;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.MartConfigSourceTree;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.component.SourceConfigPanel;
import org.biomart.configurator.view.component.TargetConfigPanel;
import org.biomart.configurator.view.gui.dialogs.DeleteReferenceDialog;
import org.biomart.configurator.view.gui.dialogs.FilterDropDownDialog;
import org.biomart.configurator.view.gui.dialogs.RenameDialog;
import org.biomart.configurator.view.gui.dialogs.SearchResultDialog;
import org.biomart.configurator.view.menu.AttributeTableConfig;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class TreeNodeHandler {
	
	/**
	 * create a filter based on the attribute, and add the filter into the same container as the attribute
	 * @param node
	 * @return
	 */
	private List<MartConfiguratorObject> foundObjects = new ArrayList<MartConfiguratorObject>();
	private Map<MartConfiguratorObject,List<MartConfiguratorObject>> refMap = new HashMap<MartConfiguratorObject,List<MartConfiguratorObject>>();
	
	public boolean requestCreateFilterFromAttribute(MartConfigTree tree, McTreeNode node) {
		Attribute attribute = (Attribute)node.getObject();
		ObjectController oc = new ObjectController();
		if(node.getObject().getObjectStatus() == ValidationStatus.VALID) {
			String filterName = McGuiUtils.INSTANCE.getUniqueFilterName(attribute.getParentConfig(),attribute.getName());
			Filter filter = new Filter(attribute, filterName);
			attribute.getParentContainer().addFilter(filter);
			Config config = attribute.getParentConfig();
			oc.generateFilterRDF(filter, config);
			//synchronise the container to show the new added filter
			//if(node.getParent() != null)
				//((McTreeNode)node.getParent()).synchronizeNode();
				//tree.getModel().reload(node.getParent());
			return true;
		} else {
			JOptionPane.showMessageDialog(tree.getParentDialog(),
				    "Invalid Attribute.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
    
    public ValidationStatus requestValidateRegistry() {
    	return Validation.validate(McGuiUtils.INSTANCE.getRegistryObject(),false);
    }

    private void deleteAttribute(MartConfigTree tree, McTreeNode node) {
    	//check references
		Attribute currentAttribute = (Attribute)node.getObject();
		
		currentAttribute.getParentContainer().removeAttribute(currentAttribute);
		McTreeNode parent = (McTreeNode)node.getParent();
		node.removeFromParent();
		tree.getModel().nodeStructureChanged(parent);
		//if master config, also delete attribtue from derived configs
		if(currentAttribute.getParentConfig().isMasterConfig()){
			//((MartConfigSourceTree)tree).refreshTargetTree();
			for(Config config : currentAttribute.getParentConfig().getMart().getConfigList()){
				Attribute a = config.getAttributeByName(currentAttribute.getName(), null);
				if(a != null){
					a.getParentContainer().removeAttribute(a);
				}
			}
		}
    }
    
    private void deleteFilter(MartConfigTree tree, McTreeNode node) {
    	Filter currentFilter = (Filter)node.getObject();
    	currentFilter.getParentContainer().removeFilter(currentFilter);
		McTreeNode parent = (McTreeNode)node.getParent();
    	node.removeFromParent();
    	tree.getModel().nodeStructureChanged(parent);
    	//if master config, also delete filter from derived configs
		if(currentFilter.getParentConfig().isMasterConfig()){
			//((MartConfigSourceTree)tree).refreshTargetTree();
			for(Config config : currentFilter.getParentConfig().getMart().getConfigList()){
				Filter f = config.getFilterByName(currentFilter.getName(), null);
				if(f != null){
					f.getParentContainer().removeFilter(f);
				}
			}
		}
    }
    
    private void deleteLink(MartConfigTree tree, McTreeNode node) {
    	Link currentLink = (Link)node.getObject();
    	//if delete a link remove all the links in the config list    	
		List<Config> configs = currentLink.getParentConfig().getMart().getConfigList();
		for(Config c : configs){
			c.removeLink(currentLink);    			
		}
		//backward compatibility
		if(currentLink.getPointedMart() != null){
			List<Config> pconfigs = currentLink.getPointedMart().getConfigList();
			for(Config c : pconfigs){
				Link link = McUtils.getOtherLink(currentLink);//McUtils.getLink(c,currentLink.getParentConfig());
				
	    		if(null!=link){
	    			c.removeLink(link);
	    		}			
			}
		}
    	
		McTreeNode parent = (McTreeNode)node.getParent();
    	node.removeFromParent();
    	
    	tree.getModel().nodeStructureChanged(parent);
    	//if master config, refresh child configs
		/*if(tree instanceof MartConfigSourceTree){
			McTreeNode root = (McTreeNode)((MartConfigSourceTree)tree).getModel().getRoot();
			root.synchronizeNode();
			tree.getModel().nodeStructureChanged(root);
			((MartConfigSourceTree)tree).refreshTargetTree();
		}
		if(tree instanceof MartConfigTargetTree){
			McTreeNode root = (McTreeNode)((MartConfigTargetTree)tree).getModel().getRoot();
			root.synchronizeNode();
			tree.getModel().nodeStructureChanged(root);
			((MartConfigTargetTree)tree).refreshSourceTree();
		}*/
    }

    
    private void deleteContainer(MartConfigTree tree, McTreeNode node) {
    	McTreeNode parent = (McTreeNode)node.getParent();
    	//cannot remove root container
		if(!(parent.getObject() instanceof Container))
			return;
		Container currentContainer = (Container)node.getObject();
		((Container)parent.getObject()).removeContainer(currentContainer);
		node.removeFromParent();
		tree.getModel().nodeStructureChanged(parent);
		//if master config, delete container from derived configs as well
		if(currentContainer.getParentConfig().isMasterConfig()){
			//((MartConfigSourceTree)tree).refreshTargetTree();
			for(Config config : currentContainer.getParentConfig().getMart().getConfigList()){
				Container c = config.getContainerByName(currentContainer.getName());
				if(c != null){
					c.getParentContainer().removeContainer(c);
				}
			}
		}
		
    }
    
    private void deleteGuiContainer(MartConfigTree tree, McTreeNode node) {
    	McTreeNode parent = (McTreeNode)node.getParent();
    	//cannot remove root container
		if(!(parent.getObject() instanceof GuiContainer))
			return;
		GuiContainer currentGC = (GuiContainer)node.getObject();
		((GuiContainer)parent.getObject()).removeGuiContainer(currentGC);
		node.removeFromParent();
		tree.getModel().nodeStructureChanged(parent);
		//if master config, refresh child configs
		/*if(tree instanceof MartConfigSourceTree){
			((MartConfigSourceTree)tree).refreshTargetTree();
		}*/
    }
    
    private void deleteMartPointer(MartConfigTree tree, McTreeNode node) {
    	McTreeNode parent = (McTreeNode)node.getParent();
    	GuiContainer gc = (GuiContainer)parent.getObject();
    	gc.removeMartPointer((MartPointer)node.getObject());
    	node.removeFromParent();
		tree.getModel().nodeStructureChanged(parent);
		//if master config, refresh child configs
		/*if(tree instanceof MartConfigSourceTree){
			((MartConfigSourceTree)tree).refreshTargetTree();
		}*/
    }
     
    public void requestDeleteNodes(MartConfigTree tree, TreePath[] treePaths) {
    	
    	//need to remove all descendant path first
    	//Date startTime = new Date(System.currentTimeMillis());
		//System.out.println("Method prepare node list start at:"+startTime.toString());
		
		
		List<McTreeNode> nodeList = new ArrayList<McTreeNode>();
		for(TreePath tp1: treePaths) {
			boolean is1Descendantof2 = false;
			for(TreePath tp2: treePaths) {
				if(tp1 == tp2)
					break;
				if(tp2.isDescendant(tp1)) {
					is1Descendantof2 = true;
					break;
				}					
			}
			if(!is1Descendantof2) {
				nodeList.add((McTreeNode)tp1.getLastPathComponent());
			}
		}	
		
		//generate a reference map based on the selected nodes
    	this.refMap.clear();
    	
		Mart currentMart = nodeList.get(0).getObject().getParentConfig().getMart();
		
		for(Mart mt: currentMart.getMartRegistry().getMartList()) {
			for(Config config: mt.getConfigList()) {
				List<Attribute> atList = config.getAttributes(new ArrayList<String>(), true, true);
				List<Filter> ftList = config.getFilters(new ArrayList<String>(), true, true);
				for(Attribute tmpAtt :atList) {
					
					if(tmpAtt.isPointer()){
						Attribute a = tmpAtt.getPointedAttribute();
						if(!refMap.containsKey(a)){
							List<MartConfiguratorObject> mcoList = new ArrayList<MartConfiguratorObject>();
							mcoList.add(tmpAtt);
							refMap.put(a , mcoList);
						}else{
							refMap.get(a).add(tmpAtt);
						}
					}
					else if(tmpAtt.isAttributeList()) {
						for(Attribute a: tmpAtt.getAttributeList()){
							if(!refMap.containsKey(a)){
								List<MartConfiguratorObject> mcoList = new ArrayList<MartConfiguratorObject>();
								mcoList.add(tmpAtt);
								refMap.put(a , mcoList);
							}else{
								refMap.get(a).add(tmpAtt);
							}
						}
					}
				}
				
				for(Filter tmpFilter :ftList) {
					
					if(tmpFilter.isPointer()){
						Filter pointedFilter = tmpFilter.getPointedFilter();
						
						if(!refMap.containsKey(pointedFilter)){
							List<MartConfiguratorObject> mcoList = new ArrayList<MartConfiguratorObject>();
							mcoList.add(tmpFilter);
							refMap.put(pointedFilter , mcoList);
						}else{
							refMap.get(pointedFilter).add(tmpFilter);
						}
						
					}
					else if((tmpFilter.isFilterList())) {
						for(Filter filter :tmpFilter.getFilterList()){
							if(!refMap.containsKey(filter)){
								List<MartConfiguratorObject> mcoList = new ArrayList<MartConfiguratorObject>();
								mcoList.add(tmpFilter);
								refMap.put(filter , mcoList);
							}else{
								refMap.get(filter).add(tmpFilter);
							}
						}
					}else{
						Attribute attribute = tmpFilter.getAttribute();
						if(!refMap.containsKey(attribute)){
							List<MartConfiguratorObject> mcoList = new ArrayList<MartConfiguratorObject>();
							mcoList.add(tmpFilter);
							refMap.put(attribute , mcoList);
						}else{
							refMap.get(attribute).add(tmpFilter);
						}
					}
					 
				}
			}
		}
    	
		//Date endTime = new Date(System.currentTimeMillis());
		//System.out.println("Method prepare node list "+nodeList.size()+" end at:"+endTime.toString());
		
		boolean hasError = false;
		//firstly sort the node list with the priority of 
		//filterlist > filter > attributelist> attribute> container
		//startTime = new Date(System.currentTimeMillis());
		//System.out.println("Method delete filter list start at:"+startTime.toString());
    	//create a tree copy
		List<MartConfiguratorObject> referenceObjs = new ArrayList<MartConfiguratorObject>();
		for(McTreeNode node : nodeList){
			if(this.refMap.containsKey(node.getObject()))
				referenceObjs.addAll(this.refMap.get(node.getObject()))	;
		}
		
		
		if(referenceObjs.size() > 0){
	    	McTreeNode root = new McTreeNode(((McTreeNode)tree.getModel().getRoot()).getObject());
	    	for(MartConfiguratorObject object: referenceObjs) 
	    		this.addObjectToTree(root, object);
	    	
	    	DeleteReferenceDialog drd = (DeleteReferenceDialog) this.showDeleteReferenceDialog(root, tree, null, "Following will be affected by deletion",tree.getParentDialog());
	    	if(!drd.isProceed())
				return;
		}
		
		
		
		for(McTreeNode node : nodeList){
			this.deleteNode(tree,node,hasError);
		}		
		
		//tree.getModel().nodeStructureChanged((TreeNode)tree.getModel().getRoot());
    	//if master config, refresh child configs
		if(tree instanceof MartConfigSourceTree){
			((MartConfigSourceTree)tree).refreshTargetTree();
		}
    }
    
   
	private void deleteMart(MartConfigTree tree, McTreeNode node) {
    	McTreeNode parent = (McTreeNode)node.getParent();
    	((MartRegistry)parent.getObject()).removeMart((Mart)node.getObject());
    	node.removeFromParent();
    	//remove options
    	Element optionElement = Options.getInstance().getOptionRootElement();
    	//find mart option
    	 @SuppressWarnings("unchecked")
    	List<Element> martOptionList = optionElement.getChildren();
    	Element martElement = null;
    	for(Element martE: martOptionList) {
    		if(martE.getAttributeValue(XMLElements.NAME.toString()).equals(node.getObject().getName())) {
    			martElement = martE;
    			break;
    		}
    	}
    	if(martElement!=null)
    		optionElement.removeContent(martElement);
    	tree.getModel().nodeStructureChanged(parent);
    	//if master config, refresh child configs
		/*if(tree instanceof MartConfigSourceTree){
			((MartConfigSourceTree)tree).refreshTargetTree();
		}*/
    }
    
	private boolean deleteNode(MartConfigTree tree, McTreeNode node, boolean hasError) {
    	MartConfiguratorObject mcObj = node.getObject();
    	//delete node only when its not in master config 
    	
    	if(mcObj instanceof MartPointer) {
    		this.deleteMartPointer(tree, node);
    	}else if(mcObj instanceof GuiContainer) {
    		this.deleteGuiContainer(tree, node);
    	}
    	else if(mcObj instanceof Mart) {
    		this.deleteMart(tree, node);
    	}else if(mcObj instanceof Container) {
    		Container container = (Container) mcObj;
    		this.deleteContainer(tree, node);
    	}else if(mcObj instanceof Filter) {
    		Filter filter = (Filter)mcObj;
    		this.deleteFilter(tree,node);
    		
    	}else if(mcObj instanceof Attribute) {
    		Attribute a = (Attribute)mcObj;
    		this.deleteAttribute(tree,node);
    		
    	}else if(mcObj instanceof Link) {
    		this.deleteLink(tree, node);
    	}
    	
    	return true;
    }

    /**
     * find the reference objects of the node and show them in the search tree
     * @param node
     */
    public void requestObjectReferences(MartConfigTree mcTree, McTreeNode node) {
    	//find reference of the object
    	List<MartConfiguratorObject> referenceObjs = new ArrayList<MartConfiguratorObject>();
    	MartConfiguratorObject mcObj = node.getObject();
    	JDialog parentDialog = ((McTreeNode)node.getRoot()).getParentDialog();
    	if(mcObj instanceof Attribute) {
    		List<MartConfiguratorObject> elementList = ((Attribute)mcObj).getAllReferences();
    		for(MartConfiguratorObject element: elementList) 
    			referenceObjs.add(element);
    		
    	}else if(mcObj instanceof Filter) {
    		List<Filter> filterList = ((Filter)mcObj).getReferences();
    		for(MartConfiguratorObject filter: filterList)
    			referenceObjs.add(filter);
    	}else if(mcObj instanceof DatasetTable) {
    		List<Attribute> attributeList = ((DatasetTable)mcObj).getReferences();
    		for(Attribute attribute: attributeList) {
    			referenceObjs.add(attribute);
    		}
    	}else if(mcObj instanceof DatasetColumn) {
    		List<Attribute> attributeList = ((DatasetColumn)mcObj).getReferences();
    		for(Attribute attribute: attributeList) {
    			referenceObjs.add(attribute);
    		}    		
    	}else if(mcObj instanceof Mart) {
    		referenceObjs.addAll( ((Mart)mcObj).getReferences());
    	}else if(mcObj instanceof ElementList) {
    		referenceObjs.addAll(((ElementList)mcObj).getReferences());
    	}
    	if(referenceObjs.isEmpty()) {
    		JOptionPane.showMessageDialog(mcTree.getParentDialog(), "no object found");
    		return;
    	}
    	
    	//create a tree copy
    	McTreeNode root = new McTreeNode(((McTreeNode)mcTree.getModel().getRoot()).getObject());
    	for(MartConfiguratorObject object: referenceObjs) 
    		this.addObjectToTree(root, object);
    	
    	this.showSearchResultDialog(root, mcTree, null, "All references",mcTree.getParentDialog());
    	    	
    }
 
    public void requestHideNodes(MartConfigTree mcTree, List<McTreeNode> nodes, boolean hide) {
    	for(McTreeNode node: nodes) {
    		node.getObject().setHideValue(hide);
     	}
    }
    
    private void addObjectToTree(McTreeNode root, MartConfiguratorObject nodeObject) {
    	List<MartConfiguratorObject> objectPath = McGuiUtils.INSTANCE.getPathToNode(root,nodeObject);
    	//start from index 1, index 0 is root
    	McTreeNode tmp = root;
    	for(int i=1; i<objectPath.size(); i++) {
    		MartConfiguratorObject object = objectPath.get(i);
    		McTreeNode child = tmp.getChildByObject(object);
    		if(child!=null) 
    			tmp = child;
    		else { //not exist, insert the rest in the tree
    			for(int j = i; j<objectPath.size(); j++) {
    				McTreeNode treeNode = new McTreeNode(objectPath.get(j));
    				tmp.add(treeNode);
    				tmp = treeNode;
    			}
    			return;
    		}
    	}
    }

    public void requestDropDown(MartConfigTree tree, McTreeNode node) {
    	new FilterDropDownDialog(tree.getParentDialog(), (Filter)node.getObject());
    }

    public void requestSearchNode(MartConfiguratorObject root, JPanel treePanel,
    		String type, String value, XMLElements xe, boolean caseSensitive, boolean like, String scope,JDialog parentDialog) {
		MartConfigTree tree = null;
		SourceConfigPanel scp = null;
    	McTreeNode rootNode = new McTreeNode(root);

		if(scope.equals(Resources.get("SCOPEENTIREPORTAL"))) {
			scp = (SourceConfigPanel)treePanel;
		}else if(scope.equals(Resources.get("SCOPECURRENTSOURCE"))) {
			scp = (SourceConfigPanel)treePanel;
		}else {
			tree = ((TargetConfigPanel)treePanel).getTree();
		}

    	List<MartConfiguratorObject> foundObjects = this.searchObjects(root, type, value, xe, caseSensitive, like);
 
    	if(!foundObjects.isEmpty()) {
    		for(MartConfiguratorObject foundObject: foundObjects) {
    			this.addObjectToTree(rootNode, foundObject);
    		}
			this.showSearchResultDialog(rootNode, tree, scp, "Search Result",parentDialog);
    	}   	
    }

    private List<MartConfiguratorObject> searchObjects(MartConfiguratorObject rootObj, 
    		String type, String value, XMLElements xtype, boolean caseSensitive, boolean like) {

    	List<MartConfiguratorObject> foundObjects = new ArrayList<MartConfiguratorObject>();
    	//TODO need to improve
    	if(PartitionReferenceController.isObjectMatch(rootObj, type, value, xtype, caseSensitive, like))
    		foundObjects.add(rootObj);
    	//check mart
    	//rootObj is either a config or martregistry for now
    	if(rootObj instanceof Config) {
    		Config config = (Config)rootObj;
    		foundObjects.addAll(this.searchObjectsInConfig(config, type, value, xtype, caseSensitive, like));
    	} else {
    		MartRegistry mr = (MartRegistry) rootObj;
    		for(Mart mart:mr.getMartList()) {
    			Config masterConfig = mart.getMasterConfig();
    			foundObjects.addAll(this.searchObjectsInConfig(masterConfig, type, value, xtype, caseSensitive, like));
    		}
    	}
    	return foundObjects;
    }
   
    private List<MartConfiguratorObject> searchObjectsInConfig(Config config, 
    		String type, String value, XMLElements xtype, boolean caseSensitive, boolean like) {

    	List<MartConfiguratorObject> foundObjects = new ArrayList<MartConfiguratorObject>();
    	//TODO need to improve
    	if(PartitionReferenceController.isObjectMatch(config, type, value, xtype, caseSensitive, like))
    		foundObjects.add(config);
    	//check mart
    	//rootObj is either a config or martregistry for now
    	//check config
		if(PartitionReferenceController.isObjectMatch(config, type, value, xtype, caseSensitive, like))
			foundObjects.add(config);
		Container rootContainer = config.getRootContainer();
		if(PartitionReferenceController.isObjectMatch(rootContainer, type, value, xtype, caseSensitive, like))
			foundObjects.add(rootContainer);
		foundObjects.addAll(searchContainer(rootContainer, type, value, xtype, caseSensitive, like));
      		
    	return foundObjects;
    }
    
    private List<MartConfiguratorObject> searchContainer(Container container, String type, String value, XMLElements sType, 
    		boolean caseSensitive, boolean like) {
    	List<MartConfiguratorObject> result = new ArrayList<MartConfiguratorObject>();
    	if(PartitionReferenceController.isObjectMatch(container,type,value,sType, caseSensitive, like))
    		result.add(container);
    	
    	for(Attribute attribute: container.getAttributeList()) {
    		if(PartitionReferenceController.isObjectMatch(attribute,type,value,sType, caseSensitive, like))
    			result.add(attribute);
    	}
    	for(Filter filter: container.getFilterList()) {
    		if(PartitionReferenceController.isObjectMatch(filter,type,value,sType, caseSensitive, like))
    			result.add(filter);
    	}
    	for(Container subContainer: container.getContainerList()) {
    		result.addAll(searchContainer(subContainer, type, value, sType, caseSensitive, like));
    	}
    	return result;
    }

    private JDialog showSearchResultDialog(McTreeNode root, MartConfigTree tree, SourceConfigPanel scp, String title,JDialog parent) {
    	return new SearchResultDialog(root,tree, scp , title,parent);
    }
    
    private JDialog showDeleteReferenceDialog(McTreeNode root, MartConfigTree tree, SourceConfigPanel scp, String title,JDialog parent){
    	return new DeleteReferenceDialog(root,tree,scp,title,parent);
    }
    
    public void requestPartitionReferences(String ref, Mart mart) {

		for(Config config: mart.getConfigList()) {
			if(this.searchPtReferences(config, ref))
				foundObjects.add(config);
			Container rootContainer = config.getRootContainer();
			PartitionReferenceController prc = new PartitionReferenceController();
			foundObjects.addAll(prc.searchContainerPtReferences(rootContainer, "all", ref, true, true));
		}
    	    	
    	if(foundObjects.isEmpty())
    		return;
    	//create a tree copy    
/*    	MartConfigTree mcTree = ((McViewTree)McViews.getInstance().getView(IdwViewType.MCTREE)).getMcTree();
    	McTreeNode root = new McTreeNode(((McTreeNode)mcTree.getModel().getRoot()).getObject());
    	for(MartConfiguratorObject object: foundObjects) 
    		this.addObjectToTree(root, object);   	
    	this.showSearchView(root, mcTree);*/
    }
    
    private boolean searchPtReferences(MartConfiguratorObject object, String ref) {
    	org.jdom.Element element = AttributeTableConfig.getInstance().getElementByName(object.getNodeType().toString());
		List<org.jdom.Element> eList = element.getChildren("item");
		for(org.jdom.Element e: eList) {
			if(!"1".equals(e.getAttributeValue("visible")))
				continue;
			XMLElements xType = XMLElements.valueFrom(e.getAttributeValue(XMLElements.NAME.toString()));
			if(PartitionReferenceController.isObjectMatch(object,"all",ref,xType,true,true)) {
				return true;
			}
		}
		return false;
    }
    
    
    public void requestObjectRename(MartConfigTree mcTree, final McTreeNode node) {
    	//check all references
    	new RenameDialog(mcTree.getParentDialog(), node.getObject());
    }

    public void requestDuplicateMartOption(String oldMartName, String newMartName) {
    	Element optionElement = Options.getInstance().getOptionRootElement();
    	//find the mart
    	Element martE = null;
    	List<Element> martElementList = optionElement.getChildren();
    	for(Element martElement: martElementList) {
    		if(martElement.getAttributeValue(XMLElements.NAME.toString()).equals(oldMartName)) {
    			martE = martElement;
    			break;
    		}
    	}
    	if(martE == null)
    		return;
    	Element newMartE = (Element)martE.clone();
    	newMartE.detach();
    	newMartE.setAttribute(XMLElements.NAME.toString(), newMartName);
    	optionElement.addContent(newMartE);
    }
    
    public void requestNewAttribute(DatasetColumn column) {
    	DatasetTable dst = column.getDatasetTable();
    	Mart mart = dst.getMart();
    	Config config = mart.getMasterConfig();
    	
		//find container
		String attContainerName = dst.getName()+"_attribute";
		Container c = config.getContainerByName(attContainerName);
		if(c == null) {
			c = new Container(attContainerName);
			config.getRootContainer().addContainer(c);
		}
		Attribute a = new Attribute(column,dst.getName()+"__"+column.getName());
		c.addAttribute(a);
		a.setVisibleModified(true);
		c.setVisibleModified(true);
		
		String filContainerName = dst.getName()+"_filter";
		Container c1 = config.getContainerByName(filContainerName);
		if(c1 == null) {
			c1 = new Container(filContainerName);
			config.getRootContainer().addContainer(c1);
		}
		Filter filter = new Filter(a,a.getName());
		c1.addFilter(filter); 
		filter.setVisibleModified(true);
		c1.setVisibleModified(true);
    	
    	dst.setOrphan(false);
    	column.setOrphan(false);
    }
    
    public void exportConfig(Component parent, Config config) {
		final String currentDir = Settings.getProperty("currentSaveDir");
		final JFileChooser xmlFileChooser = new JFileChooser();
		xmlFileChooser.setCurrentDirectory(currentDir == null ? null
				: new File(currentDir));
		if (xmlFileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			Settings.setProperty("currentSaveDir", xmlFileChooser
					.getCurrentDirectory().getPath());

			// Find out the file the user chose.
			final File saveAsFile = xmlFileChooser.getSelectedFile();

			// Skip the rest if they cancelled the save box.
			if (saveAsFile == null)
				return;

	    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	     	try {
	     		Document doc = new Document(config.generateXml());
	    		FileOutputStream fos = new FileOutputStream(saveAsFile);
	    		outputter.output(doc, fos);
	    		fos.close();
	    	}
	    	catch(Exception e) {
	    		e.printStackTrace();
	    		JOptionPane.showMessageDialog(parent, Resources.get("SAVEXMLERROR"));
	    	}   	
	 	}

    }
    
    public void exportOptions(Component parent, Config config) {
		final String currentDir = Settings.getProperty("currentSaveDir");
		final JFileChooser xmlFileChooser = new JFileChooser();
		xmlFileChooser.setCurrentDirectory(currentDir == null ? null
				: new File(currentDir));
		if (xmlFileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			Settings.setProperty("currentSaveDir", xmlFileChooser
					.getCurrentDirectory().getPath());

			// Find out the file the user chose.
			final File saveAsFile = xmlFileChooser.getSelectedFile();

			// Skip the rest if they cancelled the save box.
			if (saveAsFile == null)
				return;
			List<org.jdom.Element> optionElementList = Options.getInstance().getOptionRootElement().getChildren();
			//get the right config
			Mart mart = config.getMart();
			org.jdom.Element martElement = null;
			for(org.jdom.Element tmpElement: optionElementList) {
				if(tmpElement.getAttributeValue(XMLElements.NAME.toString()).equals(mart.getName())) {
					martElement = tmpElement;
					break;
				}
			}
			if(martElement == null)
				return;
			org.jdom.Element configElement = null;
			List<org.jdom.Element> configElementList = martElement.getChildren();
			for(org.jdom.Element tmpElement: configElementList) {
				if(tmpElement.getAttributeValue(XMLElements.NAME.toString()).equals(config.getName())) {
					configElement = tmpElement;
					break;
				}					
			}
			if(configElement == null) 
				return;



	    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	     	try {
	     		Document doc = new Document(configElement);
	    		FileOutputStream fos = new FileOutputStream(saveAsFile);
	    		outputter.output(doc, fos);
	    		fos.close();
	    	}
	    	catch(Exception e) {
	    		e.printStackTrace();
	    		JOptionPane.showMessageDialog(parent, Resources.get("SAVEXMLERROR"));
	    	}   	
	 	}    	
    }


}