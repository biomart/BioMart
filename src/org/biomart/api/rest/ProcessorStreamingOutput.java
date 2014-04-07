package org.biomart.api.rest;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang.StringEscapeUtils;
import org.biomart.api.Portal;
import org.biomart.common.resources.Log;
import org.biomart.queryEngine.QueryController;

/**
 *
 * @author jhsu
 */
public final class ProcessorStreamingOutput implements StreamingOutput {
    private final boolean iframe;
    private final String scope;
    private final String uuid;

    private QueryController qc;

    public ProcessorStreamingOutput(String query, Portal portal,
            boolean iframe, String uuid, String scope, String[] mimes) {
        this.iframe = iframe;
        this.uuid = StringEscapeUtils.escapeJavaScript(uuid);
        this.scope =  StringEscapeUtils.escapeJavaScript(scope);

        try {
            qc = new QueryController(query, portal._registry.getFullRegistry(),
                    portal._user== null ? "" : portal._user, mimes, false);
        } catch(Exception e) {
            handleException(e);
        }
    }

    public String getContentType() {
        // Force HTML for iframe types of requests
        if (iframe) {
            return "text/html";
        }
        return qc.getContentType();
    }

    @Override
    public void write(OutputStream out) throws IOException {
        OutputStream o = out;
        try {
            if (iframe) {
                o = new IframeOutputStream(uuid, out, scope);
            }
            qc.runQuery(o);
        } catch (Exception e) {
            handleException(e);
        } finally {
            if (iframe) o.close();
        }
    }

    private void handleException(Exception e) {
        // Client closed the connection, everything's okay
        if (e instanceof EOFException) {
            Log.info("Client closed connnection");
            return;
        }

        String msg = e.getMessage();
        Log.error("Exception while executing query", e);
        throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                .entity(!"".equals(msg) ? msg : "Bad Request")
                .type(MediaType.TEXT_PLAIN)
                .build());
    }
}

