package org.biomart.api.rest;

import java.io.UnsupportedEncodingException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author jhsu
 */
@Path ("martservice/file")
public class FileResource {
    private static final int MAX_SIZE = 1000000;
    @Context HttpServletRequest request;

    @Path("wrap")
    @Produces("text/html")
    @POST
    public Response javascriptWrap(@QueryParam("callback") @DefaultValue("callback") String callback) throws FileUploadException {
        try {
            String rval = null;
            callback = StringEscapeUtils.escapeJavaScript(callback);
            if (ServletFileUpload.isMultipartContent(request)) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> items = upload.parseRequest(request);
                FileItem file = items.get(0);
                if (file.getSize() < MAX_SIZE) {
                    rval = StringEscapeUtils.escapeJavaScript(new String(file.get(), "UTF-8"));
                } else {
                    rval = String.format("File must be less than %s bytes", MAX_SIZE);
                }
            }
            return Response.ok(callback + "('" + rval + "');").build();
        } catch (UnsupportedEncodingException e) {
            return Response.status(Status.BAD_REQUEST).entity("File type not supported").build();
        }
    }
}
