package org.biomart.common.exceptions.backwardscompatibility;

public class BackwardsCompatibilityException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5372793529820012709L;

	public BackwardsCompatibilityException(){
		super();
	}
	
	public BackwardsCompatibilityException(final String msg){
		super(msg);
	}
	
	public BackwardsCompatibilityException(final String msg, Throwable t){
		super(msg, t);
	}
	
	public BackwardsCompatibilityException(Throwable t){
		super(t);
	}
}
