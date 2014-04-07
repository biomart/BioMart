package org.biomart.configurator.view.gui.diagrams;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class DsTableComponent extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String dsTableName;
	private Color defaultBGColor = Color.YELLOW;
	private String title;
	
	public DsTableComponent(String name, String title) {
		this.name = name;
		this.title = title;
		init();
	}
	
	private void init() {
		this.setBackground(defaultBGColor);
		this.setOpaque(true);
		this.setBorder(new TitledBorder(new EtchedBorder(),title));
		JLabel nameLabel = new JLabel(this.name);
		this.add(nameLabel);
	}
}