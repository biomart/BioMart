/**
 * 
 */
package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.jdomUtils.XMLTreeCellRenderer;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.McSearchTree;
import org.biomart.configurator.view.component.SourceConfigPanel;

/**
 * @author lyao
 *
 */
public class DeleteReferenceDialog extends JDialog implements ActionListener{
	private static final long serialVersionUID = 1L;
	private McTreeNode root;
	private MartConfigTree tree;
	private SourceConfigPanel scp;
	private boolean isProceed;
	
	public boolean isProceed() {
		return isProceed;
	}



	public DeleteReferenceDialog(McTreeNode root, MartConfigTree tree, SourceConfigPanel scp, String title,JDialog parent) {
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
        this.add(ltreeScrollPane, BorderLayout.CENTER);
        
        JLabel warningLabel = new JLabel("Following references will be affected by deletion, Proceed?");
        this.add(warningLabel, BorderLayout.NORTH);
        
        JButton okButton = new JButton("OK");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        this.add(buttonPanel,BorderLayout.SOUTH);
	}



	@Override
	public void actionPerformed(ActionEvent ae) {
		// TODO Auto-generated method stub
		if(ae.getActionCommand().equals("ok")){
			isProceed = true;
			this.setVisible(false);
		}else if(ae.getActionCommand().equals("cancel")){
			isProceed = false;
			this.setVisible(false);
		}
	}
}
