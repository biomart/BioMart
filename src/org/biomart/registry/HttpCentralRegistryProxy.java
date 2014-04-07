package org.biomart.registry;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu
 */
public class HttpCentralRegistryProxy {
	private static HttpCentralRegistryProxy instance =  null;
	
    private HttpCentralRegistryProxy() {
    }
    
    public static HttpCentralRegistryProxy getInstance(){
    	if(instance == null)
    		instance = new HttpCentralRegistryProxy();
    	return instance;
    }


    public boolean userExists(String openid, String groupName, String location) {
        try {
            String urlStr = String.format(location + "rest/verify_user?id=%s&group=%s", openid, groupName);
            URL url = new URL(urlStr);
            InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
            String value = CharStreams.toString(isr);
            return Boolean.parseBoolean(value);
        } catch(IOException e) {
            Log.error("Request to Central Registry threw an IOException: " + e.getMessage());
            return false;
        }
    }
}
