package com.echomine.gnutella;

import com.echomine.gnutella.impl.*;
import com.echomine.net.Connection;
import com.echomine.net.ConnectionFailedException;
import com.echomine.net.ConnectionListener;
import com.echomine.net.ConnectionModel;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>Maintains connections and makes sure only a certain number of connections are maintained at a time.  It is basically the
 * lowest level for connection interaction that upper layer should touch.
 * It also listens on each connection so that when a connection is opened or closed, it'll consult the HostManager
 * and obtain another host to connect.</p> <p>Essentially, this is a thread pool manager that will create a set number of
 * threads to handle the connections for more efficient memory management.</p>
 * <p>The procedure to using the connection manager is a little different from other connection management styles.  You must
 * first indicate that you are online before you can be able to connect to any gnutella hosts (ie. you must call the
 * method start() first).  Once the state of the connection manager is changed to an online state, you can begin
 * connecting to client.  The reasoning behind this extra step is because you may not always be connected to a host when
 * you're online.  Another reason is that the hosts can actually be retrieved from a Host Manager rather than directly from
 * input of the user/developer.  If this is the case, once the state is changed to online, hosts will start getting connected
 * to the hosts provided by the host manager.</p>
 */
public class GnutellaConnectionManager extends Connection {
    GnutellaMessageRouter router;
    boolean connected;
    GnutellaContext context;
    ArrayList connectionThreads;
    GnutellaV04RequestHandler gnutellaV04Handler;
    GnutellaV06RequestHandler gnutellaV06Handler;
    IConnectionList connectionList;
    GnutellaConnectionQueue queue;
    HostManager hmanager;
    GnutellaStatistics stats;

    /** Constructor that accepts a context and uses the default for everything else. */
    public GnutellaConnectionManager(GnutellaContext context, GnutellaListenerRouter listenerRouter) {
        super();
        this.context = context;
        connectionList = new ConnectionListImpl();
        stats = new GnutellaStatisticsImpl(this);
        router = new ClientMessageRouter(context, connectionList);
        queue = new GnutellaConnectionQueue();
        //initial vector capacity is 10 connections
        connectionThreads = new ArrayList(10);
        //initialize connect list listener
        gnutellaV04Handler = new GnutellaV04RequestHandler(context, connectionList);
        gnutellaV06Handler = new GnutellaV06RequestHandler(context, connectionList);
        listenerRouter.addRequestHandler(gnutellaV04Handler);
        listenerRouter.addRequestHandler(gnutellaV06Handler);
        hmanager = new HostManagerImpl(this);
    }

    /**
     * This represents that we are online and ready for adding connections.  It doesn't mean that we have connections to
     * remote clients.  It is usually used so that we can synchronize between all thread access to this object.  It also MUST
     * be called before doing any online-related work.  This is typically the first function that you must call
     * before doing anything else.
     */
    public synchronized void start() {
        connected = true;
        connectionList.start();
        createWorkers(getMaxOutgoingConnections());
    }

    /**
     * Connects to a specific client. Note that it does not queue the connection.  Rather, it actually creates a physical
     * thread to handle the connection.  This is purposely done this way as developers might
     * want to explicitly connect to specific hosts without waiting for it to be picked up by a worker thread.
     * @return GnutellaConnectionModel that represents the connection information used
     */
    public GnutellaConnectionModel connect(String server, int port) throws UnknownHostException {
        if (!connected) return null;
        GnutellaConnectionModel model = new GnutellaConnectionModel(server, port);
        connect(model);
        return model;
    }

    /**
     * Connects to a client.  Note that it does not queue the connection.  Rather, it actually creates a physical thread to
     * handle the connection.  This is purposely done this way as developers might
     * want to explicitly connect to specific hosts without waiting for it to be picked up by a worker thread.
     */
    public void connect(GnutellaConnectionModel model) {
        if (!connected) return;
        queue.insert(model);
        //don't check if any worker threads are free.  Automatically create an extra worker thread.
        //This will temporarily cause an extra thread to exist, but eventually, extraneous threads
        //will be shutdown during idle timeouts
        createWorkers(1);
    }

    /** shutdown all connections to gnutella */
    public void disconnect() {
        synchronized (this) {
            connected = false;
        }
        //disconnect all worker threads first
        synchronized (connectionThreads) {
            int size = connectionThreads.size();
            GnutellaConnectionWorker worker;
            for (int i = 0; i < size; i++) {
                worker = (GnutellaConnectionWorker) connectionThreads.remove(0);
                worker.shutdown();
            }
        }
        connectionList.shutdown();
    }

    /**
     * disconnect only one specific connection if it happens to still be connected.
     * @return the connection associated with the model
     */
    public GnutellaConnection disconnect(ConnectionModel model) {
        GnutellaConnection conn = connectionList.getActiveConnection(model);
        if (conn != null)
            conn.disconnect();
        return conn;
    }

    /**
     * This is the appropriate method to use to send normal messages by the developers or outsiders.
     * It will add the message to the routing list of seen messages so that future messages will be
     * routed and events will be fired correctly.
     */
    public void send(GnutellaMessage msg) {
        if (!connected) return;
        router.addToOwnMsg(msg);
        //send the message
        connectionList.sendMessageToAll(msg);
    }

    /** @return the message router that routes all the messages */
    public GnutellaMessageRouter getMessageRouter() {
        return router;
    }

    /** @return the host manager that manages the host list */
    public HostManager getHostManager() {
        return hmanager;
    }

    /** @return the statistics collector that contains all stats */
    public GnutellaStatistics getStatistics() {
        return stats;
    }

    /** @return the established connection count */
    public int getEstablishedConnectionsCount() {
        return connectionList.getEstablishedConnectionsCount();
    }

    /** @return the active connection (including established) count */
    public int getActiveConnectionsCount() {
        return connectionList.getActiveConnectionsCount();
    }

    /** @return a list of active GnutellaConnection objects */
    public List getActiveConnections() {
        return connectionList.getActiveConnections();
    }

    /** @return a list of established GnutellaConnection objects */
    public List getEstablishedConnections() {
        return connectionList.getEstablishedConnections();
    }

    /**
     * retrieves the active gnutella connection that is associated with the model.
     * @return the connection or null if no connection is associated with the model
     */
    public GnutellaConnection getActiveConnection(ConnectionModel model) {
        return connectionList.getActiveConnection(model);
    }

    /**
     * retrieves the established gnutella connection that is associated with the model.
     * @return the connection or null if no connection is associated with the model
     */
    public GnutellaConnection getEstablishedConnection(ConnectionModel model) {
        return connectionList.getEstablishedConnection(model);
    }

    /** sets the max outgoing connections */
    public void setMaxOutgoingConnections(int maxOutgoingConnections) {
        connectionList.setMaxOutgoingConnections(maxOutgoingConnections);
    }

    /** @return the max outgoing connections */
    public int getMaxOutgoingConnections() {
        return connectionList.getMaxOutgoingConnections();
    }

    /** sets the max incoming connections */
    public void setMaxIncomingConnections(int maxIncomingConnections) {
        connectionList.setMaxIncomingConnections(maxIncomingConnections);
    }

    /** @return max incoming connections */
    public int getMaxIncomingConnections() {
        return connectionList.getMaxIncomingConnections();
    }

    /** adds method to add connection listener. */
    public void addConnectionListener(ConnectionListener l) {
        connectionList.addConnectionListener(l);
    }

    /** adds method to remove connection listener */
    public void removeConnectionListener(ConnectionListener l) {
        connectionList.removeConnectionListener(l);
    }

    /**
     * Listens to all incoming messages.  The messages are not filtered, meaning
     * that all incoming messages will be received.
     */
    public void addMessageListener(GnutellaMessageListener l) {
        connectionList.addMessageListener(l);
    }

    /** remove from listening to messages coming from all connections. */
    public void removeMessageListener(GnutellaMessageListener l) {
        connectionList.removeMessageListener(l);
    }

    /**
     * creates the specified number of connection worker threads that will handle connections.
     * @param num number of work threads to create at a time
     */
    protected void createWorkers(int num) {
        if (!connected || num <= 0) return;
        synchronized (connectionThreads) {
            for (int i = 0; i < num; i++) {
                GnutellaConnectionWorker worker = new GnutellaConnectionWorker(queue);
                Thread thread = new Thread(worker);
                thread.start();
                connectionThreads.add(worker);
            }
        }
    }

    /**
     * The main worker thread that takes in a connection info and connects to a specific host.  It contains its own connection
     * object so that it's reused.  This is very memory and performance efficient.
     */
    class GnutellaConnectionWorker implements Runnable {
        private static final long IDLE_TIMEOUT = 5000;
        private boolean shutdown;
        private GnutellaConnectionManager.GnutellaConnectionQueue queue;
        private GnutellaConnectorConnection connection;

        public GnutellaConnectionWorker(GnutellaConnectionQueue queue) {
            this.queue = queue;
            connection = new GnutellaConnectorConnection(context, GnutellaProtocolType.PROTOCOL_CONNECTOR_V06);
        }

        public void run() {
            shutdown = false;
            GnutellaConnectionModel model;
            while (!shutdown) {
                //wait for new requests
                model = queue.waitForWork(IDLE_TIMEOUT);
                if (model == null) {
                    if (connectionThreads.size() > connectionList.getMaxOutgoingConnections()) {
                        synchronized (connectionThreads) {
                            connectionThreads.remove(this);
                        }
                        //no model received, shutdown
                        shutdown = true;
                        break;
                    }
                }
                //got request...let's connect
                if (!shutdown && (model != null)) {
                    //synchronous connect
                    try {
                        //if connection is added just fine, let's start it
                        //otherwise, just ignore this connection and move
                        //on to the next one
                        if (connectionList.addConnection(connection, model))
                            connection.connect(model);
                    } catch (ConnectionFailedException ex) {
                        //do nothing for connection failure
                        //just loop and wait for next request
                    }
                } else {
                    //shutdown received AFTER model is retrieved
                    //put the model back for use
                    if (model != null)
                        queue.insert(model);
                }
            }
        }

        /** Shut down the connection immediately.  This will close the connection explicitly. */
        public void shutdown() {
            shutdown = true;
            Thread.currentThread().interrupt();
            connection.disconnect();
        }

        public GnutellaConnection getConnection() {
            return connection;
        }
    }


    /**
     * Contains the queue.  It will notify workers when connection requests are received.  The workers will
     * then obtain the connection.
     */
    class GnutellaConnectionQueue {
        private LinkedList connections = new LinkedList();

        /**
         * puts the thread into a wait state until new connection requests arrive.
         * @return GnutellaConnectionModel or null if none exists
         */
        public synchronized GnutellaConnectionModel waitForWork(long timeout) {
            if (connections.size() == 0) {
                //if worker threads are over max connections, then
                //notify the worker to shut itself down
                if (connectionThreads.size() <= getMaxOutgoingConnections()) {
                    Host host = hmanager.next();
                    if (host != null) {
                        return new GnutellaConnectionModel(host.getHost(), host.getPort());
                    } else {
                        try {
                            this.wait(timeout);
                        } catch (InterruptedException ex) {
                        }
                    }
                } else {
                    return null;
                }
            }
            try {
                if (connections.size() > 0)
                    return (GnutellaConnectionModel) connections.removeFirst();
            } catch (NoSuchElementException ex) {
            }
            return null;
        }

        /** adds a new connection model to the queue and notifies a worker that new connection has arrived. */
        public synchronized void add(GnutellaConnectionModel model) {
            connections.addLast(model);
            this.notify();
        }

        /** Inserts the connection to the beginning of the array so that it has top priority */
        public synchronized void insert(GnutellaConnectionModel model) {
            connections.addFirst(model);
            this.notify();
        }

        /** shuts down the queue host checker */
        public synchronized void clear() {
            connections.clear();
            this.notifyAll();
        }
    }
}
