package com.echomine.gnutella;

import com.echomine.gnutella.GnutellaConnectionEvent;
import com.echomine.gnutella.GnutellaConnectionModel;
import com.echomine.gnutella.GnutellaContext;
import com.echomine.gnutella.GnutellaProtocolType;
import com.echomine.gnutella.impl.AbstractGnutellaConnection;
import com.echomine.net.ConnectionEvent;
import com.echomine.net.ConnectionVetoException;

public class StubGnutellaConnection extends AbstractGnutellaConnection {
    public StubGnutellaConnection(GnutellaConnectionModel cmodel) {
        super(new GnutellaContext((short) 6346, 1536), GnutellaProtocolType.PROTOCOL_CONNECTOR_V06);
        this.cmodel = cmodel;
    }

    public void fireStartingEventOnly() throws ConnectionVetoException {
        GnutellaConnectionEvent e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_STARTING, this);
        GnutellaConnectionEvent vetoEvent = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_VETOED, this);
        gnutellaHandler.start();
        fireConnectionStarting(e, vetoEvent);
    }

    public void fireStartingEstablishedEventsOnly() throws ConnectionVetoException {
        GnutellaConnectionEvent e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_STARTING, this);
        GnutellaConnectionEvent vetoEvent = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_VETOED, this);
        gnutellaHandler.start();
        fireConnectionStarting(e, vetoEvent);
        e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_OPENED, this);
        fireConnectionEstablished(e);
    }

    public void fireStartingEstablishedClosedEventsOnly() throws ConnectionVetoException {
        GnutellaConnectionEvent e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_STARTING, this);
        GnutellaConnectionEvent vetoEvent = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_VETOED, this);
        gnutellaHandler.start();
        fireConnectionStarting(e, vetoEvent);
        e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_OPENED, this);
        connected = true;
        fireConnectionEstablished(e);
        e = new GnutellaConnectionEvent(cmodel, ConnectionEvent.CONNECTION_CLOSED, this);
        fireConnectionClosed(e);
    }
}
