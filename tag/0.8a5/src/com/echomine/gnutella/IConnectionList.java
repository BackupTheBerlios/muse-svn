package com.echomine.gnutella;

import com.echomine.net.ConnectionListener;
import com.echomine.net.ConnectionModel;

import java.net.InetAddress;
import java.util.List;

/**
 * The interface definition for all the methods as required by contract to work with connections
 */
public interface IConnectionList {

    /** starts up the connect list and do any data resetting */
    void start();

    /** shuts down all the known connections in the connection list */
    void shutdown();

    /** @return number of active connections (connections that are either starting or established) */
    int getActiveConnectionsCount();

    /** @return number of established connections (ie. connected, not just starting to connect) */
    int getEstablishedConnectionsCount();

    /** @return the active connection specified by the connection model, null if non-existent */
    GnutellaConnection getActiveConnection(ConnectionModel model);

    /** @return the established connection specified by the connection model, null if non-existent */
    GnutellaConnection getEstablishedConnection(ConnectionModel model);

    /** @return a list of active connections, containing objects of type GnutellaConnection */
    List getActiveConnections();

    /** @return a list of established connections, containing objects of type GnutellaConnection */
    List getEstablishedConnections();

    /** sends a message to all the established connections */
    void sendMessageToAll(GnutellaMessage msg);

    /** sends a message to all the established connections except for the connection named in the parameter */
    void sendMessageToAllExcept(GnutellaMessage msg, GnutellaConnection exceptConn);

    /** sets the restricted host callback to the one specified */
    void setRestrictedHostCallback(IRestrictedHostCallback callback);

    /** checks whether a host is restricted or not */
    boolean isHostRestricted(InetAddress host);

    /**
     * adds a new connection to the list.  The connection should be added
     * into the list way before a connection starting event is called.
     * The method is responsible for removing the connection off the list
     * once the connection closes.
     * @param conn the GnutellaConnection that will be starting
     * @param cmodel the connection model
     * @return true if connection is added successfully, false otherwise
     */
    boolean addConnection(GnutellaConnection conn, GnutellaConnectionModel cmodel);

    /** sets the max outgoing connections */
    void setMaxOutgoingConnections(int maxOutgoingConnections);

    /** @return the max outgoing connections */
    int getMaxOutgoingConnections();

    /** sets the max incoming connections */
    void setMaxIncomingConnections(int maxIncomingConnections);

    /** @return max incoming connections */
    int getMaxIncomingConnections();

    /** @return true if max outgoing connections are reached, false otherwise */
    boolean isMaxOutgoingReached();

    /** @return true if max incoming connections are reached, false otherwise */
    boolean isMaxIncomingReached();

    /** subscribes to listen for connection events coming from all connections, both incoming and outgoing */
    void addConnectionListener(ConnectionListener l);

    /** removes subscriber from listening to connection events */
    void removeConnectionListener(ConnectionListener l);

    /** subscribes to listen for messages coming from all connections */
    void addMessageListener(GnutellaMessageListener l);

    /** removes subscriber from listening to messages */
    void removeMessageListener(GnutellaMessageListener l);
}
