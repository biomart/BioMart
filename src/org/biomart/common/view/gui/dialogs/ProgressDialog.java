
package org.biomart.common.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.biomart.common.resources.Resources;
import org.biomart.common.utils.McEventBus;
import org.biomart.configurator.utils.type.McEventProperty;

/**
 * A dialog which shows progress. Similar to ProgressMonitor in the Swing API,
 * but this one actually works.
 * 
 */
public class ProgressDialog extends JDialog implements PropertyChangeListener, Runnable{
	private static final long serialVersionUID = 1;

	private JProgressBar progress;
	private JLabel statusLabel; 
	private boolean canceled = false;
	private static ProgressDialog instance;
	private String statusMessage = "processing ...";
	private static Window owner;
	
	public static ProgressDialog getInstance() {
		if(instance==null)
			instance = new ProgressDialog();
		return instance;
	}
	
	public static ProgressDialog getInstance(Dialog owner) {
		if(instance!=null) {
			instance.setVisible(false);
			instance.dispose();
			instance = null;
			instance = new ProgressDialog(owner);
		} else
			instance = new ProgressDialog(owner);
		return instance;		
	}
	
	public static ProgressDialog getInstance(Window owner) {
		if(instance!=null) {
			instance.setVisible(false);
			instance.dispose();
			instance = null;
			instance = new ProgressDialog(owner);
		} else
			instance = new ProgressDialog(owner);
		return instance;		
	}
	
	public static ProgressDialog getInstance(Frame owner) {
		if(instance!=null) {
			instance.setVisible(false);
			instance.dispose();
			instance = null;
			instance = new ProgressDialog(owner);
		} else
			instance = new ProgressDialog(owner);
		return instance;		
	}
	
	private void init() {
		this.setModal(true);
		this.setUndecorated(true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.addWindowListener(new WindowAdapter() {
            public void windowDeactivated(WindowEvent e)
            {
            	ProgressDialog.this.statusMessage = Resources.get("DEFAULTPRECESSMESSAGE");
            	firePropertyChange("test", null, "test");
            }
          });

		// Create the content pane for the dialog, ie. the bit that will hold
		// all the various questions and answers.
		final JPanel content = new JPanel(new BorderLayout());
		this.setContentPane(content);

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
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}
	
	private ProgressDialog() {
		init();
	}
	
	private ProgressDialog(Dialog owner) {
		super(owner);
		init();
	}
	
	private ProgressDialog(Frame owner) {
		super(owner);
		init();
	}
	
	private ProgressDialog(Window owner) {
		super(owner);
		init();
	}
	 
	/**
	 * Create a new progress dialog.
	 * 
	 * @param parent
	 *            the parent dialog to centre over.
	 * @param min
	 *            the minimum value of the progress bar.
	 * @param max
	 *            the maximum value of the progress bar.
	 * @param showCancel
	 *            <tt>true</tt> if a cancel button should be shown.
	 */
	private ProgressDialog(JDialog parent) {
	
		// Create the base dialog.
		super(parent);
		//this.setModal(true);
		this.setUndecorated(true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.addWindowListener(new WindowAdapter() {
            public void windowDeactivated(WindowEvent e)
            {
            	ProgressDialog.this.statusMessage = Resources.get("DEFAULTPRECESSMESSAGE");
            	firePropertyChange("test", null, "test");
            }
          });

		// Create the content pane for the dialog, ie. the bit that will hold
		// all the various questions and answers.
		final JPanel content = new JPanel(new BorderLayout());
		this.setContentPane(content);

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
		this.pack();

		// Move ourselves.
		this.setLocationRelativeTo(null);
	}

	/**
	 * Is the box canceled by the user?
	 * 
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isCanceled() {
		return this.canceled;
	}

	/**
	 * Update the progress.
	 * 
	 * @param progress
	 *            the new progress value.
	 */
	public void setProgress(final int progress) {
		if (this.progress.isIndeterminate())
			this.progress.setIndeterminate(false);
		this.progress.setValue(progress);
	}
	
	public void setStatus(final String statusMessage) {
		this.statusMessage = statusMessage;
		firePropertyChange(McEventProperty.PROGRESS_STATAUS_CHANGE.toString(), null, McEventProperty.PROGRESS_STATAUS_CHANGE.toString());
	}
	
	public void start(String statusMessage) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {				
				ProgressDialog.this.statusLabel.setText(ProgressDialog.this.statusMessage);
				ProgressDialog.this.pack();	
				ProgressDialog.this.setVisible(true);	
			}
			
		}) ;
		thread.run();
			
	}
	
	public void stop() {
		if(instance!=null) {
			this.setVisible(false);
			this.dispose();
			instance = null;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		if(evt.getPropertyName().equals(McEventProperty.PROGRESS_STATAUS_CHANGE.toString())) {
			//this.setStatus(value);
			ProgressDialog.this.statusLabel.setText(ProgressDialog.this.statusMessage);
			ProgressDialog.this.pack();
		}
	}

	@Override
	public void run() {
		ProgressDialog.this.statusLabel.setText(ProgressDialog.this.statusMessage);
		ProgressDialog.this.pack();	
		ProgressDialog.this.setVisible(true);	

		
	}
	
}
