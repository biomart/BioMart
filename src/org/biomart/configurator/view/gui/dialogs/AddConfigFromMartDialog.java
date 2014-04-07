package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.common.resources.Resources;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.portal.MartPointer;

public class AddConfigFromMartDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MartRegistry registry;
	private JList martList;
	private List<Mart> selectedMarts;
	private boolean isReport;
	private List<Mart> marts = new ArrayList<Mart>();
	
	public AddConfigFromMartDialog(MartRegistry registry, boolean isReport,List<MartPointer> marts) {
		this.registry = registry;
		this.isReport = isReport;
		if(marts != null){
			for(MartPointer m: marts){
				this.marts.add(m.getMart());
			}
		}
		if(init())
		{
			this.setModal(true);
			this.pack();
			this.setLocationRelativeTo(null);
			this.setVisible(true);
		}
	}
	
	private boolean init() {
		this.setTitle("Choose the main data source");
		this.setPreferredSize(new Dimension(300,400));
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.setActionCommand(Resources.get("OK"));
		okButton.addActionListener(this);
		
		buttonPanel.add(okButton);
		
		DefaultListModel model = new DefaultListModel();
		martList = new JList(model);
		martList.setSize(200,300);
		martList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane sp = new JScrollPane(martList);
		
		//add mart
		if(this.isReport){
			for(Mart mart : registry.getMartList()) {
				if(marts.isEmpty())
					model.addElement(mart);
				else if(!marts.contains(mart))
					model.addElement(mart);
			}
		}
		else{
			for(Mart mart : registry.getMartList()) {
				model.addElement(mart);
			}
		}
		//if model has no mart, exit dialog
		if(this.isReport && model.isEmpty()){
			JOptionPane.showMessageDialog(this, "There is no available sources to create a report from");
			return false;
		}
		
		content.add(sp,BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(Resources.get("OK"))) {
			if(martList.getSelectedValue() == null)
				return;
			this.selectedMarts = new ArrayList<Mart>();
			if(this.isReport) {
				this.selectedMarts.add((Mart)martList.getSelectedValue());
			}else {
				Object[] values = martList.getSelectedValues();
				for(Object obj: values) {
					this.selectedMarts.add((Mart)obj);
				}
			}
			this.setVisible(false);
			this.dispose();
		}	
	}
	
	public List<Mart> getSelectedMart() {
		return this.selectedMarts;
	}
	
}