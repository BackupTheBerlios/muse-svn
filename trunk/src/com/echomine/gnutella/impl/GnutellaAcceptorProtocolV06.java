package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.gnutella.GnutellaConnection;
import com.echomine.gnutella.GnutellaContext;
import com.echomine.gnutella.GnutellaProtocolType;
import com.echomine.gnutella.RawDataReceivable;
import com.echomine.net.HandshakeFailedException;
import com.echomine.util.HTTPHeader;
import com.echomine.util.HTTPResponseHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The acceptor protocol will handle incoming connection handshakes.  This will be used by a listening server to
 * process incoming gnutella client connections.
 */
public class GnutellaAcceptorProtocolV06 extends AbstractGnutellaProtocol {
    protected final static byte[] RESPONSE_STRING = "GNUTELLA/0.6 200 OK\r\n".getBytes();

    public GnutellaAcceptorProtocolV06(GnutellaConnection connection, RawDataReceivable receiver) {
        super(connection, receiver);
    }

    /** The handshake does an initial check to see if the negotiation is successful */
    public void handshake(Socket socket) throws HandshakeFailedException {
        super.handshake(socket);
        try {
            GnutellaContext context = getConnection().getContext();
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            //parse headers
            HTTPHeader supportedFeatures = new HTTPHeader();
            supportedFeatures.parseHeaders(is);
            setSupportedFeatureHeaders(supportedFeatures);
            // at this point, the remote is waiting for us to send a response
            // so send it
            os.write(RESPONSE_STRING);
            //write a set of headers that tells the remote what we support (both default and vendor specific features)
            if (context.getSupportedFeatureHeaders() != null)
                os.write(context.getSupportedFeatureHeaders().getHeadersAsString().getBytes());
            if (context.getVendorFeatureHeaders() != null)
                os.write(context.getVendorFeatureHeaders().getHeadersAsString().getBytes());
            os.write("\r\n".getBytes());
            //now read in any vendor specific reponse and headers
            HTTPResponseHeader vendorFeatures = new HTTPResponseHeader();
            vendorFeatures.parse(is);
            setVendorFeatureHeaders(vendorFeatures);
            //check if the status code is good
            if (vendorFeatures.getStatusCode() != 200)
                throw new HandshakeFailedException("Bad Status Code During Handshake");
        } catch (IOException ex) {
            throw new HandshakeFailedException(ex.getMessage());
        }
        logHandshakeHeaders();
    }

    public int getProtocolType() {
        return GnutellaProtocolType.PROTOCOL_ACCEPTOR_V06;
    }
}
