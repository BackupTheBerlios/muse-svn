package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.gnutella.GnutellaConnectionEvent;
import com.echomine.gnutella.GnutellaConnectionModel;
import com.echomine.gnutella.GnutellaContext;
import com.echomine.net.ConnectionEvent;
import com.echomine.net.ConnectionVetoException;
import com.echomine.net.HandshakeFailedException;
import com.echomine.util.IOUtil;

import java.io.IOException;

/**
 * The acceptor connection is used to create a connection object for an incoming connection.  Since the connection is already
 * established, there is no need to provide functionality such as connect() or require information such as connection models
 * and such.  This is basically a wrapper for a incoming connection so that it can work with the message routers
 * and the connection manager.
 */
public class GnutellaAcceptorConnection extends AbstractGnutellaConnection {
    /**
     * this will create a gnutella acceptor connection that uses the connection that you specify.  The list of
     * protocols that is supported is listed in the GnutellaProtocolFactory
     */
    public GnutellaAcceptorConnection(GnutellaContext context, int protocolType, GnutellaConnectionModel cmodel) {
        super(context, protocolType);
        this.cmodel = cmodel;
    }

    public void handle(Socket socket) {
        //set the connection model first
        GnutellaConnectionEvent e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_STARTING, this);
        GnutellaConnectionEvent vetoEvent = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_VETOED, this);
        try {
            gnutellaHandler.start();
            fireConnectionStarting(e, vetoEvent);
            //handshaking first
            gnutellaHandler.handshake(socket);
            e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_OPENED, this);
            connected = true;
            fireConnectionEstablished(e);
            //send out initial ping message
            resetStats();
            //then have the real handler handle the rest of the data routing.
            gnutellaHandler.handle(socket);
            //fire off connection closed event
            e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_CLOSED, this);
            fireConnectionClosed(e);
        } catch (ConnectionVetoException ex) {
            //do nothing as connection closed event is already fired
        } catch (HandshakeFailedException ex) {
            //connection handshaking problem, fire connection closed
            e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_ERRORED, "Error during handshaking: " + ex.getMessage(), this);
            fireConnectionClosed(e);
        } catch (IOException ex) {
            //error during handling
            e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_ERRORED, "Error while handling connection: " + ex.getMessage(), this);
            fireConnectionClosed(e);
        } finally {
            IOUtil.closeSocket(socket);
        }
        connected = false;
    }
}
