package org.biomart.common.exceptions;

/**
 * Exception provoked by an functional error in the algorithm
 * @author anthony
 *
 */
public class TechnicalException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6791650191055921474L;
	
	public TechnicalException(String message, Exception e) {
		super(message, e);
	}
	public TechnicalException(String message) {
		super(message);
	}
	public TechnicalException(Exception e) {
		super(e);
	}
}
