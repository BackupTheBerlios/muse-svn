package com.echomine.gnutella;

import java.util.EventObject;

/** Encapsulates a Host event.  The host contained inside the event is the host that was just received. */
public class HostEvent extends EventObject {
    private Host host;

    public HostEvent(HostManager source, Host host) {
        super(source);
        this.host = host;
    }

    public HostManager getHostManager() {
        return (HostManager) getSource();
    }

    public Host getHost() {
        return host;
    }
}
