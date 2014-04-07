package org.biomart.configurator.view.component.container;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.WrapLayout;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.configurator.view.dnd.ConfigDropTargetListener;
import org.biomart.configurator.view.gui.dialogs.AddConfigDialog;
import org.biomart.configurator.view.gui.dialogs.AddConfigFromMartDialog;
import org.biomart.configurator.view.gui.dialogs.ConfigDialog;
import org.biomart.configurator.view.gui.dialogs.RDFDialog;
import org.biomart.configurator.view.gui.dialogs.ReportAttributesSelectDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.configurator.view.menu.ContextMenuConstructor;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFileChooser;
import org.biomart.objects.objects.RDFClass;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

public class GuiContainerPanel extends JPanel implements MouseListener,ClipboardOwner, PropertyChangeListener , MouseMotionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private GuiContainer gc;
	//private JPanel configPanel;
	private JPanel checkboxPanel;
	private JCheckBox hideCheckBox;
	private JCheckBox listViewCheckBox;
	private JButton addButton;
	private JButton rdfButton;
	private JList configList;
	private ConfigTable configTable;
	private ConfigTableCellRenderer tableRenderer;
	private ConfigListCellRenderer renderer;
	
	public JTable getConfigTable() {
		return configTable;
	}

	public JList getConfigList() {
		return configList;
	}

	public GuiContainerPanel(GuiContainer gc) {
		this.gc = gc;
		init();
		this.addPropertyChangeListener(this);
		this.setDropTarget(new DropTarget(GuiContainerPanel.this, new ConfigDropTargetListener(GuiContainerPanel.this)));
	}

	public GuiContainer getGuiContainer() {
		return this.gc;
	}
	
	public void generateOntology() {
		if(this.getSelectedMPs().size() == 0){
			JOptionPane.showMessageDialog(this, "Please select one or more configs to generate semantics.");
			return;
		}
		RDFDialog dialog = new RDFDialog();
		
		if(dialog.getResult()) {
			ObjectController oc = new ObjectController();
			for( MartPointer mp : this.getSelectedMPs()) {
				Config config = mp.getConfig();
				oc.generateRDF(MartController.getInstance().getMainAttribute(config.getMart()), config,dialog.isPseudoExposed());
			}
		}
	}

    public void loadOntology(Config config) {
        // Purge existing ontology data:
        config.removeAllRDF();

        File file;
        String currentDir = Settings.getProperty("currentOpenDir");
        JFileChooser xmlFileChooser = new JFileChooser();
        xmlFileChooser.setCurrentDirectory(currentDir == null ? new File(".")
                : new File(currentDir));
        if (xmlFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Settings.setProperty("currentOpenDir", xmlFileChooser
                    .getCurrentDirectory().getPath());

            file = xmlFileChooser.getSelectedFile();
        } else
            return;

		Document document = null;
		try {
            // boolean refers to "validate"
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(file);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null,
                "Cannot parse XML. Wrong format perhaps?" +
                    "\n\nSAXBuilder exception says:\n" + e.getMessage(),
                "XML Parsing Error",
                JOptionPane.ERROR_MESSAGE);
            return;
		}

        Element root = document.getRootElement();

        if (root == null) {
			JOptionPane.showMessageDialog(null,
                "The XML files does not seem to have a root element.",
                "XML Parsing Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        Namespace classNS;
        try {
            classNS = Namespace.getNamespace("class", document.getRootElement().getNamespace("").getURI().replaceFirst("^https?:", "biomart:").replaceFirst("#$", "/class#"));
        }
        catch (Exception e) {
			JOptionPane.showMessageDialog(null,
                "Default (empty) namespace not valid. Wrong format perhaps?" +
                    "\n\nNamespace exception says:\n" + e.getMessage(),
                "XML Parsing Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        Namespace propertyNS = Namespace.getNamespace("property", document.getRootElement().getNamespace("").getURI().replaceFirst("^https?:", "biomart:").replaceFirst("#$", "/property#"));

        // Default namespaces from W3C:
        Namespace rdfNS = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        Namespace rdfsNS = Namespace.getNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        Namespace owlNS = Namespace.getNamespace("owl", "http://www.w3.org/2002/07/owl#");

        List<Element> classes = root.getChildren("Class", owlNS);
        List<Element> datatypeProperties = root.getChildren("DatatypeProperty", owlNS);

        if (classes == null || classes.size() <= 0) {
			JOptionPane.showMessageDialog(null,
                "There are no OWL classes defined in the ontology.",
                "Ontology Content Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (datatypeProperties == null || datatypeProperties.size() <= 0) {
			JOptionPane.showMessageDialog(null,
                "There are no OWL DatatypeProperties defined in the ontology.",
                "Ontology Content Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (Element classElement : classes) {
            org.jdom.Attribute aboutAttribute = classElement.getAttribute("about", rdfNS);
            String uri = aboutAttribute.getValue().replaceFirst("#\\w+$", "#");
            String name = aboutAttribute.getValue().replaceFirst(uri, "");

            RDFClass clazz;
            if (uri.equals(classNS.getURI()))
                clazz = new RDFClass("class:" + name);
            else
                clazz = new RDFClass(uri + name);

            Element parentClass = classElement.getChild("subClassOf", rdfsNS);
            if (parentClass != null)
                clazz.setSubClassOf(parentClass.getAttributeValue("resource", rdfNS));

            config.addRDF(clazz);
        }

        String domain = null, range = null, filter = null, attribute = null;
        for (Element datatypeProperty : datatypeProperties) {
            org.jdom.Attribute aboutAttribute = datatypeProperty.getAttribute("about", rdfNS);

            if (aboutAttribute == null) {
                JOptionPane.showMessageDialog(null,
                    "DatatypeProperty does not have an \"about\" attribute." +
                        "\n\nXML-element:\n" + datatypeProperty.getValue(),
                    "Ontology Content Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Content> datatypePropertyContents = datatypeProperty.getContent();

            for (Content datatypePropertyContent : datatypePropertyContents) {
                if (datatypePropertyContent instanceof Element) {
                    Element datatypePropertyElement = (Element)datatypePropertyContent;

                    if (datatypePropertyElement.getName().equals("domain") &&
                        datatypePropertyElement.getNamespace().equals(rdfsNS)) {
                        org.jdom.Attribute resourceAttribute = datatypePropertyElement.getAttribute("resource", rdfNS);

                        domain = resourceAttribute.getValue().replaceFirst("^&", "");
                    }
                    if (datatypePropertyElement.getName().equals("range") &&
                        datatypePropertyElement.getNamespace().equals(rdfsNS)) {
                        org.jdom.Attribute resourceAttribute = datatypePropertyElement.getAttribute("resource", rdfNS);

                        range = resourceAttribute.getValue().replaceFirst("^&", "");
                    }
                    if (datatypePropertyElement.getName().equals("filter") &&
                        datatypePropertyElement.getNamespace().equals(propertyNS)) {
                        filter = datatypePropertyElement.getValue();
                    }
                    if (datatypePropertyElement.getName().equals("attribute") &&
                        datatypePropertyElement.getNamespace().equals(propertyNS)) {
                        attribute = datatypePropertyElement.getValue();
                    }
                }
            }

            if (domain == null || range == null || (attribute == null && filter == null)) {
                JOptionPane.showMessageDialog(null,
                    "DatatypeProperty does not have a \"domain\", \"range\", \"attribute\" or \"filter\" attribute." +
                        "\n\nXML-element's \"about\" attribute:\n" + aboutAttribute.getValue(),
                    "Ontology Content Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Put info in filter preferrably, or fall back to attribute if filter not given.
            if (filter != null) {
                Filter f = config.getFilterByName(filter, null);
                String metadata = f.getRDF();
                String mcString = domain + ";" + aboutAttribute.getValue() + ";" + range;

                if (metadata.isEmpty())
                    f.setRDF(mcString);
                else
                    f.setRDF(f.getRDF() + "|" + mcString);
            } else {
                config.getAttributeByName(attribute, null);
            }
        }
    }
	
	public void init() {
		this.setLayout(new BorderLayout());

		checkboxPanel = new JPanel();
			
		addButton = new JButton(Resources.get("ADDACCESSPOINT"),McUtils.createImageIcon("images/add_group.gif"));
		//addButton.setToolTipText(Resources.get("ADDCONFIG"));
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addConfig();
			}			
		});
		
		this.rdfButton = new JButton(McUtils.createImageIcon("images/rdf.gif"));
        this.rdfButton.setText("Generate RDF");
		this.rdfButton.setActionCommand("rdf");
		this.rdfButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				generateOntology();
			}			
		});
		
		checkboxPanel.add(addButton);
		checkboxPanel.addMouseListener(this);

		String[] colNames = {	
								"Name",
								"Dataset",
								"Source",								
								"Group"
							};
		SharedDataModel model = new SharedDataModel(colNames);
		for(MartPointer mp :this.gc.getMartPointerList()){
			if(Boolean.parseBoolean(Settings.getProperty("hidemaskedmp"))){
				if(mp.getConfig().isHidden())
					continue;
			}
			model.addElement(mp);
		}
		
		configList = new JList(model);
		configList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		configList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		configList.setVisibleRowCount(-1);
		configList.setDragEnabled(true);
		configList.setDropMode(DropMode.INSERT);
		//configList.setDropTarget(new DropTarget(configList, new ConfigDropTargetListener(GuiContainerPanel.this)));
		ConfigListTransferHandler listHandler = new ConfigListTransferHandler(this);
		configList.setTransferHandler(listHandler);
		this.setMappings(configList);
		configList.setAutoscrolls(true);
		renderer = new ConfigListCellRenderer(true);
		configList.setCellRenderer(renderer);
		configList.addMouseListener(this);
		//configList.addMouseMotionListener(this);
		
		JScrollPane listsp = new JScrollPane(configList);
		//listsp.setPreferredSize(new Dimension(1050,750));
		//listsp.setAlignmentX(LEFT_ALIGNMENT);
		
		configTable = new ConfigTable(model);
		configTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		configTable.setDragEnabled(true);
		configTable.setDropMode(DropMode.INSERT);
		//configTable.setDropTarget(new DropTarget(configTable, new ConfigDropTargetListener(GuiContainerPanel.this)));
		ConfigTableTransferHandler tableHandler = new ConfigTableTransferHandler(this);
		configTable.setTransferHandler(tableHandler);
		this.setMappings(configTable);
		configTable.addMouseListener(this);
		//configTable.addMouseMotionListener(this);
		tableRenderer = new ConfigTableCellRenderer();
		configTable.getColumnModel().getColumn(0).setCellRenderer(tableRenderer);
		configTable.setRowHeight(20);
		configTable.setFont(new Font(this.getFont().getFamily(), Font.PLAIN, 14));		
		configTable.setAutoCreateRowSorter(false);
		
		JScrollPane tablesp = new JScrollPane(configTable);
		
		if(Boolean.parseBoolean(Settings.getProperty("portal.listview")))
			this.add(tablesp,BorderLayout.CENTER);
		else
			this.add(listsp,BorderLayout.CENTER);
		
		configTable.addMouseMotionListener(this);
		
		addShowHideCheckBox(checkboxPanel);
		addListViewCheckBox(checkboxPanel);
		this.add(checkboxPanel,BorderLayout.NORTH);
		
	}
	
	/**
	 * @param configPanel
	 */
	private void addShowHideCheckBox(JPanel configPanel) {
		hideCheckBox = new JCheckBox("Show hidden configs");
  		hideCheckBox.setSelected(!Boolean.parseBoolean(Settings.getProperty("hidemaskedmp")));
  		hideCheckBox.setActionCommand("hidemaskedmp");
  		hideCheckBox.addActionListener(new ActionListener(){
  			@Override
			public void actionPerformed(ActionEvent e) {
				//addConfig();
  				showHideItems();
			}
  		});
  		configPanel.add(hideCheckBox);
	}
	
	private void addListViewCheckBox(JPanel configPanel){
		listViewCheckBox = new JCheckBox("List View");
		listViewCheckBox.setSelected(Boolean.parseBoolean(Settings.getProperty("portal.listview")));
		listViewCheckBox.setActionCommand("showlistview");
		listViewCheckBox.addActionListener(new ActionListener(){
  			@Override
			public void actionPerformed(ActionEvent e) {
				//addConfig();
  				showListView();
			}
  		});
  		configPanel.add(listViewCheckBox);
	}
	
	private void showListView(){
		String showFlag = Settings.getProperty("portal.listview");
		boolean show = Boolean.parseBoolean(showFlag);
		show = !show;
		Settings.setProperty("portal.listview", Boolean.toString(show));
		((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refreshRootPane();
		
	}
	
	public void addConfig(ConfigComponent config) {
		//int count = this.configPanel.getComponentCount() - 1;
		//this.configPanel.add(config,count);
		
		//TODO
		((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).addConfigComponent(config);
		Set<Mart> linkedMart = this.getLinkedMartForConfig(config.getMartPointer().getConfig());
		((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getMPMartMap().put(
				config.getMartPointer(), linkedMart);
	}
	
	public void removeConfig(ConfigComponent config) {
		//this.configPanel.remove(config);
		((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).removeConfigComponent(config);
	}

	private void showHideItems() {
		String hideFlag = Settings.getProperty("hidemaskedmp");
		boolean iHide = Boolean.parseBoolean(hideFlag);
		iHide = !iHide;
		Settings.setProperty("hidemaskedmp", Boolean.toString(iHide));
		((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refreshRootPane();
		
	}

	private void addConfig() {
		if(this.gc.getGuiType().equals(GuiType.get("martreport"))) {
			this.addReportConfig();
		}else
			this.addNaiveConfig();
	}
	
	private void addReportConfig() {
		List<MartPointer> marts = this.getGuiContainer().getAllMartPointerListResursively();
		
		AddConfigFromMartDialog dialog = new AddConfigFromMartDialog(McGuiUtils.INSTANCE.getRegistryObject(),true, marts);
		
		final List<Mart> selectedmarts = dialog.getSelectedMart();
		if(McUtils.isCollectionEmpty(selectedmarts))
			return;
		final Mart mart = selectedmarts.get(0);
    	this.addReportConfig(mart);	
    			
	}
	
	public void addReportConfigs(List<Mart> martList) {
		for(final Mart mart: martList) {
			this.addReportConfig(mart);
		}
	}
	
	public void addReportConfig(final Mart mart) {
		// should be 1 master config only
		if(mart.getConfigList().size() <= 1){
			JOptionPane.showMessageDialog(this, "Please create a new configuration for data source "+mart.getDisplayName()+" before building a report");
			return;
		}
		
		
    	final AddConfigDialog acd = new AddConfigDialog(mart,true);
    	final ObjectController oc = new ObjectController();
    	if(acd.getConfigInfo()!=null) {
    		final ProgressDialog progressMonitor = ProgressDialog.getInstance();				
    		
    		final SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {


				@Override
				protected Void doInBackground() throws Exception {
    				try {
    					progressMonitor.setStatus("creating config ...");
    					ReportAttributesSelectDialog rasd = new ReportAttributesSelectDialog(oc, mart,acd.getConfigInfo().getName(),getGuiContainer());
    					  
    					((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refreshPanel(GuiContainerPanel.this);
    					
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
				
				@Override
				protected void done() {
    				progressMonitor.setVisible(false);				
				}
    		};
    		
    		worker.execute();
    		progressMonitor.start("processing ...");
    		//request update gui
    	}
	}
	
	private void addNaiveConfig() {
		
		AddConfigFromMartDialog dialog = new AddConfigFromMartDialog(McGuiUtils.INSTANCE.getRegistryObject(),false,null);
		final UserGroup user = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
		final List<Mart> marts = dialog.getSelectedMart();
		if(McUtils.isCollectionEmpty(marts))
			return;
		if(marts.size() == 1)
			this.addSingleConfig(marts.get(0), user);
		else
			this.addMultipleConfigs(marts,user);
	}
	
	public void addMultipleConfigs(final List<Mart> marts, final UserGroup user) {
    	
    	final ObjectController oc = new ObjectController();
    	//after the first config added, enable report tab
    	DnDTabbedPane tp = (DnDTabbedPane)SwingUtilities.getAncestorOfClass(DnDTabbedPane.class, this);
		if(tp != null){
			for(int i=0; i<tp.getComponentCount(); i++) {
				if(tp.getComponent(i) instanceof GuiContainerPanel)
				{
					GuiContainerPanel gcp = (GuiContainerPanel)tp.getComponent(i);
					if(gcp.getGuiContainer().getName().equals("report"))
					{
						gcp.enableControls(true);
						
						ButtonTabComponent btc = (ButtonTabComponent)tp.getTabComponentAt(i-1);
						btc.setEnableTitle(true);
					}
				}
			}
		}
		

    		final ProgressDialog progressMonitor = ProgressDialog.getInstance();			
    		
    		final SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {


				@Override
				protected Void doInBackground() throws Exception {
    				try {
    					progressMonitor.setStatus("creating config ...");
    					for(Mart mart: marts) {
    						String defaultName = McGuiUtils.INSTANCE.getUniqueConfigName(mart, mart.getName()+ Resources.get("CONFIGSUFFIX"));
    						GuiContainer gc = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getSelectedGuiContainer();
    						oc.addConfigFromMaster(mart, defaultName,user,false, gc);  
    					}
    					((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refreshPanel(GuiContainerPanel.this);   					
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
				
				@Override
				protected void done() {
					progressMonitor.setVisible(false);
				}
    		};
    		
    		worker.execute();
    		progressMonitor.start("processing ...");
    		//request update gui
    		
    					
	}
	
	public void addSingleConfig(final Mart mart, final UserGroup user) {
    	final AddConfigDialog acd = new AddConfigDialog(mart,false);
    	final ObjectController oc = new ObjectController();
    	//after the first config added, enable report tab
    	DnDTabbedPane tp = (DnDTabbedPane)SwingUtilities.getAncestorOfClass(DnDTabbedPane.class, this);
		if(tp != null){
			for(int i=0; i<tp.getComponentCount(); i++) {
				if(tp.getComponent(i) instanceof GuiContainerPanel)
				{
					GuiContainerPanel gcp = (GuiContainerPanel)tp.getComponent(i);
					if(gcp.getGuiContainer().getName().equals("report"))
					{
						gcp.enableControls(true);
					
						ButtonTabComponent btc = (ButtonTabComponent)tp.getTabComponentAt(i-1);
						btc.setEnableTitle(true);
					}
				}
			}
		}
		
    	if(acd.getConfigInfo()!=null) {
    		final ProgressDialog progressMonitor = ProgressDialog.getInstance();			
    		
    		final SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {

    			@Override
    			protected void done() {
    				// Close the progress dialog.
    				progressMonitor.setVisible(false);
    			}

				@Override
				protected Void doInBackground() throws Exception {
    				try {   
    					progressMonitor.setStatus("creating config ...");
    					GuiContainer gc = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getSelectedGuiContainer();
    					if(acd.getConfigInfo().isDoNaive())
    						oc.addConfigFromMaster(mart, acd.getConfigInfo().getName(),user, true, gc);  
    					else
    						oc.addEmptyConfig(mart, acd.getConfigInfo().getName(), user, gc);
    					((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).refreshPanel(GuiContainerPanel.this);
    					
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
    		progressMonitor.start("processing ...");
    		//request update gui
    		
    	}		
	}

	public void addTabbedPane(DnDTabbedPane tp) {
		this.removeAll();
		this.add(tp,BorderLayout.CENTER);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		this.configTable.setRow(-1);
		this.configTable.setMart(null);
		this.configTable.setSelfOnly(false);
		
		this.configTable.repaint();
		McViewSourceGroup groupView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		groupView.refreshGui();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3) {
			int row = this.configTable.rowAtPoint(e.getPoint());
			if(row >= 0) {
				this.configTable.addRowSelectionInterval(row, row);
			}
		}
		if(e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
			int index = 0;
			if(this.configList.getSelectedValue() != null)
				index = this.configList.getSelectedIndex();
			else if(this.configTable.getSelectedRowCount() > 0)
				index = this.configTable.getSelectedRow();
			
			MartPointer mp = (MartPointer)this.configList.getModel().getElementAt(index);
			new ConfigDialog(mp.getConfig());
		}
		
		McViewSourceGroup groupView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		groupView.refreshGui();
		boolean outside = true;
		if(this.configList.getModel().getSize() == 0)
			outside = true;
		else if(Boolean.parseBoolean(Settings.getProperty("portal.listview")))
			outside =  this.configTable.rowAtPoint(e.getPoint()) < 0;
		else
			outside = !this.configList.getCellBounds(0, this.configList.getModel().getSize()-1).contains(e.getPoint());
		if(!outside){
			if(e.isPopupTrigger()) {
				JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this,"config",
							this.getSelectedMPs().size()>1);
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}else{
			if(e.isPopupTrigger()) {
				JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this, "guipanel",false);
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
			this.configList.clearSelection();
			this.configTable.clearSelection();
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {

		boolean outside = true;
		if(this.configList.getModel().getSize() == 0)
			outside = true;
		else if(Boolean.parseBoolean(Settings.getProperty("portal.listview")))
			outside =  this.configTable.rowAtPoint(e.getPoint()) < 0;
		else
			outside = !this.configList.getCellBounds(0, this.configList.getModel().getSize()-1).contains(e.getPoint());
		if(!outside){
			if(e.isPopupTrigger()) {
				JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this,"config",
							this.getSelectedMPs().size()>1);
				menu.show(e.getComponent(), e.getX(), e.getY());
			}	
		}else{
			if(e.isPopupTrigger()) {
				JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this, "guipanel",false);
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
			this.configList.clearSelection();
			this.configTable.clearSelection();
			
		}
	}

	public Set<Mart> getLinkedMartForConfig(Config config) {
		Set<Mart> mpSet = new HashSet<Mart>();
		List<Attribute> attList = config.getAttributes(null, true, true);
		for(Attribute att: attList) {
			if(att.isPointer() && !McUtils.isStringEmpty(att.getPointedMartName())) {
				String martName = att.getPointedMartName();
				Mart mart = config.getMart().getMartRegistry().getMartByName(martName);
				if(mart!=null)
					mpSet.add(mart);
			}
		}
		List<Filter> filterList = config.getRootContainer().getAllFilters(null, true, true);
		for(Filter fil: filterList) {
			if(fil.isPointer() && !McUtils.isStringEmpty(fil.getPointedMartName())) {
				String martName = fil.getPointedMartName();
				Mart mart = config.getMart().getMartRegistry().getMartByName(martName);
				if(mart!=null)
					mpSet.add(mart);
			}
		}

		return mpSet;
	}

	public List<ConfigComponent> getConfigComponents() {
		List<ConfigComponent> cl = new ArrayList<ConfigComponent>();
		/*Component[] cs = this.configPanel.getComponents();
		for(Component c: cs) {
			if(c instanceof ConfigComponent) {
				cl.add((ConfigComponent)c);
			}
		}*/
		return cl;
	}
	
	// enable or disable controls in the panel
	public void enableControls(boolean enable) {
		this.addButton.setEnabled(enable);
		this.listViewCheckBox.setEnabled(enable);
		this.hideCheckBox.setEnabled(enable);
		this.rdfButton.setEnabled(enable);
	}

	public void clean() {
		/*for(Component cc: this.configPanel.getComponents()) {
			if(cc instanceof ConfigComponent) {
				((ConfigComponent)cc).clean();
				cc = null;
			}
		}*/
			
		this.gc = null;
	}
	
	public List<MartPointer> getSelectedMPs() {
		int[] indexs = {};
		if(this.getConfigList().getSelectedIndices().length > 0)
			indexs = this.getConfigList().getSelectedIndices();
		else if(this.getConfigTable().getSelectedRowCount() > 0)
			indexs = this.getConfigTable().getSelectedRows();
		
		List<MartPointer> mps = new ArrayList<MartPointer>();
		for(int index : indexs){
			MartPointer mp = (MartPointer) ((SharedDataModel)this.getConfigList().getModel()).elementAt(index);
			mps.add(mp);
		}
		
		return mps;
	}
	
	public int[] getSelectedIndexes() {
		int[] indexs = {};
		String showFlag = Settings.getProperty("portal.listview");
		boolean show = Boolean.parseBoolean(showFlag);
		if(show)
			indexs = this.getConfigList().getSelectedIndices();
		else
			indexs = this.getConfigTable().getSelectedRows();
		
		return indexs;
	}
	
	public void setSelectedIndexes(int[] selIndexes) {
		String showFlag = Settings.getProperty("portal.listview");
		boolean show = Boolean.parseBoolean(showFlag);
		if(!show)
			this.getConfigList().setSelectedIndices(selIndexes);
		else{
			for(int selIndex : selIndexes)
				this.getConfigTable().addRowSelectionInterval(selIndex, selIndex);
		}
	}

	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// TODO Auto-generated method stub
		
	}

    private void setMappings(JList list) {
        ActionMap map = list.getActionMap();
        map.put(ConfigListTransferHandler.getCutAction().getValue(Action.NAME),
        		ConfigListTransferHandler.getCutAction());
        map.put(ConfigListTransferHandler.getCopyAction().getValue(Action.NAME),
        		ConfigListTransferHandler.getCopyAction());
        map.put(ConfigListTransferHandler.getPasteAction().getValue(Action.NAME),
        		ConfigListTransferHandler.getPasteAction());
    }
    
    private void setMappings(JTable table) {
    	ActionMap map = table.getActionMap();
        map.put(ConfigTableTransferHandler.getCutAction().getValue(Action.NAME),
        		ConfigTableTransferHandler.getCutAction());
        map.put(ConfigTableTransferHandler.getCopyAction().getValue(Action.NAME),
        		ConfigTableTransferHandler.getCopyAction());
        map.put(ConfigTableTransferHandler.getPasteAction().getValue(Action.NAME),
        		ConfigTableTransferHandler.getPasteAction());
    }

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName().equals(McEventProperty.USER.toString())) {
			this.configList.repaint();
			this.configTable.repaint();
		}
		//UserGroup ug = (UserGroup)evt.getNewValue();
		
	}

	public void setHighlight(Mart mart) {
		// TODO Auto-generated method stub
		this.configTable.setMart(mart);
		this.configTable.setSelfOnly(false);
		this.configTable.repaint();
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
	
		int row = configTable.rowAtPoint(e.getPoint());
		if(row >= 0) {
			McViewSourceGroup groupView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
			groupView.refreshGui();
			this.configTable.setRow(row);
			SharedDataModel model = (SharedDataModel)configTable.getModel();
			MartPointer mp = (MartPointer)model.elementAt(row);
			this.configTable.setMart(mp.getMart());
			this.configTable.setSelfOnly(true);
		}else {
			this.configTable.setRow(row);
			this.configTable.setMart(null);
			this.configTable.setSelfOnly(false);
		}
	
		this.configTable.repaint();
		McViewSourceGroup groupView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		groupView.refreshGui();
	}

}