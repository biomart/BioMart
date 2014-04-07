package org.biomart.api.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.StringEscapeUtils;
import org.biomart.api.Jsoml;
import org.biomart.api.lite.FilterData;
import org.biomart.api.lite.LiteMartConfiguratorObject;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.utils2.XmlUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdom.Document;
import org.jdom.Element;

/**
 *
 * @author jhsu
 */
public class ResponseFormatter {
    private static HashMap<String,String> _types = new HashMap<String,String>() {{
            put("json", MediaType.APPLICATION_JSON);
            put("jsonp", MediaType.APPLICATION_JSON);
            put("xml", MediaType.APPLICATION_XML);
        }};

    // Helper method for preparing responses
    public static Response prepare(String format, LiteMartConfiguratorObject obj, String callback) 
            throws FunctionalException, TechnicalException {
        callback = StringEscapeUtils.escapeJavaScript(callback);

        String type = _types.get(format);
        String content;

        if (type == null) {
            return Response.status(Status.BAD_REQUEST).entity("Format type not set").build();
        }

        ResponseBuilder builder;

        if (format.startsWith("json")) {
            content = obj.toJsonString();

            if ("jsonp".equals(format)) {
                content = callback + "(" + content + ");";
            }
        } else if ("xml".equals(format)) {
            content = obj.toXmlString();
        } else {
            content = null;
        }

        builder = Response.ok(content);

        // Change the Content-Type header accordingly
        builder.header("Content-Type", type);

        return builder.build();
    }

    public static Response prepare(String format, List<?extends LiteMartConfiguratorObject> list, String callback) 
            throws FunctionalException, TechnicalException {
        callback = StringEscapeUtils.escapeJavaScript(callback);
        String type = _types.get(format);
        String content;

        if (type == null) {
            return Response.status(Status.BAD_REQUEST).entity("Format type not set").build();
        }

        ResponseBuilder builder;

        if (format.startsWith("json")) {
            content = LiteMartConfiguratorObject.toJsonString(list);

            if ("jsonp".equals(format)) {
                content = callback + "(" + content + ");";
            }
        } else if ("xml".equals(format)) {
            content = LiteMartConfiguratorObject.toXmlString(list);
        } else {
            content = null;
        }

        builder = Response.ok(content);

        // Change the Content-Type header accordingly
        builder.header("Content-Type", type);

        return builder.build();
    }

    public static Response prepare(String format, Map<String,Object> map, String callback)
            throws FunctionalException, TechnicalException {
        callback = StringEscapeUtils.escapeJavaScript(callback);
        String type = _types.get(format);
        String content;

        if (type == null) {
            return Response.status(Status.BAD_REQUEST).entity("Format type not set").build();
        }

        ResponseBuilder builder;

        if (format.startsWith("json")) {
            content = LiteMartConfiguratorObject.mapToJsonString(map);

            if ("jsonp".equals(format)) {
                content = callback + "(" + content + ");";
            }
        } else if ("xml".equals(format)) {
            content = LiteMartConfiguratorObject.mapToXmlString(map);
        } else {
            content = null;
        }

        builder = Response.ok(content);

        // Change the Content-Type header accordingly
        builder.header("Content-Type", type);

        return builder.build();
    }

    public static Response prepareFilterData(String format, List<FilterData> data, String callback)
            throws FunctionalException, TechnicalException, IOException {
        callback = StringEscapeUtils.escapeJavaScript(callback);
        String type = _types.get(format);
        String content;

        if (type == null) {
            return Response.status(Status.BAD_REQUEST).entity("Format type not set").build();
        }

        ResponseBuilder builder;

        if (format.startsWith("json")) {
            ArrayList<Object> array = new ArrayList<Object>();
            ObjectMapper mapper = new ObjectMapper();

            for (FilterData item : data) {
                Jsoml curr = new Jsoml(false, "filterData");
                curr.setAttribute("name", item.getName());
                curr.setAttribute("displayName", item.getDisplayName());
                array.add(curr.getJsonObject());
            }
            content = mapper.writeValueAsString(array);

            if ("jsonp".equals(format)) {
                content = callback + "(" + content + ");";
            }
        } else {
            Document document = new Document();
            Element rootElement = new Element("list");
            document.setRootElement(rootElement);
            for (FilterData item : data) {
                Jsoml curr = new Jsoml(true, "filterData");
                curr.setAttribute("name", item.getName());
                curr.setAttribute("displayName", item.getDisplayName());
                rootElement.addContent(curr.getXmlElement());
            }
            content =  XmlUtils.getXmlDocumentString(document);
        }

        builder = Response.ok(content);

        // Change the Content-Type header accordingly
        builder.header("Content-Type", type);

        return builder.build();
    }
}
