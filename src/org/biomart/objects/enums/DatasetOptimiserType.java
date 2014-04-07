package org.biomart.objects.enums;

/**
 * This class defines the various different ways of optimising a dataset
 * after it has been constructed, eg. adding boolean columns.
 */
public enum DatasetOptimiserType {
	/**
	 * Use this constant to refer to no optimisation.
	 */
	NONE ("NONE", false, false, false),
	
	/**
	 * Parent tables will inherit copies of count columns from child tables.
	 */
	COLUMN_INHERIT ("COLUMN_INHERIT", false, false, false),
	
	/**
	 * Parent tables will inherit copies of count tables from child tables.
	 */
	TABLE_INHERIT ("TABLE_INHERIT", false, true, false),
	
	/**
	 * Parent tables will inherit copies of bool columns from child tables.
	 */
	COLUMN_BOOL_INHERIT ("COLUMN_BOOL_INHERIT", true, false, false),
	
	/**
	 * Parent tables will inherit copies of bool tables from child tables.
	 */
	TABLE_BOOL_INHERIT ("TABLE_BOOL_INHERIT", true, true, false),
	
	/**
	 * Parent tables will inherit copies of bool columns from child tables.
	 */
	COLUMN_BOOL_NULL_INHERIT ("COLUMN_BOOL_NULL_INHERIT", true, false, true),
	
	/**
	 * Parent tables will inherit copies of bool tables from child tables.
	 */
	TABLE_BOOL_NULL_INHERIT ("TABLE_BOOL_NULL_INHERIT", true, true, true);
	
	final private String name;
	final private boolean bool;
	final private boolean table;
	final private boolean useNull;
	
	/**
	 * The private constructor takes a single parameter, which defines the
	 * name this optimiser type object will display when printed.
	 * 
	 * @param name
	 *            the name of the optimiser type.
	 * @param bool
	 *            <tt>true</tt> if bool values (0,1) should be used
	 *            instead of counts.
	 * @param table
	 *            <tt>true</tt> if columns should live in their own
	 *            tables.
	 * @param useNull
	 *            if this is a bool column, use null/1 instead of 0/1.
	 */
	DatasetOptimiserType(final String name, final boolean bool,
				final boolean table, final boolean useNull) {
		this.name = name;
		this.bool = bool;
		this.table = table;
		this.useNull = useNull;
	}
	
	/**
	 * Return <tt>true</tt> if columns counts should be replaced by 0/1
	 * boolean-style values.
	 * 
	 * @return <tt>true</tt> if columns counts should be replaced by 0/1
	 *         boolean-style values.
	 */
	public boolean isBool() {
		return this.bool;
	}

	/**
	 * Return <tt>true</tt> if columns 0/1 values should be replaced by
	 * null/1 equivalents.
	 * 
	 * @return <tt>true</tt> if columns 0/1 values should be replaced by
	 *         null/1 equivalents.
	 */
	public boolean isUseNull() {
		return this.useNull;
	}

	/**
	 * Return <tt>true</tt> if columns should live in their own table.
	 * 
	 * @return <tt>true</tt> if columns should live in their own table.
	 */
	public boolean isTable() {
		return this.table;
	}
	
	/**
	 * Displays the name of this optimiser type object.
	 * 
	 * @return the name of this optimiser type object.
	 */
	public String toString() {
		return this.name;
	}
	
	public static DatasetOptimiserType valueFrom(String value) {
		for(DatasetOptimiserType dot: DatasetOptimiserType.values()) {
			if(dot.toString().equalsIgnoreCase(value))
				return dot;
		}
		return null;
	}


}