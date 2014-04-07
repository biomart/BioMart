package org.biomart.configurator.view.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.McEventBus;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.controller.TreeNodeHandler;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.gui.dialogs.LinkDatasetDialog;
import org.biomart.configurator.view.gui.dialogs.LinkManagementDialog;
import org.biomart.configurator.view.gui.dialogs.PartitionTableDialog;
import org.biomart.configurator.view.gui.dialogs.SchemaDialog;
import org.biomart.configurator.view.gui.dialogs.TableDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;

/**
 * A singleton class that read contextMenu.xml and construct the popup menu
 * corresponding to the selected node
 * 
 * 
 */
public class TreeNodeMenuConstructor implements ActionListener {
	private String contextMenuXML = "conf/xml/contextMenu.xml";
	private JPopupMenu contextMenu;
	private static TreeNodeMenuConstructor instance = null;
	private Element root;
	private JTree ltree;
	private int xPoint, yPoint;
	private TreeNodeHandler nodeHandler;

	public static TreeNodeMenuConstructor getInstance() {
		if (instance == null)
			instance = new TreeNodeMenuConstructor();
		return instance;
	}

	private TreeNodeMenuConstructor() {
		contextMenu = new JPopupMenu();
		try {
			// Build the document with SAX and Xerces, no validation
			SAXBuilder builder = new SAXBuilder();
			// Create the document
			Document doc = builder.build(new File(contextMenuXML));
			root = doc.getRootElement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		nodeHandler = new TreeNodeHandler();
	}

	@SuppressWarnings("unchecked")
	// uncheck the warning message from jdom
	public JPopupMenu getContextMenu(MartConfiguratorObject mcObj, JTree ltree,
			int x, int y) {
		String name = mcObj.getNodeType().toString();
		this.ltree = ltree;
		this.xPoint = x;
		this.yPoint = y;
		contextMenu.removeAll();
		List<Element> nodeList = root.getChildren();
		if (nodeList == null)
			return null;
		for (Element nodeElement : nodeList) {
			if (nodeElement.getAttributeValue("name").equals(name)) {
				List<Element> childAttr = nodeElement.getChildren();
				if (childAttr == null)
					return null;
				for (Element item : childAttr) {
					if (item.getName().equals("Separator"))
						contextMenu.addSeparator();
					else {
						boolean shows = true;
						/*
						 * String inViews = item.getAttributeValue("inViews");
						 * if(inViews!=null) { shows = false; String[]
						 * inViewsArray = inViews.split(","); String currentView
						 * = McGuiUtils.INSTANCE.getMcViewType().toString();
						 * for(String viewItem:inViewsArray) {
						 * if(viewItem.equalsIgnoreCase(currentView)) { shows =
						 * true; break; } } }
						 */
						boolean advancedMenu = Boolean.parseBoolean(item.getAttributeValue("advanced"));
						if(advancedMenu && 
								!Boolean.parseBoolean(Settings.getProperty("showadvancedmenu")))
							shows = false;
						if (shows) {
							// is it a boolean menu?
							// get submenu
							List<Element> subMenu = item.getChildren();
							if (subMenu.size() > 0) {
								JMenu menu = new JMenu(
										item.getAttributeValue("title"));
								contextMenu.add(menu);
								for (Element subItem : subMenu) {
									menu.add(createMenuItem(subItem));
								}
							} else {
								contextMenu.add(createMenuItem(item));
							}
						}
					}
				}
				break;
			} // end of for
		}
		this.createCustomizedMenu(mcObj);
		return this.contextMenu;
	}

	private boolean isMenuDisabled(Element element) {
		String disabled = element.getAttributeValue("disabled");
		if ("1".equals(disabled))
			return true;
		// hard code for materialize and partition

		if (element.getAttributeValue("name").equals("materialize")) {
			if (ltree == null)
				return false;
			TreePath pathTarget = ltree.getPathForLocation(xPoint, yPoint);
			McTreeNode node = (McTreeNode) pathTarget.getLastPathComponent();
			if (node == null)
				return true;
			// only for source schema
			// node is mart
			Mart mart = (Mart) node.getObject();
			if (mart.hasSource())
				return false;
			return true;
			/*
			 * if(mart.getDataLinkType().equals(DataLinkType.SOURCE)) return
			 * false; else return true;
			 */
		} else if (element.getAttributeValue("name").equals("hide4user")) {
			// disable if it is not synchronized user
			// List<String> users =
			// McGuiUtils.INSTANCE.getSynchronizedUserList(McGuiUtils.INSTANCE.getCurrentUser().getUserName());
			// if(users == null || users.size() == 0)
			// return true;
			// else
			return false;
		} else if (element.getAttributeValue("name").equals("linkManagement")) {
			// set link info item to be disable if no link info
			if (ltree == null)
				return false;
			TreePath pathTarget = ltree.getPathForLocation(xPoint, yPoint);
			McTreeNode node = (McTreeNode) pathTarget.getLastPathComponent();
			if (node == null)
				return true;

			Mart mart = (Mart) node.getObject();
			if (mart.getMasterConfig() == null
					|| mart.getMasterConfig().getLinkList().isEmpty())
				return true;
			return false;
		}else if (element.getAttributeValue("name").equals("createLinkIndex")){
			if (ltree == null)
				return false;
			TreePath pathTarget = ltree.getPathForLocation(xPoint, yPoint);
			McTreeNode node = (McTreeNode) pathTarget.getLastPathComponent();
			if (node == null)
				return true;

			Mart mart = (Mart) node.getObject();
			if (mart.getMasterConfig() == null
					|| mart.getMasterConfig().getLinkList().isEmpty())
				return true;
			return false;
		}
		/*
		 * else if(element.getAttributeValue("name").equals("addLinkedDataSet")
		 * || } element.getAttributeValue("name").equals("addLinkIndices")) {
		 * //disable if no link available if(ltree == null) return false;
		 * TreePath pathTarget = ltree.getPathForLocation(xPoint,yPoint);
		 * McTreeNode node = (McTreeNode) pathTarget.getLastPathComponent();
		 * if(node == null) return false; //node is dataset
		 * 
		 * return false; } else
		 * if(element.getAttributeValue("name").equals("paste")) { McViewSchema
		 * schemaView =
		 * (McViewSchema)McViews.getInstance().getView(IdwViewType.SCHEMA);
		 * if(schemaView.getObjectCopies().isEmpty()) return true; else return
		 * false; }
		 */
		return false;
	}

	private JMenuItem createMenuItem(Element element) {
		JMenuItem menuItem = null;
		String type = element.getAttributeValue("type");
		// checkbox
		if ("checkbox".equals(type)) {
			menuItem = new JCheckBoxMenuItem(element.getAttributeValue("title"));
			((JCheckBoxMenuItem) menuItem).setSelected(isSelected(element
					.getAttributeValue("property")));
		} else {
			menuItem = new JMenuItem(element.getAttributeValue("title"));
		}
		menuItem.addActionListener(this);
		menuItem.setActionCommand(element.getAttributeValue("name"));
		String shortcut = element.getAttributeValue("shortcut");
		if (!McUtils.isStringEmpty(shortcut)) {
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
					java.awt.event.KeyEvent.VK_UP, java.awt.Event.CTRL_MASK));
		}
		if (isMenuDisabled(element))
			menuItem.setEnabled(false);

		return menuItem;
	}

	private boolean isSelected(String property) {
		TreePath pathTarget = ltree.getPathForLocation(xPoint, yPoint);
		McTreeNode node = (McTreeNode) pathTarget.getLastPathComponent();
		if (node == null)
			return false;
		if ("hide".equals(property))
			return node.getObject().isHidden();
		else if ("master".equals(property))
			return ((Config) node.getObject()).isMasterConfig();
		return false;
	}

	private void createCustomizedMenu(MartConfiguratorObject mcObj) {
		if (mcObj instanceof Mart) {
			Mart mart = (Mart) mcObj;
			JMenu partitionMenu = new JMenu("partition");
			for (PartitionTable pt : mart.getPartitionTableList()) {
				JMenuItem item = new JMenuItem(pt.getName());
				item.setActionCommand("partition." + pt.getName());
				item.addActionListener(this);
				partitionMenu.add(item);
			}

			// this.contextMenu.add(partitionMenu);
			

			DefaultMutableTreeNode root = (DefaultMutableTreeNode)this.ltree.getModel().getRoot();
			if(!root.getUserObject().equals(mcObj)){
				JMenuItem linkCreateMenu = new JMenuItem();
				linkCreateMenu.setText("Create Additional Link");
				linkCreateMenu.setActionCommand("createadditionallink");
				linkCreateMenu.addActionListener(this);
				
				this.contextMenu.add(linkCreateMenu);
			}
		}
	}

	public void actionPerformed(final ActionEvent e) {
		if (ltree == null)
			return;
		TreePath pathTarget = ltree.getPathForLocation(xPoint, yPoint);
		final McTreeNode node = (McTreeNode) pathTarget.getLastPathComponent();
		if (node == null)
			return;

		if (e.getActionCommand().equals("partition")) {
			/*
			 * MartController ds =
			 * (MartController)node.getObject().getWrapper(); DsTablesDialog
			 * dsDialog = new DsTablesDialog(ds);
			 * if(dsDialog.getPartitionColumn()==null) return; boolean result =
			 * node.getPartitionValue(dsDialog.getPartitionColumn(),dsDialog.
			 * getPartitionColumn()); if(result) {
			 * ltree.getModel().nodeStructureChanged(node);
			 * McGuiUtils.refreshGui(node); }else {
			 * JOptionPane.showMessageDialog(null,
			 * "no data in selected column"); }
			 */
		} else if (e.getActionCommand().equals("addContainer")) {
			if (node.addContainer((MartConfigTree)ltree)) {
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
			}
		} else if (e.getActionCommand().equals("remove importable")
				|| e.getActionCommand().equals("remove exportable")) {
			/*
			 * JDomNodeAdapter parent = (JDomNodeAdapter)node.getParent();
			 * node.removePortable();
			 * ltree.getModel().nodeStructureChanged(parent);
			 * McGuiUtils.refreshGui(parent);
			 */
		} else if (e.getActionCommand().equals("update")) {
			/*
			 * McTreeNode parent = (McTreeNode)node.getParent(); //UpdateMart
			 * updateMart = new UpdateMart(node); if(node.updateMart())
			 * ltree.getModel().nodeStructureChanged(parent);
			 */
			// ((McViewSchema)McViews.getInstance().getView(IdwViewType.SCHEMA)).updateSchema(node);
		} else if (e.getActionCommand().equals("addLinkedDataSet")) {
			// node.addLinkedDataSet();
		} else if (e.getActionCommand().equals("hide")) {
			// e is checkboxitem
			boolean checked = ((JCheckBoxMenuItem) e.getSource()).isSelected();
			MartConfigTree mcTree = (MartConfigTree) ltree;
			TreePath[] treePaths = mcTree.getSelectionPaths();
			List<McTreeNode> nodeList = new ArrayList<McTreeNode>();
			for (TreePath tp : treePaths) {
				nodeList.add((McTreeNode) tp.getLastPathComponent());
			}
			nodeHandler.requestHideNodes(mcTree, nodeList, checked);
			// traverse the tree to hide all the containers that has atts and
			// filters hidden
			mcTree.checkAllHiddenContainers();
			/*
			if(nodeList.size() > 0)
				Validation.validateObject(nodeList.get(0).getObject().getParentConfig());
			*/
		} else if (e.getActionCommand().equals("hide4user")) {
			// node.hideForCurrentUser();
			// McGuiUtils.refreshGui(node);
		} else if (e.getActionCommand().equals("groupDataSet")) {
			/*
			 * MartController ds =
			 * (MartController)node.getObject().getWrapper(); GroupDsDialog
			 * groupDialog = new GroupDsDialog(ds);
			 * if(groupDialog.getSelectedDataSets()!=null) { McTreeNode
			 * registryNode = (McTreeNode)node.getParent();
			 * registryNode.groupMarts(groupDialog.getMartName(),
			 * groupDialog.getSelectedDataSets(),true);
			 * ltree.getModel().nodeStructureChanged(registryNode); }
			 */
		} else if (e.getActionCommand().equals("partition2")) {
			/*
			 * MartController ds =
			 * (MartController)McUtils.getObjectFromElement(node.getNode());
			 * GroupPtDialog groupDialog = new GroupPtDialog(ds);
			 * if(groupDialog.getGroupPartitionTable()!=null) {
			 * ds.addPartition(groupDialog.getGroupPartitionTable());
			 * McGuiUtils.refreshGui(node); }
			 */
		} else if (e.getActionCommand().equals("dropDown")) {
			nodeHandler.requestDropDown((MartConfigTree)ltree, node);
		} else if (e.getActionCommand().equals("groupDimensionTable")) {

		} else if (e.getActionCommand().equals("copyAttribute")
				|| e.getActionCommand().equals("copyFilter")) {
			TreePath[] treePaths = ltree.getSelectionPaths();
			List<McTreeNode> nodeList = new ArrayList<McTreeNode>();
			for (TreePath tp : treePaths) {
				nodeList.add((McTreeNode) tp.getLastPathComponent());
			}
			node.copy(nodeList);
		} else if (e.getActionCommand().equals("cutAttribute")
				|| e.getActionCommand().equals("cutFilter")) {
			TreePath[] treePaths = ltree.getSelectionPaths();
			List<McTreeNode> nodeList = new ArrayList<McTreeNode>();
			for (TreePath tp : treePaths) {
				nodeList.add((McTreeNode) tp.getLastPathComponent());
			}
			node.cut(nodeList);
		} else if (e.getActionCommand().equals("paste")) {
			node.paste((MartConfigTree) ltree);
			((DefaultTreeModel) ltree.getModel()).nodeStructureChanged(node);
			McEventBus.getInstance().fire(McEventProperty.REFRESH_OTHERTREE.toString(), ltree);
		} else if (e.getActionCommand().equals("addGuiContainer")) {
			if (node.addGuiContainer())
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
		} else if (e.getActionCommand().equals("addMartPointer")) {
			if (node.addMartPointer())
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
		} else if (e.getActionCommand().equals("addProcessor")) {
			if (node.addProcessor())
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
		} else if (e.getActionCommand().equals("makeDefaultProcessor")) {
			node.setDefaultProcessor();
		} else if (e.getActionCommand().equals("moveup")) {
			((MartConfigTree) ltree).getModel().moveNode(node, -1);
		} else if (e.getActionCommand().equals("movedown")) {
			((MartConfigTree) ltree).getModel().moveNode(node, 1);
		} else if (e.getActionCommand().equals("orderdatasetbyname")) {
			// ((MartPointer)node.getObject()).orderDatasetList(new
			// NameComparator());
			// node.synchronizeNode();
		} else if (e.getActionCommand().equals("orderdatasetbydisplayname")) {
			// ((MartPointer)node.getObject()).orderDatasetList(new
			// DisplayNameComparator());
			// node.synchronizeNode();
		} else if (e.getActionCommand().equals("createLink")) {
			// if(node.createLink())
			// ltree.getModel().nodeStructureChanged(node);
		} else if (e.getActionCommand().equals("createAttributeList")) {
			if (node.createAttributeList()) {
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
				McEventBus.getInstance().fire(McEventProperty.REFRESH_OTHERTREE.toString(), ltree);
			}
		} else if (e.getActionCommand().equals("createFilterList")) {
			if (node.createFilterList()) {
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
				McEventBus.getInstance().fire(McEventProperty.REFRESH_OTHERTREE.toString(), ltree);
			}
		} else if (e.getActionCommand().equals("createAttribute")) {
			if (node.createAttribute()) {
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
				McEventBus.getInstance().fire(McEventProperty.REFRESH_OTHERTREE.toString(), ltree);
			}
		} else if (e.getActionCommand().equals("deleteGuiContainer")) {
			int n = JOptionPane.showConfirmDialog(null, "delete?", "Question",
					JOptionPane.YES_NO_OPTION);
			if (n != 0)
				return;

			McTreeNode parent = (McTreeNode) node.getParent();
			// cannot remove the root
			if (!(parent.getObject() instanceof GuiContainer))
				return;
			//McEventBus.getInstance().fire(McEventProperty.REFRESH_SOURCE.toString(), null);
			GuiContainer currentGC = (GuiContainer) node.getObject();
			((GuiContainer) parent.getObject()).removeGuiContainer(currentGC);
			node.removeFromParent();
			((DefaultTreeModel) ltree.getModel()).nodeStructureChanged(parent);
		} else if (e.getActionCommand().equals("addDataset")) {
			node.addDsInPartition();
			((DefaultTreeModel) ltree.getModel()).nodeStructureChanged(node);
		} else if (e.getActionCommand().equals("mergeFilters")) {
			boolean check = true;
			JDialog parent = ((MartConfigTree)ltree).getParentDialog();
			TreePath[] treePaths = ltree.getSelectionPaths();
			List<Filter> nodeList = new ArrayList<Filter>();
			for (TreePath tp : treePaths) {
				McTreeNode mcNode = (McTreeNode) tp.getLastPathComponent();
				if (!(mcNode.getObject() instanceof Filter)) {
					check = false;
					JOptionPane.showMessageDialog(parent,
							"cannot merge non filter nodes");
					return;
				}
				Filter filter = (Filter) mcNode.getObject();
				if (filter.getFilterType() == FilterType.SINGLESELECTBOOLEAN
						|| filter.getFilterType() == FilterType.SINGLESELECT) {
				} else {
					check = false;
					JOptionPane
							.showMessageDialog(parent,
									"type has to be singleselectboolean or singleselect");
					return;
				}
				nodeList.add(filter);
			}
			if (nodeList.size() < 2) {
				check = false;
				JOptionPane
						.showMessageDialog(parent, "select at least 2 filters");
				return;
			}
			if (check)
				if (node.mergeFilters(parent,nodeList)) {
					McTreeNode synNode = (McTreeNode) node.getParent()
							.getParent();
					synNode.synchronizeNode();
					((DefaultTreeModel) ltree.getModel())
							.nodeStructureChanged(synNode);
				}
		} else if (e.getActionCommand().equals("createFilterFromAttribute")) {
			McTreeNode parent = (McTreeNode) node.getParent();
			MartConfigTree mcTree = (MartConfigTree) ltree;
			TreePath[]  seltps = mcTree.getSelectionPaths();
			for(TreePath tp : seltps){
				McTreeNode selNode = (McTreeNode)tp.getLastPathComponent();
				
				this.nodeHandler.requestCreateFilterFromAttribute(mcTree,selNode);
			}
			McTreeNode root = (McTreeNode)ltree.getModel().getRoot();
			root.synchronizeNode();
			((DefaultTreeModel) ltree.getModel()).reload();
			ltree.setSelectionPaths(seltps);
			//((DefaultTreeModel) ltree.getModel()).nodeStructureChanged(parent);
		} else if (e.getActionCommand().equals("addDatasetInMartPointer")) {

		} else if (e.getActionCommand().equals("delete")) {
			JDialog parent = ((MartConfigTree)ltree).getParentDialog();
			TreePath[] treePaths = ltree.getSelectionPaths();
			if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(parent, "delete?")){
				nodeHandler.requestDeleteNodes((MartConfigTree) ltree, treePaths);
			}
		} else if (e.getActionCommand().equals("references")) {
			nodeHandler.requestObjectReferences((MartConfigTree) ltree, node);
		} else if (e.getActionCommand().equals("duplicatemart")) {
			/*
			 * try { Mart mart = nodeHandler.requestDuplicateMart(ltree, node);
			 * ((Mart)node.getObject()).getMartRegistry().addMart(mart);
			 * McTreeNode martNode = new McTreeNode(mart);
			 * ((McTreeNode)node.getParent()).add(martNode);
			 * martNode.synchronizeNode();
			 * ltree.getModel().nodeStructureChanged(node.getParent()); //copy
			 * mart options
			 * nodeHandler.requestDuplicateMartOption(node.getObject
			 * ().getName(), mart.getName()); } catch (FunctionalException e1) {
			 * // TODO Auto-generated catch block e1.printStackTrace(); }
			 */
		} else if (e.getActionCommand().equals("rename")) {
			nodeHandler.requestObjectRename((MartConfigTree) ltree, node);
		} else if (e.getActionCommand().equals("duplicatemartoptions")) {
			nodeHandler.requestDuplicateMartOption(node.getObject().getName(),
					node.getObject().getName() + "_copy");
		} else if (e.getActionCommand().equals("donaiveattribute")) {
			nodeHandler.requestNewAttribute((DatasetColumn) node
					.getUserObject());
		} else if (e.getActionCommand().equals("createlink")) {
			/*
			 * boolean check = true; TreePath[] treePaths =
			 * ltree.getSelectionPaths(); List<Filter> filterList = new
			 * ArrayList<Filter>(); for(TreePath tp: treePaths) { McTreeNode
			 * mcNode = (McTreeNode)tp.getLastPathComponent(); //check to see if
			 * they are all attributes or all filters
			 * if(!(mcNode.getUserObject() instanceof Filter)) { check = false;
			 * break; } filterList.add((Filter)mcNode.getUserObject()); }
			 * if(!check) { JOptionPane.showMessageDialog(null,
			 * "select filter only"); return; } new AddLinkDialog(filterList);
			 */
			String s = JOptionPane.showInputDialog("name");
			if (!McUtils.isStringEmpty(s)) {
				Link link = new Link(s);
				link.setObjectStatus(ValidationStatus.INVALID);
				((Config) node.getUserObject()).addLink(link);
				node.synchronizeNode();
				// McTreeNode linkNode = new McTreeNode(link);
				// node.insert(linkNode, 0);
				((DefaultTreeModel) ltree.getModel())
						.nodeStructureChanged(node);
			}
		} else if (e.getActionCommand().equals("setmaster")) {
			boolean checked = ((JCheckBoxMenuItem) e.getSource()).isSelected();
			((Config) node.getUserObject()).setMaster(checked);
		} else if (e.getActionCommand().equals("exportconfig")) {
			nodeHandler.exportConfig(ltree.getParent(),
					(Config) node.getUserObject());
		} else if (e.getActionCommand().equals("exportoptions")) {
			nodeHandler.exportOptions(ltree.getParent(),
					(Config) node.getUserObject());
		} else if (e.getActionCommand().equals("schemaeditor")) {
			final Mart mart = (Mart) node.getUserObject();
			new SchemaDialog(mart);
		} else if (e.getActionCommand().equals("removemart")) {
			JDialog parent = ((MartConfigTree)ltree).getParentDialog();
			int n = JOptionPane.showConfirmDialog(parent, "delete?", "Question",
					JOptionPane.YES_NO_OPTION);
			if (n != 0)
				return;

			final Mart mart = (Mart) node.getUserObject();
			List<MartConfiguratorObject> references = mart.getReferences();
			for (MartConfiguratorObject mcObj : references) {
				MartPointer mp = (MartPointer) mcObj;
				mp.getGuiContainer().removeMartPointer(mp);
			}
			MartRegistry mr = mart.getMartRegistry();
			mr.removeMart(mart);
			// update mcviewportal and mcviewsource
			((McViewSourceGroup) McViews.getInstance().getView(
					IdwViewType.SOURCEGROUP)).showSource(mr);
			((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL))
					.refreshRootPane();

		}  else if (e.getActionCommand().equals("schemastructure")) {
		
			final Mart mart = (Mart) node.getUserObject();
			new TableDialog(((MartConfigTree)ltree).getParentDialog(), mart);
		} else if (e.getActionCommand().equals("importconfig")) {
			final Mart mart = (Mart) node.getUserObject();
			ObjectController oc = new ObjectController();
			oc.importConfig(mart);
		} else if (e.getActionCommand().indexOf("partition.p") >= 0) {
			final Mart mart = (Mart) node.getUserObject();
			String name = e.getActionCommand();
			int index = name.indexOf(".");
			String ptName = name.substring(index + 1);
			PartitionTable pt = mart.getPartitionTableByName(ptName);
			if (pt != null)
				new PartitionTableDialog(((MartConfigTree)ltree).getParentDialog(),pt, 0, -1);
		} else if (e.getActionCommand().equals("searchfromsource")) {
			Mart mart = (Mart) node.getUserObject();
			boolean b = mart.searchFromTarget();
			mart.setSearchFromTarget(!b);
		} else if (e.getActionCommand().equals("createLinkIndex")) {
			ObjectController oc = new ObjectController();
			Mart mart = (Mart) node.getUserObject();
			oc.requestCreateLinkIndex(mart);
		} else if (e.getActionCommand().equals("linkManagement")) {
			Mart mart = (Mart) node.getUserObject();
			
			LinkManagementDialog lmDialog = new LinkManagementDialog(((MartConfigTree)ltree).getParentDialog(), mart);

		} else if (e.getActionCommand().equals("removeLinkIndex")) {
			ObjectController oc = new ObjectController();
			Mart mart = (Mart) node.getUserObject();
			oc.requestRemoveLinkIndex(mart);
		} else if(e.getActionCommand().equals("showoptions")) {
			
		} else if(e.getActionCommand().equals("createadditionallink")){
			McTreeNode root = (McTreeNode)ltree.getModel().getRoot();
			Mart smart = (Mart) root.getObject();
			TreePath treePath = ltree.getSelectionPath();
			McTreeNode tnode = (McTreeNode) treePath.getLastPathComponent();
			Mart tmart = (Mart) tnode.getObject();
			MartConfigTree mcTree = (MartConfigTree) ltree;
			LinkDatasetDialog ldd =new LinkDatasetDialog(mcTree.getParentDialog(),smart,tmart);
			//add link node to the tree
			Link link = ldd.getSourceLink();
			if(link != null){
				tnode.add(new McTreeNode(link));				
				((DefaultTreeModel) ltree.getModel()).reload(tnode);
			}
		}
		
		//after any change to the tree node (configs) do a validation to that config
		if(ltree instanceof MartConfigTree) {
			MartConfigTree mcTree = (MartConfigTree) ltree;
			mcTree.validate();
		}
	}

}
