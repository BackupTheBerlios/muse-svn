package com.echomine.gnutella.impl;

import com.echomine.gnutella.*;

import javax.swing.event.EventListenerList;

/** Base class that adds message listening capability. */
abstract public class AbstractMessageReceiver implements RawDataReceivable {
    protected EventListenerList listenerList = new EventListenerList();

    /** The listener will listen to all unfiltered messages this router receives. */
    public void addMessageListener(GnutellaMessageListener l) {
        listenerList.add(GnutellaMessageListener.class, l);
    }

    public void removeMessageListener(GnutellaMessageListener l) {
        listenerList.remove(GnutellaMessageListener.class, l);
    }

    protected void fireMessageReceived(GnutellaConnection connection, GnutellaMessage msg) {
        Object[] listeners = listenerList.getListenerList();
        GnutellaMessageEvent event = new GnutellaMessageEvent(connection, msg);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == GnutellaMessageListener.class) {
                // Lazily create the event:
                ((GnutellaMessageListener) listeners[i + 1]).messageReceived(event);
            }
        }
    }
}
