package org.biomart.dino.query;

/**
 * Created by luca on 19/05/14.
 */
public class BadQueryFormatException extends RuntimeException {

    public BadQueryFormatException(String message) {
        super(message);
    }

    public BadQueryFormatException(String message, Throwable e) {
        super(message, e);
    }
}
