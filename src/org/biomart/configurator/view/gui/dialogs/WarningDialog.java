package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.biomart.configurator.utils.MessageConfig;
import org.biomart.configurator.utils.type.MessageType;


public class WarningDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JCheckBox cb;
	private String messageType;
	
	public WarningDialog(String messageType) {
		this.messageType = messageType;
	}
	
	
	public void showDialogFor(String messageType, String message) {
		//default is 1
		this.messageType = messageType;
		MessageConfig.getInstance().put(messageType, MessageType.NO);
		this.setLayout(new BorderLayout());
		JPanel centralPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		JLabel messageLabel = new JLabel(message);
		JButton okButton = new JButton("YES");
		okButton.setActionCommand("yes");
		okButton.addActionListener(this);
		JButton cancelButton = new JButton("NO");
		cancelButton.setActionCommand("no");
		cancelButton.addActionListener(this);
		
		cb = new JCheckBox("don't warn me anymore");
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(cb);
		
		centralPanel.add(messageLabel);
		
		this.add(centralPanel,BorderLayout.CENTER);
		this.add(buttonPanel,BorderLayout.SOUTH);
		this.setLocationRelativeTo(null);
		this.pack();
		this.setModal(true);
		this.setVisible(true);
	}
	
	public MessageType getResult() {
		return MessageConfig.getInstance().get(messageType);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("yes")) {
			if(cb.isSelected())
				MessageConfig.getInstance().put(this.messageType, MessageType.YESFORALL);
			else
				MessageConfig.getInstance().put(this.messageType, MessageType.YES);
		}else if(e.getActionCommand().equals("no")) {
			if(cb.isSelected())
				MessageConfig.getInstance().put(this.messageType, MessageType.NOFORALL);
			else
				MessageConfig.getInstance().put(this.messageType, MessageType.NO);			
		}	
		this.setVisible(false);
		this.dispose();
	}
}