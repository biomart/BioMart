package org.biomart.common.exceptions;

public class BioMartIOException extends RuntimeException {
	private static final long serialVersionUID = 1;

	public BioMartIOException() {
		super();
	}

	public BioMartIOException(final String msg) {
		super(msg);
	}

	public BioMartIOException(final String msg, final Throwable t) {
		super(msg, t);
	}

	public BioMartIOException(final Throwable t) {
		super(t);
	}
}
