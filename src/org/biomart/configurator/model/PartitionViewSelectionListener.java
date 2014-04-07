package org.biomart.configurator.model;

/**
 * A listener that is called when the user changes the partition view
 * selection dropdown.
 */
public interface PartitionViewSelectionListener {

	/**
	 * This method is called when the partition view selection dropdown is
	 * changed.
	 */
	public void partitionViewSelectionChanged(String partitionStr);
}
