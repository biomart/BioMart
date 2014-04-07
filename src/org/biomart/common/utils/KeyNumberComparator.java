package org.biomart.common.utils;

import java.util.Comparator;
import org.biomart.objects.objects.DatasetTable;

public class KeyNumberComparator implements Comparator<DatasetTable> {

	public int compare(DatasetTable dst1, DatasetTable dst2) {
		return dst1.getKeyColumnCount() - dst2.getKeyColumnCount();

	}
	
}