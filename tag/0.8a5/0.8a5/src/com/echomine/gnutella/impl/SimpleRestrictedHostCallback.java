package com.echomine.gnutella.impl;

import com.echomine.gnutella.IRestrictedHostCallback;

import java.net.InetAddress;
import java.util.HashMap;

/**
 * <p>A default restrict host callback that will take in a set of IPs to restrict.
 * The matching algorithm is simple.  It does a String.startsWith().  Thus, it will
 * take any string in such a that matches from the beginning.  Some examples are
 * given below:</p>
 * <ul>
 *   <li>Full IP: 10.1.1.1</li>
 *   <li>Match 10.1.1.0 - 10.1.1.255: 10.1.1.</li>
 *   <li>Match 10.1.0.0 - 10.1.255.255: 10.1.</li>
 *   <li>Match 10.0.0.0 - 10.255.255.255: 10.</li>
 * </ul>
 * <p>The dot at the end of the IP is inclusive. The IP does not match based
 * on regular expressions (that is something you can implement yourself.</p>
 */
public class SimpleRestrictedHostCallback implements IRestrictedHostCallback {
    private HashMap hosts;

    public SimpleRestrictedHostCallback() {
        hosts = new HashMap();
    }

    /**
     * Checks whether the host is restricted or not.
     * @return true if host is restricted, false otherwise
     */
    public boolean isHostRestricted(InetAddress host) {
        String ip = host.getHostAddress();
        // checks on a x.x.x.x
        if (hosts.containsKey(ip))
            return true;
        for (int i = 0;i < 3;i++) {
            // checks on x.x.x., x.x., and x.
            // The dot at the end of the IP is inclusive
            ip = ip.substring(0, ip.lastIndexOf('.') + 1);
            if (hosts.containsKey(ip))
                return true;
        }
        return false;
    }

    /**
     * adds an IP to the list of restricted hosts to deny access.
     * The restricted host IP can be in a format of x.x.x.x, x.x.x., x.x., and x.
     */
    public void addRestrictedHost(String ip) {
        if (!hosts.containsKey(ip))
            hosts.put(ip, null);
    }

    /**
     * remove the IP off the list that was added earlier
     */
    public void removeRestrictedHost(String ip) {
        hosts.remove(ip);
    }
}
