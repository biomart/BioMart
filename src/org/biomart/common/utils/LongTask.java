package org.biomart.common.utils;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.objects.portal.UserGroup;

public class LongTask extends SwingWorker<Void,Void> {

	private DataLinkInfo dlinkInfo;
	private UserGroup user;
	private String group;
	
	public LongTask(DataLinkInfo dlinkInfo, UserGroup user, String group) {
		this.dlinkInfo = dlinkInfo;
		this.user = user;
		this.group = group;
	}
	
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
	
}