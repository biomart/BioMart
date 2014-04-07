package org.mortbay.jetty.servlet;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.VoldemortSessionManager.SessionData;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import voldemort.client.ClientConfig;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.versioning.Versioned;

/**
 *
 * @author jhsu
 *
 */
public class VoldemortSessionIdManager extends AbstractSessionIdManager {
    private final String __sessionIds = "sessionIds";

    private String __storeName;
    private String __connectionUrl;

    private final ObjectMapper _mapper = new ObjectMapper();

    private VoldemortDataStore _sessionStore;

    protected Timer _timer; //scavenge timer
    protected TimerTask _task; //scavenge task
    protected long _lastScavengeTime;
    protected long _scavengeIntervalMs = 1000 * 60 * 10; //10mins

    protected class VoldemortDataStore {
        private final StoreClient<String,Object> client;

        public VoldemortDataStore(String storeName) {
            String[] urls = __connectionUrl.split(",");
            StoreClientFactory factory = new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls(urls));
            client = factory.getStoreClient(storeName);
        }

        public Object get(String k) {
            return client.getValue(k);
        }

        public List<Object> getAll(Iterable<String> keys) {
            List<Object> list = new ArrayList<Object>();
            Map<String, Versioned<Object>> map = client.getAll(keys);
            for (Entry<String,Versioned<Object>> entry : map.entrySet()) {
                list.add(entry.getValue().getValue());
            }
            return list;
        }

        public void put(String k, Object v) {
            client.put(k, v);
        }

        public boolean delete(String k) {
            return client.delete(k);
        }
    }

    public VoldemortSessionIdManager(Server server) {
        super(server);
    }
   
    public VoldemortSessionIdManager(Server server, String storeName, String connectionUrl) {
        super(server);
        __storeName = storeName;
        __connectionUrl = connectionUrl;
    }

    public VoldemortSessionIdManager(Server server, Random random) {
       super(server, random);
    }

    /**
     * Configure voldemort connection information
     *
     * @param driverClassName
     * @param connectionUrl
     */
    public void setDriverInfo (String connectionUrl, String storeName) {
        __connectionUrl = connectionUrl;
        __storeName = storeName;
    }

    @Override
    public void doStart() {
        _sessionStore = new VoldemortDataStore(__storeName);
        synchronized (_sessionStore) {
            Object ids = _sessionStore.get(__sessionIds);
            if (ids == null) {
                List<String> list = new ArrayList<String>();
                try {
                    _sessionStore.put(__sessionIds, _mapper.writeValueAsBytes(list));
                } catch  (IOException e) {
                    Log.warn("Error creating ID list", e);
                }
            }
        }
        super.doStart();
        _timer = new Timer("VoldemortSessionScavenger", true);
        setScavengeInterval(getScavengeInterval());
    }

    @Override
    public void doStop() throws Exception {
        synchronized(this) {
            if (_task!=null)
                _task.cancel();
            if (_timer!=null)
                _timer.cancel();
            _timer=null;
        }
        super.doStop();
    }

    @Override
    public boolean idInUse(String id) {
        synchronized (_sessionStore) {
            byte[] ids = (byte[])_sessionStore.get(__sessionIds);
            Iterable<String> it = Splitter.on(',').split(new String(ids));
            return Iterables.contains(it, id);
        }
    }

    public void setScavengeInterval (long sec) {
        if (sec<=0)
            sec=60;

        long old_period=_scavengeIntervalMs;
        long period=sec*1000;

        _scavengeIntervalMs=period;

        //add a bit of variability into the scavenge time so that not all
        //nodes with the same scavenge time sync up
        long tenPercent = _scavengeIntervalMs/10;
        if ((System.currentTimeMillis()%2) == 0)
            _scavengeIntervalMs += tenPercent;

        if (Log.isDebugEnabled()) Log.debug("Scavenging every "+_scavengeIntervalMs+" ms");
        if (_timer!=null && (period!=old_period || _task==null)) {
            synchronized (this) {
                if (_task!=null)
                    _task.cancel();
                _task = new TimerTask() {
                    public void run()
                    {
                        scavenge();
                    }
                };
                _timer.schedule(_task,_scavengeIntervalMs,_scavengeIntervalMs);
            }
        }
    }

    public long getScavengeInterval () {
        return _scavengeIntervalMs/1000;
    }

    @Override
    public void addSession(HttpSession session) {
        if (session == null) return;
        String id = ((VoldemortSessionManager.Session)session).getClusterId();
        synchronized (_sessionStore) {
            try {
                byte[] bytes = (byte[])_sessionStore.get(__sessionIds);
                List<String> ids;
                if (bytes != null && bytes.length > 0) {
                    ids = _mapper.readValue(bytes, 0, bytes.length, new TypeReference<List<String>>(){});
                } else {
                    ids = new ArrayList<String>();
                }
                ids.add(id);
                _sessionStore.put(__sessionIds, _mapper.writeValueAsBytes(ids));
            } catch(IOException e) {
                Log.warn("Problem adding session: " + id, e);
            }
        }
    }

    @Override
    public void removeSession(HttpSession session) {
        if (session == null) return;
        String id = ((VoldemortSessionManager.Session)session).getClusterId();
        synchronized (_sessionStore) {
            try {
                byte[] bytes = (byte[])_sessionStore.get(__sessionIds);
                List<String> ids = _mapper.readValue(bytes, 0, bytes.length, new TypeReference<List<String>>(){});
                ids.remove(id);
                _sessionStore.put(__sessionIds, _mapper.writeValueAsBytes(ids));
            } catch(IOException e) {
                Log.warn("Problem removing session: " + id, e);
            }
        }
    }

    @Override
    public void invalidateAll(String id) {
        synchronized (_sessionStore) {
            try {
                byte[] bytes = (byte[])_sessionStore.get(__sessionIds);
                List<String> ids = _mapper.readValue(bytes, 0, bytes.length, new TypeReference<List<String>>(){});
                ids.remove(id);
                _sessionStore.put(__sessionIds, _mapper.writeValueAsBytes(ids));
                _sessionStore.delete(id);
            } catch(IOException e) {
                Log.warn("Problem invalidating session: " + id, e);
            }
        }
    }

    /**
     * Get the session id without any node identifier suffix.
     *
     * @see org.mortbay.jetty.SessionIdManager#getClusterId(java.lang.String)
     */
    @Override
    public String getClusterId(String nodeId) {
        int dot = nodeId.lastIndexOf('.');
        return (dot > 0) ? nodeId.substring(0,dot) : nodeId;
    }

    /**
     * Get the session id, including this node's id as a suffix.
     *
     * @see org.mortbay.jetty.SessionIdManager#getNodeId(java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    @Override
    public String getNodeId(String clusterId, HttpServletRequest request) {
        if (_workerName != null)
            return clusterId + '.' + _workerName;

        return clusterId;
    }

    protected VoldemortDataStore getConnection() {
        return _sessionStore;
    }

    /**
     * Look for sessions in the database that have expired.
     *
     * We do this in the SessionIdManager and not the SessionManager so
     * that we only have 1 scavenger, otherwise if there are n SessionManagers
     * there would be n scavengers, all contending for the database.
     *
     * We look first for sessions that expired in the previous interval, then
     * for sessions that expired previously - these are old sessions that no
     * node is managing any more and have become stuck in the database.
     */
    private void scavenge () {
        List expiredSessionIds = new ArrayList();
        List<String> validIds = new ArrayList<String>();
        try {
            if (Log.isDebugEnabled()) Log.debug("Scavenge sweep started at "+System.currentTimeMillis());
            byte[] bytes = (byte[])_sessionStore.get(__sessionIds);
            List<String> ids = _mapper.readValue(bytes, 0, bytes.length, new TypeReference<List<String>>(){});

            if (_lastScavengeTime > 0) {
                //"select sessionId from JettySessions where expiryTime > (lastScavengeTime - scanInterval) and expiryTime < lastScavengeTime";
                long lowerBound = (_lastScavengeTime - _scavengeIntervalMs);
                long upperBound = _lastScavengeTime;
                long upperBound2 =  _lastScavengeTime - (2 * _scavengeIntervalMs);
                if (Log.isDebugEnabled()) Log.debug("Searching for sessions expired between "+lowerBound + " and "+upperBound);
                for (String id : ids) {
                    byte[] dataBytes = (byte[])_sessionStore.get(id);
                    if (dataBytes != null) {
                        SessionData data = _mapper.readValue(dataBytes, 0, dataBytes.length, SessionData.class);
                        long exp = data.expiryTime;

                        //find all sessions that have expired at least a couple of scanIntervals ago and just delete them
                        if (upperBound2 > 0 && exp > 0 && exp <= upperBound2) {
                            _sessionStore.delete(id);
                        } else {
                            if (exp >= lowerBound && exp <= upperBound) {
                                expiredSessionIds.add(id);
                                if (Log.isDebugEnabled()) Log.debug("Found expired sessionId="+id);
                            }
                            validIds.add(id);
                        }
                    } else {
                        _sessionStore.delete(id);
                    }
                }

                // Update with new values
                synchronized(_sessionStore) {
                    _sessionStore.put(__sessionIds, _mapper.writeValueAsBytes(ids));
                }

                //tell the SessionManagers to expire any sessions with a matching sessionId in memory
                Handler[] contexts = _server.getChildHandlersByClass(WebAppContext.class);
                for (int i=0; contexts!=null && i<contexts.length; i++) {
                    AbstractSessionManager manager = ((AbstractSessionManager)((WebAppContext)contexts[i]).getSessionHandler().getSessionManager());
                    if (manager instanceof VoldemortSessionManager) {
                        ((VoldemortSessionManager)manager).expire(expiredSessionIds);
                    }
                }
            }
        } catch (Exception e) {
            Log.warn("Problem selecting expired sessions", e);
        } finally {
            _lastScavengeTime=System.currentTimeMillis();
            if (Log.isDebugEnabled()) Log.debug("Scavenge sweep ended at "+_lastScavengeTime);
        }
    }
}
