package org.biomart.queryEngine.v2;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.biomart.common.exceptions.BioMartException;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu
 *
 * Uses connection pooling to manage any amount of connections. Allows for retries,
 * etc. if the connection gets dropped.
 */
public class ConnectionManager {
    // When the number of sources in lookup map exceeds this size, show warnings 
    // in log... may be a memory problem
    private static final int DEFAULT_WARNING_SIZE = 40;

    private static final int DEFAULT_MIN_POOL_SIZE = 5;
    private static final int DEFAULT_AQUIRE_INCREMENT = 5;
    private static final int DEFAULT_AQUIRE_ATTEMPTS = 20;
    private static final int DEFAULT_AQUIRE_DELAY = 2000;
    private static final int DEFAULT_MAX_POOL_SIZE = 60;
    // Causes getConnection to timeout with SQLException after this number of seconds
    private static final int DEFAULT_CHECKOUT_TIMEOUT = 30000;

    private final Map<String,ComboPooledDataSource> lookup = new HashMap<String,ComboPooledDataSource>();

    /*
     * Returns a connection from a previously created pool. If the pool has not
     * been created already, then created it.
     */
    public Connection getConnection(String driverClass, 
            String jdbcConnectionUrl, String username, String password)
            throws SQLException {

        synchronized (lookup) {
            ComboPooledDataSource cpds = lookup.get(jdbcConnectionUrl);

            if (cpds == null) {
                cpds = new ComboPooledDataSource();
                try {
                    cpds.setDriverClass(driverClass);
                } catch (PropertyVetoException e) {
                    throw new BioMartException(e);
                }
                cpds.setJdbcUrl(jdbcConnectionUrl);

                cpds.setMinPoolSize(DEFAULT_MIN_POOL_SIZE);
                cpds.setAcquireIncrement(DEFAULT_AQUIRE_INCREMENT);
                cpds.setMaxPoolSize(DEFAULT_MAX_POOL_SIZE);
                cpds.setAcquireRetryAttempts(DEFAULT_AQUIRE_ATTEMPTS);
                cpds.setAcquireRetryDelay(DEFAULT_AQUIRE_DELAY);
                cpds.setCheckoutTimeout(DEFAULT_CHECKOUT_TIMEOUT);

                lookup.put(jdbcConnectionUrl, cpds);
            }

            if (lookup.size() >= DEFAULT_WARNING_SIZE) {
                Log.warn("ConnectionManager exceeds " + DEFAULT_WARNING_SIZE + " pools");
            }

            if (username != null && password != null) {
                return cpds.getConnection(username, password);
            }
            return cpds.getConnection();
        }
    }

    public int getCurrentSize() {
        synchronized (lookup) {
            return lookup.size();
        }
    }

    public void killConnectionPool(String jdbcConnectionUrl) throws SQLException {
        ComboPooledDataSource cpds = lookup.get(jdbcConnectionUrl);
        if (cpds != null) {
            DataSources.destroy(cpds);
        }
    }
}
