package com.echomine.gnutella;

import com.echomine.common.SendMessageFailedException;
import com.echomine.net.ConnectionListener;
import com.echomine.util.HTTPHeader;

/**
 * The interface describes an object that is capable of maintaining connection specific functions.  This interface is here
 * mainly to separate out the logic dependency between a Gnutella Connection and a RawDataReceivable.
 * It also contains the methods that works with statistics for each connection.
 * In addition, the connection also contains the time that the connection has
 * been online.
 */
public interface GnutellaConnection {
    /** Sends a message to the gnutella client */
    void send(GnutellaMessage msg) throws SendMessageFailedException;

    /** Disconnects from the remote server */
    void disconnect();

    /** checks to see if the connection is connected or not */
    boolean isConnected();

    /** @return the gnutella context associated with this connection */
    GnutellaContext getContext();

    /** @return the connection model associated with this connection, or null if non-existent */
    GnutellaConnectionModel getConnectionModel();

    /** listen for connection events for this connection */
    public void addConnectionListener(ConnectionListener l);

    /** remove from listening to connection events for this connection */
    public void removeConnectionListener(ConnectionListener l);

    /** listen for all messages going through this connection */
    void addMessageListener(GnutellaMessageListener l);

    /** remove from listening to all messages going through this connection */
    void removeMessageListener(GnutellaMessageListener l);

    /** adds new stat listener to the connection */
    void addStatisticsListener(GnutellaConnectionStatisticsListener l);

    /** removes stat listener from the connection */
    void removeStatisticsListener(GnutellaConnectionStatisticsListener l);

    /** @return number of hosts known by this connection for stat purposes */
    int getHosts();

    /** @return number of messages received from this connection for stat purposes */
    long getMessages();

    /** @return number of files shared from this connection for stat purposes */
    long getFiles();

    /** @return the size in KB that has been known coming from this connection for stat purposes */
    long getSize();

    /** reset the stats for this connection and restart the stat collection */
    void resetStats();

    /**
     * returns the protocol type that this connection is using.  The list of protocol types are listed in
     * GnutellaProtocolType.  This will let you know if the protocol is an incoming or outgoing connection,
     * and it will also let you know what protocol it is using.
     */
    int getProtocolType();

    /**
     * retrieves a list of feature headers that the current connection supports.  These will also include
     * other headers such as the user agent and other information.
     */
    HTTPHeader getSupportedFeatureHeaders();

    /**
     * retrieves a list of vendor-specific feature headers that the current connection supports.  These will also include
     * other headers such as the user agent and other information.
     */
    HTTPHeader getVendorFeatureHeaders();
}
