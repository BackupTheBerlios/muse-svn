package com.echomine.gnutella;

import java.util.EventListener;

/** interface to implement for Listening to host manager related events. */
public interface HostListener extends EventListener {
    /**
     * fires an event that indicates a host has been added to the HostManager list of hosts.  The return parameter returns
     * true if the host is used and should not be put back in the list of hosts, and false if the host isn't used and should
     * be put in the list of hosts.
     */
    void hostAdded(HostEvent event);

    void hostRemoved(HostEvent event);
}
