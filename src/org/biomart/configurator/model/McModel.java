package org.biomart.configurator.model;

import java.util.Observable;


public class McModel extends Observable {
	public void processUpdate(Object obj) {
		if(check()) {
			this.setChanged();
			this.notifyObservers(obj);
		}
	}
	
	private boolean check() {
		return true;
	}
}