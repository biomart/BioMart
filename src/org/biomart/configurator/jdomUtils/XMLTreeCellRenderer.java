
package org.biomart.configurator.jdomUtils;


import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.McFont;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.PartitionTable;

/**
 * Changes how the tree displays elements.
 */
public class XMLTreeCellRenderer extends DefaultTreeCellRenderer {
     
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean validate = false;
	//remove icons
    public XMLTreeCellRenderer() {
    	validate = Boolean.parseBoolean(System.getProperty("guivalidation"));
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

    	McTreeNode node = (McTreeNode)value;
        MartConfiguratorObject mcObject = (MartConfiguratorObject)node.getUserObject();

    	this.setMcIcon(mcObject);
    	if(tree instanceof MartConfigTree) {
	    	if(McUtils.isStringEmpty(mcObject.getDisplayName()))
	    		this.setText(mcObject.toString());
	    	else {
	    		MartConfigTree mcTree = (MartConfigTree)tree;
	    		this.setText(node.toString(mcTree.getSelectedDataset()));
	    	}
    	}else
    		this.setText(mcObject.toString());
    	if(mcObject.getObjectStatus()!=ValidationStatus.VALID)
    		this.setToolTipText(mcObject.getPropertyValue(XMLElements.ERROR));
    	else
    		this.setToolTipText(mcObject.getName());
    	this.setColor(mcObject);

        return this;
        
    }
    
    /**
     * should replaced by setIcon
     * @param mcObject
     */
    private void setColor(MartConfiguratorObject mcObject) {
    	//color for dataset
    	this.setFont(McFont.getInstance().getDefaultFont());
    	switch(mcObject.getNodeType()) {
    	case TABLE:    		
        	DatasetTable dst = (DatasetTable) mcObject;
        	if(dst.getType().equals(DatasetTableType.MAIN) || dst.getType().equals(DatasetTableType.MAIN_SUBCLASS) ) {
        		this.setFont(McFont.getInstance().getBoldFont());
        	}else
        		this.setFont(McFont.getInstance().getDefaultFont());
        	break;
    	case PARTITIONTABLE:
    		PartitionTable pt = (PartitionTable) mcObject;
    		if(pt.getPartitionType().equals(PartitionType.DATASET)) {
    			setForeground(Color.RED);
    		}else if(pt.getPartitionType().equals(PartitionType.DIMENSION)) {
    			setForeground(Color.ORANGE);
    		}else
    			setForeground(Color.BLACK);
    		
    		break;
    	case ATTRIBUTE:
    	case FILTER:
    		
    	}
    	if(mcObject.isHidden())
    		setForeground(Color.LIGHT_GRAY);
    	else if(mcObject.getObjectStatus()!=ValidationStatus.VALID && mcObject.getNodeType() != McNodeType.CONTAINER)
    		setForeground(Color.LIGHT_GRAY);
    	else if(mcObject.isVisibleModified()) {
    		setForeground(Color.GREEN);
    	} else 
    		setForeground(Color.BLACK);
    		
    }
    
    private void setMcIcon(MartConfiguratorObject object) {
    	if(object == null) {
    		Log.error("object is null");
    		return;
    	}
    	if(object.getNodeType() == null) {
    		return;
    	}
		Icon icon = McUtils.createImageIcon(this.getImageName(object));
		this.setIcon(icon);    	
    }
    
    private String getImageName(MartConfiguratorObject obj) {
    	StringBuilder sb = new StringBuilder("images/");
    	
    	sb.append(this.getObjectImageBase(obj)).append(this.getDirtyExt(obj)).
    	append(this.getHideExt(obj)).append(this.getWarningExt(obj)).append(this.getValidExt(obj)).append(".gif");
    	
    	return sb.toString();
    }
    
    private String getObjectImageBase(MartConfiguratorObject obj) {
    	String result = obj.getNodeType().toString();
    	if(obj instanceof Attribute) {
    		Attribute att = (Attribute)obj;
    		if(att.isAttributeList())
    			result = "attribute_l";
    		else if(att.isPointer())
    			result = "attribute_p";
    	} else if(obj instanceof Filter) {
    		Filter fil = (Filter)obj;
    		if(fil.isFilterList())
    			result = "filter_l";
    		else if(fil.isPointer())
    			result = "filter_p";
    	} 
    	return result;
    }
    
    private String getWarningExt(MartConfiguratorObject obj) {
    	return "";
    }
    
    private String getDirtyExt(MartConfiguratorObject obj) {
    	return "";
    }
    
    private String getHideExt(MartConfiguratorObject obj) {
    	if(obj.isHidden())
    		return "_h";
    	else
    		return "";
    }
    
    private String getValidExt(MartConfiguratorObject obj) {
    	//skip config validation
    	if(obj instanceof Config)
    		return "";
    	if(!validate)
    		return "";
    	else if(obj.getObjectStatus()!=ValidationStatus.VALID)
    		return "_w";
    	return "";
    }
        
}

