package org.biomart.api.rest;

import com.google.inject.Singleton;
import com.sun.jersey.api.core.PackagesResourceConfig;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author jhsu
 */
@Singleton
public class App extends PackagesResourceConfig {
    public App(Map<String, Object> props) {
        super(props);
    }

    @Override
    public Map<String, MediaType> getMediaTypeMappings() {
        /*
         * This is how the Jersey container dynamically changes the Accept HTTP
         * header based on the file extension. (e.g. marts.xml maps to application/xml)
         */
        Map<String, MediaType> m = new HashMap<String, MediaType> ();
        m.put("json", MediaType.APPLICATION_JSON_TYPE);
        m.put("jsonp", MediaType.APPLICATION_JSON_TYPE);
        m.put("xml", MediaType.APPLICATION_XML_TYPE);
        m.put("html", MediaType.TEXT_HTML_TYPE);
        m.put("txt", MediaType.TEXT_PLAIN_TYPE);
        m.put("jpg", MediaType.valueOf("image/jpeg"));
        m.put("jpeg", MediaType.valueOf("image/jpeg"));
        m.put("png", MediaType.valueOf("image/png"));
        m.put("gif", MediaType.valueOf("image/gif"));
        m.put("svg", MediaType.valueOf("image/svg+xml"));
        return m;
    }
}
