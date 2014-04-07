package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biomart.common.resources.Resources;
import org.biomart.configurator.utils.treelist.CheckBoxListModel;
import org.biomart.configurator.utils.treelist.LeafCheckBoxList;
import org.biomart.configurator.utils.treelist.LeafCheckBoxNode;
import org.biomart.objects.objects.Mart;

public class MartSelectionDialog extends JDialog implements ActionListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<String> _marts;
	private LeafCheckBoxList martList = new LeafCheckBoxList();
	private JCheckBox importPortalCB;
	private List<String> results = new ArrayList<String>();

	public List<String> getResults() {
		return results;
	}

	public MartSelectionDialog(JDialog parent, List<String> marts){
		super(parent);
		this._marts = marts;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
	
	private void init(){
		for(String mart : _marts){
			CheckBoxListModel  model = (CheckBoxListModel)martList.getModel();
			LeafCheckBoxNode node = new LeafCheckBoxNode(mart, false);
			model.addElement(node);
		}
		this.setLayout(new GridLayout());
		JPanel martsPanel = new JPanel(new BorderLayout());
		martsPanel.setPreferredSize(new Dimension(600,800));
		
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton("Ok");
		okButton.setActionCommand("ok");
		okButton.addActionListener(this);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		importPortalCB = new JCheckBox("Import portal");
		importPortalCB.setSelected(true);
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(importPortalCB);
		
		JLabel martLabel = new JLabel(Resources.get("MARTS"));
		
	    JScrollPane scrollPane = new JScrollPane(martList);
	    martsPanel.add(scrollPane,BorderLayout.CENTER);
	    martsPanel.add(martLabel,BorderLayout.NORTH);
	    martsPanel.add(buttonPanel,BorderLayout.SOUTH);
	    this.add(martsPanel);
	    
		
	}
	
	public boolean isImportPortal(){
		return this.importPortalCB.isSelected();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		// TODO Auto-generated method stub
		if(ae.getActionCommand().equals("ok")){
			
			for(int index : this.martList.getCheckedIndices()){
				results.add(this._marts.get(index));
			}
			
			this.setVisible(false);
		}else if(ae.getActionCommand().equals("cancel")){
			this.setVisible(false);
		}
	}
}
