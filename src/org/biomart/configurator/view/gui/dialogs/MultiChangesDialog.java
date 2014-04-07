package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.objects.objects.MartConfiguratorObject;


public class MultiChangesDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<McTreeNode> treeNodes;
	private JList list;
	private JTextArea textArea;
	private String propertyName;
	private boolean saved = false;
	
	public MultiChangesDialog(List<McTreeNode> treeNodes, String propertyName) {
		this.treeNodes = treeNodes;
		this.propertyName = propertyName;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}

	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		JPanel inputPanel = new JPanel();
		JPanel textPanel = new JPanel();
		
		this.textArea = new JTextArea(200,200);
		JScrollPane sp2 = new JScrollPane(textArea); 
		sp2.setPreferredSize(new Dimension(200,200));
		textPanel.add(sp2);
		
		DefaultListModel model = new DefaultListModel();
					
		for(McTreeNode treeNode: this.treeNodes) {
			model.addElement(treeNode.getUserObject());
		}

		this.list = new JList(model);
		JScrollPane sp = new JScrollPane(list);
		sp.setPreferredSize(new Dimension(200,200));
		inputPanel.add(sp);
		
		
		JButton upButton = new JButton(Resources.get("UP"));
		upButton.addActionListener(this);
		upButton.setActionCommand(Resources.get("UP"));
		JButton downButton = new JButton(Resources.get("DOWN"));
		downButton.addActionListener(this);
		downButton.setActionCommand(Resources.get("DOWN"));

		JButton saveButton = new JButton(Resources.get("SAVEANDQUIT"));
		saveButton.addActionListener(this);
		saveButton.setActionCommand(Resources.get("SAVEANDQUIT"));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		buttonPanel.add(upButton,c);
		c.gridy = 1;
		buttonPanel.add(downButton,c);
		c.gridy = 2;
		buttonPanel.add(saveButton,c);
		
		content.add(buttonPanel,BorderLayout.CENTER);
		content.add(inputPanel,BorderLayout.WEST);
		content.add(textPanel,BorderLayout.EAST);
		this.add(content);
		this.setTitle("Set "+this.propertyName);
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(Resources.get("UP"))) {
			int index = this.list.getSelectedIndex();
			if(index == 0)
				return;
			DefaultListModel model = (DefaultListModel)this.list.getModel();			
			Object obj = model.remove(index);
			model.insertElementAt(obj, index-1);	
		}else if(e.getActionCommand().equals(Resources.get("DOWN"))) {
			int index = this.list.getSelectedIndex();
			DefaultListModel model = (DefaultListModel)this.list.getModel();
			if(index == model.getSize()-1)
				return;
			Object obj = model.remove(index);
			model.insertElementAt(obj, index+1);
		}else if(e.getActionCommand().equals(Resources.get("SAVEANDQUIT"))) {
			DefaultListModel model = (DefaultListModel)this.list.getModel(); 	
			String text = this.textArea.getText();
			String[] lines = text.split("\\\n");
			if(model.getSize()!=lines.length) {
				JOptionPane.showMessageDialog(this, "data size not matched");
				return;
			}

			for(int i=0; i< model.getSize(); i++) {
				MartConfiguratorObject obj = (MartConfiguratorObject)model.get(i);
				obj.setMcValue(this.propertyName, lines[i]);
			}
			this.saved = true;
			this.setVisible(false);
		}				
	}
	
	public boolean isSaved() {
		return this.saved;
	}
}