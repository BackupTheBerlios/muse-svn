package com.echomine.gnutella.impl;

import com.echomine.common.SendMessageFailedException;
import com.echomine.gnutella.*;
import com.echomine.net.*;

import javax.swing.event.EventListenerList;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * <p>This implementation keeps track of all connections, active or established.  By active, it means the connection
 * is in the process of connecting or handshaking, but has not yet been determined to be valid and established
 * and ready for sending/receiving messages.  Established connections are ready for sending/receiving messages
 * and contains full capability into communicating with this connection.  All access to the connection list
 * will go through this one class for synchronization purposes and better management.  Also, this class
 * contains the methods to send messages to all the connections, acting as the main central hub for sending
 * messages.  Message routing is actually done by another class.</p>
 * <p>This class also adds the capability to restrict the max outgoing and incoming connections.  Connections
 * are actively checked during the connection starting event.  All upper level connection listeners
 * will have a chance to process the starting event first (and veto if so required).  Then, the max connections
 * will be checked and vetoed if it is above the maximum.  Subsequently upper level listeners will immediately
 * receive a connection closed event (with status of VETOED).  This is to allow upper level listeners to
 * realize that an incoming/outgoing connection exists, even if it will be vetoed later.</p>
 */
public class ConnectionListImpl extends Connection implements IConnectionList {
    boolean shutdown;
    ArrayList activeConnections;
    ArrayList establishedConnections;
    ConnectionListener listConnListener;
    GnutellaMessageListener listMsgListener;
    HashMap ipList;
    int maxIncomingConnections = 2;
    int maxOutgoingConnections = 10;
    int currentIncomingConnections;
    int currentOutgoingConnections;
    IRestrictedHostCallback restrictedHostCallback;
    EventListenerList msglistenerlist = new EventListenerList();

    public ConnectionListImpl() {
        activeConnections = new ArrayList(30);
        establishedConnections = new ArrayList(30);
        ipList = new HashMap();
        listConnListener = new ListConnectionListener();
        listMsgListener = new ListMessageListener();
    }

    /** @return number of active connections (connections that are either starting or established) */
    public int getActiveConnectionsCount() {
        return activeConnections.size();
    }

    /** @return number of established connections (ie. connected, NOT connections that are starting) */
    public int getEstablishedConnectionsCount() {
        return establishedConnections.size();
    }

    /** @return the active connection specified by the connection model, null if non-existent */
    public GnutellaConnection getActiveConnection(ConnectionModel model) {
        GnutellaConnection connection = null;
        synchronized (activeConnections) {
            int size = activeConnections.size();
            for (int i = 0; i < size; i++) {
                connection = (GnutellaConnection) activeConnections.get(i);
                if (model.equals(connection.getConnectionModel()))
                    break;
            }
        }
        return connection;
    }

    /** @return the established connection specified by the connection model, null if non-existent */
    public GnutellaConnection getEstablishedConnection(ConnectionModel model) {
        GnutellaConnection connection = null;
        synchronized (establishedConnections) {
            int size = establishedConnections.size();
            for (int i = 0; i < size; i++) {
                connection = (GnutellaConnection) establishedConnections.get(i);
                if (model.equals(connection.getConnectionModel()))
                    break;
            }
        }
        return connection;
    }

    /** @return a shallow list of active connections, containing objects of type GnutellaConnection */
    public List getActiveConnections() {
        return (ArrayList) activeConnections.clone();
    }

    /** @return a shallow list of established connections, containing objects of type GnutellaConnection */
    public List getEstablishedConnections() {
        return (ArrayList) establishedConnections.clone();
    }

    /**
     * <p>Sends a message to all the established connections.  This is usually used to send out
     * our own messages such as search requests. Note that this method does NOT add the message to the
     * list of seen messages.  This may cause certain messages not to be fired off to listeners.</p>
     * <p>If there is an error sending the message through a particular connection, the error will be ignored
     * and the message will continue to be sent to all the other connections.</p>
     */
    public void sendMessageToAll(GnutellaMessage msg) {
        synchronized (establishedConnections) {
            int size = establishedConnections.size();
            GnutellaConnection connection;
            for (int i = 0; i < size; i++) {
                connection = (GnutellaConnection) establishedConnections.get(i);
                try {
                    connection.send(msg);
                } catch (SendMessageFailedException ex) {
                    //ignore message sending failures
                }
            }
        }
    }

    /**
     * <p>Sends the message to every established connection except the one specified.
     * This is usually used for routing messages coming from other connections.
     * Note that this method does NOT add the message to the list of seen messages.  This may cause certain
     * messages not to be fired off to listeners.</p>
     * <p>If there is an error sending the message through a particular connection, the error will be ignored
     * and the message will continue to be sent to all the other connections.</p>
     */
    public void sendMessageToAllExcept(GnutellaMessage msg, GnutellaConnection exceptConn) {
        synchronized (establishedConnections) {
            int size = establishedConnections.size();
            GnutellaConnection connection;
            for (int i = 0; i < size; i++) {
                connection = (GnutellaConnection) establishedConnections.get(i);
                if (connection != exceptConn) {
                    try {
                        connection.send(msg);
                    } catch (SendMessageFailedException ex) {
                        //ignore message failures
                    }
                }
            }
        }
    }

    /** sets the restricted host callback to the one specified */
    public void setRestrictedHostCallback(IRestrictedHostCallback callback) {
        this.restrictedHostCallback = callback;
    }

    /**
     * checks whether the host is restricted or not.  By default, if there is
     * no restricted host callback set, then all hosts are allowed.
     */
    public boolean isHostRestricted(InetAddress host) {
        if (restrictedHostCallback == null) return false;
        return restrictedHostCallback.isHostRestricted(host);
    }

    /** sets the max outgoing connections */
    public void setMaxOutgoingConnections(int maxOutgoingConnections) {
        this.maxOutgoingConnections = maxOutgoingConnections;
    }

    /** @return the max outgoing connections */
    public int getMaxOutgoingConnections() {
        return maxOutgoingConnections;
    }

    /** sets the max incoming connections */
    public void setMaxIncomingConnections(int maxIncomingConnections) {
        this.maxIncomingConnections = maxIncomingConnections;
    }

    /** @return max incoming connections */
    public int getMaxIncomingConnections() {
        return maxIncomingConnections;
    }

    /** @return true if max incoming connections are reached, false otherwise */
    public boolean isMaxOutgoingReached() {
        if (currentOutgoingConnections >= maxOutgoingConnections)
            return true;
        return false;
    }

    /** @return true if max incoming connections are reached, false otherwise */
    public boolean isMaxIncomingReached() {
        if (currentIncomingConnections >= maxIncomingConnections)
            return true;
        return false;
    }

    /**
     * Adds a connection to the list. The connection will setup some listeners
     * so that when certain events occur, the connection is properly recorded
     * and sets in the connection list for future references.  The connection
     * will not be added if the connection is already in the list.
     * @return true if connection added successfully, false otherwise
     */
    public boolean addConnection(GnutellaConnection conn, GnutellaConnectionModel cmodel) {
        if (shutdown) return false;
        //check if connection reference already exists
        //if so, ignore.  Otherwise, add it.
        //The connection equality is not tested on connection model,
        //but on the connection object itself
        if (activeConnections.contains(conn)) return false;
        //check whether a connection to the IP is already made
        //if so, there is no need to duplicate the connection
        if (ipList.containsKey(cmodel.getHostAddress()))
            return false;
        conn.addConnectionListener(listConnListener);
        return true;
    }

    /**
     * resets any data that will be reused
     */
    public void start() {
        shutdown = false;
        ipList.clear();
        activeConnections.clear();
        establishedConnections.clear();
    }

    /**
     * shutdown the connections and disallow accepting of new connections
     */
    public void shutdown() {
        shutdown = true;
        Iterator iter = getActiveConnections().iterator();
        GnutellaConnection conn;
        while (iter.hasNext()) {
            conn = (GnutellaConnection) iter.next();
            conn.disconnect();
        }
    }

    /** subscribes to listen for all messages from all connections */
    public void addMessageListener(GnutellaMessageListener l) {
        msglistenerlist.add(GnutellaMessageListener.class, l);
    }

    /** unsubscribes from listening for all messages from all connections */
    public void removeMessageListener(GnutellaMessageListener l) {
        msglistenerlist.remove(GnutellaMessageListener.class, l);
    }

    class ListMessageListener implements GnutellaMessageListener {
        protected void fireMessageReceived(GnutellaMessageEvent event) {
            Object[] listeners = msglistenerlist.getListenerList();
            // Process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == GnutellaMessageListener.class) {
                    // Lazily create the event:
                    ((GnutellaMessageListener) listeners[i + 1]).messageReceived(event);
                }
            }
        }

        public void messageReceived(GnutellaMessageEvent event) {
            fireMessageReceived(event);
        }
    }

    class ListConnectionListener implements ConnectionListener {
        public void connectionStarting(ConnectionEvent e) throws ConnectionVetoException {
            ipList.put(e.getConnectionModel().getHostAddress(), null);
            //check on the max connections
            GnutellaConnection conn = ((GnutellaConnectionEvent) e).getConnection();
            GnutellaConnectionModel model = (GnutellaConnectionModel) e.getConnectionModel();
            boolean maxReached = false;
            switch (model.getConnectionType()) {
                case GnutellaConnectionModel.INCOMING:
                    synchronized (this) {
                        if (currentIncomingConnections >= maxIncomingConnections)
                            maxReached = true;
                        //if it hasn't been reached, then increment and continue
                        currentIncomingConnections++;
                    }
                    break;
                case GnutellaConnectionModel.OUTGOING:
                    synchronized (this) {
                        if (currentOutgoingConnections >= maxOutgoingConnections)
                            maxReached = true;
                        //if it hasn't been reached, then increment and continue
                        currentOutgoingConnections++;
                    }
                    break;
            }
            //record the connection into the list
            synchronized (activeConnections) {
                activeConnections.add(conn);
            }
            conn.addMessageListener(listMsgListener);
            fireConnectionStartingWithoutVeto(e);
            if (maxReached)
                throw new ConnectionVetoException("Max Connection Reached!");
        }

        public void connectionEstablished(ConnectionEvent e) {
            GnutellaConnection conn = ((GnutellaConnectionEvent) e).getConnection();
            //record the connection into the list
            synchronized (establishedConnections) {
                establishedConnections.add(conn);
            }
            fireConnectionEstablished(e);
        }

        public void connectionClosed(ConnectionEvent e) {
            GnutellaConnection conn = ((GnutellaConnectionEvent) e).getConnection();
            //remove all listeners as well
            //as remove from active and established connection lists
            conn.removeConnectionListener(this);
            synchronized (activeConnections) {
                activeConnections.remove(conn);
            }
            synchronized (establishedConnections) {
                establishedConnections.remove(conn);
            }
            fireConnectionClosed(e);
            GnutellaConnectionModel model = (GnutellaConnectionModel) e.getConnectionModel();
            switch (model.getConnectionType()) {
                case GnutellaConnectionModel.INCOMING:
                    currentIncomingConnections--;
                    break;
                case GnutellaConnectionModel.OUTGOING:
                    currentOutgoingConnections--;
                    break;
            }
            conn.removeMessageListener(listMsgListener);
            ipList.remove(e.getConnectionModel().getHostAddress());
        }
    }
}
