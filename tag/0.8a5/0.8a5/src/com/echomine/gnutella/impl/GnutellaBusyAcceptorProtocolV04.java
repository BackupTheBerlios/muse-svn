package com.echomine.gnutella.impl;

import com.echomine.gnutella.GnutellaConnection;
import com.echomine.gnutella.GnutellaProtocolType;
import com.echomine.gnutella.RawDataReceivable;
import com.echomine.net.HandshakeFailedException;

import java.io.IOException;
import java.net.Socket;

/**
 * The acceptor protocol will handle incoming connection handshakes when
 * the server is considered busy. The handshake will send a busy signal and
 * always throw an exception so that the handle method will never be called.
 */
public class GnutellaBusyAcceptorProtocolV04 extends AbstractGnutellaProtocol {
    protected final static byte[] RESPONSE_STRING = "GNUTELLA BUSY\n\n".getBytes();

    public GnutellaBusyAcceptorProtocolV04(GnutellaConnection connection, RawDataReceivable receiver) {
        super(connection, receiver);
    }

    /** The handshake sends a busy response and then throwns an exception to close the connection */
    public void handshake(Socket socket) throws HandshakeFailedException {
        super.handshake(socket);
        try {
            //busy response should be sent
            socket.getOutputStream().write(RESPONSE_STRING);
            throw new HandshakeFailedException("Server Busy");
        } catch (IOException ex) {
            throw new HandshakeFailedException(ex.getMessage());
        }
    }

    public int getProtocolType() {
        return GnutellaProtocolType.PROTOCOL_BUSY_ACCEPTOR_V04;
    }
}
