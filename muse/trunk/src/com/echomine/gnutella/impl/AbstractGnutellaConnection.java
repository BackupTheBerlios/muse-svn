package com.echomine.gnutella.impl;

import com.echomine.common.SendMessageFailedException;
import com.echomine.gnutella.*;
import com.echomine.net.TimeableConnection;
import com.echomine.util.HTTPHeader;

/**
 * The abstract connection contains common functionality that is used by most of the subclasses.
 * It also contains "dummy" method that implements its parent interface. Statistics updates are implemented
 * to collect stats for the connection itself.
 */
public abstract class AbstractGnutellaConnection extends TimeableConnection implements GnutellaConnection {
    private MsgInit pingMsg;
    protected RawDataReceivable router;
    protected GnutellaProtocolSocketHandler gnutellaHandler;
    protected GnutellaConnectionModel cmodel;
    protected boolean connected = false;
    protected long messages = 0;
    protected long files = 0;
    protected long size = 0;
    protected int hosts = 0;
    protected GnutellaContext context;
    protected IConnectionList clist;

    /**
     * this will create a gnutella acceptor connection that uses the connection that you specify.  The list of
     * protocols that is supported is listed in the GnutellaProtocolFactory
     */
    public AbstractGnutellaConnection(GnutellaContext context, int protocolType) {
        this.context = context;
        router = new GnutellaMessageReceiver(context.getMessageFactory());
        gnutellaHandler = GnutellaProtocolFactory.createHandlerFor(protocolType, this, router);
        addMessageListener(new StatMessageListener());
    }

    /** Sends a message to the gnutella client */
    public void send(GnutellaMessage msg) throws SendMessageFailedException {
        if (!connected)
            throw new SendMessageFailedException("Cannot send message...not connected to server");
        gnutellaHandler.send(msg);
    }

    public GnutellaConnectionModel getConnectionModel() {
        return cmodel;
    }

    /** Disconnects from the remote server */
    public void disconnect() {
        //reset connection state
        gnutellaHandler.shutdown();
    }

    public boolean isConnected() {
        return connected;
    }

    /** @return the protocol type as specified in GnutellaProtocolType */
    public int getProtocolType() {
        return gnutellaHandler.getProtocolType();
    }

    /** @return supported feature headers, can be null */
    public HTTPHeader getSupportedFeatureHeaders() {
        return gnutellaHandler.getSupportedFeatureHeaders();
    }

    /** @return vendor feature headers, can be null */
    public HTTPHeader getVendorFeatureHeaders() {
        return gnutellaHandler.getVendorFeatureHeaders();
    }

    /** subscribe to listen for messages */
    public void addMessageListener(GnutellaMessageListener l) {
        router.addMessageListener(l);
    }

    /** unsubscribe from listening to messages */
    public void removeMessageListener(GnutellaMessageListener l) {
        router.removeMessageListener(l);
    }

    /** adds new stat listener to the connection */
    public void addStatisticsListener(GnutellaConnectionStatisticsListener l) {
        listenerList.add(GnutellaConnectionStatisticsListener.class, l);
    }

    /** removes stat listener from the connection */
    public void removeStatisticsListener(GnutellaConnectionStatisticsListener l) {
        listenerList.remove(GnutellaConnectionStatisticsListener.class, l);
    }

    /** @return number of hosts known by this connection for stat purposes */
    public int getHosts() {
        return hosts;
    }

    /** @return number of messages received from this connection for stat purposes */
    public long getMessages() {
        return messages;
    }

    /** @return number of files shared from this connection for stat purposes */
    public long getFiles() {
        return files;
    }

    /** @return the size in KB that has been known coming from this connection for stat purposes */
    public long getSize() {
        return size;
    }

    /**
     * reset the stats for this connection and restart the stat collection.  This will
     * send out a new ping message to restart collection of new connection stats.
     */
    public void resetStats() {
        messages = 0;
        files = 0;
        size = 0;
        hosts = 0;
        //reset the statistics and the ping message to obtain new stats for this
        //connection
        pingMsg = new MsgInit();
        fireConnectionStatsUpdated();
        sendPingMsg();
    }

    /** @return the gnutella context associated with this connection */
    public GnutellaContext getContext() {
        return context;
    }

    /** sends the ping message, but does not first create a new instance of the init msg. */
    void sendPingMsg() {
        //set the TTL to 5
        pingMsg.getHeader().setTTL(5);
        //now send the message
        gnutellaHandler.send(pingMsg);
    }

    /**
     * A visitor that takes in a gnutella pong response and adds the data information it contains
     * to the stats contained for this connection.
     */
    void parseStatResponse(MsgInitResponse msgr) {
        GUID pingGUID = pingMsg.getHeader().getMsgID();
        if ((pingGUID != null) && msgr.getHeader().getMsgID().equals(pingGUID)) {
            //the PONG message is issued by us, let's get it
            files += msgr.getFileCount();
            size += msgr.getTotalSize();
            hosts++;
        }
    }

    protected void fireConnectionStatsUpdated() {
        //notify listeners
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == GnutellaConnectionStatisticsListener.class) {
                // Lazily create the event:
                ((GnutellaConnectionStatisticsListener) listeners[i + 1]).connectionStatsUpdated(this);
            }
        }
    }

    class StatMessageListener implements GnutellaMessageListener {
        public void messageReceived(GnutellaMessageEvent event) {
            messages++;
            fireConnectionStatsUpdated();
        }
    }
}
