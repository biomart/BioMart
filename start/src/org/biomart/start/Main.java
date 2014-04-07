package org.biomart.start;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.biomart.common.constants.Constants;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.servlet.AbstractSessionIdManager;
import org.mortbay.jetty.servlet.JDBCSessionIdManager;
import org.mortbay.jetty.servlet.JDBCSessionManager;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.jetty.servlet.VoldemortSessionIdManager;
import org.mortbay.jetty.servlet.VoldemortSessionManager;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.ResourceCollection;
import org.mortbay.setuid.SetUIDServer;
import org.mortbay.thread.QueuedThreadPool;
import org.mortbay.util.RolloverFileOutputStream;

/**
 * 
 * @author jhsu
 */
public class Main {

	private Server server;

	private static Main serviceInstance = new Main();

    private String _homeDir = null;
    private String _webLocation = null;

	/**
	 * Static method called by prunsrv to start/stop the service. Pass the
	 * argument "start" to start the service, and pass "stop" to stop the
	 * service.
	 */
	public static void windowsService(String args[]) {
		String cmd = "start";
		if (args.length > 0) {
			cmd = args[0];
		}
		System.out.println("service get called");

		if ("start".equals(cmd)) {			
			try {
				System.out.println("service get started within java");
				serviceInstance.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("service get stoped within java");
			serviceInstance.stop();
		}
	}

	/**
	 * Start this service instance
	 */
	public void start() throws Exception {
		AppConfig.loadProperties();

        List<Handler> contexts = new ArrayList<Handler>();

		String host = System.getProperty("http.host", "0.0.0.0");
		String port = System.getProperty("http.port", "9000");
		String sslHost = System.getProperty("https.host", host);
		String sslPort = System.getProperty("https.port");
		_homeDir = System.getProperty("biomart.dir", ".");

        File baseDir = new File(_homeDir);
        System.setProperty("biomart.basedir", baseDir.getCanonicalPath());

        // Set HTTP public facing URL
        if (System.getProperty("http.url") == null) {
            System.setProperty("http.url", String.format("http://%s:%s/", host, port));
        } else {
            // Force trailing backslash
            String url = System.getProperty("http.url");
            if (!url.endsWith("/")) {
                System.setProperty("http.url", url + "/");
            }
        }

		// Figure out martapps folder
		File webDir = new File(_homeDir + "/web");
		_webLocation = webDir.getCanonicalPath();
        System.setProperty("biomart.web.dir", _webLocation);
		File martappsDir = new File(webDir, "webapps/martapps");
		String martappsLocation = martappsDir.getCanonicalPath();

        String username = System.getProperty("username");
        if (username != null) {
            String libPath = _homeDir + "/lib/extras/libsetuid.so";
	        System.setProperty("jetty.libsetuid.path", libPath);
            server = new SetUIDServer();
            ((SetUIDServer)server).setUsername(username);
        } else {
            server = new Server();
        }

				WebAppContext martappsCxt = new WebAppContext();
        martappsCxt.setContextPath("/");
				HandlerCollection handlers = new HandlerCollection();
        List<String> appLocations = new ArrayList<String>();
        appLocations.add(martappsLocation);
        martappsCxt.setDefaultsDescriptor(_webLocation + "/etc/webdefault.xml");
        martappsCxt.setParentLoaderPriority(true);

        String includesPath = System.getProperty("site.includes", _webLocation + "/includes");
				WebAppContext confCxt = new WebAppContext(includesPath, "/includes");
        confCxt.setDefaultsDescriptor(_webLocation + "/etc/webdefault.xml");
        setupWebAppContext(confCxt);

        String pagesPath = System.getProperty("site.pages", _webLocation + "/pages");
				WebAppContext pagesCxt = new WebAppContext(pagesPath, "/pages");
        pagesCxt.setDefaultsDescriptor(_webLocation + "/etc/webdefault.xml");
        setupWebAppContext(pagesCxt);
        pagesCxt.addFilter("org.biomart.web.LocationsFilter", "*", org.mortbay.jetty.Handler.DEFAULT);

        installPlugins(appLocations);

        String[] locs = new String[appLocations.size()];
        ResourceCollection resources = new ResourceCollection(appLocations.toArray(locs));
        martappsCxt.setBaseResource(resources);
				martappsCxt.getServletContext().getContextHandler() .setMaxFormContentSize(1000000);

        contexts.add(pagesCxt);
        contexts.add(confCxt);
        contexts.add(martappsCxt);

        Handler[] contextsArray = new Handler[contexts.size()];
        contexts.toArray(contextsArray);
        handlers.setHandlers(contextsArray);

        setupThreadPool();
        setupConnection(host, port);
        setupSslConnection(sslHost, sslPort);
        setupAuthorization();
        setupSessions(martappsCxt);
        setupLogging(handlers);
        setupJawrBundling();

		// Config server
		server.setHandler(handlers);
		server.setGracefulShutdown(1000);
		server.setSendDateHeader(true);
		server.setSendServerVersion(false);

		// Start server
		System.out.println("Starting server");
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Error on server startup: " + e.getMessage());
            stop();
            return;
        }

        if (martappsCxt.getSessionHandler().getSessionManager().isFailed()) {
            System.err.println("Session manager failed to start");
            stop();
            return;
        }

        // Check if we have unavailable exceptions
        Throwable t =  martappsCxt.getUnavailableException();
        if (t != null) {
            System.err.println("Error on server startup: " + t.getMessage());
            t.printStackTrace();
            stop();
            return;
        }

		String url = System.getProperty("http.url");
		System.out.println("Server started at : " + url);

		if (java.awt.Desktop.isDesktopSupported()) {
			java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
			if (desktop.isSupported(java.awt.Desktop.Action.BROWSE))
				desktop.browse(new URI(url));
		}
	}

	/**
	 * Stop this service instance
	 */
	public void stop() {		
		try {
			System.out.println("Stopping server");
			server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.start();
	}

    private void setupWebAppContext(WebAppContext cxt) {
        cxt.setDefaultsDescriptor(_webLocation + "/etc/webdefault.xml");
    }

    private void setupThreadPool() {
        final Integer minThreads = Integer.getInteger("threadpool.minThreads", 10);
        final Integer maxThreads = Integer.getInteger("threadpool.maxThreads", 25);
        final Integer lowThreads = Integer.getInteger("threadpool.lowThreads", 5);
        final Integer spawnOrShrinkAt = Integer.getInteger("threadpool.spawnOrShrinkAt", 2);
		QueuedThreadPool pool = new QueuedThreadPool() {{
            setMinThreads(minThreads);
            setMaxThreads(maxThreads);
            setLowThreads(lowThreads);
            setSpawnOrShrinkAt(spawnOrShrinkAt);
        }};
		server.setThreadPool(pool);
    }

    private void setupConnection(String host, String port) throws GeneralSecurityException, KeyException {
        final Integer headerBufferSize = Integer.getInteger("connector.headerBufferSize", 8388608);
        final Integer reqBufferSize = Integer.getInteger("connector.requestBufferSize", 8388608);
        final Integer maxIdleTime = Integer.getInteger("connector.maxIdleTime", 0);
        final Integer lowResourcesConnections = Integer.getInteger("connector.lowResourcesConnections", 1000);
        final Integer lowResourceMaxidleTime = Integer.getInteger("connector.lowResourceMaxIdleTime", 0);
        if (port != null) {
            SelectChannelConnector connector = new SelectChannelConnector();
            connector.setHost(host);
            connector.setPort(Integer.parseInt(port));
            connector.setHeaderBufferSize(headerBufferSize);
			connector.setRequestBufferSize(reqBufferSize);
            connector.setMaxIdleTime(maxIdleTime);
            connector.setStatsOn(false);
            connector.setLowResourcesConnections(lowResourcesConnections);
            connector.setLowResourceMaxIdleTime(lowResourceMaxidleTime);
            server.addConnector(connector);
        }
    }


    private void setupSslConnection(String sslHost, String sslPort) throws GeneralSecurityException, KeyException {
        final Integer headerBufferSize = Integer.getInteger("connector.headerBufferSize", 2097152);
        final Integer reqBufferSize = Integer.getInteger("connector.requestBufferSize", 8388608);
        final Integer maxIdleTime = Integer.getInteger("connector.maxIdleTime", 0);
        final Integer lowResourceMaxidleTime = Integer.getInteger("connector.lowResourceMaxIdleTime", 0);
        if (sslPort != null) {
            SslSocketConnector sslConnector = new SslSocketConnector();
            String keystore = System.getProperty("ssl.keystore");
            String password = System.getProperty("ssl.password");
            String truststore = System.getProperty("ssl.truststore");
            String trustpassword = System.getProperty("ssl.trustpassword");

            if (!keystore.startsWith("/")) {
                keystore = _webLocation + "/" + keystore;
            }

            sslConnector.setHost(sslHost);
            sslConnector.setPort(Integer.parseInt(sslPort));
            sslConnector.setHeaderBufferSize(headerBufferSize);
			sslConnector.setRequestBufferSize(reqBufferSize);
            sslConnector.setMaxIdleTime(maxIdleTime);
            sslConnector.setStatsOn(false);
            sslConnector.setLowResourceMaxIdleTime(lowResourceMaxidleTime);

            sslConnector.setKeystore(keystore);
            sslConnector.setKeyPassword(password);

            if (truststore != null) {
                if (!truststore.startsWith("/")) {
                    truststore = _webLocation + "/" + truststore;
                }
                sslConnector.setTruststore(truststore);
                sslConnector.setTrustPassword(trustpassword);
            }

            // Set HTTP public facing URL
            if (System.getProperty("https.url") == null) {
                System.setProperty("https.url", String.format("https://%s:%s/", sslHost, sslPort));
            } else {
                // Force trailing backslash
                String url = System.getProperty("https.url");
                if (!url.endsWith("/")) {
                    System.setProperty("https.url", url + "/");
                }
            }

            server.addConnector(sslConnector);
        }

        if (Boolean.parseBoolean(System.getProperty("x509.trustall"))) {
            System.out.println("Installing all-trusting x509 mananger");
            installX509TrustAllManager();
        }


    }

    private void installX509TrustAllManager() throws KeyException, GeneralSecurityException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                    // noop
                }
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                    // noop
                }
            }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private void setupAuthorization() throws IOException {
		// Auth
		HashUserRealm realm = new HashUserRealm();
		realm.setName("Auth");
		realm.setConfig(_webLocation + "/etc/realm.properties");
		realm.setRefreshInterval(0);
		server.setUserRealms(new UserRealm[] { realm });
    }

    private void setupSessions(WebAppContext context) {
        String sessionConnection = System.getProperty("session.connection");
        if (sessionConnection != null) {
            SessionManager sessionManager = null;
            AbstractSessionIdManager sessionIdManager = null;

            if (sessionConnection.startsWith("jdbc:")) {
                System.out.println("Using JDBC session backend");
                sessionIdManager = new JDBCSessionIdManager(server);
                ((JDBCSessionIdManager)sessionIdManager).setWorkerName(System.getProperty("session.workername", "biomart1"));
                ((JDBCSessionIdManager)sessionIdManager).setDriverInfo("com.mysql.jdbc.Driver", sessionConnection);
                ((JDBCSessionIdManager)sessionIdManager).setScavengeInterval(60);
                sessionManager = new JDBCSessionManager();
            } else if (sessionConnection.startsWith("voldemort:")) {
                System.out.println("Using Voldemort session backend");
                Matcher matcher = Pattern.compile("voldemort://[a-z]+(.*?)/(.*)").matcher(sessionConnection);
                matcher.find();
                sessionIdManager = new VoldemortSessionIdManager(server);
                String connectionUrl = matcher.group(1);
                String[] urls = connectionUrl.split(",");
                connectionUrl = "";
                // Default to TCP for Voldemort connections
                for (String url : urls) {
                    if (!url.matches("[a-z]+://.*")) {
                        url = "tcp://" + url;
                    }
                    connectionUrl += url + ",";
                }
                ((VoldemortSessionIdManager)sessionIdManager).setDriverInfo(connectionUrl, matcher.group(2));
                sessionIdManager.setWorkerName(System.getProperty("session.workername", "biomart1"));
                sessionManager = new VoldemortSessionManager();
            }

            if (sessionIdManager != null && sessionManager != null) {
                String sessionDomain = System.getProperty("session.domain");
                if (sessionDomain != null) {
                    sessionManager.setSessionDomain(sessionDomain);
                }
                sessionManager.setSessionPath("/");
                sessionManager.setIdManager(sessionIdManager);
                context.setSessionHandler(new SessionHandler(sessionManager));
            }
        }

        String sessionTimeout = System.getProperty("session.timeout");
        if (sessionTimeout != null) {
            System.out.println(String.format("Setting session timeout to %s seconds", sessionTimeout));
            SessionManager mgr = context.getSessionHandler().getSessionManager();
            int timeout = Integer.parseInt(sessionTimeout);
            mgr.setMaxCookieAge(timeout);
            mgr.setMaxInactiveInterval(timeout);
        }

    }

    private void setupLogging(HandlerCollection handlers) throws IOException {
		if (!Boolean.getBoolean("biomart.debug")) {
			String logDir = _homeDir + "/logs";
            Files.createParentDirs(new File(logDir));

			NCSARequestLog log = new NCSARequestLog();
			RequestLogHandler logHandler = new RequestLogHandler();

			log.setFilename(logDir + "/request.yyyy_mm_dd.log");
			log.setFilenameDateFormat("yyyy_MM_dd");
			log.setRetainDays(90);
			log.setAppend(true);
			log.setExtended(false);
			log.setLogCookies(false);
			log.setLogTimeZone("GMT");
            log.setLogDateFormat(Constants.UTC_DATE_FORMAT);

			logHandler.setRequestLog(log);

			// Stout/err Log
			PrintStream out = new PrintStream(new RolloverFileOutputStream(
					logDir + "/stdouterr.yyyy_mm_dd.log",
					false, 90, TimeZone.getTimeZone("GMT")));

			System.setErr(out);
			System.setOut(out);

			handlers.addHandler(logHandler);
		}

    }

    private void installPlugins(List<String> appLocations) throws IOException {
        // Traverse plugins directory
        File pluginsDir = new File (_homeDir + "/plugins");
        if (pluginsDir.exists()) {
            for (File file : pluginsDir.listFiles( new FileFilter() {
                    @Override public boolean accept(File file) {
                        return file.isDirectory();
                    }})) {
                File publicDir = new File(file, "public");
                // Add all plugins that contain public directory to a new martappsCxt
                if (publicDir.exists()) {
                    appLocations.add(publicDir.getCanonicalPath());
                }
            }
        }
    }

    private void setupJawrBundling() {
        if (Boolean.getBoolean("biomart.debug")) {
            System.setProperty("net.jawr.debug.on", "true");
        } else {
            System.setProperty("net.jawr.debug.on", "false");
        }
    }
}
