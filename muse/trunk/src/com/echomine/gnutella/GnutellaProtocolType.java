package com.echomine.gnutella;

/**
 * A list of protocol handler types that you can instantiate from
 * GnutellaProtocolFactory.
 */
public interface GnutellaProtocolType {
    static final int PROTOCOL_ACCEPTOR_V04 = 1;
    static final int PROTOCOL_ACCEPTOR_V06 = 2;
    static final int PROTOCOL_BUSY_ACCEPTOR_V04 = 3;
    static final int PROTOCOL_BUSY_ACCEPTOR_V06 = 4;
    static final int PROTOCOL_REJECTED_ACCEPTOR_V04 = 5;
    static final int PROTOCOL_REJECTED_ACCEPTOR_V06 = 6;
    static final int PROTOCOL_CONNECTOR_V04 = 7;
    static final int PROTOCOL_CONNECTOR_V06 = 8;
}
