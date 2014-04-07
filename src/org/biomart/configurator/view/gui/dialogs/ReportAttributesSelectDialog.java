/**
 * 
 */
package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.UserGroup;

/**
 * @author lyao
 * 
 */
public class ReportAttributesSelectDialog extends JDialog implements
		ActionListener {
	private ObjectController objectCtl;
	private Mart mart;
	private String configName;
	private JList attributeList;
	private List<Attribute> atts;
	private GuiContainer gc;

	public ReportAttributesSelectDialog(ObjectController oc, Mart mart,
			String configName, GuiContainer gc) {

		this.objectCtl = oc;
		this.mart = mart;
		this.configName = configName;
		atts = ObjectController.getAttributesInMain(mart);
		this.gc = gc;
		init();

	}

	public void init() {
		this.setTitle("Please select the identifier field for the report");
		this.setContentPane(this.createAttSlectPanel());
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	public JPanel createAttSlectPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JPanel centerPanel = new JPanel();
		centerPanel.setPreferredSize(new Dimension(400, 600));
		JPanel buttonPanel = new JPanel();

		JButton okButton = new JButton("Select");
		okButton.setActionCommand("select");
		okButton.addActionListener(this);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);

		DefaultListModel model = new DefaultListModel();

		for (Attribute a : atts) {
			if(a.isHidden())
				continue;
			//if mart is url based drop attributes that dont have a filter
			if(mart.isURLbased()){
				if(a.getReferenceFilters().isEmpty())
					continue;
			}				
			model.addElement(a);
			
		}
		this.attributeList = new JList(model){
			public String getToolTipText(MouseEvent evt) {
		        // Get item index
		        int index = locationToIndex(evt.getPoint());

		        // Get item
		        Attribute item = (Attribute)getModel().getElementAt(index);

		        // Return the tool tip text
		        return item.getName();
		    }
		};
		this.attributeList
				.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane attsp = new JScrollPane(this.attributeList);
		attsp.setPreferredSize(new Dimension(380, 550));

		centerPanel.add(attsp);		
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);

		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getActionCommand().equals("select")){
			final UserGroup user = ((McViewPortal)McViews.getInstance().getView(IdwViewType.PORTAL)).getUser();
			//Config newConfig = this.objectCtl.addConfigFromMaster(mart, configName,user);
			Config newConfig = this.objectCtl.addReportConfigFromMaster(mart, configName,user);
			
			int sel = this.attributeList.getSelectedIndex();
			if(sel < 0){
				JOptionPane.showMessageDialog(this, "Please select a attribute first");
				return;
			}else{			
				this.objectCtl.initReportConfig(newConfig, (Attribute)this.attributeList.getSelectedValue(), this.gc);
			}		
			
			this.setVisible(false);
		}else if(e.getActionCommand().equals("cancel")){
			this.setVisible(false);
		}
	}
}
