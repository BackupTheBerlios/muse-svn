package com.echomine.gnutella;

import com.echomine.net.ConnectionModel;
import com.echomine.util.HTTPHeader;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A customized conneciton model that includes the information which tells you if the connection is an incoming or outgoing
 * connection to begin with.
 */
public class GnutellaConnectionModel extends ConnectionModel {
    public static final int OUTGOING = 1;
    public static final int INCOMING = 2;
    public static final String INCOMING_STRING = "Incoming";
    public static final String OUTGOING_STRING = "Outgoing";
    public static final String UNKNOWN_STRING = "Unknown";
    private int connectionType;
    private HTTPHeader connectionHeaders = null;

    public GnutellaConnectionModel(int port) {
        this(port, OUTGOING);
    }

    public GnutellaConnectionModel(InetAddress host, int port) {
        this(host, port, OUTGOING);
    }

    public GnutellaConnectionModel(String hostname, int port) throws UnknownHostException {
        this(hostname, port, OUTGOING);
    }

    public GnutellaConnectionModel(int port, int connectionType) {
        super(port);
        this.connectionType = connectionType;
    }

    public GnutellaConnectionModel(InetAddress host, int port, int connectionType) {
        super(host, port);
        this.connectionType = connectionType;
    }

    public GnutellaConnectionModel(String hostname, int port, int connectionType) throws UnknownHostException {
        super(hostname, port);
        this.connectionType = connectionType;
    }

    public int getConnectionType() {
        return connectionType;
    }

    /**
     * Additional connection headers that are not part of the feature headers or supported vendor feature
     * headers.  This is used mostly for HTTP protocol headers (ie. Content Encoding, etc). However, I am
     * not 100% sure this is the case because the code is contributed.  If I am wrong, please correct
     * comment and resubmit the patched file to me. -- ckchris
     * @return the additional connection headers
     */
    public HTTPHeader getConnectionHeaders() {
        return connectionHeaders;
    }

    /**
     * add in any additiona connection headers that are not part of the feature headers or supported vendor feature
     * headers.  This is used mostly for HTTP protocol headers (ie. Content Encoding, etc). However, I am
     * not 100% sure this is the case because the code is contributed.  If I am wrong, please correct
     * comment and resubmit the patched file to me. -- ckchris
     */
    public void setConnectionHeader(String headerName, String headerValue) {
        if (connectionHeaders == null)
            connectionHeaders = new HTTPHeader();
        connectionHeaders.setHeader(headerName, headerValue);
    }

    public String getConnectionTypeString() {
        switch (connectionType) {
            case INCOMING:
                return INCOMING_STRING;
            case OUTGOING:
                return OUTGOING_STRING;
            default:
                return UNKNOWN_STRING;
        }
    }
}
