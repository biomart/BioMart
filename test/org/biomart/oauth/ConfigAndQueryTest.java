package org.biomart.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.biomart.oauth.rest.OAuthSigner;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

/**
 *
 * @author jhsu
 */
public class ConfigAndQueryTest {
    private static final String CONSUMER_KEY = System.getProperty("test.oauth.consumer.key",
            "0e60157a903b15d9c6b96f7aaab0680b");
    private static final String CONSUMER_SECRET = System.getProperty("test.oauth.consumer.secret",
            "fa3264313cd6efe5a3a6e4fff6ced55bba4aa567");
    private static final String ACCESS_KEY = System.getProperty("test.oauth.access.token",
            "2dd9f52a047b4a4299b4a7cf5579f925");
    private static final String ACCESS_SECRET = System.getProperty("test.oauth.access.secret",
            "5a1136d78cf4f8b9e4452830d35f36d6");
    private static final String URL_PREFIX = System.getProperty("test.oauth.url", 
            "http://localhost:9000/martservice/");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_ATTRIBUTES = 3;

    public static void main(String[] args) throws Exception {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }

        System.out.println("Retrieving marts...");
        String data = makeRequest("marts", null);
        JsonNode node = MAPPER.readTree(data).get(1);
        final String martName = node.get("name").getTextValue();
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query>"+
                "<Query client=\"test\" processor=\"TSV\" limit=\"-1\" header=\"1\">");

        Thread.sleep(1000);

        System.out.println("\nRetrieving datasets...");

        data = makeRequest("datasets", new HashMap<String,String>(){{
            put("mart", martName);
        }});

        node = MAPPER.readTree(data).get(0);

        final String datasetName = node.get("name").getTextValue();
        builder.append("<Dataset name=\"").append(datasetName).append("\" >");

        Thread.sleep(1000);

        System.out.println("\nRetrieving attributes...");
        data = makeRequest("attributes", new HashMap<String,String>(){{
            put("datasets", datasetName);
        }});

        node = MAPPER.readTree(data);
        for (int i=0; i<MAX_ATTRIBUTES && i<node.size(); i++) {
            JsonNode curr = node.get(i);
            System.out.println(curr);
            if (!curr.get("isHidden").getBooleanValue()) {
                builder.append("<Attribute name=\"").append(curr.get("name").getTextValue()).append("\"/>");
            }
        }

        builder.append("</Dataset></Query>");

        Thread.sleep(1000);

        System.out.println("\nExecuting query...");
        printResults(builder.toString());
    }

    private static String makeRequest(String resource, Map<String, String> params) {
        String requestUrl = URL_PREFIX + resource + "?format=json";
        if (params != null) {
            for (Entry<String,String> entry : params.entrySet()) {
                requestUrl += "&" + entry.getKey() + "=" + entry.getValue();
            }
        }
        System.out.println(requestUrl);
        OAuthRequest request = OAuthSigner.instance().buildRequest(requestUrl,
                CONSUMER_KEY, CONSUMER_SECRET, ACCESS_KEY, ACCESS_SECRET);
        Response response = request.send();
        String body = response.getBody();
        System.out.println("Response:\n"+body);
        return body;
    }

    private static void printResults(String xml) throws IOException {
        System.out.println("XML: "+xml);
        OAuthRequest request = OAuthSigner.instance().buildRequest(
                Verb.POST,
                URL_PREFIX+"results",
                CONSUMER_KEY, CONSUMER_SECRET, ACCESS_KEY, ACCESS_SECRET);

        request.addBodyParameter("query", xml);
        request.addBodyParameter("streaming","true");
        request.addBodyParameter("iframe","false");

        BufferedReader in = new BufferedReader(new InputStreamReader(request.send().getStream()));
        String line = null;
        while((line = in.readLine()) != null) {
            System.out.println(line);
        }
    }
}
