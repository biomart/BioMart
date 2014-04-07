package org.biomart.configurator.wizard;

import javax.swing.JPanel;

public abstract class WizardPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Wizard wizard;
	private boolean finalPanel;

	public abstract void saveWizardState();
	
	public abstract boolean validateWizard();

	public void setWizard(Wizard wizard) {
		this.wizard = wizard;
	}

	public Wizard getWizard() {
		return wizard;
	}
	
	public abstract Object getNextPanelId();
	
	public abstract Object getBackPanelId();
	
    /**
     * Override this method to provide functionality that will be performed just before
     * the panel is to be displayed.
     */    
    public abstract void aboutToDisplayPanel(boolean next);
    
    public abstract void aboutToHidePanel(boolean next);
    
    public abstract void backClear();


	public void setFinalPanel(boolean finalPanel) {
		this.finalPanel = finalPanel;
	}

	public boolean isFinalPanel() {
		return finalPanel;
	}
 
	public Object getWizardStateObject() {
		return this.getWizard().getModel().getWizardResultObject();
	}
}