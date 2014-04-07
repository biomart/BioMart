package org.biomart.configurator.view.menu;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.XMLElements;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.TreeNodeHandler;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.gui.dialogs.AddUserDialog;
import org.biomart.configurator.view.gui.dialogs.MartSelectionDialog;
import org.biomart.configurator.view.gui.dialogs.SearchComponentDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.biomart.objects.objects.SourceContainer;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.Portal;
import org.biomart.objects.portal.UserGroup;
import org.biomart.objects.portal.Users;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * a singleton class
 */
public class McMenus implements ActionListener {
	private String menuXML = "conf/xml/menu.xml";
	private static McMenus instance = null;
	private Element root;	
	private MartConfigTree lTree;  
	private JMenuBar menuBar;
	private int firstRecentFileEntry;
	

	public static McMenus getInstance() {
		if(instance == null)
			instance = new McMenus();
		return instance;
	}
	
	
	private McMenus() {
	    try {
	       // Build the document with SAX and Xerces, no validation
	       SAXBuilder builder = new SAXBuilder();
	       // Create the document
	       Document doc = builder.build(new File(menuXML));
	       root = doc.getRootElement();
	    } catch (Exception e) {
	       e.printStackTrace();
	    }
	}
	
	
	public JMenuBar getMainMenu() {
		if(this.menuBar!=null)
			return this.menuBar;
		this.menuBar = new JMenuBar();
		Element menuBarElement = root.getChild("MenuBar");
		@SuppressWarnings("unchecked")
		List<Element> menuElementList = menuBarElement.getChildren();
		for(Element menuElement: menuElementList) {
			JMenu menu = new JMenu(menuElement.getAttributeValue("Text"));
			@SuppressWarnings("unchecked")
			List<Element> menuItemElementList = menuElement.getChildren();
			for(Element menuItemElement: menuItemElementList) {
				if(menuItemElement.getName().equals("MenuItem")) {
					//check menuType
					JMenuItem menuItem = null;
					String type = menuItemElement.getAttributeValue("Type");
					if("checkbox".equals(type)) {
						menuItem = new JCheckBoxMenuItem(menuItemElement.getAttributeValue("Text"));
						if("1".equals(Settings.getProperty(menuItemElement.getAttributeValue("Property"))))
							menuItem.setSelected(true);
						else
							menuItem.setSelected(false);
					}
					else
						menuItem = new JMenuItem(menuItemElement.getAttributeValue("Text"));
					String disabledStr = menuItemElement.getAttributeValue("Disabled");
					if(disabledStr !=null && disabledStr.equals("true")) {
						menuItem.setEnabled(false);
					}
					Element listenerElement = menuItemElement.getChild("Listener");
					menuItem.setActionCommand(listenerElement.getAttributeValue("id"));
					menuItem.addActionListener(this);
					menu.add(menuItem);
				} else if(menuItemElement.getName().equals("SubMenu")) {//submenu
					JMenu menuItem = new JMenu(menuItemElement.getAttributeValue("Text"));
					@SuppressWarnings("unchecked")
					List<Element> subItemElementList = menuItemElement.getChildren();
					for(Element subItemElement: subItemElementList) {
						JMenuItem subItem = null;
						String type = subItemElement.getAttributeValue("Type");
						if("checkbox".equals(type)) {
							subItem = new JCheckBoxMenuItem(subItemElement.getAttributeValue("Text"));
							if("1".equals(Settings.getProperty(subItemElement.getAttributeValue("Property"))))
								subItem.setSelected(true);
							else
								subItem.setSelected(false);
						}
						else
							subItem = new JMenuItem(subItemElement.getAttributeValue("Text"));
						String disabledStr = subItemElement.getAttributeValue("Disabled");
						if(disabledStr !=null && disabledStr.equals("true")) {
							subItem.setEnabled(false);
						}						
						Element listenerElement = subItemElement.getChild("Listener");
						subItem.setActionCommand(listenerElement.getAttributeValue("id"));	
						subItem.addActionListener(this);
						menuItem.add(subItem);
					}
					menu.add(menuItem);
				} else if(menuItemElement.getName().equals("Separator")) {
					menu.add(new JSeparator());
				}
			}
			menuBar.add(menu);		
		}
		//add listener to File menu
		this.initFileMenu();
		
		//add edit menu cut copy paste
		JMenuItem menuItem = null;
        JMenu mainMenu = new JMenu("Edit");
        mainMenu.setMnemonic(KeyEvent.VK_E);
        TransferActionListener actionListener = new TransferActionListener();

        menuItem = new JMenuItem("Cut");
        menuItem.setActionCommand((String)TransferHandler.getCutAction().
                 getValue(Action.NAME));
        menuItem.addActionListener(actionListener);
        menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        menuItem.setMnemonic(KeyEvent.VK_T);
        mainMenu.add(menuItem);
        
        menuItem = new JMenuItem("Copy");
        menuItem.setActionCommand((String)TransferHandler.getCopyAction().
                 getValue(Action.NAME));
        menuItem.addActionListener(actionListener);
        menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        menuItem.setMnemonic(KeyEvent.VK_C);
        mainMenu.add(menuItem);
        
        menuItem = new JMenuItem("Paste");
        menuItem.setActionCommand((String)TransferHandler.getPasteAction().
                 getValue(Action.NAME));
        menuItem.addActionListener(actionListener);
        menuItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        menuItem.setMnemonic(KeyEvent.VK_P);
        mainMenu.add(menuItem);

        //menuBar.add(mainMenu);
		return menuBar;
	}


 
	@SuppressWarnings("unused")
	private boolean requestValidatePortal() {
		return true;
	}
	
	public void requestLoadPortal(boolean newPortal) throws MartBuilderException {
		this.loadPortal(newPortal, null);
	}

	public void requestImportPortal() throws MartBuilderException{
		this.importPortal(null);
	}

	private void importPortal(File file) throws MartBuilderException{		
		File loadFile = file;
		if(loadFile == null) {
			final String currentDir = Settings.getProperty("currentOpenDir");
			final JFileChooser xmlFileChooser = new JFileChooser();
			xmlFileChooser.setCurrentDirectory(currentDir == null ? new File(".")
					: new File(currentDir));
			if (xmlFileChooser.showOpenDialog(McViews.getInstance().getView(IdwViewType.PORTAL)) == JFileChooser.APPROVE_OPTION) {
				// Update the load dialog.
				Settings.setProperty("currentOpenDir", xmlFileChooser
						.getCurrentDirectory().getPath());
	
				loadFile = xmlFileChooser.getSelectedFile();
			}
		}
		if(loadFile!=null) {
			//find the keyfile
			String tmpName = loadFile.getName();
			int index = tmpName.lastIndexOf(".");
			if(index>0)
				tmpName = tmpName.substring(0,index);
			String keyFileName = loadFile.getParent()+File.separator+"."+tmpName;
			BufferedReader input;
			String key=null;
			try {
				input = new BufferedReader(new FileReader(keyFileName));
				key = input.readLine();
				input.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				Log.error("key file not found");
				//if key file no found generate one
				generateKeyFile(loadFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(McUtils.isStringEmpty(key) && "1".equals(Settings.getProperty("promptnokeydialog"))) {
				//JOptionPane.sh
				int n = JOptionPane.showConfirmDialog(
					    null,
					    "No associated key file found, want to continue?",
					    "Confirm",
					    JOptionPane.YES_NO_OPTION);
				if(n!=JOptionPane.YES_OPTION)
					return;
			}
			importWithProgressBar(loadFile,key);
		}
	}
	
	private void loadPortal(boolean newPortal, File file) throws MartBuilderException {
		
		// Open the file chooser.
		
		//this.usersCB.removeAllItems();
		if(!newPortal) {
			File loadFile = file;
			if(loadFile == null) {
				final String currentDir = Settings.getProperty("currentOpenDir");
				final JFileChooser xmlFileChooser = new JFileChooser();
				xmlFileChooser.setCurrentDirectory(currentDir == null ? new File(".")
						: new File(currentDir));
				if (xmlFileChooser.showOpenDialog(McViews.getInstance().getView(IdwViewType.PORTAL)) == JFileChooser.APPROVE_OPTION) {
					Options.getInstance().setOptions(null);
					McViewPortal portalView = (McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL);
					portalView.clean();
				   	McViewSourceGroup sourceView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
			    	sourceView.clean();

			    	McUtils.gc();
					// Update the load dialog.
					Settings.setProperty("currentOpenDir", xmlFileChooser
							.getCurrentDirectory().getPath());
		
					loadFile = xmlFileChooser.getSelectedFile();
				}else{
					return;
				}
			}
			//choose the key file first
			if(loadFile!=null) {
				Options.getInstance().setOptions(null);
				McViewPortal portalView = (McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL);
				portalView.clean();
			   	McViewSourceGroup sourceView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		    	sourceView.clean();
		    	McUtils.gc();
				//find the keyfile
				String tmpName = loadFile.getName();
				int index = tmpName.lastIndexOf(".");
				if(index>0)
					tmpName = tmpName.substring(0,index);
				String keyFileName = loadFile.getParent()+File.separator+"."+tmpName;
				BufferedReader input;
				String key=null;
				try {
					input = new BufferedReader(new FileReader(keyFileName));
					key = input.readLine();
					input.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					Log.error("key file not found");
					//if key file no found generate one
					generateKeyFile(loadFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(McUtils.isStringEmpty(key) && "1".equals(Settings.getProperty("promptnokeydialog"))) {
					//JOptionPane.sh
					int n = JOptionPane.showConfirmDialog(
						    null,
						    "No associated key file found, want to continue?",
						    "Confirm",
						    JOptionPane.YES_NO_OPTION);
					if(n!=JOptionPane.YES_OPTION)
						return;
				}
				openWithProgressBar(loadFile,key);
			}
			//set the mart configurator title to file name after select a configure file
			setMcTitle(loadFile);
		}else {
			Options.getInstance().setOptions(null);
			File newfile = this.loadUntitledXML();
			setMcTitle(newfile);
		}
		MartController.getInstance().setChanged(false);
		System.setProperty("finishloading", Boolean.toString(true));
	}
	
	private File loadUntitledXML() {
		MartRegistry registry = new MartRegistry(XMLElements.MARTREGISTRY.toString());
		McGuiUtils.INSTANCE.setRegistry(registry);
		File newfile = new File("conf/xml/untitled.xml");
		Document document = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(newfile);
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage());
			return newfile;
		}
		this.createObjectsAndShowGui(registry, document);
		Settings.setProperty("currentFile", "");
		document = null;
		return newfile;
	}

	/**
	 * @param loadFile
	 */
	private void setMcTitle(File loadFile) {
		if(loadFile == null)
			return;
		String title = Resources.get("APPLICATIONTITLE");
		StringBuilder titleBuilder = new StringBuilder(title);
		titleBuilder.append(" - ");
		titleBuilder.append(loadFile.getName());
		Container c = SwingUtilities.getAncestorOfClass(JFrame.class, this.menuBar);
		if(c!=null) {
			JFrame frame = (JFrame)c;
			frame.setTitle(titleBuilder.toString());
		}
	}
	
	private void importMcXML(File file, String key) throws MartBuilderException {
		//clean all first
		//ProgressDialog progressMonitor = new ProgressDialog(null);
		
		MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
		Document document = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(file);
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage());
			return;
		}
		String oldKey = McUtils.getKey();
		McUtils.setKey(key);
		this.importObjectsAndShowGui(registry, document);
		if(Boolean.parseBoolean(System.getProperty("xmlautofix"))) {
			MartController.getInstance().fixPartitionTable();
		}
		//restore the oldkey after decypt the imported mart
		McUtils.setKey(oldKey);
		//clear document
		document = null;
		MartController.getInstance().setChanged(false);
		final Properties history = new Properties();
		history.setProperty("location", file.getPath());
	}
	
	private void openMcXML(File file, String key) throws MartBuilderException {
		//clean all first
		//ProgressDialog progressMonitor = new ProgressDialog(null);
		
		MartRegistry registry = new MartRegistry(XMLElements.MARTREGISTRY.toString());
		McGuiUtils.INSTANCE.setRegistry(registry);
		Document document = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(file);
			Settings.setProperty("currentFile", file.getCanonicalPath());
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage());
			return;
		}
		McUtils.setKey(key);
		this.createObjectsAndShowGui(registry, document);
		if(Boolean.parseBoolean(System.getProperty("xmlautofix"))) {
			MartController.getInstance().fixPartitionTable();
		}
		//clear document
		document = null;
		MartController.getInstance().setChanged(false);
		// Save XML filename in history of accessed files.
		final Properties history = new Properties();
		history.setProperty("location", file.getPath());
		Settings.saveHistoryProperties(McMenus.class, file.getName(), history);		
	}
	
	private void importWithProgressBar(final File file, final String key) {
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
					progressMonitor.setStatus("loading File "+file.getAbsolutePath());
					importMcXML(file,key);
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

	}
	
	private void openWithProgressBar(final File file, final String key) {
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
					progressMonitor.setStatus("loading File "+file.getAbsolutePath());
					openMcXML(file,key);
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

	}
		    
    private void createObjectsAndShowGui(MartRegistry registry, Document document) throws MartBuilderException {   
    	this.requestCreateObjects(registry, document);
		//show objects in the gui
		this.showComponents(registry);    	
    }
    
    private void importObjectsAndShowGui(MartRegistry registry, Document document) throws MartBuilderException {   
    	this.requestImportObjects(registry, document);
		//show objects in the gui
		this.showComponents(registry);    	
    }
    
    public void requestCreateObjects(MartRegistry registry, Document document) throws MartBuilderException {
    	//check version
    	if(McGuiUtils.INSTANCE.getMcXMLVersion(document).equals(Resources.BIOMART_VERSION)) {
    		MartController.getInstance().requestCreateRegistryFromXML(registry,document);
	    	//update partitiontable? if the schemapartitiontable has only one row
	    	if("1".equals(Settings.getProperty("updatepartitiontable"))) {
	    		
	    	}
    	} else {
    		JOptionPane.showMessageDialog(null, "xml version error", "error", JOptionPane.ERROR_MESSAGE);
    	}
    }
       
    private void requestImportObjects(MartRegistry registry, Document document) throws MartBuilderException {
    	//check version
    	if(McGuiUtils.INSTANCE.getMcXMLVersion(document).equals(Resources.BIOMART_VERSION)) {
    		//show mart selection dialog
    		Element registryElement = document.getRootElement();
    		@SuppressWarnings("unchecked")
			List<Element> martElementList = registryElement.getChildren(XMLElements.MART.toString());
    		List<String> martNameList = new ArrayList<String>();
    		for(Element martElement: martElementList) {
    			martNameList.add(martElement.getAttributeValue(XMLElements.NAME.toString()));
    		}  
    		MartSelectionDialog mds = new MartSelectionDialog(null,martNameList);
    		if(mds.getResults().isEmpty()) {
    			mds.dispose();
    			return;
    		}
    		//get selected mart
    		List<Mart> martList = new ArrayList<Mart>();
    		for(Element martElement: martElementList) {
    			String martName = martElement.getAttributeValue(XMLElements.NAME.toString());
    			if(mds.getResults().contains(martName)) {
    				if(registry.getMartByName(martName)!=null) {
    					JOptionPane.showMessageDialog(null, "mart "+martName+ " already exist");
    					continue;
    				}
    				Mart mart = new Mart(martElement);
    				martList.add(mart);
    				registry.addMart(mart);
    				//check group
    				String gname = mart.getGroupName();
    				if(McUtils.isStringEmpty(gname)) 
    					gname = XMLElements.DEFAULT.toString();
    				SourceContainer sc = registry.getSourcecontainers().getSourceContainerByName(gname);
    				if(sc == null) {
    					sc = new SourceContainer(gname);
    					registry.getSourcecontainers().addSourceContainer(sc);
    				}
    			}
    		}
    		if(!martList.isEmpty()) {
				Element importedOptions = registryElement.getChild(XMLElements.OPTIONS.toString());
				@SuppressWarnings("unchecked")
				List<Element> martsOptions = importedOptions.getChildren();
	    		//import options
	    		for(Mart mart: martList) {
	    			Element martElement = null;
	    			//find the mart
	    			for(Element martE: martsOptions) {
	    				if(martE.getAttributeValue(XMLElements.NAME.toString()).equals(mart.getName())) {
	    					martElement = martE;
	    					break;
	    				}
	    			}
	    			if(martElement!=null) {
	    				martElement.detach();
	    				Options.getInstance().addMartElement(martElement);
	    			}
	    		}
	    	}
    		//add related portal
    		if(mds.isImportPortal() && !martList.isEmpty()) {
    			String newgcName = McGuiUtils.INSTANCE.getNextGuiContainerName("import");
    			GuiContainer newgc = new GuiContainer(newgcName);
    			Element portalElement = registryElement.getChild(XMLElements.PORTAL.toString());
    			Portal tmpPortal = new Portal(portalElement);
    			for(Mart mart: martList) {
    				List<MartPointer> mpList = tmpPortal.getRootGuiContainer().getMartPointerListforMartName(mart.getName());
    				for(MartPointer mp: mpList) {
    					newgc.addMartPointer(mp);
    				}
    			}
    			registry.getPortal().getRootGuiContainer().addGuiContainer(newgc);
    		}

    		
    		registry.synchronizedFromXML();
    		
	    	//update partitiontable? if the schemapartitiontable has only one row
	    	if("1".equals(Settings.getProperty("updatepartitiontable"))) {
	    		
	    	}
	    	mds.dispose();
    	} else {
    		JOptionPane.showMessageDialog(null, "xml version error", "error", JOptionPane.ERROR_MESSAGE);
    	}
 
    }

    private void showComponents(MartRegistry rc) {
    	//source
    	//McViewSource sourceView = (McViewSource)McViews.getInstance().getView(IdwViewType.SOURCE);
    	McViewSourceGroup sourceView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
    	sourceView.showSource(rc);
    	//portal
    	McViewPortal portalView = (McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL);
    	portalView.showPortal(rc.getPortal());
    	
    }
    
    public boolean requestSavePortal() {
    	final String currentFileName = Settings.getProperty("currentFile");
    	if(McUtils.isStringEmpty(currentFileName)){
    		int retval = this.requestSaveAsPortal(false);
    		if(retval == JFileChooser.CANCEL_OPTION)
    			return false;
    	}
    	else {
    		File file = new File(currentFileName);
    		this.savePortalToFileWithProgressBar(file);
    		MartController.getInstance().setChanged(false);
    	}
    	return true;
    }
    
    public int requestSaveAsPortal(boolean showProgress) {
		// Show a file chooser. If the user didn't cancel it, process the
		// response.
		final String currentDir = Settings.getProperty("currentSaveDir");
		final JFileChooser xmlFileChooser = new JFileChooser();
		xmlFileChooser.setCurrentDirectory(currentDir == null ? null
				: new File(currentDir));
		int retval = xmlFileChooser.showSaveDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			Settings.setProperty("currentSaveDir", xmlFileChooser
					.getCurrentDirectory().getPath());

			// Find out the file the user chose.
			final File saveAsFile = xmlFileChooser.getSelectedFile();
			if(saveAsFile.exists()) {
	    		int ret = JOptionPane.showConfirmDialog(null, saveAsFile.getName()+" already exists, override?");
	    		if(ret != JOptionPane.OK_OPTION)
	    			return -1;
	    	}
			this.requestSavePortalToFile(saveAsFile, showProgress);
	    	//set current saved file name
	    	try {
				Settings.setProperty("currentFile", saveAsFile.getCanonicalPath());
	    	} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, Resources.get("SAVEXMLERROR"));
			}
			// Save XML filename in history of accessed files.
			final Properties history = new Properties();
			history.setProperty("location", saveAsFile.getPath());
			Settings.saveHistoryProperties(this.getClass(), saveAsFile.getName(), history);
			MartController.getInstance().setChanged(false);
			//update filename display in the title
			this.setMcTitle(saveAsFile);
		}
		return retval;
    }
   
    public void requestSavePortalToFile(File file, boolean showProgress) {

		// Skip the rest if they cancelled the save box.
		if (file == null)
			return;
		//update subclassrelation in datasets
//		McViewSchema viewSchema = (McViewSchema)McViews.getInstance().getView(IdwViewType.SCHEMA);
//		viewSchema.updateSubclassRelation((JDomNodeAdapter)lTree.getModel().getRoot());
//		lTree.getModel().save(saveAsFile);  
		
		//create a key file has the same name as .xmlfilename
		generateKeyFile(file);  
       if(showProgress)
    	   this.savePortalToFileWithProgressBar(file);
       else
    	   this.savePortalToFile(file);

    }


	/**
	 * @param file
	 */
	private void generateKeyFile(File file) {
		String tmpName = file.getName();
		int index = tmpName.lastIndexOf(".");
		if(index>0)
			tmpName = tmpName.substring(0,index);
		String keyFileName = file.getParent()+File.separator+"."+tmpName;
	    KeyGenerator kgen=null;
		try {
			kgen = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    kgen.init(128); // 192 and 256 bits may not be available


       // Generate the secret key specs.
       SecretKey skey = kgen.generateKey();
       byte[] raw = skey.getEncoded();
       String hexStr = McUtils.asHex(raw);
       McUtils.setKey(hexStr);
       try {
    	   Writer output = new BufferedWriter(new FileWriter(keyFileName));
    	   output.write(hexStr);
    	   output.close();
       } catch (IOException e) {
		// TODO Auto-generated catch block
    	   e.printStackTrace();
       }
	}
    
	private String getOptionFileName(File file) {
		String tmpName = file.getName();
		int index = tmpName.lastIndexOf(".");
		if(index>0)
			tmpName = tmpName.substring(0,index);
		return file.getParent()+File.separator+tmpName+"_option.xml";
	}
	
    private void savePortalToFileWithProgressBar(final File file) {
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
					progressMonitor.setStatus("saving file: "+file.getName());
					savePortalToFile(file);
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

    }
    
    private void savePortalToFile(File file) {
    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
     	try {
     		Document doc = McGuiUtils.INSTANCE.getRegistryObject().generateXmlDocument();
     		if(Options.getInstance().getOptionRootElement()!=null) {
        		if(Boolean.parseBoolean(Settings.getProperty("option.inseparatefile"))) {
        			File optionFile = new File(this.getOptionFileName(file));
             		FileOutputStream fos = new FileOutputStream(optionFile);
            		outputter.output(Options.getInstance().getOptionRootElement(), fos);
            		fos.close();
        		}else {
        			doc.getRootElement().addContent(Options.getInstance().getOptionRootElement());
        		}
     		}
     		FileOutputStream fos = new FileOutputStream(file);
    		outputter.output(doc, fos);
    		fos.close();
    		MartController.getInstance().setChanged(false);
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		JOptionPane.showMessageDialog(null, Resources.get("SAVEXMLERROR"));
    	}   	
    }
    
    private void startServer(){
    	this.deploy();
    }
    
    private void stopServer(){
    	String os = System.getProperty("os.name").toLowerCase();
       	String currentPath = ".";
       	String cmd = currentPath;
       	if(os.indexOf("win")>=0) 
       		cmd = cmd+"/scripts/biomart-server.bat ";
       	else
       		cmd = cmd+"/scripts/biomart-server.sh ";
       	
    	Runtime rt = Runtime.getRuntime();
        String serverStop = cmd+"stop";
        
        Log.info(serverStop);
        
		try {
			Process proc2 = rt.exec(serverStop);
			int exitCode = proc2.waitFor();
			Log.info("stop exitValue: "+exitCode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }

    public void requestDeploy(boolean serverStarted) {
    	
    	final boolean bServerStarted = serverStarted;
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
					if(bServerStarted){
						progressMonitor.setStatus("Starting server, please wait...");
						startServer();
					}else{
						progressMonitor.setStatus("Stopping server, please wait...");
						stopServer();
					}
					//deploy();
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
		progressMonitor.start("deploying ...");

    }
    
    class ShellThread extends Thread{
    	public void run(){
    		String os = System.getProperty("os.name").toLowerCase();
           	//get fullpath
           	String currentPath = ".";
           	String cmd = currentPath;
           	if(os.indexOf("win")>=0) 
           		cmd = cmd+"/scripts/biomart-server.bat ";
           	else
           		cmd = cmd+"/scripts/biomart-server.sh ";
           	try{
    		Runtime rt = Runtime.getRuntime();
            String serverStop = cmd+"stop";
            String serverStart = cmd + "start";
            Log.info(serverStop);
            Process proc2 = rt.exec(serverStop);
            int exitCode = proc2.waitFor();
            Log.info("stop exitValue: "+exitCode);
            //wait for 5 seconds
            Thread.sleep(5000);
            Log.info(serverStart);
            Process proc = rt.exec(serverStart);
            //if windows dont wait for the process to finish, cause it never will untill user stop the service
            //if(os.indexOf("win")>=0){
           	// return;
            //}
            
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            Log.info("<Info>");
            while ( (line = br.readLine()) != null)
                Log.info(line);
            Log.info("</Info>");
         
            int exitVal = proc.waitFor();
            Log.info("start exitValue: " + exitVal);
           	}catch(Exception e){
           		e.printStackTrace();
           	}
    	}
    }
    private void deploy() {   	
    	boolean hasKey = true;
    	String currentFileName = Settings.getProperty("currentFile");
    	if(McUtils.isStringEmpty(currentFileName)) {
    		currentFileName = "default.xml";
    		hasKey = false;
    	}
    	//save xml first, hardcode the path for now
    	//String path = "registry.xml";
		final File saveAsFile = new File(currentFileName);
		String fullPath = saveAsFile.getAbsolutePath();

		String keyFileName = null;
		if(hasKey) {
			String tmpName = saveAsFile.getName();
			int index = tmpName.lastIndexOf(".");
			if(index>0)
				tmpName = tmpName.substring(0,index);
			keyFileName = saveAsFile.getParent()+File.separator+"."+tmpName;
		}
		
		this.savePortalToFile(saveAsFile);
    	
/*
       	Log.error("running script"); 
       	String os = System.getProperty("os.name").toLowerCase();
       	//get fullpath
       	
       	String cmd = currentPath;
       	if(os.indexOf("win")>=0) 
       		cmd = cmd+"/scripts/biomart-server.bat ";
       	else
       		cmd = cmd+"/scripts/biomart-server.sh ";
  */     	
       	//save property file
    	String currentPath = ".";
    	savePropertyFile(hasKey, fullPath, keyFileName, currentPath); 

       	//start the shellscript using thread
       	ShellThread st = new ShellThread();
       	st.start();
       	
       	//get the port
       	try{
	        Properties props = new Properties();
	        String propsPath = currentPath+"/biomart.properties";
	        props.load(new FileInputStream(propsPath));
	        String host = props.getProperty("http.host");
	        String port = props.getProperty("http.port");
	        String url = "http://"+host+":"+port+"/";
	        int timeout = 0;
	        //listening on server port untill server is up and running
	        while(!McUtils.isHttpServerAvailable(url)){
	       	 Thread.sleep(1000);
	       	 timeout += 1000;
	       	 //for slower processors , time out set to 2 min
	       	 if(timeout > 120000){
	       		 JOptionPane.showMessageDialog(null, "Time out error! server is not started within 2 min.");
	       		 break;
	       	 }
	        }
       	}catch(Exception e){
       		e.printStackTrace();
       	}
       	/*
    	 try
         {            
             Runtime rt = Runtime.getRuntime();
             String serverStop = cmd+"stop";
             String serverStart = cmd + "start";
             Log.info(serverStop);
             Process proc2 = rt.exec(serverStop);
             int exitCode = proc2.waitFor();
             Log.info("stop exitValue: "+exitCode);
             //wait for 5 seconds
             Thread.sleep(5000);
             Log.error(serverStart);
             Process proc = rt.exec(serverStart);
             //if windows dont wait for the process to finish, cause it never will untill user stop the service
             //if(os.indexOf("win")>=0){
            	// return;
             //}
             
             InputStream stderr = proc.getErrorStream();
             InputStreamReader isr = new InputStreamReader(stderr);
             BufferedReader br = new BufferedReader(isr);
             String line = null;
             Log.error("<Info>");
             while ( (line = br.readLine()) != null)
                 Log.error(line);
             Log.error("</Info>");
          
             int exitVal = proc.waitFor();
             Log.error("start exitValue: " + exitVal);
             
             
             //commented out because of duplicate with script server start
             
             if(exitVal == 0) {
            	 String url = "http://localhost:9000";
            	 Log.info("starting web browser "+url);
                 if(java.awt.Desktop.isDesktopSupported()) {
                     java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                     if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                         java.net.URI uri = new java.net.URI(url);
                         Thread.sleep(5000);
                         desktop.browse( uri );
                     }
                 }

             }
         } catch (Throwable t)
           {
             t.printStackTrace();
           }
         Log.error("end");
         */

    }


	/**
	 * @param hasKey
	 * @param fullPath
	 * @param keyFileName
	 * @param currentPath
	 */
	private void savePropertyFile(boolean hasKey, String fullPath,
			String keyFileName, String currentPath) {
		String profile = currentPath + "/biomart.properties";
    	List<String> lines = new ArrayList<String>();
       	try {
       		BufferedReader input =  new BufferedReader(new FileReader(profile));
       		try {
       	        String line = null; //not declared within while loop
       	        /*
       	        * readLine is a bit quirky :
       	        * it returns the content of a line MINUS the newline.
       	        * it returns null only for the END of the stream.
       	        * it returns an empty String if two newlines appear in a row.
       	        */
       	        while (( line = input.readLine()) != null){
       	        	if(line.indexOf("biomart.registry.file")>=0) {
       	        		lines.add("biomart.registry.file="+fullPath+"\n");
       	        	} else if(line.indexOf("biomart.registry.key.file")>=0 && hasKey) {
       	        		lines.add("biomart.registry.key.file="+keyFileName+"\n");
       	        	}else
       	        		lines.add(line+"\n");
       	        }
       	      }
       	      finally {
       	        input.close();
       	      }
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Writer output = null;
		try {
			try {
			output = new BufferedWriter(new FileWriter(profile));
	    	for(String str: lines) 
	    		output.write(str);
			} finally {
				output.close();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

    private void requestSearch() {
    	new SearchComponentDialog(null);
    }
  
    
	public void actionPerformed(ActionEvent e) {
		String currentFileName = Settings.getProperty("currentFile");
		if(e.getActionCommand().equals("newportal")) {
			//before new portal save old xml
			if(currentFileName != null && MartController.getInstance().isRegistryChanged()){
				if(JOptionPane.showConfirmDialog(null, "Save "+currentFileName+" ?","Warning",JOptionPane.YES_NO_OPTION)
						== JOptionPane.YES_OPTION){
					requestSavePortal();
				}
			}
			try {
				requestLoadPortal(true);
			} catch (MartBuilderException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}else if(e.getActionCommand().equals("openportal")) {
			//before open portal save old xml
			if(currentFileName != null && MartController.getInstance().isRegistryChanged()){
				if(JOptionPane.showConfirmDialog(null, "Save "+currentFileName+" ?","Warning",JOptionPane.YES_NO_OPTION)
						== JOptionPane.YES_OPTION){
					requestSavePortal();
				}
			}
			try {
				requestLoadPortal(false);
			} catch (MartBuilderException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}else if(e.getActionCommand().equals("importportal")){
			try{
				this.requestImportPortal();
			}catch(MartBuilderException e1){
				e1.printStackTrace();
			}
		}
		else if(e.getActionCommand().equals("save")) {
			requestSavePortal();
		}else if(e.getActionCommand().equals("saveportalas")) {
			requestSaveAsPortal(true);
		}else if(e.getActionCommand().equals("deployportal")) {
			requestDeploy(true);
		}else if(e.getActionCommand().equals("validateportal")) {
			ValidationStatus validate = (new TreeNodeHandler()).requestValidateRegistry();
			switch(validate) {
			case VALID:
				JOptionPane.showMessageDialog(null,
					    "Valid.", 
					    "Message",JOptionPane.INFORMATION_MESSAGE);
				break;
			case INVALID:
				break;
			default:
				JOptionPane.showMessageDialog(null,
					    "With warning",
					    "warning",
					    JOptionPane.WARNING_MESSAGE);
				break;
			}
		}else if(e.getActionCommand().equals("search")) {
			requestSearch();
		}else if(e.getActionCommand().equals("hidehiddenmart")) {
			boolean isSelected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			Settings.setProperty("hidehiddenmart", isSelected?"1":"0");		
			this.synchronizeAllMarts();
		}else if(e.getActionCommand().equals("hidehiddencontainer")) {
			boolean isSelected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			Settings.setProperty("hidehiddencontainer", isSelected?"1":"0");
			this.synchronizeAllMarts();
		}else if(e.getActionCommand().equals("hidehiddenattribute")) {
			boolean isSelected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			Settings.setProperty("hidehiddenattribute", isSelected?"1":"0");
			this.synchronizeAllMarts();
		}else if(e.getActionCommand().equals("hidehiddenfilter")) {
			boolean isSelected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			Settings.setProperty("hidehiddenfilter", isSelected?"1":"0");
			this.synchronizeAllMarts();
		}else if(e.getActionCommand().equals("cleanoptions")) {
			this.cleanoptions();
		}else if(e.getActionCommand().equals("adduser")) {
			this.addUser();
		}else if(e.getActionCommand().equals("public")) {
			boolean isSelected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			Settings.setProperty("public", isSelected?"1":"0");
		}else if(e.getActionCommand().equals("impexp.hide")) {
			boolean isSelected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			Settings.setProperty("impexp.hide", Boolean.toString(isSelected));	
			McGuiUtils.INSTANCE.setFilterMap();
		}else if(e.getActionCommand().equals("cleanxml")) {
			MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
			List<MartPointer> mpList = registry.getPortal().getRootGuiContainer().getAllMartPointerListResursively();
			List<Config> removedConfig = new ArrayList<Config>();
			for(Mart mart: registry.getMartList()) {
				removedConfig.clear();
				for(Config config: mart.getConfigList()) {
					if(config.isMasterConfig()) 
						continue;
					//check references
					boolean isreferenced = false;
					for(MartPointer mp: mpList) {
						if(mp.getConfig().equals(config)) {
							isreferenced = true;
							break;
						}
					}
					if(!isreferenced) {
						removedConfig.add(config);
					}
				}
				if(!removedConfig.isEmpty()) {
					for(Config config: removedConfig) {
						Log.error("remove config "+config.getName() + " in mart "+mart.getName());
						mart.removeConfig(config);
					}
				}
			}			
		}else if(e.getActionCommand().equals("test")) {
			//clean
			
		}else if(e.getActionCommand().equals("exit")){
			//save setting
			Settings.save();
			if(MartController.getInstance().isRegistryChanged()) {
				int n = JOptionPane.showConfirmDialog(
					    null,
					    "Registry has been modified. Save changes?",
					    "Question", 
					    JOptionPane.YES_NO_CANCEL_OPTION);
				if(n==0) {
					//yes
					McMenus.getInstance().requestSavePortal();
					Runtime.getRuntime().exit(0);
				}else if(n==2) {
					//cancel do nothing
				}else {
					Runtime.getRuntime().exit(0);
				}
			}else {
				Runtime.getRuntime().exit(0);
			}
			
		}
			
		
	}
	
    /**
     * for setting hide/unhide value
     * if hide marts, remove all hidden mart nodes
     */
    private void synchronizeAllMarts() {
    	//remove all mart nodes
		McTreeNode root = (McTreeNode)this.lTree.getModel().getRoot();
		List<McTreeNode> martNodes = new ArrayList<McTreeNode>();
    	for(int i=0; i<root.getChildCount(); i++) {
    		McTreeNode node = (McTreeNode)root.getChildAt(i);
    		if(node.getObject() instanceof Mart) {
    			martNodes.add(node);
    		}
    	}
    	for(McTreeNode node: martNodes) {
    		node.removeFromParent();
    	}
    	List<Mart> martList = McGuiUtils.INSTANCE.getRegistryObject().getMartList();
    	root.addMartNodes(martList);
    	lTree.getModel().nodeStructureChanged(root);
    }


    public JMenu getMenuByName(String name) {
    	for(int i=0; i<this.menuBar.getMenuCount(); i++) {
    		if(name.equals(this.menuBar.getMenu(i).getText())) {
    			return this.menuBar.getMenu(i);
    		}
    	}
    	return null;
    }
    
    public JMenuItem getMenuItemByName(JMenu menu, String itemName) {
    	for(int i=0; i<menu.getItemCount(); i++) {
    		if(itemName.equals(menu.getItem(i).getText())) {
    			return menu.getItem(i);
    		}
    	}
    	return null;
    }

    private void initFileMenu() {
    	final JMenu fileMenu = this.menuBar.getMenu(0);
		firstRecentFileEntry = fileMenu.getMenuComponentCount();
    	fileMenu.addMenuListener(new MenuListener() {

			public void menuCanceled(MenuEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void menuDeselected(MenuEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void menuSelected(MenuEvent arg0) {
				// Wipe from the separator to the last non-separator/
				// non-numbered entry.
				// Then, insert after the separator a numbered list
				// of recent files, followed by another separator if
				// the list was not empty.
				
				
				while (fileMenu.getMenuComponentCount() > firstRecentFileEntry)
					fileMenu.remove(fileMenu
							.getMenuComponent(firstRecentFileEntry));

			   	final List<String> files = Settings.getHistoryNamesForClass(McMenus.class);
		    	//should only one item
		    	int position = 1;
		    	for(String fileName: files) {
		    		if(position==1)
		    			fileMenu.addSeparator();
		    		final File location = new File((String) Settings
							.getHistoryProperties(McMenus.class, fileName)
							.get("location"));
		    		
					final JMenuItem file = new JMenuItem(position++ + " "+ fileName);
					fileMenu.add(file);
					file.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							String currentFileName = Settings.getProperty("currentFile");
							if(currentFileName != null && MartController.getInstance().isRegistryChanged()){
								if(JOptionPane.showConfirmDialog(null, "Save "+currentFileName+" ?","Warning",JOptionPane.YES_NO_OPTION)
										== JOptionPane.YES_OPTION){
									requestSavePortal();
								}
							}
							try {
								McMenus.this.loadPortal(false, location);
							} catch (MartBuilderException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}			
					});
		    	}
				
			}
    		
    	});
     }

    public void cleanoptions() {
    	MartRegistry rc = McGuiUtils.INSTANCE.getRegistryObject();
    	Element optionElement = Options.getInstance().getOptionRootElement();
    	List<Element> martElementList = optionElement.getChildren(XMLElements.MART.toString());
    	List<Element> removedElement = new ArrayList<Element>();
    	for(Iterator<Element> it = martElementList.iterator(); it.hasNext();)  {
    		Element martElement = it.next();
    		String name = martElement.getAttributeValue("name");
    		Mart mart = rc.getMartByName(name);
    		if(mart==null) 
    			removedElement.add(martElement);
    		else {
    			//check dataset
    			List<Element> configElementList = martElement.getChildren(XMLElements.CONFIG.toString());
    			for(Element configElement: configElementList) {
    				List<Element> filterElementList = configElement.getChildren(XMLElements.FILTER.toString());
    				for(Element filterElement: filterElementList) {
    					//does it include dataset?
    					List<Element> dsElementList = filterElement.getChildren(XMLElements.DATASET.toString());
    					if(dsElementList.isEmpty() && !filterElement.getChildren().isEmpty()) {
    						//it is an old format from backwordcompatibility
    						//need to change to the new format
    						List<Element> rowList = filterElement.getChildren();

    						//add datasets
    						for(Dataset ds: mart.getDatasetList()) {
    							Element dsElement = new Element(XMLElements.DATASET.toString());
    							dsElement.setAttribute(XMLElements.NAME.toString(),ds.getName());
    							filterElement.addContent(dsElement);
    							for(Element rowElement: rowList) {
    								Element newRow = (Element)rowElement.clone();
    								newRow.detach();
    								dsElement.addContent(newRow);
    							}
    						}
    						//remove row
    						filterElement.removeChildren(XMLElements.ROW.toString());

    					}
    				}
    			}
    			
    		}
    	}//end of for(Iterator<Element> it = martElementList.iterator(); it.hasNext();)  
    	for(Element remove: removedElement) {
    		optionElement.removeContent(remove);
    	}
    }
    
    private void addUser() {
    	Users users = McGuiUtils.INSTANCE.getRegistryObject().getPortal().getUsers();
		AddUserDialog aud = new AddUserDialog(users);
		UserGroup newUser = aud.getUser();
		if(newUser!=null) {
			users.addUserGroup(newUser);
		}
		//update viewportal
		((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).
			showPortal((Portal)users.getParent());
    }

    private void export() {
    	
    }
}
