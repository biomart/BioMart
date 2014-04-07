package org.biomart.api.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author jhsu
 */
@Path("/viewer")
public class ViewerResource {
    @Context HttpServletRequest request;
    @Context UriInfo uriInfo;

    @Path("hello")
    @GET 
    public Response hello() {
        return Response.ok("Hello World").build();
    }
}
