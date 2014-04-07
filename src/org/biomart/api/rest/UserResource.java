package org.biomart.api.rest;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.biomart.api.Portal;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.common.resources.Log;
import org.codehaus.jackson.map.ObjectMapper;
import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;
import org.openid4java.util.HttpClientFactory;
import org.openid4java.util.ProxyProperties;

/**
 *
 * @author jhsu
 */
@Path("/martservice/user")
public class UserResource {
    private MartRegistryFactory factory;
    @Context HttpServletRequest request;
    @Context UriInfo uriInfo;
    @Inject Injector injector;

    private ConsumerManager manager;

    @Inject
    public UserResource(MartRegistryFactory factory) {
        this.factory = factory;

        setProxy();

        manager = new ConsumerManager();
        manager.setAllowStateless(true);
        manager.setMaxAssocAttempts(0);
        manager.getRealmVerifier().setEnforceRpId(false);
        Integer maxNonceAge = Integer.getInteger("openid.maxnonceage", 60);
        manager.setMaxNonceAge(maxNonceAge);
    }

    private static void setProxy() {
        String proxyHost = System.getProperty("http.proxyHost");
        if(proxyHost != null && proxyHost.length() > 0) {
            String proxyPort = System.getProperty("http.proxyPort");
            int proxyPortValue = 8080;
            if(proxyPort != null && proxyPort.length() > 0) {
                try {
                    proxyPortValue = Integer.parseInt(proxyPort);
               } catch(Throwable t) {
                    // do nothing
               }
            }

            ProxyProperties proxyProps = new ProxyProperties();
            proxyProps.setProxyHostName(proxyHost);
            proxyProps.setProxyPort(proxyPortValue);
            HttpClientFactory.setProxyProperties(proxyProps);
        }
    }

    @Path("relogin")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response relogin() {
        HttpSession session = request.getSession(false);
        Object obj = null;
        if (session != null) {
            obj = session.getAttribute("openid");
        }

        if (obj == null) {
            return Response.ok("{\"success\": false}").build();
        }

        return Response.ok(
                "{\"success\": true, \"attributes\":" + (String)obj+ "}"
            ).build();
    }

    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON) 
    @GET 
    public Response logout()  throws URISyntaxException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Response.seeOther(new URI(System.getProperty("http.url"))).build();
    }

    @Path("auth")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response authenticate(@QueryParam("url") @DefaultValue("") String userSuppliedString) {
        if ("".equals(userSuppliedString)) {
            return Response.status(Status.BAD_REQUEST).entity("No URL supplied").build();
        }

        try {
            String siteUrl;
            if (request.isSecure()) {
                siteUrl =  System.getProperty("https.url");
            } else {
                siteUrl = System.getProperty("http.url");
            }

            String returnToUrl = siteUrl + "martservice/user/verify";

            StringBuilder html = new StringBuilder();

            List discoveries = manager.discover(userSuppliedString.trim());

            DiscoveryInformation discovered = manager.associate(discoveries);

            request.getSession().setAttribute("openid-disc", discovered);
            request.getSession().setAttribute("back-url", request.getHeader("Referer"));

            AuthRequest authReq = manager.authenticate(discovered, returnToUrl, siteUrl);

            FetchRequest fetch = FetchRequest.createFetchRequest();
            fetch.addAttribute("email", "http://axschema.org/contact/email", true);

            SRegRequest sregReq = SRegRequest.createFetchRequest();
            sregReq.addAttribute("email", true);

            authReq.addExtension(fetch);
            authReq.addExtension(sregReq);

            if (!discovered.isVersion2()) {
                html.append("<script type=\"text/javascript\">window.location=\"").append(authReq.getDestinationUrl(true)).append("\";</script>");

            } else {
                Map<String,String> params = authReq.getParameterMap();
                html.append("<form method=\"POST\" action=\"").append(authReq.getDestinationUrl(false)).append("\">");

                for (String key : params.keySet()) {
                    html.append("<input type=\"hidden\" name=\"").append(key).append("\" value=\"")
                            .append(params.get(key)).append("\"/>");
                }

                html.append("</form>");

                html.append("<script type=\"text/javascript\">biomart.auth.postForm.children('form').eq(0).submit();</script>");
            }

            return Response.ok(html.toString()).build();
        } catch (OpenIDException e) {
            // present error to the user
            e.printStackTrace();
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .build();
        }
    }

    @Path("verify")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response verify() throws Exception {
        try {
            String siteUrl;
            if (request.isSecure()) {
                siteUrl =  System.getProperty("https.url");
            } else {
                siteUrl = System.getProperty("http.url");
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String,Object> userData = new HashMap<String,Object>();
            ParameterList response = new ParameterList(request.getParameterMap());
            String callback = System.getProperty("http.url");

            DiscoveryInformation discovered = (DiscoveryInformation)
                    request.getSession().getAttribute("openid-disc");

            String receivingURL = siteUrl + request.getRequestURI().substring(1);
            String queryString = request.getQueryString();
            if (queryString != null && queryString.length() > 0) {
                receivingURL += "?" + request.getQueryString();
            }

            VerificationResult verification = manager.verify(
                    receivingURL.toString(),
                    response, discovered);

            Identifier verified = verification.getVerifiedId();

            if (verified != null) {
                AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();
                String identifier = verified.getIdentifier().split("#")[0];
                String email = null;
                String uid = identifier;

                userData.put("identifier", identifier);

                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
                    FetchResponse fetchResp = (FetchResponse) authSuccess
                            .getExtension(AxMessage.OPENID_NS_AX);

                    List emails = fetchResp.getAttributeValues("email");
					if (!emails.isEmpty()) {
						email = (String)emails.get(0);
						userData.put("email", email);
					}

                } else if(authSuccess.hasExtension(SRegMessage.OPENID_NS_SREG)) {
                    MessageExtension ext = authSuccess.getExtension(SRegMessage.OPENID_NS_SREG);

                    if (ext instanceof SRegResponse) {
                        SRegResponse sregResp = (SRegResponse) ext;
                        email = sregResp.getAttributeValue("email");
                        userData.put("email", email);
                    }
                }

                // Work around for Google's OpenID implementation
                if (identifier.startsWith("https://www.google.com/accounts/o8")) {
                    userData.put("identifier", email);
                    uid = email;
                }

                String groupName = new Portal(factory, uid)._registry.getGroupName();

                if ("anonymous".equals(groupName)) {
                    Log.info(String.format("Failed login attempt by %s", uid));
                    addMessage("The OpenID account you used is not registered with this website.");
                } else {
                    String value = mapper.writeValueAsString(userData);
                    request.getSession().setAttribute("openid", value);
                    request.getSession().setAttribute("openid_identifier", userData.get("identifier"));
                    request.getSession().setAttribute("userData", userData);

                    callback = System.getProperty("https.url", System.getProperty("http.url"));
                }
            } else {
                addMessage("OpenID server error: " + verification.getStatusMsg());
            }
            String cb = (String)request.getSession().getAttribute("back-url");
            if (cb != null) {
                URI uri = new URI(cb);
                if (uri.getPath().startsWith("/oauth")) {
                    callback += uri.getPath().substring(1) + "?" + uri.getQuery();
                }
            }
            return Response.seeOther(new URI(callback)).build();
        } catch (OpenIDException e) {
            addMessage("OpenID server error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            addMessage("Server error: " + e.getMessage());
        }
 
        return Response.seeOther(new URI(System.getProperty("https.url", System.getProperty("http.url")))).build();
    }

    private void addMessage(String msg) {
        try {
            Object flash = request.getSession().getAttribute("flash");
            Class<?> cls = flash.getClass();
            Method method = cls.getMethod("addMessage", new Class[]{String.class});
            method.invoke(flash, new Object[]{msg});
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }
}
