package com.echomine.gnutella;

import com.echomine.net.ConnectionEvent;
import com.echomine.net.ConnectionModel;

/**
 * In addition to the normally event data stored by the parent ConnectionEvent, this class adds an extra GnutellaConnection to
 * the event so that listeners will know which connection this event came from.
 */
public class GnutellaConnectionEvent extends ConnectionEvent {
    private GnutellaConnection conn;

    /**
     * this constructor creates a new a connection event based on a previously
     * existing event.
     */
    public GnutellaConnectionEvent(ConnectionEvent e, GnutellaConnection conn) {
        this(e.getConnectionModel(), e.getStatus(), e.getErrorMessage(), conn);
    }

    public GnutellaConnectionEvent(ConnectionModel source, int status, GnutellaConnection conn) {
        super(source, status);
        this.conn = conn;
    }

    public GnutellaConnectionEvent(ConnectionModel source, int status, String errormsg, GnutellaConnection conn) {
        super(source, status, errormsg);
        this.conn = conn;
    }

    public GnutellaConnection getConnection() {
        return conn;
    }
}
