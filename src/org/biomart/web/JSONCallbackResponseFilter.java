package org.biomart.web;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public final class JSONCallbackResponseFilter implements ContainerResponseFilter {

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        if (response.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
            MultivaluedMap<String, String> queryParams = request.getQueryParameters();
            String callback = queryParams.getFirst("callback");
            if (callback != null) {
                response.setContainerResponseWriter(new JSONCallbackResponseAdapter(
                        response.getContainerResponseWriter(), callback));
            }
        }
        return response;
    }

    private class JSONCallbackResponseAdapter implements ContainerResponseWriter {
        private final ContainerResponseWriter crw;

        private OutputStream out;
        private String callback;

        JSONCallbackResponseAdapter(ContainerResponseWriter crw,
String callback) {
            this.crw = crw;
            this.callback = callback;
        }

        @Override
        public OutputStream writeStatusAndHeaders(long contentLength,
                ContainerResponse response) throws IOException {
           out = crw.writeStatusAndHeaders(-1, response);

           out.write((this.callback + "(").getBytes());
           return out;
        }

        @Override
        public void finish() throws IOException {
            out.write(")".getBytes());
        }
    }
}
