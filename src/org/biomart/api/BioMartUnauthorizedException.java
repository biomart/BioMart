package org.biomart.api;

/**
 * For unauthorized access
 *
 * @author jhsu
 */
public class BioMartUnauthorizedException extends Exception{
  public BioMartUnauthorizedException(String message) {
    super(message);
  }

  public BioMartUnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  public BioMartUnauthorizedException(Throwable cause) {
    super(cause);
  }

}
