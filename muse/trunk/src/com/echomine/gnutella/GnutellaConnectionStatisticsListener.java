package com.echomine.gnutella;

import java.util.EventListener;

/**
 * This is specially used to listen for stat changes for each connection.  Normally you should not have to work
 * with this class but go through GnutellaStatistics instead (it duplicates this behavior).  However, if you
 * want to listen specifically for individual connection stats, this will be the one to use.
 */
public interface GnutellaConnectionStatisticsListener extends EventListener {
    /**
     * The event is fired when stats are updated for the connection.
     */
    void connectionStatsUpdated(GnutellaConnection conn);
}
