package com.echomine.gnutella.impl;

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
import java.net.Socket;

/**
 * The acceptor protocol will handle incoming connection handshakes that are busy.
 * The handler simply does a handshake and does not do any processing at all
 * (considering that it is busy).  The handshake will always throw an exception
 * so that handle will never be called.
 */
public class GnutellaBusyAcceptorProtocolV06 extends AbstractGnutellaProtocol {
    public GnutellaBusyAcceptorProtocolV06(GnutellaConnection connection, RawDataReceivable receiver) {
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
            // at this point, the remote is waiting for us to send a response
            // so send a busy response
            HTTPResponseHeader response = new HTTPResponseHeader();
            response.setProtocol("GNUTELLA/0.6");
            response.setStatus(503, "Busy");
            os.write(response.toString().getBytes());
            //write a set of headers that tells the remote what we support (both default and vendor specific features)
            if (context.getSupportedFeatureHeaders() != null)
                os.write(context.getSupportedFeatureHeaders().getHeadersAsString().getBytes());
            if (context.getVendorFeatureHeaders() != null)
                os.write(context.getVendorFeatureHeaders().getHeadersAsString().getBytes());
            os.write("\r\n".getBytes());
            //now read in any vendor specific reponse and headers
            HTTPResponseHeader vendorFeatures = new HTTPResponseHeader();
            vendorFeatures.parse(is);
            //doesn't matter if status is good or not, throw exception no matter what
            throw new HandshakeFailedException("Server Busy, Connection Rejected");
        } catch (IOException ex) {
            throw new HandshakeFailedException(ex.getMessage());
        }
    }

    public int getProtocolType() {
        return GnutellaProtocolType.PROTOCOL_BUSY_ACCEPTOR_V06;
    }
}
