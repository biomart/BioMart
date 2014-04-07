package org.biomart.api.rest.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.biomart.api.BioMartApiException;

@Provider
public class BioMartApiExceptionMapper implements ExceptionMapper<BioMartApiException> {
    @Override
    public Response toResponse(BioMartApiException ex) {
        return Response.status(Status.BAD_REQUEST)
                .entity(ex.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}