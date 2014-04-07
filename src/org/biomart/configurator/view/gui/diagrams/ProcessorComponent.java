package org.biomart.configurator.view.gui.diagrams;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class ProcessorComponent extends JLabel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Color defaultBGColor = Color.GREEN; //light_yellow
	
	public ProcessorComponent(String name) {
		super(name,SwingConstants.LEFT);
		this.setBackground(defaultBGColor);
		this.setOpaque(true);
	}
}