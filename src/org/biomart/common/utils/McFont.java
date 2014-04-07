package org.biomart.common.utils;

import java.awt.Font;

import javax.swing.UIManager;

public class McFont {
	private static McFont instance;
	private Font defaultFont; 
	private Font highlightFont;
	private Font boldFont; 
	
	public Font getDefaultFont() {
		return UIManager.getFont("Button.font");
	}
	
	public Font getHighLightFont() {
		return this.highlightFont;
	}
	
	public Font getBoldFont() {
		return this.boldFont;
	}

	private McFont() {
		defaultFont = UIManager.getFont("Button.font");
		highlightFont = new Font(defaultFont.getName(), Font.ITALIC | Font.BOLD, defaultFont.getSize());
		boldFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
	}
	
	public static McFont getInstance() {
		if(instance==null)
			instance = new McFont();
		return instance;
	}
}