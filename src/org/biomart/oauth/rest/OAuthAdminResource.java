package org.biomart.oauth.rest;

import java.io.IOException;
import java.util.Set;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import net.oauth.OAuthConsumer;
import org.biomart.oauth.persist.Accessor;
import org.biomart.oauth.persist.Consumer;
import org.biomart.oauth.persist.User;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author jhsu
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class OAuthAdminResource {
    private final ObjectMapper mapper = new ObjectMapper();

    @Path("consumer")
    @GET
    public Response getConsumers() throws IOException {
        Set<Consumer> consumers = Consumer.all();
        return Response.ok(mapper.writeValueAsString(consumers)).build();
    }

    @Path("consumer/{key}")
    @GET
    public Response getConsumer(@PathParam("key") String key) throws IOException {
        Consumer c = Consumer.get(key);
        if (c == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(mapper.writeValueAsString(c)).build();
    }

    @Path("consumer/add/{key}")
    @POST
    public Response addConsumer(@PathParam("key") String key,
            @FormParam("name") String name,
            @FormParam("callback") String callbackUrl,
            @FormParam("secret") String secret,
            @FormParam("description") String description) throws IOException {
        if (Consumer.get(key) != null) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("Already exists: " + key).build();
        }
        if ("".equals(name)) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("Name cannot be blank").build();
        }
        if ("".equals(secret)) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("Secret cannot be blank").build();
        }
        OAuthConsumer oauthConsumer = new OAuthConsumer(callbackUrl, key, secret, null);
        oauthConsumer.setProperty("name", name);
        oauthConsumer.setProperty("description", description);
        Consumer c = new Consumer(oauthConsumer);
        c.save();
        SimpleOAuthProvider.loadConsumers();
        return Response.status(Status.CREATED).entity(mapper.writeValueAsString(c)).build();
    }

    @Path("consumer/delete/{key}")
    @POST
    public Response deleteConsumer(@PathParam("key") String key) throws IOException {
        Consumer c = Consumer.get(key);
        if (c == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        boolean success = c.delete();
        SimpleOAuthProvider.loadConsumers();
        return Response.ok(mapper.writeValueAsString(success)).build();
    }

    @Path("consumer/update/{key}")
    @POST
    public Response updateConsumer(@PathParam("key") String key,
            @FormParam("name") String name,
            @FormParam("description") String description) throws IOException {
        Consumer c = Consumer.get(key);
        if (c == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        c.oauthConsumer.setProperty("name", name);
        c.oauthConsumer.setProperty("description", description);
        c.save();
        SimpleOAuthProvider.loadConsumers();
        return Response.status(Status.CREATED).entity(mapper.writeValueAsString(c)).build();
    }

    @Path("user")
    @GET
    public Response getUsers() throws IOException {
        Set<User> users = User.all();
        return Response.ok(mapper.writeValueAsString(users)).build();
    }

    @Path("accessor")
    @GET
    public Response getAllTokens() throws IOException {
        Set<Accessor> accessors = Accessor.all();
        return Response.ok(mapper.writeValueAsString(accessors)).build();
    }
}
