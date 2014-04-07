package org.biomart.common.utils;

import java.util.Comparator;

import org.biomart.objects.objects.Relation;

/**
 * Rendered obsolete, see Relation.compare() instead
 */
@Deprecated
public class RelationComparator implements Comparator<Relation> {

	public int compare(Relation mcObject1, Relation mcObject2) {
		return mcObject1.toString().compareTo(mcObject2.toString());
	}
	
}