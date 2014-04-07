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

package org.biomart.configurator.view.gui.diagrams;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.model.FakeSchema;
import org.biomart.configurator.model.FakeTable;
import org.biomart.configurator.model.JoinTable;
import org.biomart.configurator.model.SelectFromTable;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.view.component.RelationComponent;
import org.biomart.configurator.view.component.TableComponent;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.ForeignKey;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.PrimaryKey;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.Table;
import org.jdom.Element;
import org.biomart.configurator.view.gui.diagrams.SchemaLayoutManager.SchemaLayoutConstraint;

/**
 * Displays a transformation step, depending on what is passed to the
 * constructor. The results is always a diagram containing only those components
 * which are involved in the current transformation.
 * <p>
 * Note how diagrams do not have contexts, in order to prevent user interaction
 * with them.
 * 
 */
public abstract class ExplainTransformationDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	private final List<TableComponent> tableComponents = new ArrayList<TableComponent>();

	private final int step;

	private final ExplainContext explainContext;

	private final Map<String,Object> shownTables;

	/**
	 * Creates an empty diagram, using the single-parameter constructor from
	 * {@link Diagram}.
	 * 
	 * @param martTab
	 *            the tabset to communicate with when (if) context menus are
	 *            selected.
	 * @param step
	 *            the step of the transformation this diagram represents.
	 * @param explainContext
	 *            the context used to provide the relation contexts, which are
	 *            the same as those that appear in the explain diagram in the
	 *            other tab to the transform view.
	 * @param shownTables
	 *            name to state map for initial table states.
	 */
	protected ExplainTransformationDiagram(
			final int step, final ExplainContext explainContext,
			final Map<String,Object> shownTables) {
		super(new SchemaLayoutManager());
		this.step = step;
		this.explainContext = explainContext;
		this.shownTables = shownTables;
		this.setDiagramContext(explainContext);

		// No listener required as diagram gets redone from
		// scratch if underlying tables change.
	}

	protected boolean isUseHideMasked() {
		return false;
	}

	/**
	 * Get which step this diagram is representing.
	 * 
	 * @return the step of the transformation.
	 */
	protected int getStep() {
		return this.step;
	}

	/**
	 * Get the state for a particular table component.
	 * 
	 * @param comp
	 *            the component.
	 * @return <tt>null</tt> for no state, an object otherwise.
	 */
	protected Object getState(final TableComponent comp) {
		return this.shownTables.get(comp.getTable().getName());
	}

	/**
	 * Find out what table components we have.
	 * 
	 * @return the list of components.
	 */
	public TableComponent[] getTableComponents() {
		final TableComponent[] comps = new TableComponent[this.tableComponents
				.size()];
		for (int i = 0; i < comps.length; i++)
			comps[i] = (TableComponent) this.tableComponents.get(i);
		return comps;
	}

	/**
	 * Add a table component to this diagram.
	 * 
	 * @param component
	 *            the component to add.
	 */
	protected void addTableComponent(final TableComponent component) {
		this.tableComponents.add(component);
	}

	/**
	 * Get the explain context which appears in the other tab, which is to be
	 * used for providing contexts for relations in this diagram.
	 * 
	 * @return the context.
	 */
	public ExplainContext getExplainContext() {
		return this.explainContext;
	}

	public void doRecalculateDiagram() {
		this.tableComponents.clear();
	}

	/**
	 * This version of the class shows a single table.
	 */
	public static class SingleTable extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private final SelectFromTable stu;

		/**
		 * Creates a diagram showing the given table.
		 * 
		 * @param martTab
		 *            the mart tab to pass menu events onto.
		 * @param stu
		 *            the transformation unit to show.
		 * @param step
		 *            the step of the transformation this diagram represents.
		 * @param explainContext
		 *            the context used to provide the relation contexts, which
		 *            are the same as those that appear in the explain diagram
		 *            in the other tab to the transform view.
		 * @param shownTables
		 *            name to state map for initial table states.
		 */
		public SingleTable(final SelectFromTable stu,
				final int step, final ExplainContext explainContext,
				final Map<String,Object> shownTables) {
			super(step, explainContext, shownTables);

			// Remember the params, and calculate the diagram.
			this.stu = stu;
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			super.doRecalculateDiagram();
			// Replicate the table in an empty schema then add the columns
			// requested.
			final FakeSchema tempSourceSchema = new FakeSchema(this.stu
					.getTable().getSchema().getName());
			final Table tempSource = new RealisedTable(
					this.stu.getTable() instanceof DatasetTable ? ((DatasetTable) this.stu
							.getTable()).getName()
							: this.stu.getTable().getName(), tempSourceSchema,
					this.stu.getTable(), this.getExplainContext());
			tempSourceSchema.getTables().add(tempSource);
			for (final Iterator<DatasetColumn> i = this.stu.getNewColumnNameMap().values()
					.iterator(); i.hasNext();) {
				final DatasetColumn col = (DatasetColumn) i.next();
				tempSource.addColumn(col);
			}
			final TableComponent tc = new TableComponent(tempSource, this);
			this.add(tc, new SchemaLayoutConstraint(0), Diagram.TABLE_LAYER);
			this.addTableComponent(tc);
			final Object tcState = this.getState(tc);
			if (tcState != null)
				tc.setState(tcState);
				
		}
	}


	/**
	 * This version of the class shows a temp table on the left and a real table
	 * on the right.
	 */
	public static class TempReal extends ExplainTransformationDiagram {
		private static final long serialVersionUID = 1;

		private final JoinTable ltu;

		private final Collection<Column> lIncludeCols;

		public TempReal(final JoinTable ltu,
				final List<Column> lIncludeCols, final int step,
				final ExplainContext explainContext, final Map shownTables) {
			super(step, explainContext, shownTables);

			// Remember the columns, and calculate the diagram.
			this.ltu = ltu;
			this.lIncludeCols = new ArrayList<Column>(lIncludeCols);
			this.recalculateDiagram();
		}

		public void doRecalculateDiagram() {
			// Removes all existing components.
			super.doRecalculateDiagram();
			// Create a temp table called TEMP with the given columns
			// and given foreign key.
			final FakeSchema tempSourceSchema = new FakeSchema(Resources
					.get("dummyTempSchemaName"));
			final FakeTable tempSource = new FakeTable(Resources
					.get("dummyTempTableName")
					+ " " + this.getStep(), tempSourceSchema);
			tempSourceSchema.getTables().add(tempSource);
			for (final Iterator<Column> i = this.lIncludeCols.iterator(); i.hasNext();) {
				final Column col = (Column) i.next();
				tempSource.addColumn(col);
			}
			Key tempSourceKey;
			if (this.ltu.getSchemaSourceKey() instanceof ForeignKey) {
				tempSourceKey = new ForeignKey(new ArrayList<Column>(this.ltu
						.getSourceDataSetColumns()));
				tempSource.addForeignKey((ForeignKey)tempSourceKey);
			} else {
				tempSourceKey = new PrimaryKey(new ArrayList<Column>(this.ltu
						.getSourceDataSetColumns()));
				tempSource.setPrimaryKey((PrimaryKey) tempSourceKey);
			}

			// Create a copy of the target table complete with target key.
			final Key realTargetKey = this.ltu.getSchemaRelation().getOtherKey(
					this.ltu.getSchemaSourceKey());
			final Table realTarget = this.ltu.getTable();
			final FakeSchema tempTargetSchema = new FakeSchema(realTarget
					.getSchema().getName());
			final Table tempTarget = new RealisedTable(realTarget.getName(),
					tempTargetSchema, realTarget, this.getExplainContext());
			tempTargetSchema.getTables().add(tempTarget);
			for (final Iterator<DatasetColumn> i = this.ltu.getNewColumnNameMap().values()
					.iterator(); i.hasNext();) {
				final DatasetColumn col = (DatasetColumn) i.next();
				tempTarget.addColumn(col);
			}
			Key tempTargetKey;
			if (realTargetKey instanceof ForeignKey) {
				tempTargetKey = new ForeignKey(realTargetKey.getColumns());
				tempTarget.getForeignKeys().add((ForeignKey)tempTargetKey);
			} else {
				tempTargetKey = new PrimaryKey(realTargetKey.getColumns());
				/*
				 * FIXME
				 */
				for(Column col: realTargetKey.getColumns()) {
					tempTarget.addColumn(col);
				}
				tempTarget.setPrimaryKey((PrimaryKey) tempTargetKey);
			}

			// Create a copy of the relation but change to be between the
			// two fake keys.
			Relation tempRelation;
			try {
				tempRelation = new RealisedRelation(tempSourceKey,
						tempTargetKey, this.ltu.getSchemaRelation()
								.getCardinality(),
						this.ltu.getSchemaRelation(), 0, this
								.getExplainContext());
				// DON'T add to keys else it causes trouble with
				// the caching system!
			} catch (final AssociationException e) {
				// Really should never happen.
				throw new BioMartError(e);
			}

			// Add source and target tables.
			final TableComponent tc1 = new TableComponent(tempSource, this);
			this.add(tc1, new SchemaLayoutConstraint(1), Diagram.TABLE_LAYER);
			this.addTableComponent(tc1);
			final Object tc1State = this.getState(tc1);
			if (tc1State != null)
				tc1.setState(tc1State);
			final TableComponent tc2 = new TableComponent(tempTarget, this);
			this.add(tc2, new SchemaLayoutConstraint(1), Diagram.TABLE_LAYER);
			this.addTableComponent(tc2);
			final Object tc2State = this.getState(tc2);
			if (tc2State != null)
				tc2.setState(tc2State);
			// Add relation.
			final RelationComponent relationComponent = new RelationComponent(
					tempRelation, this);
			this.add(relationComponent, new SchemaLayoutConstraint(0),
					Diagram.RELATION_LAYER);
					
		}
	}

	/**
	 * A realised relation is a generic relation with a specific iteration.
	 */
	public static class RealisedRelation extends Relation {
		private static final long serialVersionUID = 1L;

		private final Relation relation;

		private final int relationIteration;

		private final ExplainContext explainContext;

		
		//  Use this constant to refer to a relation that covers all iterations,
		//  not just the realised one.
		 
		public static final int NO_ITERATION = -1;

		private final PropertyChangeListener listener = new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				final PropertyChangeEvent ours = new PropertyChangeEvent(
						RealisedRelation.this, e.getPropertyName(), e
								.getOldValue(), e.getNewValue());
				ours.setPropagationId(e.getPropagationId());
			}
		};

		public RealisedRelation(final Key sourceKey, final Key targetKey,
				final Cardinality cardinality,
				final Relation relation, final int relationIteration,
				final ExplainContext explainContext)
				throws AssociationException {
			super(sourceKey, targetKey, cardinality);
			this.relation = relation;
			this.relationIteration = relationIteration;
			this.explainContext = explainContext;
		}


		public ExplainContext getExplainContext() {
			return this.explainContext;
		}


		public Relation getRelation() {
			return this.relation;
		}


		public int getRelationIteration() {
			return this.relationIteration;
		}
	}

	/**
	 * Realised tables are copies of those found in real schemas.
	 */
	public static class RealisedTable extends Table {
		private static final long serialVersionUID = 1L;

		private final Table table;

		private final ExplainContext explainContext;

		private final PropertyChangeListener listener = new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				final PropertyChangeEvent ours = new PropertyChangeEvent(
						RealisedTable.this, e.getPropertyName(), e
								.getOldValue(), e.getNewValue());
				ours.setPropagationId(e.getPropagationId());
			}
		};


		public RealisedTable(final String name, final FakeSchema schema,
				final Table table, final ExplainContext explainContext) {
			super(name);
			this.table = table;
			this.explainContext = explainContext;
		}


		public ExplainContext getExplainContext() {
			return this.explainContext;
		}


		public Table getTable() {
			return this.table;
		}


		@Override
		public SourceSchema getSchema() {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public boolean hasSubPartition() {
			// TODO Auto-generated method stub
			return false;
		}


		@Override
		public Element generateXml() throws FunctionalException {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public void synchronizedFromXML() {
			// TODO Auto-generated method stub
			
		}
	}

}
