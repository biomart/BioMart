package org.biomart.configurator.controller;

import org.biomart.common.exceptions.ListenerException;
import org.biomart.configurator.model.MartConstructorAction;

/**
 * This interface defines a listener which hears events about mart
 * construction. The events are defined as constants in this interface. The
 * listener will take these events and either build scripts for later
 * execution, or will execute them directly in order to physically construct
 * the mart.
 */
public interface MartConstructorListener {

	/**
	 * This event will occur when an action needs performing, and will be
	 * accompanied by a {@link MartConstructorAction} object describing what
	 * needs doing.
	 */
	public static final int ACTION_EVENT = 0;

	/**
	 * This event will occur when mart construction ends.
	 */
	public static final int CONSTRUCTION_ENDED = 1;

	/**
	 * This event will occur when mart construction begins.
	 */
	public static final int CONSTRUCTION_STARTED = 2;

	/**
	 * This event will occur when an individual dataset ends.
	 */
	public static final int DATASET_ENDED = 3;

	/**
	 * This event will occur when an individual dataset begins.
	 */
	public static final int DATASET_STARTED = 4;

	/**
	 * This event will occur when an individual schema partition ends.
	 */
	public static final int PARTITION_ENDED = 5;

	/**
	 * This event will occur when an individual schema partition begins.
	 */
	public static final int PARTITION_STARTED = 6;

	/**
	 * This method will be called when an event occurs.
	 * 
	 * @param event
	 *            the event that occurred. See the constants defined
	 *            elsewhere in this interface for possible events.
	 * @param data
	 *            ancilliary data, may be null.
	 * @param action
	 *            an action object that belongs to this event. Will be
	 *            <tt>null</tt> in all cases except where the event is
	 *            {@link #ACTION_EVENT}.
	 * @throws ListenerException
	 *             if anything goes wrong whilst handling the event.
	 */
	public void martConstructorEventOccurred(int event, Object data,
			MartConstructorAction action) throws ListenerException;
}
