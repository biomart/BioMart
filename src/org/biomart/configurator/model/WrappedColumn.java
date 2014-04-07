package org.biomart.configurator.model;

import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.SourceColumn;

/**
 * A column on a dataset table that wraps an existing column but is
 * otherwise identical to a normal column. It assigns itself an alias if
 * the original name is already used in the dataset table.
 */
public class WrappedColumn extends DatasetColumn {
	private static final long serialVersionUID = 1L;

	private final SourceColumn sourceColumn;

	protected WrappedColumn(WrappedColumn column) {
		super(column.getName());	
		this.sourceColumn = column.getSourceColumn();
	}

	/**
	 * This constructor wraps an existing column. It also assigns an
	 * alias to the wrapped column if another one with the same name
	 * already exists on this table.
	 * 
	 * @param column
	 *            the column to wrap.
	 * @param colName
	 *            the name to give the wrapped column.
	 * @param dsTable
	 *            the dataset table to add the wrapped column to.
	 */
	public WrappedColumn(final SourceColumn column, final String colName,
			final DatasetTable dsTable) {
		// Call the parent which will use the alias generator for us.
		super(colName);
		// Remember the wrapped column.
		this.sourceColumn = column;
		String newname = McUtils.getUniqueColumnName(dsTable, colName);
		if(!newname.equals(colName))
			this.setName(newname);
		dsTable.addColumn(this);
	}


	@Override
	/**
	 * returns the source column, the original column
	 */
	public SourceColumn getSourceColumn() {
		return this.sourceColumn;
	}

	@Override
	public boolean isVisibleModified() {
		if(super.isVisibleModified())
			return true;
		return this.sourceColumn.isVisibleModified();
	}

}
