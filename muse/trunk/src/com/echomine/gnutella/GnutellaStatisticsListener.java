package com.echomine.gnutella;


/**
 * contains listner method that should be implemented to listen for statistics.  These includes connection specific
 * events as well as global stat events.
 */
public interface GnutellaStatisticsListener extends GnutellaConnectionStatisticsListener {
    /**
     * The global stats has just been updated.  This may not correspond to when a connection stat is updated.
     * The method gets a GnutellaStatistics stats object that contains all the statistical information.
     * Simply, the conn contains the stats that has just been updated.  It
     * can be null if no connection stats is related to this update event (ie. a reset is sent,
     * which essentially means that no connection fired it, but rather explicitly resetted by the caller).
     * You must check for the null if you're going to use the connection object.
     * @param stats the global stats for gnutella network
     */
    void globalStatsUpdated(GnutellaStatistics stats);
}
