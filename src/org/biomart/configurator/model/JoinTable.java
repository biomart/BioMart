package org.biomart.configurator.model;

import java.util.Arrays;
import java.util.List;

import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.Table;


/**
 * This unit joins an existing dataset table to a schema table.
 */
public  class JoinTable extends SelectFromTable {
	private static final long serialVersionUID = 1L;

	private Relation schemaRelation;
	private List<DatasetColumn> sourceDataSetColumns;
	private Key schemaSourceKey;

	/**
	 * Construct a new join unit.
	 * 
	 * @param previousUnit
	 *            the unit that precedes this one.
	 * @param table
	 *            the table we are joining to.
	 * @param sourceDataSetColumns
	 *            the columns in the existing dataset table that are used to
	 *            make the join.
	 * @param schemaSourceKey
	 *            the key in the schema table that we are joining to.
	 * @param schemaRelation
	 *            the relation we are following to make the join.
	 * @param schemaRelationIteration
	 *            the number of the compound relation, if it is compound.
	 *            Use 0 if it is not.
	 */
	public JoinTable(final TransformationUnit previousUnit,
			final Table table, final Relation schemaRelation, final List<DatasetColumn> sourceDataSetColumn,
			final Key schemaSourceKey) {
		super(previousUnit, table);
		this.schemaRelation = schemaRelation;
		this.sourceDataSetColumns = sourceDataSetColumn;
		this.schemaSourceKey = schemaSourceKey;
	}

	public boolean appliesToPartition(final String schemaPrefix) {
		return super.appliesToPartition(schemaPrefix);
	}

	/**
	 * Get the schema table key this transformation joins to.
	 * 
	 * @return the key we are joining to.
	 */
	public Key getSchemaSourceKey() {
		return this.schemaSourceKey;
	}


	/**
	 * Get the schema relation used to make the join.
	 * 
	 * @return the relation.
	 */
	public Relation getSchemaRelation() {
		return this.schemaRelation;
	}

	public DatasetColumn getDatasetColumnFor(final Column column) {
		DatasetColumn candidate = (DatasetColumn) this
				.getNewColumnNameMap().get(column);
		if (candidate == null && this.getPreviousUnit() != null) {
			final Key ourKey = Arrays.asList(
					this.schemaRelation.getFirstKey().getColumns())
					.contains(column) ? this.schemaRelation.getFirstKey()
					: this.schemaRelation.getSecondKey();
			final Key parentKey = this.schemaRelation.getOtherKey(ourKey);
			final int pos = Arrays.asList(ourKey.getColumns()).indexOf(
					column);
			if (pos >= 0)
				candidate = this.getPreviousUnit().getDatasetColumnFor(
						parentKey.getColumns().get(pos));
			if (candidate == null)
				candidate = this.getPreviousUnit().getDatasetColumnFor(
						column);
		}
		return candidate;
	}

	/**
	 * Get the dataset columns this transformation starts from.
	 * 
	 * @return the columns.
	 */
	public List<DatasetColumn> getSourceDataSetColumns() {
		return this.sourceDataSetColumns;
	}

}
