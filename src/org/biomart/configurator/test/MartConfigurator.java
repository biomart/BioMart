
package org.biomart.configurator.test;

import net.infonode.docking.DockingWindow;
import net.infonode.docking.DockingWindowAdapter;
import net.infonode.docking.OperationAbortedException;
import net.infonode.docking.RootWindow;
import net.infonode.docking.SplitWindow;
import net.infonode.docking.View;
import net.infonode.docking.mouse.DockingWindowActionMouseButtonListener;
import net.infonode.docking.properties.RootWindowProperties;
import net.infonode.docking.theme.*;
import net.infonode.docking.util.DockingUtil;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.view.gui.LongProcess;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.model.McModel;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McIcon;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.McView;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.configurator.view.menu.McMenus;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class MartConfigurator {
  
	private String resourcesLocation = "org/biomart/configurator/resources";
	private McModel model;
	
  
	/**
   	* The one and only root window
   	*/
  	private RootWindow rootWindow;
    private java.util.List<JMenuItem> viewItems;

  	/**
  	 * The currently applied docking windows theme
  	 */
  	private DockingWindowsTheme currentTheme = new ShapedGradientDockingTheme();

  	/**
 	* In this properties object the modified property values for close buttons etc. are stored. This object is cleared
 	*  * when the theme is changed.
	*/
  	private RootWindowProperties properties = new RootWindowProperties();


  	/**
   	* The application frame
   */
  	private JFrame frame = new JFrame(Resources.get("APPLICATIONTITLE"));

	public MartConfigurator() {	
		this.initSettings();
		this.createRootWindow();
		this.setDefaultLayout();
		this.showFrame();
	}

  /**
   	* Creates a view component containing the specified text.
   *
   * @param text the text
   * @return the view component
   */
	private JComponent createViewComponent(boolean def) {
	  if(def)
		  return new JPanel(new GridLayout(1,1));
	  else
		  return new JPanel(new CardLayout());
  }


  /**
   	* Creates the root window and the views.
   */
  	private void createRootWindow() {
    // Create the views
	  model = new McModel();
	  McViews mcViews = McViews.getInstance();

	  mcViews.addView(new McViewPortal("Portal", McIcon.VIEW_ICON, createViewComponent(true),model,IdwViewType.PORTAL));
	  mcViews.addView(new McViewSourceGroup("Source", McIcon.VIEW_ICON, createViewComponent(true),model,IdwViewType.SOURCEGROUP));
	 // mcViews.addView(new McViewSourceGroup("Source", McIcon.VIEW_ICON, createViewComponent(true),model,IdwViewType.SOURCEGROUP));
//	  mcViews.addView(new McViewConfig("Config", McIcon.VIEW_ICON, createViewComponent(true),model,IdwViewType.CONFIG));
//	  mcViews.addView(new McViewAttTable("Attribute Table", McIcon.VIEW_ICON, createViewComponent(true),model,IdwViewType.ATTRIBUTETABLE));
//	  mcViews.addView(new McViewDiagram("Diagram", McIcon.VIEW_ICON, createViewComponent(true), model, IdwViewType.DIAGRAM));
	  
	  this.viewItems = new ArrayList<JMenuItem>();
	  rootWindow = DockingUtil.createRootWindow(mcViews.getViewMap(), null, true);
	  McGuiUtils.INSTANCE.setRootWindow(rootWindow);
	    // Set gradient theme. The theme properties object is the super object of our properties object, which
	    // means our property value settings will override the theme values
	  properties.addSuperObject(currentTheme.getRootWindowProperties());
	
	    // Our properties object is the super object of the root window properties object, so all property values of the
	    // theme and in our property object will be used by the root window
	  rootWindow.getRootWindowProperties().addSuperObject(properties);
	  rootWindow.getRootWindowProperties().getDockingWindowProperties().setCloseEnabled(false);
	  rootWindow.getRootWindowProperties().getDockingWindowProperties().setMinimizeEnabled(false);
	  rootWindow.getRootWindowProperties().getDockingWindowProperties().setMaximizeEnabled(false);
	  rootWindow.getRootWindowProperties().getDockingWindowProperties().setUndockEnabled(false);
	  rootWindow.getRootWindowProperties().getDockingWindowProperties().setDragEnabled(false);
	  //rootWindow.getRootWindowProperties().getDockingWindowProperties().getTabProperties().getTitledTabProperties().setEnabled(false);
	
	    // Enable the bottom window bar
	//    rootWindow.getWindowBar(Direction.DOWN).setEnabled(true);
	
	    // Add a listener which shows dialogs when a window is closing or closed.
	  rootWindow.addListener(new DockingWindowAdapter() {
	      public void windowAdded(DockingWindow addedToWindow, DockingWindow addedWindow) {
	        updateViews(addedWindow, true);
	      }
	
	      public void windowRemoved(DockingWindow removedFromWindow, DockingWindow removedWindow) {
	        updateViews(removedWindow, false);
	      }
	
	      public void windowClosing(DockingWindow window) throws OperationAbortedException {
//	        if (JOptionPane.showConfirmDialog(frame, "Really close window '" + window + "'?") != JOptionPane.YES_OPTION)
//	          throw new OperationAbortedException("Window close was aborted!");
	      }
	
	    });
	
	    // Add a mouse button listener that closes a window when it's clicked with the middle mouse button.
	  	rootWindow.addTabMouseButtonListener(DockingWindowActionMouseButtonListener.MIDDLE_BUTTON_CLOSE_LISTENER);
  	}

  /**
   	* Update view menu items and dynamic view map.
   *
   * @param window the window in which to search for views
   * @param added  if true the window was added
   */
  	private void updateViews(DockingWindow window, boolean added) {
    if (window instanceof View) {
	  //update the viewmenu
	  for(McView mcView: McViews.getInstance().getAllViews().values()) {
		  if(mcView == window) {
			  for(JMenuItem mi: this.viewItems) {
				  if(mi.getText().equals(mcView.getTitle())) {
					  mi.setEnabled(!added);
					  break;
				  }
			  }
		  }
      }
      
   }
    else {
      for (int i = 0; i < window.getChildWindowCount(); i++)
        updateViews(window.getChildWindow(i), added);
    }
  }

  /**
   	* Sets the default window layout.
   */
  	private void setDefaultLayout() {
	  Map<IdwViewType,McView> map = McViews.getInstance().getAllViews();
	  View[] views = (View[])map.values().toArray(new View[map.size()]);
	  
	  //TabWindow tabWindow = new TabWindow(views);
    
	  //change source window to the left and portal window to the right  
//	  SplitWindow swPortalSource = new SplitWindow(true, 0.33f, tabWindow, new TabWindow(new View[]{map.get(IdwViewType.PORTAL)}));
	  SplitWindow swPortalSource = new SplitWindow(true, 0.33f, map.get(IdwViewType.SOURCEGROUP), map.get(IdwViewType.PORTAL));
//	  SplitWindow swPortalSource = new SplitWindow(false, 0.5f, new TabWindow(new View[]{map.get(IdwViewType.PORTAL)}),tabWindow);
//	  SplitWindow swPortalSource = new SplitWindow(true,0.33f, tabWindow, map.get(IdwViewType.PORTAL));
//	  SplitWindow swConfig = new SplitWindow(true,0.1f,swPortalSource,swConfigAttTable);

	  rootWindow.setWindow(swPortalSource);
    
	  //close search view by default
//	  McViews.getInstance().getView(IdwViewType.CONFIG).close();
//	  McViews.getInstance().getView(IdwViewType.ATTRIBUTETABLE).close();
//	  McViews.getInstance().getView(IdwViewType.DIAGRAM).close();
  	}

	  /**
	   	* Initializes the frame and shows it.
	 * @throws MartBuilderException 
	   */
  	private void showFrame() {
  		
		//McMenus menusGUI = McMenus.getInstance();
  		// modified to have JFrame reference in menu GUI
  		McMenus menusGUI = McMenus.getInstance();
  		
	    frame.getContentPane().add(rootWindow, BorderLayout.CENTER);
	    //set customized menu
	    JMenuBar menuBar = menusGUI.getMainMenu();
	    // menuBar.add(this.createViewMenu());
	    frame.setJMenuBar(menuBar);
	    
	    GraphicsEnvironment graphicsEnvironment=GraphicsEnvironment.getLocalGraphicsEnvironment();		
	    //get maximum window bounds
	    Rectangle maximumWindowBounds=graphicsEnvironment.getMaximumWindowBounds();
	    frame.setMaximizedBounds(maximumWindowBounds);
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    frame.setSize(screenSize);
	    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	    frame.addWindowListener(new WindowAdapter(){
	    	public void windowClosing(final WindowEvent e) {
				if (e.getWindow() == frame) {
					//save setting
					Settings.save();
					if(MartController.getInstance().isRegistryChanged()) {
						int n = JOptionPane.showConfirmDialog(
							    frame,
							    "Registry has been modified. Save changes?",
							    "Question", 
							    JOptionPane.YES_NO_CANCEL_OPTION);
						if(n==0) {
							//yes
							McMenus.getInstance().requestSavePortal();
							frame.dispose();
							System.exit(0);
						}else if(n==2) {
							//cancel do nothing
						}else {
							frame.dispose();
							System.exit(0);
						}
					}else {
						frame.dispose();
						System.exit(0);
					}
				}
	    	}
	    });
	    //default new portal
	    System.setProperty("api", "0");
	    try {
			menusGUI.requestLoadPortal(true);
		} catch (MartBuilderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    frame.setVisible(true);
	}

  	private JMenu createViewMenu() {
  		JMenu menu = new JMenu("Views");
 
  	    for (final McView view : McViews.getInstance().getAllViews().values()) {
  	      JMenuItem mItem = new JMenuItem(view.getTitle());
  	      this.viewItems.add(mItem);
  	      mItem.setEnabled(view.getRootWindow() == null);
  	      menu.add(mItem).addActionListener(new ActionListener() {
  	        public void actionPerformed(ActionEvent e) {
  	          if (view.getRootWindow() != null)
  	            view.restoreFocus();
  	          else {
  	            DockingUtil.addWindow(view, rootWindow);
  	          }
  	        }
  	      });
  	    }
  	    return menu;
  	}
  	

  	public static void initForWeb() {
  		Resources.setResourceLocation("org/biomart/configurator/resources");  	
        Settings.loadWebConfigProperties();
        Settings.loadAllConfigProperties();
  	}

	
	private void initSettings() {
		Settings.setApplication(Settings.MARTCONFIGURATOR);
		Log.info("..." + Settings.getApplication() + " started.");
		Resources.setResourceLocation(resourcesLocation);

		// Attach ourselves as the main window for hourglass use.
		LongProcess.setMainWindow(this.frame);

		// Load our cache of settings.
		Settings.load();

		// Set the look and feel to the one specified by the user, or the system
		// default if not specified by the user. This may be null.
/*		Log.info("Loading look-and-feel settings");
		String lookAndFeelClass = Settings.getProperty("lookandfeel");
		try {
			UIManager.setLookAndFeel(lookAndFeelClass);
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (final Exception e) {
			// Ignore, as we'll end up with the system one if this one doesn't
			// work.
			if (lookAndFeelClass != null)
				// only worry if we were actually given one.
				Log.warn("Bad look-and-feel: " + lookAndFeelClass, e);
			// Use system default.
			lookAndFeelClass = UIManager.getSystemLookAndFeelClassName();
			try {
				UIManager.setLookAndFeel(lookAndFeelClass);
			} catch (final Exception e2) {
				// Ignore, as we'll end up with the cross-platform one if there
				// is no system one.
				Log.warn("Bad look-and-feel: " + lookAndFeelClass, e2);
			}
		}*/
		Settings.loadGUIConfigProperties();
		Settings.loadAllConfigProperties();
		//set filterMap
		McGuiUtils.INSTANCE.setFilterMap();
  }
	
  	

	public static void main(String[] args) throws Exception {
		//set mac look and feels
		String os = System.getProperty("os.name").toLowerCase();
       	if(os.indexOf("mac")>=0) 
       	{
       		//-Dcom.apple.macos.useScreenMenuBar=true -Xdock:name="Mart Configurator"
       		System.setProperty("apple.laf.useScreenMenuBar", "true");
       		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Mart Configurator");
       	}
       	// Docking windows should be run in the Swing thread
	    SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
	        new MartConfigurator();
	      }
	    });
	}

  
}
