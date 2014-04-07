/**
 * 
 */
package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.jdomUtils.McTreeModel;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.jdomUtils.McViewsFilter;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.McViewType;
import org.biomart.configurator.view.AttributeTable;
import org.biomart.configurator.view.MartConfigSourceTree;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.idwViews.LinkManageTreeCellRenderer;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;

import java.awt.Font;

/**
 * @author lyao
 *
 */
public class LinkManagementDialog extends JDialog {
	private Mart selMart;
	private MartConfigTree configTree;
	
	JLabel leftLabel;
	JLabel rightLabel;

	public LinkManagementDialog(JDialog parent,Mart mart){
		super(parent);
		this.selMart = mart;
		init();
		this.setModal(true);
		this.pack();
		this.setTitle("Link Management Dialog");
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	public void init(){
		this.setContentPane(this.getContentPane());
	}
	
	private void emptyLinkLabel(Mart mart){
		leftLabel.setText(mart.getDisplayName());
		rightLabel.setText("");
		this.pack();
	}
	private void generateLinkLabel(Link link){
		StringBuilder linkName = getLabelFromLink(link);
		leftLabel.setText(linkName.toString());
		Link otherLink = McUtils.getOtherLink(link);
		if(otherLink != null){
			rightLabel.setText(getLabelFromLink(otherLink).toString());			
		}else{
			rightLabel.setText("");
		}
		this.pack();
	}
	public JPanel getContentPane(){
		JPanel content = new JPanel(new BorderLayout());
		//data source selection panel
		JPanel dataSourcePanel = new JPanel();
		JLabel dataSource = new JLabel("Data source");
		JComboBox dataSourceCB = new JComboBox();
		dataSourcePanel.add(dataSource);
		dataSourcePanel.add(dataSourceCB);
		dataSourceCB.removeAllItems();
        for(Mart mt: this.selMart.getMartRegistry().getMartList()) {
        	if(mt.getMasterConfig()!= null && !mt.getMasterConfig().getLinkList().isEmpty())
        		dataSourceCB.addItem(mt);
        }
        dataSourceCB.setSelectedItem(this.selMart);
        dataSourceCB.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				if (e.getStateChange() == ItemEvent.SELECTED) { 
					selectSource(e.getItem());
				}
			}
        	
        });
		content.add(dataSourcePanel,BorderLayout.NORTH);
		
		//tree panel for links and database
		this.configTree = new MartConfigTree(null,true,true,true,false,this);
		this.configTree.addTreeSelectionListener(new TreeSelectionListener(){

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				// TODO Auto-generated method stub
				Object lpc = e.getPath().getLastPathComponent();
				if (lpc instanceof McTreeNode) {
					Object obj = ((McTreeNode) lpc).getUserObject();
					if(obj instanceof Link){
						generateLinkLabel((Link)obj);							
					}else if(obj instanceof Mart){
						emptyLinkLabel((Mart)obj);
					}
				}
			}
			
		});
		this.generateLinkTree(this.selMart);
		ToolTipManager.sharedInstance().registerComponent(configTree);
		LinkManageTreeCellRenderer treeCellRenderer = new LinkManageTreeCellRenderer();
        configTree.setCellRenderer(treeCellRenderer);
        configTree.setRowHeight(treeCellRenderer.getIconHeight());
/*		Map<McNodeType, Map<String, String>> filters = McGuiUtils.INSTANCE.getFilterMap(McViewType.CONFIGURATION);
		McViewsFilter filter = new McViewsFilter(filters);
		configTree.getModel().setFilter(filter);*/
        JScrollPane ltreeScrollPane = new JScrollPane(configTree);        
        content.add(ltreeScrollPane,BorderLayout.CENTER);
        
        //attribute panel for links
        //left label and left table
        leftLabel = new JLabel();
        AttributeTable leftAttrTable = new AttributeTable(configTree,this);
        configTree.setAttributeTable(leftAttrTable);
        JScrollPane leftTableScroll = new JScrollPane(leftAttrTable);
        JPanel leftTablePanel = new JPanel();
        leftTablePanel.setLayout(new BoxLayout(leftTablePanel,BoxLayout.Y_AXIS));
        leftTablePanel.add(leftLabel);
        leftTablePanel.add(leftTableScroll);
        //right label and right table
        rightLabel = new JLabel();
        AttributeTable rightAttrTable = new AttributeTable(configTree,this);
        configTree.setLinkAttributeTable(rightAttrTable);
        JScrollPane rightTableScroll = new JScrollPane(rightAttrTable);
        JPanel rightTablePanel = new JPanel();
        rightTablePanel.setLayout(new BoxLayout(rightTablePanel,BoxLayout.Y_AXIS));
        rightTablePanel.add(rightLabel);
        rightTablePanel.add(rightTableScroll);
        //set label font
        Font font = new Font("Serif",Font.BOLD, 16);
		leftLabel.setFont(font);
		rightLabel.setFont(font);
		
        JPanel linkAttPanel = new JPanel();
        linkAttPanel.add(leftTablePanel);
        linkAttPanel.add(rightTablePanel);
        content.add(linkAttPanel,BorderLayout.SOUTH);
		
		return content;
	}
	
	protected void selectSource(Object item) {
		// TODO Auto-generated method stub
		if(item instanceof Mart){
			this.generateLinkTree((Mart) item);
		}
	}

	private void generateLinkTree(Mart mart){
		McTreeModel model = this.configTree.getModel();		
		McTreeNode rootNode = this.createNodes(mart);
        model.setRoot(rootNode);
        //rootNode.synchronizeNode();
	}
	
	private McTreeNode createNodes(Mart mart){
		List<Link> links = mart.getMasterConfig().getLinkList();
		//create root with the current selected mart
		McTreeNode root = new McTreeNode(mart);
		//create other mart that the current selected mart link to
		for(Link link : links){
			if(link.getPointedMart() == null)
				continue;
			McTreeNode pMartNode = this.getNodeFromLink(link, root);
			pMartNode.add(new McTreeNode(link));
		}
		return root;
	}
	
	public McTreeNode getNodeFromLink(Link link, McTreeNode root){
		
		for(int i =0;i<root.getChildCount();i++){
			McTreeNode node = (McTreeNode)root.getChildAt(i);
			if(node.getUserObject().equals(link.getPointedMart())){
				return (McTreeNode)node;
			}
		}
		McTreeNode newNode = new McTreeNode(link.getPointedMart());
		root.add(newNode);
		return newNode;
	}

	/**
	 * @param obj
	 * @return
	 */
	private StringBuilder getLabelFromLink(Object obj) {
		StringBuilder linkName = new StringBuilder();
		linkName.append(((Link) obj).getParentConfig().getMart().getDisplayName());
		linkName.append(" => ");
		linkName.append(((Link) obj).getPointedMart().getDisplayName());
		return linkName;
	}
}
