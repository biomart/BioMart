/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.biomart.configurator.model;

import java.util.HashMap;
import java.util.Map;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;


public abstract class TransformationUnit {
	/**
	 * A map of source schema column names to dataset column objects.
	 */
	private final Map<Column,DatasetColumn> newColumnNameMap;

	private TransformationUnit previousUnit;

	/**
	 * Constructs a new transformation unit that follows on from a given
	 * previous unit (optional, can be <tt>null</tt>).
	 * 
	 * @param previousUnit
	 *            the unit this one comes after.
	 */
	public TransformationUnit(final TransformationUnit previousUnit) {
		this.newColumnNameMap = new HashMap<Column,DatasetColumn>();
		this.previousUnit = previousUnit;
	}

	/**
	 * Does this unit apply to the given schema prefix?
	 * 
	 * @param schemaPrefix
	 *            the prefix.
	 * @return <tt>true</tt> if it does.
	 */
	public boolean appliesToPartition(final String schemaPrefix) {
		return this.previousUnit == null ? true : this.previousUnit
				.appliesToPartition(schemaPrefix);
	}

	/**
	 * Find out what unit came before this one.
	 * 
	 * @return the previous unit. May be <tt>null</tt>.
	 */
	public TransformationUnit getPreviousUnit() {
		return this.previousUnit;
	}

	/**
	 * Change the previous unit to this one.
	 * 
	 * @param previousUnit
	 *            the new previous unit. <tt>null</tt> to remove it.
	 */
	public void setPreviousUnit(final TransformationUnit previousUnit) {
		this.previousUnit = previousUnit;
	}

	/**
	 * Obtain a map of columns defined in this unit. The keys are schema
	 * columns. The values are the dataset column names used for those columns
	 * after this unit has been applied.
	 * 
	 * @return the map of columns. Potentially empty but never <tt>null</tt>.
	 */
	public Map<Column,DatasetColumn> getNewColumnNameMap() {
		return this.newColumnNameMap;
	}

	
	/**

	 * Given a schema column, work out which dataset column in the
	 * transformation so far refers to it. If the column was not adopted in this
	 * particular unit it will go back until it finds the unit that adopted it,
	 * and interrogate that and return the results.
	 * 
	 * @param column
	 *            the column to look for.
	 * @return the matching dataset column. May be <tt>null</tt> if the column
	 *         is not in this dataset table at all.
	 */
	public abstract DatasetColumn getDatasetColumnFor(final Column column);

}
