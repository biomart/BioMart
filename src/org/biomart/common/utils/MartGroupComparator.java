package org.biomart.common.utils;

import java.util.Comparator;

public class MartGroupComparator implements Comparator<String> {

	@Override
	public int compare(String s0, String s1) {
		if(XMLElements.DEFAULT.toString().equals(s0) && 
				XMLElements.DEFAULT.toString().equals(s1))
			return 0;
		else if(XMLElements.DEFAULT.toString().equals(s0))
			return 1;
		else if(XMLElements.DEFAULT.toString().equals(s1))
			return -1;
		else
			return s0.compareTo(s1);
	}
	
}