package com.echomine.gnutella.impl;

import com.echomine.gnutella.*;
import com.echomine.net.ConnectionEvent;
import com.echomine.net.ConnectionListener;
import com.echomine.net.ConnectionModel;
import com.echomine.net.ConnectionVetoException;

import javax.swing.event.EventListenerList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * <p>This class implements the GnutellaStatistics interface to offer Gnutella Stats on a global level.</p>
 */
public class GnutellaStatisticsImpl implements GnutellaStatistics {
    EventListenerList listenerList = new EventListenerList();
    long totalMessages;
    long totalFiles;
    long totalSize;
    int totalHosts;
    GnutellaConnectionManager cmanager;
    HashMap connStats;

    public GnutellaStatisticsImpl(GnutellaConnectionManager cman) {
        this.cmanager = cman;
        connStats = new HashMap();
        cmanager.addConnectionListener(new StatsConnectionListener());
    }

    /**
     * retrieves the total number of messages that has been received.
     * This also counts messages that are not destined for us.  In effect,
     * it gives us the number of messages that we saw, routed or non-routed.
     */
    public long getTotalMessages() {
        return totalMessages;
    }

    /** retrieves the toal number of files offered by all the known hosts. */
    public long getTotalFiles() {
        return totalFiles;
    }

    /** retrieves the total size for all the files in kilobytes (KB). The information is inside a PONG message. */
    public long getTotalSize() {
        return totalSize;
    }

    /** retrieves the number of hosts.  The total is obtained by the number of PONG messages that replied. */
    public int getTotalHosts() {
        return totalHosts;
    }

    /**
     * resets all the host-related stats.  It will clear the total files, sizes, and number of hosts.
     * Then it will tell all the connection stats to start counting all over again.  This will also reset
     * all connection-specific stats, including the total messages received stats for each connection.
     * However, this method will not reset the global total number of messages.
     */
    public void resetHostStats() {
        totalHosts = 0;
        totalSize = 0;
        totalFiles = 0;
        //reset per connection stats
        synchronized (connStats) {
            Iterator iter = connStats.values().iterator();
            GnutellaConnection conn;
            while (iter.hasNext()) {
                conn = (GnutellaConnection) iter.next();
                conn.resetStats();
            }
        }
    }

    /** resets the total messages. It does not reset the connection total messages. */
    public void resetTotalMessages() {
        totalMessages = 0;
        fireGlobalStatsUpdated();
    }

    /**
     * This retrieves the number of active connections we are connected to
     * @return the current number of clients that we are connected to
     */
    public int getEstablishedConnectionsCount() {
        //simply use gnutellastat's established connections count
        return connStats.size();
    }

    /** @return the connection stats for a particular connection, or null if one doesn't exist for it */
    public GnutellaConnection getConnection(ConnectionModel model) {
        return (GnutellaConnection) connStats.get(model.toString());
    }

    /** @return an iterator that contains all the GnutelalConnection objects for connections */
    public Iterator getConnections() {
        HashMap statsClone = null;
        synchronized (connStats) {
            statsClone = (HashMap) connStats.clone();
        }
        return statsClone.values().iterator();
    }

    public void addStatisticsListener(GnutellaStatisticsListener l) {
        listenerList.add(GnutellaStatisticsListener.class, l);
    }

    public void removeStatisticsListener(GnutellaStatisticsListener l) {
        listenerList.remove(GnutellaStatisticsListener.class, l);
    }

    protected void fireGlobalStatsUpdated() {
        //notify listeners
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == GnutellaStatisticsListener.class) {
                // Lazily create the event:
                ((GnutellaStatisticsListener) listeners[i + 1]).globalStatsUpdated(this);
            }
        }
    }

    protected void fireConnectionStatsUpdated(GnutellaConnection conn) {
        //notify listeners
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == GnutellaStatisticsListener.class) {
                // Lazily create the event:
                ((GnutellaStatisticsListener) listeners[i + 1]).connectionStatsUpdated(conn);
            }
        }
    }

    class StatsConnectionListener implements ConnectionListener {
        GnutellaConnectionStatisticsListener cStatListener;

        public void connectionStarting(ConnectionEvent event) throws ConnectionVetoException {
            cStatListener = new GnutellaConnectionStatisticsListener() {
                public void connectionStatsUpdated(GnutellaConnection conn) {
                    fireConnectionStatsUpdated(conn);
                }
            };
        }

        public void connectionEstablished(ConnectionEvent event) {
            //send the current PING message to start collecting data
            //on this new connection
            GnutellaConnection conn = ((GnutellaConnectionEvent) event).getConnection();
            ConnectionModel model = event.getConnectionModel();
            synchronized (connStats) {
                if (!connStats.containsKey(model.toString())) {
                    connStats.put(model.toString(), conn);
                    conn.addStatisticsListener(cStatListener);
                }
            }
        }

        public void connectionClosed(ConnectionEvent event) {
            ConnectionModel model = event.getConnectionModel();
            GnutellaConnection conn = null;
            synchronized (connStats) {
                conn = (GnutellaConnection) connStats.get(model.toString());
                if (conn != null) {
                    //remove stat listener
                    conn.removeStatisticsListener(cStatListener);
                    //then remove the connection statistics information for this client
                    connStats.remove(model.toString());
                    //now subtract the stats for the connStat that was just closed
                    //this will create a more accurate global stats
                    //note that the total message are not subtracted because
                    //the total messages will always grow and counts the
                    //number of messages that's been received since network
                    //went online
                    synchronized (this) {
                        totalFiles -= conn.getFiles();
                        totalSize -= conn.getSize();
                        totalHosts -= conn.getHosts();
                    }
                }
            }
            if (conn != null)
                fireGlobalStatsUpdated();
        }
    }
}
