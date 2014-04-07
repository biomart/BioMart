package org.biomart.api.rest.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.biomart.common.exceptions.FunctionalException;

@Provider
public class FunctionalExceptionMapper implements ExceptionMapper<FunctionalException> {
    @Override
    public Response toResponse(FunctionalException ex) {
        return Response.status(Status.BAD_REQUEST)
                .entity(ex.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}