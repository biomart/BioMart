/**
 * 
 */
package org.biomart.configurator.view.idwViews;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;

/**
 * @author lyao
 * 
 */
public class SourceTreeCellRenderer extends DefaultTreeCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ImageIcon addIcon = McUtils.createImageIcon("images/add.gif");
	private ImageIcon databaseIcon = McUtils.createImageIcon("images/source.gif");
	private ImageIcon databaseIndexIcon = McUtils.createImageIcon("images/source_index.gif");
	private ArrayList<Integer> highlightedRows = new ArrayList<Integer>();
	private ArrayList<Color> highlightedColors = new ArrayList<Color>();
	
	private boolean isSelected = false;



	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		// TODO Auto-generated method stub
		JComponent node = (JComponent) super.getTreeCellRendererComponent(tree,
				value, sel, expanded, leaf, row, hasFocus);
		
		isSelected = sel;
		
		if (sel) {
			
			if (highlightedRows.contains(new Integer(row))){
				node.setOpaque(true);
				node.setForeground(this.getTextNonSelectionColor());
				int index = highlightedRows.indexOf(new Integer(row));
				node.setBackground(this.highlightedColors.get(index));
			} else {
				node.setOpaque(true);
				//disable selection
				node.setForeground(this.getTextNonSelectionColor());
				node.setBackground(this.getBackgroundNonSelectionColor());
				//set border
				//node.setBorder(BorderFactory.createLineBorder(getTextSelectionColor()));
			}
		} else {
			node.setOpaque(true);
			if (highlightedRows.contains(new Integer(row))) {
				int index = highlightedRows.indexOf(new Integer(row));
				node.setBackground(this.highlightedColors.get(index));
			} else {
				node.setBackground(this.getBackgroundNonSelectionColor());
			}
		}
		if(value instanceof McTreeNode) {
			setIcon(databaseIcon);
		} else
			setIcon(addIcon);
		return this;
	}

	public void addHighlightRow(int row, Color color) {
		Integer rowObject = new Integer(row + 1);
		if (!highlightedRows.contains(rowObject)) {
			highlightedRows.add(rowObject);
			highlightedColors.add(color);
		}
	}
	
	public boolean hasHighlightRow(int row) {
		Integer rowObject = new Integer(row + 1);
		return highlightedRows.contains(rowObject);
	}

	public void removeHighlights() {
		this.highlightedRows.clear();
		this.highlightedColors.clear();
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.DefaultTreeCellRenderer#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		super.paint(g);
		
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(this.isSelected) {
			g.setColor(backgroundSelectionColor);
			g.drawRect(0, 0, this.getSize().width-1, this.getSize().height-1);
		}
	}
}
