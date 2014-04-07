package org.biomart.configurator.view.gui.diagrams;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.resources.Resources;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XMLPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton saveButton;
	private MartConfiguratorObject object;
	
	public XMLPanel(MartConfiguratorObject mcObj) {
		this.object = mcObj;
		init();
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		JTextArea textArea = new JTextArea();
		
		//output xml to textarea
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		try {
			String output = outputter.outputString(this.object.generateXml());
			textArea.append(output);
		} catch (FunctionalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JScrollPane sp = new JScrollPane(textArea);
		this.add(sp,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		saveButton = new JButton(Resources.get("SAVE"));
		saveButton.setEnabled(false);
		
		buttonPanel.add(saveButton);
		this.add(buttonPanel,BorderLayout.SOUTH);
	}
	

}