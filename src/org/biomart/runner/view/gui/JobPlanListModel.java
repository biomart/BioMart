package org.biomart.runner.view.gui;

import java.net.Socket;
import java.util.Iterator;
import javax.swing.DefaultListModel;
import org.biomart.runner.controller.MartRunnerProtocol.Client;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobPlan;

// A model for representing lists of jobs.
public class JobPlanListModel extends DefaultListModel {
	private static final long serialVersionUID = 1L;

	private  String host;

	private  String port;

	public JobPlanListModel(final String host, final String port) {
		super();
		this.host = host;
		this.port = port;
	}

	public void updateList() throws ProtocolException {
		try {
			// Communicate and update model.
			this.removeAllElements();
			final Socket clientSocket = Client.createClientSocket(
					this.host, this.port);
			for (final Iterator<JobPlan> i = Client.listJobs(clientSocket)
					.getAllJobs().iterator(); i.hasNext();)
				this.addElement(i.next());
			clientSocket.close();
		} catch (final Throwable t) {
			throw new ProtocolException(t);
		}
	}
	
	public void updateHostAndPort(String host, String port) {
		JobPlanListModel.this.host = host;
		JobPlanListModel.this.port = port;
	}
}
