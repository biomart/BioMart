package org.biomart.configurator.view.gui.diagrams;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class ExportableComponent extends JLabel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Color defaultBGColor = Color.ORANGE;
	
	public ExportableComponent(String name) {
		super(name,SwingConstants.LEFT);
		this.setBackground(defaultBGColor);
		this.setOpaque(true);
	}
}