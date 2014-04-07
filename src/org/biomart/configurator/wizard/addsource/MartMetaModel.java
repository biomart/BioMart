package org.biomart.configurator.wizard.addsource;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class MartMetaModel {
	private PropertyChangeSupport propertyChangeSupport;
	public static final String MART_ITEM_CHANGE="martItemChangeProperty";
	public static final String RESTORE_ITEMS="restoreItemsProperty";
	public static final String SET_ITEMS="setItemsProperty";
	
	public MartMetaModel() {
		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}
	
    public void addPropertyChangeListener(PropertyChangeListener p) {
        propertyChangeSupport.addPropertyChangeListener(p);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener p) {
        propertyChangeSupport.removePropertyChangeListener(p);
    }
    
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}