package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.tree.TreePath;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.DisplayNameComparator;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.configurator.view.component.SourceConfigPanel;
import org.biomart.configurator.view.component.container.ListWithToolTip;
import org.biomart.configurator.view.dnd.AttributeTransferHandler;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Filter;

public class AddAttributeListDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField nameTF;
	private JTextField displayNameTF;
	private JTextField descriptionTF;
	private Attribute attributeList;
	private JList targetList;
	//private ListWithToolTip sourceList;
	private final Config config;
	private final boolean edit;
	private SourceConfigPanel ssp;
	
	public AddAttributeListDialog(Config config, JDialog parent) {
		super(parent);
		this.config = config;
		this.edit = false;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	public AddAttributeListDialog(Config config,Attribute node, JDialog parent) {
		super(parent);
		this.config = config;
		this.edit = true;
		this.attributeList = node;
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
				if(createAttributeList())
					AddAttributeListDialog.this.setVisible(false);
					AddAttributeListDialog.this.ssp.getConfigTree().getModel().removeFilter(McNodeType.FILTER);
					AddAttributeListDialog.this.ssp.getConfigTree().getModel().removeFilter("attributeList");
			}			
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					AddAttributeListDialog.this.setVisible(false);
					AddAttributeListDialog.this.ssp.getConfigTree().getModel().removeFilter(McNodeType.FILTER);
					AddAttributeListDialog.this.ssp.getConfigTree().getModel().removeFilter("attributeList");
			}						
		});
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		JLabel nameLabel = new JLabel(XMLElements.NAME.toString());
		inputPanel.add(nameLabel, c);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent w) {
				AddAttributeListDialog.this.ssp.getConfigTree().getModel().removeFilter(McNodeType.FILTER);
				AddAttributeListDialog.this.ssp.getConfigTree().getModel().removeFilter("attributeList");
			}
		});
		
		this.nameTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.nameTF, c);
		
		JLabel displayName = new JLabel(XMLElements.DISPLAYNAME.toString());
		c.gridx = 0;
		c.gridy = 1;
		inputPanel.add(displayName,c);
		
		this.displayNameTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.displayNameTF, c);
		
		JLabel descriptionName = new JLabel(XMLElements.DESCRIPTION.toString());
		c.gridx = 0;
		c.gridy = 2;
		inputPanel.add(descriptionName,c);
		
		this.descriptionTF = new JTextField(20);
		c.gridx = 1;
		inputPanel.add(this.descriptionTF,c);

		JPanel centralPanel = this.createCentralPanel();
		
		content.add(inputPanel, BorderLayout.NORTH);
		content.add(centralPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle(Resources.get("CREATEATTRIBUTELISTTITLE"));
		this.setFieldEnable(!edit);
		this.reloadTargetList();
	}
	
	private JPanel createCentralPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		
		JPanel centralPanel = new JPanel();
		//DefaultListModel smodel = new DefaultListModel();
		DefaultListModel tmodel = new DefaultListModel();
		targetList = new JList(tmodel){
			public String getToolTipText(MouseEvent evt) {
		        // Get item index
		        int index = locationToIndex(evt.getPoint());

		        // Get item
		        Attribute item = (Attribute)getModel().getElementAt(index);

		        // Return the tool tip text
		        return item.getName();
		    }
		};
		/*sourceList = new ListWithToolTip(smodel);
		MouseAdapter smouseListener = new MouseAdapter() {
		    public void mouseClicked(MouseEvent e) {
		        if (e.getClickCount() == 2) {
		        	addAttributeToTargetList();
		         }
		    }
		};
		sourceList.addMouseListener(smouseListener);*/
		MouseAdapter tmouseListener = new MouseAdapter() {
		    public void mouseClicked(MouseEvent e) {
		        if (e.getClickCount() == 2) {
		        	removeAttributeFromTargetList();
		         }
		    }
		};
		targetList.addMouseListener(tmouseListener);
		targetList.setDragEnabled(true);
		targetList.setDropMode(DropMode.INSERT);
		targetList.setTransferHandler(new AttributeTransferHandler());
		
		ssp = new SourceConfigPanel(config,false);
		ssp.getSourceLabel().setVisible(false);
		ssp.getSourceCB().setVisible(false);
		//add mouse listener to config tree
		final MartConfigTree tree = ssp.getConfigTree();
		tree.getModel().addFilter(McNodeType.FILTER, null);
		tree.getModel().addFilter("attributeList");
		
		tree.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if(e.getClickCount() == 2){
					McTreeNode treeNode = (McTreeNode)selPath.getLastPathComponent();
					if(treeNode.getObject() instanceof Attribute){
						Attribute attribute = (Attribute)treeNode.getObject();
						addAttributeToTargetList(attribute);
					}
				}
			}
		});
		
		
		JScrollPane tsp = new JScrollPane(targetList);
		ssp.setPreferredSize(new Dimension(300,400));
		tsp.setPreferredSize(new Dimension(300,400));
		//add all source to the list
		/*Collection<Attribute> allAttributes = config.getMart().getMasterConfig().getAllAttributes();
		List<Attribute> allAtts = new ArrayList<Attribute>(allAttributes);
		Collections.sort(allAtts, new DisplayNameComparator());
		for(Attribute attribute:allAtts) {
			if(attribute.isAttributeList())
				continue;
			smodel.addElement(attribute);
		}*/
		centralPanel.add(ssp);
		centralPanel.add(tsp);

		JLabel label = new JLabel(Resources.get("LISTDIALOGINFO"));
		panel.add(label,BorderLayout.SOUTH);
		panel.add(centralPanel,BorderLayout.CENTER);
		return panel;
	}
	
	
	private boolean createAttributeList() {
		if(McUtils.isStringEmpty(this.nameTF.getText())) {
			JOptionPane.showMessageDialog(this, Resources.get("EMPTYNAME"));
			return false;
		}
		if(targetList.getModel().getSize() == 0) {
			JOptionPane.showMessageDialog(this, "empty attributelist");
			return false;			
		}
		if(!edit && !McGuiUtils.INSTANCE.checkUniqueAttributeInConfig(config, this.nameTF.getText())) {
			JOptionPane.showMessageDialog(this,
				    this.nameTF.getText() + " already exists.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(edit) {
			this.attributeList.clearAttributeList();
			DefaultListModel model = (DefaultListModel)targetList.getModel();
			for(int i=0; i<model.getSize();i++) 
				this.attributeList.addAttribute((Attribute)model.get(i));			
		}else {
			this.attributeList = new Attribute(this.nameTF.getText(), this.displayNameTF.getText());
			this.attributeList.setDescription(this.descriptionTF.getText());
			DefaultListModel model = (DefaultListModel)targetList.getModel();
			for(int i=0; i<model.getSize();i++) {
				this.attributeList.addAttribute((Attribute)model.get(i));
			}
		}
		return true;
	}
	
	private void addAttributeToTargetList(Attribute attribute) {
		//Attribute attribute = (Attribute)sourceList.getSelectedValue();
		DefaultListModel model = (DefaultListModel)targetList.getModel();
		//check if the item exist
		boolean exist = false;
		for(int i=0; i<model.getSize();i++) {
			if(attribute.getName().equals(((Attribute)model.get(i)).getName())) {
				exist = true;
				break;
			}
		}
		if(!exist) {
			model.addElement(attribute);
		}
	}
	
	private void removeAttributeFromTargetList() {
		DefaultListModel model = (DefaultListModel)targetList.getModel();
		int index = targetList.getSelectedIndex();
		model.remove(index);
	}
	
	public Attribute getCreatedAttributeList() {
		return this.attributeList;
	}

	private void setFieldEnable(boolean b) {
		this.nameTF.setEditable(b);
		this.displayNameTF.setEnabled(b);
		this.descriptionTF.setEditable(b);
	}
	
	private void reloadTargetList() {
		if(this.edit) {
			this.nameTF.setText(this.attributeList.getName());
			this.displayNameTF.setText(this.attributeList.getDisplayName());
			this.descriptionTF.setText(this.attributeList.getDescription());
			DefaultListModel model = (DefaultListModel)targetList.getModel();
			model.clear();
			for(Attribute a:this.attributeList.getAttributeList()) {
				model.addElement(a);
			}
		}
	}
}