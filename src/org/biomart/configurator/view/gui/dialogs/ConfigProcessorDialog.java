package org.biomart.configurator.view.gui.dialogs;

import java.util.Map;

import javax.swing.JDialog;

import org.biomart.processors.ProcessorRegistry;

public class ConfigProcessorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<String,Class> processors;
	
	public ConfigProcessorDialog() {
		
	}
	
	private void init() {
		ProcessorRegistry.install();
		processors = ProcessorRegistry.getAll();
		
		
	}
}