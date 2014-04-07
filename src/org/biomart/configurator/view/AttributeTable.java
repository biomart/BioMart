/*

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.biomart.configurator.view;


import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.AttributeRowEditor;
import org.biomart.configurator.controller.AttributeTableHandler;
import org.biomart.configurator.model.XMLAttributeTableModel;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.view.menu.AttrTableContextMenu;
import org.biomart.configurator.view.menu.AttributeTableConfig;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

import e.gui.ETable;



public class AttributeTable extends JTable {

     /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
    protected Insets scrollInsets = defaultScrollInsets;
    private AttrTableContextMenu popupMenu;
    //treeNode for double click
    private MartConfiguratorObject treeNode;
    private MartConfigTree tree;
    private AttributeTableHandler atHandler;
    private AttributeRowEditor rowEditor;

    public AttributeTable(MartConfigTree tree, JDialog parent) {
      super(null);
      this.tree = tree;
      this.setRowSelectionAllowed(false);
      this.setColumnSelectionAllowed(false);
      this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      this.setShowGrid(true);
      this.setGridColor(Color.BLACK);
      this.popupMenu = new AttrTableContextMenu(this,tree);
      this.atHandler = new AttributeTableHandler(parent);
      this.setDefaultRenderer(String.class, new AttTableCellRenderer());
      MouseListener popupListener = new PopupListener();
      this.addMouseListener(popupListener);
  	  this.rowEditor = new AttributeRowEditor();

    }
    
	private void doDoubleClick(MouseEvent e) {
		Element element = AttributeTableConfig.getInstance()
			.getElementByName(this.treeNode.getNodeType().toString());
		@SuppressWarnings("unchecked")
		List<Element> dbElement = element.getChildren("doubleclickitem");
		if(McUtils.isCollectionEmpty(dbElement)) {
			//handle partitiontable case
		} else {			
			int row = this.rowAtPoint(e.getPoint());
			String rowName = (String)this.getValueAt(row, 0);
			//find the right doubleclick handler
			if(rowName.equals(XMLElements.ATTRIBUTELIST.toString())) {
				if(!McUtils.isStringEmpty((String)this.getValueAt(row, 1))) {
					this.atHandler.requestEditAttributeList(this.treeNode);
				}
			}else if(rowName.equals(XMLElements.FILTERLIST.toString())) {
				if(!McUtils.isStringEmpty((String)this.getValueAt(row, 1)))
					this.atHandler.requestEditFilterList(this.treeNode);
			}else if(rowName.equals(XMLElements.DATASETS.toString())) {
				this.atHandler.requestEditDatasetsInMartPointer(this.treeNode);
			}else if(rowName.equals(XMLElements.VALUE.toString())) {
				if(!McUtils.isStringEmpty((String)this.getValueAt(row, 1)))
					this.atHandler.requestEditPseudoAttribute(this.treeNode);
			}else if(rowName.equals(XMLElements.PROCESSOR.toString())) {
				this.atHandler.requestProcessor((Config)this.treeNode);
			}
		}
	}
    
	@Override
    public void setModel(TableModel dataModel) {
    	super.setModel(dataModel);
    	dataModel.addTableModelListener(this.tree);
    	//set roweditor
    	if(dataModel instanceof XMLAttributeTableModel) {   	
        	this.rowEditor.clear();
			org.jdom.Element element = ((XMLAttributeTableModel) dataModel).getElement();
			this.addRowEditors(element,((XMLAttributeTableModel) dataModel).getMcObject());
    	}
    }
	
	private void addRowEditors(org.jdom.Element element, MartConfiguratorObject mcObj) {
    	@SuppressWarnings("unchecked")
    	List<org.jdom.Element> items = element.getChildren("item");
    	for(int i=0; i<items.size(); i++) {
    		org.jdom.Element item = items.get(i);
    		//does item have editor
    		if(item.getChild("editor")==null)
    			continue;
    		Element editorElement = item.getChild("editor");
    		//check type
    		if(editorElement.getAttributeValue("type").equals("enum")) {
    			String className = editorElement.getAttributeValue("class");
    			try {
					Class<?> c = Class.forName(className);
					Object[] oArray = c.getEnumConstants();
	    			String[] values = new String[oArray.length];
	    			for(int j=0; j<oArray.length; j++) {
	    				values[j] = oArray[j].toString();
	    			}
	    			JComboBox cb = new JComboBox(values);
	    			this.rowEditor.addEditorForRow(i, new DefaultCellEditor(cb));

				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}else if(editorElement.getAttributeValue("type").equals("dropdown")) {
    			String methodName = editorElement.getAttributeValue("method");
    			Class<?>[] paras = {};
    			Object[] objs = {};
    			try {
    				Method thisMethod = mcObj.getClass().getDeclaredMethod(methodName, paras);
    				Object result = thisMethod.invoke(mcObj, objs);
    				List<String> resultList = (List<String>)result;
	    			String[] values = new String[resultList.size()];
	    			for(int j=0; j<resultList.size(); j++) {
	    				values[j] = resultList.get(j);
	    			}
	    			JComboBox cb = new JComboBox(values);	    			
	    			this.rowEditor.addEditorForRow(i, new DefaultCellEditor(cb));
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
    		}else if(editorElement.getAttributeValue("type").equals("boolean")) {
    			String[] values = new String[]{Boolean.toString(false),Boolean.toString(true)};
    			JComboBox cb = new JComboBox(values);	    			
    			this.rowEditor.addEditorForRow(i, new DefaultCellEditor(cb));
    		}
    	}		
	}
    
    public void setContextMenuEnv(MartConfiguratorObject obj) {
    	this.popupMenu.setContextObj(obj);
    	this.treeNode = obj;
    }
    
	class PopupListener implements MouseListener {
		public void mousePressed(MouseEvent e) {
			//for mac
			showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			//for windows
			showPopup(e);
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()  &&  e.getClickCount () == 1) {
				//popupMenu.setSelectedColumn(AttributeTable.this.columnAtPoint(e.getPoint()));		
				popupMenu.setPoint(e.getPoint());
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		public void mouseClicked(MouseEvent e) {
			if(e.getClickCount() == 2) {
				doDoubleClick(e);
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}
 
	@Override
    public TableCellEditor getCellEditor(int row, int col) {
        TableCellEditor tmpEditor = null;
        if (this.rowEditor!=null)
            tmpEditor = this.rowEditor.getEditor(row);
        if (tmpEditor!=null)
            return tmpEditor;
        return super.getCellEditor(row,col);
    }


}
