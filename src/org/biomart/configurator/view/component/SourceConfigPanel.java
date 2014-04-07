package org.biomart.configurator.view.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.tree.DefaultTreeModel;
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
import org.biomart.configurator.view.MartConfigSourceTree;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;

public class SourceConfigPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Config targetConfig;
	private boolean enableProperty;
	//private JComboBox configCB;
	private JLabel sourceLabel;
	private JComboBox sourceCB;
	private JComboBox datasetCB;
	private MartConfigSourceTree configTree;
	private JDialog parent;
	
	public SourceConfigPanel(Config config, JDialog parent) {
		this.targetConfig = config;
		this.enableProperty = true;
		this.parent = parent;
		init();
	}
	
	public SourceConfigPanel(Config config, boolean enableProperty){
		this.targetConfig = config;
		this.enableProperty = enableProperty;
		init();
		selectConfig(this.targetConfig);
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		
		this.configTree = new MartConfigSourceTree(null,true,true,true,true,parent);
		ToolTipManager.sharedInstance().registerComponent(configTree);
		XMLTreeCellRenderer treeCellRenderer = new XMLTreeCellRenderer();
        configTree.setCellRenderer(treeCellRenderer);
		Map<McNodeType, Map<String, String>> filters = McGuiUtils.INSTANCE.getFilterMap(McViewType.CONFIGURATION);
		McViewsFilter filter = new McViewsFilter(filters);
		configTree.getModel().setFilters(filter);
        JScrollPane ltreeScrollPane = new JScrollPane(configTree);
        
        AttributeTable attrTable = new AttributeTable(configTree,parent);
        configTree.setAttributeTable(attrTable);
        JScrollPane tableScroll = new JScrollPane(attrTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		ltreeScrollPane,tableScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);

        
		JPanel configPanel = new JPanel();
		this.sourceLabel = new JLabel("Source:");
		this.sourceCB = new JComboBox();
		this.sourceCB.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				if(e.getStateChange() == ItemEvent.SELECTED) {
					Mart mart = (Mart)sourceCB.getSelectedItem();
					Config config = mart.getMasterConfig();
					selectConfig(config);
				}
			}
			
		});
		
		configPanel.setLayout(new BorderLayout());
		
		configPanel.add(sourceLabel,BorderLayout.WEST);
		configPanel.add(sourceCB,BorderLayout.CENTER);
		
/*		this.configCB = new JComboBox();
		this.configCB.setVisible(false);
		this.configCB.setRenderer(new ConfigComboBoxRenderer());
		this.configCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							Config config = (Config)SourceConfigPanel.this.configCB.getSelectedItem();
							selectConfig(config);
							PartitionReferenceController prc = new PartitionReferenceController();
							prc.addPtReferences(config);
							
							
						}
						
					});
				}
			}  			
  		});*/
		
		JLabel dsLabel = new JLabel("Dataset:");
		this.datasetCB = new JComboBox();
		this.datasetCB.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) { 
					selectDataset(e.getItem());
				}				
			}			
		});
		
		
	
		//add martList
		MartRegistry registry = this.targetConfig.getMart().getMartRegistry();
		for(Mart mart: registry.getMartList()) {
			this.sourceCB.addItem(mart);
//			Config config = mart.getMasterConfig();
//			if(config!=null)
//				configCB.addItem(config);
/*			for(Config config: mart.getConfigList()) {
				if(!config.equals(targetConfig))
					configCB.addItem(config);
			}*/
		}
		
		
		this.add(configPanel,BorderLayout.NORTH);

		if(this.enableProperty){
			this.add(splitPane,BorderLayout.CENTER);
		}else{
			this.add(ltreeScrollPane,BorderLayout.CENTER);
		}
		this.setBorder(new TitledBorder(new EtchedBorder()));
		this.configTree.setSelectionRow(0);
		this.configTree.expandRow(0);
		
		//after add config check all hidden
		this.configTree.checkAllHiddenContainers();
	}
	
	private void selectConfig(Config config) {
		DefaultTreeModel model = (DefaultTreeModel)this.configTree.getModel();
		McTreeNode configNode = new McTreeNode(config);
        model.setRoot(configNode);
        configNode.synchronizeNode();
        //set dataset
        this.datasetCB.removeAllItems();
        this.datasetCB.addItem(Resources.get("ALLDATASETS"));
        for(Dataset ds: config.getMart().getDatasetList()) {
        	this.datasetCB.addItem(ds);
        }
		PartitionReferenceController prc = new PartitionReferenceController();
		prc.addPtReferences(config);		
	}
	
	public Dataset getSelectedDataset() {
		Object selItem = this.datasetCB.getSelectedItem();
		if(selItem instanceof Dataset){
			return (Dataset)this.datasetCB.getSelectedItem();
		}else
			return null;
	}

	class ConfigComboBoxRenderer extends BasicComboBoxRenderer {
	    /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(JList list, Object value,
	        int index, boolean isSelected, boolean cellHasFocus) {
	      if (isSelected) {
	        setBackground(list.getSelectionBackground());
	        setForeground(list.getSelectionForeground());
	        if (-1 < index) {
	          list.setToolTipText(((Config)value).getName());
	        }
	      } else {
	        setBackground(list.getBackground());
	        setForeground(list.getForeground());
	      }
	      setFont(list.getFont());
	      setText((value == null) ? "" : value.toString());
	      return this;
	    }
	  }

	/**
	 * @return the configTree
	 */
	public MartConfigSourceTree getConfigTree() {
		return configTree;
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

	/**
	 * @return the targetConfig
	 */
	public Config getTargetConfig() {
		return targetConfig;
	}

	/**
	 * @param targetConfig the targetConfig to set
	 */
	public void setTargetConfig(Config targetConfig) {
		this.targetConfig = targetConfig;
	}

	public void updateSourceConfig(Config config){
		this.sourceCB.setSelectedItem(config.getMart());
	}
	
	public Config getSourceConfig() {
		McTreeNode root = (McTreeNode)this.configTree.getModel().getRoot();
		return (Config)root.getUserObject();
	}

	/**
	 * @return the sourceLabel
	 */
	public JLabel getSourceLabel() {
		return sourceLabel;
	}

	/**
	 * @return the sourceCB
	 */
	public JComboBox getSourceCB() {
		return sourceCB;
	}
}

