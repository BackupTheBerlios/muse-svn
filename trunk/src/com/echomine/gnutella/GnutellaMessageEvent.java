package com.echomine.gnutella;

import java.util.EventObject;

/**
 * A message event containing the message that was received and a reference copy of the connection
 * where the message came from.
 */
public class GnutellaMessageEvent extends EventObject {
    private GnutellaMessage msg;

    public GnutellaMessageEvent(GnutellaConnection source, GnutellaMessage msg) {
        super(source);
        this.msg = msg;
    }

    public GnutellaConnection getConnection() {
        return (GnutellaConnection) getSource();
    }

    public GnutellaMessage getMessage() {
        return msg;
    }
}
