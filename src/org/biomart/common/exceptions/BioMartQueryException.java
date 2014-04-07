package org.biomart.common.exceptions;

/**
 *
 * @author jhsu
 */
public class BioMartQueryException extends RuntimeException {
    public BioMartQueryException() {
        super();
    }
    public BioMartQueryException(String msg) {
        super(msg);
    }
    public BioMartQueryException(Throwable t) {
        super(t);
    }
    public BioMartQueryException(String msg, Throwable t) {
        super(msg, t);
    }
}
