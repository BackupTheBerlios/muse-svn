package com.echomine.gnutella;

import java.net.InetAddress;

/**
 * The restricted host callback is used to check whether
 * a host is restricted or not.  The implementor can
 * choose to use different methods of restricting hosts.
 * A simple one can just do a check on a per-IP basis while
 * a more advanced one can use regular expressions to 
 * restrict the hosts and offer more advanced ways in restricting
 * the host.
 */
public interface IRestrictedHostCallback {
    /**
     * Checks whether the host is restricted or not.
     * @return true if host is restricted, false otherwise
     */
    public boolean isHostRestricted(InetAddress host);
} 
