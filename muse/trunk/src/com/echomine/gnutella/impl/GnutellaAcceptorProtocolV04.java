package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.gnutella.GnutellaConnection;
import com.echomine.gnutella.GnutellaProtocolType;
import com.echomine.gnutella.RawDataReceivable;
import com.echomine.net.HandshakeFailedException;

import java.io.IOException;

/**
 * The acceptor protocol will handle incoming connection handshakes.  This will be used by a listening server to
 * process incoming gnutella client connections.
 */
public class GnutellaAcceptorProtocolV04 extends AbstractGnutellaProtocol {
    protected final static byte[] RESPONSE_STRING = "GNUTELLA OK\n\n".getBytes();

    public GnutellaAcceptorProtocolV04(GnutellaConnection connection, RawDataReceivable receiver) {
        super(connection, receiver);
    }

    /** The handshake does an initial check to see if the negotiation is successful */
    public void handshake(Socket socket) throws HandshakeFailedException {
        super.handshake(socket);
        try {
            // the remote is waiting for us to send a GNUTELLA OK
            socket.getOutputStream().write(RESPONSE_STRING);
        } catch (IOException ex) {
            throw new HandshakeFailedException(ex.getMessage());
        }
    }

    public int getProtocolType() {
        return GnutellaProtocolType.PROTOCOL_ACCEPTOR_V04;
    }
}
