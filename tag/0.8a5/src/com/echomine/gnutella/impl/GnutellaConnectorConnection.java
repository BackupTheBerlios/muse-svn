package com.echomine.gnutella.impl;

import com.echomine.gnutella.GnutellaConnectionEvent;
import com.echomine.gnutella.GnutellaConnectionModel;
import com.echomine.gnutella.GnutellaContext;
import com.echomine.net.*;

/**
 * represent one connection to a remote client.  It contains some wrapper functions that lets you access certain underlying
 * functionality such as listening for messages.  The connection methods are asynchronous.  This means that if you want to
 * deal with any connection events, you need to subscribe as a listener before you do any connection.
 */
public class GnutellaConnectorConnection extends AbstractGnutellaConnection {
    private HandshakeableSocketConnector gnutellaConnector;

    /**
     * @param context cannot be null
     * @param protocolType must be a type from GnutellaProtocolType
     */
    public GnutellaConnectorConnection(GnutellaContext context, int protocolType) {
        super(context, protocolType);
        gnutellaConnector = new HandshakeableSocketConnector(gnutellaHandler);
        //add myself to listen for connection events
        gnutellaConnector.addConnectionListener(new GnutellaConnectionListener());
    }

    /**
     * Connect to a specific server. This method is synchronous and will not return
     * until after connection has been fully processed.  This is normally used
     * when the caller of this method is a worker thread.
     */
    public void connect(GnutellaConnectionModel cmodel) throws ConnectionFailedException {
        if (connected) return;
        this.cmodel = cmodel;
        gnutellaConnector.connect(cmodel);
    }

    class GnutellaConnectionListener implements ConnectionListener {
        public void connectionStarting(ConnectionEvent e) throws ConnectionVetoException {
            GnutellaConnectionEvent vetoEvent = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_VETOED, GnutellaConnectorConnection.this);
            fireConnectionStarting(new GnutellaConnectionEvent(e, GnutellaConnectorConnection.this), vetoEvent);
        }

        public void connectionEstablished(ConnectionEvent e) {
            connected = true;
            fireConnectionEstablished(new GnutellaConnectionEvent(e, GnutellaConnectorConnection.this));
            //reset stats and send out initial ping message
            resetStats();
        }

        public void connectionClosed(ConnectionEvent e) {
            connected = false;
            fireConnectionClosed(new GnutellaConnectionEvent(e, GnutellaConnectorConnection.this));
        }
    }
}
