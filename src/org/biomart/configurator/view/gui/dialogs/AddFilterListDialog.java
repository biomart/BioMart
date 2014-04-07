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
import org.biomart.configurator.view.dnd.FilterTransferHandler;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Filter;


public class AddFilterListDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField nameTF;
	private JTextField displayNameTF;
	private JTextField descriptionTF;
	private Filter filterList;
	private JList targetList;
	//private ListWithToolTip sourceList;
	private final Config config;
	private final boolean edit;
	
	private SourceConfigPanel ssp;
	
	public AddFilterListDialog(Config config, JDialog parent) {
		super(parent);
		this.config = config;
		this.edit = false;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	public AddFilterListDialog(Config config, Filter filterList, JDialog parent) {
		super(parent);
		this.config = config;
		this.filterList = filterList;
		this.edit = true;
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
				if(createFilterList()){
					AddFilterListDialog.this.setVisible(false);
					AddFilterListDialog.this.ssp.getConfigTree().getModel().removeFilter(McNodeType.ATTRIBUTE);
					AddFilterListDialog.this.ssp.getConfigTree().getModel().removeFilter("filterList");
				}
			}			
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					AddFilterListDialog.this.setVisible(false);
					AddFilterListDialog.this.ssp.getConfigTree().getModel().removeFilter(McNodeType.ATTRIBUTE);
					AddFilterListDialog.this.ssp.getConfigTree().getModel().removeFilter("filterList");
			}						
		});
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent w) {
				AddFilterListDialog.this.ssp.getConfigTree().getModel().removeFilter(McNodeType.ATTRIBUTE);
				AddFilterListDialog.this.ssp.getConfigTree().getModel().removeFilter("filterList");
			}
		});
		
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		JLabel nameLabel = new JLabel(XMLElements.NAME.toString());
		inputPanel.add(nameLabel, c);
		
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
		this.setTitle(Resources.get("CREATEFILTERLISTTITLE"));
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
		        Filter item = (Filter)getModel().getElementAt(index);

		        // Return the tool tip text
		        return item.getName();
		    }
		};
		//sourceList = new ListWithToolTip(smodel);
		/*
		MouseAdapter smouseListener = new MouseAdapter() {
		    public void mouseClicked(MouseEvent e) {
		        if (e.getClickCount() == 2) {
		        	addFilterToTargetList();
		         }
		    }
		};
		sourceList.addMouseListener(smouseListener);
		*/
		MouseAdapter tmouseListener = new MouseAdapter() {
		    public void mouseClicked(MouseEvent e) {
		        if (e.getClickCount() == 2) {
		        	removeFilterFromTargetList();
		         }
		    }
		};
		targetList.addMouseListener(tmouseListener);
		targetList.setDragEnabled(true);
		targetList.setDropMode(DropMode.INSERT);
		targetList.setTransferHandler(new FilterTransferHandler());
		
		ssp = new SourceConfigPanel(config,false);
		ssp.getSourceLabel().setVisible(false);
		ssp.getSourceCB().setVisible(false);
		//add mouse listener to config tree
		final MartConfigTree tree = ssp.getConfigTree();
		tree.getModel().addFilter(McNodeType.ATTRIBUTE, null);
		tree.getModel().addFilter("filterList");
		
		tree.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if(e.getClickCount() == 2){
					McTreeNode treeNode = (McTreeNode)selPath.getLastPathComponent();
					if(treeNode != null && treeNode.getObject() instanceof Filter){
						Filter filter = (Filter)treeNode.getObject();
						addFilterToTargetList(filter);
					}
				}
			}
		});
		JScrollPane tsp = new JScrollPane(targetList);
		ssp.setPreferredSize(new Dimension(300,400));
		tsp.setPreferredSize(new Dimension(300,400));
		//add all source to the list
		/*
		Collection<Filter> allFilters = config.getMart().getMasterConfig().getAllFilters();
		List<Filter> allFils = new ArrayList<Filter>(allFilters);
		Collections.sort(allFils, new DisplayNameComparator());
		for(Filter attribute:allFils) {
			if(attribute.isFilterList())
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

	
	private void addFilterToTargetList(Filter filter) {
		DefaultListModel model = (DefaultListModel)targetList.getModel();
		//check if the item exist
		boolean exist = false;
		for(int i=0; i<model.getSize();i++) {
			if(filter.getName().equals(((Filter)model.get(i)).getName())) {
				exist = true;
				break;
			}
		}
		if(!exist) {
			model.addElement(filter);
		}
	}
	
	private void removeFilterFromTargetList() {
		DefaultListModel model = (DefaultListModel)targetList.getModel();
		int index = targetList.getSelectedIndex();
		model.remove(index);
	}

	
	private boolean createFilterList() {
		if(McUtils.isStringEmpty(this.nameTF.getText())) {
			JOptionPane.showMessageDialog(this, Resources.get("EMPTYNAME"));
			return false;
		}
		if(targetList.getModel().getSize() == 0) {
			JOptionPane.showMessageDialog(this, "empty filterlist");
			return false;			
		}

		String displayName = this.displayNameTF.getText();
		if(McUtils.isStringEmpty(displayName))
			displayName = this.nameTF.getText();
		if(!edit && !McGuiUtils.INSTANCE.checkUniqueFilterInMart(config, this.nameTF.getText())){
			JOptionPane.showMessageDialog(this,
				    this.nameTF.getText() + " already exists.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(edit) {
			this.filterList.clearFilterList();
			DefaultListModel model = (DefaultListModel)targetList.getModel();
			for(int i=0; i<model.getSize();i++) 
				this.filterList.addFilter((Filter)model.get(i));			
		}else {
			this.filterList = new Filter(this.nameTF.getText(), displayName);
			this.filterList.setDescription(this.descriptionTF.getText());
			DefaultListModel model = (DefaultListModel)targetList.getModel();
			for(int i=0; i<model.getSize();i++) 
				this.filterList.addFilter((Filter)model.get(i));
		}
		return true;
	}
	
	public Filter getCreatedFilterList() {
		return this.filterList;
	}

	private void setFieldEnable(boolean b) {
		this.nameTF.setEditable(b);
		this.displayNameTF.setEnabled(b);
		this.descriptionTF.setEditable(b);
	}

	private void reloadTargetList() {
		if(this.edit) {
			this.nameTF.setText(this.filterList.getName());
			this.displayNameTF.setText(this.filterList.getDisplayName());
			this.descriptionTF.setText(this.filterList.getDescription());
			DefaultListModel model = (DefaultListModel)targetList.getModel();
			model.clear();
			for(Filter a:this.filterList.getFilterList()) {
				model.addElement(a);
			}
		}
	}
}