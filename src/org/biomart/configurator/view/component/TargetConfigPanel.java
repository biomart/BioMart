package org.biomart.configurator.view.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreePath;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.controller.PartitionReferenceController;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.jdomUtils.McViewsFilter;
import org.biomart.configurator.jdomUtils.XMLTreeCellRenderer;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.McViewType;
import org.biomart.configurator.view.AttributeTable;
import org.biomart.configurator.view.MartConfigTargetTree;
import org.biomart.configurator.view.component.container.SubPartitionPanel;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;

public class TargetConfigPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Config config;
	private JComboBox dsCB;
	private MartConfigTargetTree configTree;
	private JDialog parent;

	public TargetConfigPanel(Config config, JDialog parent) {
		this.config = config;
		this.parent = parent;
		this.init();
		
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		
		//config tree;
		McTreeNode node = new McTreeNode(config);
		boolean readonly = config.isReadOnly();
		configTree = new MartConfigTargetTree(node,!readonly,true,!readonly,!readonly,parent);
		ToolTipManager.sharedInstance().registerComponent(configTree);
		//set filter
		Map<McNodeType, Map<String, String>> filters = McGuiUtils.INSTANCE.getFilterMap(McViewType.CONFIGURATION);
		McViewsFilter filter = new McViewsFilter(filters);
		configTree.getModel().setFilters(filter);
		node.synchronizeNode();
		
        XMLTreeCellRenderer treeCellRenderer = new XMLTreeCellRenderer();
        configTree.setCellRenderer(treeCellRenderer);
        JScrollPane ltreeScrollPane = new JScrollPane(configTree);
        
        AttributeTable attrTable = new AttributeTable(configTree, parent);
        configTree.setAttributeTable(attrTable);
        JScrollPane tableScroll = new JScrollPane(attrTable);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		ltreeScrollPane,tableScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);
       
        JLabel dsLabel = new JLabel("dataset");
		this.dsCB = new JComboBox();
		JPanel toolPanel = new JPanel(new FlowLayout(0,0,this.dsCB.getHeight()+14));		
		JPanel ntoolPanel = new JPanel();
		toolPanel.add(ntoolPanel);
		//if has subpartitions
		if(config.getMart().getPartitionTableList().size()>1) {
			SubPartitionPanel spp = new SubPartitionPanel(config.getMart());		
			toolPanel.add(spp);
			spp.setEnabled(false);
		}
		
		ntoolPanel.add(dsLabel);
		ntoolPanel.add(dsCB);

		dsLabel.setVisible(false);
		dsCB.setVisible(false);
		this.dsCB.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) { 
					selectDataset(e.getItem());
				}
			}		
		});
		//add dataset
		this.dsCB.addItem(Resources.get("ALLDATASETS"));
		for(Dataset ds: this.config.getMart().getDatasetList()) {
			this.dsCB.addItem(ds);
		}

        this.add(toolPanel,BorderLayout.NORTH);
        this.add(splitPane,BorderLayout.CENTER);
		this.setBorder(new TitledBorder(new EtchedBorder()));
		this.setPartitionReferences();
		this.configTree.setSelectionRow(0);
		this.configTree.expandRow(0);
		
		this.configTree.checkAllHiddenContainers();
	}

	public Dataset getSelectedDataset() {
		return (Dataset)dsCB.getSelectedItem();
	}
	
	private void setPartitionReferences() {
		PartitionReferenceController prc = new PartitionReferenceController();
		prc.addPtReferences(config);		
	}
	
	private void selectDataset(Object object) {
		if(object instanceof Dataset) {
			configTree.setSelectedDataset((Dataset)object);
		}else
			configTree.setSelectedDataset(null);
		//update attribute table
		TreePath tp = this.configTree.getSelectionPath();
		configTree.getModel().reload();
		if(tp!=null) 
			configTree.setSelectionPath(tp);
	}
	
	public MartConfigTargetTree getTree() {
		return this.configTree;
	}

	/**
	 * @return the config
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(Config config) {
		this.config = config;
	}
	
}