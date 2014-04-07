package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;


public abstract class Table extends MartConfiguratorObject implements Comparable<Table>, Comparator<Table> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<String> existInPts;
	protected SortedMap<String,Column> columnList;
	protected PrimaryKey primaryKey;
	protected Set<ForeignKey> foreignKeyList;
		
	/**
	 * Sets the primary key of this table. It may be <tt>null</tt>,
	 * indicating that the table has no primary key.
	 * 
	 * @param pk
	 *            the primary key of this table.
	 */
	public void setPrimaryKey(final PrimaryKey pk) {
//		Log.debug("Changing PK on table " + this + " to " + pk);
		final PrimaryKey oldValue = this.primaryKey;
		if (this.primaryKey == pk || this.primaryKey != null
				&& this.primaryKey.equals(pk))
			return;
		this.primaryKey = pk;
		if(pk!=null)
			this.primaryKey.setParent(this);
	}

	/**
	 * Returns a reference to the primary key of this table. It may be
	 * <tt>null</tt>, indicating that the table has no primary key.
	 * 
	 * @return the primary key of this table.
	 */
	public PrimaryKey getPrimaryKey() {
		return this.primaryKey;
	}
	
	/**
	 * Returns a set of the foreign keys of this table. It may be empty,
	 * indicating that the table has no foreign keys. It will never return
	 * <tt>null</tt>.
	 * 
	 * @return the set of foreign keys for this table.
	 */
	public Set<ForeignKey> getForeignKeys() {		
		return this.foreignKeyList;
	}
	
	public void addForeignKey(ForeignKey fk) {
		if(!this.foreignKeyList.contains(fk)) {
			fk.setParent(this);
			this.foreignKeyList.add(fk);
		}			
	}

	public Collection<Column> getColumnList() {
			return this.columnList.values();
	}
	
	public List<Column> getColumnList(String datasetName) {
		List<Column> result = new ArrayList<Column>();
		for(Column column: this.columnList.values()) {
			if(((DatasetColumn)column).inPartition(datasetName))
				result.add(column);
		}
		return result;
	}
	
	public Column getColumnByName(String name) {
		return this.columnList.get(name);
	}
	
	public void addColumn(Column column) {
		if(!this.columnList.containsKey(column.getName())) {
			this.columnList.put(column.getName(),column);
			column.setParent(this);
		}
	}
	
	public void removeColumn(Column column) {
		this.columnList.remove(column.getName());
	}

	public Set<Relation> getRelations() {
		Set<Relation> result = new TreeSet<Relation>();	// Tree for order
		MartConfiguratorObject mcObj = this.getParent();
		if(mcObj instanceof Mart) {
			Mart mart = (Mart)mcObj;
			for(Relation r: mart.getRelations()) {
				if(this.getName().equals(r.getPropertyValue(XMLElements.FIRSTTABLE)) ||
						this.getName().equals(r.getPropertyValue(XMLElements.SECONDTABLE))) {
					result.add(r);
				}
			}
		} else if(mcObj instanceof SourceSchema) {
			SourceSchema ss = (SourceSchema)mcObj;
			for(Relation r: ss.getRelations()) {
				if(this.getName().equals(r.getPropertyValue(XMLElements.FIRSTTABLE)) ||
						this.getName().equals(r.getPropertyValue(XMLElements.SECONDTABLE))) {
					result.add(r);
				}
			}		
		}
		return result;
	}
	
	public Set<Key> getKeys() {
		Set<Key> keySet = new TreeSet<Key>();	// Tree for order
		if(this.getPrimaryKey()!=null)
			keySet.add(this.getPrimaryKey());
		keySet.addAll(this.getForeignKeys());
		return keySet;
	}

	public Table(String name) {
		super(name);
		this.columnList = new TreeMap<String,Column>(McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
		this.foreignKeyList = new TreeSet<ForeignKey>();	// Tree for order
		this.existInPts = new ArrayList<String>();
	}
	
	public Table(org.jdom.Element element) {
		super(element);
		this.columnList = new TreeMap<String,Column>(McUtils.BETTER_STRING_COMPARATOR);	// see DCCTEST-491
		this.foreignKeyList = new TreeSet<ForeignKey>();		// Tree for order
		this.existInPts = new ArrayList<String>();
	}

	public String getUniqueName(String baseName) {
		String tmpName = baseName;
		String result = "";
		return result;
	}

	public ForeignKey getFirstForeignKey() {
		if(this.foreignKeyList.isEmpty())
			return null;
		else
			return this.foreignKeyList.iterator().next();
	}

	/**
	 * used for the order of sub main
	 * @return
	 */
	public int getKeyColumnCount() {
		int count = 0;
		Collection<Column> columnList = this.getColumnList();
		for(Column col: columnList) {
			if(col.getName().indexOf(Resources.get("keySuffix"))>=0) {
				count++;
			}
		}
		return count;
	}

	//FIXME still?
	public ForeignKey getForeignKeyByName(String name) {
		for(ForeignKey foreignKey : this.getForeignKeys()) {
			if (foreignKey.getName().equals(name) || 
					foreignKey.getName().equals(name+Resources.get("FKPREFIX"))	// what use case? FIXME
				) {
				return foreignKey;
			}
		}
		return null;
	}

	public List<String> getRange() {
		return this.existInPts;
	}
	
	public void addInPartitions(String pt) {
		if(!this.existInPts.contains(pt)) {
			this.existInPts.add(pt);
			this.setProperty(XMLElements.INPARTITIONS, McUtils.StrListToStr(this.existInPts, ","));
		}
	}
	
	public void removeFromPartition(String pt) {
//		int oldcount = this.existInPts.size();
		if(this.existInPts.remove(pt)) 
			this.setVisibleModified(true);
/*		int newcount = this.existInPts.size();
		if(oldcount == 1 && newcount ==0) {
			//this.setUpdateFlag(UpdateType.CHANGED);
			if(this instanceof DatasetTable)
				((Mart)this.getParent()).removeDatasetTable((DatasetTable)this);
			else if(this instanceof SourceTable)
				((SourceSchema)this.getSchema()).removeTableByName(this.getName());
		}*/
	}
	
	public void renameInPartition(String oldvalue, String newvalue) {
		if(this.existInPts.remove(oldvalue)) {
			this.existInPts.add(newvalue);
			this.setProperty(XMLElements.INPARTITIONS, McUtils.StrListToStr(this.existInPts, ","));
		}
	}

	public abstract SourceSchema getSchema();
	
	public abstract boolean hasSubPartition();

	public int getUniqueId() {
		return 0;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (o == this)
			return true;
		else if (o == null)
			return false;
		else if (o instanceof Key) {
			final Table t = (Table) o;
			return this.getIdentifier().equals(t.getIdentifier());
		} else
			return false;
	}
	
	@Override
	public String toString() {
		return "(" + this.getClass().getSimpleName() + ") " + this.getName();
	}
	
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	@Override
	public int compareTo(Table object) {
		boolean equals = this.equals(object);
		if (equals) {
			return 0;
		} else {
			Integer compare = null;
			compare = this.getClass().toString().compareTo(object.getClass().toString());
			if (compare==0) {
				compare = this.getIdentifier().compareTo(object.getIdentifier());
			}
			return compare;	
		}
	}
	@Override
	public int compare(Table object1, Table object2) {
		return this.compareTo(object2);
	}
	private String getIdentifier() {
		String identifier = this.getClass().getName() + Resources.get("IDENTIFIER_SEPARATOR") + this.getName();
		return identifier.toLowerCase(); // toLowerCase: see DCCTEST-491
	}
	public String getHashIdentifier() {
		return this.getIdentifier();
	}


	@Override
	public boolean isVisibleModified() {
		if(super.isVisibleModified())
			return true;
		// Compute this from all rels and cols - if any are vis
		// modified then we are too.
		for (Relation relation: this.getRelations())
			if (relation.isVisibleModified())
				return true;
		for (Column column:  this.getColumnList())
			if (column.isVisibleModified())
				return true;
		return false;
	}
}