package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.tree.TreePath;

import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.McEvent;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.McEventListener;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.McEventHandler;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.controller.TreeNodeHandler;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.component.SourceConfigPanel;
import org.biomart.configurator.view.component.TargetConfigPanel;
import org.biomart.configurator.view.menu.McMenus;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.SearchInfoObject;

public class ConfigDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Config config;
	private JButton showconfigButton;
	private JButton searchButton;
	private JButton saveButton;
	private JButton rdfButton;
	private boolean showsourceConfig = false;
	private JSplitPane splitPane;
	private TargetConfigPanel tconfigPanel;
	private SourceConfigPanel sconfigPanel;
	private JCheckBox hidemaskedcb;
	private JCheckBox synccb;
	//icons
	private ImageIcon showIcon;
	private ImageIcon hideIcon;
	
	public ConfigDialog(Config config) {
		this.config = config;
		init();
		this.setModal(true);
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    this.setSize(screenSize.width-100,screenSize.height - 100);
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
		this.setTitle("Config Dialog");
	}
	
	private void init() {
		JPanel toolBarPanel = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		JPanel checkboxPanel = new JPanel();
		
		synccb = new JCheckBox("sync change");
		synccb.setSelected("1".equals(Settings.getProperty("syncchange")));
		synccb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isSelected = ((JCheckBox)e.getSource()).isSelected();
				Settings.setProperty("syncchange", isSelected?"1":"0");	
			}			
		});
		//set sync to invisible since it is not used anymore
		synccb.setVisible(false);
		
		this.hidemaskedcb = new JCheckBox("hide masked component");
		//this.hidemaskedcb.setSelected("1".equals(Settings.getProperty("hidemaskedcomponent")));
		Settings.setProperty("hidemaskedcomponent", "0");
		this.hidemaskedcb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				hideMaskedComponent();
			}
			
		});
		checkboxPanel.add(synccb);
		//checkboxPanel.add(hidemaskedcb);
		
		toolBarPanel.add(checkboxPanel,BorderLayout.EAST);
		toolBarPanel.add(toolBar,BorderLayout.CENTER);
				
		ImageIcon searchIcon = McUtils.createImageIcon("images/search.gif");
		this.searchButton = new JButton(searchIcon);
		this.searchButton.setText("Search");
		this.searchButton.setActionCommand("search");
		this.searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				search();
			}			
		});

		ImageIcon saveIcon = McUtils.createImageIcon("images/save.gif");
		this.saveButton = new JButton(saveIcon);
		this.saveButton.setActionCommand("save");
		this.saveButton.addActionListener(McMenus.getInstance());
		//this.showconfigButton = new JButton(McUtils.createImageIcon("images/showsourceconfig.gif"));
		showIcon = McUtils.createImageIcon("images/forward_nav.gif");
		hideIcon = McUtils.createImageIcon("images/backward_nav.gif");
		double scaleRate = (double)searchIcon.getIconHeight() / showIcon.getIconHeight();
		showIcon = McUtils.scale(showIcon.getImage(), scaleRate, this);
		hideIcon = McUtils.scale(hideIcon.getImage(), scaleRate, this);
		this.showconfigButton = new JButton(showIcon);
		this.showconfigButton.setText("Import from sources");		
		this.showconfigButton.setActionCommand(Resources.get("SHOWHIDECONFIG"));
		this.showconfigButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showSourceConfig(!showsourceConfig);
			}
		});
		
		this.rdfButton = new JButton(McUtils.createImageIcon("images/rdf.gif"));
        this.rdfButton.setText("Generate RDF-Ontology");
		this.rdfButton.setActionCommand("rdf");
		this.rdfButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				regenerateRDF();
			}			
		});
		toolBar.add(this.showconfigButton);
		toolBar.add(this.searchButton);
		//toolBar.add(this.rdfButton);
		toolBar.add(this.saveButton);
		
		//draw the gui
		JPanel configPanel = new JPanel(new BorderLayout());
		//right panel
		tconfigPanel = new TargetConfigPanel(config, this);
        //left panel
		sconfigPanel = new SourceConfigPanel(config, this);
		//set source tree
		tconfigPanel.getTree().setSourceTree(sconfigPanel.getConfigTree());
		//set target tree
		sconfigPanel.getConfigTree().setTargetTree(tconfigPanel.getTree());
		Dimension d = new Dimension(0,0);
		tconfigPanel.setMinimumSize(d);
		sconfigPanel.setMinimumSize(d);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,sconfigPanel,tconfigPanel);
		splitPane.setDividerLocation(0);

        configPanel.add(splitPane,BorderLayout.CENTER);
        configPanel.add(toolBarPanel, BorderLayout.NORTH);
        this.add(configPanel);
        this.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e)
          {
      		McEventBus.getInstance().removeListener(McEventProperty.SYNC_NEW_LIST.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.SYNC_UPDATE_LIST.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.SYNC_MASTER_REMOVE.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.SYNC_NEW_PSEUDO.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_SOURCE.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_TARGET.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_SOURCETABLE.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_TARGETTABLE.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_OTHERTREE.toString(), this);
    		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_OTHERTABLE.toString(), this);
    		ConfigDialog.this.dispose();
          }
        });

		McEventBus.getInstance().addListener(McEventProperty.SYNC_NEW_LIST.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.SYNC_UPDATE_LIST.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.SYNC_MASTER_REMOVE.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.SYNC_NEW_PSEUDO.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REFRESH_SOURCE.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REFRESH_TARGET.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REFRESH_SOURCETABLE.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REFRESH_TARGETTABLE.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REFRESH_OTHERTREE.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REFRESH_OTHERTABLE.toString(), this);
	}

	public void showSourceConfig(boolean b) {
		if (b)
			this.splitPane.setDividerLocation(0.5);
		else
			this.splitPane.setDividerLocation(0);
		this.showsourceConfig = b;
		this.updateButtonIcon();
	}
	
	/*
	 * TODO it will not work when the user drag the splitbar
	 */
	private void updateButtonIcon(){
		if(showsourceConfig) {
			this.showconfigButton.setIcon(this.hideIcon);
			this.showconfigButton.setText("Hide sources");
			//update source panel config to the master config of target panel
			Config tConfig = this.tconfigPanel.getConfig();
			this.sconfigPanel.setTargetConfig(tConfig.getMart().getMasterConfig());
			this.sconfigPanel.updateSourceConfig(tConfig);
		}
		else {
			this.showconfigButton.setIcon(this.showIcon);
			this.showconfigButton.setText("Import from sources");
		}
	}

	private void search() {
		SearchComponentDialog dialog = new SearchComponentDialog(this);
		if(dialog.getSearchObj()!=null) {
			SearchInfoObject sObj = dialog.getSearchObj();
			TreeNodeHandler tnh = new TreeNodeHandler();
			MartConfiguratorObject mcObj = null;
			JPanel p = null;
			if(sObj.getScopeStr().equals(Resources.get("SCOPEENTIREPORTAL"))) {
				mcObj = config.getMart().getMartRegistry();
				p = this.sconfigPanel;
			}else if(sObj.getScopeStr().equals(Resources.get("SCOPECURRENTSOURCE"))) {
				mcObj = this.sconfigPanel.getSourceConfig();
				p = this.sconfigPanel;
			}else {
				mcObj = config;
				p = this.tconfigPanel;
			}
			tnh.requestSearchNode(mcObj,p,
					sObj.getTypeStr(),sObj.getValue(), sObj.getSearchName(),
					sObj.isCaseSensitive(), sObj.isLike(), sObj.getScopeStr(), this);
			//dialog.dispose();
		}
	}
	
	private void hideMaskedComponent() {
		Settings.setProperty("hidemaskedcomponent", this.hidemaskedcb.isSelected()?"1":"0");
		MartConfigTree tTree = tconfigPanel.getTree();
		McTreeNode tRoot = (McTreeNode)tTree.getModel().getRoot();
//		root.synchronizeNode();
    	tTree.getModel().nodeStructureChanged(tRoot);
    	
    	MartConfigTree sTree = sconfigPanel.getConfigTree();
    	McTreeNode sRoot = (McTreeNode)sTree.getModel().getRoot();
//		root.synchronizeNode();
    	sTree.getModel().nodeStructureChanged(sRoot);
	}
	
	public boolean isSourceConfigVisible() {
		return this.showsourceConfig;
	}
	
	@McEventListener
	public void update(McEvent<?> event) {
		String property = event.getProperty();
		McEventHandler eventHandler = new McEventHandler();
		if(property.equals(McEventProperty.REFRESH_SOURCE.toString())) {
			MartConfigTree tree = this.sconfigPanel.getConfigTree();
			eventHandler.refreshTree(tree);
		} else if(property.equals(McEventProperty.REFRESH_TARGET.toString())) {
			MartConfigTree tree = this.tconfigPanel.getTree();
			eventHandler.refreshTree(tree);
		} else if(property.equals(McEventProperty.REFRESH_SOURCETABLE.toString())) {
			MartConfigTree tree = this.sconfigPanel.getConfigTree();
			eventHandler.refreshAttributeTable(tree);
		} else if(property.equals(McEventProperty.REFRESH_TARGETTABLE.toString())) {
			MartConfigTree tree = this.tconfigPanel.getTree();
			eventHandler.refreshAttributeTable(tree);
		} else if(property.equals(McEventProperty.REFRESH_OTHERTREE.toString())) {
			MartConfigTree tree = (MartConfigTree)event.getSource();
			McTreeNode rootNode = (McTreeNode)tree.getModel().getRoot();
			Config config = (Config)rootNode.getUserObject();
			MartConfigTree treeRefreshed = null;
			if(config.isMasterConfig()) {
				treeRefreshed = this.sconfigPanel.getConfigTree();
			}
			else {
				treeRefreshed = this.tconfigPanel.getTree();
			}
			eventHandler.refreshTree(treeRefreshed);

		} else if(property.equals(McEventProperty.REFRESH_OTHERTABLE.toString())) {
			
		} else if(property.equals(McEventProperty.SYNC_NEW_LIST.toString())) {
			MartConfiguratorObject mcObj = (MartConfiguratorObject)event.getSource();
			Config sourceConfig = mcObj.getParentConfig();
			if(mcObj instanceof Attribute) {
				eventHandler.syncAttributeListNew((Attribute)mcObj,sourceConfig);
			} //end of if attribute
			else if(mcObj instanceof Filter) {
				eventHandler.syncFilterListNew((Filter)mcObj, sourceConfig);
			}
		} else if(property.equals(McEventProperty.SYNC_NEW_PSEUDO.toString())) {
			MartConfiguratorObject mcObj = (MartConfiguratorObject)event.getSource();
			Config sourceConfig = mcObj.getParentConfig();
			eventHandler.syncPseudoAttribute((Attribute)mcObj, sourceConfig);
		} else if(property.equals(McEventProperty.SYNC_UPDATE_LIST.toString())) {
			MartConfiguratorObject mcObj = (MartConfiguratorObject)event.getSource();
			Config sourceConfig = mcObj.getParentConfig();
			if(mcObj instanceof Attribute) {
				eventHandler.syncAttributeListUpdate((Attribute)mcObj, sourceConfig);
			}
			else if(mcObj instanceof Filter) {
				eventHandler.syncFilterListUpdate((Filter)mcObj, sourceConfig);
			}
		}
	}
	
	private void regenerateRDF() {

	}

}