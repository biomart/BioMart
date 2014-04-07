package org.biomart.configurator.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import org.biomart.configurator.jdomUtils.McTreeNode;

public class McConfigTree extends JTree {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public McConfigTree(McTreeNode node) {
		super(node);
		this.putClientProperty("JTree.lineStyle", "Angled");
		this.setEditable(false);
		this.setShowsRootHandles(true);
		
		this.addMouseListener(new MouseAdapter ()  {
            public void mousePressed (MouseEvent e)  {
            }

            public void mouseReleased(MouseEvent e)  {
                if (e.getClickCount () == 2)  {
               //     doubleClick (e);
                }
            }
		});
	}
	
	public McConfigTree(TreeModel model) {
		super(model);
		this.putClientProperty("JTree.lineStyle", "Angled");
		this.setEditable(false);
		this.setShowsRootHandles(true);
	}
	
}