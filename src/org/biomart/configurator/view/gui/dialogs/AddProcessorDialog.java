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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.objects.enums.ProcessorType;
import org.biomart.objects.objects.Container;
import org.biomart.objects.portal.MartPointer;
import org.biomart.processors.Processor;
import org.biomart.processors.ProcessorGroup;

public class AddProcessorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JComboBox nameCB;
	private JList list;
	private ProcessorGroup processorGroup;
	private Processor processor;
	
	public AddProcessorDialog(ProcessorGroup pg) {
		this.processorGroup = pg;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(createProcessor())
				AddProcessorDialog.this.setVisible(false);				
			}			
		});

		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		
		JLabel nameLabel = new JLabel(XMLElements.NAME.toString());
		inputPanel.add(nameLabel,c);
		c.gridx = 1;
		
		this.nameCB = new JComboBox();
		for(ProcessorType pt: ProcessorType.values()) {
			this.nameCB.addItem(pt);
		}
		inputPanel.add(this.nameCB,c);
		
		c.gridx = 0;
		c.gridy = 1;
		
		JLabel containersLabel = new JLabel(XMLElements.CONTAINERS.toString());
		inputPanel.add(containersLabel,c);

		
		DefaultListModel model = new DefaultListModel();
		list = new JList(model);
		JScrollPane sp = new JScrollPane(this.list);
		sp.setPreferredSize(new Dimension(200,200));
		c.gridx = 1;
		inputPanel.add(sp,c);
		//get all containers
		List<Container> contList = this.processorGroup.getMartPointer().getConfig().getRootContainer().getAllContainers();
		for(Container con: contList) {
			model.addElement(con);
		}

		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		content.add(inputPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle("add processor");
	}
	
	private boolean createProcessor() {
		Object[] selectedContainers = this.list.getSelectedValues();
		if(selectedContainers.length ==0) {
			JOptionPane.showMessageDialog(this, "no martpointer name");
			return false;
		} 
		else {
			Object o = this.nameCB.getSelectedItem();
			if(o == null)
				return false;
			String nameStr = ((ProcessorType)o).toString();
			this.processor = new Processor(this.processorGroup,nameStr);
			//add containers
			for(Object object: selectedContainers) {
				Container container = (Container)object;
				this.processor.setContainer(container);
			}
			return true;
		}
	}
	
	public Processor getProcessor() {
		return this.processor;
	}
}