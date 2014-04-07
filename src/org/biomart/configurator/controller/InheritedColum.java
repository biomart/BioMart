package org.biomart.configurator.controller;

import org.biomart.common.exceptions.ValidationException;
import org.biomart.configurator.model.WrappedColumn;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.SourceColumn;

/**
 * A column on a dataset table that is inherited from a parent dataset
 * table.
 */
public  class InheritedColum extends WrappedColumn {
	private static final long serialVersionUID = 1L;
	private final WrappedColumn inheritedDsColumn;


	/**
	 * This constructor gives the column a name. The underlying relation
	 * is not required here. The name is inherited from the column too.
	 * 

	 * @param inheritedDsColumn
	 *            the column to inherit.
	 */
	public InheritedColum(final DatasetTable table, final WrappedColumn column) {
		// The super constructor will make the alias for us.
		super(column);
		// Remember the inherited column.
		this.inheritedDsColumn = column;
		table.addColumn(this);
	}
	



	/**
	 * Returns the column that has been inherited by this column.
	 * 
	 * @return the inherited column.
	 */
	public DatasetColumn getInheritedColumn() {
		return this.inheritedDsColumn;
	}

	public void setColumnRename(final String columnRename, final boolean userRequest)
		throws ValidationException {
		this.inheritedDsColumn.setColumnRename(columnRename, userRequest);
	}


	@Override
	public SourceColumn getSourceColumn() {
		DatasetColumn dsCol = this.inheritedDsColumn;
		while (dsCol instanceof InheritedColum)
			dsCol = ((InheritedColum) dsCol).getInheritedColumn();
		if (dsCol instanceof WrappedColumn)
			return ((WrappedColumn)dsCol).getSourceColumn();
		return null;
	}
}
