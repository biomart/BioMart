package org.biomart.configurator.view.gui.diagrams;

import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class SchemaPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public SchemaPanel() {
		init();
	}
	
	private void init() {
		FlowLayout flo = new FlowLayout(FlowLayout.LEFT,50,10);
		this.setLayout(flo);
	}
	
	public JScrollPane getScrollPane() {
		return new JScrollPane(this);
	}
}