package org.biomart.configurator.view.gui.diagrams;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class ImportableComponent extends JLabel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Color defaultBGColor = new Color(0xFF, 0xFF, 0x40); //light_yellow
	
	public ImportableComponent(String name) {
		super(name,SwingConstants.LEFT);
		this.setBackground(defaultBGColor);
		this.setOpaque(true);
	}
}