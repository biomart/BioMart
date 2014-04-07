package org.biomart.common.utils;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.configurator.utils.type.McEventProperty;

public class ProgressThread  extends JDialog implements Runnable, PropertyChangeListener {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JProgressBar progress;
	private JLabel statusLabel; 
	private boolean canceled = false;
	private String statusMessage = "processing ...";
	private static Window owner;

	private void init() {
		setModal(true);
		setUndecorated(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
            public void windowDeactivated(WindowEvent e)
            {
            	ProgressThread.this.statusMessage = Resources.get("DEFAULTPRECESSMESSAGE");
            	firePropertyChange("test", null, "test");
            }
          });

		// Create the content pane for the dialog, ie. the bit that will hold
		// all the various questions and answers.
		final JPanel content = new JPanel(new BorderLayout());
		setContentPane(content);

		statusLabel = new JLabel("progressing...");
		content.add(this.statusLabel, BorderLayout.NORTH);
		// Create some constraints for fields on the last row of the dialog.

		// Progress bar.
		this.progress = new JProgressBar();
		this.progress.setOrientation(JProgressBar.HORIZONTAL);
		this.progress.setBorderPainted(true);
		this.progress.setString(null);
		this.progress.setIndeterminate(true);
		content.add(this.progress, BorderLayout.CENTER);

		// Set size of window.
		pack();

		// Move ourselves.
		setLocationRelativeTo(null);
		this.setVisible(true);
	}

	public void setStatus(final String statusMessage) {
		this.statusMessage = statusMessage;
		firePropertyChange(McEventProperty.PROGRESS_STATAUS_CHANGE.toString(), null, McEventProperty.PROGRESS_STATAUS_CHANGE.toString());
	}
	
	@Override
	public void run() {
		init();
	}
	
	public void finish() {
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName().equals(McEventProperty.PROGRESS_STATAUS_CHANGE.toString())) {
			//this.setStatus(value);
			this.statusLabel.setText(this.statusMessage);
			pack();
		}		
	}
	
}