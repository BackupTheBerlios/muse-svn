package com.echomine.gnutella.impl;

import com.echomine.gnutella.GnutellaConnection;
import com.echomine.gnutella.GnutellaProtocolType;
import com.echomine.gnutella.RawDataReceivable;
import com.echomine.net.HandshakeFailedException;
import com.echomine.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/** The connector protocol will handle outgoing gnutella client connection handshakes. */
public class GnutellaConnectorProtocolV04 extends AbstractGnutellaProtocol {
    final static String RESPONSE_STRING = "GNUTELLA OK";
    final static byte[] CONNECT_STRING = "GNUTELLA CONNECT/0.4\n\n".getBytes();

    public GnutellaConnectorProtocolV04(GnutellaConnection connection, RawDataReceivable receiver) {
        super(connection, receiver);
    }

    public void handshake(Socket socket) throws HandshakeFailedException {
        super.handshake(socket);
        try {
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[64];
            //write connection string
            os.write(CONNECT_STRING);
            os.flush();
            //read reply from server
            int len = IOUtil.readToCRLF(is, buffer, 0, 64);
            String request = new String(buffer, 0, len);
            if (!request.equals(RESPONSE_STRING))
                throw new HandshakeFailedException("Error establishing handshake");
        } catch (IOException ex) {
            throw new HandshakeFailedException(ex.getMessage());
        }
    }

    public int getProtocolType() {
        return GnutellaProtocolType.PROTOCOL_CONNECTOR_V04;
    }
}
