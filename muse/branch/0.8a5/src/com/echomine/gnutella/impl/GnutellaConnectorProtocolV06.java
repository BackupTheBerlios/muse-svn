package com.echomine.gnutella.impl;

import com.echomine.gnutella.GnutellaConnection;
import com.echomine.gnutella.GnutellaContext;
import com.echomine.gnutella.GnutellaProtocolType;
import com.echomine.gnutella.RawDataReceivable;
import com.echomine.net.HandshakeFailedException;
import com.echomine.util.HTTPResponseHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/** The connector protocol will handle outgoing gnutella client connection handshakes. */
public class GnutellaConnectorProtocolV06 extends AbstractGnutellaProtocol {
    final static byte[] RESPONSE_STRING = "GNUTELLA/0.6 200 OK\r\n".getBytes();
    final static byte[] CONNECT_STRING = "GNUTELLA CONNECT/0.6\r\n".getBytes();

    public GnutellaConnectorProtocolV06(GnutellaConnection connection, RawDataReceivable receiver) {
        super(connection, receiver);
    }

    public void handshake(Socket socket) throws HandshakeFailedException {
        super.handshake(socket);
        try {
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            //write first negotiation data with supported features
            os.write(CONNECT_STRING);
            GnutellaContext context = getConnection().getContext();
            if (context.getSupportedFeatureHeaders() != null)
                os.write(context.getSupportedFeatureHeaders().getHeadersAsString().getBytes());
            if (getConnection().getConnectionModel().getConnectionHeaders() != null)
                os.write(getConnection().getConnectionModel().getConnectionHeaders().getHeadersAsString().getBytes());
            os.write("\r\n".getBytes());
            //read the response from server
            HTTPResponseHeader remoteResponse = new HTTPResponseHeader();
            remoteResponse.parse(is);
            //save the responses
            setSupportedFeatureHeaders(remoteResponse);
            if (remoteResponse.getStatusCode() != 200)
                throw new HandshakeFailedException("Bad Status Code During Handshake");
            //send our vendor specific data
            os.write(RESPONSE_STRING);
            if (context.getVendorFeatureHeaders() != null)
                os.write(context.getVendorFeatureHeaders().getHeadersAsString().getBytes());
            os.write("\r\n".getBytes());
        } catch (IOException ex) {
            throw new HandshakeFailedException(ex.getMessage());
        }
        logHandshakeHeaders();
    }

    public int getProtocolType() {
        return GnutellaProtocolType.PROTOCOL_CONNECTOR_V06;
    }
}
