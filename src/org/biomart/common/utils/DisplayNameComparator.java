package org.biomart.common.utils;

import java.util.Comparator;

import org.biomart.objects.objects.MartConfiguratorObject;

public class DisplayNameComparator implements Comparator<MartConfiguratorObject> {

	public int compare(MartConfiguratorObject mcObject1, MartConfiguratorObject mcObject2) {
		if(mcObject1.getDisplayName()==null && mcObject2.getDisplayName() == null )
			return 0;
		else if(mcObject1.getDisplayName()==null && mcObject2.getDisplayName() != null)
			return -1;
		else if(mcObject1.getDisplayName()!=null && mcObject2.getDisplayName() == null)
			return 1;
		else
			return mcObject1.getDisplayName().compareTo(mcObject2.getDisplayName());
	}
	
}