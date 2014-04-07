package org.biomart.common.utils;

import java.util.Comparator;

import org.biomart.configurator.view.component.ConfigComponent;

public class ConfigComponentComparator implements Comparator<ConfigComponent> {

	@Override
	public int compare(ConfigComponent arg0,
			ConfigComponent arg1) {
		if(arg0.getX()>arg1.getX())
			return 1;
		else if(arg0.getX()==arg1.getX()) 
			return 0;
		else 
			return -1;
	}
	
}