package org.biomart.configurator.model;

import java.util.Iterator;
import org.biomart.configurator.controller.InheritedColum;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.Table;

/**
 * This type of transformation selects columns from a single table.
 */
public  class SelectFromTable extends TransformationUnit {
	private static final long serialVersionUID = 1L;

	private final Table table;

	protected SelectFromTable(final TransformationUnit previousUnit,
			final Table table) {
		super(previousUnit);
		this.table = table;
	}



	public boolean appliesToPartition(final String schemaPrefix) {
		return super.appliesToPartition(schemaPrefix);
	}



	private boolean columnMatches(final Column column, DatasetColumn dsCol) {
		if (dsCol == null)
			return false;
		while (dsCol instanceof InheritedColum)
			dsCol = ((InheritedColum) dsCol).getInheritedColumn();
		if (dsCol instanceof WrappedColumn)
			return ((WrappedColumn) dsCol).getSourceColumn()
					.equals(column);
		return false;
	}

	public DatasetColumn getDatasetColumnFor(final Column column) {
		DatasetColumn candidate = (DatasetColumn) this
				.getNewColumnNameMap().get(column);
		if (candidate == null)
			// We need to check each of our columns to see if they
			// are dataset columns, and if so, if they point to
			// the appropriate real column.
			for (final Iterator<DatasetColumn> i = this.getNewColumnNameMap().values()
					.iterator(); i.hasNext() && candidate == null;) {
				candidate = (DatasetColumn) i.next();
				if (!this.columnMatches(column, candidate))
					candidate = null;
			}
		return candidate;
	}

	public SelectFromTable(Table table) {
		super(null);	
		this.table = table;
	}
	
	/**
	 * Find out which schema table this unit selects from.
	 * 
	 * @return the schema table this unit selects from.
	 */
	public Table getTable() {
		return this.table;
	}

}
