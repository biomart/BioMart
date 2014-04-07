package org.biomart.configurator.controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;



/**
 * Defines an split optimiser column for a table.
 */
public class SplitOptimiserColumnDef {
	private static final long serialVersionUID = 1L;

	private String separator;

	private String contentCol;

	private boolean prefix = true;

	private boolean suffix = true;

	private int size = 255;




	/**
	 * This constructor makes a new split opt definition.
	 * 
	 * @param colKey
	 *            the name of the column to get values from.
	 * @param separator
	 *            the separator to put between values.
	 */
	public SplitOptimiserColumnDef(final String colKey, String separator) {
		// Test for good arguments.
		if (separator == null)
			separator = "";

		// Remember the settings.
		this.contentCol = colKey;
		this.separator = separator;
	}

	/**
	 * Construct an exact replica.
	 * 
	 * @return the replica.
	 */
	public SplitOptimiserColumnDef replicate() {
		return new SplitOptimiserColumnDef(this.contentCol, this.separator);
	}
	/**
	 * Returns the separator.
	 * 
	 * @return the separator.
	 */
	public String getSeparator() {
		return this.separator;
	}

	/**
	 * Get the name of the value column.
	 * 
	 * @return the name.
	 */
	public String getContentCol() {
		return this.contentCol;
	}

	/**
	 * @return the prefix
	 */
	public boolean isPrefix() {
		return this.prefix;
	}

	/**
	 * @param prefix
	 *            the prefix to set
	 */
	public void setPrefix(boolean prefix) {
		if (prefix == this.prefix)
			return;
		final boolean oldValue = this.prefix;
		this.prefix = prefix;

	}

	/**
	 * @return the suffix
	 */
	public boolean isSuffix() {
		return this.suffix;
	}

	/**
	 * @param suffix
	 *            the suffix to set
	 */
	public void setSuffix(boolean suffix) {
		if (suffix == this.suffix)
			return;
		final boolean oldValue = this.suffix;
		this.suffix = suffix;

	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(int size) {
		if (size == this.size)
			return;
		final int oldValue = this.size;
		this.size = size;

	}
}
