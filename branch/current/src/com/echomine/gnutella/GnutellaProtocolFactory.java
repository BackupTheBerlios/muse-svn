package com.echomine.gnutella;

import com.echomine.gnutella.impl.*;

public class GnutellaProtocolFactory implements GnutellaProtocolType {
    /**
     * creates a socket handler for the specified protocol type
     * All parameters are required except for the HTTPHeader params.  Those are only required for protocols that requires them
     * @return the socket handler or null if the protocol is not supported
     * @throws IllegalStateException if the protocol handler is not found for the protocol specified
     */
    public static GnutellaProtocolSocketHandler createHandlerFor(int protocolType, GnutellaConnection conn, RawDataReceivable router) {
        switch (protocolType) {
            case PROTOCOL_ACCEPTOR_V04:
                return new GnutellaAcceptorProtocolV04(conn, router);
            case PROTOCOL_ACCEPTOR_V06:
                return new GnutellaAcceptorProtocolV06(conn, router);
            case PROTOCOL_BUSY_ACCEPTOR_V04:
                return new GnutellaBusyAcceptorProtocolV04(conn, router);
            case PROTOCOL_BUSY_ACCEPTOR_V06:
                return new GnutellaBusyAcceptorProtocolV06(conn, router);
            case PROTOCOL_REJECTED_ACCEPTOR_V04:
                return new GnutellaRejectedAcceptorProtocolV04(conn, router);
            case PROTOCOL_REJECTED_ACCEPTOR_V06:
                return new GnutellaRejectedAcceptorProtocolV06(conn, router);
            case PROTOCOL_CONNECTOR_V04:
                return new GnutellaConnectorProtocolV04(conn, router);
            case PROTOCOL_CONNECTOR_V06:
                return new GnutellaConnectorProtocolV06(conn, router);
            default:
                throw new IllegalStateException("Unable to find a Protocol Handler for the specified protocol type");
        }
    }
}
