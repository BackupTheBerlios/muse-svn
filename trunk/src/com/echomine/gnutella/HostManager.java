package com.echomine.gnutella;

import java.io.File;
import java.io.IOException;

public interface HostManager {
    /** save the host list out to a file */
    void save(File file) throws IOException;

    /**
     * load a list of hosts from a file and use those as the first set of hosts.
     * For efficiency, the loading is sent to a background thread for loading.  The
     * method will return immediately before all hosts are loaded.
     */
    void load(File file) throws IOException;

    /** @return the next host on the list */
    Host next();

    /**
     * adds a host to the beginning of the list.  If host is already in the list, it will not add it.
     * It will be picked up on the next next() call. This method does NOT fire a host added event since the developer
     * should know the existence of this host already.
     */
    void addHost(Host host);

    /** @return the number of hosts counts currently stored in the manager */
    int getHostCount();

    /** @return the max hosts that the manager will maintain */
    int getMaxhosts();

    /** sets the max hosts that the manager will store */
    void setMaxhosts(int maxhosts);

    /** listen for any host adding and removing events */
    void addHostListener(HostListener l);

    /** remove from listening for any host adding and removing events */
    void removeHostListener(HostListener l);
}
