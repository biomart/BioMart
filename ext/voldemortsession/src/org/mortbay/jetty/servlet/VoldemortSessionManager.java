// ========================================================================
// Copyright 2008 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================


package org.mortbay.jetty.servlet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.codehaus.jackson.map.ObjectMapper;

import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.VoldemortSessionIdManager.VoldemortDataStore;
import org.mortbay.log.Log;
import org.mortbay.util.LazyList;

/**
 * VoldemortSessionManager
 *
 * SessionManager that persists sessions to a database to enable clustering.
 *
 * Session data is persisted to the JettySessions table:
 *
 * rowId (unique in cluster: webapp name/path + virtualhost + sessionId)
 * contextPath (of the context owning the session)
 * sessionId (unique in a context)
 * lastNode (name of node last handled session)
 * accessTime (time in ms session was accessed)
 * lastAccessTime (previous time in ms session was accessed)
 * createTime (time in ms session created)
 * cookieTime (time in ms session cookie created)
 * lastSavedTime (last time in ms session access times were saved)
 * expiryTime (time in ms that the session is due to expire)
 * map (attribute map)
 *
 * As an optimisation, to prevent thrashing the database, we do not persist
 * the accessTime and lastAccessTime every time the session is accessed. Rather,
 * we write it out every so often. The frequency is controlled by the saveIntervalSec
 * field.
 */
public class VoldemortSessionManager extends AbstractSessionManager {

    private static final String __attributesSuffix = ":attributes";

    private ConcurrentHashMap _sessions;

    private final ObjectMapper _mapper = new ObjectMapper();

    /**
     * SessionData
     *
     * Persistable data about a session.
     */
    public static class SessionData {
        public String id;
        public long accessed;
        public long lastAccessed;
        public long maxIdleMs;
        public long cookieSet;
        public long created;
        public Map<String,Object> attributes;
        public String lastNode;
        public String canonicalContext;
        public long lastSaved;
        public long expiryTime;
        public String virtualHost;

        public SessionData() {}

        public SessionData (String sessionId) {
            id = sessionId;
            created = System.currentTimeMillis();
            accessed = created;
            attributes = new HashMap<String,Object>();
        }

        @Override
        public SessionData clone() {
            SessionData data = new SessionData(id);
            data.accessed = accessed;
            data.lastAccessed = lastAccessed;
            data.maxIdleMs = maxIdleMs;
            data.cookieSet = cookieSet;
            data.created = created;
            data.lastNode = lastNode;
            data.canonicalContext = canonicalContext;
            data.lastSaved = lastSaved;
            data.expiryTime = expiryTime;
            data.virtualHost = virtualHost;
            return data;
        }

        @Override
        public String toString () {
            return "Session id="+id+",lastNode="+lastNode+
                            ",created="+created+",accessed="+accessed+
                            ",lastAccessed="+lastAccessed+",cookieSet="+cookieSet+
                            "lastSaved="+lastSaved;
        }
    }



    /**
     * Session
     *
     * Session instance in memory of this node.
     */
    public class Session extends AbstractSessionManager.Session {
        private SessionData _data;

        /**
         * Session from a request.
         *
         * @param request
         */
        protected Session (HttpServletRequest request) {

            super(request);
            _data = new SessionData(_clusterId);
            _data.maxIdleMs = _dftMaxIdleSecs*1000;
            _data.canonicalContext = canonicalize(_context.getContextPath());
            _data.virtualHost = getVirtualHost(_context);
            _data.expiryTime = _maxIdleMs < 0 ? 0 : (System.currentTimeMillis() + _maxIdleMs);
            _values = _data.attributes;
        }

        /**
          * Session restored in database.
          * @param row
          */
         protected Session (SessionData data) {
             super(data.created, data.id);
             _data = data;
             _values = data.attributes;
         }

         protected Map newAttributeMap() {
             return _data.attributes;
         }

         @Override
         public void setAttribute (String name, Object value) {
             super.setAttribute(name, value);
             try {
                 updateSession(_data);
             } catch (Exception e) {
                 if (Log.isDebugEnabled())
                     Log.debug("Error occurred during session update");
             }
         }

         @Override
         public void removeAttribute (String name) {
             super.removeAttribute(name);
             try {
                 updateSession(_data);
             } catch (Exception e) {
                 if (Log.isDebugEnabled())
                     Log.debug("Error occurred during session update");
             }
         }

         @Override
         protected void cookieSet() {
             _data.cookieSet = _data.accessed;
         }

        /**
         * Entry to session.
         * Called by SessionHandler on inbound request and the session already exists in this node's memory.
         *
         * @see org.mortbay.jetty.servlet.AbstractSessionManager.Session#access(long)
         */
        @Override
        protected void access(long time) {
            super.access(time);
            _data.lastAccessed = _data.accessed;
            _data.accessed = time;
            _data.expiryTime = _maxIdleMs < 0 ? 0 : (time + _maxIdleMs);
        }

        /**
         * Exit from session
         * @see org.mortbay.jetty.servlet.AbstractSessionManager.Session#complete()
         */
        @Override
        protected void complete() {
            super.complete();
            try {
                //The session attributes have changed, write to the db, ensuring
                //http passivation/activation listeners called
                willPassivate();
                updateSession(_data);
                didActivate();
            } catch (Exception e) {
                Log.warn("Problem persisting changed session data id="+getId(), e);
            } finally {
            }
        }

        @Override
        protected void timeout() throws IllegalStateException {
            if (Log.isDebugEnabled()) Log.debug("Timing out session id="+getClusterId());
            super.timeout();
        }
    }

    /**
     * A session has been requested by it's id on this node.
     *
     * Load the session by id AND context path from the database.
     * Multiple contexts may share the same session id (due to dispatching)
     * but they CANNOT share the same contents.
     *
     * Check if last node id is my node id, if so, then the session we have
     * in memory cannot be stale. If another node used the session last, then
     * we need to refresh from the db.
     *
     * NOTE: this method will go to the database, so if you only want to check
     * for the existence of a Session in memory, use _sessions.get(id) instead.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#getSession(java.lang.String)
     */
    @Override
    public Session getSession(String idInCluster) {
        Session session;

        synchronized (this) {
            try {
                //check if we need to reload the session - don't do it on every call
                //to reduce the load on the database. This introduces a window of
                //possibility that the node may decide that the session is local to it,
                //when the session has actually been live on another node, and then
                //re-migrated to this node. This should be an extremely rare occurrence,
                //as load-balancers are generally well-behaved and consistently send
                //sessions to the same node, changing only iff that node fails.
                SessionData data = null;
                long now = System.currentTimeMillis();
//                if (Log.isDebugEnabled()) Log.debug("now="+now+
//                        " lastSaved="+(session==null?0:session._data._lastSaved)+
//                        " difference="+(now - (session==null?0:session._data._lastSaved)));

                data = loadSession(idInCluster);

                if (data != null) {
//                    if (!data.getLastNode().equals(getIdManager().getWorkerName()) || session==null) {
//                        //session last used on a different node, or we don't have it in memory
                        session = new Session(data);
                        _sessions.put(idInCluster, session);
                        session.didActivate();
//                        //TODO is this the best way to do this? Or do this on the way out using
//                        //the _dirty flag?
//                        updateSessionNode(data);
//                    } else if (Log.isDebugEnabled())
//                        Log.debug("Session not stale "+session._data);
                    //session in db shares same id, but is not for this context
                } else {
                    //No session in db with matching id and context path.
                    session=null;
                    if (Log.isDebugEnabled()) Log.debug("No session in database matching id="+idInCluster);
                }

                return session;
            } catch (Exception e) {
                Log.warn("Unable to load session from database", e);
                return null;
            }
        }
    }


    /**
     * Get all the sessions as a map of id to Session.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#getSessionMap()
     */
    @Override
    public Map getSessionMap() {
       return Collections.unmodifiableMap(_sessions);
    }


    /**
     * Get the number of sessions.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#getSessions()
     */
    @Override
    public int getSessions() {
        int size = 0;
        synchronized (this) {
            size = _sessions.size();
        }
        return size;
    }


    /**
     * Start the session manager.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#doStart()
     */
    @Override
    public void doStart() throws Exception {
        if (_sessionIdManager==null)
            throw new IllegalStateException("No session id manager defined");

        prepareData();

        _sessions = new ConcurrentHashMap();
        super.doStart();
    }


    /**
     * Stop the session manager.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#doStop()
     */
    @Override
    public void doStop() throws Exception {
        _sessions.clear();
        _sessions = null;

        super.doStop();
    }

    @Override
    protected void invalidateSessions() {
        //Do nothing - we don't want to remove and
        //invalidate all the sessions because this
        //method is called from doStop(), and just
        //because this context is stopping does not
        //mean that we should remove the session from
        //any other nodes
    }


    /**
     * Invalidate a session.
     *
     * @param idInCluster
     */
    protected void invalidateSession (String idInCluster) {
        synchronized (this) {
            Session session = (Session)_sessions.get(idInCluster);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    /**
     * Delete an existing session, both from the in-memory map and
     * the database.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#removeSession(java.lang.String)
     */
    @Override
    protected void removeSession(String idInCluster) {
        synchronized (this) {
           try {
               Session session = (Session)_sessions.remove(idInCluster);
               deleteSession(session._data);
           } catch (Exception e) {
               Log.warn("Problem deleting session id="+idInCluster, e);
           }
        }
    }


    /**
     * Add a newly created session to our in-memory list for this node and persist it.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#addSession(org.mortbay.jetty.servlet.AbstractSessionManager.Session)
     */
    @Override
    protected void addSession(AbstractSessionManager.Session session) {
        if (session==null)
            return;

        synchronized (this) {
            _sessions.put(session.getClusterId(), session);
            //TODO or delay the store until exit out of session? If we crash before we store it
            //then session data will be lost.
            try {
                ((VoldemortSessionManager.Session)session).willPassivate();
                storeSession(((VoldemortSessionManager.Session)session)._data);
                ((VoldemortSessionManager.Session)session).didActivate();
            } catch (Exception e) {
                Log.warn("Unable to store new session id="+session.getId() , e);
            }
        }
    }


    /**
     * Make a new Session.
     *
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#newSession(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected AbstractSessionManager.Session newSession(HttpServletRequest request) {
        return new Session(request);
    }

    /* ------------------------------------------------------------ */
    /** Remove session from manager
     * @param session The session to remove
     * @param invalidate True if {@link HttpSessionListener#sessionDestroyed(HttpSessionEvent)} and
     * {@link SessionIdManager#invalidateAll(String)} should be called.
     */
    @Override
    public void removeSession(AbstractSessionManager.Session session, boolean invalidate) {
        // Remove session from context and global maps
        synchronized (_sessionIdManager) {
            boolean removed = false;

            synchronized (this) {
                //take this session out of the map of sessions for this context
                if (_sessions.get(session.getClusterId()) != null)
                {
                    removed = true;
                    removeSession(session.getClusterId());
                }
            }

            if (removed) {
                // Remove session from all context and global id maps
                _sessionIdManager.removeSession(session);
                if (invalidate)
                    _sessionIdManager.invalidateAll(session.getClusterId());
            }
        }

        if (invalidate && _sessionListeners!=null) {
            HttpSessionEvent event=new HttpSessionEvent(session);
            for (int i=LazyList.size(_sessionListeners); i-->0;)
                ((HttpSessionListener)LazyList.get(_sessionListeners,i)).sessionDestroyed(event);
        }
        if (!invalidate) {
            session.willPassivate();
        }
    }


    /**
     * Expire any Sessions we have in memory matching the list of
     * expired Session ids.
     *
     * @param sessionIds
     */
    protected void expire (List sessionIds) {
        //don't attempt to scavenge if we are shutting down
        if (isStopping() || isStopped())
            return;

        //Remove any sessions we already have in memory that match the ids
        Thread thread=Thread.currentThread();
        ClassLoader old_loader=thread.getContextClassLoader();
        ListIterator itor = sessionIds.listIterator();

        try {
            while (itor.hasNext()) {
                String sessionId = (String)itor.next();
                if (Log.isDebugEnabled()) Log.debug("Expiring session id "+sessionId);
                Session session = (Session)_sessions.get(sessionId);
                if (session != null) {
                    session.timeout();
                    itor.remove();
                    int count = this._sessions.size();
                    if (count < this._minSessions)
                        this._minSessions=count;
                } else {
                    if (Log.isDebugEnabled()) Log.debug("Unrecognized session id="+sessionId);
                }
            }
        } catch (Throwable t) {
            if (t instanceof ThreadDeath)
                throw ((ThreadDeath)t);
            else
                Log.warn("Problem expiring sessions", t);
        } finally {
            thread.setContextClassLoader(old_loader);
        }
    }


    protected void prepareData () {}

    private String getAttributeKey(String key) {
        return key + "__attributeSuffix";
    }

    private SessionData loadData(String key) throws Exception {
        VoldemortDataStore store = getConnection();
        SessionData data = null;

        synchronized (store) {
            byte[] dataBytes = (byte[])store.get(key);
            String attributeKey = getAttributeKey(key);

            if (dataBytes != null) {
                data = _mapper.readValue(dataBytes, 0, dataBytes.length, SessionData.class);
                data.attributes = (Map)store.get(attributeKey);
            }
        }

        return data;
    }

    private void storeData(SessionData data) throws Exception {
        VoldemortDataStore store = getConnection();
        String key = data.id;
        String attributeKey = getAttributeKey(key);

        synchronized (store) {
            // Write without attributes
            SessionData cloned = data.clone();
            store.put(key, _mapper.writeValueAsBytes(cloned));
            store.put(attributeKey, data.attributes);
        }
    }


    /**
     * Load a session from the database
     * @param id
     * @return
     * @throws Exception
     */
    protected SessionData loadSession (String id)
            throws Exception {
        SessionData data = null;
        String key = id;

        data = loadData(key);

        if (Log.isDebugEnabled())
           Log.debug("LOADED session "+data);

        return data;
    }

    /**
     * Insert a session into the database.
     *
     * @param data
     * @throws Exception
     */
    protected void storeSession (SessionData data) throws Exception {
        if (data==null)
            return;

        long now = System.currentTimeMillis();
        data.lastNode = getIdManager().getWorkerName();
        data.lastSaved = now;

        storeData(data);

        if (Log.isDebugEnabled())
            Log.debug("Stored session "+data);

    }


    /**
     * Update data on an existing persisted session.
     *
     * @param data
     * @throws Exception
     */
    protected void updateSession(SessionData data) throws Exception {
        if (data==null)
            return;

        long now = System.currentTimeMillis();
        data.lastSaved = now;

        storeData(data);

        if (Log.isDebugEnabled())
            Log.debug("Updated session "+data);
    }


    /**
     * Delete a session from the database. Should only be called
     * when the session has been invalidated.
     *
     * @param data
     * @throws Exception
     */
    protected void deleteSession (SessionData data)
    throws Exception {
        VoldemortDataStore store = getConnection();
        String key = data.id;
        store.delete(key);
        if (Log.isDebugEnabled())
            Log.debug("Deleted Session "+data);
    }



    /**
     * Get a connection from the driver.
     * @return
     * @throws SQLException
     */
    private VoldemortDataStore getConnection () {
        return ((VoldemortSessionIdManager)getIdManager()).getConnection();
    }

    /**
     * Calculate a unique id for this session across the cluster.
     *
     * Unique id is composed of: contextpath_virtualhost0_sessionid
     * @param data
     * @return
     */
    private String calculateRowId (SessionData data) {
        String rowId = canonicalize(_context.getContextPath());
        rowId = rowId + "_" + getVirtualHost(_context);
        rowId = rowId+"_"+data.id;
        return rowId;
    }

    /**
     * Get the first virtual host for the context.
     *
     * Used to help identify the exact session/contextPath.
     *
     * @return 0.0.0.0 if no virtual host is defined
     */
    private String getVirtualHost (ContextHandler.SContext context) {
        String vhost = "0.0.0.0";

        if (context==null)
            return vhost;

        String [] vhosts = context.getContextHandler().getVirtualHosts();
        if (vhosts==null || vhosts.length==0 || vhosts[0]==null)
            return vhost;

        return vhosts[0];
    }

    /**
     * Make an acceptable file name from a context path.
     *
     * @param path
     * @return
     */
    private String canonicalize (String path) {
        if (path==null)
            return "";

        return path.replace('/', '_').replace('.','_').replace('\\','_');
    }
}
