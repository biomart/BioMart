package org.biomart.configurator.view.gui.dialogs;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.jdomUtils.XMLTreeCellRenderer;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.McSearchTree;
import org.biomart.configurator.view.component.SourceConfigPanel;

public class SearchResultDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private McTreeNode root;
	private MartConfigTree tree;
	private SourceConfigPanel scp;
	
	public SearchResultDialog(McTreeNode root, MartConfigTree tree, SourceConfigPanel scp, String title, JDialog parent) {
		super(parent);
		this.root = root;
		this.tree = tree;
		this.scp = scp;
		this.setTitle(title);
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	
	
	private void init() {
		McSearchTree lTree = new McSearchTree(root,tree, scp);
        XMLTreeCellRenderer treeCellRenderer = new XMLTreeCellRenderer();
        lTree.setCellRenderer(treeCellRenderer);
        JScrollPane ltreeScrollPane = new JScrollPane(lTree);
        this.setPreferredSize(new Dimension(450, 600));
        this.add(ltreeScrollPane);
	}
	
}