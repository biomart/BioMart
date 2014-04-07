package org.biomart.configurator.view.menu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.update.UpdateMart;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.treelist.LeafCheckBoxList;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.component.ConfigComponent;
import org.biomart.configurator.view.component.MartComponent;
import org.biomart.configurator.view.component.container.ActionPanel;
import org.biomart.configurator.view.component.container.ButtonTabComponent;
import org.biomart.configurator.view.component.container.DnDTabbedPane;
import org.biomart.configurator.view.component.container.GuiContainerPanel;
import org.biomart.configurator.view.component.container.SharedDataModel;
import org.biomart.configurator.view.component.container.TransferableConfig;
import org.biomart.configurator.view.gui.dialogs.AddGroupDialog;
import org.biomart.configurator.view.gui.dialogs.ConfigDialog;
import org.biomart.configurator.view.gui.dialogs.DatasourceDialog;
import org.biomart.configurator.view.gui.dialogs.EditDsNameDialog;
import org.biomart.configurator.view.gui.dialogs.LinkManagementDialog;
import org.biomart.configurator.view.gui.dialogs.OrderGroupDialog;
import org.biomart.configurator.view.gui.dialogs.PropertyDialog;
import org.biomart.configurator.view.gui.dialogs.ReorderConfigDialog;
import org.biomart.configurator.view.gui.dialogs.ReorderTabDialog;
import org.biomart.configurator.view.gui.dialogs.SaveDDLDialog;
import org.biomart.configurator.view.gui.dialogs.SchemaDialog;
import org.biomart.configurator.view.gui.dialogs.TableDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.enums.EntryLayout;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.biomart.objects.objects.SourceContainer;
import org.biomart.objects.objects.SourceContainers;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * A singleton class that read contextMenu.xml and construct the popup menu corresponding to the selected node
 * 
 * 
 */
public class ContextMenuConstructor implements ActionListener {
    private String contextMenuXML = "conf/xml/newContextMenu.xml";
    private JPopupMenu contextMenu;
    private static ContextMenuConstructor instance = null;
    private Element root;
    private Component owner;
    private boolean multiselect;

    public static ContextMenuConstructor getInstance() {
        if (instance == null)
            instance = new ContextMenuConstructor();
        return instance;
    }

    private ContextMenuConstructor() {
        contextMenu = new JPopupMenu();
        try {
            // Build the document with SAX and Xerces, no validation
            SAXBuilder builder = new SAXBuilder();
            // Create the document
            Document doc = builder.build(new File(contextMenuXML));
            root = doc.getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * if multiselect, the owner should be able to get all other selected owners
     * 
     * @param owner
     * @param type
     * @param multiselect
     * @return
     */
    public JPopupMenu getContextMenu(Component owner, String type, boolean multiselect) {
        this.owner = owner;
        this.multiselect = multiselect;
        contextMenu.removeAll();
        Element e = root.getChild(type);
        if (e == null)
            return null;

        @SuppressWarnings("unchecked")
        List<Element> menuItemElement = e.getChildren();
        if (menuItemElement == null)
            return null;
        for (Element item : menuItemElement) {
            if (item.getName().equals("Separator"))
                contextMenu.addSeparator();
            else {
                boolean shows = true;
                boolean advancedMenu = Boolean.parseBoolean(item.getAttributeValue("advanced"));
                if (advancedMenu && !Boolean.parseBoolean(Settings.getProperty("showadvancedmenu")))
                    shows = false;
                boolean enable = true;
                if (multiselect) {
                    if (!Boolean.parseBoolean(item.getAttributeValue("multiselect")))
                        enable = false;
                }

                if (shows) {
                    // get submenu
                    @SuppressWarnings("unchecked")
                    List<Element> subMenu = item.getChildren();
                    if (subMenu.size() > 0) {
                        JMenu menu = new JMenu(item.getAttributeValue("title"));
                        contextMenu.add(menu);
                        for (Element subItem : subMenu) {
                            JMenuItem mi = createMenuItem(subItem);
                            mi.setEnabled(enable);
                            menu.add(mi);
                        }
                    } else {
                        JMenuItem mi = createMenuItem(item);
                        mi.setEnabled(enable);
                        contextMenu.add(mi);
                    }
                }
            }
        }
        this.createCustomizedMenu();
        return this.contextMenu;
    }

    private void createCustomizedMenu() {
        UserGroup currentUser = ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
        if (this.owner instanceof ButtonTabComponent) {
            final Object object = ((ButtonTabComponent) owner).getParentTabPane().getSelectedComponent();
            GuiContainer gc = null;
            if (object instanceof GuiContainerPanel)
                gc = ((GuiContainerPanel) object).getGuiContainer();
            else if (object instanceof DnDTabbedPane)
                gc = ((DnDTabbedPane) object).getGuiContainer();

            JMenuItem addSubTabMenu = new JMenuItem("Add sub tab");
            addSubTabMenu.setActionCommand("newguicontainer");
            addSubTabMenu.addActionListener(this);
            addSubTabMenu.setEnabled(!gc.hasSubContainer());

            // don't add it to report page
            if (!gc.getName().equals("report")) {
                JMenu setGuiTypeMenu = new JMenu("Set GUI Type");
                for (GuiType glo : GuiType.values()) {
                    // don't add report for others
                    if (glo.equals(GuiType.get("martreport")))
                        continue;

                    JCheckBoxMenuItem gloItem = new JCheckBoxMenuItem(glo.getDisplayName());
                    gloItem.setActionCommand(glo.toString());
                    gloItem.setSelected(glo.equals(gc.getGuiType()));
                    gloItem.addActionListener(this);
                    // grey out MartAnalysis and MartSearch
                    // if(glo.equals(GuiType.get("martanalysis")) || glo.equals(GuiType.get("martsearch")))
                    // gloItem.setEnabled(false);

                    setGuiTypeMenu.add(gloItem);

                }
                this.contextMenu.add(setGuiTypeMenu);
            }

            JMenu setConfigTypeMenu = new JMenu("Set EntryLayout");
            for (EntryLayout clo : EntryLayout.values()) {
                JCheckBoxMenuItem configItem = new JCheckBoxMenuItem(clo.toString());
                configItem.setActionCommand(clo.toString());
                configItem.setSelected(clo.equals(gc.getEntryLayout()));
                configItem.addActionListener(this);
                setConfigTypeMenu.add(configItem);
            }

            JCheckBoxMenuItem activateMenu = new JCheckBoxMenuItem("Activate");
            activateMenu.setActionCommand(XMLElements.HIDE.toString());
            activateMenu.setSelected(gc.isActivatedInUser(currentUser));
            activateMenu.addActionListener(this);
            this.contextMenu.add(activateMenu);

            this.contextMenu.add(addSubTabMenu);
            // this.contextMenu.add(setConfigTypeMenu);

        } else if (this.owner instanceof GuiContainerPanel) {
            MartPointer mp = getSelectedMPfromGCP();
            // MartPointer mp = (MartPointer)((ConfigComponent)this.owner).getMartPointer();
            if (mp != null) {
                JCheckBoxMenuItem activateMenu = new JCheckBoxMenuItem("Activate");
                activateMenu.setActionCommand("activate");
                activateMenu.setSelected(mp.isActivatedInUser(currentUser));
                activateMenu.addActionListener(this);
                this.contextMenu.add(activateMenu);

                JCheckBoxMenuItem hideMenu = new JCheckBoxMenuItem();
                hideMenu.setText("Hide");
                hideMenu.setActionCommand("hide/unhide");
                hideMenu.setSelected(mp.getConfig().isHidden());
                hideMenu.addActionListener(this);

                JCheckBoxMenuItem readonlyMenu = new JCheckBoxMenuItem();
                readonlyMenu.setText("Readonly");
                readonlyMenu.setActionCommand("readonly");
                readonlyMenu.setSelected(mp.getConfig().isReadOnly());
                readonlyMenu.addActionListener(this);

                this.contextMenu.add(hideMenu);
                this.contextMenu.add(readonlyMenu);
            }
            JMenuItem groupMenu = new JMenuItem("Set Group");
            groupMenu.setActionCommand("setgroup");
            groupMenu.addActionListener(this);
            this.contextMenu.addSeparator();
            this.contextMenu.add(groupMenu);

            JMenuItem orderGroupMenu = new JMenuItem("Set Group Order");
            orderGroupMenu.setActionCommand("ordergroup");
            orderGroupMenu.addActionListener(this);
            this.contextMenu.add(orderGroupMenu);
        }

        else if (this.owner instanceof MartComponent && !multiselect) {
            MartComponent mc = (MartComponent) owner;
            final Mart mart = (Mart) mc.getMart();

            JCheckBoxMenuItem searchConfigMenu = new JCheckBoxMenuItem();
            searchConfigMenu.setText("Query Source Schema");
            searchConfigMenu.setActionCommand("searchfromsource");
            searchConfigMenu.setEnabled(mart.hasSource());
            searchConfigMenu.setSelected(!mart.searchFromTarget());
            searchConfigMenu.addActionListener(this);

            this.contextMenu.add(searchConfigMenu);

        } else if (this.owner instanceof ActionPanel) {
            ActionPanel ac = (ActionPanel) this.owner;
            final SourceContainer sc = (SourceContainer) ac.getSourceContainer();
            JCheckBoxMenuItem groupMenu = new JCheckBoxMenuItem();
            groupMenu.setText("Group");
            groupMenu.setActionCommand("group");
            groupMenu.setSelected(sc.isGrouped());
            groupMenu.addActionListener(this);
            this.contextMenu.add(groupMenu);

            JMenuItem deleteMenu = new JMenuItem("Delete");
            deleteMenu.setActionCommand("deletesourcecontainer");
            boolean canDelete = sc.getMartList().size() == 0;
            deleteMenu.setEnabled(canDelete);
            deleteMenu.addActionListener(this);
            this.contextMenu.add(deleteMenu);
        }

    }

    /**
     * @return
     */
    private MartPointer getSelectedMPfromGCP() {
        GuiContainerPanel gcp = (GuiContainerPanel) owner;
        int index = -1;
        if (gcp.getConfigList().getSelectedValue() != null)
            index = gcp.getConfigList().getSelectedIndex();
        else if (gcp.getConfigTable().getSelectedRowCount() > 0)
            index = gcp.getConfigTable().getSelectedRow();
        try {
            MartPointer mp = (MartPointer) ((SharedDataModel) gcp.getConfigList().getModel()).elementAt(index);
            return mp;
        } catch (ArrayIndexOutOfBoundsException exp) {
            return null;
        }
    }

    private boolean isMenuDisabled(Element element) {
        String disabled = element.getAttributeValue("disabled");
        String name = element.getAttributeValue("name");

        if ("1".equals(disabled))
            return true;
        if (owner instanceof ButtonTabComponent) {
            final Object object = ((ButtonTabComponent) owner).getParentTabPane().getSelectedComponent();
            GuiContainer gc = null;
            if (object instanceof GuiContainerPanel)
                gc = ((GuiContainerPanel) object).getGuiContainer();
            else if (object instanceof DnDTabbedPane)
                gc = ((DnDTabbedPane) object).getGuiContainer();
            if (gc.getName().equals("report") && name.equals("remove")) {
                return true;
            }
        }

        return false;
    }

    private JMenuItem createMenuItem(Element element) {
        JMenuItem menuItem = null;
        String type = element.getAttributeValue("type");
        // checkbox
        if ("checkbox".equals(type)) {
            menuItem = new JCheckBoxMenuItem(element.getAttributeValue("title"));
        } else {
            menuItem = new JMenuItem(element.getAttributeValue("title"));
        }
        menuItem.addActionListener(this);
        menuItem.setActionCommand(element.getAttributeValue("name"));
        String shortcut = element.getAttributeValue("shortcut");
        if (!McUtils.isStringEmpty(shortcut)) {
            menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.Event.CTRL_MASK));
        }
        if (isMenuDisabled(element))
            menuItem.setEnabled(false);

        return menuItem;
    }

    public void actionPerformed(final ActionEvent e) {

        if (e.getActionCommand().equals("showconfig")) {
            MartPointer mp = getSelectedMPfromGCP();
            // final MartPointer mp = ((ConfigComponent)owner).getMartPointer();
            new ConfigDialog(mp.getConfig());
        } else if (e.getActionCommand().equals("changedisplayname")) {
            List<MartPointer> mps = ((GuiContainerPanel) owner).getSelectedMPs();
            LinkedHashMap<Integer, String> lhm = new LinkedHashMap<Integer, String>();
            for (MartPointer mp : mps) {
                lhm.put(mps.indexOf(mp), mp.getConfig().getDisplayName());
            }
            EditDsNameDialog dialog = new EditDsNameDialog(null, lhm, true, true);
            if (dialog.changed()) {
                for (Map.Entry<Integer, String> entry : lhm.entrySet()) {
                    mps.get(entry.getKey()).getConfig().setDisplayName(entry.getValue());
                }
            }
        } else if (e.getActionCommand().equals("makepublic")) {
            GuiContainerPanel gcp = (GuiContainerPanel) owner;
            MartPointer mp = (MartPointer) gcp.getConfigList().getSelectedValue();
            mp.setPublic(true);
        } else if (e.getActionCommand().equals("newguicontainer")) {
            final GuiContainerPanel ppanel = (GuiContainerPanel) (((ButtonTabComponent) owner).getParentTabPane()
                    .getSelectedComponent());
            final GuiContainer gc = ppanel.getGuiContainer();
            String name = JOptionPane.showInputDialog(null, "name");
            if (!McUtils.isStringEmpty(name)) {
                GuiContainer newGc = new GuiContainer(name);
                gc.addGuiContainer(newGc);
                // move all the mp to the new gc
                for (MartPointer mp : gc.getMartPointerList()) {
                    newGc.addMartPointer(mp);
                }
                gc.getMartPointerList().clear();
                // remove current tab and add it back later
                DnDTabbedPane parentTP = ((ButtonTabComponent) owner).getParentTabPane();
                int index = parentTP.indexOfTab(gc.getDisplayName());
                // parentTP.remove(index);
                DnDTabbedPane newPane = new DnDTabbedPane(gc);
                parentTP.setComponentAt(index, newPane);
                // make boderlayout.center to make it automatically maximum.
                // GuiContainerPanel selectedPanel =
                // (GuiContainerPanel)(((ButtonTabComponent)owner).getParentTabPane().getSelectedComponent());
                ((McViewPortal) (McViews.getInstance().getView(IdwViewType.PORTAL))).refreshTabbedPane(newPane);
            }
        } else if (e.getActionCommand().equals("cut")) {
            ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).clearCopyFlag();
            final GuiContainerPanel gcp = ((GuiContainerPanel) owner);
            McGuiUtils.INSTANCE.setCutCopyOperation(1);
            TransferableConfig tc = new TransferableConfig(gcp.getSelectedMPs());
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            clip.setContents(tc, gcp);
        } else if (e.getActionCommand().equals("copy")) {
            ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).clearCopyFlag();
            final GuiContainerPanel gcp = ((GuiContainerPanel) owner);
            McGuiUtils.INSTANCE.setCutCopyOperation(2);
            TransferableConfig tc = new TransferableConfig(gcp.getSelectedMPs());
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            clip.setContents(tc, gcp);
        } else if (e.getActionCommand().equals("paste")) {
            GuiContainerPanel panel = null;
            if (owner instanceof ButtonTabComponent) {
                panel = (GuiContainerPanel) (((ButtonTabComponent) owner).getParentTabPane().getSelectedComponent());
            } else if (owner instanceof GuiContainerPanel)
                panel = (GuiContainerPanel) owner;
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable clipboardContent = clip.getContents(null);
            if (clipboardContent != null) {
                List<MartPointer> mplist = null;
                try {
                    mplist = (List<MartPointer>) clipboardContent
                            .getTransferData(TransferableConfig.MART_POINTER_FLAVOR);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return;
                }
                // remove and add configComponent, refresh
                GuiContainer rootGc = panel.getGuiContainer().getRootGuiContainer();
                // MartPointer mp = rootGc.getMartPointerByNameRecursively(mpName);

                boolean rename = true;
                if (McGuiUtils.INSTANCE.getCutCopyOperation() == 1) {// cut
                    for (MartPointer mp : mplist) {
                        GuiContainer oldGc = mp.getGuiContainer();
                        oldGc.removeMartPointer(mp);
                        // configcomponent->configpanel->viewport->scrollpane->guicontainerpanel
                        // panel.removeConfig(sourceComponent);
                        rename = false;
                        MartPointer newMP = McGuiUtils.INSTANCE.getMartPointerCopy(mp, rename);
                        panel.getGuiContainer().addMartPointer(newMP);
                    }
                    ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).refreshRootPane();
                    // new ObjectController().updateLinks(panel.getGuiContainer());
                } else if (McGuiUtils.INSTANCE.getCutCopyOperation() == 2) {
                    int n = 0;
                    // 0 = create separate config
                    // 1 = copy pointer only
                    if (n == 0) {
                        for (MartPointer mp : mplist) {
                            Config configCopy = McGuiUtils.INSTANCE.getConfigCopy(mp.getConfig(), rename);
                            mp.getConfig().getMart().addConfig(configCopy);
                            // fix link pointer, filter/attribute pointer
                            MartController.getInstance().fixObjects(configCopy.getMart().getMartRegistry());
                            MartPointer newMP = new MartPointer(configCopy, configCopy.getName());
                            newMP.addUser(((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).getUser());
                            newMP.setGroupName(mp.getGroupName());

                            panel.getGuiContainer().addMartPointer(newMP);
                        }
                        ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).refreshRootPane();
                    } else {
                        for (MartPointer mp : mplist) {
                            MartPointer newMP = McGuiUtils.INSTANCE.getMartPointerCopy(mp, rename);
                            panel.getGuiContainer().addMartPointer(newMP);
                        }
                        ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).refreshRootPane();
                    }
                    // new ObjectController().updateLinks(panel.getGuiContainer());
                }

            } // end of if ((clipboardContent != null)
        } else if (e.getActionCommand().equals("rename")) {
            if (owner instanceof ActionPanel) {
                String newName = JOptionPane.showInputDialog("name");
                if (!McUtils.isStringEmpty(newName)) {
                    ActionPanel ap = (ActionPanel) owner;
                    SourceContainer sc = ap.getSourceContainer();
                    for (Mart mart : sc.getMartList()) {
                        mart.setGroupName(newName);
                    }
                    sc.setName(newName);
                    McViewSourceGroup groupView = (McViewSourceGroup) McViews.getInstance().getView(
                            IdwViewType.SOURCEGROUP);
                    groupView.refreshGui();
                }
            } else {
                String newName = JOptionPane.showInputDialog("name");
                if (!McUtils.isStringEmpty(newName)) {
                    final DnDTabbedPane parentTP = ((ButtonTabComponent) owner).getParentTabPane();
                    final GuiContainer gc = ((ButtonTabComponent) owner).getGuiContainer();
                    gc.setDisplayName(newName);
                    int index = parentTP.indexOfTabComponent(owner);
                    if (index >= 0)
                        parentTP.setTitleAt(index, newName);
                    ((ButtonTabComponent) owner).revalidate();
                }
            }
        } else if (e.getActionCommand().equals("activate")) {
            // get current User
            UserGroup user = ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
            List<MartPointer> mps = null;
            if (owner instanceof GuiContainerPanel)
                mps = ((GuiContainerPanel) owner).getSelectedMPs();
            else if (owner instanceof ButtonTabComponent) {
                mps = ((ButtonTabComponent) owner).getGuiContainer().getMartPointerList();
            }

            for (MartPointer mp : mps) {
                if (mp.isActivatedInUser(user))
                    mp.removeUser(user);
                else
                    mp.addUser(user);
            }
        } else if (e.getActionCommand().equals("hide/unhide")) {
            // choose to show or hide
            List<MartPointer> mps = ((GuiContainerPanel) owner).getSelectedMPs();
            for (MartPointer mp : mps) {
                Boolean isHide = mp.getConfig().isHidden();
                mp.getConfig().setHideValue(!isHide);
            }
            ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).refreshPanel((GuiContainerPanel) owner);

        } else if (GuiType.get(e.getActionCommand()) != null) {
            // final GuiContainerPanel panel =
            // (GuiContainerPanel)(((ButtonTabComponent)owner).getParentTabPane().getSelectedComponent());
            final GuiContainer gc = ((ButtonTabComponent) owner).getGuiContainer();
            gc.setGuiType(GuiType.get(e.getActionCommand()));
        } else if (EntryLayout.valueFrom(e.getActionCommand()) != null) {
            // final GuiContainerPanel panel =
            // (GuiContainerPanel)(((ButtonTabComponent)owner).getParentTabPane().getSelectedComponent());
            final GuiContainer gc = ((ButtonTabComponent) owner).getGuiContainer();
            gc.setEntryLayout(EntryLayout.valueFrom(e.getActionCommand()));
        } else if (e.getActionCommand().equals("hide")) {
            final Component component = ((ButtonTabComponent) owner).getParentTabPane().getSelectedComponent();
            GuiContainer gc = null;
            if (component instanceof GuiContainerPanel)
                gc = ((GuiContainerPanel) component).getGuiContainer();
            else if (component instanceof DnDTabbedPane)
                gc = ((DnDTabbedPane) component).getGuiContainer();
            UserGroup user = ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
            if (gc.isActivatedInUser(user))
                gc.removeUser(user);
            else
                gc.addUser(user);
            ((ButtonTabComponent) owner).getButton().revalidate();
            GuiContainerPanel gcp = (GuiContainerPanel) ((ButtonTabComponent) owner).getParentTabPane()
                    .getSelectedComponent();
            for (ConfigComponent c : gcp.getConfigComponents()) {

                if (gc.isActivatedInUser(user)) {
                    ((ConfigComponent) c).setActivated(true, user);
                } else {
                    ((ConfigComponent) c).setActivated(false, user);
                }
            }

        } else if (e.getActionCommand().equals("remove")) {
            int n = JOptionPane.showConfirmDialog(null, "delete?", "Question", JOptionPane.YES_NO_OPTION);
            if (n != 0)
                return;

            // remove tabpane or configcomponent
            if (owner instanceof ButtonTabComponent) {
                final DnDTabbedPane parentTP = ((ButtonTabComponent) owner).getParentTabPane();
                GuiContainer removedGc = ((ButtonTabComponent) owner).getGuiContainer();
                GuiContainer parentGc = (GuiContainer) removedGc.getParent();
                int i = parentTP.indexOfTabComponent(owner);
                if (i != -1) {
                    n = JOptionPane.showConfirmDialog(null, "Delete the tab and its sub components?", "Question",
                            JOptionPane.YES_NO_OPTION);
                    if (n == 0) {
                        // remove all configs under it
                        List<MartPointer> mpList = removedGc.getAllMartPointerListResursively();
                        for (MartPointer mp : mpList) {
                            mp.getMart().removeConfig(mp.getConfig());
                        }
                        parentGc.removeGuiContainer(removedGc);
                        // don't select the last tab +
                        if (i == parentTP.getTabCount() - 2 && i > 0) {
                            parentTP.remove(i);
                            parentTP.setSelectedIndex(i - 1);
                        } else if (i == parentTP.getTabCount() - 2 && i == 0) {
                            if (parentTP.getParent() instanceof DnDTabbedPane) {
                                DnDTabbedPane ppTP = (DnDTabbedPane) parentTP.getParent();
                                int index = ppTP.indexOfTab(parentGc.getDisplayName());
                                if (index != -1) {
                                    // change tab 0 to an empty guicontainerpanel
                                    GuiContainerPanel gcPanel = new GuiContainerPanel(parentGc);
                                    ppTP.setComponentAt(index, gcPanel);
                                }
                            } else
                                parentTP.remove(i);
                        } else
                            parentTP.remove(i);
                    }
                }
            } else if (owner instanceof GuiContainerPanel) {
                List<MartPointer> mps = ((GuiContainerPanel) owner).getSelectedMPs();
                for (MartPointer mp : mps) {
                    mp.getMart().removeConfig(mp.getConfig());
                    mp.getGuiContainer().removeMartPointer(mp);
                }
                ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL))
                        .refreshPanel((GuiContainerPanel) owner);
            }
        } else if (e.getActionCommand().equals("updatelink")) {
            // GuiContainer gc = ((ButtonTabComponent)owner).getGuiContainer();
            // ObjectController oc = new ObjectController();
            // oc.updateLinks(gc);
        } else if (e.getActionCommand().equals("reorder")) {
            GuiContainer gc = ((GuiContainerPanel) owner).getGuiContainer();
            ReorderConfigDialog dialog = new ReorderConfigDialog(gc);
            if (dialog.changed())
                ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL))
                        .refreshPanel((GuiContainerPanel) owner);
        } else if (e.getActionCommand().equals("reordertab")) {
            GuiContainer gc = ((ButtonTabComponent) owner).getGuiContainer();
            ReorderTabDialog dialog = new ReorderTabDialog(gc);
            if (dialog.changed())
                ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL))
                        .refreshTabbedPane(((ButtonTabComponent) owner).getParentTabPane());
        } else if (e.getActionCommand().equals("editproperties")) {
            final GuiContainer gc = ((ButtonTabComponent) owner).getGuiContainer();
            new PropertyDialog(null, gc, (ButtonTabComponent) owner);
        } else if (e.getActionCommand().equals("mpproperties")) {

            MartPointer mp = this.getSelectedMPfromGCP();
            // gComponent)owner).getMartPointer();
            new PropertyDialog(null, mp, null);
        } else if (e.getActionCommand().equals("setgroup")) {
            String groupname = (String) JOptionPane.showInputDialog(null, "enter the group name", "Set Group",
                    JOptionPane.PLAIN_MESSAGE, null, null, "");
            if (McUtils.isStringEmpty(groupname))
                return;

            List<MartPointer> mps = ((GuiContainerPanel) owner).getSelectedMPs();
            for (Object obj : mps) {
                MartPointer mp = (MartPointer) obj;
                mp.setProperty(XMLElements.GROUP, groupname);
            }
        } else if (e.getActionCommand().equals("ordergroup")) {
            GuiContainer gc = ((GuiContainerPanel) owner).getGuiContainer();
            OrderGroupDialog dialog = new OrderGroupDialog(gc);
            if (dialog.changed()) {
                // reoder the martpointers based on the new group list
                List<String> groups = dialog.getNewGroupList();
                McGuiUtils.INSTANCE.orderMartPointerByGroup(gc, groups);
                ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL))
                        .refreshPanel((GuiContainerPanel) owner);
            }
        } else if (e.getActionCommand().equals("readonly")) {
            String s = (String) JOptionPane.showInputDialog(null, "enter the key", "Key", JOptionPane.PLAIN_MESSAGE,
                    null, null, "");
            if (McUtils.isStringEmpty(s))
                return;
            // encrypt it and compare it with existing one, if exist one is empty, save current key
            String hash = McUtils.byteArrayToHexString(McUtils.computeHash(s));
            GuiContainerPanel gcp = (GuiContainerPanel) owner;
            for (Object obj : gcp.getConfigList().getSelectedValues()) {
                MartPointer mp = (MartPointer) obj;
                final Config config = mp.getConfig();
                String oldPw = config.getPropertyValue(XMLElements.PASSWORD);
                if (McUtils.isStringEmpty(oldPw)) {
                    config.setProperty(XMLElements.PASSWORD, hash);
                } else {
                    // check if they are equals
                    if (!hash.equals(oldPw)) {
                        JOptionPane.showMessageDialog(null, "incorrect key");
                        return;
                    }
                }

                boolean isReadonly = config.isReadOnly();
                config.setReadOnly(!isReadonly);
            }

        } else if (e.getActionCommand().equals("orderdsbydisplayname")) {
            // ((ConfigComponent)owner).getMartPointer().orderDatasetList(new DisplayNameComparator());
            // ((MartPointer)node.getObject()).orderDatasetList(new NameComparator());
        } else if (e.getActionCommand().equals("materialize")) {
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();
            new SaveDDLDialog(mart, new ArrayList<String>(), SaveDDLDialog.RUN_DDL);
        } else if (e.getActionCommand().equals("updatemart")) {
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();
            int n = JOptionPane.showConfirmDialog(null, "Do you want to update " + mart.getDisplayName() + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (n == JOptionPane.YES_OPTION) {
                UpdateMart um = new UpdateMart(null, null);
                um.updateMartWithProgressBar(mart);
                Validation.validate(McGuiUtils.INSTANCE.getRegistryObject(), false);
            }
        } else if (e.getActionCommand().equals("removemart")) {
            int n = JOptionPane.showConfirmDialog(null, "delete?", "Question", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (n != 0)
                return;

            MartComponent stree = (MartComponent) owner;
            MartRegistry mr = stree.getMart().getMartRegistry();
            for (Mart mart : stree.getSelectedComponents()) {
                List<MartConfiguratorObject> references = mart.getReferences();
                for (MartConfiguratorObject mcObj : references) {
                    MartPointer mp = (MartPointer) mcObj;
                    mp.getGuiContainer().removeMartPointer(mp);
                }
                // remove options
                Element element = Options.getInstance().getMartElement(mart);
                if (element != null) {
                    Options.getInstance().getOptionRootElement().removeContent(element);
                }
                mr.removeMart(mart);
            }
            // update mcviewportal and mcviewsource
            ((McViewSourceGroup) McViews.getInstance().getView(IdwViewType.SOURCEGROUP)).showSource(mr);
            ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).refreshRootPane();
        } else if (e.getActionCommand().equals("schemaeditor")) {
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();
            new SchemaDialog(mart);
        } else if (e.getActionCommand().equals("schemastructure")) {
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();
            new TableDialog(null, mart);
        } else if (e.getActionCommand().equals("createLinkIndex")) {
            ObjectController oc = new ObjectController();
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();
            oc.requestCreateLinkIndex(mart);
        } else if (e.getActionCommand().equals("linkManagement")) {
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();
            LinkManagementDialog lmDialog = new LinkManagementDialog(null, mart);

        } else if (e.getActionCommand().equals("datasourcemanage")) {
            List<Mart> martList = new ArrayList<Mart>();
            MartComponent mc = (MartComponent) owner;
            final Mart mart = mc.getMart();
            martList.add(mart);
            // added cols orders to a new PtModel
            ArrayList<Integer> cols = new ArrayList<Integer>();
            cols.add(PartitionUtils.DATASETNAME);
            cols.add(PartitionUtils.DISPLAYNAME);
            cols.add(PartitionUtils.CONNECTION);
            cols.add(PartitionUtils.DATABASE);
            cols.add(PartitionUtils.SCHEMA);
            cols.add(PartitionUtils.HIDE);
            cols.add(PartitionUtils.KEY);

            new DatasourceDialog(martList, cols, !mc.getMart().getSourceContainer().isGrouped());

        } else if (e.getActionCommand().equals("renamemart")) {
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();

            JTextField inputbox = new JTextField();
            inputbox.setText(mart.getDisplayName());
            String message = "rename mart?";
            Object[] params = { message, inputbox };
            int n = JOptionPane.showConfirmDialog(null, params, "Rename Mart", JOptionPane.OK_CANCEL_OPTION);
            if (n != 0)
                return;

            mart.setDisplayName(inputbox.getText());

            ((McViewSourceGroup) McViews.getInstance().getView(IdwViewType.SOURCEGROUP)).refreshGui();
        } else if (e.getActionCommand().equals("searchfromsource")) {
            MartComponent stree = (MartComponent) owner;
            final Mart mart = stree.getMart();
            boolean b = mart.searchFromTarget();
            mart.setSearchFromTarget(!b);
        } else if (e.getActionCommand().equals("delete")) {
            ActionPanel ac = (ActionPanel) owner;
            McViewSourceGroup sourceGroup = ((McViewSourceGroup) McViews.getInstance().getView(IdwViewType.SOURCEGROUP));
            sourceGroup.deleteGroup(ac.getTitle());
        } else if (e.getActionCommand().equals("newgroup")) {
            AddGroupDialog dialog = new AddGroupDialog(owner);
            if (!McUtils.isStringEmpty(dialog.getName())) {
                ActionPanel ap = (ActionPanel) owner;
                SourceContainer sc = ap.getSourceContainer();
                SourceContainer newsc = new SourceContainer(dialog.getName());
                newsc.setGroup(dialog.isGrouped());
                ((SourceContainers) sc.getParent()).addSourceContainer(newsc);
                McViewSourceGroup sourceGroup = ((McViewSourceGroup) McViews.getInstance().getView(
                        IdwViewType.SOURCEGROUP));
                sourceGroup.refreshGui();
            }
        } else if (e.getActionCommand().equals("checkall")) {
            LeafCheckBoxList list = (LeafCheckBoxList) this.owner;
            list.checkAll(true);
        } else if (e.getActionCommand().equals("uncheckall")) {
            LeafCheckBoxList list = (LeafCheckBoxList) this.owner;
            list.checkAll(false);
        } else if (e.getActionCommand().equals("group")) {
            ActionPanel stree = (ActionPanel) owner;
            final SourceContainer sc = stree.getSourceContainer();
            boolean g = sc.isGrouped();
            sc.setGroup(!g);
            McViewSourceGroup groupView = (McViewSourceGroup) McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
            groupView.refreshGui();
        } else if (e.getActionCommand().equals("createconfig")) {
            // need to handle multiselect
            final UserGroup user = ((McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
            MartComponent mc = (MartComponent) this.owner;
            List<Mart> selectedMart = mc.getSelectedComponents();
            for (Mart mart : selectedMart) {
                McViewPortal portalView = (McViewPortal) McViews.getInstance().getView(IdwViewType.PORTAL);
                GuiContainerPanel gcpanel = portalView.getSelectedGcPanel();
                if (gcpanel != null) {
                    if (gcpanel.getGuiContainer().getGuiType().equals(GuiType.get("martreport"))) {
                        gcpanel.addReportConfig(mart);
                    } else
                        gcpanel.addSingleConfig(mart, user);
                }
            }
        } else if (e.getActionCommand().equals("deletesourcecontainer")) {
            ActionPanel stree = (ActionPanel) owner;
            final SourceContainer sc = stree.getSourceContainer();
            ((SourceContainers) sc.getParent()).removeSourceContainer(sc.getName());
            McViewSourceGroup groupView = (McViewSourceGroup) McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
            groupView.refreshGui();
        } else if (e.getActionCommand().equals("searchsource")) {
            ActionPanel stree = (ActionPanel) owner;
            java.awt.Container c = SwingUtilities.getAncestorOfClass(
                    org.biomart.configurator.view.idwViews.McViewSourceGroup.class, stree);
            if (c != null) {
                McViewSourceGroup groupview = (McViewSourceGroup) c;
                MartComponent mc = groupview.findMartComponent();
                if (mc != null) {
                    groupview.scrollToComponent(mc);
                    mc.setBackground(Color.YELLOW);
                }
            }
        } else if (e.getActionCommand().equals("generateontology")) {
            if (owner instanceof GuiContainerPanel) {
                GuiContainerPanel gcp = (GuiContainerPanel) owner;
                gcp.generateOntology();
                MartController.getInstance().setChanged(true);
            }
        } else if (e.getActionCommand().equals("loadontology")) {
            if (owner instanceof GuiContainerPanel) {
                GuiContainerPanel gcp = (GuiContainerPanel) owner;
                List<MartPointer> marts = gcp.getGuiContainer().getMartPointerList();

                for (MartPointer mart : marts)
                    gcp.loadOntology(mart.getConfig());

                MartController.getInstance().setChanged(true);
            }
        } else if (e.getActionCommand().equals("collapseall")) {
            McViewSourceGroup groupView = (McViewSourceGroup) McViews.getInstance().getView(IdwViewType.SOURCEGROUP);
            groupView.collapseall();
        }
    }

}
