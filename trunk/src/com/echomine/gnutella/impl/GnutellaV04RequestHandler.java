package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.gnutella.*;

import java.io.IOException;

/** this handler handles incoming Gnutella connections.  Incoming gnutella handshaking is done here. */
public class GnutellaV04RequestHandler implements ListenerRequestHandler {
    protected final static String CONNECT_STRING = "GNUTELLA CONNECT/0.4";
    private GnutellaContext context;
    private IConnectionList connectionList;

    public GnutellaV04RequestHandler(GnutellaContext context, IConnectionList connectionList) {
        this.connectionList = connectionList;
        this.context = context;
    }

    /** Will only handle requests for Gnutella Clients that are at protocol version 0.4 */
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
            connection = new GnutellaAcceptorConnection(context, GnutellaProtocolType.PROTOCOL_BUSY_ACCEPTOR_V04, cmodel);
        else if (context.getRestrictedHostCallback().isHostRestricted(socket.getInetAddress()))
            connection = new GnutellaAcceptorConnection(context, GnutellaProtocolType.PROTOCOL_REJECTED_ACCEPTOR_V04, cmodel);
        else
            connection = new GnutellaAcceptorConnection(context, GnutellaProtocolType.PROTOCOL_ACCEPTOR_V04, cmodel);
        //connection events gets fired inside handle()
        if (connectionList.addConnection(connection, cmodel))
            connection.handle(socket);
    }
}
