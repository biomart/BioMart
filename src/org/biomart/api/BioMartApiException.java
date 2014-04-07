package org.biomart.api;

/**
 * Wraps any exception that happens during API calls.
 *
 * @author jhsu
 */
public class BioMartApiException extends RuntimeException {
  public BioMartApiException(String message) {
    super(message);
  }

  public BioMartApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public BioMartApiException(Throwable cause) {
    super(cause);
  }
}
