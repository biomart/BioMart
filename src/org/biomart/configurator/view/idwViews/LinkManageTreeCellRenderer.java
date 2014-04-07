/**
 * 
 */
package org.biomart.configurator.view.idwViews;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Link;

/**
 * @author lyao
 *
 */
public class LinkManageTreeCellRenderer extends DefaultTreeCellRenderer {
	private ImageIcon databaseIcon = McUtils.createImageIcon("images/source.gif");
	private ImageIcon linkIcon = McUtils.createImageIcon("images/link.gif");
	
	public LinkManageTreeCellRenderer() {
		//databaseIcon = McUtils.scale(databaseIcon.getImage(), 0.4, this);
		//setOpenIcon(databaseIcon);
		//setClosedIcon(databaseIcon);
		//setLeafIcon(linkIcon);
	}
	
	
	public int getIconHeight(){
		return databaseIcon.getIconHeight();
	}


	/* (non-Javadoc)
	 * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		// TODO Auto-generated method stub
		JComponent node = (JComponent) super.getTreeCellRendererComponent(tree,
				value, sel, expanded, leaf, row, hasFocus);
		if(value instanceof McTreeNode) {
			McTreeNode treeNode = (McTreeNode) value;
			if(treeNode.getUserObject() instanceof Link)
				setIcon(linkIcon);
			else
				setIcon(databaseIcon);
		} 
			
		return this;
	}
}
