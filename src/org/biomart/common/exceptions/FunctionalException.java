package org.biomart.common.exceptions;


/**
 * Exception provoked by an functional error in the algorithm
 * @author anthony
 *
 */
public class FunctionalException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6791650191055921474L;
	
	public FunctionalException(String message, Exception e) {
		super(message, e);
	}	
	public FunctionalException(String message) {
		super(message);
	}
	public FunctionalException(Exception e) {
		super(e);
	}
	
	public static String getErrorMessageUnhandledCaseOfElementChildrenType() {
		return "Unhandled case of element children type";
	}
}
