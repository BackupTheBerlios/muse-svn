package com.echomine.gnutella;

/**
 * <p>Contains statistics about the global Gnutella network.  It includes functionality such as the total number of messages
 * received, the number of hosts, the total number of files shares, the total size in kilobytes, etc.
 * It gives the viewer a way to retrieve any connection statistics information, including per-connection stats as well
 * as global aggregated stats.  It will count all the raw messages (destined for other clients or itself).
 * The network stats can be resetted. The reset will cause a PING message to be sent out and to restart the stat
 * counter for all connections.  This does not reset the aggregated total messages counter. If you want to reset stats
 * for one connection, you should retrieve the GnutellaConnection object and then reset that connection's stats only.
 * </p><p>The stats do not automatically reset periodically. It will require you to create a thread that periodically
 * resets the host information for you.</p>
 */
public interface GnutellaStatistics {
    /** @return the total number of messages going through ALL connections */
    long getTotalMessages();

    /** retrieves the toal number of files offered by all the known hosts. */
    long getTotalFiles();

    /** retrieves the total size for all the files in kilobytes (KB). The information is inside a PONG message. */
    long getTotalSize();

    /** retrieves the number of hosts.  The total is obtained by the number of PONG messages that replied. */
    int getTotalHosts();

    /**
     * resets all the host-related stats.  It will clear the total files, sizes, and number of hosts.
     * Then it will tell all the connection stats to start counting all over again.  This will also reset
     * all connection-specific stats, including the total messages received stats for each connection.
     * However, this method will not reset the global total number of messages.
     */
    void resetHostStats();

    /** resets the total messages. It does not reset the connection total messages. */
    void resetTotalMessages();

    /** adds a listener that listens for stat updates */
    void addStatisticsListener(GnutellaStatisticsListener l);

    /** removes a lsitener from listening to stat updates */
    void removeStatisticsListener(GnutellaStatisticsListener l);
}
