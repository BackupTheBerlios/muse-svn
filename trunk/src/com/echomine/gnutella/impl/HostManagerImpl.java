package com.echomine.gnutella.impl;

import com.echomine.common.ParseException;
import com.echomine.gnutella.*;
import com.echomine.net.ConnectionEvent;
import com.echomine.net.ConnectionListener;
import com.echomine.net.ConnectionVetoException;
import com.echomine.util.IPUtil;
import com.echomine.util.HTTPHeader;

import javax.swing.event.EventListenerList;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * The Manager listens for Pong messages, and records down the hosts.
 * It also contains methods for saving and loading the hosts from a file.
 */
public class HostManagerImpl implements HostManager {
    public static final int MAX_HOSTS = 4096;
    protected EventListenerList listenerList = new EventListenerList();
    LinkedList hosts = new LinkedList();
    private int maxhosts;
    private HostManagerMessageListener msgListener;
    private HostConnectionListener connListener;

    public HostManagerImpl(GnutellaConnectionManager cmanager) {
        this(cmanager, MAX_HOSTS);
    }

    public HostManagerImpl(GnutellaConnectionManager cmanager, int maxhosts) {
        msgListener = new HostManagerMessageListener();
        connListener = new HostConnectionListener();
        //for listening to X-Try and X-Try-Ultrapeer connection headers
        cmanager.addConnectionListener(connListener);
        this.maxhosts = maxhosts;
    }

    /** save the host list out to a file */
    public void save(File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        Object[] objs = hosts.toArray();
        for (int i = 0; i < objs.length; i++) {
            if (objs[i] != null)
                writer.write(objs[i].toString() + "\n");
        }
        writer.flush();
        writer.close();
    }

    /**
     * load a list of hosts from a file and use those as the first set of hosts.
     * For efficiency, the loading is sent to a background thread for loading.  The
     * method will return immediately before all hosts are loaded.
     */
    public void load(File file) throws IOException {
        if (!file.exists())
            throw new IOException("File doesn't exist for loading hosts");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String connection;
        ArrayList hostStrings = new ArrayList();
        while ((connection = reader.readLine()) != null) {
            hostStrings.add(connection);
        }
        reader.close();
        final ArrayList tempHosts = hostStrings;
        //turn the host strings into Host objects in the
        //background thread due to Host using DNS resolution.
        Thread thread = new Thread() {
            public void run() {
                Host host;
                String conn;
                int size = tempHosts.size();
                for (int i = 0; i < size; i++) {
                    conn = (String) tempHosts.get(i);
                    try {
                        host = new Host();
                        host.deserialize(conn);
                        hosts.add(host);
                        fireHostAdded(host);
                    } catch (ParseException ex) {
                    }
                }
            }
        };
        thread.start();
    }

    /** @return the next host on the list */
    public Host next() {
        try {
            Host host = (Host) hosts.removeFirst();
            fireHostRemoved(host);
            return host;
        } catch (NoSuchElementException ex) {
            return null;
        }
    }

    /**
     * adds a host to the beginning of the list.  If host is already in the list, it will not add it.
     * It will be picked up on the next next() call. This method does NOT fire a host added event since the developer
     * should know the existence of this host already.
     */
    public void addHost(Host host) {
        if (host == null) return;
        if (!hosts.contains(host)) {
            hosts.addFirst(host);
            fireHostAdded(host);
        }
    }

    public int getHostCount() {
        return hosts.size();
    }

    public int getMaxhosts() {
        return maxhosts;
    }

    public void setMaxhosts(int maxhosts) {
        this.maxhosts = maxhosts;
    }

    public void addHostListener(HostListener l) {
        listenerList.add(HostListener.class, l);
    }

    public void removeHostListener(HostListener l) {
        listenerList.remove(HostListener.class, l);
    }

    protected void fireHostAdded(Host host) {
        Object[] listeners = listenerList.getListenerList();
        HostEvent event = new HostEvent(this, host);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == HostListener.class) {
                // Lazily create the event:
                ((HostListener) listeners[i + 1]).hostAdded(event);
            }
        }
    }

    protected void fireHostRemoved(Host host) {
        Object[] listeners = listenerList.getListenerList();
        HostEvent event = new HostEvent(this, host);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == HostListener.class) {
                // Lazily create the event:
                ((HostListener) listeners[i + 1]).hostRemoved(event);
            }
        }
    }

    class HostConnectionListener implements ConnectionListener {
        public void connectionStarting(ConnectionEvent event) throws ConnectionVetoException {
        }

        public void connectionEstablished(ConnectionEvent event) {
            storeHost(event);
            //connection established, listen for low level messages
            GnutellaConnection conn = ((GnutellaConnectionEvent) event).getConnection();
            conn.addMessageListener(msgListener);
        }

        public void connectionClosed(ConnectionEvent event) {
            storeHost(event);
        }

        void storeHost(ConnectionEvent event) {
            //connection closed, disable from listening for low level messages
            GnutellaConnection conn = ((GnutellaConnectionEvent) event).getConnection();
            conn.removeMessageListener(msgListener);
            //obtain the X-Try headers here
            HTTPHeader headers = conn.getSupportedFeatureHeaders();
            if (headers == null) return;
            String[] headerNames = new String[] {"X-Try", "X-Try-Ultrapeers"};
            String value, token;
            StringTokenizer tokenizer;
            Host host;
            for (int i = 0; i < headerNames.length;i++) {
                value = headers.getHeader(headerNames[i]);
                if (value != null) {
                    tokenizer = new StringTokenizer(value,",");
                    try {
                        //if any parsing error occurs, we reject the entire string
                        while (tokenizer.hasMoreTokens()) {
                            token = tokenizer.nextToken();
                            host = new Host();
                            host.deserialize(token);
                            if (host.isPortValid() && !IPUtil.isHostIPPrivate(host.getHost().getHostAddress()) && !hosts.contains(host)) {
                                hosts.addLast(host);
                                fireHostAdded(host);
                            }
                        }
                    } catch (ParseException ex) {
                    }
                }
            }
        }
    }


    class HostManagerMessageListener implements GnutellaMessageListener {
        public void messageReceived(GnutellaMessageEvent event) {
            //only look for Pong messages and store the hosts
            GnutellaMessage msg = event.getMessage();
            if (msg.getType() == GnutellaCode.PONG) {
                if (hosts.size() < maxhosts) {
                    MsgInitResponse pong = (MsgInitResponse) msg;
                    if (pong.getIP().getHostAddress().startsWith("0.")) return;
                    Host h = new Host(pong.getIP(), pong.getPort());
                    if (h.isPortValid() && !IPUtil.isHostIPPrivate(h.getHost().getHostAddress()) && !hosts.contains(h)) {
                        hosts.addLast(h);
                        fireHostAdded(h);
                    }
                }
            }
        }
    }
}
