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

package org.biomart.configurator.model;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import org.biomart.api.enums.Operation;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.menu.AttributeTableConfig;
import org.biomart.objects.enums.FilterOperation;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.RDFClass;
import org.biomart.objects.portal.MartPointer;
import org.biomart.queryEngine.OperatorType;
import org.biomart.objects.enums.FilterType;
import org.jdom.Element;



public class XMLAttributeTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String[] columnNames = { "Property", "Value" };
	private static final int COLUMN_COUNT = 2;
	protected List<List<String>> dataObj;
	protected String objClass;
	protected String[] firstColumnData;
	private MartConfiguratorObject mcObj;
	private org.jdom.Element configElement;
	private boolean editable;
	private Dataset selectedDs;

	public XMLAttributeTableModel(MartConfiguratorObject obj, Dataset ds, boolean editable) {
		if(obj==null)
			return;
		this.selectedDs = ds;
		this.mcObj = obj;
		this.editable = editable;
		//get the config element
		this.configElement = AttributeTableConfig.getInstance().getElementByName(this.mcObj.getNodeType().toString());
		this.dataObj = this.populateAttributeTable(configElement);
	}
	
	
	public org.jdom.Element getElement() {
		return this.configElement;
	}
	
	public int getColumnCount() {
		//Returns the number of columns in the model.
		return COLUMN_COUNT;
	}
	
	public String getColumnName(int columnIndex) {
		//Returns the name of the column at columnIndex.
		return columnNames[columnIndex];
	}

	public int getRowCount() {
		//Returns the number of rows in the model.
		if(dataObj==null) return 0;
		return dataObj.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
			return dataObj.get(rowIndex).get(columnIndex);
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if(!this.editable)
			return false;

		//Returns true if the cell at rowIndex and columnIndex is editable.
		if(this.configElement == null)
			return false;
		if (columnIndex == 0)
			return false;
		
		if(null == this.configElement)
			return false;
		
		//find the node
		@SuppressWarnings("unchecked")
		List<Element> itemList = this.configElement.getChildren("item");
		Element itemElement = null;
		for(Element item: itemList) {
			if(dataObj.get(rowIndex).get(0).equals(item.getAttributeValue(XMLElements.NAME.toString()))) {
				itemElement = item;
				break;
			}
		}
		if(itemElement == null)
			return false;
		String editableStr = itemElement.getAttributeValue("editable");
		if("1".equals(editableStr))
			return true;
		else
			return false;
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		if(!validate(value, row, col))
			return;
		if(this.selectedDs!=null) {
			int n = JOptionPane.showConfirmDialog(null, "change the value for the current dataset only, continue?",
					"confirm", JOptionPane.YES_NO_OPTION);
			if(n!=0)
				return;
		}
		//check if there is a value change
		String oldValue = (String)this.getValueAt(row, col);
		if(oldValue.equals(value))
			return;
		//McGuiUtils.ErrorMsg result = this.setValue((String)value, row);
		//temp added for multiple select change
		this.setAllSelValues((String)value,oldValue, row, col);
		//refresh treenode
	}
	
	protected void setAllSelValues(String value,String oldValue, int row, int col) {
		this.setValue(this.mcObj, value, oldValue, row);
		dataObj.get(row).set(col, value);
		fireTableCellUpdated(row,col);	
		MartController.getInstance().setChanged(true);
	}
	
	private boolean validate(Object value, int row, int col) {
		boolean res = true;
		return res;
	}

	public McGuiUtils.ErrorMsg setValue(MartConfiguratorObject object,String value,String oldValue, int row) {
		//handle the common attribute for all objects first
		McGuiUtils.ErrorMsg result = McGuiUtils.ErrorMsg.DEFAULT;
		if(object instanceof MartRegistry) {
			this.setValueForMartRegistry(object,value, row);
		}else if(object instanceof Mart) {
			this.setValueForMart(object,value, row);
		}else if(object instanceof Config) {
			this.setValueForConfig(object,value, row);
		}
		else if(object instanceof MartPointer) {
			this.setValueForMartPointer(object,value, row);
		}else if(object instanceof Container) {
			result = this.setValueForContainer(object,value, row);
		}else if(object instanceof org.biomart.objects.objects.Attribute) {
			result = this.setValueForAttribute(object,value, row);
		}else if(object instanceof org.biomart.objects.objects.Filter) {
			result = this.setValueForFilter(object,value, row);
		}else if(object instanceof org.biomart.objects.portal.GuiContainer) {
			this.setValueForGuiContainer(object,value, row);
		}else if(object instanceof org.biomart.objects.objects.Link) {
			this.setValueForLink(object,value, row);
		}
		else if(object instanceof org.biomart.objects.objects.DatasetTable) {
			this.setValueForDatasetTable(object, value, row);
		}
		else if(object instanceof org.biomart.objects.objects.RDFClass) {
			this.setValueForRDF(object, value, row);
		}
		Validation.revalidate(object);
		return result;
	}
	
	@Override
	public Class<?> getColumnClass(int c) {
        return String.class;
    }

	private void setValueForMartRegistry(MartConfiguratorObject object,String value, int row) {
		if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			object.setProperty(XMLElements.NAME, value);
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			object.setProperty(XMLElements.INTERNALNAME, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			object.setProperty(XMLElements.DISPLAYNAME, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			object.setProperty(XMLElements.DESCRIPTION, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.HIDE.toString())) {
			//check value
			Boolean b = new Boolean(value);
			object.setProperty(XMLElements.HIDE, Boolean.toString(b));
		}else if(dataObj.get(row).get(0).equals(XMLElements.DATATYPE.toString())) {
			object.setProperty(XMLElements.DATATYPE, value);
		}
	}
	
	private void setValueForMart(MartConfiguratorObject object,String value, int row) {
		if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			//TODO change the reference in martpointer
			object.setProperty(XMLElements.NAME, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			object.setProperty(XMLElements.INTERNALNAME, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			object.setProperty(XMLElements.DISPLAYNAME, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			object.setProperty(XMLElements.DESCRIPTION, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.HIDE.toString())) {
			//check value
			Boolean b = new Boolean(value);
			object.setProperty(XMLElements.HIDE, Boolean.toString(b));
		}else if(dataObj.get(row).get(0).equals(XMLElements.DATATYPE.toString())) {
			object.setProperty(XMLElements.DATATYPE, value);
		}
	}
	
	private void setValueForDatasetTable(MartConfiguratorObject object,String value, int row) {
		object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
	}
 	
	private void setValueForMartPointer(MartConfiguratorObject object, String value, int row) {
		if(dataObj.get(row).get(0).equals(XMLElements.OPERATION.toString())) {
			Operation operation = Operation.valueFrom(value);
			if(operation == null) {
				JOptionPane.showMessageDialog(null, Resources.get("INVALIDOPERATION"));
				return;
			}
			((MartPointer)object).setOperation(operation);
		}else if(dataObj.get(row).get(0).equals(XMLElements.INDEPENDENTQUERYING.toString())) {
			boolean b = Boolean.parseBoolean(value);
			((MartPointer)object).setIndependentQuery(b);
		}else if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			object.setName(value);
		}else {
			object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
		}
	}
	
	private McGuiUtils.ErrorMsg setValueForContainer(MartConfiguratorObject object,String value, int row) {
		//for containers do not sync even if master config
		//McGuiUtils.ErrorMsg needSync = McGuiUtils.INSTANCE.needSynchronized(object);
		//if(needSync == McGuiUtils.ErrorMsg.CANCEL)
		//	return needSync;
		if(dataObj.get(row).get(0).equals(XMLElements.MAXCONTAINERS.toString())) {
			String qrValue = (String) value;
			int qr = 0;
			try {
				qr = Integer.parseInt(qrValue);
			} catch(NumberFormatException e) {
					JOptionPane.showMessageDialog(null, Resources.get("INVALIDQUERYRESTRICTION"));
					return McGuiUtils.ErrorMsg.INVALID_QUERY_RESTRICTION;				
			}
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), ""+qr, this.selectedDs);
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), ""+qr, this.selectedDs);
				}
			}*/
		} else if(dataObj.get(row).get(0).equals(XMLElements.INDEPENDENTQUERYING.toString())) {
			boolean b  = Boolean.parseBoolean(value);
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), Boolean.toString(b), this.selectedDs);
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), Boolean.toString(b), this.selectedDs);
				}
			}*/
		}else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), value, this.selectedDs);
				}
			}*/
		}else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), value, this.selectedDs);
				}
			}*/			
		}else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), value, this.selectedDs);
				}
			}*/		
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			if(!MartController.getInstance().renameContainer((Container)object, value))
				return McGuiUtils.ErrorMsg.CANCEL;
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().renameContainer(c, value);
				}
			}	*/	
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.HIDE.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), value, this.selectedDs);
				}
			}*/			
		}else if (dataObj.get(row).get(0).equals(XMLElements.MAXATTRIBUTES.toString())) {
			String qrValue = (String) value;
			int qr = 0;
			try {
				qr = Integer.parseInt(qrValue);
			} catch(NumberFormatException e) {
					JOptionPane.showMessageDialog(null, Resources.get("INVALIDQUERYRESTRICTION"));
					return McGuiUtils.ErrorMsg.INVALID_QUERY_RESTRICTION;				
			}
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), ""+qr, this.selectedDs);
			/*if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Container> clist = McGuiUtils.INSTANCE.findContainerInOtherConfigs((Container)object);
				for(Container c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), ""+qr, this.selectedDs);
				}
			}*/
		}else {
			object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
		}
		return McGuiUtils.ErrorMsg.NO_MASTER_CONFIG;
	}
	
	private McGuiUtils.ErrorMsg setValueForAttribute(MartConfiguratorObject object,String value, int row) {
		McGuiUtils.ErrorMsg needSync = McGuiUtils.INSTANCE.needSynchronized(object);
		if(needSync == McGuiUtils.ErrorMsg.CANCEL)
			return needSync;
		if(dataObj.get(row).get(0).equals(XMLElements.VALUE.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {				
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c,dataObj.get(row).get(0), value, this.selectedDs);
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.ATTRIBUTELIST.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			((org.biomart.objects.objects.Attribute)object).updateAttributeList(value);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				
				for(Attribute c: clist) {
					c.updateAttributeList(value);
				}
			}			
		}else if(dataObj.get(row).get(0).equals(XMLElements.LINKOUTURL.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value, this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {				
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value, this.selectedDs);
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.POINTEDDATASET.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			((org.biomart.objects.objects.Attribute)object).setPointedDatasetName(value);
			((org.biomart.objects.objects.Attribute)object).synchronizedFromXML();
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {				
				for(Attribute c: clist) {
					c.setPointedDatasetName(value);
					c.synchronizedFromXML();
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.POINTEDMART.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			((org.biomart.objects.objects.Attribute)object).setPointedMartName(value);
			((org.biomart.objects.objects.Attribute)object).synchronizedFromXML();
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				
				for(Attribute c: clist) {
					c.setPointedMartName(value);
					c.synchronizedFromXML();
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value, this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {				
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value, this.selectedDs);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value, this.selectedDs);			
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value, this.selectedDs);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value, this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value, this.selectedDs);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.RDF.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value, this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value, this.selectedDs);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			if(!MartController.getInstance().renameAttribute((Attribute)object,value,this.selectedDs))
				return McGuiUtils.ErrorMsg.CANCEL;
			//object.setProperty(XMLElements.NAME, value);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {				
								
				for(Attribute c: clist) {
					MartController.getInstance().renameAttribute(c,value,this.selectedDs);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.HIDE.toString())) {
			//check value
			Boolean b = new Boolean(value);
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), Boolean.toString(b), this.selectedDs);			
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), Boolean.toString(b), this.selectedDs);
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.TABLE.toString())) {
			//check table
			DatasetTable dst = ((org.biomart.objects.objects.Attribute)object)
				.getParentConfig().getMart().getTableByName(value);
			if(dst == null) {
				JOptionPane.showMessageDialog(null,
					    "Table name error",
					    "error",
					    JOptionPane.ERROR_MESSAGE);

				return McGuiUtils.ErrorMsg.TABLE_NAME_ERROR;
			} else {
				((org.biomart.objects.objects.Attribute)object).setDatasetTable(dst);
				if(needSync == McGuiUtils.ErrorMsg.YES ||
						needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
					List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
					for(Attribute c: clist) {
						c.setDatasetTable(dst);
					}
				}			
			}
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.COLUMN.toString())) {
			//check if table is empty
			Attribute att = (org.biomart.objects.objects.Attribute)object;
			DatasetTable dst = att.getDatasetTable();
			if(dst == null) {
				JOptionPane.showMessageDialog(null, "Table is null", "error", JOptionPane.ERROR_MESSAGE);
				return McGuiUtils.ErrorMsg.TABLE_IS_NULL;
			}
			DatasetColumn dc = dst.getColumnByName(value);
			if(dc==null) {
				JOptionPane.showMessageDialog(null, "Column name error", "error", JOptionPane.ERROR_MESSAGE);
				return McGuiUtils.ErrorMsg.COLUMN_NAME_ERROR;				
			}
			att.updateDatasetColumn(dc);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
				for(Attribute c: clist) {
					c.updateDatasetColumn(dc);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.INUSERS.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value, this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value, this.selectedDs);
				}
			}
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.DATATYPE.toString())) {
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs((Attribute)object);
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value, this.selectedDs);
			//object.setProperty(XMLElements.NAME, value);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {				
								
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value, this.selectedDs);
				}
			}
		}
		else {
			object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
		}
		return needSync;
	}
	
	private McGuiUtils.ErrorMsg setValueForFilter(MartConfiguratorObject object, String value, int row) {
		McGuiUtils.ErrorMsg needSync = McGuiUtils.INSTANCE.needSynchronized(object);
		if(needSync == McGuiUtils.ErrorMsg.CANCEL)
			return needSync;
		//TODO dataset specific change
		if(dataObj.get(row).get(0).equals(XMLElements.QUALIFIER.toString())) {
			OperatorType qt = OperatorType.valueFrom(value);
			if(McUtils.isStringEmpty(value.trim()) || qt!=null) {
				((org.biomart.objects.objects.Filter)object).setQualifier(qt);
				if(needSync == McGuiUtils.ErrorMsg.YES ||
						needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
					List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
					for(Filter c: clist) {
						c.setQualifier(qt);
					}
				}			
			}
		}
		//TODO dataset specific change
		else if (dataObj.get(row).get(0).equals(XMLElements.OPERATION.toString())) {
			FilterOperation fo = FilterOperation.valueFrom(value);
			if(fo!=null) {
				((Filter)object).setFilterOperation(fo);
				if(needSync == McGuiUtils.ErrorMsg.YES ||
						needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
					List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
					for(Filter c: clist) {
						c.setFilterOperation(fo);
					}
				}			
			}
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.POINTEDDATASET.toString())) {
			((Filter)object).setPointedDatasetName(value);
			((Filter)object).synchronizedFromXML();
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					c.setPointedDatasetName(value);
					c.synchronizedFromXML();
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.POINTEDMART.toString())) {
			((Filter)object).setPointedMartName(value);
			((Filter)object).synchronizedFromXML();
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					c.setPointedMartName(value);
					c.synchronizedFromXML();
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.POINTEDCONFIG.toString())) {
			((Filter)object).setPointedConfigName(value);
			((Filter)object).synchronizedFromXML();
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					c.setPointedConfigName(value);
					c.synchronizedFromXML();
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			((Filter)object).setProperty(XMLElements.DESCRIPTION, value);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}
			}			
		}else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}
			}			
		}else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.RDF.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			String name = object.getName();
			List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
			
			if(!MartController.getInstance().renameFilter((Filter)object, value,this.selectedDs))
				return McGuiUtils.ErrorMsg.CANCEL;
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {				
								
				for(Filter c: clist) {
					MartController.getInstance().renameFilter(c, value,this.selectedDs);
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.FILTERLIST.toString())) {
			((Filter)object).updateFilterList(value);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					c.updateFilterList(value);
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.ATTRIBUTE.toString())) {
			((Filter)object).setAttribute(value);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					c.setAttribute(value);
				}
			}			
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.SPLITON.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.TYPE.toString())) {
			FilterType fo = FilterType.valueFrom(value);
			if(fo!=null) {
				((Filter)object).setFilterType(fo);
				if(needSync == McGuiUtils.ErrorMsg.YES ||
						needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
					List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
					for(Filter c: clist) {
						c.setFilterType(fo);
					}
				}			
			}
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.HIDE.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}
			}			
		}else if(dataObj.get(row).get(0).equals(XMLElements.INUSERS.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}	
			}
		} else if(dataObj.get(row).get(0).equals(XMLElements.DEPENDSON.toString())) {
			MartController.getInstance().setProperty(object, dataObj.get(row).get(0), value,this.selectedDs);
			if(needSync == McGuiUtils.ErrorMsg.YES ||
					needSync == McGuiUtils.ErrorMsg.NO_MORE_WARNING) {
				List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs((Filter)object);
				for(Filter c: clist) {
					MartController.getInstance().setProperty(c, dataObj.get(row).get(0), value,this.selectedDs);
				}	
			}			
		} else {
			object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
		}
		return needSync;
	}

	private void setValueForConfig(MartConfiguratorObject object, String value, int row) {
		Config config = (org.biomart.objects.objects.Config)object;
		if(dataObj.get(row).get(0).equals(XMLElements.METAINFO.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			object.setProperty(XMLElements.NAME, value);
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DEFAULT.toString())) {
			Boolean b = new Boolean(value);
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), Boolean.toString(b), this.selectedDs);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DATASETDISPLAYNAME.toString())) {
			//check valid
			if(!checkSchemaPartitionValue(value,config.getMart())) {
				JOptionPane.showMessageDialog(null, "none valid", "error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DATASETHIDEVALUE.toString())) {
			//check valid
			if(!checkSchemaPartitionValue(value,config.getMart())) {
				JOptionPane.showMessageDialog(null, "none valid", "error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);			
		}else if(dataObj.get(row).get(0).equals(XMLElements.MASTER.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
		}else if(dataObj.get(row).get(0).equals(XMLElements.READONLY.toString())) {
			MartController.getInstance().setProperty(object,dataObj.get(row).get(0), value, this.selectedDs);
		}else {
			object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
		}

	}
	
	
	private void setValueForRDF(MartConfiguratorObject object, String value, int row) {
		RDFClass rdfclass = (org.biomart.objects.objects.RDFClass)object;
		if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			object.setProperty(XMLElements.NAME, value);
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			rdfclass.setInternalName(value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			rdfclass.setDisplayName(value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			rdfclass.setDescription(value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.HIDE.toString())) {
			rdfclass.setHideValue(Boolean.parseBoolean(value));
		}else if(dataObj.get(row).get(0).equals(XMLElements.VALUE.toString())) {
			rdfclass.setProperty(XMLElements.VALUE, value);
		}else {
			object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
		}

	}

	private boolean checkSchemaPartitionValue(String value, Mart mart) {
		if(!McUtils.hasPartitionBinding(value))
			return false;
		//it has to be p0
		List<String> ptList = McUtils.extractPartitionReferences(value);
		if(ptList.size()>3)
			return false;
		if(!McUtils.getPartitionTableName(ptList.get(1)).equals("p0"))
			return false;
		if(McUtils.getPartitionColumnValue(ptList.get(1))>=mart.getSchemaPartitionTable().getTotalColumns())
			return false;
		return true;
	}
	
	private void setValueForGuiContainer(MartConfiguratorObject object,String value, int row) {
		if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			object.setProperty(XMLElements.NAME, value);
		} else if(dataObj.get(row).get(0).equals(XMLElements.INTERNALNAME.toString())) {
			object.setProperty(XMLElements.INTERNALNAME, value);
		} else if(dataObj.get(row).get(0).equals(XMLElements.DISPLAYNAME.toString())) {
			object.setProperty(XMLElements.DISPLAYNAME, value);
		}
		else if(dataObj.get(row).get(0).equals(XMLElements.DESCRIPTION.toString())) {
			((org.biomart.objects.portal.GuiContainer)object).setProperty(XMLElements.DESCRIPTION, value);
		}else if(dataObj.get(row).get(0).equals(XMLElements.HIDE.toString())) {
			//check value
			Boolean b = new Boolean(value);
			object.setProperty(XMLElements.HIDE, Boolean.toString(b));
		}else {
			object.setProperty(XMLElements.valueFrom(dataObj.get(row).get(0)), value);
		}
	}
	
	private void setValueForLink(MartConfiguratorObject object,String value, int row) {
		Link link = (Link)object;
		//TODO dataset specific change
		if(dataObj.get(row).get(0).equals(XMLElements.NAME.toString())) {
			link.setProperty(XMLElements.NAME, value);
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.POINTEDMART.toString())) {
			Mart mart = ((Config)link.getParent()).getMart().getMartRegistry().getMartByName(value);
			if(mart == null) {
				JOptionPane.showMessageDialog(null, "mart error", "error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			link.setPointerMart(mart);
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.POINTEDCONFIG.toString())) {
			Mart pointedMart = link.getPointedMart();
			if(pointedMart == null) {
				JOptionPane.showMessageDialog(null, "pointedmart null", "error", JOptionPane.ERROR_MESSAGE);
				return;				
			}
			Config config = pointedMart.getConfigByName(value);
			if(config == null) {
				JOptionPane.showMessageDialog(null, "config error", "error", JOptionPane.ERROR_MESSAGE);
				return;				
			}
			link.setPointedConfig(config);
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.FILTERS.toString())) {
			Config config = link.getPointedConfig();
			String[] filters = value.split(",");
			link.getFilterList().clear();
			for(String filterStr: filters) {
				Filter filter = config.getFilterByName(null, filterStr, true);
				if(filter == null) {
					JOptionPane.showMessageDialog(null, "filter error", "error", JOptionPane.ERROR_MESSAGE);
					return;									
				}
				link.addFilter(filter);
			}
			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.ATTRIBUTES.toString())) {
			Config config = link.getParentConfig();
			if(config == null) {
				JOptionPane.showMessageDialog(null, "config error", "error", JOptionPane.ERROR_MESSAGE);
				return;				
			}
			String[] attributes = value.split(",");
			link.getAttributeList().clear();
			for(String attributeStr: attributes) {
				Attribute attribute = config.getAttributeByName(null, attributeStr, true);
				if(attribute == null) {
					JOptionPane.showMessageDialog(null, "attribute error", "error", JOptionPane.ERROR_MESSAGE);
					return;									
				}
				link.addAttribute(attribute);
			}			
		}
		//TODO dataset specific change
		else if(dataObj.get(row).get(0).equals(XMLElements.DATASETS.toString())) {
			link.setProperty(XMLElements.DATASETS, value);
		}
		//if link is valid, recreate related exportable/importable
		//check if the link is valid
		if(Validation.revalidate(link) == ValidationStatus.VALID) {
			link.updatePortable();
		}
	}

	public MartConfiguratorObject getMcObject() {
		return this.mcObj;
	}
	
	public List<List<String>> populateAttributeTable(org.jdom.Element element) {
		List<List<String>> at = new ArrayList<List<String>>();
		@SuppressWarnings("unchecked") //uncheck jdom warning		
		List<Element> elementList = element.getChildren("item");
		for(Element e: elementList) {
			if(!"1".equals(e.getAttributeValue("visible")))
				continue;
			List<String> item = new ArrayList<String>();
			String name = e.getAttributeValue("name");
			item.add(name);
			String elementType = element.getAttributeValue("name");
			String value = "";
			XMLElements xe = null;
			//handle special case for link attributes display names
			if(elementType.equals("link") && name.equals("attributes")){
				Link link = (Link)this.mcObj;
				for(Attribute a: link.getAttributeList()){					
					value += a.getDisplayName();
					if(link.getAttributeList().indexOf(a) != link.getAttributeList().size()-1)
						value += ",";
				}
			}else if(elementType.equals("link") && name.equals("filters")){
				continue;
			}else{
				xe = XMLElements.valueFrom(name);
				value = this.mcObj.getPropertyValue(xe);
			}
			//use the default value
			if(McUtils.isStringEmpty(value) && null!=e.getChild("editor") && null!=xe) {
				Element editorE = e.getChild("editor");
				if(editorE.getAttributeValue("type").equals("boolean")) {
					value=Boolean.toString(false);
					this.mcObj.setProperty(xe, value);
				}
			}
			if(this.selectedDs!=null && "1".equals(System.getProperty("flattenpartition")) && McUtils.hasPartitionBinding(value)) {
				String realValue = McUtils.getRealName(value, this.selectedDs);
				if(realValue!=null)
					value = realValue;
				item.add(value);
			}else 
				item.add(value);
			at.add(item);
		}
		return at;
	}
	

}
