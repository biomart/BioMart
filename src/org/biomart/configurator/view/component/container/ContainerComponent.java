package org.biomart.configurator.view.component.container;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.objects.SourceContainer;
import org.biomart.objects.portal.UserGroup;


public class ContainerComponent extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private SourceGroupPanel actionPanel;
	private final SourceContainer sourceContainer;
	public SourceContainer getSourceContainer() {
		return sourceContainer;
	}

	private ExpandingPanel expandingPanel;
	private GridBagConstraints gbc;
	
	public ContainerComponent(final SourceContainer sc, boolean expanded, boolean group) {
		this.sourceContainer = sc;
		this.setLayout(new GridBagLayout());
		this.expandingPanel = new ExpandingPanel();
		//default GridBayLayout
		this.expandingPanel.setLayout(new GridBagLayout());
		this.actionPanel = new SourceGroupPanel(sc,expandingPanel,expanded, group);
        gbc = new GridBagConstraints();  
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;

		this.add(actionPanel,gbc);
	    gbc.insets = new Insets(0,10,0,0);		
		this.add(expandingPanel,gbc);
		expandingPanel.setVisible(expanded);
		JButton addButton = new JButton("Add Source",McUtils.createImageIcon("images/add_group.gif"));
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UserGroup currentUser = ((McViewPortal) McViews.getInstance()
						.getView(IdwViewType.PORTAL)).getUser();
				if (ContainerComponent.this.actionPanel.addMarts(currentUser,sc.getName())) {
					((McViewSourceGroup)McViews.getInstance().getView(IdwViewType.SOURCEGROUP)).showSource(
							(McGuiUtils.INSTANCE.getRegistryObject()));
					((McViewPortal) McViews.getInstance().getView(
							IdwViewType.PORTAL)).showPortal(McGuiUtils.INSTANCE
							.getRegistryObject().getPortal());
				}
			}
			
		});
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		this.expandingPanel.add(addButton,gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
		//this.setBorder(BorderFactory.createEtchedBorder());
	}
	
	public JPanel getExpandingArea() {
		return this.expandingPanel;
	}
	
	public void addComponent(Component c) {
       // gbc.insets = new Insets(0,10,0,0);
		this.expandingPanel.add(c,gbc);
	}
	
}
