package com.echomine.gnutella;

/**
 * Receives raw data from the underlying protocol (data from remote server).  The implementer needs to process the data and
 * send out the data somehow for other to see.
 */
public interface RawDataReceivable {
    void receive(GnutellaConnection connection, GnutellaMessageHeader header, byte[] data);

    /** The listener will listen to all unfiltered messages this router receives. */
    void addMessageListener(GnutellaMessageListener l);

    void removeMessageListener(GnutellaMessageListener l);
}
