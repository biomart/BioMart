package org.biomart.configurator.view;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.jdomUtils.McTreeModel;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.jdom.Element;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.view.AttributeTable;
import org.biomart.configurator.view.menu.TreeNodeMenuConstructor;
import org.biomart.configurator.model.TreeAttributeTableModel;
import org.biomart.configurator.model.XMLAttributeTableModel;
import org.biomart.configurator.controller.DefaultTreeTransferHandler;
import org.biomart.configurator.model.PartitionTableModel;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;



public class MartConfigTree extends JTree implements TreeExpansionListener,
	TreeWillExpandListener, Autoscroll, TableModelListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
	private Insets scrollInsets = defaultScrollInsets;
	
	private AttributeTable attributeTable;
	private AttributeTable linkAttributeTable;
	private JPopupMenu contextMenu;
	private McTreeModel model;
	private Dataset selectedDataset;
	private boolean tableEditable;
	
	private boolean acceptDrop;
	private JDialog parent;
	
	public JDialog getParentDialog(){
		return this.parent;
	}

	public void autoscroll(Point location) {
		JScrollPane scroller = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		if (scroller != null) {
			JScrollBar hBar = scroller.getHorizontalScrollBar();
			JScrollBar vBar = scroller.getVerticalScrollBar();
			Rectangle r = getVisibleRect();
			if (location.x <= r.x + scrollInsets.left) {
				// Need to scroll left
				hBar.setValue(hBar.getValue() - hBar.getUnitIncrement(-1));
			}
			if (location.y <= r.y + scrollInsets.top) {
				// Need to scroll up
				vBar.setValue(vBar.getValue() - vBar.getUnitIncrement(-1));
			}
			if (location.x >= r.x + r.width - scrollInsets.right) {
				// Need to scroll right
				hBar.setValue(hBar.getValue() + hBar.getUnitIncrement(1));
			}
			if (location.y >= r.y + r.height - scrollInsets.bottom) {
				// Need to scroll down
				vBar.setValue(vBar.getValue() + vBar.getUnitIncrement(1));
			}
		}
	}

	public Insets getAutoscrollInsets() {
		Rectangle r = getVisibleRect();
		Dimension size = getSize();
		Insets i =
			new Insets(
				r.y + scrollInsets.top,
				r.x + scrollInsets.left,
				size.height - r.y - r.height + scrollInsets.bottom,
				size.width - r.x - r.width + scrollInsets.right);
		return i;
	}
	
	public MartConfigTree(McTreeNode root, boolean acceptDrop, boolean enableSelectionEvent,
			boolean enableMouse, boolean editTable, JDialog parent) {
		this.parent = parent;
		
		this.model = new McTreeModel(root);
		this.tableEditable = editTable;
		this.setModel(model);
		this.putClientProperty("JTree.lineStyle", "Angled");

		this.setEditable(false);
		this.setShowsRootHandles(true);
		this.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		this.acceptDrop = acceptDrop;
		this.setDragEnabled(this.acceptDrop);
		this.setDropMode(DropMode.ON_OR_INSERT);
		this.setTransferHandler(new DefaultTreeTransferHandler(this, DnDConstants.ACTION_COPY_OR_MOVE));
		
		if(root != null)
			root.setParentDialog(parent);
		
		if(enableSelectionEvent)
			this.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					TreePath oldPath = e.getOldLeadSelectionPath(); 
					if(e.getOldLeadSelectionPath() != null){
						McTreeNode oldnode = (McTreeNode)e.getOldLeadSelectionPath().getLastPathComponent();
						if(attributeTable.getCellEditor() != null){
							DefaultCellEditor tce = (DefaultCellEditor) attributeTable.getCellEditor();
							Component c = tce.getComponent();
							int row = attributeTable.getSelectedRow();
							int col = 1;
							if(c instanceof JTextField){
								JTextField jtf  = (JTextField)c;
								String value = jtf.getText();
								((XMLAttributeTableModel) attributeTable.getModel()).setValue(oldnode.getObject(), value, "", row);
							}
							
							
							
						}
					}
					Object lpc = e.getPath().getLastPathComponent();
					if (lpc instanceof McTreeNode) {
						updateAttributeTable((McTreeNode)lpc);
						if(linkAttributeTable != null)
							updateLinkAttributeTable((McTreeNode)lpc);
					}
				}
			});
		
		if(enableMouse)
			this.addMouseListener (new MouseAdapter ()  {
	            public void mousePressed (MouseEvent e)  {	            	
	                if (e.isPopupTrigger ()  &&  e.getClickCount () == 1)  {
	                    doPopup (e);
	                }
	            }
	
	            public void mouseReleased(MouseEvent e)  {	            	
	                if (e.isPopupTrigger ()  &&  e.getClickCount () == 1)  {
	                    doPopup (e);
	                }
	            }
			});
	}

	private void doPopup(MouseEvent e) {
		TreePath path  = this.getPathForLocation(e.getX(),e.getY());
		if(path == null )
			return;
		//is path selected
		if(!this.isPathSelected(path)) 
			return;
		McTreeNode node = (McTreeNode) path.getLastPathComponent();
		if(node == null) 
			return;

		TreeNodeMenuConstructor cmc = TreeNodeMenuConstructor.getInstance();
		contextMenu = cmc.getContextMenu(node.getObject(),this, e.getX(), e.getY());
		if (contextMenu!=null)
			contextMenu.show(this, e.getX(), e.getY());
	}
	
	public McTreeModel getModel() {
		return this.model;
	}
		
	//FIXME: this code should be handled by model, and should not hard code for the tag
	public void updateAttributeTable(McTreeNode treeNode) {
		TreeAttributeTableModel tModel = new TreeAttributeTableModel(this,treeNode,this.selectedDataset, this.tableEditable);
		this.attributeTable.setModel(tModel);
		this.attributeTable.setContextMenuEnv(treeNode.getObject());
	}
	
	public void updateLinkAttributeTable(McTreeNode treeNode) {
		if(treeNode.getUserObject() instanceof Link){
			Link link = (Link)treeNode.getUserObject();
			Config sconfig = link.getParentConfig().getMart().getMasterConfig();
			Config tconfig = link.getPointedMart().getMasterConfig();
			Link otherlink = McUtils.getOtherLink(link);//McUtils.getLink(tconfig, sconfig);
			if(otherlink == null){
				this.linkAttributeTable.setModel(new DefaultTableModel());
				return;
			}
			//McTreeNode linkNode = McGuiUtils.INSTANCE.findTreeNodeRecursively((McTreeNode)treeNode.getRoot(),otherlink);
			XMLAttributeTableModel tModel = new XMLAttributeTableModel(otherlink,this.selectedDataset, this.tableEditable);
			this.linkAttributeTable.setModel(tModel);
			this.linkAttributeTable.setContextMenuEnv(otherlink);
		}else{
			this.linkAttributeTable.setModel(new DefaultTableModel());			
		}
	}
	
	public void setAttributeTable(AttributeTable attributeTable) {
		this.attributeTable = attributeTable;
	}
	
	public void setLinkAttributeTable(AttributeTable linkAttributeTable){
		this.linkAttributeTable = linkAttributeTable;
	}
	
	public AttributeTable getLinkAttributeTable() {
		return this.linkAttributeTable;
	}

	public AttributeTable getAttributeTable() {
		return attributeTable;
	}
	
	/**
	 * source will change after the call
	 * @param source
	 * @param target
	 * @return
	 */
	public Element getAncestor(Element source, String target) {
		if (source == null) return null;
		boolean found = false;
		boolean hasParent = true;
		while(!found && hasParent) {
			if(source.getName().equals(target)) {
				found = true;
				break;
			}
			source = source.getParentElement();
			if(source==null)
				hasParent = false;
		}
		if(found)
			return source;
		else 
			return null;
	}
	
	public Map<String,PartitionTableModel> getPartitionTable(Element martNode) {
		Map<String,PartitionTableModel> ptMap = new HashMap<String,PartitionTableModel>();
		String partitionTable = Resources.get("PARTITIONTABLE");
		List<Element> ptsList = martNode.getChildren(partitionTable);
		if (ptsList == null)
			return null;
		for(Element pts:ptsList) {
			PartitionTableModel ptm = new PartitionTableModel(pts);
			ptMap.put(ptm.getPartitionTableName(), ptm);
		}
		return ptMap;
	}
	
	public void expandAllNodes() {
		int row = 0;
		while (row < this.getRowCount()) {
			this.expandRow(row);
			row++;
      }
	}

	public void collapseAllNodes() {
		int row = 0;
		while (row < this.getRowCount()) {
			this.collapseRow(row);
			row++;
      }
	}

	public TreePath getPath(McTreeNode node) {
		
		List<McTreeNode> list = new ArrayList<McTreeNode>();
	    
        // Add all nodes to list
        while (node != null && !(node.getObject() instanceof Mart)) {
            list.add(node);
            node = (McTreeNode)node.getParent();
            
        }
        Collections.reverse(list);   
        // Convert array of nodes to TreePath
        return new TreePath(list.toArray());
	}
	
/*	private TreePath findByName(String type, String name, McTreeNode parent) {
		parent.getChildCount();
		McTreeNode node = (McTreeNode)parent.getLastPathComponent(); 
		Object o = node; 
		
		if (o.equals(nodes[depth])) { 
			// If at end, return match 
			if (depth == nodes.length-1) { 
				return parent; 
			} 
			// Traverse children 
			if (node.getChildCount() >= 0) { 
				for (Enumeration e=node.children(); e.hasMoreElements(); ) { 
					TreeNode n = (TreeNode)e.nextElement(); 
					TreePath path = parent.pathByAddingChild(n); 
					TreePath result = find2(tree, path, nodes, depth+1, byName); 
					// Found a match 
					if (result != null) { 
						return result; 
					} 
				} 
			} 
		}
	}*/
	
	/**
	 * hardcoded
	 */
	public int expandNodeToDataSets(String nodelevel, McTreeNode selectedNode) {
		int selectedRow = 0;
		final String[] nodes = {XMLElements.MARTREGISTRY.toString(),
//				Resources.get("DATASET")
				};
		HashSet<String> hs = new HashSet<String>(Arrays.asList(nodes));
		int row = 0;
		while (row < this.getRowCount()) {
			TreePath tp = this.getPathForRow(row);
			McTreeNode node = (McTreeNode)tp.getLastPathComponent();
			if(selectedNode!=null && node.equals(selectedNode))
				selectedRow = row;
			if(node!=null && hs.contains(node.getObject().getName()))
					this.expandRow(row);
			row++;
		}
		return selectedRow;
	}
	
	
	public void treeCollapsed(TreeExpansionEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("collapsed");
	}

	public void treeExpanded(TreeExpansionEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("expanded");
	}

	public void treeWillCollapse(TreeExpansionEvent arg0)
			throws ExpandVetoException {
		// TODO Auto-generated method stub
		System.out.println("collapse");
	}

	public void treeWillExpand(TreeExpansionEvent arg0)
			throws ExpandVetoException {
		// TODO Auto-generated method stub
		System.out.println("expand");
	}

	public void tableChanged(TableModelEvent e) {
		//whenever master config table changes update all trees
		if(e.getSource() instanceof TreeAttributeTableModel) {
			TreeAttributeTableModel sourceModel = (TreeAttributeTableModel) e.getSource();
			McTreeNode sourceNode = sourceModel.getTreeNode();
			if(sourceNode != null && !sourceNode.getObject().getParentConfig().isMasterConfig())
			{
				MartConfiguratorObject mco = sourceNode.getObject();				
				McTreeNode node = McGuiUtils.INSTANCE.findTreeNodeRecursively((McTreeNode)this.model.getRoot(),mco);
				if(node != null){
					node.synchronizeNode();
					this.model.reload(node);
				}
			}
			else if(sourceNode != null && this instanceof MartConfigSourceTree){
				MartConfigTree targetTree = ((MartConfigSourceTree)this).getTargetTree();
				MartConfiguratorObject mco = sourceNode.getObject();
				if(targetTree != null && mco != null){
					McTreeNode node = McGuiUtils.INSTANCE.findTreeNodeRecursively((McTreeNode)targetTree.model.getRoot(),mco);
					if(node != null){
						node.synchronizeNode();
						targetTree.model.reload(node);
					}
				}
			}
		}
/*		if(e.getSource().getClass().getName().equals(XMLAttributeTableModel.class.getName())) {
			int row = e.getFirstRow();
			Object obj = e.getSource();
			XMLAttributeTableModel model = (XMLAttributeTableModel)obj;
			String name = (String)model.getValueAt(row, 0);
			String value = (String)model.getValueAt(row, 1);
			TreePath tp = this.getSelectionPath();
			if(tp == null)
				return;
			JDomNodeAdapter node = (JDomNodeAdapter)tp.getLastPathComponent();
			node.getNode().setAttribute(name, value);
			this.getModel().nodeStructureChanged(node.getParent());
			//update the object also
			Object elementObject = McUtils.getObjectFromElement(node.getNode());
			if(elementObject instanceof ElementController) {
				if(name.equals(Resources.get("DISPLAYNAME"))) {
					((ElementController) elementObject).setDisplayName(value);
				}
			}
				
			McGuiUtils.refreshGui(node);
			this.setSelectionPath(tp);
		} else if(e.getSource().getClass().getName().equals(PtModel.class.getName())) {
			PtModel model = (PtModel)e.getSource();
			int row = e.getFirstRow();
			int col = e.getColumn();
			if(col<=0 || model.getColumnCount()<=col)
				return;
			String ptName = model.getPartitionTableName();
			TreePath tp = this.getSelectionPath();
			if(tp == null)
				return;
			JDomNodeAdapter node = (JDomNodeAdapter)tp.getLastPathComponent();
			//node is a dataset
			JDomNodeAdapter ptNode = new JDomNodeAdapter(JDomUtils.searchElement(node.getNode(), 
					Resources.get("PARTITIONTABLE"), ptName));
			JDomUtils.updatePartitionTable(ptNode.getNode(), row, col, (String)model.getValueAt(row, col));
			this.getModel().nodeStructureChanged(ptNode);
		}
		*/
	}

	public boolean acceptDrop() {
		return this.acceptDrop;
	}

	public void setSelectedDataset(Dataset selectedDataset) {
		this.selectedDataset = selectedDataset;
	}

	public Dataset getSelectedDataset() {
		return selectedDataset;
	}
	
	public void checkAllHiddenContainers(){
		
		McTreeNode rootNode = (McTreeNode)this.model.getRoot();
		if(rootNode != null){
			this.hideAllContainers(rootNode);
		}
		this.model.nodeChanged(rootNode);
	}
	//recursively check all containers if should be hidden
    private boolean hideAllContainers(McTreeNode node){
    	if(node.getObject() instanceof Config ||
    			node.getObject() instanceof Container){
    		int childNum = this.model.getChildCount(node);    		
    		if(childNum == 0)
    			return node.getObject().isHidden();
    		if(node.getObject().isHidden())
    			return node.getObject().isHidden();
    		boolean hideParent = true;
    		for(int i=0;i<childNum;i++){
    			McTreeNode childNode = (McTreeNode)this.model.getChild(node, i);
    			if(!hideAllContainers(childNode)){
    				hideParent = false;
    			}    				
    		}
    		
    		node.getObject().setHideValue(hideParent);
    		
    		return hideParent;
    	}else{
    		return node.getObject().isHidden();
    	}
    }

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		System.out.println("test");
		
	}
	
/*	public void validateObjects(){
		McTreeNode node = (McTreeNode)this.model.getRoot();

		if( node.getUserObject() instanceof Mart){
			Validation.validate(node.getObject());
		}else if(node.getUserObject() instanceof Config){
			Validation.validate(node.getObject());
		}else if(node.getUserObject() instanceof Container){
			Validation.validate(((Container)node.getUserObject()).getParentConfig());
		}else if(node.getUserObject() instanceof Attribute){
			Validation.validate(((Attribute)node.getUserObject()).getParentConfig());
		}else if(node.getUserObject() instanceof Filter){
			Validation.validate(((Filter)node.getUserObject()).getParentConfig());
		}
	}*/
}