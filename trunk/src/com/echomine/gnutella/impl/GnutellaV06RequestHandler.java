package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.gnutella.*;

import java.io.IOException;

/**
 * this handler handles incoming Gnutella 0.6 connections.  Incoming gnutella handshaking is done here.
 * Note that Gnutella 0.6 specs supposedly state that new protocols (versions higher than 0.6 for instance)
 * should be parsed correctly for incoming, but the response should still be Gnutella 0.6.  This is NOT supported
 * in this class.  This is because each protocol should have its own procotol handlers to begin with anyways.
 */
public class GnutellaV06RequestHandler implements ListenerRequestHandler {
    protected final static String CONNECT_STRING = "GNUTELLA CONNECT/0.6";
    private GnutellaContext context;
    private IConnectionList connectionList;

    public GnutellaV06RequestHandler(GnutellaContext context, IConnectionList connectionList) {
        this.context = context;
        this.connectionList = connectionList;
    }

    /** Will only handle requests for Gnutella Clients that are at protocol version 0.6 */
    public boolean canHandle(String request) {
        if (CONNECT_STRING.equals(request)) return true;
        return false;
    }

    /** simply handles the request and routes packets in the background */
    public void handleRequest(String request, Socket socket) throws IOException {
        GnutellaAcceptorConnection connection;
        //if server max connections are reached, use a busy-handler handshake
        GnutellaConnectionModel cmodel = new GnutellaConnectionModel(socket.getInetAddress(), socket.getPort(), GnutellaConnectionModel.INCOMING);
        if (connectionList.isMaxIncomingReached())
            connection = new GnutellaAcceptorConnection(context, GnutellaProtocolType.PROTOCOL_BUSY_ACCEPTOR_V06, cmodel);
        else if (context.getRestrictedHostCallback() != null && context.getRestrictedHostCallback().isHostRestricted(socket.getInetAddress()))
            connection = new GnutellaAcceptorConnection(context, GnutellaProtocolType.PROTOCOL_REJECTED_ACCEPTOR_V06, cmodel);
        else
            connection = new GnutellaAcceptorConnection(context, GnutellaProtocolType.PROTOCOL_ACCEPTOR_V06, cmodel);
        //connection events gets fired inside handle()
        if (connectionList.addConnection(connection, cmodel))
            connection.handle(socket);
    }
}
