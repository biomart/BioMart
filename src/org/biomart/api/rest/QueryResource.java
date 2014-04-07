package org.biomart.api.rest;

import com.google.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.biomart.api.factory.MartRegistryFactory;

/**
 *
 * @author jhsu
 */
@Path("biomart")
public class QueryResource extends PortalResource {
    @Inject
    public QueryResource(MartRegistryFactory factory) {
        super(factory);
    }

    @Path("martservice")
    @GET
    public Response doProxyGetStreamResults(
            @QueryParam("download") @DefaultValue("false")  Boolean download,
            @QueryParam("iframe") @DefaultValue("false") Boolean iframe,
            @QueryParam("uuid") @DefaultValue("default") String uuid,
            @QueryParam("scope") @DefaultValue("biomart.streaming") String scope,
            @QueryParam("query") String query) throws Exception {

        return handleResults(query, download, iframe, uuid, scope);
    }

    @Path("martservice")
    @POST
    public Response doProxyPostStreamResults(
            @FormParam("download") @DefaultValue("false")  Boolean download,
            @FormParam("iframe") @DefaultValue("false")  Boolean iframe,
            @FormParam("uuid") @DefaultValue("default") String uuid,
            @FormParam("scope") @DefaultValue("biomart.streaming") String scope,
            @FormParam("query") String query) throws Exception {
        return handleResults(query, download, iframe, uuid, scope);
    }
}
