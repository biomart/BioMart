package org.biomart.configurator.view;

import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;

public class AddSourceButton extends JButton {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public AddSourceButton(String text, Icon icon) {
		super(text,icon);
	}
	
	public AddSourceButton(String text) {
		super(text);
	}
	
	@Override 
	public void processMouseEvent(final MouseEvent evt) {
		System.out.println("t2");
		super.processMouseEvent(evt);
	}
	
}