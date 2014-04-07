package org.biomart.configurator.utils;

import java.util.EventObject;

import org.biomart.configurator.utils.type.EventType;

public class McEventObject extends EventObject{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private EventType eventType;
	private Object object;
	private String contextString;
	
	public McEventObject(EventType type, Object obj) {
		super(obj);
		this.eventType = type;
		this.object = obj;
	}
	
	public EventType getEventType() {
		return this.eventType;
	}
	
	public Object getObject() {
		return this.object;
	}

	public void SetContextString(String value) {
		this.contextString = value;
	}
	
	public String getContextString() {
		return this.contextString;
	}
}