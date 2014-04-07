package org.biomart.configurator.view.idwViews;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.McEvent;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.McEventListener;
import org.biomart.common.utils.WeakPropertyChangeSupport;
import org.biomart.configurator.model.McModel;
import org.biomart.configurator.model.object.ObjectCopy;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.UserGroup;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.view.McView;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.configurator.view.component.container.ButtonTabComponent;
import org.biomart.configurator.view.component.container.DnDTabbedPane;
import org.biomart.configurator.view.component.container.GuiContainerPanel;
import org.biomart.configurator.view.component.container.PlusTabComponent;
import org.biomart.configurator.view.gui.dialogs.UserManagementDialog;
import org.biomart.configurator.view.menu.McMenus;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.Portal;
import org.biomart.objects.portal.User;
import org.biomart.objects.portal.Users;


public class McViewPortal extends McView implements ActionListener {

	private JButton deployButton;
	private boolean serverStarted = false;
	private JButton addUserButton;
	private JButton umButton;
	private JComboBox usersCB;
	private DnDTabbedPane tp;
	private WeakPropertyChangeSupport pcs;
	//TODO should use weakreference
	private Map<Mart,Set<ConfigComponent>> components4Mart;
	//TODO tmp
	private List<ObjectCopy> objectCopies;
	private Map<MartPointer,Set<Mart>> configMartMap;

 	public McViewPortal(String title, Icon icon, Component component,
			McModel model, IdwViewType type) {
		super(title, icon, component, model, type);
		this.add(this.createToolBar(),BorderLayout.NORTH);
		this.components4Mart = new HashMap<Mart,Set<ConfigComponent>>();
		this.pcs = new WeakPropertyChangeSupport(this);
		this.configMartMap = new HashMap<MartPointer,Set<Mart>>();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public void enableControls(boolean enable) {
		deployButton.setEnabled(enable);
		addUserButton.setEnabled(enable);
		umButton.setEnabled(enable);
		usersCB.setEnabled(enable);

		tp.setEnabled(enable);
		for(int i=0; i<tp.getComponentCount(); i++) {
			if(tp.getComponent(i) instanceof GuiContainerPanel)
			{
				GuiContainerPanel gcp = (GuiContainerPanel)tp.getComponent(i);
				boolean hasConfig = (McGuiUtils.INSTANCE.getRegistryObject().getPortal().getRootGuiContainer().getAllMartPointerListResursively().size()>0);
				if(gcp.getGuiContainer().getName().equals("report") && !hasConfig)
				{
					gcp.enableControls(false);				
				}else{
					//disable all the controls in the tabpane
					gcp.enableControls(enable);
				}
			}
		}
		this.pcs.firePropertyChange(McEventProperty.ENABLE.toString(), null, enable);
	}
	
  	private JPanel createToolBar() {
  		JPanel toolBarsPanel = new JPanel(new BorderLayout());
  		
  		JToolBar buttonBar = new JToolBar();	
  		JPanel eastPanel = new JPanel();
  		deployButton = new JButton("Start Server",McUtils.createImageIcon("images/run.gif"));
  		deployButton.setToolTipText(Resources.get("DEPLOY"));
  		deployButton.setActionCommand(Resources.get("DEPLOY"));
  		deployButton.addActionListener(this);
  		
  		//JCheckBox hidemaskedcb = new JCheckBox("Hide Masked Config");
  		//addShowHideCheckBox(eastPanel);
  		eastPanel.add(deployButton);
  		
  		JToolBar dropdownBar = new JToolBar();
  		JLabel userLabel = new JLabel("User group");
  		usersCB = new JComboBox();
  		usersCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					final UserGroup user = (UserGroup)McViewPortal.this.usersCB.getSelectedItem();
					selectUser(user);
				}
			}  			
  		});
  		dropdownBar.add(userLabel);
  		dropdownBar.add(usersCB);
  		dropdownBar.addSeparator();
  		
  		this.addUserButton = new JButton(McUtils.createImageIcon("images/adduser.png"));
  		this.addUserButton.setToolTipText("addusergroup");
  		this.addUserButton.setActionCommand("addusergroup");
  		this.addUserButton.addActionListener(this);
  		this.addUserButton.setVisible(false);
  		
  		this.umButton = new JButton(McUtils.createImageIcon("images/um.gif"));
  		this.umButton.setToolTipText("user management");
  		this.umButton.setActionCommand("usermanagement");
  		this.umButton.addActionListener(this);
  		//dropdownBar.add(this.addUserButton);
  		JPanel spacePanel = new JPanel();
  		
  		//toolBarsPanel.add(dropdownBar);
  		buttonBar.add(userLabel);
  		buttonBar.add(usersCB);
  		buttonBar.addSeparator();
  		buttonBar.add(this.addUserButton);
  		buttonBar.add(this.umButton);
  		toolBarsPanel.add(buttonBar,BorderLayout.WEST);

  		toolBarsPanel.add(spacePanel,BorderLayout.CENTER);
  		toolBarsPanel.add(eastPanel,BorderLayout.EAST);
  		//toolBarsPanel.add(dropdownBar,BorderLayout.CENTER);
	    return toolBarsPanel;
  	}

	
	public JButton getDeployButton() {
		return deployButton;
	}

	public void clearUsers() {
		this.usersCB.removeAllItems();
	}
	
	public void showPortal(Portal portal) {
		GuiContainer rootGc = portal.getRootGuiContainer();
		this.createRootGuiTabs(rootGc);
		this.showCard(tp);
		this.reloadUsers(portal.getUsers());
		
		// disable all controls if source view has no source
		McViewSourceGroup sourceView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		this.enableControls(sourceView.hasSource());
	}
	
	private void reloadUsers(Users users) {
		this.usersCB.removeAllItems();
		for(UserGroup user: users.getUserList()) {
			this.usersCB.addItem(user);
		}		
	}
	
	private void selectUser(UserGroup user) {
		this.pcs.firePropertyChange(McEventProperty.USER.toString(), null, user);
	}
	
	private void showCard(final JComponent card) {
		if (card == null) {
			return;
		}
		final JPanel panel = (JPanel) this.getComponent();
		panel.removeAll();
		panel.add(card);
		panel.revalidate();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() instanceof JButton) {
			//if(e.getActionCommand().equals("Deploy")) {
			if(e.getActionCommand().equals(Resources.get("DEPLOY"))) {
				if(!serverStarted){
					final String currentFileName = Settings.getProperty("currentFile");
			    	if(McUtils.isStringEmpty(currentFileName)){
			    		int retval = McMenus.getInstance().requestSaveAsPortal(false);
			    		if(retval == JFileChooser.CANCEL_OPTION)
			    			return;
			    	}
				}
				serverStarted = !serverStarted;
				if(serverStarted){
					McMenus.getInstance().requestDeploy(serverStarted);
					this.deployButton.setText("Stop Server");
					this.deployButton.setIcon(McUtils.createImageIcon("images/stop.gif"));
				}else{
					McMenus.getInstance().requestDeploy(serverStarted);
					this.deployButton.setText("Start Server");
					this.deployButton.setIcon(McUtils.createImageIcon("images/run.gif"));
				}
				//McMenus.getInstance().requestDeploy();
			}else if(e.getActionCommand().equals("addusergroup")) {
				addUser();
			}else if(e.getActionCommand().equals("usermanagement")) {
		    	Users users = (Users)this.getUser().getParent();
		    	//add to the listener
		    	McEventBus.getInstance().addListener(McEventProperty.USERGROUPCHANGED.toString(), this);
		    	UserManagementDialog umd = new UserManagementDialog(users);
		    	//remove from listener
		    	McEventBus.getInstance().removeListener(McEventProperty.USERGROUPCHANGED.toString(), this);
			}
		} else if(e.getActionCommand().equals("hidemaskedmp")) {
			boolean isSelected = ((JCheckBox)e.getSource()).isSelected();
			Settings.setProperty("hidemaskedmp", Boolean.toString(isSelected));
			this.refreshTabbedPane(tp);
		}
	}
	
	public UserGroup getUser() {
		if(this.usersCB.getSelectedItem() == null)
			return null;
		else
			return (UserGroup)this.usersCB.getSelectedItem();				
	}
	
	public void refresh() {
		this.selectUser(this.getUser());
	}

    private void addUser() {
    	Users users = McGuiUtils.INSTANCE.getRegistryObject().getPortal().getUsers();
    	String groupName = JOptionPane.showInputDialog("group name");
    	if(!McUtils.isStringEmpty(groupName)) {
    		//check exists
    		if(users.getUserGroupByName(groupName)!=null) {
    			JOptionPane.showMessageDialog(this, "group name conflict", "error",JOptionPane.ERROR_MESSAGE);
    			return;
    		}
    		UserGroup newUg = new UserGroup(groupName,groupName,"");
    		User user = new User(groupName,groupName,"");
    		newUg.addUser(user);
    		users.addUserGroup(newUg);
    		this.usersCB.addItem(newUg);
    		this.usersCB.setSelectedItem(newUg);   		
    	}
    }
    public void addPropertyChangeListener(String property, PropertyChangeListener pcl) {
    	this.pcs.addPropertyChangeListener(property,pcl);
    }
    	
	private void createRootGuiTabs(GuiContainer rootGC) {
		tp = new DnDTabbedPane(rootGC);
		this.createSubGCTabs(rootGC, tp);		
	}
	
	private DnDTabbedPane getTabbedPaneByGC(DnDTabbedPane parent, GuiContainer gc) {
		int count = parent.getTabCount();
		for(int i=0; i<count; i++) {
			Component c = parent.getTabComponentAt(i);
			if(c instanceof DnDTabbedPane ) {
				if(((DnDTabbedPane)c).getGuiContainer().equals(gc)) {
					return (DnDTabbedPane)c;
				} else {
					DnDTabbedPane tmp = getTabbedPaneByGC((DnDTabbedPane)c, gc);
					if(tmp!=null)
						return tmp;
				}
			}
		}
		return null;
	}
	
	public List<Integer> getSelectedGcPanelPath(){
		List<Integer> path = new ArrayList<Integer>();
		int selIndex = this.tp.getSelectedIndex();
		DnDTabbedPane curPane = this.tp;
		while(curPane.getSelectedIndex() >= 0){
			path.add(new Integer(selIndex));
			Component c = curPane.getSelectedComponent();
			if(c instanceof DnDTabbedPane)
			{
				curPane = (DnDTabbedPane) c;
				selIndex = curPane.getSelectedIndex();
			}else
				break;
		}
		return path;
	}
	public List<Integer> getTabIndexPath(GuiContainer gc) {
		List<Integer> path = new ArrayList<Integer>();
		try{
			DnDTabbedPane pane = this.getTabbedPaneByGC(this.tp, gc);
			DnDTabbedPane parent = (DnDTabbedPane)pane.getParent();
			while(parent!=null && !parent.equals(this.tp) && (parent instanceof DnDTabbedPane)) {
				int i = this.getTabIndex(parent, pane);
				path.add(i);
				pane = parent;
				parent = (DnDTabbedPane)pane.getParent();
			}
			Collections.reverse(path);
			return path;
		}catch(NullPointerException npe){
			//npe.printStackTrace();
			return path;
		}
		
	}
	
	public void setSelectedTabIndex(List<Integer> selIndexes) {
		DnDTabbedPane curTab = this.tp;
		for(Integer i : selIndexes) {
			int selIndex = i.intValue();
			curTab.setSelectedIndex(selIndex);
			Component c = curTab.getSelectedComponent();
			if(c instanceof DnDTabbedPane){
				curTab = (DnDTabbedPane) c;
			}else {
				break;
			}
		}
	}
	
	private int getTabIndex(DnDTabbedPane parent, DnDTabbedPane pane) {
		for(int i=0; i<parent.getTabCount(); i++) {
			if(parent.getTabComponentAt(i).equals(pane)) {
				return i;
			}
		}
		return -1;
	}

	public GuiContainerPanel getPanelByGC(GuiContainer gc) {
		return null;
	}

	private void createSubGCTabs(GuiContainer gc, DnDTabbedPane parent) {
		if(gc.isLeaf()) {
			GuiContainerPanel panel = new GuiContainerPanel(gc);
			parent.add(gc.getDisplayName(),panel);	

			int index = parent.indexOfTab(gc.getDisplayName());
			if(index>=0) {
				ButtonTabComponent btc = new ButtonTabComponent(parent,gc);
				this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(), btc);
				this.pcs.addPropertyChangeListener(McEventProperty.ENABLE.toString(),btc);
				this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(), panel);
				parent.setTabComponentAt(index, btc);
			}
			
			for(MartPointer mp: gc.getMartPointerList()) {
				if(mp.getConfig().isHidden() && Boolean.parseBoolean(Settings.getProperty("hidemaskedmp"))) 
					continue;

				ConfigComponent cc = new ConfigComponent(mp,this.getUser());
				this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(), cc);
				panel.addConfig(cc);				
			}				
		}else {
			for(GuiContainer subGc: gc.getGuiContainerList()) {
				if(subGc.isLeaf()) {
					GuiContainerPanel panel = new GuiContainerPanel(subGc);
					parent.add(subGc.getDisplayName(),panel);	
	
					int index = parent.indexOfTab(subGc.getDisplayName());
					if(index>=0) {
						ButtonTabComponent btc = new ButtonTabComponent(parent,subGc);
						this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(), btc);
						this.pcs.addPropertyChangeListener(McEventProperty.ENABLE.toString(),btc);
						this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(), panel);
						parent.setTabComponentAt(index, btc);
					}
					
					for(MartPointer mp: subGc.getMartPointerList()) {
						if(mp.getConfig().isHidden() && Boolean.parseBoolean(Settings.getProperty("hidemaskedmp"))) 
							continue;

						ConfigComponent cc = new ConfigComponent(mp,this.getUser());
						this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(), cc);
						panel.addConfig(cc);
						
					}				
				}else {
					DnDTabbedPane subPane = new DnDTabbedPane(subGc);
					parent.add(subGc.getDisplayName(),subPane);
	
					int index = parent.indexOfTab(subGc.getDisplayName());
					if(index>=0) {
						ButtonTabComponent btc = new ButtonTabComponent(parent,subGc);
						this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(), btc);
						this.pcs.addPropertyChangeListener(McEventProperty.ENABLE.toString(),btc);
						parent.setTabComponentAt(index,  btc);
					}
					
					this.createSubGCTabs(subGc, subPane);				
				}
			}
	
		}
		parent.add("+",new JPanel());
		int index = parent.indexOfTab("+");
		PlusTabComponent ptc = new PlusTabComponent(parent);
		this.pcs.addPropertyChangeListener(McEventProperty.ENABLE.toString(),ptc);
		parent.setTabComponentAt(index, ptc);
	}
	
	public void refreshPanel(GuiContainerPanel gcPanel) {
		gcPanel.removeAll();
		gcPanel.init();
		for(MartPointer mp: gcPanel.getGuiContainer().getMartPointerList()) {
			if(mp.getConfig().isHidden() && Boolean.parseBoolean(Settings.getProperty("hidemaskedmp"))) 
				continue;

			ConfigComponent cc = new ConfigComponent(mp,this.getUser());
			this.pcs.addPropertyChangeListener(McEventProperty.USER.toString(),cc);
			gcPanel.addConfig(cc);
			
		}
		gcPanel.revalidate();
	}
	
	public void refreshRootPane() {
		//preserve selections
		List<Integer> path = this.getSelectedGcPanelPath();
		int[] selIndexes = this.getSelectedGcPanel().getSelectedIndexes();
		this.refreshTabbedPane(this.tp);
		this.setSelectedTabIndex(path);
		this.getSelectedGcPanel().setSelectedIndexes(selIndexes);
	}
	
	public void refreshTabbedPane(DnDTabbedPane tabbedpane) {
		//remember the selected index
		int selectedindex = tabbedpane.getSelectedIndex();
		tabbedpane.removeAll();
		GuiContainer gc = tabbedpane.getGuiContainer();
		this.createSubGCTabs(gc, tabbedpane);
		//restore the selected one
		if(selectedindex>=0 && selectedindex < tabbedpane.getTabCount() - 1) 
			tabbedpane.setSelectedIndex(selectedindex);
		
		//update enable and disable controls
		McViewSourceGroup sourceView = (McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
		this.enableControls(sourceView.hasSource());
	}


	public GuiContainer getSelectedGuiContainer() {
		Component c = this.tp.getSelectedComponent();
		if(c instanceof DnDTabbedPane) {
			return this.getSelectedSubGc((DnDTabbedPane)c);
		}else if(c instanceof GuiContainerPanel){
			return ((GuiContainerPanel)c).getGuiContainer();
		}
		return null;
	}
	
	private GuiContainer getSelectedSubGc(DnDTabbedPane subTP) {
		Component c = subTP.getSelectedComponent();
		if(c instanceof DnDTabbedPane) {
			return this.getSelectedSubGc((DnDTabbedPane)c);
		}else if(c instanceof GuiContainerPanel){
			return ((GuiContainerPanel)c).getGuiContainer();
		}
		return null;
	}
	
	private GuiContainerPanel getSelectedSubGcPanel(DnDTabbedPane subTP) {
		Component c = subTP.getSelectedComponent();
		if(c instanceof DnDTabbedPane) {
			return this.getSelectedSubGcPanel((DnDTabbedPane)c);
		}else if(c instanceof GuiContainerPanel){
			return (GuiContainerPanel)c;
		}
		return null;		
	}
	
	public GuiContainerPanel getSelectedGcPanel() {
		Component c = this.tp.getSelectedComponent();
		if(c instanceof DnDTabbedPane) {
			return this.getSelectedSubGcPanel((DnDTabbedPane)c);
		}else if(c instanceof GuiContainerPanel){
			return (GuiContainerPanel)c;
		}
		return null;		
	}

	public void addConfigComponent(ConfigComponent cc) {
		Mart mart = cc.getMartPointer().getMart();
		Set<ConfigComponent> cclist = this.components4Mart.get(mart);
		if(cclist == null) {
			cclist = new HashSet<ConfigComponent>();
			this.components4Mart.put(mart, cclist);
		} else {
			//if the cc already exist, use the latest one.
			//this part is depends on the equals and hashcode of configcomponent, 
			//may change later
			if(cclist.contains(cc))
				cclist.remove(cc);

		}
		cclist.add(cc);
	}
	
	public void removeConfigComponent(ConfigComponent cc) {
		Mart mart = cc.getMartPointer().getMart();
		Set<ConfigComponent> cclist = this.components4Mart.get(mart);
		if(cclist != null) {
			cclist.remove(cc);
		}
		if(McUtils.isCollectionEmpty(cclist)) {
			this.components4Mart.remove(mart);
		}
	}
	
	public Set<ConfigComponent> getComponentListByMart(Mart mart) {
		Set<ConfigComponent> result = this.components4Mart.get(mart);
		if(result==null)
			return new HashSet<ConfigComponent>();
		return this.components4Mart.get(mart);
	}
	
	public ConfigComponent getComponentByMartPointer(MartPointer mp) {
		Set<ConfigComponent> result = this.components4Mart.get(mp.getMart());
		if(result == null)
			return null;
		for(ConfigComponent cc: result) {
			if(cc.getMartPointer().equals(mp))
				return cc;
		}
		return null;
	}
	
	public void clearCopyFlag() {
		for(Set<ConfigComponent> ccSet: this.components4Mart.values()) {
			for(ConfigComponent cc: ccSet) {
				cc.setCutCopyOperation(0);
			}
		}
	}
	
	/**
	 * store the copy/cut objects
	 * @param objects
	 */
	public void setObjectCopies(List<ObjectCopy> objects) {
		this.objectCopies = objects;
	}
	
	public List<ObjectCopy> getObjectCopies() {
		if(this.objectCopies == null)
			return new ArrayList<ObjectCopy>();
		return this.objectCopies;
	}

	public Map<MartPointer,Set<Mart>> getMPMartMap() {
		return this.configMartMap;
	}
	
	/**
	 *  to remove all references to the objects
	 */
	public void clean() {
		this.usersCB.removeAllItems();
		if(this.tp!=null) {
			this.tp.clean();
			this.tp.removeAll();			
		}
		for(Set<ConfigComponent> cc: this.components4Mart.values()) {
			for(ConfigComponent c: cc) {
				c.clean();
				c = null;
			}
			cc.clear();
		}
		this.components4Mart.clear();
		for(Set<Mart> cc: this.configMartMap.values()) {
			for(Mart mart : cc){
				mart.clean();
			}
			cc.clear();
		}
		this.configMartMap.clear();
		this.pcs.clearAll();
		McGuiUtils.INSTANCE.setRegistry(null);
	}


	@McEventListener
	public void test(McEvent<?> event) {
		Users users = McGuiUtils.INSTANCE.getRegistryObject().getPortal().getUsers();
		UserGroup selectedUser = this.getUser();
		this.reloadUsers(users);
		this.usersCB.setSelectedItem(selectedUser);	
	}
}