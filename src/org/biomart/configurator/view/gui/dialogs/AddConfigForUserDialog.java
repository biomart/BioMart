package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.biomart.common.resources.Resources;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.Portal;
import org.biomart.objects.portal.UserGroup;

public class AddConfigForUserDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<MartPointer> configList;
	private JList list;
	private UserGroup user;
	
	public AddConfigForUserDialog(UserGroup user) {
		this.user = user;
		init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		this.configList = new ArrayList<MartPointer>();
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		
		JButton okButton = new JButton(Resources.get("OK"));
		okButton.setActionCommand(Resources.get("OK"));
		
		DefaultListModel model = new DefaultListModel();
		list = new JList(model);
		list.setSize(200,200);
		JScrollPane sp = new JScrollPane(list);
		//add all configs
		Portal portal = (Portal)this.user.getParent().getParent();
		List<MartPointer> mpList = portal.getRootGuiContainer().getAllMartPointerListResursively();
		for(MartPointer mp: mpList) {
			model.addElement(mp);
		}
		okButton.addActionListener(this);
		buttonPanel.add(okButton);
		
		content.add(sp,BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(Resources.get("OK"))) {
			GuiContainer gc = null;
			if(!this.user.getMartPointers().isEmpty())
				gc = this.user.getMartPointers().get(0).getGuiContainer(); 
			//check if they are in the same guicontainer
			boolean valid = true;
			for(Object object: this.list.getSelectedValues()) {
				MartPointer mp = (MartPointer)object;
				if(gc == null)
					gc = mp.getGuiContainer();
				else {
					if(!gc.equals(mp.getGuiContainer())) {
						valid = false;
					}
				}
/*				if(!valid) {
					JOptionPane.showMessageDialog(this, "selected config should be in the same guicontainer","error",JOptionPane.ERROR_MESSAGE);
					this.configList.clear();
					return;
				}*/
				this.configList.add((MartPointer)object);
			}
			this.setVisible(false);
			this.dispose();
		}	
	}
	
	public List<MartPointer> getConfigList() {
		return this.configList;
	}
	
}