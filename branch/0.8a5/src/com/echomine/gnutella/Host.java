package com.echomine.gnutella;

import com.echomine.common.ParseException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 * Represents one host, and contains the hostname and port.  It also contains methods to check if an
 * ip is a private IP or not.
 */
public class Host {
    private InetAddress host;
    private int port;

    public Host() {
    }

    public Host(String host, int port) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        this.port = port;
    }

    public Host(InetAddress host, int port) {
        this.host = host;
        this.port = port;
    }

    /** deserialize the string and parse the string into a host and a port The format of the string should be host:port */
    public void deserialize(String hostport) throws ParseException {
        StringTokenizer tokens = new StringTokenizer(hostport, ":");
        try {
            host = InetAddress.getByName(tokens.nextToken());
            port = Integer.parseInt(tokens.nextToken());
        } catch (Exception ex) {
            throw new ParseException("Error Parsing String");
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetAddress getHost() {
        return host;
    }

    public void setHost(InetAddress host) {
        this.host = host;
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        //check equality by comparing host and port
        if (!(obj instanceof Host)) return false;
        Host h = (Host) obj;
        if (this.port != h.getPort()) return false;
        if (!this.host.getHostAddress().equals(h.getHost().getHostAddress())) return false;
        return true;
    }

    public boolean isPortValid() {
        return (port <= 0 ? false : true);
    }

    public String toString() {
        return host.getHostAddress() + ":" + port;
    }
}
