package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.objects.SearchInfoObject;

public class SearchComponentDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JComboBox componentCB;
	private JComboBox scopeCB;
	private JTextField textTF;
	private JRadioButton byNameRB;
	private JRadioButton byDisplayNameRB;
	private JRadioButton byInternalNameRB;
	private JRadioButton equalRB;
	private JRadioButton likeRB;
	private JCheckBox caseCB;
	private SearchInfoObject sObj;
	

	public SearchComponentDialog(JDialog owner) {
		super(owner);
		this.init();
		this.pack();
		this.setLocationRelativeTo(null);
		this.setModal(true);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton searchButton = new JButton(Resources.get("FIND"));
		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!McUtils.isStringEmpty(textTF.getText())) {
					int type = 0;
					if(byInternalNameRB.isSelected())
						type = 1;
					else if(byDisplayNameRB.isSelected())
						type = 2;
					sObj = new SearchInfoObject(textTF.getText(),componentCB.getSelectedItem().toString(),
							caseCB.isSelected(),likeRB.isSelected(),type,scopeCB.getSelectedItem().toString());
					SearchComponentDialog.this.setVisible(false);
/*					TreeNodeHandler tnh = new TreeNodeHandler();
					if(tnh.requestSearchNode(config,tree,componentCB.getSelectedItem().toString(),textTF.getText(), type, 
							caseCB.isSelected(), likeRB.isSelected()))
						SearchComponentDialog.this.setVisible(false);
					else {
						JOptionPane.showMessageDialog(null, "no object found");
					}
					*/
				}
			}			
		});

		JButton closeButton = new JButton(Resources.get("CLOSE"));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SearchComponentDialog.this.setVisible(false);	
			}			
		});
		buttonPanel.add(searchButton);
		buttonPanel.add(closeButton);
		
		JPanel inputPanel = new JPanel(new BorderLayout());
		
		JPanel rbPanel = new JPanel();
		ButtonGroup group = new ButtonGroup();
		this.byNameRB = new JRadioButton(XMLElements.NAME.toString());
		this.byNameRB.setSelected(true);
		this.byInternalNameRB = new JRadioButton(XMLElements.INTERNALNAME.toString());
		this.byDisplayNameRB = new JRadioButton(XMLElements.DISPLAYNAME.toString());
		group.add(byNameRB);
		group.add(byInternalNameRB);
		group.add(byDisplayNameRB);
		rbPanel.add(byNameRB);
		rbPanel.add(byInternalNameRB);
		rbPanel.add(byDisplayNameRB);
		
		TitledBorder titled = new TitledBorder("Search for");
		rbPanel.setBorder(titled);
		
		JPanel typePanel = new JPanel();
		JLabel typeLabel = new JLabel(XMLElements.TYPE.toString());
		typePanel.add(typeLabel);
		
		this.componentCB = new JComboBox();
		//get all node types
		this.componentCB.addItem("all");
		for(McNodeType nodeType: McNodeType.values()) {
			this.componentCB.addItem(nodeType.toString());
		}
		typePanel.add(this.componentCB);
		
		JLabel scopeLabel = new JLabel("Scope");
		this.scopeCB = new JComboBox();
		this.scopeCB.addItem(Resources.get("SCOPECURRENTCONFIG"));
		this.scopeCB.addItem(Resources.get("SCOPECURRENTSOURCE"));
		this.scopeCB.addItem(Resources.get("SCOPEENTIREPORTAL"));
		typePanel.add(scopeLabel);
		typePanel.add(this.scopeCB);
		
		JLabel textLabel = new JLabel("Text");
		JPanel textPanel = new JPanel();
		textPanel.add(textLabel);
		
		this.textTF = new JTextField(20);
		textPanel.add(this.textTF);
		
		this.caseCB = new JCheckBox("Case sensitive");
		this.caseCB.setSelected(true);
		textPanel.add(this.caseCB);
				
		JPanel operationPanel = new JPanel();
		this.equalRB = new JRadioButton("equal");
		this.likeRB = new JRadioButton("include text");
		ButtonGroup bg = new ButtonGroup();
		bg.add(equalRB);
		bg.add(likeRB);
		operationPanel.add(equalRB);
		operationPanel.add(likeRB);
		equalRB.setSelected(true);
		
		TitledBorder tb = new TitledBorder("operation");
		operationPanel.setBorder(tb);
	
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(textPanel,BorderLayout.CENTER);
		southPanel.add(operationPanel,BorderLayout.SOUTH);
		
		
		inputPanel.add(rbPanel,BorderLayout.CENTER);
		inputPanel.add(typePanel,BorderLayout.NORTH);
		inputPanel.add(southPanel,BorderLayout.SOUTH);

		
		content.add(inputPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.addWindowListener( 
			    new WindowAdapter() {
			        public void windowOpened( WindowEvent e ){
			            textTF.requestFocus();
			        }
			      }
			    ); 
		this.add(content);
		this.setTitle(Resources.get("SEARCHTITLE"));
	}
	
	public SearchInfoObject getSearchObj() {
		return this.sObj;
	}
	
}