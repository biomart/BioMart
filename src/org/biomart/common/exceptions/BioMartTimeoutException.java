package org.biomart.common.exceptions;

public class BioMartTimeoutException extends RuntimeException {
	private static final long serialVersionUID = 1;

	public BioMartTimeoutException() {
		super();
	}

	public BioMartTimeoutException(final String msg) {
		super(msg);
	}

	public BioMartTimeoutException(final String msg, final Throwable t) {
		super(msg, t);
	}

	public BioMartTimeoutException(final Throwable t) {
		super(t);
	}
}
