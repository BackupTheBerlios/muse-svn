package com.echomine.gnutella;

public interface GnutellaMessageRouter extends GnutellaMessageListener {
    /** add our own message to the model.. this is a convenience method to the message model's method. */
    void addToOwnMsg(GnutellaMessage msg);
}
