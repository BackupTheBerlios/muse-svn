package com.echomine.gnutella;

import java.util.EventListener;

/**
 * Processes the messages (raw data that's been converted into recognizable messages).  Implementors can process specific
 * subsets of message types (ie. File messages, search messages, chat messages).
 */
public interface GnutellaMessageListener extends EventListener {
    void messageReceived(GnutellaMessageEvent event);
}
