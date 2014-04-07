package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.TreePath;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.resources.Log;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.DefaultListTransferHandler;
import org.biomart.configurator.controller.LinkListTransferHandler;
import org.biomart.configurator.controller.ListTransferHandler;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.test.MartConfigurator;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.view.MartConfigTargetTree;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.component.SourceConfigPanel;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

import com.hp.hpl.jena.sparql.function.library.date;

public class LinkDatasetDialog extends JDialog implements  ActionListener, MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JList leftDsList;
	private DefaultListModel modelleft;
	private JList rightDsList;
	private DefaultListModel modelright;
	private JList leftSelList;
	private JList rightSelList;
	private JPanel centerPanel;
	
	private Config sourceConfig;
	private Config targetConfig;
	//private MartConfigTree sourceTree;
	//private MartConfigTree targetTree;
	private List<McTreeNode> nodeList;
	private McTreeNode newParentNode;
	
	private List<Attribute> sourceAttributeList;
	private List<Attribute> targetAttributeList;
	
	private boolean isCanceled = false;
	
	private Link sourceLink = null;
	private Link targetLink = null;
	
	private SourceConfigPanel ssp1;
	private SourceConfigPanel ssp2;
	
	public LinkDatasetDialog(JDialog parent, MartConfiguratorObject sourceConfig, MartConfiguratorObject targetConfig, 
			MartConfigTree sourceTree, MartConfigTree targetTree, List<McTreeNode> nodeList,McTreeNode newParentNode){
		super(parent);
		this.sourceConfig = (Config)sourceConfig;
		this.targetConfig = (Config)targetConfig;
		//this.sourceTree = sourceTree;
		//this.targetTree = targetTree;
		this.nodeList = nodeList;
		this.newParentNode = newParentNode;
		init();
	}
	
	public LinkDatasetDialog(JDialog parent, Mart smart, Mart tmart){
		super(parent);
		this.sourceConfig = smart.getMasterConfig();
		this.targetConfig = tmart.getMasterConfig();
		this.nodeList = new ArrayList<McTreeNode>();
		init();
	}
	
	private void init() {		
		this.add(this.createDatasetPanel());
		this.setTitle("Dataset Link Dialog");
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
	
	private void removeTreeFilter(){
		final MartConfigTree tree1 = ssp1.getConfigTree();
		tree1.getModel().removeFilter(McNodeType.FILTER);
		tree1.getModel().removeFilter("attributelist");
		tree1.getModel().removeFilter("filterlist");
		tree1.getModel().removeFilter("hasFilter");
		
		final MartConfigTree tree2 = ssp2.getConfigTree();
		tree2.getModel().removeFilter(McNodeType.FILTER);
		tree2.getModel().removeFilter("attributelist");
		tree2.getModel().removeFilter("filterlist");
		tree2.getModel().removeFilter("hasFilter");
	}
	
	public JPanel createDatasetPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JPanel centerPanel = new JPanel();
		JPanel buttonPanel = new JPanel();

		JButton addButton = new JButton("OK");
		addButton.setActionCommand("ok");
		addButton.addActionListener(this);
		
		JButton removeButton = new JButton("Cancel");
		removeButton.setActionCommand("cancel");
		removeButton.addActionListener(this);
		
		buttonPanel.add(removeButton);
		buttonPanel.add(addButton);
		
		modelleft = new DefaultListModel();
		modelright = new DefaultListModel();
		DefaultListModel modelSelLeft = new DefaultListModel();
		DefaultListModel modelSelRight = new DefaultListModel();
		
		final ProgressDialog progressMonitor = ProgressDialog.getInstance(this);
		
		final SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {

			@Override
			protected void done() {
				// Close the progress dialog.
				progressMonitor.setVisible(false);
				progressMonitor.stop();
			}

			@Override
			protected Void doInBackground() throws Exception {
				try {
					progressMonitor.setStatus("Loading all attributes and datasets...");
					LoadAttributesWithFilters();
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}finally {
					progressMonitor.setVisible(false);
				}
				return null;
			}
		};
		
		worker.execute();
		progressMonitor.start("Loading all attributes and datasets...");

		/*
		this.leftDsList = new JList(modelleft){
			public String getToolTipText(MouseEvent evt) {
		        // Get item index
		        int index = locationToIndex(evt.getPoint());

		        // Get item
		        Attribute item = (Attribute)getModel().getElementAt(index);

		        // Return the tool tip text
		        return item.getName();
		    }
		};
		this.leftDsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.leftDsList.addMouseListener(this);
		
		this.rightDsList = new JList(modelright){
			public String getToolTipText(MouseEvent evt) {
		        // Get item index
		        int index = locationToIndex(evt.getPoint());

		        // Get item
		        Attribute item = (Attribute)getModel().getElementAt(index);
		        

		        // Return the tool tip text
		        return item.getName();
		    }
		};
		this.rightDsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.rightDsList.addMouseListener(this);
		*/
		this.leftSelList = new JList(modelSelLeft);		
		this.rightSelList = new JList(modelSelRight);		
		
		JLabel leftDsName = new JLabel(this.sourceConfig.getDisplayName(),JLabel.CENTER);
		JLabel rightDsName = new JLabel(this.targetConfig.getDisplayName(),JLabel.CENTER);
		
		JPanel leftDsPanel = new JPanel();
		JPanel rightDsPanel = new JPanel();
		
		ssp1 = new SourceConfigPanel(sourceConfig,false);
		ssp1.getSourceLabel().setVisible(false);
		ssp1.getSourceCB().setVisible(false);
		
		//add mouse listener to config tree
		final MartConfigTree tree1 = ssp1.getConfigTree();
		tree1.getModel().addFilter(McNodeType.FILTER, null);
		tree1.getModel().addFilter("attributelist");
		tree1.getModel().addFilter("filterlist");
		tree1.getModel().addFilter("hasFilter");
		
		tree1.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				int selRow = tree1.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree1.getPathForLocation(e.getX(), e.getY());
				if(e.getClickCount() == 2){
					McTreeNode treeNode = (McTreeNode)selPath.getLastPathComponent();
					if(treeNode.getObject() instanceof Attribute){
						Attribute attribute = (Attribute)treeNode.getObject();
						//addAttributeToTargetList(attribute);
						DefaultListModel model = (DefaultListModel)leftSelList.getModel();
						if(attribute.hasReferenceFilters())
							model.addElement(attribute);
						else
							JOptionPane.showMessageDialog(null, "please select an attribute that has a filter!");
						
					}
				}
			}
		});
		
		//JScrollPane sp1 = new JScrollPane(this.leftDsList);
		//ssp1.setPreferredSize(new Dimension(400, 400));
		
		ssp2 = new SourceConfigPanel(targetConfig,false);
		ssp2.getSourceLabel().setVisible(false);
		ssp2.getSourceCB().setVisible(false);
		
		//add mouse listener to config tree
		final MartConfigTree tree2 = ssp2.getConfigTree();
		tree2.getModel().addFilter(McNodeType.FILTER, null);
		tree1.getModel().addFilter("attributelist");
		tree2.getModel().addFilter("filterlist");
		tree2.getModel().addFilter("hasFilter");
		
		tree2.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				int selRow = tree2.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree2.getPathForLocation(e.getX(), e.getY());
				if(e.getClickCount() == 2){
					McTreeNode treeNode = (McTreeNode)selPath.getLastPathComponent();
					if(treeNode.getObject() instanceof Attribute){
						Attribute attribute = (Attribute)treeNode.getObject();
						//addAttributeToTargetList(attribute);
						DefaultListModel model = (DefaultListModel)rightSelList.getModel();
						if(attribute.hasReferenceFilters())
							model.addElement(attribute);
						else
							JOptionPane.showMessageDialog(null, "please select an attribute that has a filter!");
					}
				}
			}
		});
		
		//JScrollPane sp2 = new JScrollPane(this.rightDsList);
		//ssp2.setPreferredSize(new Dimension(400, 400));
		
		JScrollPane sp3 = new JScrollPane(this.leftSelList);
		//sp3.setPreferredSize(new Dimension(100, 80));
		this.leftSelList.setDragEnabled(true);
		this.leftSelList.setDropMode(DropMode.INSERT);
		this.leftSelList.setTransferHandler(new LinkListTransferHandler());
		this.leftSelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.leftSelList.addMouseListener(this);
		
		JScrollPane sp4 = new JScrollPane(this.rightSelList);
		//sp4.setPreferredSize(new Dimension(100, 80));
		this.rightSelList.setDragEnabled(true);
		this.rightSelList.setDropMode(DropMode.INSERT);
		this.rightSelList.setTransferHandler(new LinkListTransferHandler());
		this.rightSelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.rightSelList.addMouseListener(this);
		
		leftDsPanel.setLayout(new BoxLayout(leftDsPanel,BoxLayout.Y_AXIS));
		leftDsPanel.add(leftDsName);
		leftDsPanel.add(ssp1);
		JLabel selLeftLabel = new JLabel("Selected attributes in "+this.sourceConfig.getDisplayName());
		leftDsPanel.add(selLeftLabel);
		leftDsPanel.add(sp3);
		
		rightDsPanel.setLayout(new BoxLayout(rightDsPanel,BoxLayout.Y_AXIS));
		rightDsPanel.add(rightDsName);
		rightDsPanel.add(ssp2);
		JLabel selRightLabel = new JLabel("Selected attributes in "+this.targetConfig.getDisplayName());
		rightDsPanel.add(selRightLabel);
		rightDsPanel.add(sp4);
	
				
		panel.add(leftDsPanel,BorderLayout.WEST);
		panel.add(rightDsPanel,BorderLayout.EAST);
		panel.add(centerPanel,BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		panel.setOpaque(true);
		return panel;
	}

	protected void LoadAttributesWithFilters() {
		// TODO Auto-generated method stub
		//sourceMap = new HashMap<Integer, Attribute>();
		sourceAttributeList = new ArrayList<Attribute>();
		targetAttributeList = new ArrayList<Attribute>();
		
		for(Filter filter : this.sourceConfig.getFilters(null, true, true))
		{
			if(filter.getFilterType() != FilterType.BOOLEAN &&
					filter.getAttribute() != null){
				sourceAttributeList.add(filter.getAttribute());
			}
		}
		
		//sourceAttributeList = this.sourceConfig.getAttributes(null, true, true);
		//sort sourceAttributeList first
		Collections.sort(sourceAttributeList);
		
		
		//create a map from Jlist to attributelist
		for(Attribute attribute : sourceAttributeList){
			modelleft.addElement(attribute);
		}
		
		//targetMap = new HashMap<Integer, Attribute>();
		for(Filter filter: this.targetConfig.getFilters(null, true, true)){
			if(filter.getFilterType() != FilterType.BOOLEAN &&
					filter.getAttribute() != null){
				targetAttributeList.add(filter.getAttribute());
			}
		}
		
		//targetAttributeList = this.targetConfig.getAttributes(null, true, true);
		//sort target attribute list first
		Collections.sort(targetAttributeList);
		
		
		//create a map from Jlist to attributelist	
		for(Attribute attribute : targetAttributeList){
			modelright.addElement(attribute);
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getActionCommand() == "ok"){
			int numSourceAttr = this.leftSelList.getModel().getSize();
			int numTargetAttr = this.rightSelList.getModel().getSize();
			if( numSourceAttr != numTargetAttr)
			{
				JOptionPane.showMessageDialog(this, "source selected are not matched with target set!");
				return;
			}else if(numSourceAttr == 0)
			{
				JOptionPane.showMessageDialog(this, "attribute lists are empty!");
				return;
			}
			Link slink = McUtils.getLink(sourceConfig, targetConfig);
			Link dlink = McUtils.getLink(targetConfig, sourceConfig);
			if(slink != null && dlink != null){
				createLink(numSourceAttr, slink,dlink);
			}else{
				//check if dataset matching is 1 to 1 or multiple first
				List<Dataset> sourceDataset = this.sourceConfig.getMart().getDatasetList();
				List<Dataset> targetDataset = this.targetConfig.getMart().getDatasetList();
				//if(sourceDataset.size() >1 && targetDataset.size() >1) {
				//set visible false for the drag and drop in matchdatasetdialog
				this.setVisible(false);
				MatchDatasetDialog mdd = new MatchDatasetDialog(this, this.sourceConfig, this.targetConfig);
				//this.setVisible(true);
				//this.repaint();
				if(!mdd.isOk())
					return;
				//create link based on attribute list
				createLink(numSourceAttr,mdd.getCreatedTargetCol(), mdd.getCreatedSourceCol());
			}
			
			this.createPointers();
			
			//this.refreshTree();
			this.setVisible(false);
			
			removeTreeFilter();
		}
		else if(e.getActionCommand() == "cancel"){
			this.setCanceled(true);
			this.setVisible(false);
			removeTreeFilter();
		}
	}

	private void createLink(int numSourceAttr, Link slink, Link dlink) {
		List<Dataset> sourceDataset = this.sourceConfig.getMart().getDatasetList();
		List<Dataset> targetDataset = this.targetConfig.getMart().getDatasetList();
		
		String sBaseName = McUtils.getLinkName(sourceConfig, targetConfig);
		sBaseName = McUtils.getUniqueLinkName(sourceConfig, sBaseName);
		sourceLink = new Link(sBaseName);
		String tBaseName = McUtils.getLinkName(targetConfig, sourceConfig);
		tBaseName = McUtils.getUniqueLinkName(targetConfig, tBaseName);
		targetLink = new Link(tBaseName);
		
		
		sourceLink.setPointerMart(this.targetConfig.getMart());
		sourceLink.setPointedConfig(targetConfig.getMart().getMasterConfig());		
		
		targetLink.setPointerMart(this.sourceConfig.getMart());
		targetLink.setPointedConfig(this.sourceConfig.getMart().getMasterConfig());
		//create pointed dataset based on source dataset size and target dataset size
		//if(sourceDataset.size() >1 && targetDataset.size() >1) {
		//create pointer for pointed dataset
		sourceLink.setPointedDataset(slink.getPointedDataset());
		targetLink.setPointedDataset(dlink.getPointedDataset());
		/*}else{
			//create names for pointed dataset
			StringBuilder sDatasetName = new StringBuilder();
			for(Dataset ds:targetDataset){
				sDatasetName.append(ds.getName());
				if(targetDataset.indexOf(ds) != targetDataset.size()-1)
					sDatasetName.append(',');
			}
			sourceLink.setPointedDataset(sDatasetName.toString());
			
			StringBuilder tDatasetName = new StringBuilder();
			for(Dataset ds:sourceDataset){
				tDatasetName.append(ds.getName());
				if(sourceDataset.indexOf(ds) != sourceDataset.size()-1)
					tDatasetName.append(',');
			}
			targetLink.setPointedDataset(tDatasetName.toString());
		}*/
		
		for(int i=0;i<numSourceAttr;i++){
			Attribute sourceAttr = (Attribute)this.leftSelList.getModel().getElementAt(i);
			Attribute targetAttr = (Attribute)this.rightSelList.getModel().getElementAt(i);
			if(sourceAttr != null && targetAttr != null){
				//add source attr and target filter to source link
				sourceLink.addAttribute(sourceAttr);
				List<Filter> srefs = targetAttr.getReferenceFilters();
				for(MartConfiguratorObject ref : srefs){						
					sourceLink.addFilter((Filter)ref);
					break;//only add the first one for now
				}
				//add source filter and target attr to target link
				targetLink.addAttribute(targetAttr);
				List<Filter> trefs = sourceAttr.getReferenceFilters();
				for(MartConfiguratorObject ref : trefs){						
					targetLink.addFilter((Filter)ref);
					break;
				}				
			}			
		}
		//add link to all the configs
		try{
			List<Config> sconfigs = this.sourceConfig.getMart().getConfigList();
			for(Config config : sconfigs){
				org.jdom.Element e = sourceLink.generateXml();
				Link link = new Link(e);
				config.addLink(link);
				link.synchronizedFromXML();
			}
			sourceLink.setParent(this.sourceConfig);
			List<Config> tconfigs = this.targetConfig.getMart().getConfigList();
			for(Config config : tconfigs){
				org.jdom.Element e = targetLink.generateXml();
				Link link = new Link(e);
				config.addLink(link);
				link.synchronizedFromXML();
			}
			targetLink.setParent(this.targetConfig);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * @param numSourceAttr
	 */
	private void createLink(int numSourceAttr, int sourceCol, int targetCol) {
		List<Dataset> sourceDataset = this.sourceConfig.getMart().getDatasetList();
		List<Dataset> targetDataset = this.targetConfig.getMart().getDatasetList();
		
		String sBaseName = McUtils.getLinkName(sourceConfig, targetConfig);
		sBaseName = McUtils.getUniqueLinkName(sourceConfig, sBaseName);
		sourceLink = new Link(sBaseName);
		String tBaseName = McUtils.getLinkName(targetConfig, sourceConfig);
		tBaseName = McUtils.getUniqueLinkName(targetConfig, tBaseName);
		targetLink = new Link(tBaseName);
		
		
		sourceLink.setPointerMart(this.targetConfig.getMart());
		sourceLink.setPointedConfig(targetConfig.getMart().getMasterConfig());		
		
		targetLink.setPointerMart(this.sourceConfig.getMart());
		targetLink.setPointedConfig(this.sourceConfig.getMart().getMasterConfig());
		//create pointed dataset based on source dataset size and target dataset size
		//if(sourceDataset.size() >1 && targetDataset.size() >1) {
		//create pointer for pointed dataset
		sourceLink.setPointedDataset("(p0c"+Integer.toString(targetCol)+")");
		targetLink.setPointedDataset("(p0c"+Integer.toString(sourceCol)+")");
		/*}else{
			//create names for pointed dataset
			StringBuilder sDatasetName = new StringBuilder();
			for(Dataset ds:targetDataset){
				sDatasetName.append(ds.getName());
				if(targetDataset.indexOf(ds) != targetDataset.size()-1)
					sDatasetName.append(',');
			}
			sourceLink.setPointedDataset(sDatasetName.toString());
			
			StringBuilder tDatasetName = new StringBuilder();
			for(Dataset ds:sourceDataset){
				tDatasetName.append(ds.getName());
				if(sourceDataset.indexOf(ds) != sourceDataset.size()-1)
					tDatasetName.append(',');
			}
			targetLink.setPointedDataset(tDatasetName.toString());
		}*/
		
		for(int i=0;i<numSourceAttr;i++){
			Attribute sourceAttr = (Attribute)this.leftSelList.getModel().getElementAt(i);
			Attribute targetAttr = (Attribute)this.rightSelList.getModel().getElementAt(i);
			if(sourceAttr != null && targetAttr != null){
				//add source attr and target filter to source link
				sourceLink.addAttribute(sourceAttr);
				List<Filter> srefs = targetAttr.getReferenceFilters();
				for(MartConfiguratorObject ref : srefs){						
					sourceLink.addFilter((Filter)ref);
					break;//only add the first one for now
				}
				//add source filter and target attr to target link
				targetLink.addAttribute(targetAttr);
				List<Filter> trefs = sourceAttr.getReferenceFilters();
				for(MartConfiguratorObject ref : trefs){						
					targetLink.addFilter((Filter)ref);
					break;
				}				
			}			
		}
		//add link to all the configs
		try{
			List<Config> sconfigs = this.sourceConfig.getMart().getConfigList();
			for(Config config : sconfigs){
				org.jdom.Element e = sourceLink.generateXml();
				Link link = new Link(e);
				config.addLink(link);
				link.synchronizedFromXML();
			}
			sourceLink.setParent(this.sourceConfig);
			List<Config> tconfigs = this.targetConfig.getMart().getConfigList();
			for(Config config : tconfigs){
				org.jdom.Element e = targetLink.generateXml();
				Link link = new Link(e);
				config.addLink(link);
				link.synchronizedFromXML();
			}
			targetLink.setParent(this.targetConfig);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		// whenever user double clicks
		if(e.getClickCount() == 2) {
			if(e.getSource() instanceof JList){
				JList list = (JList)e.getSource();
				if(list.equals(this.leftDsList)){
					Attribute value =  (Attribute)this.leftDsList.getSelectedValue();
					DefaultListModel model = (DefaultListModel)this.leftSelList.getModel();
					if(value.hasReferenceFilters())
						model.addElement(value);
					else
						JOptionPane.showMessageDialog(this, "please select an attribute that has a filter!");
				}else if(list.equals(this.leftSelList)){
					Object value =  this.leftSelList.getSelectedValue();
					DefaultListModel model = (DefaultListModel)this.leftSelList.getModel();
					model.removeElement(value);
				}else if(list.equals(this.rightDsList)){
					Attribute value =  (Attribute)this.rightDsList.getSelectedValue();
					DefaultListModel model = (DefaultListModel)this.rightSelList.getModel();
					if(value.hasReferenceFilters())
						model.addElement(value);
					else
						JOptionPane.showMessageDialog(this, "please select an attribute that has a filter!");
				}else if(list.equals(this.rightSelList)){
					Object value =  this.rightSelList.getSelectedValue();
					DefaultListModel model = (DefaultListModel)this.rightSelList.getModel();
					model.removeElement(value);
				}
			}
		}
		this.repaint();
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see java.awt.Window#paint(java.awt.Graphics)
	 */
	
	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		super.paint(g);
		Rectangle cellRect = this.leftSelList.getCellBounds(0, 0);
		DefaultListModel leftModel = (DefaultListModel)this.leftSelList.getModel();
		DefaultListModel rightModel = (DefaultListModel)this.rightSelList.getModel();
		for(int i=0;i<Math.min(leftModel.getSize(), rightModel.getSize());i++){
			if(cellRect != null){
				Point p1 = this.leftSelList.getLocationOnScreen();
				p1.translate(this.leftSelList.getWidth(), 0);
				Point p2 = this.rightSelList.getLocationOnScreen();
				Point p = this.getLocationOnScreen();
				//shift drawing lines to the middle of the list row
				p1.translate(0, cellRect.height/2 + i * cellRect.height);
				p2.translate(0, cellRect.height/2 + i * cellRect.height);
				
				p1.x = p1.x - p.x;
				p1.y = p1.y - p.y;
				p2.x = p2.x - p.x;
				p2.y = p2.y - p.y;
				g.setColor(Color.green);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}
		
	}

	/**
	 * @return the isCanceled
	 */
	public boolean isCanceled() {
		return isCanceled;
	}

	/**
	 * @param isCanceled the isCanceled to set
	 */
	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}
	
	public boolean createPointers(){
		//create a copy the source to the target, replace attribute with pointers
		
		String baseName = McUtils.getLinkName(targetConfig, sourceConfig);//draggedNodes.get(0).getObject().getName()+"_"+sconfig.getName()+"_"+tconfig.getName();
		//Link link = targetConfig.getLinkByName(baseName);
		Link link = McUtils.getLink(targetConfig, sourceConfig);
		String pointedDataset = "";
		if(link == null)
			return false;
		if(link.getPointedDataset().contains(",")){
			//if there is multiple dataset, then open the dataset selection dialog
			
			String[] datasets = link.getPointedDataset().split(",");
			
			DatasetSelectionDialog dsd = new DatasetSelectionDialog(this, datasets);
			pointedDataset = dsd.getSelectedDataset();
		}else{
			pointedDataset = link.getPointedDataset();
		}
		
		for(McTreeNode node: nodeList) {
			if(node.getUserObject() instanceof Attribute) {
				Attribute attr = (Attribute)node.getUserObject();
				
				/*if(targetConfig.getMart().getMasterConfig().hasPointedAttribute(attr))
					continue;*/
				if(targetConfig.hasPointedAttribute(attr))
					continue;
				baseName = McUtils.getUniqueAttributeName(targetConfig, attr.getName());
				//baseName = McGuiUtils.INSTANCE.getPointedAttributeName(attr);
	    		Attribute attPointer = new Attribute(baseName,(Attribute)node.getObject());
	    		//use the old displayname
	    		attPointer.setDisplayName(node.getObject().getDisplayName());
	    		((Container)newParentNode.getObject()).addAttribute(attPointer);
	    		attPointer.setPointedConfigName(sourceConfig.getName());
	    		//set pointed dataset
	    		if(link != null)
	    			attPointer.addPointedDataset(pointedDataset);
	    			//attPointer.setPointedDatasetName(link.getPointedDataset());
	    		
	    		attPointer.synchronizedFromXML();
	    		
			}else if(node.getUserObject() instanceof Filter) {
				Filter filter = (Filter)node.getUserObject();
				/*
				if(targetConfig.getMart().getMasterConfig().hasPointedFilter(filter))
					continue;*/
				if(targetConfig.hasPointedFilter(filter))
					continue;
				//get unique name
				baseName = McGuiUtils.INSTANCE.getUniqueFilterName(targetConfig, filter.getName());
	    		//baseName = McGuiUtils.INSTANCE.getPointedFilterName(filter);
				
	    		Filter filPointer = new Filter(baseName,node.getObject().getName(),link.getPointedDataset());
	    		//filPointer.setPointedInfo(draggedNodes.get(0).getObject().getName(), 
	    			//	leftDs.getName(), config.getName(), config.getMart().getName());
	    		filPointer.setDisplayName(node.getObject().getDisplayName());
	    		filPointer.setPointedElement((Filter)node.getObject());
	    		((Container)newParentNode.getObject()).addFilter(filPointer);
	    		filPointer.setPointedConfigName(sourceConfig.getName());
	    		if(link != null)
	    			filPointer.addPointedDataset(pointedDataset);
	    			//filPointer.setPointedDatasetName(link.getPointedDataset());
	    		
	    		filPointer.synchronizedFromXML();
	    		
			}else if(node.getObject() instanceof Container) {
				try {
					boolean rename = false;
					if(targetConfig.getContainerByName(((MartConfiguratorObject)node.getUserObject()).getName())!=null) {
						int n = JOptionPane.showConfirmDialog(
							    this,
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
						newFilter.setPointedConfigName(sourceConfig.getName());
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
		return true;
	}
	/*
	public void refreshTree(){
		//Synchronize derived config tree to master config tree for newly added nodes
    	targetConfig.syncWithMasterconfig();
    	//synchronize root
		McTreeNode root = (McTreeNode)this.targetTree.getModel().getRoot();
		root.synchronizeNode();
		this.targetTree.getModel().reload(root);
    	//newParentNode.synchronizeNode();
		
    	
		TreePath treePath = new TreePath(newParentNode.getPath());
		targetTree.expandPath(treePath);
		targetTree.scrollPathToVisible(treePath);
		targetTree.setSelectionPath(treePath);
		//McGuiUtils.refreshGui(newParentNode);
		
	}*/

	/**
	 * @return the sourceLink
	 */
	public Link getSourceLink() {
		return sourceLink;
	}

	/**
	 * @return the targetLink
	 */
	public Link getTargetLink() {
		return targetLink;
	}
}