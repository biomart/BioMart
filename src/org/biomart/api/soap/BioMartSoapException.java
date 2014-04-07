package org.biomart.api.soap;

import javax.xml.ws.WebFault;
import org.biomart.api.BioMartApiException;

/**
 *
 * @author jhsu
 */
@WebFault
public class BioMartSoapException extends BioMartApiException {
  public BioMartSoapException(String message) {
    super(message);
  }

  public BioMartSoapException(String message, Throwable cause) {
    super(message, cause);
  }

  public BioMartSoapException(Throwable cause) {
    super(cause);
  }
}
