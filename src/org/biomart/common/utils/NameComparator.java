package org.biomart.common.utils;

import java.util.Comparator;

import org.biomart.objects.objects.MartConfiguratorObject;

public class NameComparator implements Comparator<MartConfiguratorObject> {

	public int compare(MartConfiguratorObject mcObject1, MartConfiguratorObject mcObject2) {
		if(mcObject1.getName()==null && mcObject2.getName() == null )
			return 0;
		else if(mcObject1.getName()==null && mcObject2.getName() != null)
			return -1;
		else if(mcObject1.getName()!=null && mcObject2.getName() == null)
			return 1;
		else
			return mcObject1.getName().compareTo(mcObject2.getName());
	}
	
}