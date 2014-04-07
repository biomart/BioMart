package org.biomart.configurator.view.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.utils.type.ComponentStatus;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.view.component.ColumnComponent;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.configurator.view.gui.diagrams.Diagram;
import org.biomart.configurator.view.gui.diagrams.SchemaDiagram;
import org.biomart.configurator.view.gui.diagrams.TargetDiagram;
import org.biomart.configurator.view.gui.dialogs.ExplainTableDialog;
import org.biomart.configurator.view.gui.dialogs.PropertyDialog;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.RelationSource;
import org.biomart.objects.objects.SourceColumn;
import org.biomart.objects.objects.SourceTable;
import org.biomart.objects.objects.Table;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class DiagramMenuConstructor implements ActionListener {
	private String contextMenuXML = "conf/xml/diagramContextMenu.xml";
	private JPopupMenu contextMenu;
	private Element root;
	private Object owner;
	private JDialog componentOwner;
	private JComponent component;
	private static DiagramMenuConstructor instance;

	public static DiagramMenuConstructor getInstance() {
		if(instance == null)
			instance = new DiagramMenuConstructor();
		return instance;
	}
	
	private DiagramMenuConstructor() {
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
	
	public JPopupMenu getContextMenu(JDialog componentOwner, JComponent component, Object owner) {
		this.owner = owner;
		this.componentOwner = componentOwner;
		this.component = component;
		contextMenu.removeAll();
		String type = null;
		if(owner instanceof MartConfiguratorObject) {
			type = ((MartConfiguratorObject)this.owner).getNodeType().toString();
			if(this.owner instanceof DatasetTable)
				type = "datasettable";
			else if(this.owner instanceof DatasetColumn)
				type = "datasetcolumn";	
			else if(this.owner instanceof SourceTable)
				type = "sourcetable";
		} else if(this.owner instanceof TargetDiagram) {
			type = "targetdiagram"; 
		} else if(this.owner instanceof SchemaDiagram) {
			type = "schemadiagram";
		}
		this.createCustomizedMenu();			
		
		Element e = root.getChild(type);
		if(e == null)
			return this.contextMenu;
		@SuppressWarnings("unchecked")	
		List<Element> menuItemElement = e.getChildren();
		if(menuItemElement==null) 
			return this.contextMenu;
		for (Element item:menuItemElement) {
			if(item.getName().equals("Separator"))
				contextMenu.addSeparator();
			else {
				//get submenu
				@SuppressWarnings("unchecked")
				List<Element> subMenu = item.getChildren();
				if(subMenu.size()>0) {
					JMenu menu = new JMenu(item.getAttributeValue("title"));
					contextMenu.add(menu);
					for(Element subItem:subMenu){
						menu.add(createMenuItem(subItem));
					}
				}else {
				    contextMenu.add(createMenuItem(item));
				}				
			}
		}
		return this.contextMenu;
	}	
	
	private JMenuItem createMenuItem(Element element) {
		JMenuItem menuItem = null;
		String type = element.getAttributeValue("type");
		//checkbox
		if("checkbox".equals(type)) {
			menuItem = new JCheckBoxMenuItem(element.getAttributeValue("title"));
		}else {
			menuItem = new JMenuItem(element.getAttributeValue("title"));
		}
		menuItem.addActionListener(this);
		menuItem.setActionCommand(element.getAttributeValue("name"));
		String shortcut = element.getAttributeValue("shortcut");
		if(!McUtils.isStringEmpty(shortcut)) {
			menuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP,
                       java.awt.Event.CTRL_MASK));
		}
		if(isMenuDisabled(element))
			menuItem.setEnabled(false);
		
		return menuItem;
	}

	private boolean isMenuDisabled(Element element) {
		String disabled = element.getAttributeValue("disabled");
		if("1".equals(disabled))
			return true;
		return false;
	}

	
	private void createCustomizedMenu() {
		if(owner instanceof DatasetTable) {
			final DatasetTable dst = (DatasetTable)owner;
			//cannot be main table
			if(dst.getType() == DatasetTableType.DIMENSION) {
				JCheckBoxMenuItem hideMenu = new JCheckBoxMenuItem(XMLElements.HIDE.toString());
				hideMenu.setActionCommand(XMLElements.HIDE.toString());
				hideMenu.setSelected(((DatasetTable)owner).isHidden());
				hideMenu.addActionListener(this);
				this.contextMenu.add(hideMenu);
				
				//convert to subclass
				final JMenuItem subclass = new JMenuItem(Resources
						.get("DMTOSUBMAIN"));
				subclass.setMnemonic(Resources.get("DMTOSUBMAINM")
						.charAt(0));
				subclass.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						Mart mart = dst.getMart();
						Relation r = dst.getFocusRelation();
						try {
							r.setSubclassRelation(true,mart.getName());
						} catch (ValidationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						McEventBus.getInstance().fire(McEventProperty.REBUILD_MART.toString(), mart);
					}
				});
				
				if (dst.isHidden() || MartController.getInstance().hasSubMain(dst.getParentMainTable()))
					subclass.setEnabled(false);
				//contextMenu.add(subclass);
			} else if(dst.getType() == DatasetTableType.MAIN_SUBCLASS) {
				// The subclass table can be removed by using this option. This
				// simply masks the relation that caused the subclass to exist.
				final JMenuItem unsubclass = new JMenuItem(Resources
						.get("removeSubclassTitle"));
				unsubclass.setMnemonic(Resources.get("removeSubclassMnemonic").charAt(0));
				unsubclass.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						Mart mart = dst.getMart();
						Relation r = dst.getFocusRelation();
						try {
							r.setSubclassRelation(false,mart.getName());
						} catch (ValidationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						McEventBus.getInstance().fire(McEventProperty.REBUILD_MART.toString(), mart);
					}
				});
				//contextMenu.add(unsubclass);
			}
		} else if(owner instanceof DatasetColumn) {
			//TODO cannot hide a key column
			JCheckBoxMenuItem hideMenu = new JCheckBoxMenuItem(XMLElements.HIDE.toString());
			hideMenu.setActionCommand(XMLElements.HIDE.toString());
			hideMenu.setSelected(((DatasetColumn)owner).isHidden());
			hideMenu.addActionListener(this);
			this.contextMenu.add(hideMenu);			
		} else if(owner instanceof SourceTable) {
			JCheckBoxMenuItem hideMenu = new JCheckBoxMenuItem(XMLElements.HIDE.toString());
			hideMenu.setActionCommand(XMLElements.HIDE.toString());
			hideMenu.setSelected(((SourceTable)owner).isHidden());
			hideMenu.addActionListener(this);
			this.contextMenu.add(hideMenu);						
		} else if(owner instanceof SourceColumn) {
			JCheckBoxMenuItem hideMenu = new JCheckBoxMenuItem(XMLElements.HIDE.toString());
			hideMenu.setActionCommand(XMLElements.HIDE.toString());
			hideMenu.setSelected(((SourceColumn)owner).isHidden());
			hideMenu.addActionListener(this);
			this.contextMenu.add(hideMenu);								
		} else if(owner instanceof RelationSource) {
			// What relation is this? And is it correct?
			final Relation relation = (Relation) owner;
			//only for source relation			
			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();
			final boolean relationIncorrect = relation.getStatus().equals(
					ComponentStatus.INFERRED_INCORRECT);

			// Set up a radio group for the cardinality.
			final ButtonGroup cardGroup = new ButtonGroup();

			// Set the relation to be 1:1, but only if it is correct.
			final JRadioButtonMenuItem oneToOne = new JRadioButtonMenuItem(
					Resources.get("ONETOONETITLE"));
			oneToOne.setMnemonic(Resources.get("ONETOONEMNEMONIC").charAt(0));
			oneToOne.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					relation.setCardinality(Cardinality.ONE);
					changeSourceRelation(relation);
				}
			});
			cardGroup.add(oneToOne);
			contextMenu.add(oneToOne);
			if (relationIncorrect)
				oneToOne.setEnabled(false);
			if (relation.isOneToOne())
				oneToOne.setSelected(true);
			
			// Set the relation to be 1:M, but only if it is correct.
			final JRadioButtonMenuItem oneToManyA = new JRadioButtonMenuItem(
					Resources.get("ONETOMANYATITLE", relation.getFirstKey().toString()));
			oneToManyA.setMnemonic(Resources.get("ONETOMANYAMNEMONIC").charAt(0));
			oneToManyA.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
					relation.setCardinality(Cardinality.MANY_A);
					changeSourceRelation(relation);
				}
			});
			cardGroup.add(oneToManyA);
			if(relation.isOneToMany())
				oneToManyA.setSelected(true);
			contextMenu.add(oneToManyA);


			// Separator.
			contextMenu.addSeparator();


			// Remove the relation from the schema, but only if handmade.
			final JMenuItem remove = new JMenuItem(Resources
					.get("removeRelationTitle"), new ImageIcon("images/cut.gif"));
			remove.setMnemonic(Resources.get("removeRelationMnemonic")
					.charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent evt) {
				}
			});
			contextMenu.add(remove);
			if (!relation.getStatus().equals(ComponentStatus.HANDMADE))
				remove.setEnabled(false);
			
			JCheckBoxMenuItem hideMenu = new JCheckBoxMenuItem(XMLElements.HIDE.toString());
			hideMenu.setActionCommand(XMLElements.HIDE.toString());
			hideMenu.setSelected(((Relation)owner).isHidden());
			hideMenu.addActionListener(this);
			this.contextMenu.add(hideMenu);												
		}
		this.contextMenu.addSeparator();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("explaintable")) {
			if(owner instanceof DatasetTable) {
				new ExplainTableDialog(componentOwner, (DatasetTable)owner);
			}
		}else if(e.getActionCommand().equals("hide")) {
			boolean b = ((MartConfiguratorObject)owner).isHidden();
			ObjectController oc = new ObjectController();
			oc.setHide((MartConfiguratorObject)owner, !b);
			if(owner instanceof DatasetTable) {
				McEventBus.getInstance().fire(McEventProperty.REFRESH_TARGETDIAGRAM.toString(), null);
			}else if(owner instanceof DatasetColumn) {
				McEventBus.getInstance().fire(McEventProperty.REFRESH_TARGETDIAGRAM.toString(), null);
			}else if(owner instanceof SourceTable) {
				//update myself
				TableComponent tc = (TableComponent)component;
				tc.repaintDiagramComponent();
				McEventBus.getInstance().fire(McEventProperty.REBUILD_MART.toString(), ((SourceTable)owner).getParent().getParent());
			}else if(owner instanceof SourceColumn) {
				//update myself
				ColumnComponent tc = (ColumnComponent)component;
				tc.repaintDiagramComponent();
				McEventBus.getInstance().fire(McEventProperty.REBUILD_MART.toString(), ((SourceColumn)owner).getParent().getParent().getParent());				
			}
			else if(owner instanceof RelationSource) {
				RelationComponent rc = (RelationComponent)component;
				rc.repaintDiagramComponent();
				McEventBus.getInstance().fire(McEventProperty.REBUILD_MART.toString(), ((Relation)owner).getFirstKey().getTable().getParent().getParent());
			}
		}else if(e.getActionCommand().equals(Resources.get("SHOWPROPERTIES"))) {
				new PropertyDialog(this.componentOwner,(MartConfiguratorObject)owner,null);
		}else if(e.getActionCommand().equals("findtable")) {
			final Table table = ((Diagram)owner).askUserForTable();
			if (table != null)
				((Diagram)owner).findObject(table);

		}
		
	}
	
	private void changeSourceRelation(Relation relation) {
		if (!relation.getStatus().equals(ComponentStatus.HANDMADE))
			try {
				relation.setStatus(ComponentStatus.MODIFIED);
				relation.setProperty(XMLElements.VISIBLEMODIFIED, Boolean.toString(true));
			} catch (AssociationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//TODO use property event later
		MartController.getInstance().rebuildMartFromSource((Mart)relation.getFirstKey().getTable().getParent().getParent());
		//Diagram d = (Diagram) SwingUtilities.getAncestorOfClass(Diagram.class, component);
		//if(d!=null)
		RelationComponent rc = (RelationComponent)component;
		rc.repaintDiagramComponent();
		//refresh the target gui
		Diagram d = (Diagram) SwingUtilities.getAncestorOfClass(Diagram.class, component);
		if(d!=null) {
			JTabbedPane tp = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, d);
			JScrollPane sp = (JScrollPane)tp.getComponentAt(1);
			TargetDiagram ts = (TargetDiagram)sp.getViewport().getView();
			ts.recalculateDiagram();
		}

	}
	
}