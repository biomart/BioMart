package org.biomart.common.exceptions;


/**
 * Exception provoked by an functional error in the algorithm
 * @author anthony
 *
 */
public class WarningException extends Exception {

	private static final long serialVersionUID = -8735971590323783190L;

	public WarningException(String message) {
		super(message);
	}
	public WarningException(Exception e) {
		super(e);
	}
}
