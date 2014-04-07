package org.biomart.configurator.view.component.container;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.LongTask;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.MessageConfig;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.gui.dialogs.DatasourceDialog;
import org.biomart.configurator.view.gui.dialogs.WarningDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViewSourceGroup;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.configurator.wizard.Wizard;
import org.biomart.configurator.wizard.WizardPanel;
import org.biomart.configurator.wizard.addsource.ASDBPanel;
import org.biomart.configurator.wizard.addsource.ASFilePanel;
import org.biomart.configurator.wizard.addsource.ASMartsPanel;
import org.biomart.configurator.wizard.addsource.ASStartPanel;
import org.biomart.configurator.wizard.addsource.ASURLPanel;
import org.biomart.configurator.wizard.addsource.AddSourceWizardObject;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.SourceContainer;
import org.biomart.objects.portal.UserGroup;


class SourceGroupPanel extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final SourceContainer sourceContainer;
	private final boolean isgrouped;
	
	SourceGroupPanel(SourceContainer sc, MouseListener ml, boolean selected, boolean isgrouped) {
		this.sourceContainer = sc;
		this.isgrouped = isgrouped;
		this.setLayout(new BorderLayout());
		ActionPanel ap = new ActionPanel(sc,ml,selected);
		this.setBackground(ActionPanel.lightblue);
		this.add(ap,BorderLayout.CENTER);
		this.add(getToolBar(),BorderLayout.EAST);
	    this.setBorder(BorderFactory.createEtchedBorder());
	}
	
	private JToolBar getToolBar() {
		JToolBar panel = new JToolBar();
		panel.setBackground(ActionPanel.lightblue);
		if(this.isgrouped) {
			JButton gpButton = new JButton("G");
			gpButton.setToolTipText("show group data source management");
			gpButton.setActionCommand("groupsourcemanagement");
			gpButton.addActionListener(this);
			panel.add(gpButton);
		}
		
		panel.setFloatable(false);
		return panel;
	}
 
	public boolean addMarts(final UserGroup user, final String group) {
        Wizard wizard = new Wizard();
        wizard.setTitle(Resources.get("ADDSOURCETITLE"));
        
        WizardPanel descriptor1 = new ASStartPanel();
        wizard.registerWizardPanel(ASStartPanel.IDENTIFIER, descriptor1);

        WizardPanel descriptor2 = new ASFilePanel();
        wizard.registerWizardPanel(ASFilePanel.IDENTIFIER, descriptor2);
        
        WizardPanel descriptor3 = new ASURLPanel();
        wizard.registerWizardPanel(ASURLPanel.IDENTIFIER, descriptor3);
        
        WizardPanel descriptor4 = new ASDBPanel();
        wizard.registerWizardPanel(ASDBPanel.IDENTIFIER, descriptor4);
         
        WizardPanel descriptor5 = new ASMartsPanel();
        descriptor5.setFinalPanel(true);
        wizard.registerWizardPanel(ASMartsPanel.IDENTIFIER, descriptor5);
        
        wizard.setCurrentPanel(ASStartPanel.IDENTIFIER,true);
        
        int ret = wizard.showModalDialog();
        if(ret == 0) {
        	AddSourceWizardObject wizardResult = (AddSourceWizardObject)wizard.getModel().getWizardResultObject();
        	final DataLinkInfo dlinkInfo = wizardResult.getDlinkInfo();
        
	
			final ProgressDialog progressMonitor = ProgressDialog.getInstance();
		
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					try {
						ObjectController oc = new ObjectController();
						oc.initMarts(dlinkInfo,user,group);
					} catch (final Throwable t) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								StackTrace.showStackTrace(t);
							}
						});
					}finally {
						ProgressDialog.getInstance().setVisible(false);
					}
	
					return null;
				}
				
				@Override
				protected void done() {
					ProgressDialog.getInstance().setVisible(false);
				}
				
			};
			
			worker.execute();
			progressMonitor.start("processing ...");
			return true;
        } else
        	return false;
	}
	@Override
	public void actionPerformed(ActionEvent event) {
		if(event.getActionCommand().equals("groupsourcemanagement")) {
			// added cols orders to a new PtModel
			ArrayList<Integer> cols = new ArrayList<Integer>();
			cols.add(PartitionUtils.DATASETNAME);
			cols.add(PartitionUtils.DISPLAYNAME);
			cols.add(PartitionUtils.CONNECTION);
			cols.add(PartitionUtils.DATABASE);
			cols.add(PartitionUtils.SCHEMA);
			cols.add(PartitionUtils.HIDE);
			cols.add(PartitionUtils.KEY);

			List<Mart> marts = McGuiUtils.INSTANCE.getRegistryObject().getMartsInGroup(this.sourceContainer.getName());
			new DatasourceDialog(marts, cols,true);
		}
		
	}
	
	
}