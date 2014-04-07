package org.biomart.web;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import net.oauth.OAuthConsumer;
import org.apache.commons.lang.StringEscapeUtils;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.ClassPathUtils;
import org.biomart.common.utils.LinkedProperties;
import org.biomart.common.utils.Utf8ResourceBundle;
import org.biomart.oauth.persist.Consumer;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;

public class GuiceServletConfig extends GuiceServletContextListener {
    private static Injector injector = null;

    public final class Location {
        final String code;
        final String label;
        final String url;
        final boolean current;
        public String getCode() { return code; }
        public String getLabel() { return label; }
        public String getUrl() { return url; }
        public boolean getCurrent() { return current; }
        public Location(String code, String label, String url, boolean current) {
            this.code = code;
            this.label = label;
            this.url = url;
            this.current = current;
        }
    }

    private static Pattern MEDIA_REGEX = Pattern.compile("^.*\\.(css|js|png|jpg|jpeg|gif)$");

    protected static ResourceBundle _messages = null;

    protected static Map<String,List<Location>> _locations = null;
    protected static Location _currLocation = null;

    protected static String _locale = null;

    public static Map<String,String> labels = null;

    @Override
    @SuppressWarnings("unchecked")
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        super.contextInitialized(servletContextEvent);

        try {
            // Add OAuth Consumer key if not already added
            String consumerKey = System.getProperty("oauth.consumer.key");
            String consumerSecret = System.getProperty("oauth.consumer.secret");
            if (consumerKey != null && consumerSecret != null) {
                Log.info(String.format("Populating default BioMart OAuth consumer: key=%s", consumerKey));
                OAuthConsumer oauthConsumer = new OAuthConsumer("oob", consumerKey, consumerSecret, null);
                oauthConsumer.setProperty("name", "BioMart");
                oauthConsumer.setProperty("description", "The default BioMart OAuth consumer");
                Consumer c = new Consumer(oauthConsumer);
                c.save();
                SimpleOAuthProvider.loadConsumers();
            }

            // Setting up localized _messages/labels
            String dir = System.getProperty("biomart.web.dir");
            ClassPathUtils.addToClassPath(dir + "/etc/labels");

            _locale = System.getProperty("locale.name", "en");

            Locale locale = new Locale(_locale);
            _messages = Utf8ResourceBundle.getBundle("messages", locale);

            // Put locale messags into immutable labels map
            ImmutableMap.Builder labelsBuilder = new ImmutableMap.Builder<String,String>();
            for (String key : _messages.keySet()) {
                labelsBuilder.put(key, StringEscapeUtils.escapeJavaScript(_messages.getString(key)));
            }
            labels = labelsBuilder.build();

            // Loading _locations info
            String currLocation = System.getProperty("location.code");
            if (currLocation != null) {
                boolean locationValid = false;
                LinkedProperties props = new LinkedProperties();
                _locations = new TreeMap<String,List<Location>>();
                props.load(new FileInputStream(dir + "/etc/locations.properties"));
                for (Object key : props.keySet()) {
                    String keyStr = (String)key,
                            label, country, url;
                    boolean selected = false;
                    if (keyStr.endsWith(".label")) {
                        String baseKey = keyStr.split("\\.")[0];
                        label = new String(props.getProperty(keyStr).getBytes("ISO-8859-1"), "UTF-8");
                        country = props.getProperty(baseKey + ".country");
                        url = props.getProperty(baseKey + ".url");

                        if (url == null || "".equals(url)) {
                            continue;
                        }

                        if (country == null || "".equals(country)) {
                            continue;
                        }

                        country = new String(country.getBytes("ISO-8859-1"), "UTF-8");

                        selected = currLocation.equals(baseKey);

                        if (!_locations.containsKey(country)) {
                            _locations.put(country, new ArrayList<Location>());
                        }

                        List<Location> list = _locations.get(country);
                        Location loc = new Location(baseKey, label, url, selected);
                        list.add(loc);
                        _locations.put(country, list);

                        // User specified location must exist
                        if (selected) {
                            _currLocation = loc;
                            locationValid = true;
                        }
                    }
                }

                if (locationValid) {
                    Log.error("Location code \"" + currLocation + "\" is not valid");
                } else {
                    List<String> locationKeys = (List)_locations.keySet();
                    Collections.sort(locationKeys);
                }
            }

            // Set locations as empty list
            if (_locations == null) {
                _locations = Collections.EMPTY_MAP;
            }
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static Map<String,String> getLabelMap() { return labels; }

    public static boolean isMediaResource(ServletRequest request) {
        String uri = ((HttpServletRequest)request).getRequestURI();
        return MEDIA_REGEX.matcher(uri).matches();
    }

    public static MartRegistryFactory getMartRegistryFactory() {
        return injector.getInstance(MartRegistryFactory.class);
    }

    @Override
    protected Injector getInjector() {
        if (injector == null) {
            injector = Guice.createInjector(
                    Stage.PRODUCTION,
                    new WebServiceModule()
            );
        }
        return injector;
    }
}
